package com.unimarket.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信服务配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms.spug")
public class SmsProperties {

    /**
     * Spug短信发送接口URL
     */
    private String url;

    /**
     * 是否启用短信发送
     */
    private boolean enabled = true;
}
