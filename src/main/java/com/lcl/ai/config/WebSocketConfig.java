package com.lcl.ai.config;

import com.lcl.ai.websocket.AudioToTtsWebSocketHandler;
import com.lcl.ai.websocket.ChatWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final ChatWebSocketHandler chatWebSocketHandler;

    // 注入自定义的WebSocket处理器
    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册聊天通道，允许跨域，路径：/ws/chat/{userId}（通过userId区分用户）
        registry.addHandler(chatWebSocketHandler, "/ws/chat/{userId}")
                .setAllowedOrigins("*"); // 生产环境需指定具体域名，如http://localhost:8080
    }

}
