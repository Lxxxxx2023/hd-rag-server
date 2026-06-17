package com.hd.rag.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据源持久化对象
 */
@Data
@TableName("t_data_source")
public class DataSourcePO {

    /** 主键id */
    @TableId(type = IdType.INPUT)
    private String id;

    /** 知识库id */
    @TableField("kb_id")
    private String kbId;

    /** 数据源名称 */
    @TableField("name")
    private String name;

    /** 数据源描述 */
    @TableField("description")
    private String description;

    /** 数据源类型 */
    @TableField("source_type")
    private String sourceType;

    /** 数据源配置（JSON） */
    @TableField("config_json")
    private String configJson;

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
