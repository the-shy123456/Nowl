package com.unimarket.admin.service;

import com.unimarket.admin.dto.AdminOperationAuditQueryDTO;
import com.unimarket.admin.dto.LoginTraceQueryDTO;
import com.unimarket.admin.dto.PermissionChangeQueryDTO;
import com.unimarket.admin.vo.AdminOperationAuditVO;
import com.unimarket.admin.vo.AuditOverviewVO;
import com.unimarket.admin.vo.LoginTraceVO;
import com.unimarket.admin.vo.PermissionChangeVO;
import com.unimarket.common.result.PageResult;

/**
 * 管理后台-审计中心服务
 */
public interface AdminAuditService {

    PageResult<AdminOperationAuditVO> getAdminOperationAudits(Long operatorId, AdminOperationAuditQueryDTO query);

    PageResult<PermissionChangeVO> getPermissionChanges(Long operatorId, PermissionChangeQueryDTO query);

    PageResult<LoginTraceVO> getLoginTraces(Long operatorId, LoginTraceQueryDTO query);

    AuditOverviewVO getAuditOverview(Long operatorId, Integer days);
}
