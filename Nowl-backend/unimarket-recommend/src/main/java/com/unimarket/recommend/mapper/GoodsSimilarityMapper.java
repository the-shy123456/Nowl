package com.unimarket.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.recommend.entity.GoodsSimilarity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品相似度Mapper
 */
@Mapper
public interface GoodsSimilarityMapper extends BaseMapper<GoodsSimilarity> {

    /**
     * 查询相似商品
     */
    @Select("SELECT similar_product_id, similarity_score FROM goods_similarity " +
            "WHERE product_id = #{productId} AND similarity_type = #{type} " +
            "ORDER BY similarity_score DESC LIMIT #{limit}")
    List<GoodsSimilarity> selectSimilarProducts(
        @Param("productId") Long productId,
        @Param("type") Integer type,
        @Param("limit") int limit
    );

    /**
     * 查询所有类型的相似商品（融合）
     */
    @Select("SELECT similar_product_id, MAX(similarity_score) as similarity_score " +
            "FROM goods_similarity WHERE product_id = #{productId} " +
            "GROUP BY similar_product_id ORDER BY similarity_score DESC LIMIT #{limit}")
    List<GoodsSimilarity> selectAllSimilarProducts(
        @Param("productId") Long productId,
        @Param("limit") int limit
    );
}
