package com.hd.rag.trigger.http.ingestion;

import com.hd.rag.api.ingestion.dto.request.KnowledgeDocumentUploadReqDTO;
import com.hd.rag.domain.ingestion.service.IKnowledgeDocumentService;
import com.hd.rag.types.convention.Result;
import com.hd.rag.types.web.Results;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库文档controller
 */
@RestController
@RequestMapping("/document")
public class KnowledgeDocumentController {

    @Resource
    private IKnowledgeDocumentService knowledgeDocumentService;

    @PostMapping("/upload")
    Result<Void> uploadDocument(KnowledgeDocumentUploadReqDTO reqDTO) throws Exception {
        knowledgeDocumentService.uploadDocument(reqDTO);
        return Results.success();
    }
}
