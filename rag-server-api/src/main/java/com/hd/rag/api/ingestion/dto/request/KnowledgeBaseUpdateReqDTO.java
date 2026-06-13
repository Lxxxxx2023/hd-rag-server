package com.hd.rag.api.ingestion.dto.request;

/**
 * 更新知识库请求参数
 *
 * @param id         知识库id
 * @param name       知识库名称
 * @param intro      知识库简介
 * @param searchSet  检索设置
 */
public record KnowledgeBaseUpdateReqDTO(
        String id,
        String name,
        String intro,
        String searchSet) {
}
