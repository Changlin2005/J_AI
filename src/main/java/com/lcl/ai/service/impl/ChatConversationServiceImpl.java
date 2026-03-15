package com.lcl.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcl.ai.mapper.ChatConversationMapper;
import com.lcl.ai.pojo.ChatConversation;
import com.lcl.ai.service.ChatConversationService;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class ChatConversationServiceImpl extends ServiceImpl<ChatConversationMapper, ChatConversation> implements ChatConversationService {
    @Override
    public List<String> selectAllConversationIds(Long userid) {
        // 构造查询条件：查询所有对话ID（去重）
        LambdaQueryWrapper<ChatConversation> queryWrapper = new LambdaQueryWrapper<ChatConversation>()
                .select(ChatConversation::getConversationId) // 只查 conversation_id 字段
                .eq(ChatConversation::getUserid,userid)
                .groupBy(ChatConversation::getConversationId); // 去重
        // 调用 MyBatis-Plus 通用方法 list()，返回对话ID列表
        return listObjs(queryWrapper, Object::toString);
    }

    @Override
    public ChatConversation selectByConversationId(String conversationId) {
        // 构造查询条件：对话ID等于目标值
        LambdaQueryWrapper<ChatConversation> queryWrapper = new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getConversationId, conversationId);
        // 调用通用方法 getOne()，返回单条对话
        return getOne(queryWrapper);
    }
}
