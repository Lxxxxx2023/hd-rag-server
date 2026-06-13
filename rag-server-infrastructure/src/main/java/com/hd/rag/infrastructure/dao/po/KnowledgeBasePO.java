package com.hd.rag.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库持久化对象（MyBatis-Plus 映射）
 */
@Data
@TableName("t_knowledge_base")
public class KnowledgeBasePO {

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;

    private String intro;

    @TableField("search_set")
    private String searchSet;

    @TableField("segment_set")
    private String segmentSet;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
