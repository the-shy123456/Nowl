package com.unimarket.admin.controller;

import com.unimarket.admin.dto.BehaviorControlUpsertDTO;
import com.unimarket.admin.dto.RiskCaseHandleDTO;
import com.unimarket.admin.dto.RiskCaseQueryDTO;
import com.unimarket.admin.dto.RiskEventQueryDTO;
import com.unimarket.admin.dto.RiskRuleUpsertDTO;
import com.unimarket.admin.service.AdminRiskCenterService;
import com.unimarket.admin.service.AdminRiskService;
import com.unimarket.admin.vo.RiskCaseVO;
import com.unimarket.admin.vo.RiskEventVO;
import com.unimarket.admin.vo.RiskRuleVO;
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
 * 管理后台-风控管理接口
 */
@RestController
@RequestMapping("/admin/risk")
@RequiredArgsConstructor
@Validated
public class AdminRiskController {

    private final AdminRiskService adminRiskService;
    private final AdminRiskCenterService adminRiskCenterService;

    /**
     * 查询某个用户的行为管控规则
     */
    @GetMapping("/behavior-control/{userId}")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:risk:behavior:view')")
    public Result<List<UserBehaviorControlVO>> listUserBehaviorControls(@PathVariable Long userId) {
        Long operatorId = UserContextHolder.getUserId();
        return Result.success(adminRiskService.listUserBehaviorControls(operatorId, userId));
    }

    /**
     * 新增或更新用户行为管控
     */
    @PutMapping("/behavior-control")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:risk:behavior:manage')")
    public Result<Void> upsertBehaviorControl(@Valid @RequestBody BehaviorControlUpsertDTO dto) {
        Long operatorId = UserContextHolder.getUserId();
        adminRiskService.upsertBehaviorControl(operatorId, dto);
        return Result.success();
    }

    /**
     * 关闭某条行为管控
     */
    @DeleteMapping("/behavior-control/{controlId}")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:risk:behavior:manage')")
    public Result<Void> disableBehaviorControl(@PathVariable Long controlId) {
        Long operatorId = UserContextHolder.getUserId();
        adminRiskService.disableBehaviorControl(operatorId, controlId);
        return Result.success();
    }

    /**
     * 分页查询风控事件
     */
    @GetMapping("/events")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:event:view')")
    public Result<PageResult<RiskEventVO>> getRiskEvents(@Valid RiskEventQueryDTO query) {
        Long operatorId = UserContextHolder.getUserId();
        return Result.success(adminRiskCenterService.getRiskEventPage(operatorId, query));
    }

    /**
     * 分页查询风控工单
     */
    @GetMapping("/cases")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:case:handle')")
    public Result<PageResult<RiskCaseVO>> getRiskCases(@Valid RiskCaseQueryDTO query) {
        Long operatorId = UserContextHolder.getUserId();
        return Result.success(adminRiskCenterService.getRiskCasePage(operatorId, query));
    }

    /**
     * 处理风控工单
     */
    @PutMapping("/case/handle")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:case:handle')")
    public Result<Void> handleRiskCase(@Valid @RequestBody RiskCaseHandleDTO dto) {
        Long operatorId = UserContextHolder.getUserId();
        adminRiskCenterService.handleRiskCase(operatorId, dto);
        return Result.success();
    }

    /**
     * 分页查询风控规则
     */
    @GetMapping("/rules")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:rule:manage')")
    public Result<PageResult<RiskRuleVO>> getRiskRules(@Valid PageQuery query,
                                                       @RequestParam(required = false) String eventType) {
        return Result.success(adminRiskCenterService.getRiskRulePage(query, eventType));
    }

    /**
     * 新增/更新风控规则
     */
    @PutMapping("/rule")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:rule:manage')")
    public Result<Void> upsertRiskRule(@Valid @RequestBody RiskRuleUpsertDTO dto) {
        adminRiskCenterService.upsertRiskRule(dto);
        return Result.success();
    }

    /**
     * 启用/禁用风控规则
     */
    @PutMapping("/rule/{ruleId}/status")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'risk:rule:manage')")
    public Result<Void> updateRiskRuleStatus(@PathVariable Long ruleId, @RequestParam @Min(0) @Max(1) Integer status) {
        adminRiskCenterService.updateRiskRuleStatus(ruleId, status);
        return Result.success();
    }
}
