package com.unimarket.module.common.controller;

import com.unimarket.common.result.Result;
import com.unimarket.module.common.service.FileService;
import com.unimarket.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传Controller
 */
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 单文件上传
     */
    @PostMapping("/upload")
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        Long userId = UserContextHolder.getUserId();
        String url = fileService.uploadFile(userId, file);
        return Result.success(url);
    }
    
    /**
     * 多文件上传
     */
    @PostMapping("/upload/batch")
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<List<String>> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        Long userId = UserContextHolder.getUserId();
        List<String> urls = fileService.uploadFiles(userId, files);
        return Result.success(urls);
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/delete")
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<Void> deleteFile(@RequestParam("url") String fileUrl) {
        Long userId = UserContextHolder.getUserId();
        fileService.deleteFile(userId, fileUrl);
        return Result.success();
    }
}
