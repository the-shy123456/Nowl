package com.unimarket.admin.service;

import com.unimarket.admin.service.impl.AdminAuditServiceImpl;
import com.unimarket.admin.vo.AuditOverviewVO;
import com.unimarket.module.audit.entity.AuditAdminOperation;
import com.unimarket.module.audit.mapper.AuditAdminOperationMapper;
import com.unimarket.module.audit.mapper.AuditLoginTraceMapper;
import com.unimarket.module.audit.mapper.AuditPermissionChangeMapper;
import com.unimarket.module.iam.entity.IamAdminScopeBinding;
import com.unimarket.module.iam.service.IamAccessService;
import com.unimarket.module.user.mapper.UserInfoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditServiceImplTest {

    @Mock
    private AuditAdminOperationMapper auditAdminOperationMapper;
    @Mock
    private AuditPermissionChangeMapper auditPermissionChangeMapper;
    @Mock
    private AuditLoginTraceMapper auditLoginTraceMapper;
    @Mock
    private IamAccessService iamAccessService;
    @Mock
    private UserInfoMapper userInfoMapper;

    @InjectMocks
    private AdminAuditServiceImpl adminAuditService;

    @Test
    @DisplayName("getAuditOverview: ALL 范围返回平台总览")
    void getAuditOverview_allScope() {
        IamAdminScopeBinding allScope = new IamAdminScopeBinding();
        allScope.setScopeType("ALL");
        when(iamAccessService.getAdminScopes(1L)).thenReturn(List.of(allScope));

        when(auditAdminOperationMapper.selectCount(any())).thenReturn(120L, 9L);
        when(auditPermissionChangeMapper.selectCount(any())).thenReturn(14L);
        when(auditLoginTraceMapper.selectCount(any())).thenReturn(80L, 21L, 6L);

        AuditAdminOperation latest = new AuditAdminOperation();
        latest.setCreateTime(LocalDateTime.of(2026, 2, 13, 21, 30, 0));
        when(auditAdminOperationMapper.selectOne(any())).thenReturn(latest);

        AuditOverviewVO overview = adminAuditService.getAuditOverview(1L, 7);

        assertEquals(7, overview.getWindowDays());
        assertEquals(120L, overview.getTotalOperations());
        assertEquals(9L, overview.getFailedOperations());
        assertEquals(14L, overview.getPermissionChanges());
        assertEquals(80L, overview.getLoginAttempts());
        assertEquals(21L, overview.getLoginFailures());
        assertEquals(6L, overview.getHighRiskLoginCount());
        assertEquals(LocalDateTime.of(2026, 2, 13, 21, 30, 0), overview.getLastOperationTime());
    }

    @Test
    @DisplayName("getAuditOverview: 非 ALL 且无可管用户时审计汇总降级")
    void getAuditOverview_noManageableUsers() {
        IamAdminScopeBinding campusScope = new IamAdminScopeBinding();
        campusScope.setScopeType("CAMPUS");
        campusScope.setSchoolCode("SC001");
        campusScope.setCampusCode("CP001");
        when(iamAccessService.getAdminScopes(9L)).thenReturn(List.of(campusScope));

        when(userInfoMapper.selectList(any())).thenReturn(List.of());
        when(auditAdminOperationMapper.selectCount(any())).thenReturn(6L, 1L);
        when(auditAdminOperationMapper.selectOne(any())).thenReturn(null);

        AuditOverviewVO overview = adminAuditService.getAuditOverview(9L, 7);

        assertEquals(6L, overview.getTotalOperations());
        assertEquals(1L, overview.getFailedOperations());
        assertEquals(0L, overview.getPermissionChanges());
        assertEquals(0L, overview.getLoginAttempts());
        assertEquals(0L, overview.getLoginFailures());
        assertEquals(0L, overview.getHighRiskLoginCount());
    }
}
