package com.lcl.ai.pojo;

import lombok.Data;

@Data

public class TtsRequest {
        private String character;
        private String emotion;
        private String text;
        private String text_language;
        private int batch_size;
        private double speed;
        private int top_k;
        private double top_p;
        private double temperature;
        private String stream;
        private String save_temp = "False";

    public TtsRequest(String character, String emotion, String text, String text_language, int batch_size, double speed, int top_k, double top_p, double temperature, String stream) {
        this.character = character;
        this.emotion = emotion;
        this.text = text;
        this.text_language = text_language;
        this.batch_size = batch_size;
        this.speed = speed;
        this.top_k = top_k;
        this.top_p = top_p;
        this.temperature = temperature;
        this.stream = stream;
        this.save_temp = save_temp;
    }
}
