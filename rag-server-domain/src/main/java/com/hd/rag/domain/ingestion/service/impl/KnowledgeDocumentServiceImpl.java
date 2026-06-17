package com.hd.rag.domain.ingestion.service.impl;

import cn.hutool.core.util.IdUtil;
import com.hd.rag.api.ingestion.dto.request.KnowledgeDocumentUploadReqDTO;
import com.hd.rag.domain.ingestion.adapter.port.IFileStoragePort;
import com.hd.rag.domain.ingestion.adapter.repository.IKnowledgeDocumentRepository;
import com.hd.rag.domain.ingestion.model.entity.KnowledgeDocumentEntity;
import com.hd.rag.domain.ingestion.model.valobj.DataSourceType;
import com.hd.rag.domain.ingestion.model.valobj.DocumentStatus;
import com.hd.rag.domain.ingestion.service.IKnowledgeDocumentService;
import com.hd.rag.types.context.UserContext;
import com.hd.rag.types.context.UserContextHolder;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 知识库文档服务实现
 */
@Service
@AllArgsConstructor
public class KnowledgeDocumentServiceImpl implements IKnowledgeDocumentService {

    private final IFileStoragePort fileStorage;

    private final IKnowledgeDocumentRepository knowledgeDocumentRepository;

    @Override
    public void uploadDocument(KnowledgeDocumentUploadReqDTO reqDTO) throws Exception {
        MultipartFile file = reqDTO.file();
        String storagePath = fileStorage.uploadFile(file, reqDTO.path());

        UserContext user = UserContextHolder.get();
        LocalDateTime now = LocalDateTime.now();
        String originalFilename = file.getOriginalFilename();
        KnowledgeDocumentEntity documentEntity = KnowledgeDocumentEntity.builder()
                .id(IdUtil.getSnowflakeNextIdStr())
                .kbId(reqDTO.kbId())
                .docName(originalFilename)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .fileUrl(storagePath)
                .pipelineId(reqDTO.pipelineId())
                .status(DocumentStatus.UPLOADED)
                .sourceType(DataSourceType.MANUAL_UPLOAD)
                .createBy(user.userId())
                .createTime(now)
                .updateBy(user.userId())
                .updateTime(now)
                .build();

        knowledgeDocumentRepository.insert(documentEntity);
    }
}
