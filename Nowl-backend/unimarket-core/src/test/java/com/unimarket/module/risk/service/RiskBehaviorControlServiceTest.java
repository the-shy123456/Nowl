package com.unimarket.module.risk.service;

import com.unimarket.common.utils.RedisCache;
import com.unimarket.module.risk.entity.RiskBehaviorControl;
import com.unimarket.module.risk.mapper.RiskBehaviorControlMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskBehaviorControlServiceTest {

    @Mock
    private RiskBehaviorControlMapper riskBehaviorControlMapper;
    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private RiskBehaviorControlService riskBehaviorControlService;

    @Test
    @DisplayName("findMatchedControl: 命中精确事件优先返回")
    void findMatchedControl_exactFirst() {
        RiskBehaviorControl exact = new RiskBehaviorControl();
        exact.setEventType("CHAT_SEND");
        exact.setControlAction("REJECT");
        when(riskBehaviorControlMapper.selectOne(any())).thenReturn(exact);

        RiskBehaviorControl result = riskBehaviorControlService.findMatchedControl(1001L, "chat_send");

        assertSame(exact, result);
        verify(riskBehaviorControlMapper, times(1)).selectOne(any());
    }

    @Test
    @DisplayName("findMatchedControl: 精确未命中时回退ALL")
    void findMatchedControl_fallbackAll() {
        RiskBehaviorControl wildcard = new RiskBehaviorControl();
        wildcard.setEventType("ALL");
        wildcard.setControlAction("REJECT");
        when(riskBehaviorControlMapper.selectOne(any())).thenReturn(null, wildcard);

        RiskBehaviorControl result = riskBehaviorControlService.findMatchedControl(1002L, "GOODS_PUBLISH");

        assertSame(wildcard, result);
        verify(riskBehaviorControlMapper, times(2)).selectOne(any());
    }

    @Test
    @DisplayName("isHardBlocked: REJECT 视为硬阻断")
    void isHardBlocked_rejectTrue() {
        RiskBehaviorControl control = new RiskBehaviorControl();
        control.setControlAction("REJECT");
        when(riskBehaviorControlMapper.selectOne(any())).thenReturn(control, control);

        assertTrue(riskBehaviorControlService.isHardBlocked(1003L, "LOGIN"));
        assertFalse(riskBehaviorControlService.isAllowed(1003L, "LOGIN"));
    }

    @Test
    @DisplayName("isAllowed: REVIEW/CHALLENGE/LIMIT 不作为鉴权层硬阻断")
    void isAllowed_nonRejectAllowed() {
        RiskBehaviorControl control = new RiskBehaviorControl();
        control.setControlAction("REVIEW");
        when(riskBehaviorControlMapper.selectOne(any())).thenReturn(control, control);

        assertFalse(riskBehaviorControlService.isHardBlocked(1004L, "CHAT_SEND"));
        assertTrue(riskBehaviorControlService.isAllowed(1004L, "CHAT_SEND"));
    }
}
