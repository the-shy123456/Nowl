package com.unimarket.admin.controller;

import com.unimarket.admin.dto.AdminOperationAuditQueryDTO;
import com.unimarket.admin.dto.LoginTraceQueryDTO;
import com.unimarket.admin.dto.PermissionChangeQueryDTO;
import com.unimarket.admin.service.AdminAuditService;
import com.unimarket.admin.vo.AdminOperationAuditVO;
import com.unimarket.admin.vo.AuditOverviewVO;
import com.unimarket.admin.vo.LoginTraceVO;
import com.unimarket.admin.vo.PermissionChangeVO;
import com.unimarket.common.result.PageResult;
import com.unimarket.common.result.Result;
import com.unimarket.security.UserContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台-审计中心接口
 */
@RestController
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
@Validated
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @GetMapping("/operations")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:audit:operation:view')")
    public Result<PageResult<AdminOperationAuditVO>> getOperationAudits(@Valid AdminOperationAuditQueryDTO query) {
        Long operatorId = UserContextHolder.getUserId();
        return Result.success(adminAuditService.getAdminOperationAudits(operatorId, query));
    }

    @GetMapping("/permission-changes")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:audit:permission:view')")
    public Result<PageResult<PermissionChangeVO>> getPermissionChanges(@Valid PermissionChangeQueryDTO query) {
        Long operatorId = UserContextHolder.getUserId();
        return Result.success(adminAuditService.getPermissionChanges(operatorId, query));
    }

    @GetMapping("/login-traces")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:audit:login:view')")
    public Result<PageResult<LoginTraceVO>> getLoginTraces(@Valid LoginTraceQueryDTO query) {
        Long operatorId = UserContextHolder.getUserId();
        return Result.success(adminAuditService.getLoginTraces(operatorId, query));
    }

    @GetMapping("/overview")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:audit:operation:view')"
            + " || @iamAuth.hasPerm(authentication.principal.userId, 'admin:audit:permission:view')"
            + " || @iamAuth.hasPerm(authentication.principal.userId, 'admin:audit:login:view')")
    public Result<AuditOverviewVO> getAuditOverview(
            @RequestParam(value = "days", required = false) @Min(1) @Max(365) Integer days) {
        Long operatorId = UserContextHolder.getUserId();
        return Result.success(adminAuditService.getAuditOverview(operatorId, days));
    }
}
