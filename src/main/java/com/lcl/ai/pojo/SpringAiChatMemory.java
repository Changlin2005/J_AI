package com.lcl.ai.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.ai.chat.messages.MessageType;

import java.time.LocalDateTime;

/**
 * Spring AI 聊天记忆实体类（对应表 spring_ai_chat_memory）
 */
@Data
@TableName("spring_ai_chat_memory") // 与数据库表名完全匹配
public class SpringAiChatMemory {

    /**
     * 对话ID（对应字段 conversation_id）
     * 非主键，但为核心关联字段（表中未设主键，可根据业务补充主键或使用联合唯一键）
     */
    @TableId(type = IdType.INPUT)
    @TableField("conversation_id")
    private String conversationId;

    /**
     * 消息内容（对应字段 content）
     * 非空字段，类型为 TEXT
     */
    @TableField("content")
    private String text;

    /**
     * 消息类型（对应字段 type）
     * 枚举类型：USER(用户)、ASSISTANT(助手)、SYSTEM(系统)、TOOL(工具)
     */
    @TableField("type")
    private MessageType messageType;
    //消息类型：0:文字，1:图频
    private int filetype;

    /**
     * 时间戳（对应字段 timestamp）
     * 非空字段，存储消息发送/创建时间
     */
    @TableField("timestamp")
    private LocalDateTime timestamp;


    /**
     * 消息类型枚举（与数据库 ENUM 值完全对应）
     */

}