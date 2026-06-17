package com.hd.rag.domain.ingestion.model.valobj;

import lombok.Getter;

/**
 * 文档处理状态枚举
 */
@Getter
public enum DocumentStatus {
    UPLOADED("已上传", 0),
    PARSING("解析中", 1),
    PARSED("解析完成", 2),
    CLEANING("清洗中", 3),
    CLEANED("清洗完成", 4),
    CHUNKING("分块中", 5),
    CHUNKED("分块完成", 6),
    EMBEDDING("向量化中", 7),
    EMBEDDED("向量化完成", 8),
    INDEXING("索引写入中", 9),
    READY("就绪", 10),
    FAILED("失败", -1);

    private final String description;
    private final Integer value;

    DocumentStatus(String description, Integer value) {
        this.description = description;
        this.value = value;
    }

    /**
     * 根据数值获取枚举
     */
    public static DocumentStatus fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (DocumentStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }
}
