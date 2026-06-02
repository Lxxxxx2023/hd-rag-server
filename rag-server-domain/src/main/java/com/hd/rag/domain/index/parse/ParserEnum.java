package com.hd.rag.domain.index.parse;

import lombok.Getter;

@Getter
public enum ParserEnum {
    MARKDOWN("markdown"),
    COMMON("common");
    private final String type;


    ParserEnum(String type) {
        this.type = type;
    }

}
