package com.hd.rag.domain.ingestion.service;

import com.hd.rag.api.ingestion.dto.request.KnowledgeDocumentUploadReqDTO;

/**
 * 知识库文档服务
 */
public interface IKnowledgeDocumentService {

    /**
     * 上传文档
     * @param reqDTO 上传文档请求参数
     */
    void uploadDocument(KnowledgeDocumentUploadReqDTO reqDTO) throws Exception;
}
