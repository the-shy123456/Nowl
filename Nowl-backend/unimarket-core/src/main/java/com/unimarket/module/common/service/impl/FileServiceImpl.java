package com.unimarket.module.common.service.impl;

import cn.hutool.core.util.StrUtil;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.common.result.ResultCode;
import com.unimarket.module.common.service.FileService;
import com.unimarket.utils.CosUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 文件服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final String USER_UPLOAD_PREFIX = "uploads/%d/";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

    private final CosUtils cosUtils;

    @Override
    public String uploadFile(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文件不能为空");
        }
        try {
            return uploadDirect(userId, file);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR.getCode(), "文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> uploadFiles(Long userId, MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文件不能为空");
        }

        List<String> fileUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            try {
                fileUrls.add(uploadDirect(userId, file));
            } catch (IOException e) {
                log.error("批量上传部分失败", e);
                throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR.getCode(), "批量上传失败: " + e.getMessage());
            }
        }
        return fileUrls;
    }

    @Override
    public void deleteFile(Long userId, String fileUrl) {
        if (StrUtil.isBlank(fileUrl)) {
            return;
        }
        if (!cosUtils.isOwnedByUser(fileUrl, userId)) {
            throw new BusinessException(ResultCode.NOT_POWER.getCode(), "无权限删除该文件");
        }
        cosUtils.delete(fileUrl);
    }

    private String uploadDirect(Long userId, MultipartFile file) throws IOException {
        validateUploadFile(file);
        String keyPrefix = resolveUserKeyPrefix(userId);
        return cosUtils.upload(file, keyPrefix);
    }

    private String resolveUserKeyPrefix(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ResultCode.USER_NOT_LOGIN);
        }
        return String.format(USER_UPLOAD_PREFIX, userId);
    }

    private void validateUploadFile(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "仅支持上传 JPG/PNG/WEBP/GIF 图片");
        }

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (StrUtil.isNotBlank(originalName) && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文件扩展名不合法");
        }

        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文件内容不是有效图片");
            }
        }
    }
}
