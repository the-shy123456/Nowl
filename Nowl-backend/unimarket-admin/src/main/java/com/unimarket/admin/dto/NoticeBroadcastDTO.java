package com.unimarket.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 系统通知广播请求
 */
@Data
public class NoticeBroadcastDTO {

    @NotBlank(message = "通知标题不能为空")
    @Size(max = 100, message = "通知标题长度不能超过100")
    private String title;

    @NotBlank(message = "通知内容不能为空")
    @Size(max = 2000, message = "通知内容长度不能超过2000")
    private String content;

    /**
     * 可选：只向指定学校广播（会与管理员可管辖范围求交集）
     */
    @Size(max = 32, message = "学校编码长度不能超过32")
    private String schoolCode;

    /**
     * 可选：只向指定校区广播（必须与schoolCode配合）
     */
    @Size(max = 32, message = "校区编码长度不能超过32")
    private String campusCode;
}
