package com.unimarket.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiAuditResult {
    private boolean safe;
    private String reason;
    /**
     * 风险等级: low-低风险, medium-中风险(需人工复核), high-高风险(直接拒绝)
     */
    private String riskLevel;

    public AiAuditResult(boolean safe, String reason) {
        this.safe = safe;
        this.reason = reason;
        this.riskLevel = safe ? "low" : "high";
    }
}
