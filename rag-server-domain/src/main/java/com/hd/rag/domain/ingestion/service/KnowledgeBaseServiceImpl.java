package com.hd.rag.domain.ingestion.service;

import cn.hutool.core.util.IdUtil;
import com.hd.rag.domain.ingestion.adapter.repository.IKnowledgeBaseRepository;
import com.hd.rag.domain.ingestion.model.entity.KnowledgeBaseEntity;
import com.hd.rag.types.exception.ClientException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库领域服务实现
 */
@Service
public class KnowledgeBaseServiceImpl implements IKnowledgeBaseService {

    @Resource
    private IKnowledgeBaseRepository knowledgeBaseRepository;

    @Override
    public KnowledgeBaseEntity create(KnowledgeBaseEntity knowledgeBaseEntity) {
        knowledgeBaseEntity.setId(IdUtil.getSnowflakeNextIdStr());
        knowledgeBaseEntity.setCreateTime(LocalDateTime.now());
        knowledgeBaseEntity.setUpdateTime(LocalDateTime.now());
        knowledgeBaseRepository.insert(knowledgeBaseEntity);
        return knowledgeBaseEntity;
    }

    @Override
    public KnowledgeBaseEntity update(KnowledgeBaseEntity knowledgeBaseEntity) {
        knowledgeBaseRepository.findById(knowledgeBaseEntity.getId())
                .orElseThrow(() -> new ClientException("知识库不存在"));
        knowledgeBaseEntity.setUpdateTime(LocalDateTime.now());
        knowledgeBaseRepository.update(knowledgeBaseEntity);
        return knowledgeBaseEntity;
    }

    @Override
    public KnowledgeBaseEntity getById(String id) {
        return knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new ClientException("知识库不存在"));
    }

    @Override
    public List<KnowledgeBaseEntity> list(KnowledgeBaseEntity query, Integer pageNum, Integer pageSize) {
        return knowledgeBaseRepository.findList(query, pageNum, pageSize);
    }

    @Override
    public void delete(String id) {
        knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new ClientException("知识库不存在"));
        knowledgeBaseRepository.deleteById(id);
    }
}
