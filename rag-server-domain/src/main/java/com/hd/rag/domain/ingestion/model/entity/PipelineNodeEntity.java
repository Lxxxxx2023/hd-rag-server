package com.hd.rag.domain.ingestion.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流水线节点实体
 */
@Data
public class PipelineNodeEntity {

    /** 主键id */
    private String id;

    /** 节点名称 */
    private String name;

    /** 关联流水线id */
    private String pipelineId;

    /** 节点类型 */
    private String nodeType;

    /** 下一个节点id */
    private String nextNodeId;

    /** 节点配置（JSON） */
    private String settingsJson;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新人 */
    private String updateBy;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
