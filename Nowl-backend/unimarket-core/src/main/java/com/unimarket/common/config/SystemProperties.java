package com.unimarket.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 系统配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "system")
public class SystemProperties {

    /**
     * 默认头像URL
     */
    private String defaultAvatar;

    /**
     * 文件上传路径
     */
    private String uploadPath;
}
