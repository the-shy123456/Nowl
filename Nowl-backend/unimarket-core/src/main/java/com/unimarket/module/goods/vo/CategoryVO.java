package com.unimarket.module.goods.vo;

import lombok.Data;

import java.util.List;

/**
 * 商品分类返回对象
 */
@Data
public class CategoryVO {

    private Integer categoryId;

    private String categoryName;

    private Integer parentId;

    private Integer sort;

    private Integer status;

    private List<CategoryVO> children;
}
