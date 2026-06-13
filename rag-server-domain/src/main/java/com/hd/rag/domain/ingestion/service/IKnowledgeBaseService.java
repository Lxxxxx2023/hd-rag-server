package com.hd.rag.domain.ingestion.service;

import com.hd.rag.domain.ingestion.model.entity.KnowledgeBaseEntity;

import java.util.List;

/**
 * 知识库领域服务
 */
public interface IKnowledgeBaseService {

    /**
     * 创建知识库
     */
    KnowledgeBaseEntity create(KnowledgeBaseEntity knowledgeBaseEntity);

    /**
     * 更新知识库
     */
    KnowledgeBaseEntity update(KnowledgeBaseEntity knowledgeBaseEntity);

    /**
     * 根据 ID 查询
     */
    KnowledgeBaseEntity getById(String id);

    /**
     * 条件查询列表
     */
    List<KnowledgeBaseEntity> list(KnowledgeBaseEntity query, Integer pageNum, Integer pageSize);

    /**
     * 逻辑删除
     */
    void delete(String id);
}
