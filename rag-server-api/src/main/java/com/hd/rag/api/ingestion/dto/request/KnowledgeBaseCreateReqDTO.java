package com.hd.rag.api.ingestion.dto.request;

/**
 * 创建知识库请求参数
 *
 * @param name       知识库名称
 * @param intro      知识库简介
 * @param searchSet  检索设置
 * @param segmentSet 分段设置
 */
public record KnowledgeBaseCreateReqDTO(
        String name,
        String intro,
        String searchSet,
        String segmentSet) {
}
