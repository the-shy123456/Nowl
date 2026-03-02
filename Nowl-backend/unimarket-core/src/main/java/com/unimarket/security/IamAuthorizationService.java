package com.unimarket.security;

import com.unimarket.module.iam.service.IamAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * IAM鉴权表达式服务（供@PreAuthorize使用）
 */
@Service("iamAuth")
@RequiredArgsConstructor
public class IamAuthorizationService {

    private final IamAccessService iamAccessService;

    /**
     * 判断用户是否拥有指定权限
     */
    public boolean hasPerm(Long userId, String permissionCode) {
        return iamAccessService.hasPermission(userId, permissionCode);
    }

    /**
     * 判断用户是否拥有管理指定学校/校区的范围权限
     */
    public boolean canManageScope(Long userId, String schoolCode, String campusCode) {
        return iamAccessService.canManageScope(userId, schoolCode, campusCode);
    }
}
