package com.unimarket.search.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.unimarket.module.goods.service.GoodsReferenceService;
import com.unimarket.search.document.GoodsDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 估价参考数据服务 - Elasticsearch 实现
 * 替代原有的 MySQL 查询，从 ES goods 索引获取同类商品参考数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsReferenceEsServiceImpl implements GoodsReferenceService {

    private static final int MAX_LIMIT = 50;

    private static final List<FieldValue> REFERENCE_TRADE_STATUS = Arrays.asList(
            FieldValue.of(0), FieldValue.of(1)  // ON_SALE + SOLD
    );
    private static final List<FieldValue> REFERENCE_REVIEW_STATUS = Arrays.asList(
            FieldValue.of(1), FieldValue.of(2)  // AI_PASSED + MANUAL_PASSED
    );

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public String buildPriceReferenceData(Long categoryId, int limit) {
        List<GoodsDocument> referenceGoods = listReferenceGoods(categoryId, limit);
        if (referenceGoods.isEmpty()) {
            return "";
        }

        StringBuilder referenceData = new StringBuilder("参考同类商品数据：\n");
        for (int index = 0; index < referenceGoods.size(); index++) {
            GoodsDocument doc = referenceGoods.get(index);
            referenceData.append(String.format(
                    "%d. 标题：%s，成色：%d成新，价格：%.2f元\n",
                    index + 1,
                    doc.getTitle() != null ? doc.getTitle() : "未知",
                    doc.getItemCondition() != null ? doc.getItemCondition() : 5,
                    doc.getPrice() != null ? doc.getPrice().doubleValue() : 0.0
            ));
        }
        return referenceData.toString();
    }

    @Override
    public int countPriceReferenceGoods(Long categoryId) {
        NativeQuery query = buildReferenceQuery(categoryId, 1);
        try {
            SearchHits<GoodsDocument> hits = elasticsearchOperations.search(query, GoodsDocument.class);
            return (int) hits.getTotalHits();
        } catch (Exception e) {
            log.error("ES count failed for price reference, categoryId={}", categoryId, e);
            return 0;
        }
    }

    private List<GoodsDocument> listReferenceGoods(Long categoryId, int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        NativeQuery query = buildReferenceQuery(categoryId, normalizedLimit);
        query.addSort(Sort.by(Sort.Order.desc("createTime")));

        try {
            SearchHits<GoodsDocument> hits = elasticsearchOperations.search(query, GoodsDocument.class);
            return hits.getSearchHits().stream()
                    .map(hit -> hit.getContent())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("ES query failed for price reference, categoryId={}", categoryId, e);
            return List.of();
        }
    }

    private NativeQuery buildReferenceQuery(Long categoryId, int limit) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // Category filter
        boolQuery.filter(Query.of(q -> q
                .term(t -> t.field("categoryId").value(categoryId))
        ));

        // Trade status: ON_SALE(0) + SOLD(1)
        boolQuery.filter(Query.of(q -> q
                .terms(t -> t.field("tradeStatus")
                        .terms(v -> v.value(REFERENCE_TRADE_STATUS)))
        ));

        // Review status: AI_PASSED(1) + MANUAL_PASSED(2)
        boolQuery.filter(Query.of(q -> q
                .terms(t -> t.field("reviewStatus")
                        .terms(v -> v.value(REFERENCE_REVIEW_STATUS)))
        ));

        // Price > 0
        boolQuery.filter(Query.of(q -> q
                .range(r -> r.field("price").gt(JsonData.of(0)))
        ));

        return NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(boolQuery.build())))
                .withPageable(PageRequest.of(0, limit))
                .build();
    }
}
