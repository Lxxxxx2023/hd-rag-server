-- 用户表
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user(
    `id`          CHAR(32)     NOT NULL COMMENT '主键id',
    `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(256) NOT NULL COMMENT '密码（BCrypt密文）',
    `create_by`   VARCHAR(64)  NOT NULL COMMENT '创建人',
    `create_time` DATETIME     NOT NULL COMMENT '创建时间',
    `update_by`   VARCHAR(64)  NOT NULL COMMENT '更新人',
    `update_time` DATETIME     NOT NULL COMMENT '更新时间',
    `deleted`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否删除 0：正常 1：删除',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_username (username),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
