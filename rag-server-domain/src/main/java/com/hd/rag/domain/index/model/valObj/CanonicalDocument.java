package com.hd.rag.domain.index.model.valObj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CanonicalDocument {

    private String sourceType;
    private String mimeType;

    /** 文档级别元数据（文件名、作者、创建时间等） */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /** ContentTree 根节点 */
    private ContentNode root;
}
