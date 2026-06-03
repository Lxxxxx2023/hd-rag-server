package com.hd.rag.domain.index.vector;

import cn.hutool.core.util.IdUtil;
import com.hd.rag.domain.index.chunk.TextChunk;
import jakarta.annotation.Resource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingServiceImpl implements EmbeddingService{

    @Resource(name = "embeddingModel")
    private EmbeddingModel embeddingModel;

    @Override
    public List<VectorChunk> embed(List<TextChunk> textChunkList) {
        List<String> textList = textChunkList.stream().map(TextChunk::getContent).toList();

        List<float[]> embedList = embeddingModel.embed(textList);

        int i = 0;
        List<VectorChunk> vectorChunks = new ArrayList<>();

    for (TextChunk textChunk : textChunkList) {
            vectorChunks.add(VectorChunk.builder()
                .chunkId(IdUtil.getSnowflakeNextIdStr())
                .content(textChunk.getContent())
                .embedding(embedList.get(i ++))
                .sourcePointers(textChunk.getSourcePointers())
                .build());
        }
        return vectorChunks;
    }
}
