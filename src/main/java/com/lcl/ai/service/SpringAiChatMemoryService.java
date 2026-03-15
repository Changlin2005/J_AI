package com.lcl.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lcl.ai.pojo.SpringAiChatMemory;

import java.util.List;

public interface SpringAiChatMemoryService extends IService<SpringAiChatMemory> {
    // 根据对话ID查询所有消息（按发送时间升序）
    List<SpringAiChatMemory> selectByConversationId(String conversationId);

    // 根据对话ID删除所有消息
    int deleteByConversationId( String conversationId);
    public List<SpringAiChatMemory> selectAll();
}
