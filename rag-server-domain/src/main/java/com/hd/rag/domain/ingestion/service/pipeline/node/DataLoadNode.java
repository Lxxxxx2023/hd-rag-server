package com.hd.rag.domain.ingestion.service.pipeline.node;

import com.alibaba.fastjson2.JSONObject;
import com.hd.rag.domain.ingestion.service.pipeline.PipelineContext;
import com.hd.rag.domain.ingestion.service.pipeline.PipelineNode;
import org.springframework.stereotype.Component;


@Component("pipelineDataLoadNode")
public class DataLoadNode extends PipelineNode {

    private String fileUrl;

    private String dataSource;

    @Override
    protected String getNodeType() {
        return "dataLoadNode";
    }

    @Override
    protected PipelineNode nextNode() {
        return this.nextNode;
    }

    @Override
    public DataLoadNode build(String settingJson) {
        JSONObject setting = JSONObject.parseObject(settingJson);
        String dataSource1 = setting.getString("dataSource");
        return new DataLoadNode();
    }

    @Override
    public PipelineContext process(PipelineContext pipelineContext) {
        return null;
    }
}
