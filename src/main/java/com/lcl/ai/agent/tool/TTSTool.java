package com.lcl.ai.agent.tool;


import com.alibaba.fastjson.JSON;
import com.lcl.ai.pojo.SpringAiChatMemory;
import com.lcl.ai.utils.TTSCline;
import com.lcl.ai.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * TTS工具类：文本转语音 + WebSocket推送语音URL给前端
 */
@Component
@RequiredArgsConstructor
public class TTSTool {
    // 注入WebSocket连接管理器
    private final WebSocketSessionManager webSocketSessionManager;
    // 构造器注入
    private final TTSCline ttsCline;

    /**
     * 核心方法：文本转语音 + WebSocket推送语音URL
     * @param text 待转换的文本
     * @param userId 目标用户ID（前端连接WebSocket的用户ID）
     * @return 推送结果
     */
    @Tool(description = "文字转语音并推送给用户")
    public Map<String, Object> tts_convertAndPush(@ToolParam(description = "需要转换的文本")String text) {
        try {
            // ========== 1. 已实现的核心逻辑：文本转语音，生成可访问的URL ==========
            String voiceUrl = ttsCline.ttsfile(text);
            // ========== 2. WebSocket推送语音URL给前端 ==========
            // 构建推送消息（前后端统一格式）
            SpringAiChatMemory voiceMessage=new SpringAiChatMemory();
            voiceMessage.setFiletype(1); // 消息类型：语音
            voiceMessage.setConversationId("1");
            voiceMessage.setText(voiceUrl); // 语音URL
            voiceMessage.setTimestamp(LocalDateTime.now());
            // 转换为JSON字符串并推送
            String messageJson = JSON.toJSONString(voiceMessage);
            boolean pushSuccess = webSocketSessionManager.sendMessageToUser("1", messageJson);

            // ========== 3. 返回结果给大模型 ==========
            if (pushSuccess) {
                return Map.of(
                        "status", "success",
                        "message", "语音已推送给用户",
                        "voiceUrl", voiceUrl
                );
            } else {
                return Map.of(
                        "status", "failed",
                        "message", "WebSocket推送失败，用户连接已断开"
                );
            }
        } catch (Exception e) {
            return Map.of(
                    "status", "error",
                    "message", "TTS转换/推送失败：" + e.getMessage()
            );
        }
    }

}