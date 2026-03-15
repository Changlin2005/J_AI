package com.lcl.ai.service.impl;

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcl.ai.pojo.GraphCheckpoint;
import com.lcl.ai.mapper.GraphCheckpointMapper;
import com.lcl.ai.mapper.GraphThreadMapper;
import com.lcl.ai.pojo.GraphThread;
import com.lcl.ai.pojo.SpringAiChatMemory;
import com.lcl.ai.service.IGraphCheckpointService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lcl
 * @since 2026-02-23
 */
@Service
public class GraphCheckpointServiceImpl extends ServiceImpl<GraphCheckpointMapper, GraphCheckpoint> implements IGraphCheckpointService {
    // 框架默认序列化器（和 MysqlSaver 保持一致）
    private final StateSerializer stateSerializer = StateGraph.DEFAULT_JACKSON_SERIALIZER;

    // 注入 MyBatis-Plus Mapper
    @Resource
    private GraphThreadMapper graphThreadMapper;
    @Resource
    private GraphCheckpointMapper graphCheckpointMapper;

    /**
     * 查询指定会话的历史用户提问（含对应 AI 回复）
     */
    @Override
    public List<SpringAiChatMemory> queryUserHistory(String threadName) {
        // 1. 先查询会话信息（获取 threadId）
        GraphThread graphThread = graphThreadMapper.selectByThreadName(threadName);
        if (graphThread == null) {
            return Collections.emptyList();
        }

        // 2. 根据 threadId 查询所有 Checkpoint
        List<GraphCheckpoint> checkpointList = graphCheckpointMapper.selectByThreadId(graphThread.getThreadId());
        if (checkpointList.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 解析 Checkpoint 提取用户消息和 AI 回复

        List<SpringAiChatMemory> aimemory = new ArrayList<>() ;
        for (GraphCheckpoint checkpoint : checkpointList) {
            if (checkpoint.getNodeId().equals("__START__")||checkpoint.getNextNodeId().equals("__END__")){
                try {
                    // 3.1 解析 state_data 字段
                    String stateDataJson = checkpoint.getStateData();
                    String base64Data = extractBase64FromJson(stateDataJson);
                    if (!StringUtils.hasText(base64Data)) {
                        continue;
                    }

                    byte[] binaryData = Base64.getDecoder().decode(base64Data);
                    Map<String, Object> stateMap = stateSerializer.dataFromBytes(binaryData);

                    // 3.2 提取 messages 列表
                    List<Map<String, Object>>  messages=((List<Map<String, Object>>) stateMap.get("messages")) ;
                    if (messages == null || messages.isEmpty()) {
                        continue;
                    }
                    System.out.println(messages);
                    SpringAiChatMemory memory= new SpringAiChatMemory();
                    AbstractMessage abstractMessage= (AbstractMessage) messages.getLast();
                    memory.setText(abstractMessage.getText());
                    memory.setMessageType(abstractMessage.getMessageType());
                    memory.setTimestamp(checkpoint.getSavedAt());
                    memory.setFiletype(0);
                    aimemory.add(memory);
                } catch (Exception e) {
                    // 单个 Checkpoint 解析失败不影响整体
                    e.printStackTrace();
                    continue;
                }
            }

        }
        System.out.println("-----------------------------------------------------------------");

        // 4. 转换为有序列表返回
        return aimemory;
    }



    // 辅助方法：提取 Base64 内容
    private String extractBase64FromJson(String stateDataJson) {
        // 1. 空值前置校验
        if (!StringUtils.hasText(stateDataJson)) {
            return "";
        }
        try {
            // 2. 解析 JSON 字符串
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> jsonMap = objectMapper.readValue(stateDataJson, new TypeReference<Map<String, String>>() {});
            // 3. 提取 binaryPayload（为空则返回空字符串）
            return jsonMap.getOrDefault("binaryPayload", "");
        } catch (Exception e) {
            // 解析失败时打印日志，返回空
            log.error("解析 stateDataJson 失败", e);
            return "";
        }

    }


}
