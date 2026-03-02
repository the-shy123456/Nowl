package com.unimarket.module.dispute.controller;

import com.unimarket.common.result.PageResult;
import com.unimarket.common.result.Result;
import com.unimarket.module.dispute.dto.DisputeCreateDTO;
import com.unimarket.module.dispute.dto.DisputeReplyDTO;
import com.unimarket.module.dispute.service.DisputeService;
import com.unimarket.module.dispute.vo.DisputeDetailVO;
import com.unimarket.module.dispute.vo.DisputeListItemVO;
import com.unimarket.security.UserContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 纠纷Controller（用户端）
 */
@Slf4j
@RestController
@RequestMapping("/dispute")
@RequiredArgsConstructor
@Validated
public class DisputeController {

    private final DisputeService disputeService;

    /**
     * 发起纠纷
     */
    @PostMapping
    @PreAuthorize("@bizAuth.canCreateDispute(authentication.principal.userId)")
    public Result<Void> create(@Valid @RequestBody DisputeCreateDTO dto) {
        Long userId = UserContextHolder.getUserId();
        disputeService.createDispute(userId, dto);
        return Result.success();
    }

    /**
     * 获取我的纠纷列表
     */
    @GetMapping("/list")
    public Result<PageResult<DisputeListItemVO>> getMyDisputes(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize,
            @RequestParam(required = false) Integer handleStatus) {
        Long userId = UserContextHolder.getUserId();
        PageResult<DisputeListItemVO> result = disputeService.getMyDisputes(userId, pageNum, pageSize, handleStatus);
        return Result.success(result);
    }

    /**
     * 获取纠纷详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("@bizAuth.canViewDispute(#id, authentication.principal.userId)")
    public Result<DisputeDetailVO> getDetail(@PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        DisputeDetailVO detail = disputeService.getDisputeDetail(userId, id);
        return Result.success(detail);
    }

    /**
     * 撤回纠纷
     */
    @PutMapping("/{id}/withdraw")
    @PreAuthorize("@bizAuth.canWithdrawDispute(#id, authentication.principal.userId)")
    public Result<Void> withdraw(@PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        disputeService.withdrawDispute(userId, id);
        return Result.success();
    }

    /**
     * 补充证据
     */
    @PostMapping("/evidence")
    @PreAuthorize("@bizAuth.canAddDisputeEvidence(#dto.recordId, authentication.principal.userId)")
    public Result<Void> addEvidence(@Valid @RequestBody DisputeReplyDTO dto) {
        Long userId = UserContextHolder.getUserId();
        disputeService.addEvidence(userId, dto);
        return Result.success();
    }
}
