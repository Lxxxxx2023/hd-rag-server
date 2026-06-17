-- 流水线表
DROP TABLE IF EXISTS t_pipeline;
CREATE TABLE t_pipeline(
    `id`          CHAR(32)     NOT NULL COMMENT '主键id',
    `kd_id`       CHAR(32)     NOT NULL COMMENT '知识库文档id',
    `name`        VARCHAR(64)  NOT NULL COMMENT '流水线名称',
    `intro`       VARCHAR(255) NULL COMMENT '流水线简介',
    `create_by`   VARCHAR(64)  NOT NULL COMMENT '创建人',
    `create_time` DATETIME     NOT NULL COMMENT '创建时间',
    `update_by`   VARCHAR(64)  NOT NULL COMMENT '更新人',
    `update_time` DATETIME     NOT NULL COMMENT '更新时间',
    `deleted`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否删除 0：正常 1：删除',
    PRIMARY KEY (id),
    INDEX idx_kd_id (kd_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线表';
