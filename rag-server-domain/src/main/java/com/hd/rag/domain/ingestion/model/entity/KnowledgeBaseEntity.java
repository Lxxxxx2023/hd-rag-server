package com.hd.rag.domain.ingestion.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库领域实体
 */
@Data
public class KnowledgeBaseEntity {

    /**
     * 主键id
     */
    private String id;

    /**
     * 名称
     */
    private String name;

    /**
     * 简介
     */
    private String intro;

    /**
     * 检索设置 (JSON)
     */
    private String searchSet;

    /**
     * 分段设置 (JSON)
     */
    private String segmentSet;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
