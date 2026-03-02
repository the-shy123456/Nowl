package com.unimarket.module.common.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件服务接口
 */
public interface FileService {

    /**
     * 单文件上传
     */
    String uploadFile(Long userId, MultipartFile file);

    /**
     * 批量文件上传
     */
    List<String> uploadFiles(Long userId, MultipartFile[] files);

    /**
     * 删除文件
     */
    void deleteFile(Long userId, String fileUrl);
}
