package com.unimarket.search.repository;

import com.unimarket.search.document.GoodsDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 商品ES仓库
 */
@Repository
public interface GoodsSearchRepository extends ElasticsearchRepository<GoodsDocument, Long> {

    /**
     * 根据学校编码查询
     */
    List<GoodsDocument> findBySchoolCode(String schoolCode);

    /**
     * 根据分类ID查询
     */
    List<GoodsDocument> findByCategoryId(Integer categoryId);

    /**
     * 根据卖家ID查询
     */
    List<GoodsDocument> findBySellerId(Long sellerId);

    /**
     * 根据交易状态查询
     */
    List<GoodsDocument> findByTradeStatus(Integer tradeStatus);
}
