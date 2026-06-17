package com.hd.rag.domain.ingestion.model.valobj;

import com.hd.rag.domain.ingestion.service.pipeline.PipelineNode;
import com.hd.rag.domain.ingestion.service.pipeline.node.DataLoadNode;
import lombok.Getter;

/**
 * 流水线节点类型
 */
@Getter
public enum PipelineNodeType {
    DATA_LOAD("dataLoad", "数据加载节点") {
        @Override
        public PipelineNode buildNode(String settingJson) {
            DataLoadNode dataLoadNode = new DataLoadNode();
            return dataLoadNode.build(settingJson);
        }
    },

    DATA_CLEAN("dataClean", "数据清洗节点") {
        @Override
        public PipelineNode buildNode(String settingJson) {
            return null;
        }
    },

    CHUNK("chunk", "切块节点") {
        @Override
        public PipelineNode buildNode(String settingJson) {
            return null;
        }
    },

    RERANK("rerank", "重排序节点") {
        @Override
        public PipelineNode buildNode(String settingJson) {
            return null;
        }
    },

    BM25("BM25", "ES节点") {
        @Override
        public PipelineNode buildNode(String settingJson) {
            return null;
        }
    };

    private final String type;
    private final String desc;

    PipelineNodeType(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    /**
     * 根据数值获取枚举
     */
    public static PipelineNodeType fromValue(String type) {
        if (type == null) {
            return null;
        }
        for (PipelineNodeType status : values()) {
            if (status.type.equals(type)) {
                return status;
            }
        }
        return null;
    }

    public abstract PipelineNode buildNode(String settingJson);
}
