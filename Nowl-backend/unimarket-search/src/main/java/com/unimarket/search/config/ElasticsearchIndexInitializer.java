package com.unimarket.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.unimarket.search.document.ErrandDocument;
import com.unimarket.search.document.GoodsDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * ES索引初始化器
 * 启动时确保索引存在且使用正确的 ik/pinyin 分词器。
 * 如果索引不存在或分词器不正确，会（重新）创建索引。
 * 数据通过 MQ 链路实时同步，无需全量同步。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchIndexInitializer {

    private final ElasticsearchClient client;
    private final ElasticsearchOperations elasticsearchOperations;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndices() {
        ensureIndex("goods", GoodsDocument.class);
        ensureIndex("errand", ErrandDocument.class);
    }

    /**
     * 确保索引存在且使用正确的分词器
     */
    private void ensureIndex(String indexName, Class<?> documentClass) {
        IndexOperations indexOps = elasticsearchOperations.indexOps(documentClass);

        if (indexOps.exists()) {
            if (hasCorrectAnalyzer(indexName)) {
                log.info("ES索引[{}]正常", indexName);
                return;
            }
            indexOps.delete();
            log.info("ES索引[{}]分词器不正确，已删除", indexName);
        }

        indexOps.create();
        indexOps.putMapping();
        log.info("ES索引[{}]已创建（ik/pinyin分词器）", indexName);
    }

    /**
     * 通过 _analyze API 检测索引是否包含 pinyin_analyzer
     */
    private boolean hasCorrectAnalyzer(String indexName) {
        try {
            client.indices().analyze(r -> r
                    .index(indexName)
                    .analyzer("pinyin_analyzer")
                    .text("test")
            );
            return true;
        } catch (Exception e) {
            log.debug("索引[{}]不包含pinyin_analyzer: {}", indexName, e.getMessage());
            return false;
        }
    }
}
