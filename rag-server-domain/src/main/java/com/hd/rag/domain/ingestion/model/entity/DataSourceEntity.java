package com.hd.rag.domain.ingestion.model.entity;

import com.hd.rag.domain.ingestion.model.valobj.DataSourceConfigVO;
import com.hd.rag.domain.ingestion.model.valobj.DataSourceType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据源实体
 */
@Data
public class DataSourceEntity {

    /** 主键id */
    private String id;

    /** 知识库id */
    private String kbId;

    /** 数据源名称 */
    private String name;

    /** 数据源描述 */
    private String description;

    /** 数据源类型 */
    private DataSourceType sourceType;

    /** 数据源配置 */
    private DataSourceConfigVO configVO;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新人 */
    private String updateBy;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
