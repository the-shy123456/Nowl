package com.unimarket.recommend.controller;

import com.unimarket.common.result.PageResult;
import com.unimarket.common.result.Result;
import com.unimarket.recommend.service.BehaviorCollectService;
import com.unimarket.recommend.service.RecommendService;
import com.unimarket.recommend.vo.RecommendItemVO;
import com.unimarket.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推荐Controller
 */
@Slf4j
@RestController
@RequestMapping("/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;
    private final BehaviorCollectService behaviorCollectService;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_RECOMMEND_SIZE = 50;

    /**
     * 首页推荐（猜你喜欢）
     */
    @GetMapping("/home")
    public Result<PageResult<RecommendItemVO>> homeRecommend(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String schoolCode,
            @RequestParam(required = false) String campusCode) {

        int safePageNum = clamp(pageNum, 1, Integer.MAX_VALUE);
        int safePageSize = clamp(pageSize, 1, MAX_PAGE_SIZE);
        Long userId = UserContextHolder.getUserId();
        PageResult<RecommendItemVO> result = recommendService.homeRecommend(userId, safePageNum, safePageSize, schoolCode, campusCode);
        return Result.success(result);
    }

    /**
     * 热门推荐
     */
    @GetMapping("/hot")
    public Result<List<RecommendItemVO>> hotRecommend(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String schoolCode,
            @RequestParam(required = false) String campusCode) {

        int safeSize = clamp(size, 1, MAX_RECOMMEND_SIZE);
        Long userId = UserContextHolder.getUserId();
        List<RecommendItemVO> result = recommendService.hotRecommend(userId, categoryId, safeSize, schoolCode, campusCode);
        return Result.success(result);
    }

    /**
     * 相似商品推荐（商品详情页）
     */
    @GetMapping("/similar/{productId}")
    public Result<List<RecommendItemVO>> similarRecommend(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String schoolCode,
            @RequestParam(required = false) String campusCode) {

        int safeSize = clamp(size, 1, MAX_RECOMMEND_SIZE);
        Long userId = UserContextHolder.getUserId();
        List<RecommendItemVO> result = recommendService.similarRecommend(userId, productId, safeSize, schoolCode, campusCode);
        return Result.success(result);
    }

    /**
     * 关注的人在卖
     */
    @GetMapping("/following")
    public Result<PageResult<RecommendItemVO>> followingRecommend(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String schoolCode,
            @RequestParam(required = false) String campusCode) {

        int safePageNum = clamp(pageNum, 1, Integer.MAX_VALUE);
        int safePageSize = clamp(pageSize, 1, MAX_PAGE_SIZE);
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.success(new PageResult<>(0L, List.of()));
        }
        PageResult<RecommendItemVO> result = recommendService.followingRecommend(userId, safePageNum, safePageSize, schoolCode, campusCode);
        return Result.success(result);
    }

    /**
     * 记录商品浏览行为
     */
    @PostMapping("/behavior/view")
    public Result<Void> recordView(
            @RequestParam Long productId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer duration) {

        Long userId = UserContextHolder.getUserId();
        behaviorCollectService.recordView(userId, productId, categoryId, duration);
        return Result.success();
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }
}
