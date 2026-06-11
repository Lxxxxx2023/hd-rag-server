package com.hd.rag.domain.ingestion.service.chunker;

import com.hd.rag.domain.ingestion.model.valobj.ChunkOptions;
import com.hd.rag.domain.ingestion.model.valobj.ChunkStrategyType;
import com.hd.rag.domain.ingestion.model.valobj.TextChunk;
import com.hd.rag.domain.ingestion.model.valobj.TextSegment;

import java.util.List;

/**
 * 切块策略接口
 */
public interface IChunkStrategy {

    ChunkStrategyType getType();

    List<TextChunk> chunk(List<TextSegment> textSegmentList, ChunkOptions chunkOptions);
}
