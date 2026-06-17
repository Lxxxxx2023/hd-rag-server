package com.hd.rag.domain.ingestion.service.impl;

import cn.hutool.core.util.IdUtil;
import com.hd.rag.api.ingestion.dto.request.KnowledgeBaseCreateReqDTO;
import com.hd.rag.api.ingestion.dto.request.KnowledgeBaseUpdateReqDTO;
import com.hd.rag.api.ingestion.dto.response.KnowledgeBaseRespDTO;
import com.hd.rag.domain.ingestion.adapter.repository.IKnowledgeBaseRepository;
import com.hd.rag.domain.ingestion.model.entity.KnowledgeBaseEntity;
import com.hd.rag.domain.ingestion.service.IKnowledgeBaseService;
import com.hd.rag.types.context.UserContext;
import com.hd.rag.types.context.UserContextHolder;
import com.hd.rag.types.exception.ClientException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库领域服务实现
 */
@Service
public class KnowledgeBaseServiceImpl implements IKnowledgeBaseService {

    @Resource
    private IKnowledgeBaseRepository knowledgeBaseRepository;

    @Override
    public KnowledgeBaseRespDTO create(KnowledgeBaseCreateReqDTO reqDTO) {
        UserContext user = UserContextHolder.get();
        LocalDateTime now = LocalDateTime.now();
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setId(IdUtil.getSnowflakeNextIdStr());
        entity.setName(reqDTO.name());
        entity.setIntro(reqDTO.intro());
        entity.setSearchSet(reqDTO.searchSet());
        entity.setSegmentSet(reqDTO.segmentSet());
        entity.setCreateBy(user.userId());
        entity.setCreateTime(now);
        entity.setUpdateBy(user.userId());
        entity.setUpdateTime(now);
        knowledgeBaseRepository.insert(entity);
        return toRespDTO(entity);
    }

    @Override
    public KnowledgeBaseRespDTO update(KnowledgeBaseUpdateReqDTO reqDTO) {
        KnowledgeBaseEntity entity = knowledgeBaseRepository.findById(reqDTO.id());
        if (entity == null) {
            throw new ClientException("知识库不存在");
        }
        entity.setName(reqDTO.name());
        entity.setIntro(reqDTO.intro());
        entity.setSearchSet(reqDTO.searchSet());
        entity.setUpdateBy(UserContextHolder.get().userId());
        entity.setUpdateTime(LocalDateTime.now());
        knowledgeBaseRepository.update(entity);
        return toRespDTO(entity);
    }

    @Override
    public KnowledgeBaseRespDTO getById(String id) {
        KnowledgeBaseEntity entity = knowledgeBaseRepository.findById(id);
        if (entity == null) {
            throw new ClientException("知识库不存在");
        }
        return toRespDTO(entity);
    }

    @Override
    public List<KnowledgeBaseRespDTO> list(Integer pageNum, Integer pageSize) {
        return knowledgeBaseRepository.findList(new KnowledgeBaseEntity(), pageNum, pageSize).stream()
                .map(this::toRespDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String id) {
        if (knowledgeBaseRepository.findById(id) == null) {
            throw new ClientException("知识库不存在");
        }
        knowledgeBaseRepository.deleteById(id);
    }

    // ==================== Entity → DTO ====================

    private KnowledgeBaseRespDTO toRespDTO(KnowledgeBaseEntity entity) {
        return new KnowledgeBaseRespDTO(
                entity.getId(),
                entity.getName(),
                entity.getIntro(),
                entity.getSearchSet(),
                entity.getSegmentSet(),
                entity.getCreateBy(),
                entity.getCreateTime(),
                entity.getUpdateBy(),
                entity.getUpdateTime());
    }
}
