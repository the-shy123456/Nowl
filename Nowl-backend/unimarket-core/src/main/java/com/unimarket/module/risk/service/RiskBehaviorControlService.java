package com.unimarket.module.risk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.module.risk.entity.RiskBehaviorControl;
import com.unimarket.module.risk.enums.RiskAction;
import com.unimarket.module.risk.mapper.RiskBehaviorControlMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户行为管控查询服务
 */
@Service
@RequiredArgsConstructor
public class RiskBehaviorControlService {

    public static final String EVENT_ALL = "ALL";
    private static final int STATUS_ENABLED = 1;

    private final RiskBehaviorControlMapper riskBehaviorControlMapper;

    /**
     * 判断用户在指定行为下是否允许执行（仅 REJECT 视为硬阻断，支持 ALL 全局兜底）
     */
    public boolean isAllowed(Long userId, String eventType) {
        return !isHardBlocked(userId, eventType);
    }

    /**
     * 判断用户在指定行为下是否被硬阻断（REJECT）
     */
    public boolean isHardBlocked(Long userId, String eventType) {
        RiskBehaviorControl matched = findMatchedControl(userId, eventType);
        if (matched == null) {
            return false;
        }
        return RiskAction.from(matched.getControlAction()) == RiskAction.REJECT;
    }

    /**
     * 获取用户在指定行为下生效的管控规则（优先精确行为，其次ALL）
     */
    public RiskBehaviorControl findMatchedControl(Long userId, String eventType) {
        if (userId == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        String normalizedEventType = normalizeEventType(eventType);
        if (normalizedEventType != null) {
            RiskBehaviorControl exact = riskBehaviorControlMapper.selectOne(activeQuery(userId, normalizedEventType, now));
            if (exact != null) {
                return exact;
            }
        }

        return riskBehaviorControlMapper.selectOne(activeQuery(userId, EVENT_ALL, now));
    }

    private LambdaQueryWrapper<RiskBehaviorControl> activeQuery(Long userId, String eventType, LocalDateTime now) {
        return new LambdaQueryWrapper<RiskBehaviorControl>()
                .eq(RiskBehaviorControl::getUserId, userId)
                .eq(RiskBehaviorControl::getEventType, eventType)
                .eq(RiskBehaviorControl::getStatus, STATUS_ENABLED)
                .and(w -> w.isNull(RiskBehaviorControl::getExpireTime)
                        .or()
                        .gt(RiskBehaviorControl::getExpireTime, now))
                .orderByDesc(RiskBehaviorControl::getUpdateTime)
                .orderByDesc(RiskBehaviorControl::getId)
                .last("LIMIT 1");
    }

    private String normalizeEventType(String eventType) {
        if (eventType == null) {
            return null;
        }
        String normalized = eventType.trim().toUpperCase();
        return normalized.isEmpty() ? null : normalized;
    }
}
