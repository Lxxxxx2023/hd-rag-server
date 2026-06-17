package com.hd.rag.domain.ingestion.service.pipeline.node;

import cn.hutool.core.collection.CollectionUtil;
import com.hd.rag.domain.ingestion.service.pipeline.PipelineContext;
import com.hd.rag.domain.ingestion.service.pipeline.PipelineNode;
import com.hd.rag.types.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("pipelineRootNode")
public class RootNode extends PipelineNode {
    @Override
    protected String getNodeType() {
        return "rootNode";
    }

    @Override
    protected PipelineNode nextNode() {
        return this.nextNode;
    }

    @Override
    public PipelineNode build(String settingJson) {
        return new RootNode();
    }

    @Override
    public PipelineContext process(PipelineContext pipelineContext) {
        log.info("流水线处理头节点 taskId:{}, pipelineId: {}", pipelineContext.getTaskId(), pipelineContext.getPipelineId());
        if(CollectionUtil.isEmpty(pipelineContext.getDocumentEntityList())) {
            throw new ServiceException("本次处理文档不能为空");
        }
        return nextNode.process(pipelineContext);
    }
}
