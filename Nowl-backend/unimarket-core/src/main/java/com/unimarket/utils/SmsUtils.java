package com.unimarket.utils;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.unimarket.common.config.SmsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 短信发送工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsUtils {

    private final SmsProperties smsProperties;

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 是否发送成功
     */
    public boolean sendSmsCode(String phone, String code) {
        log.info("【短信发送】手机号: {}", maskPhone(phone));

        if (!smsProperties.isEnabled()) {
            log.info("【短信发送】短信服务已禁用，跳过发送");
            return true;
        }

        try {
            // 构建请求参数
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("code", code);
            paramMap.put("to", phone);
            // 补充name字段，防止模板依赖该变量
            paramMap.put("name", "UniMarket");

            String jsonBody = JSONUtil.toJsonStr(paramMap);

            log.info("正在调用Spug短信接口, phone={}", maskPhone(phone));

            // 发送POST请求
            String result = HttpUtil.post(smsProperties.getUrl(), jsonBody);

            log.info("Spug响应结果: {}", result);

            // 解析返回结果
            cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(result);
            if (jsonObject.getInt("code") == 200) {
                return true;
            } else {
                log.error("短信发送失败: {}", jsonObject.getStr("msg"));
                return false;
            }
        } catch (Exception e) {
            log.error("短信发送异常", e);
            return false;
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
