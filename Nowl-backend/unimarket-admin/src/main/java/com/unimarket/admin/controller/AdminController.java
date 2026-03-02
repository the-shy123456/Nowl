package com.unimarket.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.admin.dto.NoticeBroadcastDTO;
import com.unimarket.admin.service.AdminService;
import com.unimarket.admin.vo.DisputeVO;
import com.unimarket.admin.vo.DashboardStatsVO;
import com.unimarket.common.result.PageQuery;
import com.unimarket.common.result.PageResult;
import com.unimarket.common.result.Result;
import com.unimarket.module.errand.vo.ErrandVO;
import com.unimarket.module.goods.vo.GoodsVO;
import com.unimarket.module.order.vo.OrderVO;
import com.unimarket.module.user.vo.UserInfoVO;
import com.unimarket.security.UserContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台Controller
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Validated
public class AdminController {

    private final AdminService adminService;

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/dashboard/stats")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:dashboard:view')")
    public Result<DashboardStatsVO> getDashboardStats() {
        Long operatorId = UserContextHolder.getUserId();
        return Result.success(adminService.getDashboardStats(operatorId));
    }

    /**
     * 审核商品
     */
    @PostMapping("/goods/audit")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:goods:audit')")
    public Result<Void> auditGoods(@RequestParam Long goodsId, @RequestParam Integer status, @RequestParam(required = false) String reason) {
        Long operatorId = UserContextHolder.getUserId();
        adminService.auditGoods(operatorId, goodsId, status, reason);
        return Result.success();
    }

    /**
     * 获取待审核商品列表
     */
    @GetMapping("/goods/pending")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:goods:pending:view')")
    public Result<PageResult<GoodsVO>> getPendingAuditGoods(@Valid PageQuery query,
                                                            @RequestParam(required = false) String schoolCode,
                                                            @RequestParam(required = false) String campusCode,
                                                            @RequestParam(required = false) Integer reviewStatus) {
        Long operatorId = UserContextHolder.getUserId();
        Page<GoodsVO> page = adminService.getPendingAuditGoods(operatorId, query, schoolCode, campusCode, reviewStatus);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    /**
     * 获取全部商品列表（支持关键词搜索）
     */
    @GetMapping("/goods/list")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:goods:list:view')")
    public Result<PageResult<GoodsVO>> getAllGoods(@Valid PageQuery query,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String schoolCode,
                                                   @RequestParam(required = false) String campusCode,
                                                   @RequestParam(required = false) Integer tradeStatus,
                                                   @RequestParam(required = false) Integer reviewStatus) {
        Long operatorId = UserContextHolder.getUserId();
        Page<GoodsVO> page = adminService.getAllGoods(operatorId, query, keyword, schoolCode, campusCode, tradeStatus, reviewStatus);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    /**
     * 强制下架商品
     */
    @PostMapping("/goods/offline")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:goods:offline')")
    public Result<Void> forceOfflineGoods(@RequestParam Long goodsId, @RequestParam(required = false) String reason) {
        Long operatorId = UserContextHolder.getUserId();
        adminService.forceOfflineGoods(operatorId, goodsId, reason != null ? reason : "管理员操作");
        return Result.success();
    }

    /**
     * 更新用户状态（封禁/解封）
     */
    @PutMapping("/user/status")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:user:status:update')")
    public Result<Void> updateUserStatus(@RequestParam Long userId, @RequestParam Integer status) {
        Long operatorId = UserContextHolder.getUserId();
        adminService.updateUserStatus(operatorId, userId, status);
        return Result.success();
    }

    /**
     * 获取用户列表（支持关键词搜索）
     */
    @GetMapping("/user/list")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:user:list:view')")
    public Result<PageResult<UserInfoVO>> getUserList(@Valid PageQuery query,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String schoolCode,
                                                      @RequestParam(required = false) String campusCode,
                                                      @RequestParam(required = false) Integer accountStatus,
                                                      @RequestParam(required = false) Integer authStatus) {
        Long operatorId = UserContextHolder.getUserId();
        Page<UserInfoVO> page = adminService.getUserList(operatorId, query, keyword, schoolCode, campusCode, accountStatus, authStatus);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    /**
     * 获取待认证用户列表
     */
    @GetMapping("/auth/pending")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:auth:pending:view')")
    public Result<PageResult<UserInfoVO>> getPendingAuthUsers(@Valid PageQuery query,
                                                              @RequestParam(required = false) String schoolCode,
                                                              @RequestParam(required = false) String campusCode) {
        Long operatorId = UserContextHolder.getUserId();
        Page<UserInfoVO> page = adminService.getPendingAuthUsers(operatorId, query, schoolCode, campusCode);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    /**
     * 获取待审核跑腿员列表
     */
    @GetMapping("/runner/pending")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:runner:pending:view')")
    public Result<PageResult<UserInfoVO>> getPendingRunnerUsers(@Valid PageQuery query,
                                                                @RequestParam(required = false) String schoolCode,
                                                                @RequestParam(required = false) String campusCode) {
        Long operatorId = UserContextHolder.getUserId();
        Page<UserInfoVO> page = adminService.getPendingRunnerUsers(operatorId, query, schoolCode, campusCode);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    /**
     * 审核跑腿员
     */
    @PostMapping("/runner/audit")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:runner:audit')")
    public Result<Void> auditRunner(@RequestParam Long userId, @RequestParam Integer status, @RequestParam(required = false) String reason) {
        Long operatorId = UserContextHolder.getUserId();
        adminService.auditRunner(operatorId, userId, status, reason);
        return Result.success();
    }

    /**
     * 审核用户认证
     */
    @PostMapping("/auth/audit")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:auth:audit')")
    public Result<Void> auditUserAuth(@RequestParam Long userId, @RequestParam Integer status, @RequestParam(required = false) String reason) {
        Long operatorId = UserContextHolder.getUserId();
        adminService.auditUserAuth(operatorId, userId, status, reason);
        return Result.success();
    }

    /**
     * 获取订单列表（支持关键词搜索）
     */
    @GetMapping("/order/list")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:order:list:view')")
    public Result<PageResult<OrderVO>> getOrderList(@Valid PageQuery query,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String schoolCode,
                                                    @RequestParam(required = false) String campusCode,
                                                    @RequestParam(required = false) Integer orderStatus) {
        Long operatorId = UserContextHolder.getUserId();
        Page<OrderVO> page = adminService.getOrderList(operatorId, query, keyword, schoolCode, campusCode, orderStatus);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    /**
     * 获取纠纷列表
     */
    @GetMapping("/dispute/list")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:dispute:list:view')")
    public Result<PageResult<DisputeVO>> getDisputeList(@Valid PageQuery query,
                                                        @RequestParam(required = false) Integer status,
                                                        @RequestParam(required = false) String schoolCode,
                                                        @RequestParam(required = false) String campusCode) {
        Long operatorId = UserContextHolder.getUserId();
        Page<DisputeVO> page = adminService.getDisputeList(operatorId, query, status, schoolCode, campusCode);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    /**
     * 处理纠纷
     */
    @PostMapping("/dispute/handle")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:dispute:handle')")
    public Result<Void> handleDispute(
            @RequestParam Long disputeId,
            @RequestParam String result,
            @RequestParam(defaultValue = "2") Integer handleStatus,
            @RequestParam(required = false) Integer deductCreditScore,
            @RequestParam(required = false) java.math.BigDecimal refundAmount
    ) {
        Long operatorId = UserContextHolder.getUserId();
        adminService.handleDispute(operatorId, disputeId, result, handleStatus, deductCreditScore, refundAmount);
        return Result.success();
    }

    /**
     * 获取跑腿任务列表（管理员，支持搜索和状态筛选）
     */
    @GetMapping("/errand/list")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:errand:list:view')")
    public Result<PageResult<ErrandVO>> getAdminErrandList(@Valid PageQuery query,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) Integer status,
                                                            @RequestParam(required = false) Integer reviewStatus,
                                                            @RequestParam(required = false) String schoolCode,
                                                            @RequestParam(required = false) String campusCode) {
        Long operatorId = UserContextHolder.getUserId();
        Page<ErrandVO> page = adminService.getAdminErrandList(operatorId, query, keyword, status, reviewStatus, schoolCode, campusCode);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    /**
     * 跑腿任务人工复核
     */
    @PostMapping("/errand/audit")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:errand:audit')")
    public Result<Void> auditErrand(@RequestParam Long taskId,
                                    @RequestParam Integer status,
                                    @RequestParam(required = false) String reason) {
        Long operatorId = UserContextHolder.getUserId();
        adminService.auditErrand(operatorId, taskId, status, reason);
        return Result.success();
    }

    /**
     * 获取用户详情（含信用分）
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:user:list:view')")
    public Result<UserInfoVO> getUserDetail(@PathVariable Long userId) {
        Long operatorId = UserContextHolder.getUserId();
        return Result.success(adminService.getUserDetail(operatorId, userId));
    }

    /**
     * 调整用户信用分
     */
    @PutMapping("/user/credit")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:user:status:update')")
    public Result<Void> adjustUserCredit(@RequestParam Long userId, @RequestParam int change, @RequestParam String reason) {
        Long operatorId = UserContextHolder.getUserId();
        adminService.adjustUserCreditScore(operatorId, userId, change, reason);
        return Result.success();
    }

    /**
     * 系统通知广播
     */
    @PostMapping("/notice/broadcast")
    @PreAuthorize("@iamAuth.hasPerm(authentication.principal.userId, 'admin:notice:broadcast')")
    public Result<Void> broadcastNotice(@Valid @RequestBody NoticeBroadcastDTO body) {
        Long operatorId = UserContextHolder.getUserId();
        adminService.broadcastNotice(
                operatorId,
                body.getTitle(),
                body.getContent(),
                body.getSchoolCode(),
                body.getCampusCode()
        );
        return Result.success();
    }
}
