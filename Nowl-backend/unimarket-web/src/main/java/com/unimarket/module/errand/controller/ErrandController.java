package com.unimarket.module.errand.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.common.result.PageQuery;
import com.unimarket.common.result.PageResult;
import com.unimarket.common.result.Result;

import com.unimarket.module.errand.dto.ErrandPublishDTO;
import com.unimarket.module.errand.dto.ErrandQueryDTO;
import com.unimarket.module.errand.service.ErrandService;
import com.unimarket.module.errand.vo.ErrandVO;
import com.unimarket.security.UserContextHolder;
import cn.hutool.core.util.StrUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 跑腿任务Controller
 */
@RestController
@RequestMapping("/errand")
@RequiredArgsConstructor
@Validated
public class ErrandController {

    private final ErrandService errandService;

    /**
     * 发布跑腿任务
     * 权限：仅已认证用户可发布
     */
    @PostMapping
    @PreAuthorize("@bizAuth.canPublishErrand(authentication.principal.userId)")
    public Result<Void> publishErrand(@Valid @RequestBody ErrandPublishDTO dto) {
        Long userId = UserContextHolder.getUserId();
        dto.setSchoolCode(UserContextHolder.getSchoolCode());
        if (dto.getCampusCode() == null || dto.getCampusCode().isEmpty()) {
            dto.setCampusCode(UserContextHolder.getCampusCode());
        }
        errandService.publishErrand(userId, dto);
        return Result.success();
    }

    /**
     * 查询跑腿任务列表
     */
    @GetMapping("/list")
    public Result<PageResult<ErrandVO>> getErrandList(@Valid ErrandQueryDTO dto) {
        if (UserContextHolder.isAuthenticated() && StrUtil.isBlank(dto.getSchoolCode())) {
            dto.setSchoolCode(UserContextHolder.getSchoolCode());
            dto.setCampusCode(UserContextHolder.getCampusCode());
        }
        Page<ErrandVO> page = errandService.getErrandList(dto);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    /**
     * 查询跑腿任务详情
     */
    @GetMapping("/{taskId}")
    public Result<ErrandVO> getErrandDetail(@PathVariable Long taskId) {
        return Result.success(errandService.getErrandDetail(taskId));
    }

    /**
     * 修改跑腿任务（仅发布者且待接单）
     */
    @PutMapping("/{taskId}")
    @PreAuthorize("@bizAuth.canEditErrand(#taskId, authentication.principal.userId)")
    public Result<Void> updateErrand(@PathVariable Long taskId, @Valid @RequestBody ErrandPublishDTO dto) {
        Long userId = UserContextHolder.getUserId();
        if (dto.getCampusCode() == null || dto.getCampusCode().isEmpty()) {
            dto.setCampusCode(UserContextHolder.getCampusCode());
        }
        if (dto.getSchoolCode() == null || dto.getSchoolCode().isEmpty()) {
            dto.setSchoolCode(UserContextHolder.getSchoolCode());
        }
        errandService.updateErrand(userId, taskId, dto);
        return Result.success();
    }

    /**
     * 接单
     * 权限：已认证且为跑腿员
     */
    @PostMapping("/{taskId}/accept")
    @PreAuthorize("@bizAuth.canAcceptErrand(#taskId, authentication.principal.userId)")
    public Result<Void> acceptErrand(@PathVariable Long taskId) {
        Long userId = UserContextHolder.getUserId();
        errandService.acceptErrand(userId, taskId);
        return Result.success();
    }

    /**
     * 送达任务（接单人上传凭证）
     * 权限：仅接单人可操作，且任务状态为进行中
     */
    @PutMapping("/{taskId}/deliver")
    @PreAuthorize("@bizAuth.canDeliverErrand(#taskId, authentication.principal.userId)")
    public Result<Void> deliverErrand(@PathVariable Long taskId, @RequestParam String evidenceImage) {
        errandService.deliverErrand(taskId, evidenceImage);
        return Result.success();
    }

    /**
     * 确认完成（发布者确认）
     * 权限：仅发布者可操作，且任务状态为待确认
     */
    @PutMapping("/{taskId}/confirm")
    @PreAuthorize("@bizAuth.canConfirmErrand(#taskId, authentication.principal.userId)")
    public Result<Void> confirmErrand(@PathVariable Long taskId) {
        errandService.confirmErrand(taskId);
        return Result.success();
    }

    /**
     * 取消任务
     * 权限：待接单时仅发布者可取消，进行中时双方都可取消
     */
    @DeleteMapping("/{taskId}")
    @PreAuthorize("@bizAuth.canCancelErrand(#taskId, authentication.principal.userId)")
    public Result<Void> cancelErrand(@PathVariable Long taskId,
                                      @RequestParam(required = false) String reason) {
        errandService.cancelErrand(taskId, reason);
        return Result.success();
    }

    /**
     * 查询我发布的跑腿任务
     */
    @GetMapping("/my/published")
    public Result<PageResult<ErrandVO>> getMyPublishedErrands(@Valid PageQuery query) {
        Long userId = UserContextHolder.getUserId();
        Page<ErrandVO> page = errandService.getMyPublishedErrands(userId, query);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    /**
     * 查询我接的跑腿任务
     */
    @GetMapping("/my/accepted")
    public Result<PageResult<ErrandVO>> getMyAcceptedErrands(@Valid PageQuery query) {
        Long userId = UserContextHolder.getUserId();
        Page<ErrandVO> page = errandService.getMyAcceptedErrands(userId, query);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal()));
    }
}
