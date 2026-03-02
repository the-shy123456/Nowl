package com.unimarket.module.notice.controller;

import com.unimarket.common.result.Result;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.notice.vo.NoticeVO;
import com.unimarket.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/list")
    public Result<List<NoticeVO>> getMyNotices() {
        Long userId = UserContextHolder.getUserId();
        return Result.success(noticeService.getMyNotices(userId));
    }

    @PostMapping("/read/{noticeId}")
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<Void> markAsRead(@PathVariable Long noticeId) {
        Long userId = UserContextHolder.getUserId();
        noticeService.markAsRead(userId, noticeId);
        return Result.success();
    }

    @PostMapping("/read/all")
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<Void> markAllAsRead() {
        Long userId = UserContextHolder.getUserId();
        noticeService.markAllAsRead(userId);
        return Result.success();
    }

    @GetMapping("/unread/count")
    public Result<Long> getUnreadCount() {
        Long userId = UserContextHolder.getUserId();
        return Result.success(noticeService.getUnreadCount(userId));
    }
}
