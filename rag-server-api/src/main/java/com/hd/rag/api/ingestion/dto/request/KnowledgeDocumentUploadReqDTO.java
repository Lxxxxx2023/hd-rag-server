package com.hd.rag.api.ingestion.dto.request;

import org.springframework.web.multipart.MultipartFile;

/**
 * 上传文件请求参数
 * @param kbId 知识库id
 * @param file 文件
 * @param path 文件路径
 */
public record KnowledgeDocumentUploadReqDTO (
        String kbId,
        MultipartFile file,
        String path){
}
