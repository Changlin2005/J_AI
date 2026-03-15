package com.lcl.ai.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket连接管理器：管理所有用户的WebSocket会话，供TTS工具调用
 */
@Component
public class WebSocketSessionManager {
    // 存储用户会话：key=userId，value=WebSocketSession
    private final Map<String, WebSocketSession> userSessionMap = new ConcurrentHashMap<>();
    /**
     * 添加用户会话
     */
    public void addSession(String userId, WebSocketSession session) {
        userSessionMap.put(userId, session);
    }
    /**
     * 移除用户会话
     */
    public void removeSession(String userId) {
        userSessionMap.remove(userId);
    }
    /**
     * 获取用户会话
     */
    public WebSocketSession getSession(String userId) {
        return userSessionMap.get(userId);
    }
    /**
     * 推送消息给指定用户（核心方法）
     * @param userId 用户ID
     * @param message 推送的消息（JSON格式）
     * @return 是否推送成功
     */
    public boolean sendMessageToUser(String userId, String message) {
        WebSocketSession session = getSession(userId);
        if (session == null || !session.isOpen()) {
            return false;
        }
        try {
            // 发送文本消息（WebSocket核心推送API）
            session.sendMessage(new org.springframework.web.socket.TextMessage(message));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}