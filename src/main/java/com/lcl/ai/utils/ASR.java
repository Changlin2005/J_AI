package com.lcl.ai.utils;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.lcl.ai.dto.Result;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

@Component
public class ASR {
    public Result file_asr(String localFilePath) {
        // 直接使用本地文件路径，无需下载
        File audioFile = new File(localFilePath);
        if (!audioFile.exists() || !audioFile.isFile()) {
            return Result.fail("错误：本地文件不存在或不是有效文件！");
        }
        // 创建Recognition实例
        Recognition recognizer = new Recognition();
        // 创建RecognitionParam
        RecognitionParam param =
                RecognitionParam.builder()
                        // 若没有将API Key配置到环境变量中，需将下面这行代码注释放开，并将apiKey替换为自己的API Key
                        .apiKey("sk-9ab02c515d0d499fbf17610ed1ff77d6")
                        .model("fun-asr-realtime")
                        .format("wav")  // 根据本地文件实际格式调整（如mp3）
                        .sampleRate(48000)  // 需与本地文件采样率一致
                        .parameter("language_hints", new String[]{"zh", "en"})
                        .build();

            String filestring= recognizer.call(param, audioFile);
        if (filestring!=null){
            return Result.ok(filestring);
        }
        else {
            return Result.fail("错误：语音识别为空");
        }
    }
    public Result stream_asr(ByteBuffer byteBuffer) {
        // 直接使用本地文件路径，无需下载

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


        // 3. 实现DashScope的ResultCallback回调接口
        ResultCallback<RecognitionResult> callback = new ResultCallback<RecognitionResult>() {
            @Override
            public void onEvent(RecognitionResult result) {
                // 处理流式识别结果
            }

            @Override
            public void onComplete() {
                // 识别完成
            }

            @Override
            public void onError(Exception e) {
                // 识别异常
            }
        };


            // 4. 绑定回调并启动流式识别
            recognizer.call(param, callback);



        return null;
    }
}