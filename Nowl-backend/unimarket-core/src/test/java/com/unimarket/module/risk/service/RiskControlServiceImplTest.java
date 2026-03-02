package com.unimarket.module.risk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.audit.mapper.AuditLoginTraceMapper;
import com.unimarket.module.risk.dto.RiskContext;
import com.unimarket.module.risk.entity.RiskBehaviorControl;
import com.unimarket.module.risk.entity.RiskCase;
import com.unimarket.module.risk.entity.RiskDecision;
import com.unimarket.module.risk.entity.RiskEvent;
import com.unimarket.module.risk.entity.RiskRule;
import com.unimarket.module.risk.enums.RiskAction;
import com.unimarket.module.risk.mapper.RiskBlacklistMapper;
import com.unimarket.module.risk.mapper.RiskCaseMapper;
import com.unimarket.module.risk.mapper.RiskDecisionMapper;
import com.unimarket.module.risk.mapper.RiskEventMapper;
import com.unimarket.module.risk.mapper.RiskRuleMapper;
import com.unimarket.module.risk.mapper.RiskWhitelistMapper;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskControlServiceImplTest {

    @Mock
    private RiskEventMapper riskEventMapper;
    @Mock
    private RiskDecisionMapper riskDecisionMapper;
    @Mock
    private RiskRuleMapper riskRuleMapper;
    @Mock
    private RiskBehaviorControlService riskBehaviorControlService;
    @Mock
    private RiskBlacklistMapper riskBlacklistMapper;
    @Mock
    private RiskWhitelistMapper riskWhitelistMapper;
    @Mock
    private RiskCaseMapper riskCaseMapper;
    @Mock
    private AuditLoginTraceMapper auditLoginTraceMapper;

    @InjectMocks
    private RiskControlServiceImpl riskControlService;

    @Test
    @DisplayName("evaluate: 用户行为管控优先于规则")
    void evaluate_behaviorControlFirst() {
        RiskBehaviorControl control = new RiskBehaviorControl();
        control.setControlAction("LIMIT");
        control.setReason("手动限流");

        when(riskBehaviorControlService.findMatchedControl(1001L, "CHAT_SEND")).thenReturn(control);

        mockInsertIds();

        RiskDecisionResult result = riskControlService.evaluate(RiskContext.builder()
                .eventType("CHAT_SEND")
                .userId(1001L)
                .schoolCode("SC001")
                .campusCode("CP001")
                .rawPayload(Map.of("content", "hello"))
                .build());

        assertEquals(RiskAction.LIMIT, result.getAction());
        assertTrue(result.getReason().contains("用户行为受控"));
        verify(riskRuleMapper, never()).selectList(any(LambdaQueryWrapper.class));
        verify(riskCaseMapper, never()).insert(any(RiskCase.class));
    }

    @Test
    @DisplayName("evaluate: THRESHOLD 规则使用 maxCount 键")
    void evaluate_thresholdRule_useMaxCount() {
        when(riskBehaviorControlService.findMatchedControl(2002L, "CHAT_SEND")).thenReturn(null);
        when(riskWhitelistMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(riskBlacklistMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        RiskRule rule = new RiskRule();
        rule.setRuleCode("RULE_CHAT_BURST");
        rule.setRuleName("聊天频率限制");
        rule.setRuleType("THRESHOLD");
        rule.setDecisionAction("LIMIT");
        rule.setRuleConfig("{\"maxCount\":1,\"windowMinutes\":10,\"subjectType\":\"USER\"}");
        when(riskRuleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));
        when(riskEventMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        mockInsertIds();

        RiskDecisionResult result = riskControlService.evaluate(RiskContext.builder()
                .eventType("CHAT_SEND")
                .userId(2002L)
                .subjectType("USER")
                .subjectId("2002")
                .rawPayload(Map.of("content", "test"))
                .build());

        assertEquals(RiskAction.LIMIT, result.getAction());
        verify(riskCaseMapper, never()).insert(any(RiskCase.class));
    }

    @Test
    @DisplayName("evaluate: REVIEW 决策会创建风险工单")
    void evaluate_reviewRule_createCase() {
        when(riskBehaviorControlService.findMatchedControl(3003L, "CHAT_SEND")).thenReturn(null);
        when(riskWhitelistMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(riskBlacklistMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        RiskRule rule = new RiskRule();
        rule.setRuleCode("RULE_CHAT_KEYWORD");
        rule.setRuleName("敏感词复核");
        rule.setRuleType("KEYWORD");
        rule.setDecisionAction("REVIEW");
        rule.setRuleConfig("{\"field\":\"content\",\"keywords\":[\"spam\"]}");
        when(riskRuleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));

        mockInsertIds();

        RiskDecisionResult result = riskControlService.evaluate(RiskContext.builder()
                .eventType("CHAT_SEND")
                .userId(3003L)
                .schoolCode("SC001")
                .campusCode("CP001")
                .rawPayload(Map.of("content", "this is spam message"))
                .build());

        assertEquals(RiskAction.REVIEW, result.getAction());

        ArgumentCaptor<RiskCase> caseCaptor = ArgumentCaptor.forClass(RiskCase.class);
        verify(riskCaseMapper).insert(caseCaptor.capture());
        RiskCase created = caseCaptor.getValue();
        assertEquals("OPEN", created.getCaseStatus());
        assertEquals("SC001", created.getSchoolCode());
        assertEquals("CP001", created.getCampusCode());
        assertEquals(101L, created.getEventId());
        assertEquals(201L, created.getDecisionId());
    }

    @Test
    @DisplayName("evaluate: 登录IP失败画像触发 CHALLENGE")
    void evaluate_loginIpProfile_challenge() {
        when(riskWhitelistMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(riskBlacklistMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(auditLoginTraceMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L);

        mockInsertIds();

        RiskDecisionResult result = riskControlService.evaluate(RiskContext.builder()
                .eventType("LOGIN")
                .requestIp("10.0.0.8")
                .deviceId("dev-001")
                .rawPayload(Map.of("userAgent", "Mozilla/5.0"))
                .build());

        assertEquals(RiskAction.CHALLENGE, result.getAction());
    }

    @Test
    @DisplayName("evaluate: 设备指纹关联多账号触发 REVIEW")
    void evaluate_deviceFingerprint_review() {
        when(riskWhitelistMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(riskBlacklistMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        RiskEvent e1 = new RiskEvent();
        e1.setSubjectId("1001");
        RiskEvent e2 = new RiskEvent();
        e2.setSubjectId("1002");
        RiskEvent e3 = new RiskEvent();
        e3.setSubjectId("1003");
        when(riskEventMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(e1, e2, e3));

        mockInsertIds();

        RiskDecisionResult result = riskControlService.evaluate(RiskContext.builder()
                .eventType("CHAT_SEND")
                .requestIp("10.0.0.9")
                .deviceId("device-shared")
                .rawPayload(Map.of("content", "hi", "userAgent", "Mozilla/5.0"))
                .build());

        assertEquals(RiskAction.REVIEW, result.getAction());
        verify(riskCaseMapper).insert(any(RiskCase.class));
    }

    @Test
    @DisplayName("assertAllowed: CHALLENGE 返回统一业务提示")
    void assertAllowed_challenge_throwBusinessException() {
        RiskBehaviorControl control = new RiskBehaviorControl();
        control.setControlAction("CHALLENGE");
        when(riskBehaviorControlService.findMatchedControl(4004L, "LOGIN")).thenReturn(control);

        mockInsertIds();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> riskControlService.assertAllowed(RiskContext.builder()
                        .eventType("LOGIN")
                        .userId(4004L)
                        .build()));

        assertEquals("触发安全校验，请稍后重试", ex.getMessage());
    }

    private void mockInsertIds() {
        doAnswer(invocation -> {
            RiskEvent event = invocation.getArgument(0);
            event.setEventId(101L);
            return 1;
        }).when(riskEventMapper).insert(any(RiskEvent.class));

        doAnswer(invocation -> {
            RiskDecision decision = invocation.getArgument(0);
            decision.setDecisionId(201L);
            return 1;
        }).when(riskDecisionMapper).insert(any(RiskDecision.class));
    }
}
