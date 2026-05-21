package com.hd.rag.domain.index.service.chain.node;

import com.hd.rag.domain.index.model.aggregate.CanonicalDocumentAggregate;
import com.hd.rag.domain.index.service.chain.DataHandleContent;
import com.hd.rag.domain.index.service.chain.DataIndexResult;
import com.hd.rag.domain.index.service.chain.IDataIndexChain;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataChunkNode implements IDataIndexChain {

    private static final int CHUNK_SIZE = 1000;
    private static final int OVERLAP = 200;

    @Resource
    private DataVectorizationNode dataVectorizationNode;

    @Override
    public IDataIndexChain next() {
        return dataVectorizationNode;
    }

    @Override
    public DataIndexResult handle(DataHandleContent dataHandleContent) {
        CanonicalDocumentAggregate aggregate = dataHandleContent.getCanonicalDocumentAggregate();
        if (aggregate != null && aggregate.getContent() != null) {
            List<String> chunks = splitText(aggregate.getContent(), CHUNK_SIZE, OVERLAP);
            aggregate.setChunks(chunks);
        }
        return dataVectorizationNode.handle(dataHandleContent);
    }

    private List<String> splitText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start += (chunkSize - overlap);
        }
        return chunks;
    }
}
