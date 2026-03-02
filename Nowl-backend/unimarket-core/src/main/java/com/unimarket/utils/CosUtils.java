package com.unimarket.utils;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 腾讯云COS工具类
 */
@Slf4j
@Component
public class CosUtils {

    @Value("${cos.secret-id}")
    private String secretId;

    @Value("${cos.secret-key}")
    private String secretKey;

    @Value("${cos.region}")
    private String regionName;

    @Value("${cos.bucket-name}")
    private String bucketName;

    @Value("${cos.base-url}")
    private String baseUrl;

    private COSClient cosClient;

    @PostConstruct
    public void init() {
        // 1 初始化用户身份信息 (secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 2 设置 bucket 的地域
        Region region = new Region(regionName);
        ClientConfig clientConfig = new ClientConfig(region);
        // 3 生成 cos 客户端
        cosClient = new COSClient(cred, clientConfig);
        log.info("腾讯云COS客户端初始化成功, region={}, bucket={}", regionName, bucketName);
    }

    /**
     * 上传文件
     * @param inputStream 文件流
     * @param originalFilename 原始文件名
     * @param size 文件大小
     * @param contentType 文件类型
     * @return 文件访问URL
     */
    public String upload(InputStream inputStream, String originalFilename, long size, String contentType) {
        return upload(inputStream, originalFilename, size, contentType, "uploads/");
    }

    public String upload(InputStream inputStream, String originalFilename, long size, String contentType, String keyPrefix) {
        try {
            // 生成唯一文件名
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
            String normalizedPrefix = normalizePrefix(keyPrefix);
            String key = normalizedPrefix + UUID.randomUUID().toString() + extension;

            // 设置元数据
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(size);
            objectMetadata.setContentType(contentType);

            // 上传
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, inputStream, objectMetadata);
            cosClient.putObject(putObjectRequest);

            // 返回URL
            return getNormalizedBaseUrl() + "/" + key;
        } catch (Exception e) {
            log.error("COS上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件
     * @param file 文件
     * @return 文件访问URL
     */
    public String upload(MultipartFile file) {
        return upload(file, "uploads/");
    }

    public String upload(MultipartFile file, String keyPrefix) {
        try {
            return upload(file.getInputStream(), file.getOriginalFilename(), file.getSize(), file.getContentType(), keyPrefix);
        } catch (Exception e) {
            log.error("COS上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     * @param fileUrl 文件URL
     */
    public void delete(String fileUrl) {
        if (fileUrl == null) {
            return;
        }
        try {
            String key = extractKey(fileUrl);
            cosClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            log.error("COS删除失败, fileUrl: {}", fileUrl, e);
            throw new RuntimeException("文件删除失败: " + e.getMessage());
        }
    }

    public boolean isOwnedByUser(String fileUrl, Long userId) {
        if (userId == null || userId <= 0 || fileUrl == null) {
            return false;
        }
        try {
            String key = extractKey(fileUrl);
            return key.startsWith("uploads/" + userId + "/");
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public String extractKey(String fileUrl) {
        String normalizedBaseUrl = getNormalizedBaseUrl();
        if (fileUrl == null || !fileUrl.startsWith(normalizedBaseUrl + "/")) {
            throw new IllegalArgumentException("fileUrl is invalid");
        }
        String key = fileUrl.substring((normalizedBaseUrl + "/").length());
        int queryIdx = key.indexOf('?');
        if (queryIdx >= 0) {
            key = key.substring(0, queryIdx);
        }
        int fragmentIdx = key.indexOf('#');
        if (fragmentIdx >= 0) {
            key = key.substring(0, fragmentIdx);
        }
        if (key.isBlank()) {
            throw new IllegalArgumentException("fileUrl is invalid");
        }
        return key;
    }

    /**
     * 列出指定前缀下、早于指定时间的文件URL
     * @param prefix 前缀（如 "uploads/"）
     * @param before 截止时间，只返回早于此时间的文件
     * @param maxKeys 最大返回数量
     * @return 文件URL列表
     */
    public List<String> listObjectUrls(String prefix, Date before, int maxKeys) {
        List<String> urls = new ArrayList<>();
        String marker = null;
        String normalizedBase = getNormalizedBaseUrl();
        do {
            ListObjectsRequest req = new ListObjectsRequest();
            req.setBucketName(bucketName);
            req.setPrefix(prefix);
            req.setMaxKeys(Math.min(maxKeys - urls.size(), 1000));
            if (marker != null) {
                req.setMarker(marker);
            }
            ObjectListing listing = cosClient.listObjects(req);
            for (COSObjectSummary summary : listing.getObjectSummaries()) {
                if (before != null && summary.getLastModified().after(before)) {
                    continue;
                }
                urls.add(normalizedBase + "/" + summary.getKey());
                if (urls.size() >= maxKeys) {
                    return urls;
                }
            }
            marker = listing.isTruncated() ? listing.getNextMarker() : null;
        } while (marker != null && urls.size() < maxKeys);
        return urls;
    }

    private String normalizePrefix(String keyPrefix) {
        if (keyPrefix == null || keyPrefix.isBlank()) {
            return "uploads/";
        }
        String normalized = keyPrefix.trim();
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private String getNormalizedBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("baseUrl is not configured");
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
