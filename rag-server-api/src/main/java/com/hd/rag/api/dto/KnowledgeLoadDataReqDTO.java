package com.hd.rag.api.dto;

import lombok.Data;

/**
 * 知识库加载数据 请求参数
 */
@Data
public class KnowledgeLoadDataReqDTO {
    /**
     * 知识库id
     */
    private String kbId;
    /**
     * 数据源配置id
     */
    private String datasourceConfigId;
}
