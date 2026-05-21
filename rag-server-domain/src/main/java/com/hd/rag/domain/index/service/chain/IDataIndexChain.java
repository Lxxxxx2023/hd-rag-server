package com.hd.rag.domain.index.service.chain;

/**
 * 数据索引责任链
 */
public interface IDataIndexChain {

    IDataIndexChain next();

    DataIndexResult handle(DataHandleContent dataHandleContent) throws Exception;
}
