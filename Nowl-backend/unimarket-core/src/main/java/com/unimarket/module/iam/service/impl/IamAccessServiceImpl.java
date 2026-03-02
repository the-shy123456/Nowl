package com.unimarket.module.iam.service.impl;

import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.iam.entity.IamAdminScopeBinding;
import com.unimarket.module.iam.mapper.IamAdminScopeBindingMapper;
import com.unimarket.module.iam.mapper.IamPermissionMapper;
import com.unimarket.module.iam.mapper.IamRoleMapper;
import com.unimarket.module.iam.service.IamAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * IAM访问服务实现
 */
@Service
@RequiredArgsConstructor
public class IamAccessServiceImpl implements IamAccessService {

    private static final String SCOPE_ALL = "ALL";
    private static final String SCOPE_SCHOOL = "SCHOOL";
    private static final String SCOPE_CAMPUS = "CAMPUS";

    private final IamRoleMapper iamRoleMapper;
    private final IamPermissionMapper iamPermissionMapper;
    private final IamAdminScopeBindingMapper iamAdminScopeBindingMapper;

    @Override
    public Set<String> getRoleCodes(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        List<String> roleCodes = iamRoleMapper.selectRoleCodesByUserId(userId);
        return roleCodes == null ? Collections.emptySet() : new HashSet<>(roleCodes);
    }

    @Override
    public Set<String> getPermissionCodes(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        List<String> permissionCodes = iamPermissionMapper.selectPermissionCodesByUserId(userId);
        return permissionCodes == null ? Collections.emptySet() : new HashSet<>(permissionCodes);
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        if (userId == null || permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        return getPermissionCodes(userId).contains(permissionCode);
    }

    @Override
    public List<IamAdminScopeBinding> getAdminScopes(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        Set<String> roleCodes = getRoleCodes(userId);
        if (roleCodes.contains("SUPER_ADMIN")) {
            IamAdminScopeBinding allScope = new IamAdminScopeBinding();
            allScope.setUserId(userId);
            allScope.setScopeType(SCOPE_ALL);
            allScope.setStatus(1);
            return List.of(allScope);
        }

        List<IamAdminScopeBinding> scopes = iamAdminScopeBindingMapper.selectActiveByUserId(userId);
        return scopes == null ? Collections.emptyList() : scopes;
    }

    @Override
    public boolean canManageScope(Long userId, String schoolCode, String campusCode) {
        if (userId == null) {
            return false;
        }
        List<IamAdminScopeBinding> scopes = getAdminScopes(userId);
        if (scopes.isEmpty()) {
            return false;
        }

        for (IamAdminScopeBinding scope : scopes) {
            if (SCOPE_ALL.equalsIgnoreCase(scope.getScopeType())) {
                return true;
            }
        }

        if (schoolCode == null || schoolCode.isBlank()) {
            return false;
        }

        for (IamAdminScopeBinding scope : scopes) {
            if (SCOPE_SCHOOL.equalsIgnoreCase(scope.getScopeType())
                    && schoolCode.equals(scope.getSchoolCode())) {
                return true;
            }
            if (SCOPE_CAMPUS.equalsIgnoreCase(scope.getScopeType())
                    && schoolCode.equals(scope.getSchoolCode())
                    && campusCode != null
                    && campusCode.equals(scope.getCampusCode())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void assertCanManageScope(Long userId, String schoolCode, String campusCode) {
        if (!canManageScope(userId, schoolCode, campusCode)) {
            throw new BusinessException("无权限管理该学校/校区数据");
        }
    }
}
