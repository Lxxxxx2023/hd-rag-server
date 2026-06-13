package com.hd.rag.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hd.rag.domain.ingestion.adapter.repository.IKnowledgeBaseRepository;
import com.hd.rag.domain.ingestion.model.entity.KnowledgeBaseEntity;
import com.hd.rag.infrastructure.dao.KnowledgeBaseDao;
import com.hd.rag.infrastructure.dao.po.KnowledgeBasePO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 知识库仓储实现（MyBatis-Plus 适配器）
 */
@Repository
public class KnowledgeBaseRepository implements IKnowledgeBaseRepository {

    @Resource
    private KnowledgeBaseDao knowledgeBaseDao;

    @Override
    public void insert(KnowledgeBaseEntity knowledgeBaseEntity) {
        knowledgeBaseDao.insert(toPO(knowledgeBaseEntity));
    }

    @Override
    public void update(KnowledgeBaseEntity knowledgeBaseEntity) {
        knowledgeBaseDao.updateById(toPO(knowledgeBaseEntity));
    }

    @Override
    public Optional<KnowledgeBaseEntity> findById(String id) {
        return Optional.ofNullable(knowledgeBaseDao.selectById(id))
                .map(this::toDomain);
    }

    @Override
    public List<KnowledgeBaseEntity> findList(KnowledgeBaseEntity query, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<KnowledgeBasePO> wrapper = new LambdaQueryWrapper<>();
        if (query == null) {
            return List.of();
        }
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(KnowledgeBasePO::getName, query.getName());
        }
        if (StringUtils.hasText(query.getCreateBy())) {
            wrapper.eq(KnowledgeBasePO::getCreateBy, query.getCreateBy());
        }
        wrapper.orderByDesc(KnowledgeBasePO::getCreateTime);
        IPage<KnowledgeBasePO> page = new Page<>(pageNum, pageSize);
        return knowledgeBaseDao.selectPage(page, wrapper).getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        knowledgeBaseDao.deleteById(id);
    }

    // ==================== PO ↔ Domain ====================

    private KnowledgeBasePO toPO(KnowledgeBaseEntity kb) {
        KnowledgeBasePO po = new KnowledgeBasePO();
        po.setId(kb.getId());
        po.setName(kb.getName());
        po.setIntro(kb.getIntro());
        po.setSearchSet(kb.getSearchSet());
        po.setSegmentSet(kb.getSegmentSet());
        po.setCreateBy(kb.getCreateBy());
        po.setCreateTime(kb.getCreateTime());
        po.setUpdateBy(kb.getUpdateBy());
        po.setUpdateTime(kb.getUpdateTime());
        return po;
    }

    private KnowledgeBaseEntity toDomain(KnowledgeBasePO po) {
        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setId(po.getId());
        kb.setName(po.getName());
        kb.setIntro(po.getIntro());
        kb.setSearchSet(po.getSearchSet());
        kb.setSegmentSet(po.getSegmentSet());
        kb.setCreateBy(po.getCreateBy());
        kb.setCreateTime(po.getCreateTime());
        kb.setUpdateBy(po.getUpdateBy());
        kb.setUpdateTime(po.getUpdateTime());
        return kb;
    }
}
