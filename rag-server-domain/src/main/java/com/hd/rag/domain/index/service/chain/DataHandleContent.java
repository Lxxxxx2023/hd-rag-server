package com.hd.rag.domain.index.service.chain;

import com.hd.rag.domain.index.model.valObj.CanonicalDocument;
import lombok.Data;

/**
 * 数据处理上下文
 */
@Data
public class DataHandleContent {

    /**
     * 原始数据
     */
    private RawData rawData;

    /**
     * 标准文档数据
     */
    private CanonicalDocument canonicalDocument;
}
