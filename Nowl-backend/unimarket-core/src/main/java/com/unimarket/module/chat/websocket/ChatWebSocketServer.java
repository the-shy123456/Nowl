package com.unimarket.module.chat.websocket;

import cn.hutool.json.JSONUtil;
import com.unimarket.common.constant.CacheConstants;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.chat.dto.ChatMessageDTO;
import com.unimarket.module.chat.service.ChatService;
import com.unimarket.security.util.JwtUtils;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket聊天服务端
 */
@Slf4j
@Component
@ServerEndpoint(value = "/ws/chat", configurator = ChatWebSocketServer.HttpHeaderConfigurator.class)
public class ChatWebSocketServer {

    private static final Map<Long, Set<Session>> userSessionMap = new ConcurrentHashMap<>();
    private static final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    private static JwtUtils jwtUtils;
    private static org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private static ChatService chatService;

    @Autowired
    public void setJwtUtils(JwtUtils jwtUtils) {
        ChatWebSocketServer.jwtUtils = jwtUtils;
    }
    
    @Autowired
    public void setRedisTemplate(org.springframework.data.redis.core.StringRedisTemplate redisTemplate) {
        ChatWebSocketServer.redisTemplate = redisTemplate;
    }

    @Autowired
    public void setChatService(ChatService chatService) {
        ChatWebSocketServer.chatService = chatService;
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        try {
            String token = resolveAccessToken(endpointConfig);
            Long userId = authenticateUserId(token);
            if (userId == null) {
                closeUnauthorizedSession(session);
                return;
            }

            session.getUserProperties().put("userId", userId);
            sessionUserMap.put(session.getId(), userId);
            userSessionMap.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);

            log.info("WebSocket连接建立: userId={}, 当前在线会话数={}", userId, onlineSessionCount());
            pushOfflineMessages(session, userId);
        } catch (Exception e) {
            log.error("WebSocket连接认证失败", e);
            closeUnauthorizedSession(session);
        }
    }

    @OnClose
    public void onClose(Session session) {
        try {
            Long userId = resolveSessionUserId(session);
            removeSession(userId, session);
            log.info("WebSocket连接关闭: userId={}, 当前在线会话数={}", userId, onlineSessionCount());
        } catch (Exception e) {
            log.error("WebSocket关闭异常", e);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            Map<String, Object> payload = JSONUtil.toBean(message, Map.class);
            if (isPing(payload)) {
                session.getAsyncRemote().sendText(JSONUtil.toJsonStr(Map.of("type", "pong")));
                return;
            }

            Long senderId = resolveSessionUserId(session);
            if (senderId == null) {
                closeUnauthorizedSession(session);
                return;
            }

            ChatMessageDTO dto = toMessageDto(payload);
            if (dto.getReceiverId() == null || dto.getReceiverId() <= 0 || dto.getContent() == null || dto.getContent().isBlank()) {
                session.getAsyncRemote().sendText(JSONUtil.toJsonStr(Map.of("type", "ERROR", "message", "消息参数不合法")));
                return;
            }

            // 统一复用服务层，确保拉黑/风控等校验逻辑一致。
            chatService.sendMessage(senderId, dto);
            String pushPayload = JSONUtil.toJsonStr(Map.of(
                    "senderId", senderId,
                    "receiverId", dto.getReceiverId(),
                    "content", dto.getContent(),
                    "createTime", LocalDateTime.now().toString()
            ));

            if (sendToUser(dto.getReceiverId(), pushPayload)) {
                return;
            }
            if (redisTemplate != null) {
                String key = "offline:msg:" + dto.getReceiverId();
                redisTemplate.opsForList().rightPush(key, pushPayload);
                redisTemplate.expire(key, 24, TimeUnit.HOURS);
            }
        } catch (BusinessException ex) {
            session.getAsyncRemote().sendText(JSONUtil.toJsonStr(Map.of("type", "ERROR", "message", ex.getMessage())));
        } catch (Exception e) {
            log.error("处理WebSocket消息异常", e);
            session.getAsyncRemote().sendText(JSONUtil.toJsonStr(Map.of("type", "ERROR", "message", "消息处理失败，请稍后重试")));
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        Long userId = resolveSessionUserId(session);
        log.error("WebSocket发生错误: userId={}, sessionId={}", userId, session == null ? null : session.getId(), error);
        removeSession(userId, session);
    }

    /**
     * 系统级消息推送（用于实时地图等）
     */
    public static void sendMessage(Long userId, Object message) {
        if (userId == null) {
            return;
        }
        if (sendToUser(userId, JSONUtil.toJsonStr(message))) {
            log.debug("WebSocket推送成功: userId={}", userId);
            return;
        }
        log.debug("WebSocket推送跳过，用户当前无在线会话: userId={}", userId);
    }

    /**
     * 推送位置更新
     */
    public static void sendLocationUpdate(Long userId, Long taskId, java.math.BigDecimal lat, java.math.BigDecimal lng) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("type", "LOCATION_UPDATE");
        data.put("taskId", taskId);
        data.put("latitude", lat);
        data.put("longitude", lng);
        sendMessage(userId, data);
    }

    private static boolean sendToUser(Long userId, String payload) {
        Set<Session> sessions = userSessionMap.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }
        boolean delivered = false;
        for (Session receiverSession : sessions) {
            if (receiverSession != null && receiverSession.isOpen()) {
                receiverSession.getAsyncRemote().sendText(payload);
                delivered = true;
            } else {
                sessions.remove(receiverSession);
            }
        }
        if (sessions.isEmpty()) {
            userSessionMap.remove(userId);
        }
        return delivered;
    }

    private void pushOfflineMessages(Session session, Long userId) {
        if (redisTemplate == null) {
            return;
        }
        String key = "offline:msg:" + userId;
        while (redisTemplate.opsForList().size(key) > 0) {
            String msg = redisTemplate.opsForList().leftPop(key);
            if (msg != null) {
                session.getAsyncRemote().sendText(msg);
            }
        }
    }

    private Long authenticateUserId(String token) {
        if (token == null || token.isBlank() || jwtUtils == null || !jwtUtils.validateToken(token)) {
            return null;
        }
        if (redisTemplate != null && Boolean.TRUE.equals(redisTemplate.hasKey(CacheConstants.TOKEN_BLACKLIST + token))) {
            return null;
        }
        return jwtUtils.getUserIdFromToken(token);
    }

    private ChatMessageDTO toMessageDto(Map<String, Object> payload) {
        ChatMessageDTO dto = new ChatMessageDTO();
        Object receiverId = payload.get("receiverId");
        if (receiverId instanceof Number number) {
            dto.setReceiverId(number.longValue());
        }
        Object content = payload.get("content");
        dto.setContent(content == null ? null : String.valueOf(content).trim());
        Object type = payload.get("type");
        if (type instanceof Number number) {
            dto.setType(number.intValue());
        } else {
            Object messageType = payload.get("messageType");
            if (messageType instanceof Number number) {
                dto.setType(number.intValue());
            }
        }
        return dto;
    }

    private boolean isPing(Map<String, Object> payload) {
        Object type = payload.get("type");
        return type != null && "ping".equalsIgnoreCase(String.valueOf(type));
    }

    private Long resolveSessionUserId(Session session) {
        if (session == null) {
            return null;
        }
        Object userId = session.getUserProperties().get("userId");
        if (userId instanceof Long id) {
            return id;
        }
        return sessionUserMap.get(session.getId());
    }

    private void removeSession(Long userId, Session session) {
        if (session != null) {
            sessionUserMap.remove(session.getId());
        }
        if (userId == null || session == null) {
            return;
        }
        Set<Session> sessions = userSessionMap.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            userSessionMap.remove(userId);
        }
    }

    private int onlineSessionCount() {
        return userSessionMap.values().stream().mapToInt(Set::size).sum();
    }

    private String resolveAccessToken(EndpointConfig endpointConfig) {
        if (endpointConfig == null) {
            return null;
        }
        Object headersObj = endpointConfig.getUserProperties().get("headers");
        if (!(headersObj instanceof Map<?, ?> headers)) {
            return null;
        }
        for (Map.Entry<?, ?> entry : headers.entrySet()) {
            if (entry.getKey() == null || !"cookie".equalsIgnoreCase(String.valueOf(entry.getKey()))) {
                continue;
            }
            Object value = entry.getValue();
            if (!(value instanceof List<?> cookieHeaders) || cookieHeaders.isEmpty()) {
                continue;
            }
            String rawCookie = String.valueOf(cookieHeaders.get(0));
            return extractCookie(rawCookie, "access_token");
        }
        return null;
    }

    private String extractCookie(String cookieHeader, String key) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return null;
        }
        String[] parts = cookieHeader.split(";");
        for (String part : parts) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            if (Objects.equals(pair[0].trim(), key)) {
                return URLDecoder.decode(pair[1].trim(), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private void closeUnauthorizedSession(Session session) {
        if (session == null) {
            return;
        }
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "UNAUTHORIZED"));
        } catch (IOException ex) {
            log.debug("关闭未授权WebSocket会话失败: sessionId={}, err={}", session.getId(), ex.getMessage());
        }
    }

    public static class HttpHeaderConfigurator extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig sec,
                                    jakarta.websocket.server.HandshakeRequest request,
                                    jakarta.websocket.HandshakeResponse response) {
            Map<String, List<String>> headers = request == null ? Collections.emptyMap() : request.getHeaders();
            sec.getUserProperties().put("headers", headers);
        }
    }
}
