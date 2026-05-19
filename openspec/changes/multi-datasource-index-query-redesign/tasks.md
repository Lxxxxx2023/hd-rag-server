# Tasks: Multi-DataSource Index & Query Redesign

## Schema: spec-driven | Progress: 0/58 tasks

---

## 1. DataSource Domain Model（Index 侧新增）(10 tasks)

### 1.1 核心实体与值对象
- [ ] DS-001 定义 DataSourceType 枚举（URL | FEISHU | YUQUE | DATABASE | FILE）与 SyncStrategy 枚举（ONCE | SCHEDULED | WEBHOOK）
- [ ] DS-002 定义 DataSource 聚合根 — id / kbId / name / type / config / syncStrategy / syncSchedule / status / lastSyncAt
- [ ] DS-003 定义 DataSourceConfig 多态值对象体系 — UrlSourceConfig / FeishuSourceConfig / YuqueSourceConfig / DatabaseSourceConfig（含 TableConfig）/ FileSourceConfig
- [ ] DS-004 定义 DataSourceDocument 值对象 — dataSourceId / externalId / documentId / externalVersion / lastSyncedAt（增量同步映射）
- [ ] DS-005 定义 SchemaIndex 实体 — dataSourceId / tableName / tableComment / columns(ColumnInfo[]) / relationships(TableRelationship[]) / sampleRows / indexedAt

### 1.2 Repository 接口与持久化
- [ ] DS-006 实现 DataSourceRepository 及 MyBatis 持久化 — data_sources 表 CRUD + findByKbId
- [ ] DS-007 实现 DataSourceDocumentRepository — datasource_documents 表 upsert + findByDataSourceId + findByExternalId
- [ ] DS-008 实现 SchemaIndexRepository 及 MyBatis 持久化 — schema_index 表 upsert + findByDataSourceId

### 1.3 数据库表结构
- [ ] DS-009 创建 data_sources / datasource_documents / schema_index 三张新表的 DDL（含索引）
- [ ] DS-010 为 raw_documents 表新增 datasource_id 外键列的迁移脚本

---

## 2. ISourceConnector 策略体系（Index 侧新增）(12 tasks)

### 2.1 接口与注册表
- [ ] SC-001 定义 ISourceConnector 策略接口 — getType() / fetchAll() / fetchUpdated(since) / handleWebhook() / testConnection()
- [ ] SC-002 定义 RawSourceDocument 值对象 — externalId / externalVersion / sourceType / mimeType / rawContent / rawText / metadata
- [ ] SC-003 实现 SourceConnectorRegistry — 按 DataSourceType 路由到对应 Connector，Spring 自动注入 Map<DataSourceType, ISourceConnector>

### 2.2 各类型 Connector 实现
- [ ] SC-004 实现 FileConnector — 读取对象存储文件字节，包装为 RawSourceDocument，交给 IParserStrategy（复用现有 Parser）
- [ ] SC-005 实现 UrlConnector — HttpClient 抓取 + Jsoup 提取正文，按 depth/scope/excludePatterns 控制爬取范围；可选 Playwright JS 渲染
- [ ] SC-006 实现 FeishuConnector — 飞书开放平台 API OAuth 2.0，拉取 space/folder 下所有文档（Block 结构 → RawSourceDocument），支持 fetchUpdated 增量
- [ ] SC-007 实现 YuqueConnector — 语雀 API Personal Token，支持 Markdown 导出，fetchAll + fetchUpdated
- [ ] SC-008 实现 DatabaseConnector — JDBC 连接池，按 TableConfig 查询：textColumns 包装为 RawSourceDocument（每行一个），schema 元数据写入 SchemaIndex 路径

### 2.3 同步流程编排
- [ ] SC-009 实现 DataSourceSyncCase（Case 层） — 编排 Connector.fetchAll/fetchUpdated → 比对 DataSourceDocument 版本 → 触发新增/更新/删除的 Document 处理
- [ ] SC-010 实现定时同步调度器 — 扫描 syncStrategy=SCHEDULED 的 DataSource，按 syncSchedule cron 发送 datasource.sync.requested 事件
- [ ] SC-011 实现 Webhook 接收 Controller — /api/v1/webhooks/feishu/{dsId} 和 /api/v1/webhooks/yuque/{dsId}，验签后调用对应 Connector.handleWebhook()
- [ ] SC-012 实现 SchemaIndexWriter — DatabaseConnector 产出的 Schema 元数据写入 schema_index 表，发送 schema.indexed 事件

---

## 3. 新增 Kafka Topics 与消费者（Index 侧）(4 tasks)

- [ ] KF-001 新增 Kafka Topic 配置 — datasource.sync.requested / datasource.fetched / schema.indexed / datasource.sync.completed
- [ ] KF-002 实现 DatasourceSyncRequestedConsumer — 消费 datasource.sync.requested，调用 DataSourceSyncCase 执行同步
- [ ] KF-003 实现 DatasourceFetchedConsumer — 消费 datasource.fetched，将 RawSourceDocument 路由到 IParserStrategy（衔接现有 doc.imported 处理链）
- [ ] KF-004 实现同步完成事件发布 — DataSourceSyncCase 完成后发送 datasource.sync.completed（含同步统计：新增/更新/删除文档数、耗时）

---

## 4. DataSource Management API（Index 侧）(5 tasks)

- [ ] API-DS-001 实现 DataSourceManagementAppService — DataSource CRUD + 连接测试 + 手动触发同步
- [ ] API-DS-002 实现 DataSource Management API Controller — POST/GET/PUT/DELETE /api/v1/knowledge-bases/{kbId}/datasources
- [ ] API-DS-003 实现连接测试接口 — POST /api/v1/knowledge-bases/{kbId}/datasources/{dsId}/test-connection，调用 Connector.testConnection()
- [ ] API-DS-004 实现手动同步接口 — POST /api/v1/knowledge-bases/{kbId}/datasources/{dsId}/sync（触发全量同步）
- [ ] API-DS-005 实现同步状态查询接口 — GET /api/v1/knowledge-bases/{kbId}/datasources/{dsId}/sync-status（返回 status / lastSyncAt / 统计数据）

---

## 5. SchemaIndex 查询能力（Query 侧新增）(4 tasks)

- [ ] SI-001 定义 ISchemaIndexRepository — Query 域读取 SchemaIndex 的接口（findByDataSourceId / searchByQuery）
- [ ] SI-002 实现 SchemaIndex 语义检索 — 将 query 与 tableComment + columnInfo 拼接后做向量相似度检索，返回 topK 个相关 TableConfig
- [ ] SI-003 实现 schema_retrieve 算子 — 封装 ISchemaIndexRepository.searchByQuery()，产出注入 Text-to-SQL 的 Schema 上下文字符串
- [ ] SI-004 实现 SchemaIndex 的 ES/pgvector 索引写入 — 将 tableComment + column 描述向量化后存入 service_vectors，供语义检索

---

## 6. intent_detect 升级为强制路由器（Query 侧重构）(4 tasks)

- [ ] IR-001 定义 QueryIntent 枚举 — DOCUMENT_SEARCH / DATA_QUERY / HYBRID
- [ ] IR-002 重构 intent_detect 算子 — 从可选预处理步骤升级为强制路由器；根据 query 特征 + Service 数据源类型输出 QueryIntent
- [ ] IR-003 实现 QueryRouter — 读取 intent_detect 输出，分发到 Path1（文档检索）/ Path2（数据查询）/ Path3（混合）执行路径
- [ ] IR-004 实现意图判断规则 — 统计词检测、时间范围+数值检测、Service 无 DATABASE DataSource 则强制 DOCUMENT_SEARCH

---

## 7. Text-to-SQL 路径算子（Query 侧新增）(8 tasks)

- [ ] SQL-001 实现 text_to_sql 算子 — 将 query + Schema 上下文（来自 schema_retrieve）拼装 Prompt，调用 LLM 生成 SQL；maxRetries 次重试（sql_validate 失败触发）
- [ ] SQL-002 实现 sql_validate 算子 — 解析 SQL AST 做语法校验；拒绝 DML/DDL；自动追加 LIMIT maxRows（若无 LIMIT）
- [ ] SQL-003 实现 sql_execute 算子 — 通过 IDatabaseExecutorPort（Infrastructure 适配器）执行 SQL，超时控制 timeoutMs，返回 ResultSet
- [ ] SQL-004 实现 IDatabaseExecutorPort 接口与 JDBC 适配器实现 — 连接池管理，按 dataSourceId 路由到对应 JDBC 连接
- [ ] SQL-005 实现 result_format 算子 — ResultSet → Markdown 表格字符串；超出 maxCells 时截断并附注；产出 DataQueryResult 值对象
- [ ] SQL-006 实现 Text-to-SQL 专用 Prompt 模板 — Schema 上下文注入格式，包含表结构、列注释、样本数据、SQL 方言说明
- [ ] SQL-007 实现 Text-to-SQL 路径的 QueryPipelineCase 分支 — schema_retrieve → text_to_sql → sql_validate → sql_execute → result_format → llm_generate
- [ ] SQL-008 实现 Text-to-SQL 的错误处理 — sql_validate 失败触发 text_to_sql 重试；sql_execute 超时/异常返回明确错误信息给 LLM

---

## 8. 混合路径（Query 侧新增）(4 tasks)

- [ ] HB-001 实现 context_merge 算子 — 合并文档检索 ChunkContext 与数据查询 DataQueryResult，标注来源，按 token 预算裁剪
- [ ] HB-002 实现混合路径并行调度 — Path1 + Path2 并行执行，等待两路结果后进入 context_merge
- [ ] HB-003 实现混合路径的引用溯源 — citation_extract 同时处理文档引用（[ref_N]）和数据来源（"来自 {tableName} 查询"）
- [ ] HB-004 实现 HYBRID 模式的 QueryPipelineCase 分支 — 并行执行 + context_merge + llm_generate + 双溯源后处理

---

## 9. 更新 Pipeline Config 与响应模型（Query 侧）(5 tasks)

- [ ] PC-001 更新 Pipeline Config 数据结构 — 扩展为四段（routing + document_search + data_query + postprocessing），向后兼容旧版仅含 document_search 的配置
- [ ] PC-002 实现 Pipeline Config 的序列化/反序列化 — routing.intent_detect / data_query.text_to_sql 等新字段的 JSONB 映射
- [ ] PC-003 更新 PipelineConfigRepository — 支持新结构的读写，旧配置自动补全 routing.default = "document_search"
- [ ] PC-004 更新 RAGResponse 模型 — 新增 queryType / dataResult 字段；更新 SSE 事件协议（新增 `event: data_result` 事件类型）
- [ ] PC-005 更新 Pipeline Config API Controller — PUT /api/v1/services/{serviceId}/pipeline-config 支持新结构验证

---

## 10. 安全加固（DataSource 敏感信息）(2 tasks)

- [ ] SEC-DS-001 实现 DataSourceConfig 敏感字段加解密 — appSecret / token / password / connectionString 在持久化时 AES 加密，读取时解密；密钥通过 KMS/环境变量注入
- [ ] SEC-DS-002 实现 Webhook 签名验证 — 飞书 Webhook 验证 X-Lark-Signature；语雀 Webhook 验证 HMAC-SHA256 签名

---

**总计：58 tasks**
- DataSource Domain Model: DS-001 ~ DS-010（10 tasks）
- ISourceConnector 策略体系: SC-001 ~ SC-012（12 tasks）
- Kafka Topics & Consumers: KF-001 ~ KF-004（4 tasks）
- DataSource Management API: API-DS-001 ~ API-DS-005（5 tasks）
- SchemaIndex 查询能力: SI-001 ~ SI-004（4 tasks）
- intent_detect 升级: IR-001 ~ IR-004（4 tasks）
- Text-to-SQL 路径算子: SQL-001 ~ SQL-008（8 tasks）
- 混合路径: HB-001 ~ HB-004（4 tasks）
- Pipeline Config 与响应模型: PC-001 ~ PC-005（5 tasks）
- 安全加固: SEC-DS-001 ~ SEC-DS-002（2 tasks）
