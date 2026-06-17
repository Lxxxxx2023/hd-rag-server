package com.hd.rag.infrastructure.adapter.repository;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hd.rag.domain.ingestion.adapter.repository.IDataSourceRepository;
import com.hd.rag.domain.ingestion.model.entity.DataSourceEntity;
import com.hd.rag.domain.ingestion.model.valobj.DataSourceConfigVO;
import com.hd.rag.domain.ingestion.model.valobj.DataSourceType;
import com.hd.rag.infrastructure.dao.DataSourceDao;
import com.hd.rag.infrastructure.dao.po.DataSourcePO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据源仓储实现（MyBatis-Plus 适配器）
 */
@Repository
public class DataSourceRepository implements IDataSourceRepository {

    @Resource
    private DataSourceDao dataSourceDao;

    @Override
    public void insert(DataSourceEntity entity) {
        dataSourceDao.insert(toPO(entity));
    }

    @Override
    public void update(DataSourceEntity entity) {
        dataSourceDao.updateById(toPO(entity));
    }

    @Override
    public DataSourceEntity findById(String id) {
        DataSourcePO po = dataSourceDao.selectById(id);
        if (po == null) {
            return null;
        }
        return toDomain(po);
    }

    @Override
    public List<DataSourceEntity> findList(DataSourceEntity query, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<DataSourcePO> wrapper = new LambdaQueryWrapper<>();
        if (query == null) {
            return List.of();
        }
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(DataSourcePO::getName, query.getName());
        }
        if (StringUtils.hasText(query.getKbId())) {
            wrapper.eq(DataSourcePO::getKbId, query.getKbId());
        }
        if (query.getSourceType() != null) {
            wrapper.eq(DataSourcePO::getSourceType, query.getSourceType().name());
        }
        if (StringUtils.hasText(query.getCreateBy())) {
            wrapper.eq(DataSourcePO::getCreateBy, query.getCreateBy());
        }
        wrapper.orderByDesc(DataSourcePO::getCreateTime);
        IPage<DataSourcePO> page = new Page<>(pageNum, pageSize);
        return dataSourceDao.selectPage(page, wrapper).getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        dataSourceDao.deleteById(id);
    }

    // ==================== PO ↔ Domain ====================

    private DataSourcePO toPO(DataSourceEntity entity) {
        DataSourcePO po = new DataSourcePO();
        po.setId(entity.getId());
        po.setKbId(entity.getKbId());
        po.setName(entity.getName());
        po.setDescription(entity.getDescription());
        po.setSourceType(entity.getSourceType() != null ? entity.getSourceType().name() : null);
        po.setConfigJson(entity.getConfigVO() != null ? JSON.toJSONString(entity.getConfigVO()) : null);
        po.setCreateBy(entity.getCreateBy());
        po.setCreateTime(entity.getCreateTime());
        po.setUpdateBy(entity.getUpdateBy());
        po.setUpdateTime(entity.getUpdateTime());
        return po;
    }

    private DataSourceEntity toDomain(DataSourcePO po) {
        DataSourceEntity entity = new DataSourceEntity();
        entity.setId(po.getId());
        entity.setKbId(po.getKbId());
        entity.setName(po.getName());
        entity.setDescription(po.getDescription());
        entity.setSourceType(po.getSourceType() != null ? DataSourceType.valueOf(po.getSourceType()) : null);
        entity.setConfigVO(parseConfig(po.getConfigJson(), po.getSourceType()));
        entity.setCreateBy(po.getCreateBy());
        entity.setCreateTime(po.getCreateTime());
        entity.setUpdateBy(po.getUpdateBy());
        entity.setUpdateTime(po.getUpdateTime());
        return entity;
    }

    // ==================== JSON 序列化 ====================

    private DataSourceConfigVO parseConfig(String configJson, String sourceType) {
        if (!StringUtils.hasText(configJson)) {
            return null;
        }
        Class<? extends DataSourceConfigVO> clazz = resolveConfigClass(sourceType);
        return JSON.parseObject(configJson, clazz);
    }

    private Class<? extends DataSourceConfigVO> resolveConfigClass(String sourceType) {
        if (sourceType == null) {
            return DataSourceConfigVO.class;
        }
        return switch (DataSourceType.valueOf(sourceType)) {
            case MANUAL_UPLOAD -> DataSourceConfigVO.ManualUploadDataSourceConfig.class;
            case OSS -> DataSourceConfigVO.OSSDatasourceConfig.class;
            case FEI_SHU -> DataSourceConfigVO.FeiShuDataSourceConfig.class;
        };
    }
}
