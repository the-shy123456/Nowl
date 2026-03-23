package com.unimarket.module.errand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 跑腿任务VO
 */
@Data
public class ErrandVO {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务内容
     */
    private String taskContent;

    /**
     * 任务图片（JSON格式）
     */
    private String imageList;

    /**
     * 取件地址
     */
    private String pickupAddress;

    /**
     * 送达地址
     */
    private String deliveryAddress;

    /**
     * 报酬金额
     */
    private BigDecimal reward;

    /**
     * 任务状态：0-待接单，1-进行中，2-待确认，3-已完成，4-已取消
     */
    private Integer taskStatus;

    /**
     * 审核状态：0-待审核，1-AI审核通过，2-人工审核通过，3-违规驳回，4-待人工复核
     */
    private Integer reviewStatus;

    /**
     * 审核状态文本
     */
    private String reviewStatusText;

    /**
     * 审核原因（驳回/复核说明）
     */
    private String auditReason;

    /**
     * 状态描述文本
     */
    private String statusText;

    /**
     * 发布者ID
     */
    private Long publisherId;

    /**
     * 发布者昵称
     */
    private String publisherName;

    /**
     * 发布者头像
     */
    private String publisherAvatar;

    /**
     * 接单者ID
     */
    private Long acceptorId;

    /**
     * 接单者昵称
     */
    private String acceptorName;

    /**
     * 接单者头像
     */
    private String acceptorAvatar;

    /**
     * 截止时间
     */
    private LocalDateTime deadline;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 完成凭证图片URL
     */
    private String evidenceImage;

    /**
     * 接单时间
     */
    private LocalDateTime acceptTime;

    /**
     * 送达时间
     */
    private LocalDateTime deliverTime;

    /**
     * 确认时间
     */
    private LocalDateTime confirmTime;

    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;

    /**
     * 取消原因
     */
    private String cancelReason;

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
     * 实时纬度 (仅详情展示用)
     */
    private BigDecimal currentLatitude;

    /**
     * 实时经度 (仅详情展示用)
     */
    private BigDecimal currentLongitude;

    /**
     * 是否存在进行中的纠纷（待处理/处理中）
     */
    private Boolean hasActiveDispute;

    /**
     * 进行中纠纷ID
     */
    private Long activeDisputeId;

    /**
     * 进行中纠纷状态：0-待处理，1-处理中
     */
    private Integer activeDisputeStatus;

    /**
     * 最近一次已处理纠纷ID
     */
    private Long latestClosedDisputeId;

    /**
     * 最近一次已处理纠纷状态：2-已解决，3-已驳回
     */
    private Integer latestClosedDisputeStatus;

    /**
     * 最近一次已处理纠纷结果摘要
     */
    private String latestClosedDisputeResult;

    /**
     * 最近一次已处理纠纷实际退款金额
     */
    private BigDecimal latestClosedDisputeRefundAmount;

    /**
     * 最近一次已处理纠纷实际扣除信用分
     */
    private Integer latestClosedDisputeCreditPenalty;
}
