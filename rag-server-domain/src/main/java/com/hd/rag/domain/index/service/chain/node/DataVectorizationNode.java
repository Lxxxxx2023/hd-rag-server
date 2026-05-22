package com.hd.rag.domain.index.service.chain.node;

import com.hd.rag.domain.index.service.chain.DataHandleContent;
import com.hd.rag.domain.index.service.chain.DataIndexResult;
import com.hd.rag.domain.index.service.chain.IDataIndexChain;
import jakarta.annotation.Resource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

@Component
public class DataVectorizationNode implements IDataIndexChain {

    @Resource
    private DataStorageNode dataStorageNode;

    @Resource
    private EmbeddingModel embeddingModel;

    @Override
    public IDataIndexChain next() {
        return dataStorageNode;
    }

    @Override
    public DataIndexResult handle(DataHandleContent dataHandleContent) {
        return dataStorageNode.handle(dataHandleContent);
    }
}
