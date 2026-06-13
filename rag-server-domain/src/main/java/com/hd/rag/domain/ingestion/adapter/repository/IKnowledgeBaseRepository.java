package com.hd.rag.domain.ingestion.adapter.repository;

import com.hd.rag.domain.ingestion.model.entity.KnowledgeBaseEntity;

import java.util.List;
import java.util.Optional;

/**
 * 知识库仓储接口（Port）
 */
public interface IKnowledgeBaseRepository {

    /**
     * 新增知识库
     */
    void insert(KnowledgeBaseEntity knowledgeBaseEntity);

    /**
     * 更新知识库
     */
    void update(KnowledgeBaseEntity knowledgeBaseEntity);

    /**
     * 根据 ID 查询
     */
    Optional<KnowledgeBaseEntity> findById(String id);

    /**
     * 条件查询列表（分页）
     */
    List<KnowledgeBaseEntity> findList(KnowledgeBaseEntity query, Integer pageNum, Integer pageSize);

    /**
     * 逻辑删除
     */
    void deleteById(String id);
}
