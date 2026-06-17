package com.hd.rag.infrastructure.adapter.repository;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hd.rag.domain.ingestion.adapter.repository.IKnowledgeDocumentRepository;
import com.hd.rag.domain.ingestion.model.entity.KnowledgeDocumentEntity;
import com.hd.rag.domain.ingestion.model.valobj.DataSourceType;
import com.hd.rag.domain.ingestion.model.valobj.DocumentStatus;
import com.hd.rag.infrastructure.dao.KnowledgeDocumentDao;
import com.hd.rag.infrastructure.dao.po.KnowledgeDocumentPO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库文档仓储实现
 */
@Repository
public class KnowledgeDocumentRepository implements IKnowledgeDocumentRepository {

    @Resource
    private KnowledgeDocumentDao knowledgeDocumentDao;

    @Override
    public void insert(KnowledgeDocumentEntity entity) {
        knowledgeDocumentDao.insert(toPO(entity));
    }

    @Override
    public void update(KnowledgeDocumentEntity entity) {
        knowledgeDocumentDao.updateById(toPO(entity));
    }

    @Override
    public KnowledgeDocumentEntity findById(String id) {
        KnowledgeDocumentPO po = knowledgeDocumentDao.selectById(id);
        if (po == null) {
            return null;
        }
        return toDomain(po);
    }

    @Override
    public List<KnowledgeDocumentEntity> findDocumentByPipelineId(String pipelineId) {
        List<KnowledgeDocumentEntity> documentEntities = new ArrayList<>();

        LambdaQueryWrapper<KnowledgeDocumentPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeDocumentPO::getPipelineId, pipelineId);
        queryWrapper.eq(KnowledgeDocumentPO::getDeleted, 0);

        List<KnowledgeDocumentPO> knowledgeDocumentPOS = knowledgeDocumentDao.selectList(queryWrapper);

        if (CollectionUtil.isNotEmpty(knowledgeDocumentPOS)) {
            knowledgeDocumentPOS.forEach(po -> documentEntities.add(toDomain(po)));
        }

        return documentEntities;
    }

    @Override
    public List<KnowledgeDocumentEntity> findUnprocessDocumentByPipelineId(String pipelineId) {
        List<KnowledgeDocumentEntity> documentEntities = new ArrayList<>();

        LambdaQueryWrapper<KnowledgeDocumentPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeDocumentPO::getPipelineId, pipelineId);
        queryWrapper.eq(KnowledgeDocumentPO::getDeleted, 0);
        queryWrapper.eq(KnowledgeDocumentPO::getStatus, DocumentStatus.UPLOADED.getValue()); // 已上传

        List<KnowledgeDocumentPO> knowledgeDocumentPOS = knowledgeDocumentDao.selectList(queryWrapper);

        if (CollectionUtil.isNotEmpty(knowledgeDocumentPOS)) {
            knowledgeDocumentPOS.forEach(po -> documentEntities.add(toDomain(po)));
        }

        return documentEntities;
    }

    @Override
    public List<KnowledgeDocumentEntity> findList(KnowledgeDocumentEntity query, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<KnowledgeDocumentPO> wrapper = new LambdaQueryWrapper<>();
        if (query == null) {
            return List.of();
        }
        if (StringUtils.hasText(query.getDocName())) {
            wrapper.like(KnowledgeDocumentPO::getDocName, query.getDocName());
        }
        if (StringUtils.hasText(query.getKbId())) {
            wrapper.eq(KnowledgeDocumentPO::getKbId, query.getKbId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(KnowledgeDocumentPO::getStatus, query.getStatus().getValue());
        }
        if (query.getSourceType() != null) {
            wrapper.eq(KnowledgeDocumentPO::getSourceType, query.getSourceType().name());
        }
        if (StringUtils.hasText(query.getCreateBy())) {
            wrapper.eq(KnowledgeDocumentPO::getCreateBy, query.getCreateBy());
        }
        wrapper.orderByDesc(KnowledgeDocumentPO::getCreateTime);
        IPage<KnowledgeDocumentPO> page = new Page<>(pageNum, pageSize);
        return knowledgeDocumentDao.selectPage(page, wrapper).getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        knowledgeDocumentDao.deleteById(id);
    }

    // ==================== PO ↔ Domain ====================

    private KnowledgeDocumentPO toPO(KnowledgeDocumentEntity entity) {
        KnowledgeDocumentPO po = new KnowledgeDocumentPO();
        po.setId(entity.getId());
        po.setKbId(entity.getKbId());
        po.setDocName(entity.getDocName());
        po.setFileSize(entity.getFileSize());
        po.setFileType(entity.getFileType());
        po.setFileUrl(entity.getFileUrl());
        po.setStatus(entity.getStatus() != null ? entity.getStatus().getValue() : null);
        po.setPipelineId(entity.getPipelineId());
        po.setErrorMessage(entity.getErrorMessage());
        po.setTotalChunks(entity.getTotalChunks());
        po.setChunkCount(entity.getChunkCount());
        po.setSourceType(entity.getSourceType() != null ? entity.getSourceType().name() : null);
        po.setCreateBy(entity.getCreateBy());
        po.setCreateTime(entity.getCreateTime());
        po.setUpdateBy(entity.getUpdateBy());
        po.setUpdateTime(entity.getUpdateTime());
        return po;
    }

    private KnowledgeDocumentEntity toDomain(KnowledgeDocumentPO po) {
        return KnowledgeDocumentEntity.builder()
                .id(po.getId())
                .kbId(po.getKbId())
                .docName(po.getDocName())
                .fileSize(po.getFileSize())
                .fileType(po.getFileType())
                .fileUrl(po.getFileUrl())
                .status(DocumentStatus.fromValue(po.getStatus()))
                .pipelineId(po.getPipelineId())
                .errorMessage(po.getErrorMessage())
                .totalChunks(po.getTotalChunks())
                .chunkCount(po.getChunkCount())
                .sourceType(po.getSourceType() != null ? DataSourceType.valueOf(po.getSourceType()) : null)
                .createBy(po.getCreateBy())
                .createTime(po.getCreateTime())
                .updateBy(po.getUpdateBy())
                .updateTime(po.getUpdateTime())
                .build();
    }
}
