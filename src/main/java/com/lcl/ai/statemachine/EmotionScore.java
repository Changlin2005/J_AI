package com.lcl.ai.statemachine;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 情感数值模型：每种情绪 0~100 分
 */
@Data
public class EmotionScore {
    // 基础情绪分值（0~100）
    private int happy = 50;    // 开心
    private int angry = 0;     // 生气
    private int sad = 0;       // 委屈
    private int shy = 0;       // 害羞

    // 每次对话的情绪衰减系数（防止情绪永久堆积）
    private static final int DECAY = 5;

    /**
     * 对话后更新情绪分值
     */
    public EmotionDelta updateScore(int happyAdd, int angryAdd, int sadAdd, int shyAdd) {
        // 1. 自然衰减：所有情绪缓慢回落
        decayAll();
        // 2. 累加本次对话带来的情绪变化
        this.happy = clamp(this.happy + happyAdd);
        this.angry = clamp(this.angry + angryAdd);
        this.sad = clamp(this.sad + sadAdd);
        this.shy = clamp(this.shy + shyAdd);
        return new EmotionDelta(this.happy,this.angry,this.sad,this.shy);
    }

    /**
     * 自然衰减：所有情绪向中间值回落
     */
    private void decayAll() {
        this.happy = clamp(this.happy - DECAY);
        this.angry = Math.max(0, this.angry - DECAY);
        this.sad = Math.max(0, this.sad - DECAY);
        this.shy = Math.max(0, this.shy - DECAY);
    }

    /**
     * 限制分值 0~100
     */
    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    /**
     * 根据分值自动判断【当前主导情绪】
     */
    public EmotionState getCurrentState() {
        Map<EmotionState, Integer> scoreMap = new HashMap<>();
        scoreMap.put(EmotionState.HAPPY, this.happy);
        scoreMap.put(EmotionState.ANGRY, this.angry);
        scoreMap.put(EmotionState.SAD, this.sad);
        scoreMap.put(EmotionState.SHY, this.shy);

        // 找最高分
        return scoreMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(EmotionState.NORMAL);
    }
}