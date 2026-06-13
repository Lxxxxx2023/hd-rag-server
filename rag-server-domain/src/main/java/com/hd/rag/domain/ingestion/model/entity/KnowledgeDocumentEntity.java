package com.hd.rag.domain.ingestion.model.entity;

import com.hd.rag.domain.ingestion.model.valobj.DataSourceType;
import com.hd.rag.domain.ingestion.model.valobj.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库文档领域实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeDocumentEntity {

    /** 主键id */
    private String id;

    /** 知识库id */
    private String kbId;

    /** 文档名称 */
    private String docName;

    /** 文件大小 */
    private Long fileSize;

    /** 文件类型 */
    private String fileType;

    /** 文件地址 */
    private String fileUrl;

    /** 处理状态 */
    private DocumentStatus status;

    /** 关联管道id */
    private String pipelineId;

    /** 错误信息 */
    private String errorMessage;

    /** 分块总数 */
    private Integer totalChunks;

    /** 已完成分块数 */
    private Integer chunkCount;

    /** 文档来源类型 */
    private DataSourceType sourceType;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新人 */
    private String updateBy;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
