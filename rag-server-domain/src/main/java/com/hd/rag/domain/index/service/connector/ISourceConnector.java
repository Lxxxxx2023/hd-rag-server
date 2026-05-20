package com.hd.rag.domain.index.service.connector;

import com.hd.rag.domain.index.model.entity.DataSourceEntity;
import com.hd.rag.domain.index.model.entity.RawSourceDocumentEntity;
import com.hd.rag.domain.index.model.valObj.ConnectionTestResult;
import com.hd.rag.domain.index.model.valObj.DataSourceType;
import com.hd.rag.domain.index.model.valObj.WebhookPayload;

import java.time.Instant;
import java.util.List;

/**
 * 数据源连接器策略接口。每种数据源类型对应一个实现。
 * 新增数据源只需实现此接口并注册到 SourceConnectorRegistry。
 */
public interface ISourceConnector {

    DataSourceType getType();

    /** 全量拉取：首次同步或手动触发全量重建 */
    List<RawSourceDocumentEntity> fetchAll(DataSourceEntity source);

    /** 增量拉取：只拉取 since 之后变更的文档 */
    List<RawSourceDocumentEntity> fetchUpdated(DataSourceEntity source, Instant since);

    /** 处理 Webhook 推送（飞书/语雀文档更新实时通知） */
    List<RawSourceDocumentEntity> handleWebhook(DataSourceEntity source, WebhookPayload payload);

    /** 连接测试：用户填写配置后验证连通性 */
    ConnectionTestResult testConnection(DataSourceEntity config);

}
