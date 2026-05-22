package com.hd.rag.domain.index.model.valObj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentNode {

    private ContentNodeType type;

    /** 页码 (PDF) / 行号 / CSS Selector 等 */
    private String sourcePointer;

    /** 在页面上的位置 */
    private Float x;
    private Float y;
    private Float width;
    private Float height;

    private String markdown;
    private String plainText;

    @Builder.Default
    private List<ContentNode> children = new ArrayList<>();
}
