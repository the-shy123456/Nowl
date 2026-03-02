package com.unimarket.module.review.controller;

import com.unimarket.common.result.PageResult;
import com.unimarket.common.result.Result;
import com.unimarket.module.review.dto.ReviewCreateDTO;
import com.unimarket.module.review.service.ReviewService;
import com.unimarket.module.review.vo.ReviewListItemVO;
import com.unimarket.module.review.vo.UserReviewStatsVO;
import com.unimarket.security.UserContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 评价Controller
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 创建评价
     */
    @PostMapping
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<Void> create(@Valid @RequestBody ReviewCreateDTO dto) {
        Long userId = UserContextHolder.getUserId();
        reviewService.createReview(userId, dto);
        return Result.success();
    }

    /**
     * 获取用户收到的评价列表
     */
    @GetMapping("/received/{userId}")
    public Result<PageResult<ReviewListItemVO>> getReceivedReviews(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {
        PageResult<ReviewListItemVO> result = reviewService.getReceivedReviews(userId, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取我发出的评价列表
     */
    @GetMapping("/sent")
    public Result<PageResult<ReviewListItemVO>> getSentReviews(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {
        Long userId = UserContextHolder.getUserId();
        PageResult<ReviewListItemVO> result = reviewService.getSentReviews(userId, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取用户评价统计信息
     */
    @GetMapping("/stats/{userId}")
    public Result<UserReviewStatsVO> getUserStats(@PathVariable Long userId) {
        UserReviewStatsVO stats = reviewService.getUserReviewStats(userId);
        return Result.success(stats);
    }

    /**
     * 检查是否可以评价
     */
    @GetMapping("/can-review")
    public Result<Boolean> canReview(
            @RequestParam Integer targetType,
            @RequestParam Long contentId,
            @RequestParam Long reviewedId) {
        Long userId = UserContextHolder.getUserId();
        boolean canReview = reviewService.canReview(userId, targetType, contentId, reviewedId);
        return Result.success(canReview);
    }

    /**
     * 检查是否已评价
     */
    @GetMapping("/has-reviewed")
    public Result<Boolean> hasReviewed(
            @RequestParam Integer targetType,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long taskId) {
        Long userId = UserContextHolder.getUserId();
        boolean hasReviewed = reviewService.hasReviewed(orderId, taskId, targetType, userId);
        return Result.success(hasReviewed);
    }
}
