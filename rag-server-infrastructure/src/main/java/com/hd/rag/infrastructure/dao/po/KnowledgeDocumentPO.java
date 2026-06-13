package com.hd.rag.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档持久化对象（MyBatis-Plus 映射）
 */
@Data
@TableName("t_knowledge_document")
public class KnowledgeDocumentPO {

    /** 主键id */
    @TableId(type = IdType.INPUT)
    private String id;

    /** 知识库id */
    @TableField("kb_id")
    private String kbId;

    /** 文档名称 */
    @TableField("doc_name")
    private String docName;

    /** 文件大小 */
    @TableField("file_size")
    private Long fileSize;

    /** 文件类型 */
    @TableField("file_type")
    private String fileType;

    /** 文件地址 */
    @TableField("file_url")
    private String fileUrl;

    /** 处理状态 */
    @TableField("status")
    private String status;

    /** 关联管道id */
    @TableField("pipeline_id")
    private String pipelineId;

    /** 错误信息 */
    @TableField("error_message")
    private String errorMessage;

    /** 分块总数 */
    @TableField("total_chunks")
    private Integer totalChunks;

    /** 已完成分块数 */
    @TableField("chunk_count")
    private Integer chunkCount;

    /** 文档来源类型 */
    @TableField("source_type")
    private String sourceType;

    /** 创建人 */
    @TableField("create_by")
    private String createBy;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新人 */
    @TableField("update_by")
    private String updateBy;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 是否删除 0：正常 1：删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
