package com.lcl.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lcl.ai.pojo.ChatConversation;

import java.util.List;

public interface ChatConversationService extends IService<ChatConversation> {
    // 查询所有对话ID（用于 findConversationIds 方法）
    List<String> selectAllConversationIds(Long userid);

    // 根据对话ID查询（用于判断对话是否存在）
    ChatConversation selectByConversationId(String conversationId);
}
