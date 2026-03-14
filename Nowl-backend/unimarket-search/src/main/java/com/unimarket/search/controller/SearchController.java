package com.unimarket.search.controller;

import com.unimarket.common.result.PageResult;
import com.unimarket.common.result.Result;
import com.unimarket.module.goods.service.CategoryService;
import com.unimarket.search.dto.ErrandSearchRequestDTO;
import com.unimarket.search.dto.SearchRequestDTO;
import com.unimarket.search.service.ErrandSearchService;
import com.unimarket.search.service.SearchService;
import com.unimarket.search.service.SearchSyncService;
import com.unimarket.search.service.SearchTrackingAsyncService;
import com.unimarket.search.vo.ErrandSearchResultVO;
import com.unimarket.search.vo.SearchResultVO;
import com.unimarket.security.UserContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索Controller
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final SearchSyncService searchSyncService;
    private final ErrandSearchService errandSearchService;
    private final CategoryService categoryService;
    private final SearchTrackingAsyncService searchTrackingAsyncService;

    /**
     * 商品搜索
     */
    @GetMapping("/goods")
    public Result<PageResult<SearchResultVO>> search(@Valid SearchRequestDTO request,
                                                     @RequestParam(required = false) Integer parentCategoryId) {
        Long userId = UserContextHolder.getUserId();
        String schoolCode = UserContextHolder.getSchoolCode();

        // 如果用户已认证且未指定学校，使用用户的学校
        if (request.getSchoolCode() == null && schoolCode != null && UserContextHolder.isAuthenticated()) {
            request.setSchoolCode(schoolCode);
        }

        // 处理一级分类：查询其下所有子分类ID
        if (parentCategoryId != null && request.getCategoryId() == null) {
            List<Integer> childCategoryIds = categoryService.getChildCategoryIds(parentCategoryId);
            if (!childCategoryIds.isEmpty()) {
                request.setCategoryIds(childCategoryIds);
            }
        }

        // 记录搜索历史和热搜
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            searchTrackingAsyncService.recordSearchHistory(userId, request.getKeyword());
            searchTrackingAsyncService.incrementHotWord(request.getKeyword(), request.getSchoolCode());
        }

        PageResult<SearchResultVO> result = searchService.search(request, userId);
        return Result.success(result);
    }

    /**
     * 搜索建议
     */
    @GetMapping("/suggest")
    public Result<List<String>> suggest(@RequestParam String keyword,
                                        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        List<String> suggestions = searchService.suggest(keyword, size);
        return Result.success(suggestions);
    }

    /**
     * 热搜词
     */
    @GetMapping("/hot")
    public Result<List<String>> getHotWords(
            @RequestParam(required = false) String schoolCode,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        // 如果未指定学校，尝试使用用户的学校
        if (schoolCode == null) {
            schoolCode = UserContextHolder.getSchoolCode();
        }

        List<String> hotWords = searchService.getHotWords(schoolCode, size);
        return Result.success(hotWords);
    }

    /**
     * 获取搜索历史
     */
    @GetMapping("/history")
    public Result<List<String>> getSearchHistory(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.success(List.of());
        }
        List<String> history = searchService.getSearchHistory(userId, size);
        return Result.success(history);
    }

    /**
     * 清空搜索历史
     */
    @DeleteMapping("/history")
    public Result<Void> clearSearchHistory() {
        Long userId = UserContextHolder.getUserId();
        if (userId != null) {
            searchService.clearSearchHistory(userId);
        }
        return Result.success();
    }

    /**
     * 全量同步（管理员接口）
     */
    @PostMapping("/sync/full")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public Result<Void> fullSync() {
        searchSyncService.fullSync();
        return Result.success();
    }

    /**
     * 创建索引（管理员接口）
     */
    @PostMapping("/index/create")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public Result<Void> createIndex() {
        searchSyncService.createOrUpdateIndex();
        return Result.success();
    }

    /**
     * 跑腿搜索
     */
    @GetMapping("/errand")
    public Result<PageResult<ErrandSearchResultVO>> searchErrands(@Valid ErrandSearchRequestDTO request) {
        String schoolCode = UserContextHolder.getSchoolCode();
        if (request.getSchoolCode() == null && schoolCode != null && UserContextHolder.isAuthenticated()) {
            request.setSchoolCode(schoolCode);
        }
        PageResult<ErrandSearchResultVO> result = errandSearchService.search(request);
        return Result.success(result);
    }

    /**
     * 跑腿全量同步（管理员接口）
     */
    @PostMapping("/errand/sync/full")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public Result<Void> fullSyncErrands() {
        errandSearchService.fullSyncErrands();
        return Result.success();
    }

    /**
     * 创建跑腿索引（管理员接口）
     */
    @PostMapping("/errand/index/create")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public Result<Void> createErrandIndex() {
        errandSearchService.createOrUpdateErrandIndex();
        return Result.success();
    }
}
