package com.unimarket.module.iam.service;

import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.iam.entity.IamAdminScopeBinding;
import com.unimarket.module.iam.mapper.IamAdminScopeBindingMapper;
import com.unimarket.module.iam.mapper.IamPermissionMapper;
import com.unimarket.module.iam.mapper.IamRoleMapper;
import com.unimarket.module.iam.service.impl.IamAccessServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IamAccessServiceImplTest {

    @Mock
    private IamRoleMapper iamRoleMapper;
    @Mock
    private IamPermissionMapper iamPermissionMapper;
    @Mock
    private IamAdminScopeBindingMapper iamAdminScopeBindingMapper;

    @InjectMocks
    private IamAccessServiceImpl iamAccessService;

    @Test
    @DisplayName("hasPermission: 空参数返回 false")
    void hasPermission_invalidInput_false() {
        assertFalse(iamAccessService.hasPermission(null, "admin:goods:list:view"));
        assertFalse(iamAccessService.hasPermission(1L, null));
        assertFalse(iamAccessService.hasPermission(1L, " "));
    }

    @Test
    @DisplayName("hasPermission: 命中权限返回 true")
    void hasPermission_hit_true() {
        when(iamPermissionMapper.selectPermissionCodesByUserId(10L))
                .thenReturn(List.of("risk:event:view", "admin:goods:list:view"));

        assertTrue(iamAccessService.hasPermission(10L, "risk:event:view"));
        assertFalse(iamAccessService.hasPermission(10L, "admin:iam:scope:manage"));
    }

    @Test
    @DisplayName("canManageScope: ALL 范围可管理任意数据")
    void canManageScope_all_true() {
        IamAdminScopeBinding scope = new IamAdminScopeBinding();
        scope.setScopeType("ALL");
        when(iamAdminScopeBindingMapper.selectActiveByUserId(8L)).thenReturn(List.of(scope));

        assertTrue(iamAccessService.canManageScope(8L, "SC001", "CP001"));
        assertTrue(iamAccessService.canManageScope(8L, null, null));
    }

    @Test
    @DisplayName("canManageScope: SCHOOL/CAMPUS 范围命中与越权")
    void canManageScope_schoolAndCampus() {
        IamAdminScopeBinding schoolScope = new IamAdminScopeBinding();
        schoolScope.setScopeType("SCHOOL");
        schoolScope.setSchoolCode("SC001");

        IamAdminScopeBinding campusScope = new IamAdminScopeBinding();
        campusScope.setScopeType("CAMPUS");
        campusScope.setSchoolCode("SC002");
        campusScope.setCampusCode("CP001");

        when(iamAdminScopeBindingMapper.selectActiveByUserId(9L)).thenReturn(List.of(schoolScope, campusScope));

        assertTrue(iamAccessService.canManageScope(9L, "SC001", "CP999"));
        assertTrue(iamAccessService.canManageScope(9L, "SC002", "CP001"));
        assertFalse(iamAccessService.canManageScope(9L, "SC002", "CP002"));
        assertFalse(iamAccessService.canManageScope(9L, "SC003", "CP001"));
        assertFalse(iamAccessService.canManageScope(9L, null, "CP001"));
    }

    @Test
    @DisplayName("assertCanManageScope: 越权抛 BusinessException")
    void assertCanManageScope_denied_throw() {
        when(iamAdminScopeBindingMapper.selectActiveByUserId(7L)).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> iamAccessService.assertCanManageScope(7L, "SC001", "CP001"));
        assertEquals("无权限管理该学校/校区数据", ex.getMessage());
    }

    @Test
    @DisplayName("assertCanManageScope: 命中范围不抛异常")
    void assertCanManageScope_allowed_ok() {
        IamAdminScopeBinding scope = new IamAdminScopeBinding();
        scope.setScopeType("SCHOOL");
        scope.setSchoolCode("SC001");
        when(iamAdminScopeBindingMapper.selectActiveByUserId(6L)).thenReturn(List.of(scope));

        assertDoesNotThrow(() -> iamAccessService.assertCanManageScope(6L, "SC001", null));
    }
}
