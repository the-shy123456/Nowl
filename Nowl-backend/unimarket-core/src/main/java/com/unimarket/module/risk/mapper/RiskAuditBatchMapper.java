package com.unimarket.module.risk.mapper;

import com.unimarket.module.risk.entity.RiskCase;
import com.unimarket.module.risk.entity.RiskDecision;
import com.unimarket.module.risk.entity.RiskEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 风控批量落库 Mapper。
 */
@Mapper
public interface RiskAuditBatchMapper {

    void batchInsertEvents(@Param("items") List<RiskEvent> items);

    void batchInsertDecisions(@Param("items") List<RiskDecision> items);

    void batchInsertCases(@Param("items") List<RiskCase> items);
}
