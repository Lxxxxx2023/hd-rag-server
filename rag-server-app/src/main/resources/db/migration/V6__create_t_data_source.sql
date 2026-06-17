-- 数据源表
DROP TABLE IF EXISTS t_data_source;
CREATE TABLE t_data_source(
    `id`          CHAR(32)     NOT NULL COMMENT '主键id',
    `kb_id`       CHAR(32)     NOT NULL COMMENT '知识库id',
    `name`        VARCHAR(64)  NOT NULL COMMENT '数据源名称',
    `description` VARCHAR(255) NULL COMMENT '数据源描述',
    `source_type` VARCHAR(32)  NOT NULL COMMENT '数据源类型（MANUAL_UPLOAD / OSS / FEI_SHU）',
    `config_json` TEXT         NULL COMMENT '数据源配置（JSON）',
    `create_by`   VARCHAR(64)  NOT NULL COMMENT '创建人',
    `create_time` DATETIME     NOT NULL COMMENT '创建时间',
    `update_by`   VARCHAR(64)  NOT NULL COMMENT '更新人',
    `update_time` DATETIME     NOT NULL COMMENT '更新时间',
    `deleted`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否删除 0：正常 1：删除',
    PRIMARY KEY (id),
    INDEX idx_kb_id (kb_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源表';
