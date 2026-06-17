package com.hd.rag.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流水线持久化对象
 */
@Data
@TableName("t_pipeline")
public class PipelinePO {

    /** 主键id */
    @TableId(type = IdType.INPUT)
    private String id;

    /** 知识库文档id */
    @TableField("kd_id")
    private String kdId;

    /** 流水线名称 */
    @TableField("name")
    private String name;

    /** 流水线简介 */
    @TableField("intro")
    private String intro;

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
