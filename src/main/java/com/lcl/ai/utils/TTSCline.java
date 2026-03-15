package com.lcl.ai.utils;
import cn.hutool.core.lang.UUID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcl.ai.pojo.TtsRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.sound.sampled.*;
@Component
public class TTSCline {
    private static final int AUDIO_CHANNELS = 1;
    private static final int SAMPLE_RATE = 32000;
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int CHUNK_SIZE = 1024;
    public String ttsfile(String text) throws IOException, InterruptedException {
        // 1. 构造请求参数（示例值，需替换为实际参数）
        TtsRequest requestData = new TtsRequest(
                "【崩三】爱莉希雅", // character：角色文件夹名称
                "default",        // emotion：角色情感
                text,// text：要转换的文本
                "auto",            // text_language：文本语言
                1,                 // batch_size：批处理大小
                1.2,               // speed：语速
                5,                 // top_k：GPT模型参数
                0.8,               // top_p：GPT模型参数
                0.8,               // temperature：GPT模型参数
                "false"            // stream：是否流式传输
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

        // 4. 接收响应并保存WAV音频（成功时返回WAV流）
        HttpResponse<byte[]> response = client.send(
                request, HttpResponse.BodyHandlers.ofByteArray()
        );

        if (response.statusCode() == 200) {
            // 保存音频到本地文件
            String filemp3=UUID.randomUUID() + ".wav";
            String filePath = "D:/lcl-project/AI/target/classes/static/audio/" + filemp3;
            Files.write(Paths.get(filePath), response.body());
            System.out.println("音频生成成功，已保存为"+filePath);
            return filemp3;
        } else {
            // 失败时打印错误原因
            String errorMsg = new String(response.body(), StandardCharsets.UTF_8);
            System.out.println("请求失败：" + errorMsg);
        }
        return "filePath,ERROR";
    }
    public void ttsstream(WebSocketSession session, String text) throws IOException, InterruptedException, LineUnavailableException {
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
        // 3. 发送POST请求
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:5000/tts"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();
        // 流式获取并发送音频
        try (InputStream audioIn = client.send(request, HttpResponse.BodyHandlers.ofInputStream()).body()) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int len;
            while ((len = audioIn.read(buffer)) != -1 && session.isOpen()) {
                session.sendMessage(new BinaryMessage(buffer, 0, len, true));
            }
        }

    }

}
