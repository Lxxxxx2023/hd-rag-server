package com.hd.rag.domain.index.service.chain;

import com.hd.rag.domain.index.model.aggregate.CanonicalDocumentAggregate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据索引上下文
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataHandleContent {

    /**
     * 文件类型
     */
    private String mimeType;

    /**
     * 文件字节
     */
    private byte[] content;

    CanonicalDocumentAggregate canonicalDocumentAggregate;

}
