package com.lcl.ai.websocket;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.audio.asr.recognition.timestamp.Sentence;
import com.alibaba.dashscope.common.ResultCallback;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcl.ai.pojo.TtsRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;


public class AudioToTtsWebSocketHandler extends BinaryWebSocketHandler {
    // 音频缓冲区大小
    private static final int CHUNK_SIZE = 1024;
    // AI处理客户端（实际项目中应通过依赖注入）
    @Autowired
    private ChatClient chatClient;
    @Autowired
    private VectorStore vectorStore;
    // 线程本地存储 - 每个会话独立
    private ThreadLocal<Recognition> recognizerLocal = new ThreadLocal<>();
    private ThreadLocal<List<String>> resultCacheLocal = new ThreadLocal<>();
    private ThreadLocal<CountDownLatch> latchLocal = new ThreadLocal<>();
    public String chat(String prompt, String conversationId){
        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor=RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder().vectorStore(vectorStore).build())
                .build();
        return chatClient.prompt()
                .user(prompt)
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID,conversationId))
                .advisors(retrievalAugmentationAdvisor)
                .call()
                .content();
    }
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 初始化线程本地资源
        resultCacheLocal.set(new ArrayList<>());
        latchLocal.set(new CountDownLatch(1));

        // 1. 初始化ASR识别器
        Recognition recognizer = new Recognition();
        RecognitionParam param = RecognitionParam.builder()
                .apiKey("${Qwen_Api_Key}")
                .model("fun-asr-realtime")
                .format("wav")
                .sampleRate(48000)
                .parameter("language_hints", new String[]{"zh", "en"})
                .build();

        // 2. 设置ASR回调
        ResultCallback<RecognitionResult> callback = new ResultCallback<RecognitionResult>() {
            @Override
            public void onEvent(RecognitionResult result) {
                // 缓存句子级识别结果
                if (result.isSentenceEnd() && result.getSentence() != null) {
                    Sentence sentence = result.getSentence();
                    resultCacheLocal.get().add(sentence.getText());
                }
            }
            @Override
            public void onComplete() {
                latchLocal.get().countDown(); // 识别完成信号
            }
            @Override
            public void onError(Exception e) {
                try {
                    session.sendMessage(new TextMessage("ASR识别失败：" + e.getMessage()));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                latchLocal.get().countDown();
            }
        };
        // 启动ASR识别
        recognizer.call(param, callback);
        recognizerLocal.set(recognizer);
    }
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        // 接收前端音频流并转发给ASR
        ByteBuffer byteBuffer = message.getPayload();
        byte[] audioFrame = new byte[byteBuffer.remaining()];
        byteBuffer.get(audioFrame);
        Recognition recognizer = recognizerLocal.get();
        if (recognizer != null) {
            recognizer.sendAudioFrame(ByteBuffer.wrap(audioFrame));
        }
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        // 1. 处理ASR收尾工作
        Recognition recognizer = recognizerLocal.get();
        if (recognizer != null) {
//            recognizer.sendFinish(); // 告知识别结束
        }
        // 2. 等待ASR识别完成
        CountDownLatch latch = latchLocal.get();
        if (latch != null) {
            latch.await(30, TimeUnit.SECONDS); // 超时保护
        }
        // 3. 拼接ASR结果并进行AI处理
        List<String> asrResults = resultCacheLocal.get();
        if (asrResults == null || asrResults.isEmpty()) {
            session.sendMessage(new TextMessage("未识别到有效内容"));
            return;
        }
        String asrText = String.join("", asrResults);
//        session.sendMessage(new TextMessage("ASR识别结果：" + asrText));
        // 4. 调用AI处理
        String aiResponse = chat(asrText,"1");
//        session.sendMessage(new TextMessage("AI处理结果：" + aiResponse));
        // 5. 将AI结果转换为语音（TTS）
        generateAndSendTts(session, aiResponse);
        // 6. 清理资源
        recognizerLocal.remove();
        resultCacheLocal.remove();
        latchLocal.remove();
    }

    // 生成TTS并发送给前端
    private void generateAndSendTts(WebSocketSession session, String text) throws Exception {
        // 构建TTS请求
        TtsRequest requestData = new TtsRequest(
                "【崩三】爱莉希雅",
                "default",
                text,
                "auto",
                1,
                1.0,
                5,
                0.8,
                0.8,
                "true"
        );

        // 转换为JSON请求体
        ObjectMapper mapper = new ObjectMapper();
        String requestBody = mapper.writeValueAsString(requestData);

        // 发送TTS请求并处理流式响应
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