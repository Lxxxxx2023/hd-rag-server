package com.hd.rag.domain.index.parse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextSegment {

    /**
     * 片段类型
     */
    private SegmentType segmentType;

    /**
     * heading 层级 (h1=1, h2=2...)，非 heading 为 0
     */
    private int level;
    /**
     * 文本内容
     */
    private String text;
    /**
     * 溯源：页码/行号
     */
    private String sourcePointer;
}
