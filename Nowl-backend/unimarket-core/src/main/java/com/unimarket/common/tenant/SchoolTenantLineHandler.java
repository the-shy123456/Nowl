package com.unimarket.common.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.unimarket.security.UserContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 学校/校区多租户处理器
 * 实现数据隔离规则：
 * 1. 游客（未登录或未认证）：可以看全部数据，不过滤
 * 2. 已认证学生：默认看本学校本校区数据，可手动切换校区
 * 3. 不能查看不同学校的数据
 */
@Component
public class SchoolTenantLineHandler implements TenantLineHandler {

    /**
     * 需要进行租户过滤的表
     */
    private static final Set<String> TENANT_TABLES = new HashSet<>(Arrays.asList(
            "goods_info",
            "errand_task"
    ));

    @Override
    public String getTenantIdColumn() {
        return "school_code";
    }

    /**
     * 需要跳过租户隔离的维护接口（后台已做IAM范围控制）
     */
    private static final Set<String> MAINTAIN_PATHS = new HashSet<>(Arrays.asList(
            "/search/sync/full",
            "/search/index/create",
            "/search/errand/sync/full",
            "/search/errand/index/create"
    ));

    @Override
    public Expression getTenantId() {
        // 获取当前用户的学校编码
        String schoolCode = UserContextHolder.getSchoolCode();
        if (schoolCode == null || schoolCode.isEmpty()) {
            // 游客返回null，表示不过滤
            return new NullValue();
        }
        return new StringValue(schoolCode);
    }


    @Override
    public boolean ignoreTable(String tableName) {
        // 管理后台接口、搜索索引维护接口由IAM范围控制，不叠加租户过滤
        if (isManagementRequest()) {
            return true;
        }

        // 超级管理员放行租户过滤，避免全局运营任务被限制在单校
        if (hasSuperAdminRole()) {
            return true;
        }

        // 如果是游客（未登录或未认证），不进行租户过滤
        if (UserContextHolder.isGuest()) {
            return true;
        }

        // 如果用户学校编码为空，不过滤
        String schoolCode = UserContextHolder.getSchoolCode();
        if (schoolCode == null || schoolCode.isEmpty()) {
            return true;
        }

        // 仅对白名单中的表进行过滤，其他表均不过滤 (Default Ignore Strategy)
        return !TENANT_TABLES.contains(tableName.toLowerCase());
    }

    private boolean hasSuperAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
    }

    private boolean isManagementRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return false;
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String uri = request.getRequestURI();
        if (uri == null || uri.isBlank()) {
            return false;
        }
        if ("/admin".equals(uri) || uri.startsWith("/admin/")) {
            return true;
        }
        return MAINTAIN_PATHS.contains(uri);
    }
}
