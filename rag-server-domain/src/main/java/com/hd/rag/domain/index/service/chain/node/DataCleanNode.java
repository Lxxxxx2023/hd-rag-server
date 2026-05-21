package com.hd.rag.domain.index.service.chain.node;

import com.hd.rag.domain.index.service.chain.DataHandleContent;
import com.hd.rag.domain.index.service.chain.DataIndexResult;
import com.hd.rag.domain.index.service.chain.IDataIndexChain;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class DataCleanNode implements IDataIndexChain {

    @Resource
    private DataChunkNode dataChunkNode;
    @Override
    public IDataIndexChain next() {
        return dataChunkNode;
    }

    @Override
    public DataIndexResult handle(DataHandleContent dataHandleContent) {
        return dataChunkNode.handle(dataHandleContent);
    }
}
