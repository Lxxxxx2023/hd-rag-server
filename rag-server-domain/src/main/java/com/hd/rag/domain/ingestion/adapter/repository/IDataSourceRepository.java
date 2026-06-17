package com.hd.rag.domain.ingestion.adapter.repository;

import com.hd.rag.domain.ingestion.model.entity.DataSourceEntity;

import java.util.List;

/**
 * 数据源仓储接口（Port）
 */
public interface IDataSourceRepository {

    /**
     * 新增数据源
     */
    void insert(DataSourceEntity entity);

    /**
     * 更新数据源
     */
    void update(DataSourceEntity entity);

    /**
     * 根据 ID 查询
     */
    DataSourceEntity findById(String id);

    /**
     * 条件查询列表（分页）
     */
    List<DataSourceEntity> findList(DataSourceEntity query, Integer pageNum, Integer pageSize);

    /**
     * 逻辑删除
     */
    void deleteById(String id);
}
