package com.unimarket.module.notice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统通知实体类
 */
@Data
@TableName("sys_notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long noticeId;

    private Long userId; // 接收者ID

    private String title;

    private String content;

    private Integer type; // 0-系统通知, 1-交易通知, 2-评价通知, 3-纠纷通知

    private Long relatedId; // 关联ID

    private Integer isRead; // 0-未读, 1-已读

    private LocalDateTime createTime;
}
