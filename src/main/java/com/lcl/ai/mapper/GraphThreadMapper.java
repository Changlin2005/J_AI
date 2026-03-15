package com.lcl.ai.mapper;

import com.lcl.ai.pojo.GraphThread;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author lcl
 * @since 2026-02-23
 */
public interface GraphThreadMapper extends BaseMapper<GraphThread> {
    /**
     * 根据 threadName 查询未释放的会话
     */
    @Select("SELECT * FROM GRAPH_THREAD WHERE thread_name = #{threadName} AND is_released = FALSE LIMIT 1")
    GraphThread selectByThreadName(@Param("threadName") String threadName);

}
