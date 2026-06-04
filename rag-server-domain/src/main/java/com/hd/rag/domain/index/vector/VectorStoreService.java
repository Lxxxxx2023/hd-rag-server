package com.hd.rag.domain.index.vector;

import java.util.List;

/**
 * 向量存储服务
 */
public interface VectorStoreService {

    void batchInsert(List<VectorChunk> vectorChunks);

    void batchUpdate(List<VectorChunk> vectorChunks);

    void batchInsertOrUpdate(List<VectorChunk> vectorChunks);

    void doDelete(String chunkId);

    void batchDelete(List<String> chunkIds);
}