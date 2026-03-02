package com.unimarket.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户行为管控新增/更新请求
 */
@Data
public class BehaviorControlUpsertDTO {

    @NotNull(message = "目标用户不能为空")
    private Long userId;

    /**
     * 行为类型: LOGIN/GOODS_PUBLISH/ERRAND_PUBLISH/ERRAND_ACCEPT/CHAT_SEND/AI_CHAT_SEND/FOLLOW_USER/ALL
     */
    @NotBlank(message = "行为类型不能为空")
    private String eventType;

    /**
     * 管控动作: ALLOW/REJECT/REVIEW/LIMIT/CHALLENGE
     */
    @NotBlank(message = "管控动作不能为空")
    private String controlAction;

    private String reason;

    private LocalDateTime expireTime;
}

