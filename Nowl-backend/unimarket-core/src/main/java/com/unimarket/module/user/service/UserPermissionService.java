package com.unimarket.module.user.service;

import com.unimarket.common.enums.RunnableStatus;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户权限校验服务
 * 用于 @PreAuthorize 注解中的权限判断
 *
 * 使用方式：@PreAuthorize("@userPermission.isAuthenticated(authentication.principal.userId)")
 */
@Slf4j
@Service("userPermission")
@RequiredArgsConstructor
public class UserPermissionService {

    private final UserInfoMapper userInfoMapper;

    /**
     * 判断用户是否已完成实名认证（authStatus=2）
     * @param userId 用户ID
     * @return 是否已认证
     */
    public boolean isAuthenticated(Long userId) {
        if (userId == null) {
            return false;
        }
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            log.warn("用户不存在: userId={}", userId);
            return false;
        }
        return user.getAuthStatus() != null && user.getAuthStatus() == 2;
    }

    /**
     * 判断用户是否为跑腿员（runnableStatus=2）
     * @param userId 用户ID
     * @return 是否为跑腿员
     */
    public boolean isRunner(Long userId) {
        if (userId == null) {
            return false;
        }
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        return user.getRunnableStatus() != null
                && user.getRunnableStatus().equals(RunnableStatus.APPROVED.getCode());
    }

    /**
     * 判断用户是否已认证且为跑腿员
     * @param userId 用户ID
     * @return 是否满足条件
     */
    public boolean isAuthenticatedRunner(Long userId) {
        return isAuthenticated(userId) && isRunner(userId);
    }
}
