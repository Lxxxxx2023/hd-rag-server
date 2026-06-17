package com.hd.rag.domain.ingestion.service.pipeline;

import lombok.Data;

/**
 * 流水线节点
 */
@Data
public abstract class PipelineNode implements Pipeline {

    protected PipelineNode nextNode;

    protected abstract String getNodeType();

    protected abstract PipelineNode nextNode();

    public abstract PipelineNode build(String settingJson);

}
