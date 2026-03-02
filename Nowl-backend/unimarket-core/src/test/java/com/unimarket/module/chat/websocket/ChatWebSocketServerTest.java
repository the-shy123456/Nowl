package com.unimarket.module.chat.websocket;

import com.unimarket.module.chat.dto.ChatMessageDTO;
import com.unimarket.module.chat.service.ChatService;
import com.unimarket.security.util.JwtUtils;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatWebSocketServerTest {

    private final ChatWebSocketServer server = new ChatWebSocketServer();

    @BeforeEach
    void setUp() throws Exception {
        resetStaticState();
    }

    @AfterEach
    void tearDown() throws Exception {
        resetStaticState();
    }

    @Test
    @DisplayName("onOpen: 缺少 access_token 时关闭未授权会话")
    void onOpen_shouldCloseSessionWhenTokenMissing() throws Exception {
        Session session = mock(Session.class);
        EndpointConfig endpointConfig = mock(EndpointConfig.class);
        when(endpointConfig.getUserProperties()).thenReturn(new HashMap<>());

        server.onOpen(session, endpointConfig);

        verify(session).close(any(CloseReason.class));
    }

    @Test
    @DisplayName("onOpen: token 有效时在 userProperties 写入 userId 且不关闭连接")
    void onOpen_shouldAttachUserIdWhenTokenValid() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        when(jwtUtils.validateToken(eq("token-123"))).thenReturn(true);
        when(jwtUtils.getUserIdFromToken(eq("token-123"))).thenReturn(1001L);
        server.setJwtUtils(jwtUtils);

        Session session = mock(Session.class);
        Map<String, Object> userProps = new HashMap<>();
        when(session.getUserProperties()).thenReturn(userProps);
        when(session.getId()).thenReturn("s-1");

        EndpointConfig endpointConfig = mock(EndpointConfig.class);
        Map<String, Object> endpointProps = new HashMap<>();
        endpointProps.put("headers", Map.of("cookie", List.of("access_token=token-123; foo=bar")));
        when(endpointConfig.getUserProperties()).thenReturn(endpointProps);

        server.onOpen(session, endpointConfig);

        assertEquals(1001L, userProps.get("userId"));
        verify(session, never()).close(any(CloseReason.class));
        verify(jwtUtils).validateToken("token-123");
    }

    @Test
    @DisplayName("onMessage: 收到 ping 返回 pong")
    void onMessage_shouldReplyPongWhenPing() {
        Session session = mock(Session.class);
        RemoteEndpoint.Async async = mock(RemoteEndpoint.Async.class);
        when(session.getAsyncRemote()).thenReturn(async);

        server.onMessage("{\"type\":\"ping\"}", session);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(async).sendText(payload.capture());
        assertTrue(payload.getValue().contains("\"pong\""));
    }

    @Test
    @DisplayName("onMessage: 正常消息走服务层校验与发送")
    void onMessage_shouldDelegateToChatService() {
        ChatService chatService = mock(ChatService.class);
        server.setChatService(chatService);

        Session session = mock(Session.class);
        Map<String, Object> userProps = new HashMap<>();
        userProps.put("userId", 1L);
        when(session.getUserProperties()).thenReturn(userProps);

        RemoteEndpoint.Async async = mock(RemoteEndpoint.Async.class);
        when(session.getAsyncRemote()).thenReturn(async);

        server.onMessage("{\"receiverId\":2,\"content\":\"hi\"}", session);

        ArgumentCaptor<ChatMessageDTO> dtoCaptor = ArgumentCaptor.forClass(ChatMessageDTO.class);
        verify(chatService).sendMessage(eq(1L), dtoCaptor.capture());

        ChatMessageDTO dto = dtoCaptor.getValue();
        assertNotNull(dto);
        assertEquals(2L, dto.getReceiverId());
        assertEquals("hi", dto.getContent());

        // 发送成功场景下，不应给发送者返回错误提示
        verify(async, never()).sendText(anyString());
    }

    private static void resetStaticState() throws Exception {
        clearStaticMap("userSessionMap");
        clearStaticMap("sessionUserMap");
        setStaticField("jwtUtils", null);
        setStaticField("redisTemplate", null);
        setStaticField("chatService", null);
    }

    private static void clearStaticMap(String fieldName) throws Exception {
        Field field = ChatWebSocketServer.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(null);
        if (value instanceof Map<?, ?> map) {
            map.clear();
        }
    }

    private static void setStaticField(String fieldName, Object value) throws Exception {
        Field field = ChatWebSocketServer.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }
}
