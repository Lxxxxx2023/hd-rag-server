package com.hd.rag.domain.ingestion.model.valobj;

import lombok.Getter;

@Getter
public enum ParserType {
    MARKDOWN("markdown"),
    COMMON("common");
    private final String type;


    ParserType(String type) {
        this.type = type;
    }

}
