package com.unimarket.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户信息实体类
 */
@Data
@TableName("user_info")
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户主键ID
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /**
     * 学号/教职工号（唯一）
     */
    private String studentNo;

    /**
     * 密码（加密存储）
     */
    private String password;

    /**
     * 用户头像URL
     */
    private String imageUrl;

    /**
     * 证件照URL
     */
    private String certImage;

    /**
     * 本人照URL
     */
    private String selfImage;

    /**
     * 用户真实姓名
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
     * 身份认证状态：0-未认证，1-待审核，2-通过，3-拒绝
     */
    private Integer authStatus;

    /**
     * 信用分（初始100分）
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
     * 性别：0-未知，1-男，2-女，3-其他
     */
    private Integer gender;

    /**
     * 年级（如：大三、研二）
     */
    private String grade;

    /**
     * 用户类型：0-游客，1-学生，2-教职工
     */
    private Integer userType;

    /**
     * 用户昵称（页面展示用）
     */
    private String nickName;

    /**
     * 账户余额（模拟支付）
     */
    private BigDecimal money;

    /**
     * 关注数（冗余缓存）
     */
    private Integer followCount;

    /**
     * 粉丝数（冗余缓存）
     */
    private Integer fanCount;

    /**
     * 用户创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 用户信息更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
