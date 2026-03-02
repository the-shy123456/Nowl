package com.unimarket.module.notice.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统通知返回对象
 */
@Data
public class NoticeVO {

    private Long noticeId;

    private String title;

    private String content;

    private Integer type;

    private Long relatedId;

    /**
     * 业务目标类型：system/product/order/errand/dispute/review
     */
    private String bizType;

    private Integer isRead;

    private LocalDateTime createTime;
}
