package com.unimarket.module.iam.service;

import com.unimarket.module.iam.entity.IamAdminScopeBinding;

import java.util.List;
import java.util.Set;

/**
 * IAM访问服务
 */
public interface IamAccessService {

    /**
     * 获取用户角色编码集合
     */
    Set<String> getRoleCodes(Long userId);

    /**
     * 获取用户权限编码集合
     */
    Set<String> getPermissionCodes(Long userId);

    /**
     * 是否拥有指定权限
     */
    boolean hasPermission(Long userId, String permissionCode);

    /**
     * 获取管理员范围绑定
     */
    List<IamAdminScopeBinding> getAdminScopes(Long userId);

    /**
     * 是否可管理指定学校/校区
     */
    boolean canManageScope(Long userId, String schoolCode, String campusCode);

    /**
     * 断言可管理指定学校/校区
     */
    void assertCanManageScope(Long userId, String schoolCode, String campusCode);
}
