package com.unimarket.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.admin.dto.AdminOperationAuditQueryDTO;
import com.unimarket.admin.dto.LoginTraceQueryDTO;
import com.unimarket.admin.dto.PermissionChangeQueryDTO;
import com.unimarket.admin.service.AdminAuditService;
import com.unimarket.admin.vo.AdminOperationAuditVO;
import com.unimarket.admin.vo.AuditOverviewVO;
import com.unimarket.admin.vo.LoginTraceVO;
import com.unimarket.admin.vo.PermissionChangeVO;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.common.result.PageResult;
import com.unimarket.module.audit.entity.AuditAdminOperation;
import com.unimarket.module.audit.entity.AuditLoginTrace;
import com.unimarket.module.audit.entity.AuditPermissionChange;
import com.unimarket.module.audit.mapper.AuditAdminOperationMapper;
import com.unimarket.module.audit.mapper.AuditLoginTraceMapper;
import com.unimarket.module.audit.mapper.AuditPermissionChangeMapper;
import com.unimarket.module.iam.entity.IamAdminScopeBinding;
import com.unimarket.module.iam.service.IamAccessService;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理后台-审计中心服务实现
 */
@Service
@RequiredArgsConstructor
public class AdminAuditServiceImpl implements AdminAuditService {

    private static final String SCOPE_ALL = "ALL";
    private static final String SCOPE_SCHOOL = "SCHOOL";
    private static final String SCOPE_CAMPUS = "CAMPUS";

    private final AuditAdminOperationMapper auditAdminOperationMapper;
    private final AuditPermissionChangeMapper auditPermissionChangeMapper;
    private final AuditLoginTraceMapper auditLoginTraceMapper;
    private final IamAccessService iamAccessService;
    private final UserInfoMapper userInfoMapper;

    @Override
    public PageResult<AdminOperationAuditVO> getAdminOperationAudits(Long operatorId, AdminOperationAuditQueryDTO query) {
        List<IamAdminScopeBinding> scopes = getOperatorScopes(operatorId);
        LambdaQueryWrapper<AuditAdminOperation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getOperatorId() != null, AuditAdminOperation::getOperatorId, query.getOperatorId())
                .eq(StrUtil.isNotBlank(query.getModule()), AuditAdminOperation::getModule, query.getModule())
                .eq(StrUtil.isNotBlank(query.getAction()), AuditAdminOperation::getAction, query.getAction())
                .eq(StrUtil.isNotBlank(query.getResultStatus()), AuditAdminOperation::getResultStatus, query.getResultStatus())
                .ge(query.getStartTime() != null, AuditAdminOperation::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, AuditAdminOperation::getCreateTime, query.getEndTime())
                .orderByDesc(AuditAdminOperation::getCreateTime);

        if (!containsAllScope(scopes)) {
            wrapper.eq(AuditAdminOperation::getOperatorId, operatorId);
        }

        Page<AuditAdminOperation> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<AuditAdminOperation> result = auditAdminOperationMapper.selectPage(page, wrapper);
        List<AdminOperationAuditVO> records = result.getRecords().stream()
                .map(item -> BeanUtil.copyProperties(item, AdminOperationAuditVO.class))
                .toList();
        return PageResult.of(records, result.getTotal());
    }

    @Override
    public PageResult<PermissionChangeVO> getPermissionChanges(Long operatorId, PermissionChangeQueryDTO query) {
        List<IamAdminScopeBinding> scopes = getOperatorScopes(operatorId);
        Set<Long> manageableUserIds = resolveManageableUserIds(scopes);

        LambdaQueryWrapper<AuditPermissionChange> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getOperatorId() != null, AuditPermissionChange::getOperatorId, query.getOperatorId())
                .eq(query.getTargetUserId() != null, AuditPermissionChange::getTargetUserId, query.getTargetUserId())
                .eq(StrUtil.isNotBlank(query.getChangeType()), AuditPermissionChange::getChangeType, query.getChangeType())
                .ge(query.getStartTime() != null, AuditPermissionChange::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, AuditPermissionChange::getCreateTime, query.getEndTime())
                .orderByDesc(AuditPermissionChange::getCreateTime);

        if (!containsAllScope(scopes)) {
            if (manageableUserIds.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0L);
            }
            wrapper.in(AuditPermissionChange::getTargetUserId, manageableUserIds);
        }

        Page<AuditPermissionChange> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<AuditPermissionChange> result = auditPermissionChangeMapper.selectPage(page, wrapper);
        List<PermissionChangeVO> records = result.getRecords().stream()
                .map(item -> BeanUtil.copyProperties(item, PermissionChangeVO.class))
                .toList();
        return PageResult.of(records, result.getTotal());
    }

    @Override
    public PageResult<LoginTraceVO> getLoginTraces(Long operatorId, LoginTraceQueryDTO query) {
        List<IamAdminScopeBinding> scopes = getOperatorScopes(operatorId);
        Set<Long> manageableUserIds = resolveManageableUserIds(scopes);

        LambdaQueryWrapper<AuditLoginTrace> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getUserId() != null, AuditLoginTrace::getUserId, query.getUserId())
                .eq(StrUtil.isNotBlank(query.getPhone()), AuditLoginTrace::getPhone, query.getPhone())
                .eq(StrUtil.isNotBlank(query.getLoginResult()), AuditLoginTrace::getLoginResult, query.getLoginResult())
                .eq(StrUtil.isNotBlank(query.getRiskLevel()), AuditLoginTrace::getRiskLevel, query.getRiskLevel())
                .ge(query.getStartTime() != null, AuditLoginTrace::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, AuditLoginTrace::getCreateTime, query.getEndTime())
                .orderByDesc(AuditLoginTrace::getCreateTime);

        if (!containsAllScope(scopes)) {
            if (manageableUserIds.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0L);
            }
            wrapper.in(AuditLoginTrace::getUserId, manageableUserIds);
        }

        Page<AuditLoginTrace> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<AuditLoginTrace> result = auditLoginTraceMapper.selectPage(page, wrapper);
        List<LoginTraceVO> records = result.getRecords().stream()
                .map(item -> BeanUtil.copyProperties(item, LoginTraceVO.class))
                .toList();
        return PageResult.of(records, result.getTotal());
    }

    @Override
    public AuditOverviewVO getAuditOverview(Long operatorId, Integer days) {
        List<IamAdminScopeBinding> scopes = getOperatorScopes(operatorId);
        boolean allScope = containsAllScope(scopes);
        int safeDays = days == null || days <= 0 ? 7 : Math.min(days, 60);
        LocalDateTime startTime = LocalDateTime.now().minusDays(safeDays);

        AuditOverviewVO vo = new AuditOverviewVO();
        vo.setWindowDays(safeDays);

        LambdaQueryWrapper<AuditAdminOperation> operationWrapper = new LambdaQueryWrapper<AuditAdminOperation>()
                .ge(AuditAdminOperation::getCreateTime, startTime);
        if (!allScope) {
            operationWrapper.eq(AuditAdminOperation::getOperatorId, operatorId);
        }

        Long totalOperations = auditAdminOperationMapper.selectCount(operationWrapper);
        Long failedOperations = auditAdminOperationMapper.selectCount(new LambdaQueryWrapper<AuditAdminOperation>()
                .ge(AuditAdminOperation::getCreateTime, startTime)
                .eq(!allScope, AuditAdminOperation::getOperatorId, operatorId)
                .ne(AuditAdminOperation::getResultStatus, "SUCCESS"));

        Set<Long> manageableUserIds = resolveManageableUserIds(scopes);
        LambdaQueryWrapper<AuditPermissionChange> permissionWrapper = new LambdaQueryWrapper<AuditPermissionChange>()
                .ge(AuditPermissionChange::getCreateTime, startTime);
        LambdaQueryWrapper<AuditLoginTrace> loginWrapper = new LambdaQueryWrapper<AuditLoginTrace>()
                .ge(AuditLoginTrace::getCreateTime, startTime);

        if (!allScope) {
            if (manageableUserIds.isEmpty()) {
                vo.setTotalOperations(defaultLong(totalOperations));
                vo.setFailedOperations(defaultLong(failedOperations));
                vo.setPermissionChanges(0L);
                vo.setLoginAttempts(0L);
                vo.setLoginFailures(0L);
                vo.setHighRiskLoginCount(0L);
                vo.setLastOperationTime(findLastOperationTime(operatorId));
                return vo;
            }
            permissionWrapper.in(AuditPermissionChange::getTargetUserId, manageableUserIds);
            loginWrapper.in(AuditLoginTrace::getUserId, manageableUserIds);
        }

        Long permissionChanges = auditPermissionChangeMapper.selectCount(permissionWrapper);
        Long loginAttempts = auditLoginTraceMapper.selectCount(loginWrapper);
        Long loginFailures = auditLoginTraceMapper.selectCount(new LambdaQueryWrapper<AuditLoginTrace>()
                .ge(AuditLoginTrace::getCreateTime, startTime)
                .in(!allScope, AuditLoginTrace::getUserId, manageableUserIds)
                .ne(AuditLoginTrace::getLoginResult, "SUCCESS"));
        Long highRiskLogins = auditLoginTraceMapper.selectCount(new LambdaQueryWrapper<AuditLoginTrace>()
                .ge(AuditLoginTrace::getCreateTime, startTime)
                .in(!allScope, AuditLoginTrace::getUserId, manageableUserIds)
                .in(AuditLoginTrace::getRiskLevel, List.of("high", "critical", "HIGH", "CRITICAL")));

        vo.setTotalOperations(defaultLong(totalOperations));
        vo.setFailedOperations(defaultLong(failedOperations));
        vo.setPermissionChanges(defaultLong(permissionChanges));
        vo.setLoginAttempts(defaultLong(loginAttempts));
        vo.setLoginFailures(defaultLong(loginFailures));
        vo.setHighRiskLoginCount(defaultLong(highRiskLogins));
        vo.setLastOperationTime(findLastOperationTime(allScope ? null : operatorId));
        return vo;
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

    private Set<Long> resolveManageableUserIds(List<IamAdminScopeBinding> scopes) {
        if (containsAllScope(scopes)) {
            return Collections.emptySet();
        }

        Set<Long> userIds = new HashSet<>();
        for (IamAdminScopeBinding scope : scopes) {
            if (SCOPE_SCHOOL.equalsIgnoreCase(scope.getScopeType()) && StrUtil.isNotBlank(scope.getSchoolCode())) {
                List<UserInfo> users = userInfoMapper.selectList(new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getSchoolCode, scope.getSchoolCode()));
                userIds.addAll(users.stream().map(UserInfo::getUserId).collect(Collectors.toSet()));
            }
            if (SCOPE_CAMPUS.equalsIgnoreCase(scope.getScopeType())
                    && StrUtil.isNotBlank(scope.getSchoolCode())
                    && StrUtil.isNotBlank(scope.getCampusCode())) {
                List<UserInfo> users = userInfoMapper.selectList(new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getSchoolCode, scope.getSchoolCode())
                        .eq(UserInfo::getCampusCode, scope.getCampusCode()));
                userIds.addAll(users.stream().map(UserInfo::getUserId).collect(Collectors.toSet()));
            }
        }
        return userIds;
    }

    private LocalDateTime findLastOperationTime(Long operatorId) {
        AuditAdminOperation latest = auditAdminOperationMapper.selectOne(new LambdaQueryWrapper<AuditAdminOperation>()
                .eq(operatorId != null, AuditAdminOperation::getOperatorId, operatorId)
                .orderByDesc(AuditAdminOperation::getCreateTime)
                .last("LIMIT 1"));
        return latest == null ? null : latest.getCreateTime();
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}
