package com.unimarket.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户上下文信息
 * 用于在ThreadLocal中存储当前请求的用户信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 学校编码
     */
    private String schoolCode;

    /**
     * 校区编码
     */
    private String campusCode;

    /**
     * 认证状态：0-未认证，1-待审核，2-通过，3-拒绝
     */
    private Integer authStatus;

    /**
     * 当前选择的校区编码（用于切换校区查看）
     */
    private String selectedCampusCode;
}
