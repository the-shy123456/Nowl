package com.unimarket.module.aiassistant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.unimarket.ai.service.AiChatService;
import com.unimarket.ai.vo.AiChatMessageVO;
import com.unimarket.ai.vo.AiChatResponseVO;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.common.result.ResultCode;
import com.unimarket.module.aiassistant.model.AiChatQueryContext;
import com.unimarket.module.aiassistant.service.AiAssistantService;
import com.unimarket.module.aiassistant.service.AiChatHistoryService;
import com.unimarket.module.aiassistant.service.AiGoodsQueryService;
import com.unimarket.module.aiassistant.util.AiChatContentCodec;
import com.unimarket.module.risk.dto.RiskContext;
import com.unimarket.module.risk.enums.RiskAction;
import com.unimarket.module.risk.enums.RiskEventType;
import com.unimarket.module.risk.service.RiskControlService;
import com.unimarket.module.risk.vo.RiskDecisionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI助手业务编排服务实现：
 * - chat：风控校验 + 历史记录 + AI 编排（函数调用/规则兜底）
 * - getHistory/clearHistory：历史查询与清理
 */
@Slf4j
@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private static final int HISTORY_CONTEXT_LIMIT = 100;
    private static final int DEFAULT_HISTORY_LIMIT = 100;

    private final AiChatService aiChatService;
    private final AiChatHistoryService aiChatHistoryService;
    private final RiskControlService riskControlService;

    private final AiAssistantGoodsQueryEngine goodsQueryEngine;
    private final AiAssistantFunctionCallingEngine functionCallingEngine;

    public AiAssistantServiceImpl(AiChatService aiChatService,
                                  AiChatHistoryService aiChatHistoryService,
                                  AiGoodsQueryService aiGoodsQueryService,
                                  RiskControlService riskControlService) {
        this.aiChatService = aiChatService;
        this.aiChatHistoryService = aiChatHistoryService;
        this.riskControlService = riskControlService;
        this.goodsQueryEngine = new AiAssistantGoodsQueryEngine(aiGoodsQueryService);
        this.functionCallingEngine = new AiAssistantFunctionCallingEngine(aiChatService, aiChatHistoryService, goodsQueryEngine);
    }

    @Override
    public AiChatResponseVO chat(
            Long userId,
            String schoolCode,
            String campusCode,
            String message,
            String imageUrl,
            AiChatQueryContext queryContext
    ) {
        if (StrUtil.isAllBlank(message, imageUrl)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "消息不能为空");
        }

        String normalizedMessage = StrUtil.trimToEmpty(message);
        boolean switchBatchRequest = AiAssistantTextSupport.isSwitchBatchRequest(normalizedMessage, queryContext);
        QueryIntent intent = switchBatchRequest
                ? QueryIntent.RECOMMEND
                : AiAssistantTextSupport.resolveIntent(normalizedMessage, imageUrl);
        QueryConstraints constraints = AiAssistantQuerySupport.resolveQueryConstraints(
                normalizedMessage,
                intent,
                queryContext,
                switchBatchRequest
        );

        Map<String, Object> features = new HashMap<>();
        features.put("hasImage", StrUtil.isNotBlank(imageUrl));
        features.put("messageLength", normalizedMessage.length());
        features.put("intent", intent.name());
        if (StrUtil.isNotBlank(constraints.keyword)) {
            features.put("keyword", constraints.keyword);
        }
        if (constraints.maxPrice != null) {
            features.put("maxPrice", constraints.maxPrice.toPlainString());
        }
        features.put("queryLimit", constraints.limit);
        features.put("queryPage", constraints.page);
        features.put("switchBatch", switchBatchRequest);

        Map<String, Object> payload = new HashMap<>();
        payload.put("content", normalizedMessage);
        payload.put("imageUrl", imageUrl);
        payload.put("userId", userId);
        payload.put("queryContext", queryContext);

        RiskDecisionResult decision = riskControlService.evaluate(RiskContext.builder()
                .eventType(RiskEventType.AI_CHAT_SEND)
                .userId(userId)
                .subjectId(String.valueOf(userId))
                .schoolCode(schoolCode)
                .campusCode(campusCode)
                .features(features)
                .rawPayload(payload)
                .build());

        if (decision.getAction() != RiskAction.ALLOW) {
            throw new BusinessException("当前 Nowl AI 对话行为触发风控策略，请稍后再试");
        }

        aiChatHistoryService.saveUserMessage(userId, message, imageUrl, decision.getRiskLevel());

        if (!switchBatchRequest && AiAssistantTextSupport.isPricingOrAuditRequest(normalizedMessage)) {
            AiChatResponseVO response = buildPricingOrAuditUnavailableResponse();
            aiChatHistoryService.saveModelMessage(userId, AiChatContentCodec.encodeModelContent(response));
            return response;
        }

        AiChatResponseVO response;
        if (StrUtil.isNotBlank(imageUrl)) {
            response = callGeneralChat(userId, normalizedMessage, imageUrl);
        } else {
            response = functionCallingEngine.callFunctionCallingChat(
                    userId,
                    schoolCode,
                    campusCode,
                    normalizedMessage,
                    intent,
                    constraints
            );
            if (response == null) {
                response = intent == QueryIntent.GENERAL
                        ? callGeneralChat(userId, normalizedMessage, imageUrl)
                        : goodsQueryEngine.handleGoodsQuery(intent, schoolCode, campusCode, constraints);
            }
        }

        String replyText = StrUtil.blankToDefault(response.getReplyText(), "我先帮你记下了，你可以继续补充细节。");
        response.setReplyText(replyText);
        aiChatHistoryService.saveModelMessage(userId, AiChatContentCodec.encodeModelContent(response));
        return response;
    }

    @Override
    public List<AiChatMessageVO> getHistory(Long userId, int limit) {
        int resolvedLimit = limit > 0 ? limit : DEFAULT_HISTORY_LIMIT;
        return aiChatHistoryService.getHistory(userId, resolvedLimit);
    }

    @Override
    public void clearHistory(Long userId) {
        aiChatHistoryService.clearHistory(userId);
    }

    private AiChatResponseVO callGeneralChat(Long userId, String message, String imageUrl) {
        String historyContext = aiChatHistoryService.getRecentContext(userId, HISTORY_CONTEXT_LIMIT);
        String replyText = aiChatService.chat(StrUtil.blankToDefault(message, "请看这张图"), imageUrl, historyContext);

        AiChatResponseVO response = new AiChatResponseVO();
        response.setReplyText(replyText);
        response.setCards(Collections.emptyList());
        response.setIntent(QueryIntent.GENERAL.toCode());
        response.setKeyword(null);
        return response;
    }

    private AiChatResponseVO buildPricingOrAuditUnavailableResponse() {
        AiChatResponseVO response = new AiChatResponseVO();
        response.setReplyText("Nowl AI 聊天暂不提供估价或审核。请在发布商品页面使用对应能力。");
        response.setCards(Collections.emptyList());
        response.setIntent(QueryIntent.GENERAL.toCode());
        response.setKeyword(null);
        return response;
    }
}

