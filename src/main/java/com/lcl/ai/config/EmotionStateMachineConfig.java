package com.lcl.ai.config;

import com.lcl.ai.statemachine.EmotionEvent;
import com.lcl.ai.statemachine.EmotionState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;

@Configuration
public class EmotionStateMachineConfig {

    @Bean
    public StateMachine<EmotionState, EmotionEvent> emotionStateMachine() throws Exception {
        StateMachineBuilder.Builder<EmotionState, EmotionEvent> builder = StateMachineBuilder.builder();

        // ====================== 1. 状态配置（不变） ======================
        builder.configureStates()
                .withStates()
                .initial(EmotionState.NORMAL)
                .state(EmotionState.HAPPY)
                .state(EmotionState.ANGRY)
                .state(EmotionState.SAD)
                .state(EmotionState.SHY);

        // ====================== 2. 转换配置（核心改动） ======================
        builder.configureTransitions()
                // ========== 固定：NORMAL 跳转（保留动作效果） ==========
                .withExternal()
                .source(EmotionState.NORMAL).target(EmotionState.HAPPY)
                .event(EmotionEvent.UPDATE_EMOTION)
                .action(ctx -> System.out.println("[动作] 人物眼睛发亮，嘴角上扬~"))

                .and().withExternal()
                .source(EmotionState.NORMAL).target(EmotionState.ANGRY)
                .event(EmotionEvent.UPDATE_EMOTION)
                .action(ctx -> System.out.println("[动作] 人物叉腰，气鼓鼓！"))

                .and().withExternal()
                .source(EmotionState.NORMAL).target(EmotionState.SAD)
                .event(EmotionEvent.UPDATE_EMOTION)
                .action(ctx -> System.out.println("[动作] 人物低下头，眼眶红红的..."))

                .and().withExternal()
                .source(EmotionState.NORMAL).target(EmotionState.SHY)
                .event(EmotionEvent.UPDATE_EMOTION)
                .action(ctx -> System.out.println("[动作] 人物脸红，捂住脸！"));

        // 全局万能转换：任意状态 → 任意状态（先配置全量转换）
        for (EmotionState source : EmotionState.values()) {
            for (EmotionState target : EmotionState.values()) {
                if (source != target) {
                    builder.configureTransitions()
                            .withExternal()
                            .source(source)
                            .target(target)
                            .event(EmotionEvent.UPDATE_EMOTION)
                            // 只有当事件头的 targetState 等于当前 target 时才允许转换
                            .guard(ctx -> target.equals(ctx.getMessage().getHeaders().get("targetState")));
                }
            }
        }
        return builder.build();
    }
}