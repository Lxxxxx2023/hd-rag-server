package com.hd.rag.domain.index.vector;

import com.hd.rag.domain.index.chunk.TextChunk;

import java.util.List;

public interface EmbeddingService {

    List<VectorChunk> embed(List<TextChunk> textChunkList);

}
