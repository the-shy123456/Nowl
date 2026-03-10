package com.unimarket.admin.controller;

import com.unimarket.admin.dto.BehaviorControlUpsertDTO;
import com.unimarket.admin.dto.RiskCaseHandleDTO;
import com.unimarket.admin.dto.RiskCaseQueryDTO;
import com.unimarket.admin.dto.RiskEventQueryDTO;
import com.unimarket.admin.dto.RiskModeUpdateDTO;
import com.unimarket.admin.dto.RiskRuleUpsertDTO;
import com.unimarket.admin.dto.RiskSubjectListQueryDTO;
import com.unimarket.admin.dto.RiskSubjectListUpsertDTO;
import com.unimarket.admin.service.AdminRiskCenterService;
import com.unimarket.admin.service.AdminRiskService;
import com.unimarket.admin.vo.RiskCaseVO;
import com.unimarket.admin.vo.RiskEventVO;
import com.unimarket.admin.vo.RiskModeVO;
import com.unimarket.admin.vo.RiskRuleVO;
import com.unimarket.admin.vo.RiskSubjectListVO;
import com.unimarket.admin.vo.UserBehaviorControlVO;
import com.unimarket.common.result.PageQuery;
import com.unimarket.common.result.PageResult;
import com.unimarket.common.result.Result;
import com.unimarket.security.UserContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
 * 管理后台-风控管理接口。
 */
@RestController
@RequestMapping("/admin/risk")
@RequiredArgsConstructor
@Validated
public class AdminRiskController {

    private final AdminRiskService adminRiskService;
    private final AdminRiskCenterService adminRiskCenterService;

    @GetMapping("/behavior-control/{userId}")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:risk:behavior:view')")
    public Result<List<UserBehaviorControlVO>> listUserBehaviorControls(@PathVariable Long userId) {
        Long operatorId = UserContextHolder.getUserId();
        return Result.success(adminRiskService.listUserBehaviorControls(operatorId, userId));
    }

    @PutMapping("/behavior-control")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:risk:behavior:manage')")
    public Result<Void> upsertBehaviorControl(@Valid @RequestBody BehaviorControlUpsertDTO dto) {
        Long operatorId = UserContextHolder.getUserId();
        adminRiskService.upsertBehaviorControl(operatorId, dto);
        return Result.success();
    }

    @DeleteMapping("/behavior-control/{controlId}")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:risk:behavior:manage')")
    public Result<Void> disableBehaviorControl(@PathVariable Long controlId) {
        Long operatorId = UserContextHolder.getUserId();
        adminRiskService.disableBehaviorControl(operatorId, controlId);
        return Result.success();
    }

    @GetMapping("/mode")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:mode:view')")
    public Result<RiskModeVO> getRiskMode() {
        Long operatorId = UserContextHolder.getUserId();
        return Result.success(adminRiskCenterService.getRiskMode(operatorId));
    }

    @PutMapping("/mode")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:mode:manage')")
    public Result<Void> updateRiskMode(@Valid @RequestBody RiskModeUpdateDTO dto) {
        Long operatorId = UserContextHolder.getUserId();
        adminRiskCenterService.updateRiskMode(operatorId, dto);
        return Result.success();
    }

    @GetMapping("/blacklist")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:list:view')")
    public Result<PageResult<RiskSubjectListVO>> getBlacklist(@Valid RiskSubjectListQueryDTO query) {
        Long operatorId = UserContextHolder.getUserId();
        return Result.success(adminRiskCenterService.getBlacklistPage(operatorId, query));
    }

    @PutMapping("/blacklist")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:list:manage')")
    public Result<Void> upsertBlacklist(@Valid @RequestBody RiskSubjectListUpsertDTO dto) {
        Long operatorId = UserContextHolder.getUserId();
        adminRiskCenterService.upsertBlacklist(operatorId, dto);
        return Result.success();
    }

    @PutMapping("/blacklist/{id}/status")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:list:manage')")
    public Result<Void> updateBlacklistStatus(@PathVariable Long id, @RequestParam @Min(0) @Max(1) Integer status) {
        Long operatorId = UserContextHolder.getUserId();
        adminRiskCenterService.updateBlacklistStatus(operatorId, id, status);
        return Result.success();
    }

    @GetMapping("/whitelist")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:list:view')")
    public Result<PageResult<RiskSubjectListVO>> getWhitelist(@Valid RiskSubjectListQueryDTO query) {
        Long operatorId = UserContextHolder.getUserId();
        return Result.success(adminRiskCenterService.getWhitelistPage(operatorId, query));
    }

    @PutMapping("/whitelist")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:list:manage')")
    public Result<Void> upsertWhitelist(@Valid @RequestBody RiskSubjectListUpsertDTO dto) {
        Long operatorId = UserContextHolder.getUserId();
        adminRiskCenterService.upsertWhitelist(operatorId, dto);
        return Result.success();
    }

    @PutMapping("/whitelist/{id}/status")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:list:manage')")
    public Result<Void> updateWhitelistStatus(@PathVariable Long id, @RequestParam @Min(0) @Max(1) Integer status) {
        Long operatorId = UserContextHolder.getUserId();
        adminRiskCenterService.updateWhitelistStatus(operatorId, id, status);
        return Result.success();
    }

    @GetMapping("/events")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:event:view')")
    public Result<PageResult<RiskEventVO>> getRiskEvents(@Valid RiskEventQueryDTO query) {
        Long operatorId = UserContextHolder.getUserId();
        return Result.success(adminRiskCenterService.getRiskEventPage(operatorId, query));
    }

    @GetMapping("/cases")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:case:handle')")
    public Result<PageResult<RiskCaseVO>> getRiskCases(@Valid RiskCaseQueryDTO query) {
        Long operatorId = UserContextHolder.getUserId();
        return Result.success(adminRiskCenterService.getRiskCasePage(operatorId, query));
    }

    @PutMapping("/case/handle")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:case:handle')")
    public Result<Void> handleRiskCase(@Valid @RequestBody RiskCaseHandleDTO dto) {
        Long operatorId = UserContextHolder.getUserId();
        adminRiskCenterService.handleRiskCase(operatorId, dto);
        return Result.success();
    }

    @GetMapping("/rules")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:rule:manage')")
    public Result<PageResult<RiskRuleVO>> getRiskRules(@Valid PageQuery query,
                                                       @RequestParam(required = false) String eventType) {
        return Result.success(adminRiskCenterService.getRiskRulePage(query, eventType));
    }

    @PutMapping("/rule")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:rule:manage')")
    public Result<Void> upsertRiskRule(@Valid @RequestBody RiskRuleUpsertDTO dto) {
        adminRiskCenterService.upsertRiskRule(dto);
        return Result.success();
    }

    @PutMapping("/rule/{ruleId}/status")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:rule:manage')")
    public Result<Void> updateRiskRuleStatus(@PathVariable Long ruleId, @RequestParam @Min(0) @Max(1) Integer status) {
        adminRiskCenterService.updateRiskRuleStatus(ruleId, status);
        return Result.success();
    }
}
