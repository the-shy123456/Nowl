package com.unimarket.admin.controller;

import com.unimarket.admin.dto.AdminScopeBindingUpsertDTO;
import com.unimarket.admin.dto.UserRoleGrantDTO;
import com.unimarket.admin.service.AdminIamService;
import com.unimarket.admin.vo.AdminScopeBindingVO;
import com.unimarket.admin.vo.IamRoleVO;
import com.unimarket.admin.vo.UserRoleBindingVO;
import com.unimarket.common.result.Result;
import com.unimarket.security.UserContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理后台-IAM运维接口
 */
@RestController
@RequestMapping("/admin/iam")
@RequiredArgsConstructor
public class AdminIamController {

    private final AdminIamService adminIamService;

    @GetMapping("/roles")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:iam:role:view')")
    public Result<List<IamRoleVO>> listRoles() {
        return Result.success(adminIamService.listRoles());
    }

    @GetMapping("/user-roles/{userId}")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:iam:user-role:view')")
    public Result<List<UserRoleBindingVO>> listUserRoles(@PathVariable Long userId) {
        Long operatorId = UserContextHolder.getUserId();
        return Result.success(adminIamService.listUserRoles(operatorId, userId));
    }

    @PutMapping("/user-role/grant")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:iam:user-role:manage')")
    public Result<Void> grantUserRole(@Valid @RequestBody UserRoleGrantDTO dto) {
        Long operatorId = UserContextHolder.getUserId();
        adminIamService.grantUserRole(operatorId, dto);
        return Result.success();
    }

    @DeleteMapping("/user-role/{bindingId}")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:iam:user-role:manage')")
    public Result<Void> revokeUserRole(@PathVariable Long bindingId,
                                       @RequestParam(required = false) String reason) {
        Long operatorId = UserContextHolder.getUserId();
        adminIamService.revokeUserRole(operatorId, bindingId, reason);
        return Result.success();
    }

    @GetMapping("/scopes/{userId}")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:iam:scope:view')")
    public Result<List<AdminScopeBindingVO>> listAdminScopes(@PathVariable Long userId) {
        Long operatorId = UserContextHolder.getUserId();
        return Result.success(adminIamService.listAdminScopes(operatorId, userId));
    }

    @PutMapping("/scope")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:iam:scope:manage')")
    public Result<Void> upsertAdminScope(@Valid @RequestBody AdminScopeBindingUpsertDTO dto) {
        Long operatorId = UserContextHolder.getUserId();
        adminIamService.upsertAdminScope(operatorId, dto);
        return Result.success();
    }

    @DeleteMapping("/scope/{bindingId}")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:iam:scope:manage')")
    public Result<Void> disableAdminScope(@PathVariable Long bindingId,
                                          @RequestParam(required = false) String reason) {
        Long operatorId = UserContextHolder.getUserId();
        adminIamService.disableAdminScope(operatorId, bindingId, reason);
        return Result.success();
    }
}

