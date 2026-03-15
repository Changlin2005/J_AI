package com.lcl.ai.statemachine;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmotionDelta {
    private int happy;
    private int angry;
    private int sad;
    private int shy;
}