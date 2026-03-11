package com.unimarket.module.risk.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.risk.dto.RiskAuditMessage;
import com.unimarket.module.risk.dto.RiskContext;
import com.unimarket.module.risk.entity.RiskBehaviorControl;
import com.unimarket.module.risk.entity.RiskBlacklist;
import com.unimarket.module.risk.entity.RiskCase;
import com.unimarket.module.risk.entity.RiskDecision;
import com.unimarket.module.risk.entity.RiskEvent;
import com.unimarket.module.risk.entity.RiskRule;
import com.unimarket.module.risk.enums.RiskAction;
import com.unimarket.module.risk.enums.RiskLevel;
import com.unimarket.module.risk.enums.RiskMode;
import com.unimarket.module.risk.service.RiskAuditPublisher;
import com.unimarket.module.risk.service.RiskBehaviorControlService;
import com.unimarket.module.risk.service.RiskControlService;
import com.unimarket.module.risk.service.RiskIdGenerator;
import com.unimarket.module.risk.service.RiskModeService;
import com.unimarket.module.risk.service.RiskPolicyCacheService;
import com.unimarket.module.risk.service.RiskRealtimeStore;
import com.unimarket.module.risk.vo.RiskDecisionResult;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 风控服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskControlServiceImpl implements RiskControlService {

    private static final String SUBJECT_USER = "USER";
    private static final String COUNTER_EVENT_COUNT = "EVENT_COUNT";
    private static final String COUNTER_LOGIN_FAILURE = "LOGIN_FAILURE_COUNT";
    private static final String COUNTER_DEVICE_SUBJECT = "DEVICE_SUBJECT_COUNT";

    private final RiskPolicyCacheService riskPolicyCacheService;
    private final RiskBehaviorControlService riskBehaviorControlService;
    private final RiskModeService riskModeService;
    private final RiskRealtimeStore riskRealtimeStore;
    private final RiskAuditPublisher riskAuditPublisher;
    private final RiskIdGenerator riskIdGenerator;

    @Override
    public RiskDecisionResult evaluate(RiskContext context) {
        long startNs = System.nanoTime();
        RiskContext normalized = normalizeContext(context);
        RiskMode mode = riskModeService.getMode();

        if (mode == RiskMode.OFF) {
            recordMetrics(normalized.getEventType(), RiskAction.ALLOW, RiskLevel.LOW, startNs);
            return RiskDecisionResult.builder()
                    .action(RiskAction.ALLOW)
                    .riskLevel(RiskLevel.LOW.getCode())
                    .riskScore(0D)
                    .reason("风控已关闭")
                    .build();
        }

        LocalDateTime eventTime = LocalDateTime.now();
        RiskEvent event = buildRiskEvent(normalized, eventTime);
        if (mode == RiskMode.FULL) {
            riskRealtimeStore.recordEvaluation(normalized, event.getTraceId(), eventTime);
        }

        DecisionDraft decisionDraft = resolveDecision(normalized, mode);
        RiskDecision decision = buildRiskDecision(event.getEventId(), decisionDraft);
        RiskCase riskCase = decisionDraft.action == RiskAction.REVIEW ? buildRiskCase(event, decision) : null;
        riskAuditPublisher.publish(RiskAuditMessage.builder()
                .event(event)
                .decision(decision)
                .riskCase(riskCase)
                .build());

        recordMetrics(normalized.getEventType(), decisionDraft.action, decisionDraft.riskLevel, startNs);
        return RiskDecisionResult.builder()
                .eventId(event.getEventId())
                .decisionId(decision.getDecisionId())
                .action(decisionDraft.action)
                .riskLevel(decisionDraft.riskLevel.getCode())
                .riskScore(decisionDraft.riskScore)
                .reason(decisionDraft.reason)
                .build();
    }

    @Override
    public void assertAllowed(RiskContext context) {
        RiskDecisionResult result = evaluate(context);
        if (result.getAction() == RiskAction.ALLOW) {
            return;
        }

        String reason = result.getReason();
        if (reason == null || reason.isBlank()) {
            reason = "当前行为触发平台风控策略，已被限制";
        }

        if (result.getAction() == RiskAction.CHALLENGE) {
            throw new BusinessException("触发安全校验，请稍后重试");
        }
        if (result.getAction() == RiskAction.REVIEW) {
            throw new BusinessException("当前行为已进入人工复核，请稍后再试");
        }
        if (result.getAction() == RiskAction.LIMIT) {
            throw new BusinessException("操作过于频繁，请稍后再试");
        }
        throw new BusinessException(reason);
    }

    private void recordMetrics(String eventType, RiskAction action, RiskLevel level, long startNs) {
        Metrics.counter("unimarket.risk.decision.total",
                "eventType", eventType,
                "action", action.name(),
                "riskLevel", level.getCode()).increment();
        Metrics.timer("unimarket.risk.evaluate.duration", "eventType", eventType)
                .record(System.nanoTime() - startNs, java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    private RiskContext normalizeContext(RiskContext context) {
        if (context == null || context.getEventType() == null || context.getEventType().isBlank()) {
            throw new IllegalArgumentException("risk eventType must not be blank");
        }

        String subjectType = context.getSubjectType();
        if (subjectType == null || subjectType.isBlank()) {
            subjectType = SUBJECT_USER;
        }

        String subjectId = context.getSubjectId();
        if (subjectId == null || subjectId.isBlank()) {
            if (context.getUserId() != null) {
                subjectId = String.valueOf(context.getUserId());
            } else if (context.getRequestIp() != null && !context.getRequestIp().isBlank()) {
                subjectId = context.getRequestIp();
                subjectType = "IP";
            } else {
                subjectId = "UNKNOWN";
            }
        }

        Map<String, Object> normalizedFeatures = new HashMap<>();
        if (context.getFeatures() != null) {
            normalizedFeatures.putAll(context.getFeatures());
        }
        if (context.getRequestIp() != null && !context.getRequestIp().isBlank()) {
            normalizedFeatures.putIfAbsent("requestIp", context.getRequestIp());
        }
        if (context.getDeviceId() != null && !context.getDeviceId().isBlank()) {
            normalizedFeatures.putIfAbsent("deviceId", context.getDeviceId());
        }

        String deviceFingerprint = buildDeviceFingerprint(context, normalizedFeatures);
        if (deviceFingerprint != null && !deviceFingerprint.isBlank()) {
            normalizedFeatures.put("deviceFingerprint", deviceFingerprint);
        }

        Map<String, Object> normalizedPayload = new HashMap<>();
        if (context.getRawPayload() != null) {
            normalizedPayload.putAll(context.getRawPayload());
        }
        normalizedPayload.putIfAbsent("requestIp", context.getRequestIp());
        normalizedPayload.putIfAbsent("deviceId", context.getDeviceId());
        normalizedPayload.putIfAbsent("deviceFingerprint", deviceFingerprint);

        return RiskContext.builder()
                .eventType(context.getEventType().trim().toUpperCase())
                .subjectType(subjectType.trim().toUpperCase())
                .subjectId(subjectId)
                .userId(context.getUserId())
                .schoolCode(context.getSchoolCode())
                .campusCode(context.getCampusCode())
                .requestIp(context.getRequestIp())
                .deviceId(context.getDeviceId())
                .features(normalizedFeatures)
                .rawPayload(normalizedPayload)
                .build();
    }

    private RiskEvent buildRiskEvent(RiskContext context, LocalDateTime eventTime) {
        RiskEvent event = new RiskEvent();
        event.setEventId(riskIdGenerator.nextId());
        event.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        event.setEventType(context.getEventType());
        event.setSubjectType(context.getSubjectType());
        event.setSubjectId(context.getSubjectId());
        event.setSchoolCode(context.getSchoolCode());
        event.setCampusCode(context.getCampusCode());
        event.setRiskFeatures(JSON.toJSONString(context.getFeatures()));
        event.setRawPayload(JSON.toJSONString(context.getRawPayload()));
        event.setEventTime(eventTime);
        return event;
    }

    private RiskDecision buildRiskDecision(Long eventId, DecisionDraft decisionDraft) {
        RiskDecision decision = new RiskDecision();
        decision.setDecisionId(riskIdGenerator.nextId());
        decision.setEventId(eventId);
        decision.setDecisionAction(decisionDraft.action.name());
        decision.setRiskLevel(decisionDraft.riskLevel.getCode());
        decision.setRiskScore(BigDecimal.valueOf(decisionDraft.riskScore));
        decision.setMatchedRuleCodes(JSON.toJSONString(decisionDraft.matchedRuleCodes));
        decision.setDecisionReason(decisionDraft.reason);
        return decision;
    }

    private RiskCase buildRiskCase(RiskEvent event, RiskDecision decision) {
        RiskCase riskCase = new RiskCase();
        riskCase.setCaseId(riskIdGenerator.nextId());
        riskCase.setEventId(event.getEventId());
        riskCase.setDecisionId(decision.getDecisionId());
        riskCase.setSchoolCode(event.getSchoolCode());
        riskCase.setCampusCode(event.getCampusCode());
        riskCase.setCaseStatus("OPEN");
        return riskCase;
    }

    private String buildDeviceFingerprint(RiskContext context, Map<String, Object> features) {
        Object featureFingerprint = features.get("deviceFingerprint");
        if (featureFingerprint instanceof String str && !str.isBlank()) {
            return str;
        }

        if (context.getDeviceId() != null && !context.getDeviceId().isBlank()) {
            return context.getDeviceId().trim();
        }

        Object uaObj = context.getRawPayload() == null ? null : context.getRawPayload().get("userAgent");
        String userAgent = uaObj == null ? "" : String.valueOf(uaObj);
        if ((context.getRequestIp() == null || context.getRequestIp().isBlank()) && userAgent.isBlank()) {
            return null;
        }
        int hash = Objects.hash(context.getRequestIp(), userAgent);
        return Integer.toHexString(hash);
    }

    private DecisionDraft resolveDecision(RiskContext context, RiskMode mode) {
        DecisionDraft behaviorControlDecision = matchBehaviorControl(context);
        if (behaviorControlDecision != null) {
            return behaviorControlDecision;
        }

        if (riskPolicyCacheService.isWhitelisted(context.getSubjectType(), context.getSubjectId())) {
            return DecisionDraft.allow("命中风控白名单");
        }

        RiskBlacklist blacklist = riskPolicyCacheService.getActiveBlacklist(context.getSubjectType(), context.getSubjectId());
        if (blacklist != null) {
            return DecisionDraft.reject("命中风控黑名单: " + safeReason(blacklist.getReason()));
        }

        if (mode == RiskMode.BASIC) {
            return DecisionDraft.allow("风控基础模式：仅行为管控与黑白名单生效");
        }

        List<RiskRule> rules = riskPolicyCacheService.getEnabledRules(context.getEventType());
        for (RiskRule rule : rules) {
            if (!ruleMatched(rule, context)) {
                continue;
            }
            RiskAction action = RiskAction.from(rule.getDecisionAction());
            RiskLevel level = levelByAction(action);
            List<String> matchedRuleCodes = new ArrayList<>();
            matchedRuleCodes.add(rule.getRuleCode());
            String reason = "命中规则[" + rule.getRuleCode() + "] " + safeReason(rule.getRuleName());
            double score = scoreByAction(action);
            return new DecisionDraft(action, level, score, reason, matchedRuleCodes);
        }

        return DecisionDraft.allow("未命中风控规则");
    }

    private DecisionDraft matchBehaviorControl(RiskContext context) {
        if (context.getUserId() == null) {
            return null;
        }

        RiskBehaviorControl matched = riskBehaviorControlService.findMatchedControl(context.getUserId(), context.getEventType());
        if (matched == null) {
            return null;
        }

        RiskAction action = RiskAction.from(matched.getControlAction());
        RiskLevel level = levelByAction(action);
        String reason = "用户行为受控: " + safeReason(matched.getReason());
        if (action == RiskAction.ALLOW) {
            return new DecisionDraft(RiskAction.ALLOW, RiskLevel.LOW, 0D, reason, Collections.emptyList());
        }
        return new DecisionDraft(action, level, scoreByAction(action), reason, Collections.emptyList());
    }

    private boolean ruleMatched(RiskRule rule, RiskContext context) {
        if (rule == null || rule.getRuleType() == null) {
            return false;
        }
        JSONObject cfg = parseConfig(rule.getRuleConfig());
        String ruleType = rule.getRuleType().trim().toUpperCase();
        return switch (ruleType) {
            case "THRESHOLD" -> thresholdMatched(cfg, context);
            case "KEYWORD" -> keywordMatched(cfg, context);
            default -> false;
        };
    }

    private boolean thresholdMatched(JSONObject cfg, RiskContext context) {
        int windowMinutes = cfg.getIntValue("windowMinutes", 10);
        int maxCount = cfg.getIntValue("maxCount", 20);
        String subjectType = cfg.getString("subjectType");
        if (subjectType == null || subjectType.isBlank()) {
            subjectType = context.getSubjectType();
        } else {
            subjectType = subjectType.trim().toUpperCase();
        }

        String subjectId = resolveThresholdSubjectId(subjectType, context);
        if (subjectId == null || subjectId.isBlank()) {
            return false;
        }

        long total = resolveThresholdCount(cfg.getString("counterType"), context, subjectType, subjectId, windowMinutes);
        return total >= Math.max(maxCount, 1);
    }

    private long resolveThresholdCount(String counterType,
                                       RiskContext context,
                                       String subjectType,
                                       String subjectId,
                                       int windowMinutes) {
        String normalizedCounterType = counterType == null || counterType.isBlank()
                ? COUNTER_EVENT_COUNT
                : counterType.trim().toUpperCase();
        return switch (normalizedCounterType) {
            case COUNTER_EVENT_COUNT -> riskRealtimeStore.countEvents(context.getEventType(), subjectType, subjectId, windowMinutes);
            case COUNTER_LOGIN_FAILURE -> riskRealtimeStore.countLoginFailures(subjectId, windowMinutes);
            case COUNTER_DEVICE_SUBJECT -> riskRealtimeStore.countDeviceSubjects(subjectId, windowMinutes);
            default -> {
                log.warn("未知风控计数类型: {}", normalizedCounterType);
                yield 0L;
            }
        };
    }

    private String resolveThresholdSubjectId(String subjectType, RiskContext context) {
        if (Objects.equals(subjectType, context.getSubjectType())) {
            return context.getSubjectId();
        }
        if ("IP".equalsIgnoreCase(subjectType)) {
            return context.getRequestIp();
        }
        if ("DEVICE".equalsIgnoreCase(subjectType)) {
            Object fingerprint = context.getFeatures().get("deviceFingerprint");
            if (fingerprint != null && !String.valueOf(fingerprint).isBlank()) {
                return String.valueOf(fingerprint);
            }
            return context.getDeviceId();
        }
        return context.getSubjectId();
    }

    private boolean keywordMatched(JSONObject cfg, RiskContext context) {
        String field = cfg.getString("field");
        if (field == null || field.isBlank()) {
            field = "content";
        }
        Object raw = context.getRawPayload().get(field);
        if (raw == null) {
            return false;
        }
        String text = String.valueOf(raw).toLowerCase();
        if (text.isBlank()) {
            return false;
        }

        JSONArray keywords = cfg.getJSONArray("keywords");
        if (keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (int i = 0; i < keywords.size(); i++) {
            String keyword = keywords.getString(i);
            if (keyword != null && !keyword.isBlank() && text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private JSONObject parseConfig(String ruleConfig) {
        if (ruleConfig == null || ruleConfig.isBlank()) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(ruleConfig);
        } catch (Exception e) {
            log.warn("解析风控规则配置失败: {}", e.getMessage());
            return new JSONObject();
        }
    }

    private RiskLevel levelByAction(RiskAction action) {
        if (action == null) {
            return RiskLevel.LOW;
        }
        return switch (action) {
            case REJECT -> RiskLevel.HIGH;
            case CHALLENGE, REVIEW, LIMIT -> RiskLevel.MEDIUM;
            case ALLOW -> RiskLevel.LOW;
        };
    }

    private double scoreByAction(RiskAction action) {
        if (action == null) {
            return 0D;
        }
        return switch (action) {
            case ALLOW -> 0D;
            case LIMIT -> 55D;
            case CHALLENGE -> 70D;
            case REVIEW -> 80D;
            case REJECT -> 95D;
        };
    }

    private String safeReason(String raw) {
        if (raw == null || raw.isBlank()) {
            return "无";
        }
        return raw;
    }

    private static class DecisionDraft {
        private final RiskAction action;
        private final RiskLevel riskLevel;
        private final Double riskScore;
        private final String reason;
        private final List<String> matchedRuleCodes;

        private DecisionDraft(RiskAction action,
                              RiskLevel riskLevel,
                              Double riskScore,
                              String reason,
                              List<String> matchedRuleCodes) {
            this.action = action;
            this.riskLevel = riskLevel;
            this.riskScore = riskScore;
            this.reason = reason;
            this.matchedRuleCodes = matchedRuleCodes;
        }

        private static DecisionDraft allow(String reason) {
            return new DecisionDraft(RiskAction.ALLOW, RiskLevel.LOW, 0D, reason, Collections.emptyList());
        }

        private static DecisionDraft reject(String reason) {
            return new DecisionDraft(RiskAction.REJECT, RiskLevel.HIGH, 95D, reason, Collections.emptyList());
        }
    }
}
