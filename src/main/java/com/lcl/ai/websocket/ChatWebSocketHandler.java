package com.lcl.ai.websocket;

import jakarta.websocket.DeploymentException;
import org.apache.tomcat.websocket.server.UriTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final WebSocketSessionManager sessionManager;

    public ChatWebSocketHandler(WebSocketSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    // 连接建立时，注册用户会话
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从URL中提取userId（路径：/ws/chat/{userId}）


        // 关键修复：正确获取路径参数
        Map<String, String> pathVars = getPathVariables(session);

        // 空值校验
        if (pathVars == null || pathVars.get("userId") == null) {
            session.close(CloseStatus.BAD_DATA.withReason("缺少userId参数"));
            return;
        }

        String userId = pathVars.get("userId");
        System.out.println("用户 " + userId + " 建立WebSocket连接");

        if (userId != null) {
            sessionManager.addSession(userId, session); // 注册到管理器
        }
    }

    // 连接关闭时，移除会话
    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        // 关键修复：正确获取路径参数
        Map<String, String> pathVars = getPathVariables(session);

        // 空值校验
        if (pathVars != null && pathVars.get("userId") != null) {
            String userId = pathVars.get("userId");
            System.out.println("用户 " + userId + " 断开WebSocket连接");
            sessionManager.removeSession(userId);
        }
    }

    // 处理前端消息（如需调用TTS，可在此触发）
    @Override
    protected void handleTextMessage(WebSocketSession session, org.springframework.web.socket.TextMessage message) throws Exception {
        // 示例：前端发送"转语音：xxx"时，调用TTS工具
        System.out.println("----0-----0-----");
    }


    /**
     * 从WebSocketSession中获取路径参数
     */
    private Map<String, String> getPathVariables(WebSocketSession session) throws DeploymentException {
        URI uri = session.getUri();
        if (uri == null) {
            return Collections.emptyMap();
        }

        String path = uri.getPath(); // 例如: "/ws/chat/123"
        if (path == null || !path.startsWith("/ws/chat/")) {
            return Collections.emptyMap();
        }

        // 按 "/" 分割，取最后一段作为 userId
        String[] pathSegments = path.split("/");
        if (pathSegments.length < 4) { // 索引 0 是空, 1:ws, 2:chat, 3:userId
            return Collections.emptyMap();
        }

        String userId = pathSegments[3];
        return Collections.singletonMap("userId", userId);
    }
}