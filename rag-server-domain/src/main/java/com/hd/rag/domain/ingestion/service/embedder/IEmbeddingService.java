package com.hd.rag.domain.ingestion.service.embedder;

import com.hd.rag.domain.ingestion.model.valobj.TextChunk;
import com.hd.rag.domain.ingestion.model.valobj.VectorChunk;

import java.util.List;

public interface IEmbeddingService {

    List<VectorChunk> embed(List<TextChunk> textChunkList);

}
