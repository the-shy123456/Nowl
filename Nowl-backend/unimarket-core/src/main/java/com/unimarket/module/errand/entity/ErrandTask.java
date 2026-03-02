package com.unimarket.module.errand.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 跑腿任务实体类
 */
@Data
@TableName("errand_task")
public class ErrandTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID（主键）
     */
    @TableId(value = "task_id", type = IdType.AUTO)
    private Long taskId;

    /**
     * 发单人ID
     */
    private Long publisherId;

    /**
     * 接单人ID
     */
    private Long acceptorId;

    /**
     * 标题
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 截止时间
     */
    private LocalDateTime deadline;

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
     * 悬赏佣金
     */
    private BigDecimal reward;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 任务状态：0-待接单，1-进行中，2-待确认，3-已完成，4-已取消
     */
    private Integer taskStatus;

    /**
     * 审核状态：0-待审核，1-AI审核通过，2-人工审核通过，3-违规驳回，4-待人工复核
     */
    private Integer reviewStatus;

    /**
     * 审核原因（驳回/复核说明）
     */
    private String auditReason;

    /**
     * 学校编码
     */
    private String schoolCode;

    /**
     * 校区编码
     */
    private String campusCode;

    /**
     * 完成凭证图片URL
     */
    private String evidenceImage;

    /**
     * 接单时间
     */
    private LocalDateTime acceptTime;

    /**
     * 送达时间（跑腿员上传凭证时间）
     */
    private LocalDateTime deliverTime;

    /**
     * 确认时间（发布者确认完成时间）
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
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 取件纬度
     */
    private BigDecimal pickupLatitude;

    /**
     * 取件经度
     */
    private BigDecimal pickupLongitude;

    /**
     * 送达纬度
     */
    private BigDecimal deliveryLatitude;

    /**
     * 送达经度
     */
    private BigDecimal deliveryLongitude;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
