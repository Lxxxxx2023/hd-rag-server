package com.hd.rag.domain.index.chunk;

import com.hd.rag.domain.index.parse.TextSegment;

import java.util.List;

/**
 * 切块策略接口
 */
public interface ChunkStrategy {

    ChunkStrategyType getType();

    List<TextChunk> chunk (List<TextSegment> textSegmentList, ChunkOptions chunkOptions);
}
