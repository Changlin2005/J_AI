package com.lcl.ai.agent.tool;

import com.lcl.ai.statemachine.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmotionDeltaTool {
    private final EmotionScore emotionScore;
    private final StateMachine<EmotionState, EmotionEvent> stateMachine;
    @Tool(description = "分析用户对话，返回角色情绪分值变化，取值范围-15~+15")
    public EmotionDelta getEmotionDelta(
            @ToolParam(description = "开心增减 -15~15") int happy,
            @ToolParam(description = "生气增减 -15~15") int angry,
            @ToolParam(description = "委屈增减 -15~15") int sad,
            @ToolParam(description = "害羞增减 -15~15") int shy
    ) {
        emotionScore.updateScore(happy,angry,sad,shy);
        // 1. 后端计算出当前主导情绪（你已经做好了）
        EmotionState targetState = emotionScore.getCurrentState();
        Message<EmotionEvent> message = MessageBuilder
                .withPayload(EmotionEvent.UPDATE_EMOTION)
                .setHeader("targetState", targetState) // 关键！
                .build();

        // 发送事件（真正标准写法）
        stateMachine.sendEvent(message);
        // 后端强制限制范围，防止大模型乱输出
        return emotionScore.updateScore(happy,angry,sad,shy);
    }


}