package com.hd.rag.domain.index.service.chain.node;

import com.hd.rag.domain.index.model.aggregate.CanonicalDocumentAggregate;
import com.hd.rag.domain.index.service.chain.DataHandleContent;
import com.hd.rag.domain.index.service.chain.DataIndexResult;
import com.hd.rag.domain.index.service.chain.IDataIndexChain;
import jakarta.annotation.Resource;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Component;

import java.util.List;

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
        CanonicalDocumentAggregate aggregate = dataHandleContent.getCanonicalDocumentAggregate();
        if (aggregate != null && aggregate.getChunks() != null && !aggregate.getChunks().isEmpty()) {
            EmbeddingResponse response = embeddingModel.embedForResponse(aggregate.getChunks());
            List<float[]> embeddings = response.getResults().stream()
                    .map(Embedding::getOutput)
                    .toList();
            aggregate.setEmbeddings(embeddings);
        }
        return dataStorageNode.handle(dataHandleContent);
    }
}
