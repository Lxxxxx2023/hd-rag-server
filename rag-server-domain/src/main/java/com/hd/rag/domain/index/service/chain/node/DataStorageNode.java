package com.hd.rag.domain.index.service.chain.node;

import com.hd.rag.domain.index.service.chain.DataHandleContent;
import com.hd.rag.domain.index.service.chain.DataIndexResult;
import com.hd.rag.domain.index.service.chain.IDataIndexChain;
import org.springframework.stereotype.Component;

@Component
public class DataStorageNode implements IDataIndexChain {
    @Override
    public IDataIndexChain next() {
        return null;
    }

    @Override
    public DataIndexResult handle(DataHandleContent dataHandleContent) {
        return null;
    }
}
