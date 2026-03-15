package com.lcl.ai.mapper;

import com.lcl.ai.pojo.GraphCheckpoint;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author lcl
 * @since 2026-02-23
 */
public interface GraphCheckpointMapper extends BaseMapper<GraphCheckpoint> {
    /**
     * 根据 threadId 查询未释放会话的所有 Checkpoint（按保存时间升序）
     */
    @Select("SELECT * FROM GRAPH_CHECKPOINT WHERE thread_id = #{threadId} ORDER BY saved_at ASC")
    List<GraphCheckpoint> selectByThreadId(@Param("threadId") String threadId);

    /**
     * 查询指定会话的最终回复 Checkpoint（next_node_id = END）
     */
    @Select("SELECT * FROM GRAPH_CHECKPOINT c " +
            "INNER JOIN GRAPH_THREAD t ON c.thread_id = t.thread_id " +
            "WHERE t.thread_name = #{threadName} AND t.is_released = FALSE " +
            "  AND c.next_node_id = 'END' " +
            "ORDER BY c.saved_at DESC LIMIT 1")
    GraphCheckpoint selectFinalReplyCheckpoint(@Param("threadName") String threadName);

}
