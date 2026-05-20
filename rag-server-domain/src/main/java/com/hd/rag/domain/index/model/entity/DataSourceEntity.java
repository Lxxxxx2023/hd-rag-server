package com.hd.rag.domain.index.model.entity;

import com.hd.rag.domain.index.model.valObj.DataSourceConfig;
import com.hd.rag.domain.index.model.valObj.DataSourceStatus;
import com.hd.rag.domain.index.model.valObj.SyncStrategy;
import lombok.Data;

import java.time.Instant;

/**
 * 数据源实体
 */
@Data
public class DataSourceEntity {

    /**
     * 数据源id
     */
    private String id;
    /**
     * 知识库id
     */
    private String kbId;
    /**
     * 数据源名称
     */
    private String name;
    /**
     * 数据源配置
     */
    private DataSourceConfig dataSourceConfig;
    /**
     * 数据源同步策略 ONCE | SCHEDULED | WEBHOOK
     */
    private SyncStrategy syncStrategy;
    /**
     *  cron 表达式，仅 SCHEDULED 有效
     */
    private String syncSchedule;
    /**
     * 数据源状态 CONNECTED | SYNCING | ERROR
     */
    private DataSourceStatus status;
    /**
     * 最后一次获取数据时间
     */
    private Instant lastSyncAt;
    /**
     * 报错信息
     */
    private String errorMessage;
}
