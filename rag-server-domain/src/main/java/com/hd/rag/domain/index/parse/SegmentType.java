package com.hd.rag.domain.index.parse;

import lombok.Getter;

@Getter
public enum SegmentType {

    HEADING("标题"),
    PARAGRAPH("段落"),
    TABLE("表格"),
    CODE("代码块"),
    LIST("列表");

    private final String name;

    SegmentType(String name) {
        this.name = name;
    }
}
