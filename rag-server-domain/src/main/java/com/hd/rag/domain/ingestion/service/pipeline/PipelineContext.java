package com.hd.rag.domain.ingestion.service.pipeline;

import com.hd.rag.domain.ingestion.model.entity.KnowledgeDocumentEntity;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class PipelineContext {
    /**
     * 本次流水线执行任务id
     */
    private String taskId;
    /**
     * 流水线id
     */
    private String pipelineId;
    /**
     * 本次任务涉及的文档
     */
    private List<KnowledgeDocumentEntity> documentEntityList;
    /**
     * 各节点处理结果
     */
    private Map<String, Object> results = new HashMap<>();

}
