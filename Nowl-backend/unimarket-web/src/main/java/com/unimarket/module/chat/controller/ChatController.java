package com.unimarket.module.chat.controller;

import com.unimarket.common.result.Result;
import com.unimarket.module.chat.dto.ChatMessageDTO;
import com.unimarket.module.chat.service.ChatService;
import com.unimarket.module.chat.vo.ChatBlockItemVO;
import com.unimarket.module.chat.vo.ChatMessageVO;
import com.unimarket.module.chat.vo.ContactVO;
import com.unimarket.security.UserContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/send")
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<Void> sendMessage(@Valid @RequestBody ChatMessageDTO dto) {
        Long userId = UserContextHolder.getUserId();
        chatService.sendMessage(userId, dto);
        return Result.success();
    }

    @GetMapping("/contacts")
    public Result<List<ContactVO>> getContacts() {
        Long userId = UserContextHolder.getUserId();
        return Result.success(chatService.getRecentContacts(userId));
    }

    @GetMapping("/history/{contactId}")
    public Result<List<ChatMessageVO>> getHistory(@PathVariable Long contactId) {
        Long userId = UserContextHolder.getUserId();
        return Result.success(chatService.getHistory(userId, contactId));
    }

    @PostMapping("/read/{contactId}")
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<Void> markAsRead(@PathVariable Long contactId) {
        Long userId = UserContextHolder.getUserId();
        chatService.markAsRead(userId, contactId);
        return Result.success();
    }

    @PostMapping("/read/all")
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<Void> markAllAsRead() {
        Long userId = UserContextHolder.getUserId();
        chatService.markAllAsRead(userId);
        return Result.success();
    }

    @PostMapping("/block/{targetUserId}")
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<Void> blockUser(@PathVariable Long targetUserId) {
        Long userId = UserContextHolder.getUserId();
        chatService.blockUser(userId, targetUserId);
        return Result.success();
    }

    @DeleteMapping("/block/{targetUserId}")
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<Void> unblockUser(@PathVariable Long targetUserId) {
        Long userId = UserContextHolder.getUserId();
        chatService.unblockUser(userId, targetUserId);
        return Result.success();
    }

    @GetMapping("/blocks")
    public Result<List<ChatBlockItemVO>> getBlockList(
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        Long userId = UserContextHolder.getUserId();
        return Result.success(chatService.getBlockList(userId, keyword));
    }

    @GetMapping("/block/relation/{targetUserId}")
    public Result<Integer> getBlockRelation(@PathVariable Long targetUserId) {
        Long userId = UserContextHolder.getUserId();
        return Result.success(chatService.getBlockRelation(userId, targetUserId));
    }
}
