package com.unimarket.common.interceptor;

import com.alibaba.fastjson2.JSON;
import com.unimarket.common.security.HttpRequestIpResolver;
import com.unimarket.module.audit.entity.AuditAdminOperation;
import com.unimarket.module.audit.mapper.AuditAdminOperationMapper;
import com.unimarket.security.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 管理后台操作审计拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuditInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "adminAuditStartTime";

    private final AuditAdminOperationMapper auditAdminOperationMapper;
    private final HttpRequestIpResolver httpRequestIpResolver;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (isAdminPath(request)) {
            request.setAttribute(START_TIME, System.currentTimeMillis());
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        if (!isAdminPath(request)) {
            return;
        }

        try {
            Long start = (Long) request.getAttribute(START_TIME);
            int cost = (int) (start == null ? 0L : (System.currentTimeMillis() - start));
            int statusCode = response.getStatus();

            AuditAdminOperation op = new AuditAdminOperation();
            op.setTraceId(UUID.randomUUID().toString().replace("-", ""));
            op.setOperatorId(UserContextHolder.getUserId() == null ? 0L : UserContextHolder.getUserId());
            op.setOperatorIp(resolveIp(request));
            op.setCreateTime(LocalDateTime.now());
            op.setCostMs(cost);

            String uri = request.getRequestURI();
            String[] parts = Arrays.stream(uri.split("/"))
                    .filter(s -> !s.isBlank())
                    .toArray(String[]::new);
            String module = parts.length >= 2 ? parts[1] : "admin";
            String action = parts.length >= 3
                    ? String.join(":", Arrays.copyOfRange(parts, 2, parts.length))
                    : request.getMethod().toLowerCase();
            op.setModule(module);
            op.setAction(action);

            TargetRef targetRef = resolveTarget(parts, request.getParameterMap());
            op.setTargetType(targetRef.targetType);
            op.setTargetId(targetRef.targetId);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("method", request.getMethod());
            payload.put("uri", request.getRequestURI());
            payload.put("queryString", request.getQueryString() == null ? null : "<redacted>");
            payload.put("params", request.getParameterMap());
            op.setRequestPayload(JSON.toJSONString(payload));

            if (ex == null && statusCode < 400) {
                op.setResultStatus("SUCCESS");
                op.setResultMessage(null);
            } else {
                op.setResultStatus("FAIL");
                op.setResultMessage(ex == null ? "HTTP_" + statusCode : ex.getMessage());
            }

            auditAdminOperationMapper.insert(op);
        } catch (Exception e) {
            log.warn("写入后台审计日志失败: {}", e.getMessage());
        }
    }

    private boolean isAdminPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/admin");
    }

    private String resolveIp(HttpServletRequest request) {
        return httpRequestIpResolver.resolve(request);
    }

    private TargetRef resolveTarget(String[] pathParts, Map<String, String[]> params) {
        String targetType = pathParts.length >= 2 ? pathParts[1].toUpperCase() : "UNKNOWN";
        String targetId = firstParam(params, "userId", "goodsId", "orderId", "taskId", "disputeId", "controlId");
        if (targetId == null && pathParts.length > 0) {
            String maybeId = pathParts[pathParts.length - 1];
            if (maybeId.matches("\\d+")) {
                targetId = maybeId;
            }
        }
        return new TargetRef(targetType, targetId);
    }

    private String firstParam(Map<String, String[]> params, String... keys) {
        for (String key : keys) {
            String[] values = params.get(key);
            if (values != null && values.length > 0 && values[0] != null && !values[0].isBlank()) {
                return values[0];
            }
        }
        return null;
    }

    private record TargetRef(String targetType, String targetId) {
    }
}
