# Spec: Index Domain — DataSource & SchemaIndex

## Overview

定义 DataSource 实体模型、五类 ISourceConnector 策略接口与实现、增量同步机制、SchemaIndex，以及 DataSource Management API。文档解析管道见 document-indexing change。

## 1. DataSource 实体

- **REQ-DS-001**: 定义 DataSourceType 枚举（URL | FEISHU | YUQUE | DATABASE | FILE）与 SyncStrategy 枚举（ONCE | SCHEDULED | WEBHOOK）
- **REQ-DS-002**: 定义 DataSource 聚合根，含 id / kbId / name / type / config（JSONB）/ syncStrategy / syncSchedule / status / lastSyncAt
- **REQ-DS-003**: 定义 DataSourceConfig 多态值对象体系 — UrlSourceConfig / FeishuSourceConfig / YuqueSourceConfig / DatabaseSourceConfig（含 TableConfig）/ FileSourceConfig
- **REQ-DS-004**: TableConfig 支持 textColumns / filterColumns / ignoreColumns / titleTemplate / where / syncColumn
- **REQ-DS-005**: DataSource 支持连接测试（testConnection），验证连通性后状态标记为 CONNECTED

## 2. DataSourceDocument（增量同步映射）

- **REQ-DS-006**: 定义 DataSourceDocument 值对象，记录 externalId → documentId 映射 + externalVersion
- **REQ-DS-007**: externalId 按 DataSourceType 有不同含义：飞书 pageToken / URL / 语雀 docId / 数据库行主键

## 3. ISourceConnector 策略接口

- **REQ-DS-010**: 定义 ISourceConnector 策略接口 — getType() / fetchMetadata(DataSource, cursor) / fetchContent(DataSource, externalId) / handleWebhook(DataSource, payload) / testConnection(config)
- **REQ-DS-011**: Connector 流式拉取：fetchMetadata 返回轻量 DocumentMeta（externalId + version + title），fetchContent 按需拉取单篇内容
- **REQ-DS-012**: 定义 RawSourceDocument 值对象 — Connector 产出物，含 externalId / externalVersion / sourceType / mimeType / rawContent / metadata
- **REQ-DS-013**: Connector 不负责 judge 哪些文档需要处理 — 版本比对由 DataSourceSyncCase 负责

### 各类型 Connector 实现

- **REQ-DS-020**: FileConnector — 读取对象存储文件字节，包装为 RawSourceDocument，交给 IParserStrategy
- **REQ-DS-021**: UrlConnector — HttpClient 抓取 + Jsoup 提取正文，按 depth/scope/excludePatterns 控制爬取范围
- **REQ-DS-022**: FeishuConnector — 飞书开放平台 API OAuth 2.0，分页拉取 space/folder 下所有文档
- **REQ-DS-023**: YuqueConnector — 语雀 API Personal Token，Markdown 导出
- **REQ-DS-024**: DatabaseConnector — JDBC 连接池，按 TableConfig 查询：textColumns → RawSourceDocument + schema → SchemaIndex

## 4. 同步流程

- **REQ-DS-030**: 支持三种同步触发方式：手动触发 / 定时调度（cron）/ Webhook 推送
- **REQ-DS-031**: DataSourceSyncCase 编排同步流程：Connector 拉取 → 比对版本 → 新增/更新/删除 → 触发文档处理
- **REQ-DS-032**: 同步完成后发布 datasource.sync.completed 事件（含统计：新增/更新/删除文档数、耗时）
- **REQ-DS-033**: 同步状态查询 — GET .../{dsId}/sync-status 返回 status / lastSyncAt / 统计数据

## 5. SchemaIndex（DATABASE 类型专属）

- **REQ-SI-001**: 定义 SchemaIndex 实体 — dataSourceId / tableName / tableComment / columns / relationships / sampleRows
- **REQ-SI-002**: SchemaIndex 在 DatabaseConnector 同步时自动写入 schema_index 表
- **REQ-SI-003**: SchemaIndex 的 tableComment + column 描述向量化后存入 pgvector，供 Text-to-SQL 检索

## 6. DataSourceType 与 SourceType 的关系

- **REQ-DS-040**: DataSourceType 描述连接器类型，SourceType 描述单个文档来源。映射：URL→WEB, FEISHU→API, YUQUE→API, DATABASE→DATABASE, FILE→FILE

## 7. DataSource 安全

- **REQ-DS-050**: DataSourceConfig 敏感字段（appSecret / token / password / connectionString）AES-256 加密存储
- **REQ-DS-051**: 飞书 Webhook 验证 X-Lark-Signature；语雀 Webhook 验证 HMAC-SHA256
- **REQ-DS-052**: DataSource API 响应中不返回解密后的敏感字段值

## 8. Kafka Topics

```
业务事件（外部关心）:
  datasource.sync.completed — DataSource 同步结束（含统计）
  schema.indexed            — SchemaIndex 写入完成
```

## 9. Management API

```
DataSource CRUD:
  POST/GET/PUT/DELETE /api/v1/knowledge-bases/{kbId}/datasources

DataSource 操作:
  POST /api/v1/knowledge-bases/{kbId}/datasources/{dsId}/test-connection
  POST /api/v1/knowledge-bases/{kbId}/datasources/{dsId}/sync
  GET  /api/v1/knowledge-bases/{kbId}/datasources/{dsId}/sync-status

Webhook 接收:
  POST /api/v1/webhooks/feishu/{dsId}
  POST /api/v1/webhooks/yuque/{dsId}
```

## 10. Index OperatorType

```
数据源同步:
  datasource_fetch, datasource_sync, schema_index_write
```
