package com.hd.rag.domain.ingestion.model.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 流水线聚合根
 * <p>
 * 流水线与节点是强一致的聚合关系：节点离开流水线无意义，流水线删除时节点应级联清除。
 * </p>
 */
@Data
public class PipelineEntity {

    /** 主键id */
    private String id;

    /** 知识库文档id */
    private String kdId;

    /** 流水线名称 */
    private String name;

    /** 简介 */
    private String intro;

    /** 节点列表 */
    private List<PipelineNodeEntity> nodes = new ArrayList<>();

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新人 */
    private String updateBy;

    /** 更新时间 */
    private LocalDateTime updateTime;

    // ==================== 领域行为 ====================

    /**
     * 添加节点
     */
    public void addNode(PipelineNodeEntity node) {
        if (this.nodes == null) {
            this.nodes = new ArrayList<>();
        }
        this.nodes.add(node);
    }

    /**
     * 获取根节点（链表头）
     */
    public PipelineNodeEntity getRootNode() {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        return nodes.get(0);
    }

    /**
     * 判断流水线是否为空
     */
    public boolean isEmpty() {
        return nodes == null || nodes.isEmpty();
    }
}
