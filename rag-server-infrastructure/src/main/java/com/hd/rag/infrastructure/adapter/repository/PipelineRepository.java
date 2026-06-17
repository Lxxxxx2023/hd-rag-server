package com.hd.rag.infrastructure.adapter.repository;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hd.rag.domain.ingestion.adapter.repository.IPipelineRepository;
import com.hd.rag.domain.ingestion.model.entity.PipelineEntity;
import com.hd.rag.domain.ingestion.model.entity.PipelineNodeEntity;
import com.hd.rag.infrastructure.dao.PipelineDao;
import com.hd.rag.infrastructure.dao.PipelineNodeDao;
import com.hd.rag.infrastructure.dao.po.PipelineNodePO;
import com.hd.rag.infrastructure.dao.po.PipelinePO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流水线仓储实现（MyBatis-Plus 适配器）
 * <p>
 * 节点为流水线内部实体。所有写操作在事务中完成，保证聚合一致性。
 * </p>
 */
@Repository
public class PipelineRepository implements IPipelineRepository {

    @Resource
    private PipelineDao pipelineDao;

    @Resource
    private PipelineNodeDao pipelineNodeDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insert(PipelineEntity entity) {
        pipelineDao.insert(toPO(entity));
        if (CollectionUtil.isNotEmpty(entity.getNodes())) {
            entity.getNodes().forEach(node -> {
                node.setPipelineId(entity.getId());
                pipelineNodeDao.insert(toNodePO(node));
            });
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PipelineEntity entity) {
        pipelineDao.updateById(toPO(entity));
        // 先删后插：保证节点数据与聚合根一致
        LambdaQueryWrapper<PipelineNodePO> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(PipelineNodePO::getPipelineId, entity.getId());
        pipelineNodeDao.delete(deleteWrapper);
        if (CollectionUtil.isNotEmpty(entity.getNodes())) {
            entity.getNodes().forEach(node -> {
                node.setPipelineId(entity.getId());
                pipelineNodeDao.insert(toNodePO(node));
            });
        }
    }

    @Override
    public PipelineEntity findById(String id) {
        PipelinePO pipelinePO = pipelineDao.selectById(id);
        if (pipelinePO == null) {
            return null;
        }
        PipelineEntity entity = toDomain(pipelinePO);
        entity.setNodes(loadNodes(entity.getId()));
        return entity;
    }

    @Override
    public List<PipelineEntity> findByKdId(String kdId) {
        LambdaQueryWrapper<PipelinePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PipelinePO::getKdId, kdId);
        wrapper.orderByDesc(PipelinePO::getCreateTime);
        return pipelineDao.selectList(wrapper).stream()
                .map(this::toDomain)
                .peek(entity -> entity.setNodes(loadNodes(entity.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<PipelineEntity> findList(PipelineEntity query, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<PipelinePO> wrapper = new LambdaQueryWrapper<>();
        if (query == null) {
            return List.of();
        }
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(PipelinePO::getName, query.getName());
        }
        if (StringUtils.hasText(query.getKdId())) {
            wrapper.eq(PipelinePO::getKdId, query.getKdId());
        }
        if (StringUtils.hasText(query.getCreateBy())) {
            wrapper.eq(PipelinePO::getCreateBy, query.getCreateBy());
        }
        wrapper.orderByDesc(PipelinePO::getCreateTime);
        IPage<PipelinePO> page = new Page<>(pageNum, pageSize);
        return pipelineDao.selectPage(page, wrapper).getRecords().stream()
                .map(this::toDomain)
                .peek(entity -> entity.setNodes(loadNodes(entity.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(String id) {
        // 级联删除节点
        LambdaQueryWrapper<PipelineNodePO> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(PipelineNodePO::getPipelineId, id);
        pipelineNodeDao.delete(deleteWrapper);
        pipelineDao.deleteById(id);
    }

    // ==================== 节点加载 ====================

    private List<PipelineNodeEntity> loadNodes(String pipelineId) {
        LambdaQueryWrapper<PipelineNodePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PipelineNodePO::getPipelineId, pipelineId);
        wrapper.orderByAsc(PipelineNodePO::getCreateTime);
        List<PipelineNodePO> nodePOs = pipelineNodeDao.selectList(wrapper);
        if (CollectionUtil.isEmpty(nodePOs)) {
            return new ArrayList<>();
        }
        return nodePOs.stream()
                .map(this::toNodeDomain)
                .collect(Collectors.toList());
    }

    // ==================== Pipeline PO ↔ Domain ====================

    private PipelinePO toPO(PipelineEntity entity) {
        PipelinePO po = new PipelinePO();
        po.setId(entity.getId());
        po.setKdId(entity.getKdId());
        po.setName(entity.getName());
        po.setIntro(entity.getIntro());
        po.setCreateBy(entity.getCreateBy());
        po.setCreateTime(entity.getCreateTime());
        po.setUpdateBy(entity.getUpdateBy());
        po.setUpdateTime(entity.getUpdateTime());
        return po;
    }

    private PipelineEntity toDomain(PipelinePO po) {
        PipelineEntity entity = new PipelineEntity();
        entity.setId(po.getId());
        entity.setKdId(po.getKdId());
        entity.setName(po.getName());
        entity.setIntro(po.getIntro());
        entity.setCreateBy(po.getCreateBy());
        entity.setCreateTime(po.getCreateTime());
        entity.setUpdateBy(po.getUpdateBy());
        entity.setUpdateTime(po.getUpdateTime());
        return entity;
    }

    // ==================== PipelineNode PO ↔ Domain ====================

    private PipelineNodePO toNodePO(PipelineNodeEntity entity) {
        PipelineNodePO po = new PipelineNodePO();
        po.setId(entity.getId());
        po.setName(entity.getName());
        po.setPipelineId(entity.getPipelineId());
        po.setNodeType(entity.getNodeType());
        po.setNextNodeId(entity.getNextNodeId());
        po.setSettingsJson(entity.getSettingsJson());
        po.setCreateBy(entity.getCreateBy());
        po.setCreateTime(entity.getCreateTime());
        po.setUpdateBy(entity.getUpdateBy());
        po.setUpdateTime(entity.getUpdateTime());
        return po;
    }

    private PipelineNodeEntity toNodeDomain(PipelineNodePO po) {
        PipelineNodeEntity entity = new PipelineNodeEntity();
        entity.setId(po.getId());
        entity.setName(po.getName());
        entity.setPipelineId(po.getPipelineId());
        entity.setNodeType(po.getNodeType());
        entity.setNextNodeId(po.getNextNodeId());
        entity.setSettingsJson(po.getSettingsJson());
        entity.setCreateBy(po.getCreateBy());
        entity.setCreateTime(po.getCreateTime());
        entity.setUpdateBy(po.getUpdateBy());
        entity.setUpdateTime(po.getUpdateTime());
        return entity;
    }
}
