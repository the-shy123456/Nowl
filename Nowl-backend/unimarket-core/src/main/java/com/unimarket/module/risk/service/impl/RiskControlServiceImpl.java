package com.unimarket.module.risk.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.audit.entity.AuditLoginTrace;
import com.unimarket.module.audit.mapper.AuditLoginTraceMapper;
import com.unimarket.module.risk.dto.RiskContext;
import com.unimarket.module.risk.entity.RiskBehaviorControl;
import com.unimarket.module.risk.entity.RiskBlacklist;
import com.unimarket.module.risk.entity.RiskCase;
import com.unimarket.module.risk.entity.RiskDecision;
import com.unimarket.module.risk.entity.RiskEvent;
import com.unimarket.module.risk.entity.RiskRule;
import com.unimarket.module.risk.entity.RiskWhitelist;
import com.unimarket.module.risk.enums.RiskAction;
import com.unimarket.module.risk.enums.RiskLevel;
import com.unimarket.module.risk.mapper.RiskBlacklistMapper;
import com.unimarket.module.risk.mapper.RiskCaseMapper;
import com.unimarket.module.risk.mapper.RiskDecisionMapper;
import com.unimarket.module.risk.mapper.RiskEventMapper;
import com.unimarket.module.risk.mapper.RiskRuleMapper;
import com.unimarket.module.risk.mapper.RiskWhitelistMapper;
import com.unimarket.module.risk.service.RiskBehaviorControlService;
import com.unimarket.module.risk.service.RiskControlService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 风控服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskControlServiceImpl implements RiskControlService {

    private static final String SUBJECT_USER = "USER";
    private static final int STATUS_ENABLED = 1;

    private final RiskEventMapper riskEventMapper;
    private final RiskDecisionMapper riskDecisionMapper;
    private final RiskRuleMapper riskRuleMapper;
    private final RiskBehaviorControlService riskBehaviorControlService;
    private final RiskBlacklistMapper riskBlacklistMapper;
    private final RiskWhitelistMapper riskWhitelistMapper;
    private final RiskCaseMapper riskCaseMapper;
    private final AuditLoginTraceMapper auditLoginTraceMapper;

    @Override
    public RiskDecisionResult evaluate(RiskContext context) {
        long startNs = System.nanoTime();
        RiskContext normalized = normalizeContext(context);
        RiskEvent event = createRiskEvent(normalized);

        DecisionDraft decisionDraft = resolveDecision(normalized);

        RiskDecision decision = new RiskDecision();
        decision.setEventId(event.getEventId());
        decision.setDecisionAction(decisionDraft.action.name());
        decision.setRiskLevel(decisionDraft.riskLevel.getCode());
        decision.setRiskScore(BigDecimal.valueOf(decisionDraft.riskScore));
        decision.setMatchedRuleCodes(JSON.toJSONString(decisionDraft.matchedRuleCodes));
        decision.setDecisionReason(decisionDraft.reason);
        riskDecisionMapper.insert(decision);

        if (decisionDraft.action == RiskAction.REVIEW) {
            RiskCase riskCase = new RiskCase();
            riskCase.setEventId(event.getEventId());
            riskCase.setDecisionId(decision.getDecisionId());
            riskCase.setSchoolCode(normalized.getSchoolCode());
            riskCase.setCampusCode(normalized.getCampusCode());
            riskCase.setCaseStatus("OPEN");
            riskCaseMapper.insert(riskCase);
        }

        Metrics.counter("unimarket.risk.decision.total",
                "eventType", normalized.getEventType(),
                "action", decisionDraft.action.name(),
                "riskLevel", decisionDraft.riskLevel.getCode()).increment();
        Metrics.timer("unimarket.risk.evaluate.duration", "eventType", normalized.getEventType())
                .record(System.nanoTime() - startNs, java.util.concurrent.TimeUnit.NANOSECONDS);

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

    private RiskEvent createRiskEvent(RiskContext context) {
        RiskEvent event = new RiskEvent();
        event.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        event.setEventType(context.getEventType());
        event.setSubjectType(context.getSubjectType());
        event.setSubjectId(context.getSubjectId());
        event.setSchoolCode(context.getSchoolCode());
        event.setCampusCode(context.getCampusCode());
        event.setRiskFeatures(JSON.toJSONString(context.getFeatures()));
        event.setRawPayload(JSON.toJSONString(context.getRawPayload()));
        event.setEventTime(LocalDateTime.now());
        riskEventMapper.insert(event);
        return event;
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

    private DecisionDraft resolveDecision(RiskContext context) {
        DecisionDraft behaviorControlDecision = matchBehaviorControl(context);
        if (behaviorControlDecision != null) {
            return behaviorControlDecision;
        }

        if (inWhitelist(context.getSubjectType(), context.getSubjectId())) {
            return DecisionDraft.allow("命中风控白名单");
        }

        RiskBlacklist blacklist = findActiveBlacklist(context.getSubjectType(), context.getSubjectId());
        if (blacklist != null) {
            return DecisionDraft.reject("命中风控黑名单: " + safeReason(blacklist.getReason()));
        }

        DecisionDraft advancedDecision = matchAdvancedSignals(context);
        if (advancedDecision != null) {
            return advancedDecision;
        }

        List<RiskRule> rules = riskRuleMapper.selectList(new LambdaQueryWrapper<RiskRule>()
                .eq(RiskRule::getEventType, context.getEventType())
                .eq(RiskRule::getStatus, STATUS_ENABLED)
                .orderByAsc(RiskRule::getPriority)
                .orderByAsc(RiskRule::getRuleId));

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

    private DecisionDraft matchAdvancedSignals(RiskContext context) {
        DecisionDraft ipSignal = matchIpProfileSignal(context);
        if (ipSignal != null) {
            return ipSignal;
        }

        DecisionDraft deviceSignal = matchDeviceFingerprintSignal(context);
        if (deviceSignal != null) {
            return deviceSignal;
        }

        return matchIpBurstSignal(context);
    }

    private DecisionDraft matchIpProfileSignal(RiskContext context) {
        if (!"LOGIN".equalsIgnoreCase(context.getEventType())
                || context.getRequestIp() == null
                || context.getRequestIp().isBlank()) {
            return null;
        }

        LocalDateTime since = LocalDateTime.now().minusMinutes(30);
        Long failedCount = auditLoginTraceMapper.selectCount(new LambdaQueryWrapper<AuditLoginTrace>()
                .eq(AuditLoginTrace::getIp, context.getRequestIp())
                .ge(AuditLoginTrace::getCreateTime, since)
                .in(AuditLoginTrace::getLoginResult, List.of("FAIL", "CHALLENGE")));
        long total = failedCount == null ? 0L : failedCount;
        if (total >= 10) {
            return new DecisionDraft(RiskAction.CHALLENGE, RiskLevel.HIGH, 90D,
                    "登录IP近期失败次数过高，触发安全校验", Collections.emptyList());
        }
        if (total >= 6) {
            return new DecisionDraft(RiskAction.REVIEW, RiskLevel.MEDIUM, 78D,
                    "登录IP存在异常失败行为，进入人工复核", Collections.emptyList());
        }
        return null;
    }

    private DecisionDraft matchDeviceFingerprintSignal(RiskContext context) {
        Object fingerprintObj = context.getFeatures().get("deviceFingerprint");
        if (fingerprintObj == null) {
            return null;
        }
        String fingerprint = String.valueOf(fingerprintObj);
        if (fingerprint.isBlank()) {
            return null;
        }

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        String marker = buildFeatureContainsMarker("deviceFingerprint", fingerprint);
        List<RiskEvent> recentEvents = riskEventMapper.selectList(new LambdaQueryWrapper<RiskEvent>()
                .ge(RiskEvent::getEventTime, since)
                .like(RiskEvent::getRiskFeatures, marker)
                .orderByDesc(RiskEvent::getEventTime)
                .last("LIMIT 300"));
        if (recentEvents == null || recentEvents.isEmpty()) {
            return null;
        }

        Set<String> subjects = new HashSet<>();
        for (RiskEvent event : recentEvents) {
            if (event.getSubjectId() != null && !event.getSubjectId().isBlank()) {
                subjects.add(event.getSubjectId());
            }
        }

        int linkedUsers = subjects.size();
        if (linkedUsers >= 5) {
            return new DecisionDraft(RiskAction.CHALLENGE, RiskLevel.HIGH, 88D,
                    "设备指纹关联账号过多，触发强校验", Collections.emptyList());
        }
        if (linkedUsers >= 3) {
            return new DecisionDraft(RiskAction.REVIEW, RiskLevel.MEDIUM, 72D,
                    "设备指纹关联账号异常，进入人工复核", Collections.emptyList());
        }
        return null;
    }

    private DecisionDraft matchIpBurstSignal(RiskContext context) {
        if (context.getRequestIp() == null || context.getRequestIp().isBlank()) {
            return null;
        }
        if (!List.of("CHAT_SEND", "AI_CHAT_SEND", "FOLLOW_USER", "GOODS_PUBLISH", "ERRAND_PUBLISH")
                .contains(context.getEventType())) {
            return null;
        }

        LocalDateTime since = LocalDateTime.now().minusMinutes(5);
        String marker = buildFeatureContainsMarker("requestIp", context.getRequestIp());
        Long burstCount = riskEventMapper.selectCount(new LambdaQueryWrapper<RiskEvent>()
                .eq(RiskEvent::getEventType, context.getEventType())
                .ge(RiskEvent::getEventTime, since)
                .like(RiskEvent::getRiskFeatures, marker));
        long total = burstCount == null ? 0L : burstCount;
        if (total >= 45) {
            return new DecisionDraft(RiskAction.LIMIT, RiskLevel.MEDIUM, 62D,
                    "IP行为频率异常，已触发限流", Collections.emptyList());
        }
        return null;
    }

    private String buildFeatureContainsMarker(String key, String value) {
        return "\"" + key + "\":\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private boolean inWhitelist(String subjectType, String subjectId) {
        LocalDateTime now = LocalDateTime.now();
        Long count = riskWhitelistMapper.selectCount(new LambdaQueryWrapper<RiskWhitelist>()
                .eq(RiskWhitelist::getSubjectType, subjectType)
                .eq(RiskWhitelist::getSubjectId, subjectId)
                .eq(RiskWhitelist::getStatus, STATUS_ENABLED)
                .and(w -> w.isNull(RiskWhitelist::getExpireTime)
                        .or()
                        .gt(RiskWhitelist::getExpireTime, now)));
        return count != null && count > 0;
    }

    private RiskBlacklist findActiveBlacklist(String subjectType, String subjectId) {
        LocalDateTime now = LocalDateTime.now();
        return riskBlacklistMapper.selectOne(new LambdaQueryWrapper<RiskBlacklist>()
                .eq(RiskBlacklist::getSubjectType, subjectType)
                .eq(RiskBlacklist::getSubjectId, subjectId)
                .eq(RiskBlacklist::getStatus, STATUS_ENABLED)
                .and(w -> w.isNull(RiskBlacklist::getExpireTime)
                        .or()
                        .gt(RiskBlacklist::getExpireTime, now))
                .last("LIMIT 1"));
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

        String subjectId = context.getSubjectId();
        if (!Objects.equals(subjectType, context.getSubjectType())) {
            if ("IP".equalsIgnoreCase(subjectType) && context.getRequestIp() != null) {
                subjectId = context.getRequestIp();
            }
        }

        if (subjectId == null || subjectId.isBlank()) {
            return false;
        }

        LocalDateTime since = LocalDateTime.now().minusMinutes(Math.max(windowMinutes, 1));
        Long count = riskEventMapper.selectCount(new LambdaQueryWrapper<RiskEvent>()
                .eq(RiskEvent::getEventType, context.getEventType())
                .eq(RiskEvent::getSubjectType, subjectType)
                .eq(RiskEvent::getSubjectId, subjectId)
                .ge(RiskEvent::getEventTime, since));
        long total = count == null ? 0 : count;
        return total >= Math.max(maxCount, 1);
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
