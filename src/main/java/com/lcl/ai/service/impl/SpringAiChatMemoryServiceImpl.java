package com.lcl.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcl.ai.mapper.SpringAiChatMemoryMapper;
import com.lcl.ai.pojo.SpringAiChatMemory;
import com.lcl.ai.service.SpringAiChatMemoryService;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class SpringAiChatMemoryServiceImpl extends ServiceImpl<SpringAiChatMemoryMapper, SpringAiChatMemory> implements SpringAiChatMemoryService {
    @Override
    public List<SpringAiChatMemory> selectByConversationId(String conversationId) {

        // 构造查询条件：对话ID等于目标值，按发送时间升序
        LambdaQueryWrapper<SpringAiChatMemory> queryWrapper = new LambdaQueryWrapper<SpringAiChatMemory>()
                .eq(SpringAiChatMemory::getConversationId, conversationId)
                .orderByAsc(SpringAiChatMemory::getTimestamp);
        // 调用通用方法 list()，返回消息列表
        return list(queryWrapper);
    }
    @Override
    public List<SpringAiChatMemory> selectAll() {

        // 构造查询条件：对话ID等于目标值，按发送时间升序
        LambdaQueryWrapper<SpringAiChatMemory> queryWrapper = new LambdaQueryWrapper<SpringAiChatMemory>()
                .orderByAsc(SpringAiChatMemory::getTimestamp);
        // 调用通用方法 list()，返回消息列表
        return list(queryWrapper);
    }
    @Override
    public int deleteByConversationId(String conversationId) {
        // 构造删除条件：对话ID等于目标值
        LambdaQueryWrapper<SpringAiChatMemory> queryWrapper = new LambdaQueryWrapper<SpringAiChatMemory>()
                .eq(SpringAiChatMemory::getConversationId, conversationId);
        // 调用通用方法 remove()，返回删除记录数
        return remove(queryWrapper) ? 1 : 0; // 若删除成功返回1，否则0
    }



}
