package com.unimarket.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.admin.dto.BehaviorControlUpsertDTO;
import com.unimarket.admin.service.AdminRiskService;
import com.unimarket.admin.vo.UserBehaviorControlVO;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.iam.service.IamAccessService;
import com.unimarket.module.risk.entity.RiskBehaviorControl;
import com.unimarket.module.risk.enums.RiskAction;
import com.unimarket.module.risk.enums.RiskEventType;
import com.unimarket.module.risk.mapper.RiskBehaviorControlMapper;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 管理后台-风控服务实现
 */
@Service
@RequiredArgsConstructor
public class AdminRiskServiceImpl implements AdminRiskService {

    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;

    private static final Set<String> ALLOWED_EVENT_TYPES = Set.of(
            "ALL",
            RiskEventType.LOGIN,
            RiskEventType.GOODS_PUBLISH,
            RiskEventType.ERRAND_PUBLISH,
            RiskEventType.ERRAND_ACCEPT,
            RiskEventType.CHAT_SEND,
            RiskEventType.AI_CHAT_SEND,
            RiskEventType.FOLLOW_USER
    );

    private final RiskBehaviorControlMapper riskBehaviorControlMapper;
    private final UserInfoMapper userInfoMapper;
    private final IamAccessService iamAccessService;

    @Override
    public List<UserBehaviorControlVO> listUserBehaviorControls(Long operatorId, Long targetUserId) {
        UserInfo targetUser = getTargetUserAndCheckScope(operatorId, targetUserId);

        List<RiskBehaviorControl> controls = riskBehaviorControlMapper.selectList(
                new LambdaQueryWrapper<RiskBehaviorControl>()
                        .eq(RiskBehaviorControl::getUserId, targetUserId)
                        .eq(RiskBehaviorControl::getStatus, STATUS_ENABLED)
                        .orderByDesc(RiskBehaviorControl::getUpdateTime)
                        .orderByDesc(RiskBehaviorControl::getId)
        );

        List<UserBehaviorControlVO> result = new ArrayList<>();
        for (RiskBehaviorControl control : controls) {
            UserBehaviorControlVO vo = new UserBehaviorControlVO();
            vo.setId(control.getId());
            vo.setUserId(control.getUserId());
            vo.setUserName(targetUser.getNickName());
            vo.setSchoolCode(targetUser.getSchoolCode());
            vo.setCampusCode(targetUser.getCampusCode());
            vo.setEventType(control.getEventType());
            vo.setControlAction(control.getControlAction());
            vo.setReason(control.getReason());
            vo.setExpireTime(control.getExpireTime());
            vo.setStatus(control.getStatus());
            vo.setOperatorId(control.getOperatorId());
            vo.setCreateTime(control.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    @Override
    public void upsertBehaviorControl(Long operatorId, BehaviorControlUpsertDTO dto) {
        if (dto == null || dto.getUserId() == null) {
            throw new BusinessException("目标用户不能为空");
        }

        String eventType = normalizeEventType(dto.getEventType());
        String action = normalizeAction(dto.getControlAction());
        getTargetUserAndCheckScope(operatorId, dto.getUserId());

        RiskBehaviorControl existed = riskBehaviorControlMapper.selectOne(
                new LambdaQueryWrapper<RiskBehaviorControl>()
                        .eq(RiskBehaviorControl::getUserId, dto.getUserId())
                        .eq(RiskBehaviorControl::getEventType, eventType)
                        .last("LIMIT 1")
        );

        if (existed == null) {
            RiskBehaviorControl control = new RiskBehaviorControl();
            control.setUserId(dto.getUserId());
            control.setEventType(eventType);
            control.setControlAction(action);
            control.setReason(dto.getReason());
            control.setExpireTime(dto.getExpireTime());
            control.setStatus(STATUS_ENABLED);
            control.setOperatorId(operatorId);
            riskBehaviorControlMapper.insert(control);
            return;
        }

        existed.setControlAction(action);
        existed.setReason(dto.getReason());
        existed.setExpireTime(dto.getExpireTime());
        existed.setOperatorId(operatorId);
        existed.setStatus(STATUS_ENABLED);
        existed.setUpdateTime(LocalDateTime.now());
        riskBehaviorControlMapper.updateById(existed);
    }

    @Override
    public void disableBehaviorControl(Long operatorId, Long controlId) {
        if (controlId == null) {
            throw new BusinessException("管控记录ID不能为空");
        }
        RiskBehaviorControl control = riskBehaviorControlMapper.selectById(controlId);
        if (control == null) {
            throw new BusinessException("管控记录不存在");
        }

        getTargetUserAndCheckScope(operatorId, control.getUserId());

        control.setStatus(STATUS_DISABLED);
        control.setOperatorId(operatorId);
        control.setUpdateTime(LocalDateTime.now());
        riskBehaviorControlMapper.updateById(control);
    }

    private UserInfo getTargetUserAndCheckScope(Long operatorId, Long targetUserId) {
        UserInfo targetUser = userInfoMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException("目标用户不存在");
        }
        iamAccessService.assertCanManageScope(operatorId, targetUser.getSchoolCode(), targetUser.getCampusCode());
        return targetUser;
    }

    private String normalizeEventType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("行为类型不能为空");
        }
        String eventType = raw.trim().toUpperCase();
        if (!ALLOWED_EVENT_TYPES.contains(eventType)) {
            throw new BusinessException("不支持的行为类型: " + eventType);
        }
        return eventType;
    }

    private String normalizeAction(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("管控动作不能为空");
        }
        String action = raw.trim().toUpperCase();
        try {
            RiskAction.valueOf(action);
            return action;
        } catch (IllegalArgumentException e) {
            throw new BusinessException("不支持的管控动作: " + action);
        }
    }
}
