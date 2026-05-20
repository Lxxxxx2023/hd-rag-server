# Spec: Index Domain (写路径)

## Overview

Index 域负责 RAG 系统的**写路径**：数据从外部进入系统，经过数据源接入、解析、清洗、分块、向量化、实体关系抽取，最终写入索引（pgvector + Elasticsearch + 图数据库）。它是数据的拥有者，Query 域通过其 Repository 接口读取数据。

核心职责：
- 多源数据接入与增量同步（URL/飞书/语雀/数据库/文件）
- 统一解析为 CanonicalDocument（ContentTree）
- 文档清洗与噪音过滤
- 按 Service 配置分块
- 向量化并写入索引
- Schema 索引（DATABASE 类型专属）
- 实体/关系抽取与图索引写入
- 索引的生命周期管理（增量更新、级联删除）

---

## 1. DataSource & Multi-Source Ingestion

### DataSource 实体

- **REQ-DS-001**: 定义 DataSourceType 枚举（URL | FEISHU | YUQUE | DATABASE | FILE）与 SyncStrategy 枚举（ONCE | SCHEDULED | WEBHOOK）
- **REQ-DS-002**: 定义 DataSource 聚合根，含 id / kbId / name / type / config（JSONB）/ syncStrategy / syncSchedule / status / lastSyncAt。一个 KB 可拥有多个 DataSource
- **REQ-DS-003**: 定义 DataSourceConfig 多态值对象体系 — UrlSourceConfig（url, depth, scope, renderJs, excludePatterns）/ FeishuSourceConfig / YuqueSourceConfig / DatabaseSourceConfig（含 TableConfig）/ FileSourceConfig
- **REQ-DS-004**: TableConfig 支持 textColumns（语义索引）、filterColumns（元数据过滤）、ignoreColumns、titleTemplate、where 过滤条件、syncColumn（增量列）
- **REQ-DS-005**: DataSource 支持连接测试（testConnection），验证连通性后状态标记为 CONNECTED

### DataSourceDocument（增量同步映射）

- **REQ-DS-006**: 定义 DataSourceDocument 值对象，记录外部 ID → 内部 Document ID 映射与版本号（externalVersion），支持增量同步（仅处理版本变更的文档）
- **REQ-DS-007**: DataSourceDocument 的 externalId 按 DataSourceType 有不同含义：飞书 pageToken / URL / 语雀 docId / 数据库行主键

### ISourceConnector 策略接口

- **REQ-DS-010**: 定义 ISourceConnector 策略接口 — getType() / fetchAll(DataSource) / fetchUpdated(DataSource, since) / handleWebhook(DataSource, payload) / testConnection(config)
- **REQ-DS-011**: 定义 RawSourceDocument 值对象 — Connector 的产出物，含 externalId / externalVersion / sourceType / mimeType / rawContent（原始字节）/ metadata。Connector 只负责拉取原始字节，文本提取和结构化解析由 IParserStrategy 完成
- **REQ-DS-012**: 实现 SourceConnectorRegistry — 按 DataSourceType 路由到对应 Connector，Spring 自动注入

### 各类型 Connector 实现

- **REQ-DS-020**: FileConnector — 读取对象存储文件字节，包装为 RawSourceDocument，交给 IParserStrategy（复用现有 Parser）
- **REQ-DS-021**: UrlConnector — HttpClient 抓取 + Jsoup 提取正文，按 depth/scope/excludePatterns 控制爬取范围；可选 Playwright JS 渲染
- **REQ-DS-022**: FeishuConnector — 飞书开放平台 API OAuth 2.0，拉取 space/folder 下所有文档（Block 结构 → RawSourceDocument），支持 fetchUpdated 增量 + Webhook 实时通知
- **REQ-DS-023**: YuqueConnector — 语雀 API Personal Token，支持 Markdown 导出，fetchAll + fetchUpdated + Webhook
- **REQ-DS-024**: DatabaseConnector — JDBC 连接池，按 TableConfig 查询：textColumns 包装为 RawSourceDocument（每行一个），schema 元数据写入 SchemaIndex 路径

### 同步流程

- **REQ-DS-030**: 支持三种同步触发方式：手动触发（POST /datasources/{dsId}/sync）、定时调度（cron 表达式，syncStrategy=SCHEDULED）、Webhook 推送（飞书/语雀实时通知）
- **REQ-DS-031**: DataSourceSyncCase 编排同步流程：Connector 拉取 → 比对 DataSourceDocument 版本 → 新增/更新/删除的 Document → 触发解析处理链
- **REQ-DS-032**: 同步完成后发布 datasource.sync.completed 事件（含统计：新增/更新/删除文档数、耗时）

### DataSourceType 与 SourceType 的关系

- **REQ-DS-040**: DataSourceType 描述连接器类型（如何获取数据），SourceType 描述单个文档的来源（文档从哪来）。映射关系：URL→WEB, FEISHU→API, YUQUE→API, DATABASE→DATABASE, FILE→FILE
- **REQ-DS-041**: 一个 DataSource 产出的 Document 带有对应的 SourceType 标记，存储在 DocumentMetadata.sourceType 中

---

## 2. SchemaIndex（DATABASE 类型专属）

- **REQ-SI-001**: 定义 SchemaIndex 实体 — dataSourceId / tableName / tableComment / columns（ColumnInfo[]，含 name/type/comment/enumValues/nullable）/ relationships（TableRelationship[]，含 fk/refTable/refColumn）/ sampleRows（3-5行样本数据）/ indexedAt
- **REQ-SI-002**: SchemaIndex 在 DatabaseConnector 同步时自动写入 schema_index 表，发送 schema.indexed 事件
- **REQ-SI-003**: SchemaIndex 的 tableComment + column 描述向量化后存入 pgvector，供 Query 域 schema_retrieve 语义检索

---

## 3. 文档导入与解析

### 文档导入

- **REQ-DP-001**: 支持文件上传导入，首期支持 PDF、Markdown、HTML、DOCX、TXT 格式，后期可扩展到 Excel、JSON、代码文件、图片OCR 等
- **REQ-DP-002**: 支持 API 直接传入文本内容导入（纯文本/JSON/Markdown），以及通过 DataSource 自动拉取
- **REQ-DP-003**: 支持批量导入（一次上传多个文件 / 一个 DataSource 拉取多篇文档）
- **REQ-DP-004**: 支持文件夹上传（.zip 或递归目录），自动识别内部文件类型并分别解析，文件夹路径保留为文档元数据
- **REQ-DP-005**: 文件类型通过 mimeType + 扩展名 + 内容探测（probe）三重检测，不依赖单一判断方式

### 文档解析

- **REQ-DP-010**: 解析器采用策略模式（IParserStrategy），通过 ParserRegistry 根据 SourceType 和 mimeType 路由到对应解析器
- **REQ-DP-011**: 所有解析器产出一致的 CanonicalDocument 格式（ContentTree），包含源元数据和结构化节点树
- **REQ-DP-012**: ContentTree 中每个节点保留溯源定位（sourcePointer），支持 PDF 页码、HTML CSS Selector、JSON Pointer、数据库 row_id 等多种定位方式
- **REQ-DP-013**: ContentTree 中每个节点预渲染 markdown 和 plainText 两份文本：markdown 用于 embedding，plainText 用于 BM25 全文检索
- **REQ-DP-014**: 解析失败需输出明确的错误信息（页码/行号/原因）
- **REQ-DP-015**: 解析器支持 probe() 方法进行文件内容自动检测，用于未识别格式的兜底判断

### 文档清洗

- **REQ-DP-020**: 自动去除文档噪音（页眉页脚/水印/HTML标签/导航栏），噪音标记在 ContentNode 生成阶段完成
- **REQ-DP-021**: 支持按规则配置清洗策略（正则匹配删除/替换模式），清洗规则在 Service 级别配置
- **REQ-DP-022**: 清洗后的 ContentTree 存储于 raw_documents 表，为 KB 级共享

---

## 4. 分块策略

- **REQ-DP-030**: 分块策略操作 ContentTree（统一中间格式），按节点类型和标题层级决策分块边界，TABLE 和 CODE 类型节点保持完整不被拆分
- **REQ-DP-031**: 分块策略按文档结构特征自动适配：标题层级丰富 → 标题分块 / 检测 Q&A 模式 → Q&A 分块 / 否则 → 段落边界 + 固定大小
- **REQ-DP-032**: 支持 Service 级别配置 chunk_size 和 chunk_overlap
- **REQ-DP-033**: 分块结果存储时保留结构元数据（标题路径/页码/位置索引/sourcePointer 列表/structurePath）

---

## 5. 向量化与索引写入

### 向量化

- **REQ-DP-040**: 支持配置 embedding 模型（Service 级别）
- **REQ-DP-041**: 批量 embedding 支持并发 + 失败重试
- **REQ-DP-042**: 增量更新时仅向量化变更的 chunks

### 索引写入

- **REQ-IDX-001**: Chunk 的 markdown 字段拼接后送入 Embedding API，向量写入 pgvector
- **REQ-IDX-002**: Chunk 的 plainText 字段送入 Elasticsearch BM25 索引（按 Service 物理隔离，命名格式 `idx_{serviceId}_chunks`）
- **REQ-IDX-003**: pgvector 和 ES 写入支持批量操作，单次最多 100 条
- **REQ-IDX-004**: 索引写入失败时标记 chunk 状态为 failed，支持自动重试（最多 3 次）

---

## 6. 知识图谱抽取（Index 侧）

### 数据结构

- **REQ-KG-001**: 定义 Entity 纯数据类 — entityId / name / type（PERSON, ORG, PRODUCT, DATE, LOCATION 等）/ aliases / props
- **REQ-KG-002**: 定义 Relation 纯数据类 — relationId / subject / object / predicate / sourceChunkId（溯源）/ confidence（0.0-1.0）

### 抽取策略接口

- **REQ-KG-010**: 定义 IEntityExtractor 策略接口 — canHandle(SourceType) → boolean + extract(CanonicalDocument, ContentNode) → List<Entity>。Phase 1 仅定义接口，Phase 3 提供 LLMEntityExtractor 实现
- **REQ-KG-011**: 定义 IRelationExtractor 策略接口 — extract(List<Entity>, ContentNode) → List<Relation>。Phase 1 仅定义接口，Phase 3 提供 LLMRelationExtractor 实现
- **REQ-KG-012**: LLM 抽取使用 few-shot prompt，支持按 Service 配置 schema 约束（限定 EntityType 和 RelationType），抽取结果持久化到图数据库
- **REQ-KG-013**: 支持增量抽取 — 仅处理新增或变更的 Chunk，抽取结果持久化避免重复

### 图存储

- **REQ-KG-020**: 定义 GraphRepository 抽象接口 — saveEntities(serviceId, entities) / saveRelations(serviceId, relations) / query(GraphQuery) / deleteByService(serviceId) / deleteByDocument(documentId)
- **REQ-KG-021**: Phase 1/2 无实现，Phase 3 提供 Neo4jGraphRepository 实现

### 结构图谱（Phase 2）

- **REQ-KG-030**: 基于 ContentTree 的 Chunk 关系图：PARENT_OF（父子）、SIBLING_OF（兄弟）、PREV_OF / NEXT_OF（前后）。通过 structurePath 计算，零额外存储
- **REQ-KG-031**: ContentTreeTraverser 基于 structurePath 计算 Chunk 间关系，GraphExpander 沿关系拉入上下文 Chunk

---

## 7. 处理状态与增量更新

- **REQ-DP-050**: 文档处理状态机：uploaded → parsing → parsed → chunking → chunked → embedding → ready
- **REQ-DP-051**: 每个状态节点可标记 failed，支持部分重试和断点恢复
- **REQ-DP-052**: 处理进度可查询（已处理 chunks / 总 chunks）
- **REQ-DP-053**: 文档更新时自动检测变更范围，仅重处理受影响的 chunks
- **REQ-DP-054**: 文档删除时级联清理关联的 chunks、vectors 和图数据

---

## 8. Index OperatorType 全集

```
文档处理:
  parse, clean, chunk, embed, index_write

数据源同步:
  datasource_fetch, datasource_sync, schema_index_write

知识图谱:
  entity_extract, relation_extract, graph_index
```
