-- 流水线节点表
DROP TABLE IF EXISTS t_pipeline_node;
CREATE TABLE t_pipeline_node(
    `id`            CHAR(32)     NOT NULL COMMENT '主键id',
    `name`          VARCHAR(64)  NOT NULL COMMENT '节点名称',
    `pipeline_id`   CHAR(32)     NOT NULL COMMENT '关联流水线id',
    `node_type` VARCHAR(32)  NOT NULL COMMENT '节点类型',
    `next_node_id`  CHAR(32)     NULL COMMENT '下一个节点id',
    `settings_json` TEXT         NULL COMMENT '节点配置（JSON）',
    `create_by`     VARCHAR(64)  NOT NULL COMMENT '创建人',
    `create_time`   DATETIME     NOT NULL COMMENT '创建时间',
    `update_by`     VARCHAR(64)  NOT NULL COMMENT '更新人',
    `update_time`   DATETIME     NOT NULL COMMENT '更新时间',
    `deleted`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否删除 0：正常 1：删除',
    PRIMARY KEY (id),
    INDEX idx_pipeline_id (pipeline_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线节点表';
