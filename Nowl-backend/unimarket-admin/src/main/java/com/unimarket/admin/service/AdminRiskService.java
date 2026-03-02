package com.unimarket.admin.service;

import com.unimarket.admin.dto.BehaviorControlUpsertDTO;
import com.unimarket.admin.vo.UserBehaviorControlVO;

import java.util.List;

/**
 * 管理后台-风控服务
 */
public interface AdminRiskService {

    List<UserBehaviorControlVO> listUserBehaviorControls(Long operatorId, Long targetUserId);

    void upsertBehaviorControl(Long operatorId, BehaviorControlUpsertDTO dto);

    void disableBehaviorControl(Long operatorId, Long controlId);
}

