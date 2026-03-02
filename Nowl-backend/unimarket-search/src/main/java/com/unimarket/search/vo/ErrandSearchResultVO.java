package com.unimarket.search.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 跑腿搜索结果VO
 */
@Data
public class ErrandSearchResultVO {

    private Long taskId;

    private String title;

    private String description;

    private String taskContent;

    private BigDecimal reward;

    private Integer taskStatus;

    private String statusText;

    private Long publisherId;

    private String publisherName;

    private String publisherAvatar;

    private String schoolCode;

    private String campusCode;

    private String pickupAddress;

    private String deliveryAddress;

    private String image;

    private LocalDateTime deadline;

    private LocalDateTime createTime;

    private Float score;
}
