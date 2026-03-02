package com.unimarket.module.goods.service;

import com.unimarket.module.goods.vo.CategoryVO;

import java.util.List;

/**
 * 分类Service接口
 */
public interface CategoryService {

    /**
     * 查询分类树
     */
    List<CategoryVO> getCategoryTree();

    /**
     * 查询一级分类列表
     */
    List<CategoryVO> getFirstLevelCategories();

    /**
     * 查询一级分类下的子分类ID
     */
    List<Integer> getChildCategoryIds(Integer parentCategoryId);
}
