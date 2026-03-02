package com.unimarket.admin.service.impl.support;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.iam.entity.IamAdminScopeBinding;
import com.unimarket.module.iam.service.IamAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 管理员数据范围能力封装（按学校/校区过滤）。
 */
@Component
@RequiredArgsConstructor
public class AdminScopeSupport {

    private static final String SCOPE_ALL = "ALL";
    private static final String SCOPE_SCHOOL = "SCHOOL";
    private static final String SCOPE_CAMPUS = "CAMPUS";

    private final IamAccessService iamAccessService;

    public List<IamAdminScopeBinding> getOperatorScopes(Long operatorId) {
        List<IamAdminScopeBinding> scopes = iamAccessService.getAdminScopes(operatorId);
        if (scopes == null || scopes.isEmpty()) {
            throw new BusinessException("管理员未配置可管理范围");
        }
        return scopes;
    }

    public boolean containsAllScope(List<IamAdminScopeBinding> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return false;
        }
        return scopes.stream().anyMatch(scope -> SCOPE_ALL.equalsIgnoreCase(scope.getScopeType()));
    }

    public <T> void applyScopeFilter(LambdaQueryWrapper<T> wrapper,
                                     List<IamAdminScopeBinding> scopes,
                                     SFunction<T, String> schoolField,
                                     SFunction<T, String> campusField) {
        if (wrapper == null || scopes == null || scopes.isEmpty()) {
            return;
        }
        if (containsAllScope(scopes)) {
            return;
        }

        wrapper.and(scopeWrapper -> {
            boolean hasCondition = false;
            for (IamAdminScopeBinding scope : scopes) {
                if (SCOPE_SCHOOL.equalsIgnoreCase(scope.getScopeType()) && StrUtil.isNotBlank(scope.getSchoolCode())) {
                    if (hasCondition) {
                        scopeWrapper.or();
                    }
                    scopeWrapper.eq(schoolField, scope.getSchoolCode());
                    hasCondition = true;
                    continue;
                }
                if (SCOPE_CAMPUS.equalsIgnoreCase(scope.getScopeType())
                        && StrUtil.isNotBlank(scope.getSchoolCode())
                        && StrUtil.isNotBlank(scope.getCampusCode())) {
                    if (hasCondition) {
                        scopeWrapper.or();
                    }
                    scopeWrapper.eq(schoolField, scope.getSchoolCode())
                            .eq(campusField, scope.getCampusCode());
                    hasCondition = true;
                }
            }
            if (!hasCondition) {
                // 兜底：没有有效范围时返回空集，避免误放行
                scopeWrapper.eq(schoolField, "__NO_SCOPE__");
            }
        });
    }
}

