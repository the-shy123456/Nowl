package com.unimarket.module.ai.controller;

import com.unimarket.ai.dto.GoodsPriceEstimateDTO;
import com.unimarket.ai.vo.AiChatMessageVO;
import com.unimarket.ai.vo.AiChatResponseVO;
import com.unimarket.ai.vo.GoodsPriceEstimateVO;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.common.result.Result;
import com.unimarket.module.ai.dto.AiChatRequestDTO;
import com.unimarket.module.aiassistant.model.AiChatQueryContext;
import com.unimarket.module.aiassistant.service.AiAssistantService;
import com.unimarket.module.aiassistant.service.AiGoodsCapabilityService;
import com.unimarket.security.UserContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 小Q对话的Controller
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Validated
public class AiController {

    private final AiAssistantService aiAssistantService;
    private final AiGoodsCapabilityService aiGoodsCapabilityService;

    /**
     * 与小Q对话
     */
    @PostMapping("/chat")
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<AiChatResponseVO> chat(@RequestBody(required = false) @Valid AiChatRequestDTO params) {
        Long userId = UserContextHolder.getUserId();
        String message = params == null ? null : trimToNull(params.getMessage());
        String imageUrl = params == null ? null : trimToNull(params.getImageUrl());
        AiChatQueryContext queryContext = params == null ? null : params.getQueryContext();
        if (message == null && imageUrl == null) {
            throw new BusinessException("消息和图片不能同时为空");
        }
        AiChatResponseVO response = aiAssistantService.chat(
                userId,
                UserContextHolder.getSchoolCode(),
                UserContextHolder.getCampusCode(),
                message,
                imageUrl,
                queryContext
        );
        return Result.success(response);
    }

    /**
     * 获取AI聊天历史记录
     */
    @GetMapping("/history")
    public Result<List<AiChatMessageVO>> getHistory() {
        Long userId = UserContextHolder.getUserId();
        List<AiChatMessageVO> history = aiAssistantService.getHistory(userId, 100);
        return Result.success(history);
    }

    /**
     * 清除AI聊天历史记录
     */
    @DeleteMapping("/history")
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<Void> clearHistory() {
        Long userId = UserContextHolder.getUserId();
        aiAssistantService.clearHistory(userId);
        return Result.success();
    }

    /**
     * 商品发布估价接口
     */
    @PostMapping("/price-estimate")
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<GoodsPriceEstimateVO> estimatePrice(@RequestBody @Valid GoodsPriceEstimateDTO dto) {
        GoodsPriceEstimateVO result = aiGoodsCapabilityService.estimatePriceForPublish(dto);
        return Result.success(result);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
