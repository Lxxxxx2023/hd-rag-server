package com.hd.rag.domain.index.service.load.document;

/**
 * 文档解析策略接口
 */
public interface IDocumentLoad {


    /**
     * 获取 文档解析器类型
     * @return 文档解析器类型
     */
    String getType();

    /**
     * 解析文件
     * @param o 传入参数
     * @return 解析结果
     */
    Object load(Object o);
}
