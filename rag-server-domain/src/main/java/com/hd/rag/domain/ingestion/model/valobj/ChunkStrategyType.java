package com.hd.rag.domain.ingestion.model.valobj;

import lombok.Getter;

@Getter
public enum ChunkStrategyType {
    FIXED("固定大小切块"),
    STRUCT_AWARE("结构感知切块");

    private final String type;

    ChunkStrategyType(String type) {
        this.type = type;
    }
}
