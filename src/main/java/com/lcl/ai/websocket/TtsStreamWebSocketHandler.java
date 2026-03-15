package com.lcl.ai.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcl.ai.pojo.TtsRequest;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
public class TtsStreamWebSocketHandler extends BinaryWebSocketHandler {
    // 音频缓冲区大小（和前端/TTs服务对齐）
    private static final int CHUNK_SIZE = 1024;
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 1. 获取前端传入的文本参数（前端连接时携带，如：ws://localhost:8080/tts/stream?text=测试文本）
        String text = session.getUri().getQuery().split("text=")[1];

        // 1. 构造请求参数（示例值，需替换为实际参数）
        TtsRequest requestData = new TtsRequest(
                "【崩三】爱莉希雅", // character：角色文件夹名称
                "default",        // emotion：角色情感
                text,// text：要转换的文本
                "auto",            // text_language：文本语言
                1,                 // batch_size：批处理大小
                1.0,               // speed：语速
                5,                 // top_k：GPT模型参数
                0.8,               // top_p：GPT模型参数
                0.8,               // temperature：GPT模型参数
                "true"            // stream：是否流式传输
        );
        // 2. 转换为JSON请求体
        ObjectMapper mapper = new ObjectMapper();
        String requestBody = mapper.writeValueAsString(requestData);
        System.out.println("请求体：" + requestBody);
        // 3. 发送POST请求
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:5000/tts"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        // 3. 流式读取TTS音频数据，并实时推送给前端
        HttpResponse<InputStream> response = client.send(
                request, HttpResponse.BodyHandlers.ofInputStream()
        );

        try (InputStream audioIn = response.body()) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int len;
            // 循环读取音频块，推送给前端
            while ((len = audioIn.read(buffer)) != -1 && session.isOpen()) {
                // 发送二进制音频数据给前端
                session.sendMessage(new BinaryMessage(buffer, 0, len, true));
            }
        } finally {
            // 传输完成后关闭WebSocket连接
            session.close();
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        // 前端无需发送二进制数据，空实现
    }
}
