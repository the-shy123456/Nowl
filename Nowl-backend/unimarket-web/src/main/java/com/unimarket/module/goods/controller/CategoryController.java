package com.unimarket.module.goods.controller;

import com.unimarket.common.result.Result;
import com.unimarket.module.goods.service.CategoryService;
import com.unimarket.module.goods.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类Controller
 */
@Slf4j
@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 查询分类树
     */
    @GetMapping("/tree")
    public Result<List<CategoryVO>> tree() {
        List<CategoryVO> tree = categoryService.getCategoryTree();
        return Result.success(tree);
    }

    /**
     * 查询一级分类列表
     */
    @GetMapping("/list")
    public Result<List<CategoryVO>> list() {
        List<CategoryVO> list = categoryService.getFirstLevelCategories();
        return Result.success(list);
    }
}
