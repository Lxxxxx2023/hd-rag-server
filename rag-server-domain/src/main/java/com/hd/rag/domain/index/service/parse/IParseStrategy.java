package com.hd.rag.domain.index.service.parse;

import com.hd.rag.domain.index.model.valObj.CanonicalDocument;
import com.hd.rag.domain.index.service.chain.RawData;

public interface IParseStrategy {

    /**
     * 是否能处理该类型的数据
     * @param mimeType 数据类型
     * @return 是否能处理
     */
    boolean canHandle(String mimeType);

    /**
     * 解析数据
     * @param rawData 源数据
     * @return 统一的文档数据
     */
    CanonicalDocument parse(RawData rawData);
}
