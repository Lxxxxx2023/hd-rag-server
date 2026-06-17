package com.hd.rag.domain.ingestion.service.pipeline;

import cn.hutool.core.util.StrUtil;
import com.hd.rag.domain.ingestion.adapter.repository.IPipelineRepository;
import com.hd.rag.domain.ingestion.model.entity.PipelineEntity;
import com.hd.rag.domain.ingestion.model.entity.PipelineNodeEntity;
import com.hd.rag.domain.ingestion.model.valobj.PipelineNodeType;
import com.hd.rag.domain.ingestion.service.pipeline.node.RootNode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 流水线工厂
 * 负责根据知识库配置对流水线进行组装
 */
@Component
@AllArgsConstructor
public class PipelineFactory {

    private final IPipelineRepository pipelineRepository;

    /**
     * 根据流水线id，编排流水线
     * @param pipelineId 流水线
     * @return 流水线实体
     */
    public Pipeline orchestration(String pipelineId) {
        PipelineEntity pipelineEntity = pipelineRepository.findById(pipelineId);

        List<PipelineNodeEntity> nodes = pipelineEntity.getNodes();
        Map<String, PipelineNodeEntity> pipelineNodeEntityMap = nodes.stream().collect(Collectors.toMap(
                PipelineNodeEntity::getId,
                Function.identity(),
                (exist, replace) -> exist)
        );

        PipelineNodeEntity currentNodeEntity = pipelineEntity.getRootNode();
        RootNode rootNode = new RootNode();
        String nextNodeId = currentNodeEntity.getNextNodeId();
        PipelineNode currentNode = rootNode;
        while((StrUtil.isNotEmpty(nextNodeId) && Objects.nonNull(pipelineNodeEntityMap.get(nextNodeId)))) {
            PipelineNodeEntity pipelineNodeEntity = pipelineNodeEntityMap.get(nextNodeId);
            String settingsJson = pipelineNodeEntity.getSettingsJson();
            PipelineNodeType pipelineNodeType = PipelineNodeType.fromValue(pipelineNodeEntity.getNodeType());
            PipelineNode pipelineNode = pipelineNodeType.buildNode(settingsJson);
            currentNode.setNextNode(pipelineNode);
            currentNode = pipelineNode;
            nextNodeId = pipelineNodeEntity.getNextNodeId();
        }

        return rootNode;
    }
}
