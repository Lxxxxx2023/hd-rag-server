-- 知识库基础信息表
DROP TABLE IF EXISTS t_knowledge_base;
CREATE TABLE t_knowledge_base(
    `id`          CHAR(32)     NOT NULL COMMENT '主键id',
    `name`        VARCHAR(64)  NOT NULL COMMENT '名称',
    `intro`       VARCHAR(255) COMMENT '简介',
    `search_set`  TEXT         COMMENT '检索设置',
    `segment_set` TEXT         NOT NULL COMMENT '分段设置',
    `create_by`   VARCHAR(64)  NOT NULL COMMENT '创建人',
    `create_time` DATETIME     NOT NULL COMMENT '创建时间',
    `update_by`   VARCHAR(64)  NOT NULL COMMENT '更新人',
    `update_time` DATETIME     NOT NULL COMMENT '更新时间',
    `deleted`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否删除 0：正常 1：删除',
    PRIMARY KEY (id),
    INDEX idx_deleted (deleted),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库基础信息表';
