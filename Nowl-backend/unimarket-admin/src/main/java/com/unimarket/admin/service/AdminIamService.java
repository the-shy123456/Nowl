package com.unimarket.admin.service;

import com.unimarket.admin.dto.AdminScopeBindingUpsertDTO;
import com.unimarket.admin.dto.UserRoleGrantDTO;
import com.unimarket.admin.vo.AdminScopeBindingVO;
import com.unimarket.admin.vo.IamRoleVO;
import com.unimarket.admin.vo.UserRoleBindingVO;

import java.util.List;

/**
 * 管理后台-IAM运维服务
 */
public interface AdminIamService {

    List<IamRoleVO> listRoles();

    List<UserRoleBindingVO> listUserRoles(Long operatorId, Long userId);

    void grantUserRole(Long operatorId, UserRoleGrantDTO dto);

    void revokeUserRole(Long operatorId, Long bindingId, String reason);

    List<AdminScopeBindingVO> listAdminScopes(Long operatorId, Long userId);

    void upsertAdminScope(Long operatorId, AdminScopeBindingUpsertDTO dto);

    void disableAdminScope(Long operatorId, Long bindingId, String reason);
}

