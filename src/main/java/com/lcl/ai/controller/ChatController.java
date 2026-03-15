package com.lcl.ai.controller;


import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.lcl.ai.agent.tool.TTSTool;
import com.lcl.ai.dto.Result;
import com.lcl.ai.mapper.GraphThreadMapper;
import com.lcl.ai.pojo.GraphThread;
import com.lcl.ai.service.IGraphCheckpointService;
import com.lcl.ai.utils.ASR;
import com.lcl.ai.utils.TTSCline;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Collections;


@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class ChatController {
    private final ChatClient chatClient;
    private final ReactAgent qwenAgent;
    private final ReactAgent ElysiaAgent;
    private final ASR asr;
    private final IGraphCheckpointService iGraphCheckpointService;
    private final GraphThreadMapper graphThreadMapper;
    private final TTSTool ttsTool;
    @RequestMapping(value = "/agent1" ,produces = "text/html;charset=utf-8")
    public Flux<String> agrnt(String prompt) throws GraphRunnerException {
        return qwenAgent.stream(prompt).filter(output -> output instanceof StreamingOutput)
                .cast(StreamingOutput.class) // 类型转换为 StreamingOutput
                // 只保留模型流式输出（最终给用户的内容）
                .filter(streamingOutput -> OutputType.AGENT_MODEL_STREAMING.equals(streamingOutput.getOutputType()))
                // 提取增量文本（核心：逐段返回给用户）
                .map(streamingOutput -> streamingOutput.message().getText())
                // 异常处理：出错时返回友好提示
                .onErrorReturn("抱歉，处理请求时发生错误，请稍后重试");

    }

    @RequestMapping(value = "/chat" ,produces = "text/html;charset=utf-8")
    public String agent(String prompt) throws GraphRunnerException {
        RunnableConfig config = RunnableConfig.builder()
                .threadId("1")
                .build();
        String s=qwenAgent.call(prompt,config).getText();
//        ttsTool.tts_convertAndPush(s,"1");
        return s;
    }

//    @RequestMapping(value = "/chat" ,produces = "text/html;charset=utf-8")
//    public Flux<String> chat(String prompt,String conversationId){
//        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor=RetrievalAugmentationAdvisor.builder()
//                .documentRetriever(VectorStoreDocumentRetriever.builder().vectorStore(vectorStore).build())
//                .build();
//        return chatClient.prompt()
//                .user(prompt)
//                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID,conversationId))
//                .advisors(retrievalAugmentationAdvisor)
//                .stream()
//                .content();
//
//    }
//    @RequestMapping(value = "/arschat" ,produces = "text/html;charset=utf-8")
//    public Flux<String> arschat(String localFilePath,String conversationId){
//        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor=RetrievalAugmentationAdvisor.builder()
//                .documentRetriever(VectorStoreDocumentRetriever.builder().vectorStore(vectorStore).build())
//                .build();
//        String prompt= (String) asr.file_asr(localFilePath).getData();
//
//        return chatClient.prompt()
//                .user(prompt)
//                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID,conversationId))
//                .advisors(retrievalAugmentationAdvisor)
//                .stream()
//                .content();
//
//    }
//    @RequestMapping(value = "/ttschat" ,produces = "text/html;charset=utf-8")
//    public String ttschat(@RequestParam("audioFile") MultipartFile audioFile, String conversationId) throws IOException, InterruptedException {
//        // 1. 校验文件是否为空
//        if (audioFile.isEmpty()) {
//            throw new RuntimeException("上传的音频文件为空");
//        }
//        // 2. 确保保存目录存在
//
//        String savePath = "D:/lcl-project/AI/audio/" + UUID.randomUUID() + ".wav";
//        File destFile = new File(savePath);
//
//        audioFile.transferTo(destFile);
//        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor=RetrievalAugmentationAdvisor.builder()
//                .documentRetriever(VectorStoreDocumentRetriever.builder().vectorStore(vectorStore).build())
//                .build();
//
//        String prompt= (String) asr.file_asr(savePath).getData();
//
//        String ai_text= chatClient.prompt()
//                .user(prompt)
//                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID,conversationId))
//                .advisors(retrievalAugmentationAdvisor).call().content();
//
////        String filePath=tts.streamAudioDataToSpeaker(ai_text);
//        String filePath=ttsCline.ttsfile(ai_text);
//        return filePath;
//    }
    @RequestMapping(value = "/test" ,produces = "text/html;charset=utf-8")
    public String test() throws IOException, InterruptedException {

        TTSCline ttsCline=new TTSCline();
        String file=ttsCline.ttsfile("嗨~，想我了吗？");
        return file;

    }
    @GetMapping("/getchat")
    public Result selectByConversationId(){
//        List<Message> message =redisChatMemoryRepository.findByConversationId("conv_1764403977967");

        String threadName = "1";
        GraphThread graphThread = graphThreadMapper.selectByThreadName(threadName);
        if (graphThread == null) {
            return Result.ok(Collections.emptyList()) ;
        }
        else {
            return  Result.ok(iGraphCheckpointService.queryUserHistory(threadName));
        }
    }
}
