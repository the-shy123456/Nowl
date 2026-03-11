package com.unimarket.module.risk.service;

import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.risk.dto.RiskAuditMessage;
import com.unimarket.module.risk.dto.RiskContext;
import com.unimarket.module.risk.entity.RiskBehaviorControl;
import com.unimarket.module.risk.entity.RiskRule;
import com.unimarket.module.risk.enums.RiskAction;
import com.unimarket.module.risk.enums.RiskMode;
import com.unimarket.module.risk.service.impl.RiskControlServiceImpl;
import com.unimarket.module.risk.vo.RiskDecisionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskControlServiceImplTest {

    @Mock
    private RiskPolicyCacheService riskPolicyCacheService;
    @Mock
    private RiskBehaviorControlService riskBehaviorControlService;
    @Mock
    private RiskModeService riskModeService;
    @Mock
    private RiskRealtimeStore riskRealtimeStore;
    @Mock
    private RiskAuditPublisher riskAuditPublisher;
    @Mock
    private RiskIdGenerator riskIdGenerator;

    @InjectMocks
    private RiskControlServiceImpl riskControlService;

    @Test
    @DisplayName("evaluate: 关闭风控时直接放行")
    void evaluate_offMode_allow() {
        when(riskModeService.getMode()).thenReturn(RiskMode.OFF);

        RiskDecisionResult result = riskControlService.evaluate(RiskContext.builder()
                .eventType("CHAT_SEND")
                .userId(1000L)
                .build());

        assertEquals(RiskAction.ALLOW, result.getAction());
        assertEquals("风控已关闭", result.getReason());
        verify(riskAuditPublisher, never()).publish(any(RiskAuditMessage.class));
    }

    @Test
    @DisplayName("evaluate: 用户行为管控优先于规则")
    void evaluate_behaviorControlFirst() {
        when(riskModeService.getMode()).thenReturn(RiskMode.FULL);
        when(riskIdGenerator.nextId()).thenReturn(101L, 201L);
        RiskBehaviorControl control = new RiskBehaviorControl();
        control.setControlAction("LIMIT");
        control.setReason("手动限流");

        when(riskBehaviorControlService.findMatchedControl(1001L, "CHAT_SEND")).thenReturn(control);

        RiskDecisionResult result = riskControlService.evaluate(RiskContext.builder()
                .eventType("CHAT_SEND")
                .userId(1001L)
                .schoolCode("SC001")
                .campusCode("CP001")
                .rawPayload(Map.of("content", "hello"))
                .build());

        assertEquals(RiskAction.LIMIT, result.getAction());
        assertTrue(result.getReason().contains("用户行为受控"));
        verify(riskPolicyCacheService, never()).getEnabledRules(any());
    }

    @Test
    @DisplayName("evaluate: THRESHOLD 规则使用 Redis 计数")
    void evaluate_thresholdRule_useRealtimeCounter() {
        when(riskModeService.getMode()).thenReturn(RiskMode.FULL);
        when(riskIdGenerator.nextId()).thenReturn(101L, 201L);
        when(riskBehaviorControlService.findMatchedControl(2002L, "CHAT_SEND")).thenReturn(null);
        when(riskPolicyCacheService.isWhitelisted("USER", "2002")).thenReturn(false);
        when(riskPolicyCacheService.getActiveBlacklist("USER", "2002")).thenReturn(null);

        RiskRule rule = new RiskRule();
        rule.setRuleCode("RULE_CHAT_BURST");
        rule.setRuleName("聊天频率限制");
        rule.setRuleType("THRESHOLD");
        rule.setDecisionAction("LIMIT");
        rule.setRuleConfig("{\"maxCount\":1,\"windowMinutes\":10,\"subjectType\":\"USER\",\"counterType\":\"EVENT_COUNT\"}");
        when(riskPolicyCacheService.getEnabledRules("CHAT_SEND")).thenReturn(List.of(rule));
        when(riskRealtimeStore.countEvents("CHAT_SEND", "USER", "2002", 10)).thenReturn(1L);

        RiskDecisionResult result = riskControlService.evaluate(RiskContext.builder()
                .eventType("CHAT_SEND")
                .userId(2002L)
                .subjectType("USER")
                .subjectId("2002")
                .rawPayload(Map.of("content", "test"))
                .build());

        assertEquals(RiskAction.LIMIT, result.getAction());
    }

    @Test
    @DisplayName("evaluate: REVIEW 决策会产生风控工单消息")
    void evaluate_reviewRule_publishCase() {
        when(riskModeService.getMode()).thenReturn(RiskMode.FULL);
        when(riskIdGenerator.nextId()).thenReturn(101L, 201L, 301L);
        when(riskBehaviorControlService.findMatchedControl(3003L, "CHAT_SEND")).thenReturn(null);
        when(riskPolicyCacheService.isWhitelisted("USER", "3003")).thenReturn(false);
        when(riskPolicyCacheService.getActiveBlacklist("USER", "3003")).thenReturn(null);

        RiskRule rule = new RiskRule();
        rule.setRuleCode("RULE_CHAT_KEYWORD");
        rule.setRuleName("敏感词复核");
        rule.setRuleType("KEYWORD");
        rule.setDecisionAction("REVIEW");
        rule.setRuleConfig("{\"field\":\"content\",\"keywords\":[\"spam\"]}");
        when(riskPolicyCacheService.getEnabledRules("CHAT_SEND")).thenReturn(List.of(rule));

        RiskDecisionResult result = riskControlService.evaluate(RiskContext.builder()
                .eventType("CHAT_SEND")
                .userId(3003L)
                .subjectType("USER")
                .subjectId("3003")
                .schoolCode("SC001")
                .campusCode("CP001")
                .rawPayload(Map.of("content", "this is spam message"))
                .build());

        assertEquals(RiskAction.REVIEW, result.getAction());
        ArgumentCaptor<RiskAuditMessage> captor = ArgumentCaptor.forClass(RiskAuditMessage.class);
        verify(riskAuditPublisher).publish(captor.capture());
        RiskAuditMessage message = captor.getValue();
        assertNotNull(message.getRiskCase());
        assertEquals(101L, message.getEvent().getEventId());
        assertEquals(201L, message.getDecision().getDecisionId());
        assertEquals(301L, message.getRiskCase().getCaseId());
    }

    @Test
    @DisplayName("evaluate: 登录失败阈值规则触发 CHALLENGE")
    void evaluate_loginIpProfile_challenge() {
        when(riskModeService.getMode()).thenReturn(RiskMode.FULL);
        when(riskIdGenerator.nextId()).thenReturn(101L, 201L);
        when(riskPolicyCacheService.isWhitelisted("IP", "10.0.0.8")).thenReturn(false);
        when(riskPolicyCacheService.getActiveBlacklist("IP", "10.0.0.8")).thenReturn(null);

        RiskRule rule = new RiskRule();
        rule.setRuleCode("RULE_LOGIN_FAIL_IP_CHALLENGE");
        rule.setRuleName("登录失败画像-IP触发安全校验");
        rule.setRuleType("THRESHOLD");
        rule.setDecisionAction("CHALLENGE");
        rule.setRuleConfig("{\"windowMinutes\":30,\"maxCount\":10,\"subjectType\":\"IP\",\"counterType\":\"LOGIN_FAILURE_COUNT\"}");
        when(riskPolicyCacheService.getEnabledRules("LOGIN")).thenReturn(List.of(rule));
        when(riskRealtimeStore.countLoginFailures("10.0.0.8", 30)).thenReturn(10L);

        RiskDecisionResult result = riskControlService.evaluate(RiskContext.builder()
                .eventType("LOGIN")
                .requestIp("10.0.0.8")
                .deviceId("dev-001")
                .rawPayload(Map.of("userAgent", "Mozilla/5.0"))
                .build());

        assertEquals(RiskAction.CHALLENGE, result.getAction());
    }

    @Test
    @DisplayName("evaluate: 设备指纹阈值规则触发 REVIEW")
    void evaluate_deviceFingerprint_review() {
        when(riskModeService.getMode()).thenReturn(RiskMode.FULL);
        when(riskIdGenerator.nextId()).thenReturn(101L, 201L, 301L);
        when(riskPolicyCacheService.isWhitelisted("IP", "10.0.0.9")).thenReturn(false);
        when(riskPolicyCacheService.getActiveBlacklist("IP", "10.0.0.9")).thenReturn(null);

        RiskRule rule = new RiskRule();
        rule.setRuleCode("RULE_DEVICE_MULTI_ACCOUNT_REVIEW");
        rule.setRuleName("设备指纹关联账号异常人工复核");
        rule.setRuleType("THRESHOLD");
        rule.setDecisionAction("REVIEW");
        rule.setRuleConfig("{\"windowMinutes\":1440,\"maxCount\":3,\"subjectType\":\"DEVICE\",\"counterType\":\"DEVICE_SUBJECT_COUNT\"}");
        when(riskPolicyCacheService.getEnabledRules("CHAT_SEND")).thenReturn(List.of(rule));
        when(riskRealtimeStore.countDeviceSubjects("device-shared", 1440)).thenReturn(3);

        RiskDecisionResult result = riskControlService.evaluate(RiskContext.builder()
                .eventType("CHAT_SEND")
                .requestIp("10.0.0.9")
                .deviceId("device-shared")
                .rawPayload(Map.of("content", "hi", "userAgent", "Mozilla/5.0"))
                .build());

        assertEquals(RiskAction.REVIEW, result.getAction());
    }

    @Test
    @DisplayName("assertAllowed: CHALLENGE 返回统一业务提示")
    void assertAllowed_challenge_throwBusinessException() {
        when(riskModeService.getMode()).thenReturn(RiskMode.FULL);
        when(riskIdGenerator.nextId()).thenReturn(101L, 201L);
        RiskBehaviorControl control = new RiskBehaviorControl();
        control.setControlAction("CHALLENGE");
        when(riskBehaviorControlService.findMatchedControl(4004L, "LOGIN")).thenReturn(control);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> riskControlService.assertAllowed(RiskContext.builder()
                        .eventType("LOGIN")
                        .userId(4004L)
                        .build()));

        assertEquals("触发安全校验，请稍后重试", ex.getMessage());
    }
}
