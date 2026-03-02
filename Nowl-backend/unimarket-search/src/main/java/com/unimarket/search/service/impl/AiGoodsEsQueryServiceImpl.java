package com.unimarket.search.service.impl;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.unimarket.ai.vo.AiGoodsCardVO;
import com.unimarket.module.aiassistant.service.AiGoodsQueryService;
import com.unimarket.search.document.GoodsDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 商品查询服务 - Elasticsearch 实现
 * 替代原有的 MySQL LIKE 查询，使用 IK 分词 + 拼音搜索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGoodsEsQueryServiceImpl implements AiGoodsQueryService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private static final List<FieldValue> PASSED_REVIEW_STATUS = Arrays.asList(
            FieldValue.of(1), FieldValue.of(2)
    );

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public List<AiGoodsCardVO> searchAvailableGoodsByKeyword(
            String schoolCode,
            String campusCode,
            String keyword,
            int limit,
            BigDecimal maxPrice,
            int page
    ) {
        int resolvedLimit = resolveLimit(limit);
        int resolvedPage = resolvePage(page);
        NativeQuery query = buildQuery(schoolCode, campusCode, keyword, true, resolvedLimit, maxPrice, resolvedPage);
        query.addSort(Sort.by(Sort.Order.desc("createTime"), Sort.Order.desc("collectCount")));
        return executeAndConvert(query);
    }

    @Override
    public List<AiGoodsCardVO> findCheapestGoodsByKeyword(
            String schoolCode,
            String campusCode,
            String keyword,
            int limit,
            BigDecimal maxPrice,
            int page
    ) {
        int resolvedLimit = resolveLimit(limit);
        int resolvedPage = resolvePage(page);
        NativeQuery query = buildQuery(schoolCode, campusCode, keyword, true, resolvedLimit, maxPrice, resolvedPage);
        query.addSort(Sort.by(Sort.Order.asc("price"), Sort.Order.desc("createTime")));
        return executeAndConvert(query);
    }

    @Override
    public List<AiGoodsCardVO> recommendAvailableGoods(
            String schoolCode,
            String campusCode,
            String keyword,
            int limit,
            BigDecimal maxPrice,
            int page
    ) {
        int resolvedLimit = resolveLimit(limit);
        int resolvedPage = resolvePage(page);
        NativeQuery query = buildQuery(schoolCode, campusCode, keyword, true, resolvedLimit, maxPrice, resolvedPage);

        if (StrUtil.isNotBlank(keyword)) {
            // With keyword: relevance + hotScore + collectCount
            query.addSort(Sort.by(
                    Sort.Order.desc("_score"),
                    Sort.Order.desc("hotScore"),
                    Sort.Order.desc("collectCount"),
                    Sort.Order.desc("createTime")
            ));
        } else {
            // Without keyword: hotScore + collectCount
            query.addSort(Sort.by(
                    Sort.Order.desc("hotScore"),
                    Sort.Order.desc("collectCount"),
                    Sort.Order.desc("createTime")
            ));
        }

        return executeAndConvert(query);
    }

    @Override
    public long countAvailableGoodsByKeyword(String schoolCode, String campusCode, String keyword, BigDecimal maxPrice) {
        NativeQuery query = buildQuery(schoolCode, campusCode, keyword, true, 0, maxPrice, 0);
        SearchHits<GoodsDocument> hits = elasticsearchOperations.search(query, GoodsDocument.class);
        return hits.getTotalHits();
    }

    /**
     * Build the ES bool query with common filters
     *
     * @param onSaleOnly true = only ON_SALE(0); false = ON_SALE(0) + SOLD(1)
     */
    private NativeQuery buildQuery(
            String schoolCode, String campusCode, String keyword,
            boolean onSaleOnly, int limit, BigDecimal maxPrice, int page) {

        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // Keyword: multi_match with IK + pinyin
        if (StrUtil.isNotBlank(keyword)) {
            String kw = keyword.trim();
            boolQuery.must(Query.of(q -> q
                    .multiMatch(m -> m
                            .query(kw)
                            .fields("title^3", "title.pinyin^2", "description")
                            .fuzziness("AUTO")
                    )
            ));
        }

        // Trade status filter
        if (onSaleOnly) {
            boolQuery.filter(Query.of(q -> q
                    .term(t -> t.field("tradeStatus").value(0))
            ));
        } else {
            boolQuery.filter(Query.of(q -> q
                    .terms(t -> t.field("tradeStatus")
                            .terms(v -> v.value(Arrays.asList(FieldValue.of(0), FieldValue.of(1)))))
            ));
        }

        // Review status: AI_PASSED(1) or MANUAL_PASSED(2)
        boolQuery.filter(Query.of(q -> q
                .terms(t -> t.field("reviewStatus")
                        .terms(v -> v.value(PASSED_REVIEW_STATUS)))
        ));

        // School scope
        if (StrUtil.isNotBlank(schoolCode)) {
            boolQuery.filter(Query.of(q -> q
                    .term(t -> t.field("schoolCode").value(schoolCode))
            ));
        }

        // Campus scope
        if (StrUtil.isNotBlank(campusCode)) {
            boolQuery.filter(Query.of(q -> q
                    .term(t -> t.field("campusCode").value(campusCode))
            ));
        }

        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) > 0) {
            boolQuery.filter(Query.of(q -> q
                    .range(r -> {
                        r.field("price");
                        r.lte(co.elastic.clients.json.JsonData.of(maxPrice));
                        return r;
                    })
            ));
        }

        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(boolQuery.build())));

        if (limit > 0) {
            builder.withPageable(PageRequest.of(page, limit));
        } else {
            // count-only: fetch 0 documents but still get totalHits
            builder.withPageable(PageRequest.of(0, 1));
        }

        return builder.build();
    }

    private List<AiGoodsCardVO> executeAndConvert(NativeQuery query) {
        try {
            SearchHits<GoodsDocument> hits = elasticsearchOperations.search(query, GoodsDocument.class);
            return hits.getSearchHits().stream()
                    .map(hit -> convertToCard(hit.getContent()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("ES query failed for AI goods search", e);
            return Collections.emptyList();
        }
    }

    private AiGoodsCardVO convertToCard(GoodsDocument doc) {
        AiGoodsCardVO card = new AiGoodsCardVO();
        card.setProductId(doc.getProductId());
        card.setTitle(StrUtil.blankToDefault(doc.getTitle(), "未命名商品"));
        card.setPrice(doc.getPrice());
        card.setImage(doc.getImage());
        card.setSellerName(StrUtil.blankToDefault(doc.getSellerName(), "匿名卖家"));
        card.setSchoolCode(doc.getSchoolCode());
        card.setCampusCode(doc.getCampusCode());
        return card;
    }

    private int resolveLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private int resolvePage(int page) {
        return Math.max(page, 0);
    }
}
