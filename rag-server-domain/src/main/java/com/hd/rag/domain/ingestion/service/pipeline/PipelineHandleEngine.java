package com.hd.rag.domain.ingestion.service.pipeline;

import cn.hutool.core.util.IdUtil;
import com.hd.rag.domain.ingestion.adapter.repository.IKnowledgeDocumentRepository;
import com.hd.rag.domain.ingestion.model.entity.KnowledgeDocumentEntity;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class PipelineHandleEngine {

    private final PipelineFactory pipelineFactory;

    private final IKnowledgeDocumentRepository knowledgeDocumentRepository;

    public void engine(String pipelineId) {
        Pipeline pipeline = pipelineFactory.orchestration(pipelineId);

        List<KnowledgeDocumentEntity> documentByPipelineId = knowledgeDocumentRepository.findUnprocessDocumentByPipelineId(pipelineId);

        PipelineContext pipelineContext = new PipelineContext();
        pipelineContext.setTaskId(IdUtil.getSnowflakeNextIdStr());
        pipelineContext.setPipelineId(pipelineId);
        pipelineContext.setDocumentEntityList(documentByPipelineId);

        pipeline.process(pipelineContext);
    }
}
