package com.lcl.ai.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author lcl
 * @since 2026-02-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("graph_checkpoint")
public class GraphCheckpoint implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "checkpoint_id", type = IdType.AUTO)
    private String checkpointId;

    private String threadId;

    private String nodeId;

    private String nextNodeId;

    private String stateData;

    private LocalDateTime savedAt;


}
