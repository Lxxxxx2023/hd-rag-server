package com.hd.rag.domain.ingestion.model.valobj;

import lombok.Getter;

/**
 * 文档处理状态枚举
 */
@Getter
public enum DocumentStatus {
    UPLOADED("已上传"),
    PARSING("解析中"),
    PARSED("解析完成"),
    CLEANING("清洗中"),
    CLEANED("清洗完成"),
    CHUNKING("分块中"),
    CHUNKED("分块完成"),
    EMBEDDING("向量化中"),
    EMBEDDED("向量化完成"),
    INDEXING("索引写入中"),
    READY("就绪"),
    FAILED("失败");

    private final String description;

    DocumentStatus(String description) {
        this.description = description;
    }
}
