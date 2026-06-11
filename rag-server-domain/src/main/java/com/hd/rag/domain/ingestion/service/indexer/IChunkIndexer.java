package com.hd.rag.domain.ingestion.service.indexer;

import com.hd.rag.domain.ingestion.model.valobj.VectorChunk;

import java.util.List;

/**
 * 向量存储服务接口
 */
public interface IChunkIndexer {

    void batchInsert(List<VectorChunk> vectorChunks);

    void batchUpdate(List<VectorChunk> vectorChunks);

    void batchInsertOrUpdate(List<VectorChunk> vectorChunks);

    void doDelete(String chunkId);

    void batchDelete(List<String> chunkIds);
}
