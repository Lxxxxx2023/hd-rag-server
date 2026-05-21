# Tasks: Multi-Source Data Ingestion

## Schema: spec-driven | Progress: 0/31 tasks

---

## 1. DataSource Domain Model (5 tasks)

- [ ] DS-001 定义 DataSourceType 枚举（URL | FEISHU | YUQUE | DATABASE | FILE）与 SyncStrategy 枚举（ONCE | SCHEDULED | WEBHOOK）
- [ ] DS-002 定义 DataSource 聚合根 — id / kbId / name / type / config / syncStrategy / syncSchedule / status / lastSyncAt
- [ ] DS-003 定义 DataSourceConfig 多态值对象体系 — UrlSourceConfig / FeishuSourceConfig / YuqueSourceConfig / DatabaseSourceConfig（含 TableConfig）/ FileSourceConfig
- [ ] DS-004 定义 DataSourceDocument 值对象 — dataSourceId / externalId / documentId / externalVersion（增量同步映射）
- [ ] DS-005 定义 SchemaIndex 实体 — dataSourceId / tableName / tableComment / columns / relationships / sampleRows

## 2. Repository & Persistence (5 tasks)

- [ ] DS-006 实现 DataSourceRepository 及 MyBatis 持久化 — data_sources 表 CRUD
- [ ] DS-007 实现 DataSourceDocumentRepository — datasource_documents 表 upsert + findByDataSourceId
- [ ] DS-008 实现 SchemaIndexRepository 及 MyBatis 持久化 — schema_index 表 upsert
- [ ] DS-009 创建 data_sources / datasource_documents / schema_index 三张新表的 DDL
- [ ] DS-010 为 raw_documents 表新增 datasource_id 外键列的迁移脚本

## 3. ISourceConnector 策略体系 (8 tasks)

- [ ] SC-001 定义 ISourceConnector 策略接口 — getType() / fetchMetadata(DataSource, cursor) / fetchContent(DataSource, externalId) / handleWebhook() / testConnection()
- [ ] SC-002 定义 RawSourceDocument 值对象 — externalId / externalVersion / sourceType / mimeType / rawContent / metadata
- [ ] SC-003 实现 SourceConnectorRegistry — 按 DataSourceType 路由到对应 Connector
- [ ] SC-004 实现 FileConnector — 读取对象存储文件，产出 RawSourceDocument → 复用 IParserStrategy
- [ ] SC-005 实现 UrlConnector — HttpClient 抓取 + Jsoup 提取正文，按 depth/scope 控制爬取范围
- [ ] SC-006 实现 FeishuConnector — 飞书开放平台 API OAuth 2.0，分页拉取 space/folder 下文档
- [ ] SC-007 实现 YuqueConnector — 语雀 API Personal Token，Markdown 导出
- [ ] SC-008 实现 DatabaseConnector — JDBC 连接池，按 TableConfig 分流 textColumns + schema

## 4. 同步流程编排 (5 tasks)

- [ ] SC-009 实现 DataSourceSyncCase（Case 层）— Connector.fetchMetadata → 比对版本 → 触发 Document 处理
- [ ] SC-010 实现定时同步调度器 — 扫描 syncStrategy=SCHEDULED 的 DataSource，按 cron 触发
- [ ] SC-011 实现 Webhook 接收 Controller — /api/v1/webhooks/feishu/{dsId} 和 /api/v1/webhooks/yuque/{dsId}
- [ ] SC-012 实现 SchemaIndexWriter — DatabaseConnector 产出的 Schema 元数据写入 schema_index 表
- [ ] SC-013 实现 DataSourceConfig 敏感字段加解密 — appSecret / token / password / connectionString AES 加密

## 5. Kafka Topics & Consumers (3 tasks)

- [ ] KF-001 新增 Kafka Topic — datasource.sync.requested / datasource.sync.completed / schema.indexed
- [ ] KF-002 实现 DatasourceSyncConsumer — 消费 datasource.sync.requested，调用 DataSourceSyncCase
- [ ] KF-003 实现同步完成事件发布 — DataSourceSyncCase 完成后发送 datasource.sync.completed

## 6. DataSource Management API (5 tasks)

- [ ] API-DS-001 实现 DataSourceManagementAppService — DataSource CRUD + 连接测试 + 手动触发同步
- [ ] API-DS-002 实现 DataSource API Controller — POST/GET/PUT/DELETE /api/v1/knowledge-bases/{kbId}/datasources
- [ ] API-DS-003 实现连接测试接口 — POST .../{dsId}/test-connection
- [ ] API-DS-004 实现手动同步接口 — POST .../{dsId}/sync
- [ ] API-DS-005 实现同步状态查询接口 — GET .../{dsId}/sync-status

---

## Task Summary

| 模块 | 内容 | Tasks |
|------|------|-------|
| Domain Model | DataSource / Config / DataSourceDocument / SchemaIndex | DS-001 ~ DS-005 (5) |
| Repository | MyBatis 持久化 + DDL + 迁移 | DS-006 ~ DS-010 (5) |
| Connectors | ISourceConnector + 5 类实现 | SC-001 ~ SC-008 (8) |
| Sync Flow | DataSourceSyncCase + 定时 + Webhook + SchemaWriter + 加密 | SC-009 ~ SC-013 (5) |
| Kafka | Topics + Consumer + Event | KF-001 ~ KF-003 (3) |
| API | AppService + Controller + test/sync/status | API-DS-001 ~ API-DS-005 (5) |
| **Total** | | **31** |

## Implementation Order

1. DS-001~005 → 领域模型
2. DS-006~010 → 持久化 + DDL
3. SC-001~002 → Connector 接口定义
4. SC-003~008 → 五类 Connector 实现
5. SC-009~013 → 同步编排 + Webhook + 加密
6. KF-001~003 → Kafka 事件
7. API-DS-001~005 → Management API
