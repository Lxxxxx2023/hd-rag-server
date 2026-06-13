package com.hd.rag.api.ingestion.dto.response;

import java.time.LocalDateTime;

/**
 * 知识库响应参数
 *
 * @param id          知识库id
 * @param name        知识库名称
 * @param intro       知识库简介
 * @param searchSet   检索设置
 * @param segmentSet  分段设置
 * @param createBy    创建人
 * @param createTime  创建时间
 * @param updateBy    更新人
 * @param updateTime  更新时间
 */
public record KnowledgeBaseRespDTO(
        String id,
        String name,
        String intro,
        String searchSet,
        String segmentSet,
        String createBy,
        LocalDateTime createTime,
        String updateBy,
        LocalDateTime updateTime) {
}
