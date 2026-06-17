package com.hd.rag.domain.ingestion.adapter.repository;

import com.hd.rag.domain.ingestion.model.entity.PipelineEntity;

import java.util.List;

/**
 * 流水线仓储接口（Port）
 * <p>
 * 流水线节点是其内部实体。所有节点操作通过流水线实体完成，保证一致性。
 * </p>
 */
public interface IPipelineRepository {

    /**
     * 新增流水线（含全部节点，事务写入）
     */
    void insert(PipelineEntity entity);

    /**
     * 更新流水线（同步节点：先删后插）
     */
    void update(PipelineEntity entity);

    /**
     * 根据 ID 查询（含全部节点）
     */
    PipelineEntity findById(String id);

    /**
     * 根据文档id查询流水线列表（含全部节点）
     */
    List<PipelineEntity> findByKdId(String kdId);

    /**
     * 条件查询列表（分页，含全部节点）
     */
    List<PipelineEntity> findList(PipelineEntity query, Integer pageNum, Integer pageSize);

    /**
     * 逻辑删除（级联删除全部节点）
     */
    void deleteById(String id);
}
