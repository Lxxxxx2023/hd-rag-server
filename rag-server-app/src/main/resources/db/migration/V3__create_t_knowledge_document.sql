-- 知识库文档表
DROP TABLE IF EXISTS t_knowledge_document;
CREATE TABLE t_knowledge_document(
    `id`            CHAR(32)     NOT NULL COMMENT '主键id',
    `kb_id`         CHAR(32)     NOT NULL COMMENT '知识库id',
    `doc_name`      VARCHAR(64)  NOT NULL COMMENT '文档名称',
    `file_size`     BIGINT       NOT NULL COMMENT '文件大小',
    `file_type`     VARCHAR(32)  NOT NULL COMMENT '文件类型',
    `file_url`      VARCHAR(255) NOT NULL COMMENT '文件地址',
    `status`        INT          NOT NULL DEFAULT 0 COMMENT '处理状态（0:已上传 1:解析中 2:解析完成 3:清洗中 4:清洗完成 5:分块中 6:分块完成 7:向量化中 8:向量化完成 9:索引写入中 10:就绪 -1:失败）',
    `pipeline_id`   CHAR(32)     NULL COMMENT '关联管道id',
    `error_message` TEXT         NULL COMMENT '错误信息',
    `total_chunks`  INT          NULL COMMENT '分块总数',
    `chunk_count`   INT          NULL DEFAULT 0 COMMENT '已完成分块数',
    `source_type`   VARCHAR(32)  NOT NULL DEFAULT 'MANUAL_UPLOAD' COMMENT '文档来源类型',
    `create_by`     VARCHAR(64)  NOT NULL COMMENT '创建人',
    `create_time`   DATETIME     NOT NULL COMMENT '创建时间',
    `update_by`     VARCHAR(64)  NOT NULL COMMENT '更新人',
    `update_time`   DATETIME     NOT NULL COMMENT '更新时间',
    `deleted`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否删除 0：正常 1：删除',
    PRIMARY KEY (id),
    INDEX idx_kb_id (kb_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';
