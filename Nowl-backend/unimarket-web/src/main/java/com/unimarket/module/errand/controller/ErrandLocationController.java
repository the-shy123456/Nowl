package com.unimarket.module.errand.controller;

import com.unimarket.common.result.Result;
import com.unimarket.module.errand.dto.LocationUploadDTO;
import com.unimarket.module.errand.service.ErrandService;
import com.unimarket.security.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "跑腿位置管理")
@RestController
@RequestMapping("/errand/location")
@RequiredArgsConstructor
@Slf4j
public class ErrandLocationController {

    private final ErrandService errandService;

    @Operation(summary = "上报位置")
    @PostMapping("/upload")
    @PreAuthorize("@bizAuth.canUploadErrandLocation(#dto.taskId, authentication.principal.userId)")
    public Result<Void> uploadLocation(@Valid @RequestBody LocationUploadDTO dto) {
        Long userId = UserContextHolder.getUserId();
        errandService.uploadLocation(userId, dto);
        return Result.success();
    }

    @Operation(summary = "获取位置")
    @GetMapping("/{taskId}")
    @PreAuthorize("@bizAuth.canViewErrandLocation(#taskId, authentication.principal.userId)")
    public Result<String> getLocation(@PathVariable Long taskId) {
        return Result.success(errandService.getLocation(taskId));
    }
}
