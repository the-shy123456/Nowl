package com.unimarket.module.ai.dto;

import com.unimarket.module.aiassistant.model.AiChatQueryContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI对话请求参数
 */
@Data
public class AiChatRequestDTO {

    @Size(max = 4000, message = "消息长度不能超过4000")
    private String message;

    @Size(max = 1024, message = "图片地址长度不能超过1024")
    private String imageUrl;

    @Valid
    private AiChatQueryContext queryContext;
}
