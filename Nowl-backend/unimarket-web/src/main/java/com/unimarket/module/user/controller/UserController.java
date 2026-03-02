package com.unimarket.module.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.common.result.Result;
import com.unimarket.module.user.dto.UserUpdateDTO;
import com.unimarket.module.user.service.UserService;
import com.unimarket.module.user.vo.FollowUserVO;
import com.unimarket.module.user.vo.UserInfoVO;
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
 * 用户Controller
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<UserInfoVO> getCurrentUserInfo() {
        Long userId = UserContextHolder.getUserId();
        UserInfoVO userInfo = userService.getCurrentUserInfo(userId);
        return Result.success(userInfo);
    }

    /**
     * 根据用户ID获取用户信息
     */
    @GetMapping("/{userId}")
    public Result<UserInfoVO> getUserInfo(@PathVariable Long userId) {
        UserInfoVO userInfo = userService.getPublicUserInfo(userId);
        return Result.success(userInfo);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<Void> updateUserInfo(@Valid @RequestBody UserUpdateDTO dto) {
        Long userId = UserContextHolder.getUserId();
        userService.updateUserInfo(userId, dto);
        return Result.success();
    }

    /**
     * 申请成为跑腿员（需已认证）
     */
    @PostMapping("/runnable/apply")
    @PreAuthorize("@bizAuth.canApplyRunner(authentication.principal.userId)")
    public Result<Void> applyRunner() {
        Long userId = UserContextHolder.getUserId();
        userService.applyRunner(userId);
        return Result.success();
    }

    /**
     * 关注用户
     */
    @PostMapping("/follow/{userId}")
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<Void> followUser(@PathVariable("userId") Long targetUserId) {
        Long userId = UserContextHolder.getUserId();
        userService.followUser(userId, targetUserId);
        return Result.success();
    }

    /**
     * 取消关注
     */
    @DeleteMapping("/follow/{userId}")
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<Void> unfollowUser(@PathVariable("userId") Long targetUserId) {
        Long userId = UserContextHolder.getUserId();
        userService.unfollowUser(userId, targetUserId);
        return Result.success();
    }

    /**
     * 检查是否关注了某用户
     */
    @GetMapping("/follow/check/{userId}")
    public Result<Boolean> isFollowing(@PathVariable("userId") Long targetUserId) {
        Long userId = UserContextHolder.getUserId();
        boolean result = userService.isFollowing(userId, targetUserId);
        return Result.success(result);
    }

    /**
     * 获取某用户的关注列表
     */
    @GetMapping("/{userId}/following")
    public Result<Page<FollowUserVO>> getFollowingList(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        Long currentUserId = UserContextHolder.getUserId();
        Page<FollowUserVO> page = userService.getFollowingList(userId, currentUserId, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 获取某用户的粉丝列表
     */
    @GetMapping("/{userId}/followers")
    public Result<Page<FollowUserVO>> getFollowerList(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        Long currentUserId = UserContextHolder.getUserId();
        Page<FollowUserVO> page = userService.getFollowerList(userId, currentUserId, pageNum, pageSize);
        return Result.success(page);
    }
}
