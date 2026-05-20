package com.hd.rag.domain.index.service.parse;

import com.hd.rag.domain.index.model.aggregate.CanonicalDocumentAggregate;
import com.hd.rag.domain.index.model.entity.RawSourceDocumentEntity;

public interface IParseStrategy {

    /**
     * 是否能处理该类型的数据
     * @param rawSourceDocumentEntity 数据
     * @return 是否能处理
     */
    boolean canHandle(RawSourceDocumentEntity rawSourceDocumentEntity);

    /**
     * 解析数据
     * @param rawSourceDocumentEntity 数据
     * @return 统一的文档数据
     */
    CanonicalDocumentAggregate parse(RawSourceDocumentEntity rawSourceDocumentEntity);

    /**
     * 检查数据类型
     * @param rawSourceDocumentEntity 数据
     * @return 数据类型
     */
    String probe(RawSourceDocumentEntity rawSourceDocumentEntity);
}
