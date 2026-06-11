package com.hd.rag.domain.ingestion.service.embedder;

import cn.hutool.core.util.IdUtil;
import com.hd.rag.domain.ingestion.model.valobj.TextChunk;
import com.hd.rag.domain.ingestion.model.valobj.VectorChunk;
import jakarta.annotation.Resource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingServiceImpl implements IEmbeddingService {

    @Resource(name = "embeddingModel")
    private EmbeddingModel embeddingModel;

    @Override
    public List<VectorChunk> embed(List<TextChunk> textChunkList) {
        List<VectorChunk> vectorChunks = new ArrayList<>();
        for (TextChunk textChunk : textChunkList) {
            float[] embedding = embeddingModel.embed(textChunk.getContent());
            VectorChunk vectorChunk = VectorChunk.builder()
                    .chunkId(IdUtil.simpleUUID())
                    .content(textChunk.getContent())
                    .sourcePointers(textChunk.getSourcePointers())
                    .embedding(embedding)
                    .build();
            vectorChunks.add(vectorChunk);
        }
        return vectorChunks;
    }
}
