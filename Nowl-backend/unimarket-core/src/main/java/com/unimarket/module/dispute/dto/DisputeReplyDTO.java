package com.unimarket.module.dispute.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 纠纷补充说明/证据DTO
 */
@Data
public class DisputeReplyDTO {

    /**
     * 纠纷记录ID
     */
    @NotNull(message = "纠纷ID不能为空")
    private Long recordId;

    /**
     * 补充说明
     */
    @Length(max = 500, message = "补充说明不能超过500个字符")
    private String additionalContent;

    /**
     * 补充证据URL（JSON格式）
     */
    private String additionalEvidence;

}
