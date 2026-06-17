package com.hd.rag.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流水线节点持久化对象
 */
@Data
@TableName("t_pipeline_node")
public class PipelineNodePO {

    /** 主键id */
    @TableId(type = IdType.INPUT)
    private String id;

    /** 节点名称 */
    @TableField("name")
    private String name;

    /** 关联流水线id */
    @TableField("pipeline_id")
    private String pipelineId;

    /** 节点类型 */
    @TableField("node_type")
    private String nodeType;

    /** 下一个节点id */
    @TableField("next_node_id")
    private String nextNodeId;

    /** 节点配置（JSON） */
    @TableField("settings_json")
    private String settingsJson;

    /** 创建人 */
    @TableField("create_by")
    private String createBy;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新人 */
    @TableField("update_by")
    private String updateBy;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 是否删除 0：正常 1：删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
