package com.unimarket.module.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注/粉丝用户VO
 */
@Data
public class FollowUserVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 头像
     */
    private String imageUrl;

    /**
     * 认证状态：0-未认证，1-待审核，2-已认证
     */
    private Integer authStatus;

    /**
     * 信用分
     */
    private Integer creditScore;

    /**
     * 学校名称
     */
    private String schoolName;

    /**
     * 校区名称
     */
    private String campusName;

    /**
     * 关注时间
     */
    private LocalDateTime followTime;

    /**
     * 是否互相关注
     */
    private Boolean isMutual;

    /**
     * 当前用户是否关注了该用户
     */
    private Boolean isFollowed;
}
