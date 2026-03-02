package com.unimarket.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.admin.dto.RiskCaseHandleDTO;
import com.unimarket.admin.dto.RiskCaseQueryDTO;
import com.unimarket.admin.dto.RiskEventQueryDTO;
import com.unimarket.admin.dto.RiskRuleUpsertDTO;
import com.unimarket.admin.service.AdminRiskCenterService;
import com.unimarket.admin.vo.RiskCaseVO;
import com.unimarket.admin.vo.RiskEventVO;
import com.unimarket.admin.vo.RiskRuleVO;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.common.result.PageQuery;
import com.unimarket.common.result.PageResult;
import com.unimarket.module.iam.entity.IamAdminScopeBinding;
import com.unimarket.module.iam.service.IamAccessService;
import com.unimarket.module.risk.entity.RiskCase;
import com.unimarket.module.risk.entity.RiskDecision;
import com.unimarket.module.risk.entity.RiskEvent;
import com.unimarket.module.risk.entity.RiskRule;
import com.unimarket.module.risk.enums.RiskAction;
import com.unimarket.module.risk.mapper.RiskCaseMapper;
import com.unimarket.module.risk.mapper.RiskDecisionMapper;
import com.unimarket.module.risk.mapper.RiskEventMapper;
import com.unimarket.module.risk.mapper.RiskRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理后台风控中心服务实现
 */
@Service
@RequiredArgsConstructor
public class AdminRiskCenterServiceImpl implements AdminRiskCenterService {

    private static final String SCOPE_ALL = "ALL";
    private static final String SCOPE_SCHOOL = "SCHOOL";
    private static final String SCOPE_CAMPUS = "CAMPUS";

    private final RiskEventMapper riskEventMapper;
    private final RiskDecisionMapper riskDecisionMapper;
    private final RiskCaseMapper riskCaseMapper;
    private final RiskRuleMapper riskRuleMapper;
    private final IamAccessService iamAccessService;

    @Override
    public PageResult<RiskEventVO> getRiskEventPage(Long operatorId, RiskEventQueryDTO query) {
        List<IamAdminScopeBinding> scopes = getOperatorScopes(operatorId);
        LambdaQueryWrapper<RiskEvent> wrapper = new LambdaQueryWrapper<>();
        String decisionAction = StrUtil.trimToNull(query.getDecisionAction());
        String riskLevel = StrUtil.trimToNull(query.getRiskLevel());

        wrapper.eq(StrUtil.isNotBlank(query.getEventType()), RiskEvent::getEventType, query.getEventType())
                .eq(StrUtil.isNotBlank(query.getSubjectType()), RiskEvent::getSubjectType, query.getSubjectType())
                .eq(StrUtil.isNotBlank(query.getSubjectId()), RiskEvent::getSubjectId, query.getSubjectId())
                .eq(StrUtil.isNotBlank(query.getSchoolCode()), RiskEvent::getSchoolCode, query.getSchoolCode())
                .eq(StrUtil.isNotBlank(query.getCampusCode()), RiskEvent::getCampusCode, query.getCampusCode())
                .ge(query.getStartTime() != null, RiskEvent::getEventTime, query.getStartTime())
                .le(query.getEndTime() != null, RiskEvent::getEventTime, query.getEndTime())
                .orderByDesc(RiskEvent::getEventTime);

        if (decisionAction != null && riskLevel != null) {
            wrapper.apply(
                    "EXISTS (SELECT 1 FROM risk_decision rd WHERE rd.event_id = event_id AND rd.decision_action = {0} AND rd.risk_level = {1})",
                    decisionAction.toUpperCase(),
                    riskLevel.toLowerCase());
        } else if (decisionAction != null) {
            wrapper.apply(
                    "EXISTS (SELECT 1 FROM risk_decision rd WHERE rd.event_id = event_id AND rd.decision_action = {0})",
                    decisionAction.toUpperCase());
        } else if (riskLevel != null) {
            wrapper.apply(
                    "EXISTS (SELECT 1 FROM risk_decision rd WHERE rd.event_id = event_id AND rd.risk_level = {0})",
                    riskLevel.toLowerCase());
        }

        applyScopeFilter(wrapper, scopes, RiskEvent::getSchoolCode, RiskEvent::getCampusCode);

        Page<RiskEvent> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<RiskEvent> eventPage = riskEventMapper.selectPage(page, wrapper);
        if (eventPage.getRecords().isEmpty()) {
            return PageResult.of(Collections.emptyList(), eventPage.getTotal());
        }

        List<Long> eventIds = eventPage.getRecords().stream().map(RiskEvent::getEventId).toList();
        List<RiskDecision> decisions = riskDecisionMapper.selectList(new LambdaQueryWrapper<RiskDecision>()
                .in(RiskDecision::getEventId, eventIds)
                .orderByDesc(RiskDecision::getDecisionId));
        Map<Long, RiskDecision> decisionMap = decisions.stream()
                .collect(Collectors.toMap(RiskDecision::getEventId, d -> d, (oldV, newV) -> oldV));

        List<RiskEventVO> records = eventPage.getRecords().stream()
                .map(event -> {
                    RiskEventVO vo = BeanUtil.copyProperties(event, RiskEventVO.class);
                    RiskDecision decision = decisionMap.get(event.getEventId());
                    if (decision != null) {
                        vo.setDecisionId(decision.getDecisionId());
                        vo.setDecisionAction(decision.getDecisionAction());
                        vo.setRiskLevel(decision.getRiskLevel());
                        BigDecimal score = decision.getRiskScore();
                        vo.setRiskScore(score == null ? null : score.doubleValue());
                        vo.setMatchedRuleCodes(decision.getMatchedRuleCodes());
                        vo.setDecisionReason(decision.getDecisionReason());
                    }
                    return vo;
                })
                .toList();

        return PageResult.of(records, eventPage.getTotal());
    }

    @Override
    public PageResult<RiskCaseVO> getRiskCasePage(Long operatorId, RiskCaseQueryDTO query) {
        List<IamAdminScopeBinding> scopes = getOperatorScopes(operatorId);
        LambdaQueryWrapper<RiskCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(query.getCaseStatus()), RiskCase::getCaseStatus, query.getCaseStatus())
                .eq(query.getAssigneeId() != null, RiskCase::getAssigneeId, query.getAssigneeId())
                .eq(StrUtil.isNotBlank(query.getSchoolCode()), RiskCase::getSchoolCode, query.getSchoolCode())
                .eq(StrUtil.isNotBlank(query.getCampusCode()), RiskCase::getCampusCode, query.getCampusCode())
                .ge(query.getStartTime() != null, RiskCase::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, RiskCase::getCreateTime, query.getEndTime())
                .orderByDesc(RiskCase::getCreateTime);

        applyScopeFilter(wrapper, scopes, RiskCase::getSchoolCode, RiskCase::getCampusCode);

        Page<RiskCase> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<RiskCase> casePage = riskCaseMapper.selectPage(page, wrapper);
        if (casePage.getRecords().isEmpty()) {
            return PageResult.of(Collections.emptyList(), casePage.getTotal());
        }

        List<Long> eventIds = casePage.getRecords().stream().map(RiskCase::getEventId).toList();
        List<RiskEvent> events = riskEventMapper.selectBatchIds(eventIds);
        Map<Long, RiskEvent> eventMap = events.stream().collect(Collectors.toMap(RiskEvent::getEventId, e -> e));

        List<Long> decisionIds = casePage.getRecords().stream()
                .map(RiskCase::getDecisionId)
                .filter(id -> id != null && id > 0)
                .toList();
        Map<Long, RiskDecision> decisionMap = decisionIds.isEmpty()
                ? Collections.emptyMap()
                : riskDecisionMapper.selectBatchIds(decisionIds).stream()
                .collect(Collectors.toMap(RiskDecision::getDecisionId, d -> d));

        List<RiskCaseVO> records = new ArrayList<>();
        for (RiskCase riskCase : casePage.getRecords()) {
            RiskCaseVO vo = BeanUtil.copyProperties(riskCase, RiskCaseVO.class);
            RiskEvent event = eventMap.get(riskCase.getEventId());
            if (event != null) {
                vo.setEventType(event.getEventType());
                vo.setSubjectType(event.getSubjectType());
                vo.setSubjectId(event.getSubjectId());
            }
            if (riskCase.getDecisionId() != null) {
                RiskDecision decision = decisionMap.get(riskCase.getDecisionId());
                if (decision != null) {
                    vo.setDecisionAction(decision.getDecisionAction());
                    vo.setRiskLevel(decision.getRiskLevel());
                }
            }
            records.add(vo);
        }
        return PageResult.of(records, casePage.getTotal());
    }

    @Override
    public void handleRiskCase(Long operatorId, RiskCaseHandleDTO dto) {
        RiskCase riskCase = riskCaseMapper.selectById(dto.getCaseId());
        if (riskCase == null) {
            throw new BusinessException("风控工单不存在");
        }
        iamAccessService.assertCanManageScope(operatorId, riskCase.getSchoolCode(), riskCase.getCampusCode());

        String caseStatus = dto.getCaseStatus().trim().toUpperCase();
        Set<String> validStatus = Set.of("OPEN", "PROCESSING", "CLOSED", "REJECTED");
        if (!validStatus.contains(caseStatus)) {
            throw new BusinessException("非法工单状态: " + caseStatus);
        }

        riskCase.setCaseStatus(caseStatus);
        riskCase.setResult(dto.getResult());
        riskCase.setResultReason(dto.getResultReason());
        riskCase.setAssigneeId(operatorId);
        riskCase.setUpdateTime(LocalDateTime.now());
        riskCaseMapper.updateById(riskCase);
    }

    @Override
    public PageResult<RiskRuleVO> getRiskRulePage(PageQuery query, String eventType) {
        LambdaQueryWrapper<RiskRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(eventType), RiskRule::getEventType, eventType)
                .orderByAsc(RiskRule::getPriority)
                .orderByAsc(RiskRule::getRuleId);
        Page<RiskRule> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<RiskRule> rulePage = riskRuleMapper.selectPage(page, wrapper);
        List<RiskRuleVO> records = rulePage.getRecords().stream()
                .map(rule -> BeanUtil.copyProperties(rule, RiskRuleVO.class))
                .toList();
        return PageResult.of(records, rulePage.getTotal());
    }

    @Override
    public void upsertRiskRule(RiskRuleUpsertDTO dto) {
        String action = dto.getDecisionAction().trim().toUpperCase();
        try {
            RiskAction.valueOf(action);
        } catch (Exception e) {
            throw new BusinessException("不支持的决策动作: " + action);
        }

        String ruleType = dto.getRuleType().trim().toUpperCase();
        if (!Set.of("THRESHOLD", "KEYWORD").contains(ruleType)) {
            throw new BusinessException("不支持的规则类型: " + ruleType);
        }

        RiskRule target;
        if (dto.getRuleId() == null) {
            target = new RiskRule();
            target.setStatus(1);
        } else {
            target = riskRuleMapper.selectById(dto.getRuleId());
            if (target == null) {
                throw new BusinessException("风控规则不存在");
            }
        }

        target.setRuleCode(dto.getRuleCode().trim());
        target.setRuleName(dto.getRuleName().trim());
        target.setEventType(dto.getEventType().trim().toUpperCase());
        target.setRuleType(ruleType);
        target.setRuleConfig(dto.getRuleConfig());
        target.setDecisionAction(action);
        target.setPriority(dto.getPriority() == null ? 100 : dto.getPriority());

        if (dto.getRuleId() == null) {
            riskRuleMapper.insert(target);
            return;
        }
        target.setUpdateTime(LocalDateTime.now());
        riskRuleMapper.updateById(target);
    }

    @Override
    public void updateRiskRuleStatus(Long ruleId, Integer status) {
        if (ruleId == null || status == null) {
            throw new BusinessException("参数不能为空");
        }
        if (status != 0 && status != 1) {
            throw new BusinessException("状态仅支持0/1");
        }
        RiskRule rule = riskRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new BusinessException("风控规则不存在");
        }
        rule.setStatus(status);
        rule.setUpdateTime(LocalDateTime.now());
        riskRuleMapper.updateById(rule);
    }

    private List<IamAdminScopeBinding> getOperatorScopes(Long operatorId) {
        List<IamAdminScopeBinding> scopes = iamAccessService.getAdminScopes(operatorId);
        if (scopes == null || scopes.isEmpty()) {
            throw new BusinessException("当前管理员未配置可管理范围");
        }
        return scopes;
    }

    private boolean containsAllScope(List<IamAdminScopeBinding> scopes) {
        return scopes.stream().anyMatch(scope -> SCOPE_ALL.equalsIgnoreCase(scope.getScopeType()));
    }

    private <T> void applyScopeFilter(LambdaQueryWrapper<T> wrapper,
                                      List<IamAdminScopeBinding> scopes,
                                      SFunction<T, String> schoolField,
                                      SFunction<T, String> campusField) {
        if (containsAllScope(scopes)) {
            return;
        }
        wrapper.and(group -> {
            boolean first = true;
            for (IamAdminScopeBinding scope : scopes) {
                String type = scope.getScopeType();
                if (SCOPE_SCHOOL.equalsIgnoreCase(type) && StrUtil.isNotBlank(scope.getSchoolCode())) {
                    if (!first) {
                        group.or();
                    }
                    group.eq(schoolField, scope.getSchoolCode());
                    first = false;
                    continue;
                }
                if (SCOPE_CAMPUS.equalsIgnoreCase(type)
                        && StrUtil.isNotBlank(scope.getSchoolCode())
                        && StrUtil.isNotBlank(scope.getCampusCode())) {
                    if (!first) {
                        group.or();
                    }
                    group.eq(schoolField, scope.getSchoolCode()).eq(campusField, scope.getCampusCode());
                    first = false;
                }
            }
            if (first) {
                group.apply("1 = 0");
            }
        });
    }
}
