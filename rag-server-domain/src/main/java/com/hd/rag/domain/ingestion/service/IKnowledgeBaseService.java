package com.hd.rag.domain.ingestion.service;

import com.hd.rag.api.ingestion.dto.request.KnowledgeBaseCreateReqDTO;
import com.hd.rag.api.ingestion.dto.request.KnowledgeBaseUpdateReqDTO;
import com.hd.rag.api.ingestion.dto.response.KnowledgeBaseRespDTO;

import java.util.List;

/**
 * 知识库领域服务
 */
public interface IKnowledgeBaseService {

    /**
     * 创建知识库
     */
    KnowledgeBaseRespDTO create(KnowledgeBaseCreateReqDTO reqDTO);

    /**
     * 更新知识库
     */
    KnowledgeBaseRespDTO update(KnowledgeBaseUpdateReqDTO reqDTO);

    /**
     * 根据 ID 查询
     */
    KnowledgeBaseRespDTO getById(String id);

    /**
     * 条件查询列表
     */
    List<KnowledgeBaseRespDTO> list(Integer pageNum, Integer pageSize);

    /**
     * 逻辑删除
     */
    void delete(String id);
}
