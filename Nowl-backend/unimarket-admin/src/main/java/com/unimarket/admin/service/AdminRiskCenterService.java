package com.unimarket.admin.service;

import com.unimarket.admin.dto.RiskCaseHandleDTO;
import com.unimarket.admin.dto.RiskCaseQueryDTO;
import com.unimarket.admin.dto.RiskEventQueryDTO;
import com.unimarket.admin.dto.RiskModeUpdateDTO;
import com.unimarket.admin.dto.RiskRuleUpsertDTO;
import com.unimarket.admin.dto.RiskSubjectListQueryDTO;
import com.unimarket.admin.dto.RiskSubjectListUpsertDTO;
import com.unimarket.admin.vo.RiskCaseVO;
import com.unimarket.admin.vo.RiskEventVO;
import com.unimarket.admin.vo.RiskModeVO;
import com.unimarket.admin.vo.RiskRuleVO;
import com.unimarket.admin.vo.RiskSubjectListVO;
import com.unimarket.common.result.PageQuery;
import com.unimarket.common.result.PageResult;

/**
 * 管理后台-风控中心服务。
 */
public interface AdminRiskCenterService {

    PageResult<RiskEventVO> getRiskEventPage(Long operatorId, RiskEventQueryDTO query);

    PageResult<RiskCaseVO> getRiskCasePage(Long operatorId, RiskCaseQueryDTO query);

    void handleRiskCase(Long operatorId, RiskCaseHandleDTO dto);

    PageResult<RiskRuleVO> getRiskRulePage(PageQuery query, String eventType);

    void upsertRiskRule(RiskRuleUpsertDTO dto);

    void updateRiskRuleStatus(Long ruleId, Integer status);

    RiskModeVO getRiskMode(Long operatorId);

    void updateRiskMode(Long operatorId, RiskModeUpdateDTO dto);

    PageResult<RiskSubjectListVO> getBlacklistPage(Long operatorId, RiskSubjectListQueryDTO query);

    PageResult<RiskSubjectListVO> getWhitelistPage(Long operatorId, RiskSubjectListQueryDTO query);

    void upsertBlacklist(Long operatorId, RiskSubjectListUpsertDTO dto);

    void upsertWhitelist(Long operatorId, RiskSubjectListUpsertDTO dto);

    void updateBlacklistStatus(Long operatorId, Long id, Integer status);

    void updateWhitelistStatus(Long operatorId, Long id, Integer status);
}
