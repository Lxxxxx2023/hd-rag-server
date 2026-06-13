package com.hd.rag.infrastructure.adapter.repository;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson2.JSON;
import com.hd.rag.domain.ingestion.model.valobj.VectorChunk;
import com.hd.rag.domain.ingestion.service.indexer.IChunkIndexer;
import com.pgvector.PGvector;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service("pgVectorStore")
@AllArgsConstructor
public class PGVectorIndexWriter implements IChunkIndexer, InitializingBean {

    private final JdbcTemplate jdbcTemplate;

    private static final int BATCHSIZE = 500;

    @Override
    public void batchInsert(List<VectorChunk> vectorChunks) {
        List<List<VectorChunk>> batchVectorChunks = CollectionUtil.split(vectorChunks, BATCHSIZE);
        batchVectorChunks.forEach(this::batchInsertOrUpdate);
    }

    @Override
    public void batchUpdate(List<VectorChunk> vectorChunks) {
        List<List<VectorChunk>> batchVectorChunks = CollectionUtil.split(vectorChunks, BATCHSIZE);
        batchVectorChunks.forEach(this::batchInsertOrUpdate);
    }

    @Override
    public void batchInsertOrUpdate(List<VectorChunk> batchVectorChunks) {
        String sql = "INSERT INTO rag_vector_store (id, content, metadata, embedding) VALUES(?, ?, ?::jsonb, ?)" +
                "ON CONFLICT (id) DO UPDATE SET content = ? , metadata = ?::jsonb , embedding = ? ";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@NotNull PreparedStatement ps, int i) throws SQLException {
                VectorChunk vectorChunk = batchVectorChunks.get(i);
                Map<String, Object> metadata = vectorChunk.getMetadata();
                PGvector pgVector = new PGvector(vectorChunk.getEmbedding());
                StatementCreatorUtils.setParameterValue(ps, 1, SqlTypeValue.TYPE_UNKNOWN, vectorChunk.getChunkId());
                StatementCreatorUtils.setParameterValue(ps, 2, SqlTypeValue.TYPE_UNKNOWN, vectorChunk.getContent());
                StatementCreatorUtils.setParameterValue(ps, 3, SqlTypeValue.TYPE_UNKNOWN, JSON.toJSONString(metadata));
                StatementCreatorUtils.setParameterValue(ps, 4, SqlTypeValue.TYPE_UNKNOWN, pgVector);
                StatementCreatorUtils.setParameterValue(ps, 5, SqlTypeValue.TYPE_UNKNOWN, vectorChunk.getContent());
                StatementCreatorUtils.setParameterValue(ps, 6, SqlTypeValue.TYPE_UNKNOWN, JSON.toJSONString(metadata));
                StatementCreatorUtils.setParameterValue(ps, 7, SqlTypeValue.TYPE_UNKNOWN, pgVector);
            }

            @Override
            public int getBatchSize() {
                return batchVectorChunks.size();
            }
        });
    }
    @Override
    public void doDelete(String chunkId) {
        jdbcTemplate.update("DELETE FROM rag_vector_store WHERE id = ?",  chunkId);
    }

    @Override
    public void batchDelete(List<String> chunkIds) {
        List<List<String>> batchDeleteChunkIds = CollectionUtil.split(chunkIds, BATCHSIZE);
        batchDeleteChunkIds.forEach(batchChunkIds -> {
            String sql = "DELETE FROM rag_vector_store WHERE id IN (%s)";
            jdbcTemplate.update(String.format(sql, String.join(",", Collections.unmodifiableList(batchChunkIds))));
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // TODO 自动创建向量表？
    }

}
