package com.lcl.ai.service;

import com.lcl.ai.pojo.GraphCheckpoint;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lcl.ai.pojo.SpringAiChatMemory;
import com.lcl.ai.service.impl.GraphCheckpointServiceImpl;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lcl
 * @since 2026-02-23
 */
public interface IGraphCheckpointService extends IService<GraphCheckpoint> {

    List<SpringAiChatMemory> queryUserHistory(String threadName);

}
