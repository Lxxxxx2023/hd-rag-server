package com.hd.rag.domain.ingestion.adapter.repository;

import com.hd.rag.domain.ingestion.model.entity.KnowledgeDocumentEntity;

import java.util.List;

/**
 * 知识库文档仓储接口
 */
public interface IKnowledgeDocumentRepository {

    /**
     * 新增文档
     */
    void insert(KnowledgeDocumentEntity entity);

    /**
     * 更新文档
     */
    void update(KnowledgeDocumentEntity entity);

    /**
     * 根据 ID 查询
     */
    KnowledgeDocumentEntity findById(String id);

    /**
     * 获取文档
     * @param pipelineId 流水线id
     * @return 文档
     */
    List<KnowledgeDocumentEntity> findDocumentByPipelineId(String pipelineId);

    /**
     * 获取未处理的文档
     * @param pipelineId 流水线id
     * @return 未处理的文档
     */
    List<KnowledgeDocumentEntity> findUnprocessDocumentByPipelineId(String pipelineId);

    /**
     * 条件查询列表（分页）
     */
    List<KnowledgeDocumentEntity> findList(KnowledgeDocumentEntity query, Integer pageNum, Integer pageSize);

    /**
     * 逻辑删除
     */
    void deleteById(String id);
}
