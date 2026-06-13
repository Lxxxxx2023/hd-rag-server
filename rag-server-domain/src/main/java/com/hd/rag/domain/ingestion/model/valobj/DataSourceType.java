package com.hd.rag.domain.ingestion.model.valobj;

import lombok.Getter;

/**
 * 数据来源枚举
 */
@Getter
public enum DataSourceType {
    MANUAL_UPLOAD("MANUAL_UPLOAD");

    private final String description;

    DataSourceType(String description) {
        this.description = description;
    }
}
