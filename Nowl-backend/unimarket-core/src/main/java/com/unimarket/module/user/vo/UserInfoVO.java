package com.unimarket.module.user.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

/**
 * 用户信息VO
 */
@Data
public class UserInfoVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 头像URL
     */
    private String imageUrl;

    /**
     * 真实姓名
     */
    private String userName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 学校编码
     */
    private String schoolCode;

    /**
     * 校区编码
     */
    private String campusCode;

    /**
     * 学校名称
     */
    private String schoolName;

    /**
     * 校区名称
     */
    private String campusName;

    /**
     * 认证状态：0-未认证，1-待审核，2-通过，3-拒绝
     */
    private Integer authStatus;

    /**
     * 信用分
     */
    private Integer creditScore;

    /**
     * 账号状态：0-正常，1-封禁
     */
    private Integer accountStatus;

    /**
     * 跑腿认证状态：0-未认证，1-已认证
     */
    private Integer runnableStatus;

    /**
     * 账户余额
     */
    private BigDecimal money;

    /**
     * 关注数
     */
    private Integer followCount;

    /**
     * 粉丝数
     */
    private Integer fanCount;

    /**
     * 证件照URL
     */
    private String certImage;

    /**
     * 本人照URL
     */
    private String selfImage;

    /**
     * 用户类型：0-游客，1-学生，2-教职工
     */
    private Integer userType;

    /**
     * 角色编码集合（IAM）
     */
    private Set<String> roleCodes;

    /**
     * 权限点集合（IAM）
     */
    private Set<String> permissionCodes;

}
