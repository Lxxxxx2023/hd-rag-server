package com.hd.rag.domain.ingestion.service.pipeline;

/**
 * 流水线接口
 */
public interface Pipeline {

    /**
     * 材料加工
     * @param pipelineContext 流水线上下文对象
     * @return 流水线上下文对象
     */
    PipelineContext process(PipelineContext pipelineContext);
}
