package com.lcl.ai.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("chat_conversation")
public class ChatConversation {
    private Long id;
    private String conversationId; // 对话唯一标识
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long userid;

}
