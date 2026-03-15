package com.lcl.ai.websocket;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.audio.asr.recognition.timestamp.Sentence;
import com.alibaba.dashscope.common.ResultCallback;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AudioStreamWebSocketHandler extends BinaryWebSocketHandler {

    // DashScope FunASR识别器（每个会话独立创建）
    private Recognition recognizer;

    // 每个会话的识别器+结果缓存
    private ThreadLocal<Recognition> recognizerLocal = new ThreadLocal<>();

    private ThreadLocal<List<String>> resultCacheLocal = new ThreadLocal<>();

    // 用于等待识别完成的闭锁
    private ThreadLocal<CountDownLatch> latchLocal = new ThreadLocal<>();
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 创建Recognition实例
        Recognition recognizer = new Recognition();
        // 创建RecognitionParam
        RecognitionParam param =
                RecognitionParam.builder()
                        // 若没有将API Key配置到环境变量中，需将下面这行代码注释放开，并将apiKey替换为自己的API Key
                        .apiKey("${Qwen_Api_Key}")
                        .model("fun-asr-realtime")
                        .format("wav")  // 根据本地文件实际格式调整（如mp3）
                        .sampleRate(48000)  // 需与本地文件采样率一致
                        .parameter("language_hints", new String[]{"zh", "en"})
                        .build();

        // 3. 绑定回调：将FunASR结果推送给前端
        ResultCallback<RecognitionResult> callback = new ResultCallback<RecognitionResult>() {
            @Override
            public void onEvent(RecognitionResult result) {
                // 缓存识别结果（解析单个Sentence的文本）
                if (result.isSentenceEnd()) { // 句子结束标识 → 此时Sentence是最终结果
                    Sentence sentence = result.getSentence();
                    if (sentence != null) {
                        resultCacheLocal.get().add(sentence.getText());
                    }
                }
            }

            @Override
            public void onComplete() {
                // 识别完成，触发闭锁
                latchLocal.get().countDown();
                // 释放FunASR资源
            }

            @Override
            public void onError(Exception e) {
                // 识别异常，推送错误信息并触发闭锁
                try {
                    session.sendMessage(new TextMessage("识别失败：" + e.getMessage()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                latchLocal.get().countDown();
            }
        };

        // 4. 启动FunASR流式识别
        recognizer.call(param, callback);
    }


    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        // 接收前端推送的音频字节流，实时转发给FunASR
        ByteBuffer byteBuffer = message.getPayload();
        byte[] audioFrame = new byte[byteBuffer.remaining()];
        byteBuffer.get(audioFrame);

        Recognition recognizer = recognizerLocal.get();
        if (recognizer != null) {
            recognizer.sendAudioFrame(ByteBuffer.wrap(audioFrame)); // 推送音频帧
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws InterruptedException, IOException {
        // 前端关闭连接（音频流推送完成），发送结束信号给FunASR
        Recognition recognizer = recognizerLocal.get();
        if (recognizer != null) {
//            recognizer.sendFinish(); // 告知识别服务音频流结束
        }

        // 等待FunASR识别完成
        CountDownLatch latch = latchLocal.get();
        if (latch != null) {
            latch.await(); // 阻塞直到识别完成
        }

        // 拼接最终文本并返回给前端
        List<String> resultCache = resultCacheLocal.get();
        if (resultCache != null && !resultCache.isEmpty()) {
            String finalText = String.join("", resultCache);
            session.sendMessage(new TextMessage("识别完成，最终文本：" + finalText));
        }

        // 清理ThreadLocal资源
        recognizerLocal.remove();
        resultCacheLocal.remove();
        latchLocal.remove();
    }

    // 解析RecognitionResult为文本（需根据实际结构调整）
    private String parseResultText(Object result) {
        // 示例：假设result是RecognitionResult，需调用其getText()等方法
        // 实际需根据DashScope SDK的RecognitionResult结构实现
        return result.toString();
    }
}