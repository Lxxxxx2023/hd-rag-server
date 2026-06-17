package com.hd.rag.domain.ingestion.model.valobj;

import lombok.Getter;

/**
 * 数据来源枚举
 */
@Getter
public enum DataSourceType {
    MANUAL_UPLOAD("MANUAL_UPLOAD", "手动上传"),
    OSS("OSS", "对象存储中获取"),
    FEI_SHU("FEI_SHU", "飞书")
    ;

    private final String type;
    private final String description;

    DataSourceType(String type, String description) {
        this.type = type;
        this.description = description;
    }
}
