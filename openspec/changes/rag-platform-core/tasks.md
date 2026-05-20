# Tasks: RAG Platform Core

## Schema: spec-driven | Progress: 0/191 tasks

---

## 1. Platform Architecture (16 tasks)

### 1.1 Domain Model
- [ ] PA-001 定义 Service 聚合根与配置值对象
- [ ] PA-002 定义 KnowledgeBase 聚合根与文档管理接口
- [ ] PA-003 定义 ServiceKBGrant 关联实体与授权类型枚举
- [ ] PA-004 实现 KB 删除授权检查领域服务
- [ ] PA-005 实现 Service 删除级联清理领域服务

### 1.2 Repository & Persistence
- [ ] PA-006 实现 ServiceRepository 及 MyBatis 持久化
- [ ] PA-007 实现 KnowledgeBaseRepository 及 MyBatis 持久化
- [ ] PA-008 实现 ServiceKBGrantRepository 及 MyBatis 持久化
- [ ] PA-009 实现授权生效/撤销触发 Index 处理链的领域事件
- [ ] PA-010 创建 raw_documents / service_chunks / service_vectors 表 DDL

### 1.3 Quota & Rate Limit
- [ ] PA-011 实现 Service 配额值对象（QPS / 日请求量 / 存储配额）
- [ ] PA-012 实现配额校验领域服务

### 1.4 Application & API
- [ ] PA-013 实现 ServiceManagementAppService (CRUD)
- [ ] PA-014 实现 KnowledgeBaseManagementAppService (CRUD)
- [ ] PA-015 实现 GrantManagementAppService (授权/撤销)
- [ ] PA-016 实现 Service/KB Management API Controller

---

## 2. Index Domain — Core (31 tasks)

### 2.1 Canonical Document Model
- [ ] DP-001 定义 SourceType 枚举（WEB, FILE, DATABASE, API, CRAWL）与 NodeType 枚举（SECTION, PARAGRAPH, TABLE, LIST, CODE, IMAGE, QUOTE）
- [ ] DP-002 定义 ContentNode 实体 — 树形结构节点，含 type/heading/headingLevel/markdown/plainText/children
- [ ] DP-003 定义 SourcePosition 值对象 — 多源溯源定位（pageNumber, lineStart/End, sourcePointer）
- [ ] DP-004 定义 CanonicalDocument 聚合根 — metadata + ContentTree + statistics
- [ ] DP-005 定义 Document 聚合根与状态机（uploaded → parsing → parsed → chunking → chunked → embedding → ready）

### 2.2 Parser Strategy
- [ ] DP-006 定义 IParserStrategy 策略接口 — canHandle(sourceType, mimeType) + parse(sourceInput) + probe(sourceInput)
- [ ] DP-007 定义 ParserRegistry 策略路由 — 精确 mimeType → SourceType 兜底 → probe 自动检测
- [ ] DP-008 定义文档清洗规则配置值对象 — CleaningRule（正则删除/替换/块类型过滤/位置过滤）

### 2.3 Chunking
- [ ] DP-009 定义 IChunkingStrategy 分块策略接口 — chunk(CanonicalDocument, ChunkConfig) → List<Chunk>
- [ ] DP-010 实现 HeadingChunkStrategy — 按 SECTION 标题层级分块
- [ ] DP-011 实现 FAQChunkStrategy — 按 Q&A 模式检测分块
- [ ] DP-012 实现 FixedSizeChunkStrategy — token 计数超限时分块，段落边界优先
- [ ] DP-013 实现 AutoChunkStrategy — 根据 ContentTree 结构特征自动选择策略
- [ ] DP-014 定义 Chunk 实体 — 保留 structurePath、sourcePositions 列表等元数据

### 2.4 Infrastructure — Parsers
- [ ] DP-015 实现 PdfParser 适配器 — PDFBox 解析 PDF → CanonicalDocument
- [ ] DP-016 实现 HtmlParser 适配器 — Jsoup 解析 HTML → CanonicalDocument
- [ ] DP-017 实现 MarkdownParser 适配器 — flexmark 解析 Markdown → CanonicalDocument
- [ ] DP-018 实现 DocxParser 适配器 — Apache POI 解析 DOCX → CanonicalDocument
- [ ] DP-019 实现 PlainTextParser 适配器 — 纯文本兜底解析
- [ ] DP-020 实现文档清洗服务 — 遍历 ContentTree 标记/删除噪音节点

### 2.5 Infrastructure — Persistence & Embedding
- [ ] DP-021 实现 EmbeddingService — 批量并发 + 失败重试
- [ ] DP-022 实现 DocumentRepository 及 MyBatis 持久化
- [ ] DP-023 实现 ChunkRepository — Service 级 chunk 存储

### 2.6 Infrastructure — Index Storage
- [ ] DP-024 实现 pgvector 向量索引管理 (IVFFlat) 与写入适配器
- [ ] DP-025 实现 Elasticsearch 索引管理 — Chunk.plainText → ES BM25，按 Service 物理隔离
- [ ] DP-026 实现批量索引写入 — pgvector + ES 批量操作，单次最多 100 条
- [ ] DP-027 实现索引写入失败重试 — chunk 标记 failed，自动重试最多 3 次

### 2.7 Processing Pipeline
- [ ] DP-028 实现 IndexProcessingSaga — 编排 import→parse→clean→chunk→embed→index_write
- [ ] DP-029 实现 Kafka 消息驱动处理链（doc.imported → doc.parsed → doc.chunked → doc.embedded → doc.indexed）
- [ ] DP-030 实现文件夹导入 Case（FolderImportCase）— 递归遍历 + probe + 路由解析器
- [ ] DP-031 实现增量更新检测 — 仅重处理受影响的 chunks

---

## 3. DataSource & Connectors (27 tasks)

### 3.1 DataSource Domain Model
- [ ] DS-001 定义 DataSourceType 枚举（URL | FEISHU | YUQUE | DATABASE | FILE）与 SyncStrategy 枚举（ONCE | SCHEDULED | WEBHOOK）
- [ ] DS-002 定义 DataSource 聚合根 — id / kbId / name / type / config / syncStrategy / syncSchedule / status / lastSyncAt
- [ ] DS-003 定义 DataSourceConfig 多态值对象体系 — UrlSourceConfig / FeishuSourceConfig / YuqueSourceConfig / DatabaseSourceConfig（含 TableConfig）/ FileSourceConfig
- [ ] DS-004 定义 DataSourceDocument 值对象 — dataSourceId / externalId / documentId / externalVersion（增量同步映射）
- [ ] DS-005 定义 SchemaIndex 实体 — dataSourceId / tableName / tableComment / columns / relationships / sampleRows

### 3.2 Repository & Persistence
- [ ] DS-006 实现 DataSourceRepository 及 MyBatis 持久化 — data_sources 表 CRUD
- [ ] DS-007 实现 DataSourceDocumentRepository — datasource_documents 表 upsert + findByDataSourceId
- [ ] DS-008 实现 SchemaIndexRepository 及 MyBatis 持久化 — schema_index 表 upsert
- [ ] DS-009 创建 data_sources / datasource_documents / schema_index 三张新表的 DDL
- [ ] DS-010 为 raw_documents 表新增 datasource_id 外键列的迁移脚本

### 3.3 ISourceConnector 策略体系
- [ ] SC-001 定义 ISourceConnector 策略接口 — getType() / fetchAll() / fetchUpdated(since) / handleWebhook() / testConnection()
- [ ] SC-002 定义 RawSourceDocument 值对象 — externalId / externalVersion / sourceType / mimeType / rawContent / metadata
- [ ] SC-003 实现 SourceConnectorRegistry — 按 DataSourceType 路由到对应 Connector
- [ ] SC-004 实现 FileConnector — 读取对象存储文件，复用 IParserStrategy
- [ ] SC-005 实现 UrlConnector — HttpClient 抓取 + Jsoup 提取正文，按 depth/scope 控制爬取范围
- [ ] SC-006 实现 FeishuConnector — 飞书开放平台 API OAuth 2.0，拉取 space/folder 下所有文档
- [ ] SC-007 实现 YuqueConnector — 语雀 API Personal Token，Markdown 导出
- [ ] SC-008 实现 DatabaseConnector — JDBC 连接池，按 TableConfig 分流 textColumns + schema

### 3.4 同步流程编排
- [ ] SC-009 实现 DataSourceSyncCase（Case 层）— Connector.fetchAll/fetchUpdated → 比对版本 → 触发 Document 处理
- [ ] SC-010 实现定时同步调度器 — 扫描 syncStrategy=SCHEDULED 的 DataSource，按 cron 触发
- [ ] SC-011 实现 Webhook 接收 Controller — /api/v1/webhooks/feishu/{dsId} 和 /api/v1/webhooks/yuque/{dsId}
- [ ] SC-012 实现 SchemaIndexWriter — DatabaseConnector 产出的 Schema 元数据写入 schema_index 表

---

## 4. Kafka Topics & Consumers (4 tasks)

- [ ] KF-001 新增 Kafka Topic 配置 — datasource.sync.requested / datasource.fetched / schema.indexed / datasource.sync.completed
- [ ] KF-002 实现 DatasourceSyncRequestedConsumer — 消费 datasource.sync.requested，调用 DataSourceSyncCase
- [ ] KF-003 实现 DatasourceFetchedConsumer — 消费 datasource.fetched，路由 RawSourceDocument 到 IParserStrategy
- [ ] KF-004 实现同步完成事件发布 — DataSourceSyncCase 完成后发送 datasource.sync.completed

---

## 5. DataSource Management API (5 tasks)

- [ ] API-DS-001 实现 DataSourceManagementAppService — DataSource CRUD + 连接测试 + 手动触发同步
- [ ] API-DS-002 实现 DataSource API Controller — POST/GET/PUT/DELETE /api/v1/knowledge-bases/{kbId}/datasources
- [ ] API-DS-003 实现连接测试接口 — POST .../{dsId}/test-connection
- [ ] API-DS-004 实现手动同步接口 — POST .../{dsId}/sync
- [ ] API-DS-005 实现同步状态查询接口 — GET .../{dsId}/sync-status

---

## 6. Query Domain — Core (38 tasks)

> Phase 1 编排方式：Pipeline Config — 固定拓扑 + enable/disable 开关。Phase 2/3 升级为 DAG Engine。

### 6.1 Query Preprocessing
- [ ] QRY-001 实现 query_rewrite 算子 — LLM 改写查询，可生成多个变体
- [ ] QRY-002 实现 term_expand 算子 — 同义词/上下位词扩展
- [ ] QRY-003 实现 hyde_generate 算子 — HyDE 假设文档生成

### 6.2 Retrieval Operators
- [ ] QRY-004 定义 IRetriever 统一检索接口 — retrieve(QueryContext, RetrievalConfig) → List<RetrievedChunk>
- [ ] QRY-005 实现 VectorRetriever — 通过 IVectorRepository 查询 pgvector 语义检索
- [ ] QRY-006 实现 KeywordRetriever — 通过 IChunkRepository 查询 ES BM25 检索
- [ ] QRY-007 实现 HybridRetriever — vector + keyword 并行执行 + fusion
- [ ] QRY-008 实现 metadata_filter 算子 — 按文档元数据过滤

### 6.3 Fusion & Ranking
- [ ] QRY-009 实现 rrf_fusion 算子 — Reciprocal Rank Fusion
- [ ] QRY-010 实现 linear_fusion 算子 — 线性加权融合
- [ ] QRY-011 Service 级别融合策略配置
- [ ] QRY-012 实现 rerank 算子 — Cross-encoder 精排
- [ ] QRY-013 实现 deduplicate 算子 — 内容相似度去重

### 6.4 Context Assembly
- [ ] QRY-014 实现 Token 计数器 — 实时计算 chunk token 数
- [ ] QRY-015 实现 Top-N 截断策略
- [ ] QRY-016 实现上下文压缩策略
- [ ] QRY-017 实现动态预算分配策略
- [ ] QRY-018 实现引用标记自动生成 ([ref_1], [ref_2]...)

### 6.5 Prompt Template System
- [ ] QRY-019 定义模板三层数据模型
- [ ] QRY-020 实现模板变量引擎 — {role}, {context}, {query}, {history} 等
- [ ] QRY-021 实现条件片段渲染
- [ ] QRY-022 实现平台预置模板初始化数据（客服/文档总结/代码审查/翻译）
- [ ] QRY-023 实现 Service 模板继承与覆盖
- [ ] QRY-024 实现 PromptTemplateRepository 及 Redis 缓存

### 6.6 LLM Call
- [ ] QRY-025 实现 LLM Provider 抽象 — 对接外部 API
- [ ] QRY-026 实现多模型路由 — primary / fallback
- [ ] QRY-027 实现重试策略 — fixed / exponential backoff
- [ ] QRY-028 实现 SSE 流式输出适配器

### 6.7 Citation & Post-processing
- [ ] QRY-029 实现引用溯源 — [ref_N] → chunk 原始信息映射
- [ ] QRY-030 实现 LLM 引用与 chunk 自动对齐
- [ ] QRY-031 实现 safety_filter 后处理算子 — PII/敏感词/prompt injection
- [ ] QRY-032 实现 format_convert 后处理算子 — Markdown/JSON/文本

### 6.8 Pipeline Config (Phase 1 编排)
- [ ] QRY-033 实现 PipelineConfigRepository — JSONB 存储统一 4 段结构（routing + document_search + data_query + postprocessing）
- [ ] QRY-034 实现 QueryPipelineCase — 按 Pipeline Config 执行，含三路分叉（DOCUMENT_SEARCH / DATA_QUERY / HYBRID）
- [ ] QRY-035 实现 Pipeline Config API Controller — PUT/PATCH service-pipeline-config

### 6.9 Query Domain Repository Interfaces
- [ ] QRY-036 定义 IChunkSearchRepository — Query 域读取 Chunks 的接口
- [ ] QRY-037 定义 IVectorSearchRepository — Query 域读取 Vectors 的接口
- [ ] QRY-038 定义 ILLMProviderPort — LLM 外部 API 调用接口

---

## 7. Intent Router (4 tasks)

- [ ] IR-001 定义 QueryIntent 枚举 — DOCUMENT_SEARCH / DATA_QUERY / HYBRID
- [ ] IR-002 实现 intent_detect 强制路由器 — 统计词检测、时间范围+数值检测、DATABASE DataSource 存在性判断
- [ ] IR-003 实现 QueryRouter — 读取 intent_detect 输出，分发到 Path1/Path2/Path3
- [ ] IR-004 实现 query_router 和 kb_router — 运行时根据查询特征选择检索策略 + 匹配授权 KB

---

## 8. SchemaIndex & Text-to-SQL (12 tasks)

### 8.1 SchemaIndex 查询
- [ ] SI-001 定义 ISchemaIndexRepository — Query 域读取 SchemaIndex 的接口
- [ ] SI-002 实现 SchemaIndex 语义检索 — query 与 tableComment + columnInfo 向量相似度检索
- [ ] SI-003 实现 schema_retrieve 算子 — 封装 ISchemaIndexRepository，产出 Schema 上下文字符串
- [ ] SI-004 实现 SchemaIndex 的 ES/pgvector 索引写入 — tableComment + column 描述向量化

### 8.2 Text-to-SQL 算子
- [ ] SQL-001 实现 text_to_sql 算子 — query + Schema 上下文 → LLM 生成 SQL，maxRetries 次重试
- [ ] SQL-002 实现 sql_validate 算子 — SQL AST 语法校验；拒绝 DML/DDL；自动追加 LIMIT
- [ ] SQL-003 实现 sql_execute 算子 — 通过 IDatabaseExecutorPort 执行 SQL，超时控制 timeoutMs
- [ ] SQL-004 实现 IDatabaseExecutorPort 接口与 JDBC 适配器 — 连接池管理，按 dataSourceId 路由
- [ ] SQL-005 实现 result_format 算子 — ResultSet → Markdown 表格；超出 maxCells 截断
- [ ] SQL-006 实现 Text-to-SQL 专用 Prompt 模板 — Schema 上下文注入格式
- [ ] SQL-007 实现 Text-to-SQL 路径的 QueryPipelineCase 分支 — schema_retrieve → text_to_sql → sql_validate → sql_execute → result_format → llm_generate
- [ ] SQL-008 实现 Text-to-SQL 错误处理 — sql_validate 失败触发重试；sql_execute 超时/异常返回错误信息

---

## 9. Hybrid Path (4 tasks)

- [ ] HB-001 实现 context_merge 算子 — 合并文档检索 ChunkContext 与数据查询 DataQueryResult，标注来源
- [ ] HB-002 实现混合路径并行调度 — Path1 + Path2 并行执行，等待两路结果后进入 context_merge
- [ ] HB-003 实现混合路径的引用溯源 — citation_extract 同时处理文档引用和数据来源
- [ ] HB-004 实现 HYBRID 模式的 QueryPipelineCase 分支 — 并行执行 + context_merge + llm_generate + 双溯源

---

## 10. Knowledge Graph (30 tasks)

### 10.1 Phase 1 — 架构预留 (8 tasks)
- [ ] KG-001 在 types/ 中创建 Entity 实体数据类（entityId, name, type, aliases, props）
- [ ] KG-002 在 types/ 中创建 Relation 关系数据类（relationId, subject, object, predicate, sourceChunkId, confidence）
- [ ] KG-003 在 types/ 中创建 GraphQuery 查询入参类（serviceId, entityIds, maxHops, relationTypes, maxResults）
- [ ] KG-004 在 types/ 中创建 GraphRetrievalResult 检索结果类（entities, relations, relatedChunkIds, graphScore）
- [ ] KG-005 在 domain/index/ 中定义 IEntityExtractor 策略接口（canHandle + extract）
- [ ] KG-006 在 domain/index/ 中定义 IRelationExtractor 策略接口（extract 方法签名）
- [ ] KG-007 在 domain/query/ 中定义 IGraphExpander / IGraphRetriever / IEntityLinker 策略接口
- [ ] KG-008 创建 infrastructure/graph/ 包，定义 GraphRepository 抽象接口（saveEntities, saveRelations, query, deleteByService, deleteByDocument），Phase 1 无实现

### 10.2 Phase 2 — 结构图谱 (7 tasks)
- [ ] KG-009 实现 ContentTreeTraverser — 基于 structurePath 计算 Chunk 的 PARENT_OF / SIBLING_OF / PREV_OF 关系
- [ ] KG-010 实现 GraphExpander（IGraphExpander 实现）— 给定 Chunk 列表和 expandDepth，沿 ContentTree 关系拉入父/子/兄弟 Chunk
- [ ] KG-011 实现 GraphExpansionCase — 在 Query 流程中，检索结果 → graph_expand → 扩展后的 Chunk 列表
- [ ] KG-012 在 Pipeline Config 的 document_search.kg_expand 中新增 graph_expand 节点（默认 enabled=false）
- [ ] KG-013 编写结构图谱单元测试：验证父子/兄弟关系计算正确
- [ ] KG-014 编写结构图谱集成测试：验证 graph_expand 算子插入 Query Pipeline 后检索结果正确扩展
- [ ] KG-015 验证 graph_expand 对已有查询流程无影响（enabled=false 时路径不变）

### 10.3 Phase 3 — 实体图谱 (10 tasks)
- [ ] KG-016 引入 spring-boot-starter-data-neo4j 依赖，添加 Neo4jConfig 配置类
- [ ] KG-017 实现 Neo4jGraphRepository（GraphRepository 的 Neo4j 实现）— 实体/关系 CRUD + Cypher 遍历
- [ ] KG-018 实现 LLMEntityExtractor（IEntityExtractor 实现）— few-shot prompt + LLM 调用
- [ ] KG-019 实现 LLMRelationExtractor（IRelationExtractor 实现）— 基于已抽取实体 + 上下文抽取关系三元组
- [ ] KG-020 在 service_index_graph DAG 配置中注册 entity_extract / relation_extract / graph_index 算子节点
- [ ] KG-021 实现 EntityLinker（IEntityLinker 实现）— 查询提及文本 → 图数据库实体消歧匹配
- [ ] KG-022 实现 GraphRetriever（IGraphRetriever 实现）— 基于 GraphQuery 执行 Cypher 多跳遍历
- [ ] KG-023 实现 GraphVectorFusion — graph_first / interleave / score_combine 三种融合策略
- [ ] KG-024 在 service_query_graph DAG 配置中注册 entity_link / graph_traverse / graph_vector_fusion 算子节点
- [ ] KG-025 编写实体图谱端到端测试：文档导入 → LLM 抽取 → Neo4j 写入 → 实体链接检索 → 图向量融合

### 10.4 Phase 4 — 全局图谱 (5 tasks)
- [ ] KG-026 实现 LeidenCommunityDetector — 对 Neo4j 中实体-关系图执行 Leiden 社区发现
- [ ] KG-027 实现 CommunitySummarizer — LLM 生成社区摘要
- [ ] KG-028 社区摘要存储到 Neo4j（作为 Community 节点属性）
- [ ] KG-029 实现 GlobalGraphSearcher — 基于社区摘要的全局搜索
- [ ] KG-030 实现 GraphRAGCase — 编排全局检索流程：社区匹配 → 摘要注入 → LLM 生成全局性答案

---

## 11. DAG Execution Engine — Infrastructure 层 (7 tasks) 【Phase 2/3】

> DAG 执行引擎位于 `infrastructure/dag/`，是纯技术组件。不依赖任何 Domain，只依赖 `types/` 中的数据。
> Phase 1 不使用 DAG 引擎。Phase 2/3 引入用于支持自定义拓扑、条件分支和 Agentic RAG。

- [ ] DAG-001 定义 DAG 核心模型 — Graph / Node / Edge / OperatorType
- [ ] DAG-002 实现 DAG 拓扑排序与执行调度器 — 无依赖节点并行，有依赖者串行
- [ ] DAG-003 实现 DAG Schema 校验器 — 上下游算子类型兼容性校验
- [ ] DAG-004 实现节点超时控制与降级策略执行 — skip / error / pass_through
- [ ] DAG-005 定义算子 Schema 基类 — 输入/输出类型声明
- [ ] DAG-006 实现算子注册表 — OperatorRegistry，Index 算子和 Query 算子统一注册
- [ ] DAG-007 实现算子配置参数声明与校验

---

## 12. API & Access (7 tasks)

### 12.1 Runtime API
- [ ] API-001 实现 Runtime API Controller — /search + /rag + SSE
- [ ] API-002 实现 SSE 事件协议 — token / citation / data_result / error / done
- [ ] API-003 实现 Layer 3 参数覆盖逻辑

### 12.2 Pipeline Config & Response
- [ ] API-004 更新 RAGResponse 模型 — 新增 queryType / dataResult 字段
- [ ] API-005 实现 Pipeline Config 序列化/反序列化 — routing.intent_detect / data_query.text_to_sql 等新字段的 JSONB 映射
- [ ] API-006 实现 Pipeline Config API Controller — PUT/PATCH service-pipeline-config 支持新结构验证

### 12.3 SDK
- [ ] API-007 实现 Java SDK — HTTP Client + SSE 解析 + 自动重试

---

## 13. Security (19 tasks)

### 13.1 API Key Management
- [ ] SEC-001 定义 ApiKey 聚合根与 Key 生成策略
- [ ] SEC-002 实现平台级 Key 创建与管理（pk_ 前缀 + RBAC 绑定）
- [ ] SEC-003 实现 Service 级 Key 创建与管理（sk_ 前缀）
- [ ] SEC-004 实现 Key 吊销 / 有效期 / 禁用
- [ ] SEC-005 实现 ApiKeyRepository — bcrypt 哈希存储

### 13.2 Authentication & Authorization
- [ ] SEC-006 实现 API Key 认证过滤器 — pk_ / sk_ 前缀路由
- [ ] SEC-007 实现 Service Key 跨 Service 访问拦截
- [ ] SEC-008 实现 Service-KB 授权运行时校验
- [ ] SEC-009 实现内部调用 Token 认证

### 13.3 RBAC
- [ ] SEC-010 定义 RBAC 角色模型 — admin / operator / viewer
- [ ] SEC-011 实现 RBAC 权限校验切面/拦截器

### 13.4 Audit Log
- [ ] SEC-012 定义审计日志实体 — 操作人/时间/资源/操作/来源IP/结果
- [ ] SEC-013 实现审计日志 AOP 切面 — 自动记录所有 API 操作
- [ ] SEC-014 实现审计日志查询服务 — 按 Service/时间/操作类型
- [ ] SEC-015 实现审计日志按月分表 + 不可删除约束

### 13.5 DataSource 安全
- [ ] SEC-016 实现 DataSourceConfig 敏感字段加解密 — appSecret / token / password / connectionString AES 加密
- [ ] SEC-017 实现 Webhook 签名验证 — 飞书 X-Lark-Signature；语雀 HMAC-SHA256

### 13.6 Network Security
- [ ] SEC-018 实现 IP 白名单过滤器 — Service 级别配置
- [ ] SEC-019 配置强制 HTTPS + gRPC mTLS 支持

---

## 14. Storage & Infrastructure (9 tasks)

### 14.1 Database
- [ ] INF-001 创建 PostgreSQL 核心表结构（services, knowledge_bases, data_sources, schema_index, service_kb_grants, api_keys, chunks, audit_logs）
- [ ] INF-002 配置 JSONB Service/DAG 配置字段
- [ ] INF-003 实现审计日志按月分表 DDL 与自动归档

### 14.2 Vector & Search
- [ ] INF-004 配置 pgvector 扩展 + IVFFlat 索引 + probes 参数
- [ ] INF-005 实现多 embedding 模型维度共存
- [ ] INF-006 预留向量存储抽象接口 — 支持后续 Milvus 切换

### 14.3 Message Queue
- [ ] INF-007 配置 Kafka Topics（doc.imported / doc.parsed / doc.chunked / doc.embedded / doc.indexed / doc.failed / datasource.*）
- [ ] INF-008 实现死信队列 + 3 次重试消费者

### 14.4 Cache
- [ ] INF-009 实现 Redis 缓存层 — Service 配置 / Prompt 模板 / Embedding / API Key / 授权关系

---

## 15. Observability & Evaluation (36 tasks)

### 15.1 Tracing
- [ ] OBS-001 实现 trace_id 生成器 — 每个 RAG 请求唯一 trace
- [ ] OBS-002 实现 DAG Span 拦截器 — 每个算子节点自动记录 span
- [ ] OBS-003 实现 Span 输入输出摘要记录 — 不记录完整 LLM 内容
- [ ] OBS-004 实现 OpenTelemetry 导出器 — 对接 Jaeger / Grafana Tempo
- [ ] OBS-005 trace_id 返回调用方 — 加入 API Response Header

### 15.2 Metrics
- [ ] OBS-006 实现 Platform 级指标收集 — QPS/错误率/P99/活跃Service/Kafka lag
- [ ] OBS-007 实现 Service 级指标收集 — QPS/错误率/P99/日成本/活跃请求
- [ ] OBS-008 实现 Operator 级指标收集 — P50/P99 延迟/token 消耗
- [ ] OBS-009 实现 Prometheus 指标导出

### 15.3 Alerts
- [ ] OBS-010 实现告警规则引擎 — 阈值检测 + 级别分类
- [ ] OBS-011 配置 Service 错误率 > 5% 告警 (P1)
- [ ] OBS-012 配置 P99 > 10s 告警 (P2)
- [ ] OBS-013 配置 LLM API 可用性 < 99% 告警 + 自动切换 fallback (P1)
- [ ] OBS-014 配置 Kafka lag > 1000 告警 + 自动扩展消费者 (P2)
- [ ] OBS-015 配置 Service 日成本异常飙升告警 (P2)

### 15.4 Cost Tracking
- [ ] OBS-016 实现 LLM / Embedding / Rerank 调用成本记录
- [ ] OBS-017 实现成本分摊计算 — 按 Service × Model × 时间
- [ ] OBS-018 实现本地模型 GPU 成本折算
- [ ] OBS-019 实现 Service 日/周/月成本报表 API

### 15.5 Offline Evaluation
- [ ] OBS-020 实现评测数据集 CRUD — question/expected_answer/relevant_chunks/difficulty/category
- [ ] OBS-021 实现评测集来源管理 — 人工标注 / 生产抽样 / LLM 合成
- [ ] OBS-022 实现 RAGAS 四维度评估指标 — Faithfulness, Answer Relevance, Context Relevance, Context Recall
- [ ] OBS-023 实现 Retriever-only 评估模式
- [ ] OBS-024 实现 Full RAG 评估模式
- [ ] OBS-025 实现评估执行引擎 — 选定评测集 + 指定 Service → 指标报告 + 历史对比

### 15.6 Online Sampling
- [ ] OBS-026 实现生产流量比例抽样器 — 默认 5% 可配置
- [ ] OBS-027 实现 LLM-as-Judge 自动评估 — 抽样结果入库
- [ ] OBS-028 实现低分 Case 自动标记待审核
- [ ] OBS-029 采样评估 LLM 调用不计入 Service 成本

### 15.7 User Feedback
- [ ] OBS-030 实现用户反馈上报 API — 点赞/点踩/复制/追问 + trace_id 关联
- [ ] OBS-031 实现反馈仪表板数据聚合 — 按 Service × 时间满意度趋势

### 15.8 Feedback Loop
- [ ] OBS-032 实现低分 Case 自动入库待审核
- [ ] OBS-033 实现根因归类模型 — 文档缺失/分块不当/检索遗漏/LLM 幻觉/其他
- [ ] OBS-034 实现根因 → 可执行改进项关联

### 15.9 Trace Query
- [ ] OBS-035 实现 Trace 查询 API — 按 trace_id / Service / 时间范围
- [ ] OBS-036 实现单 Trace 详情 — DAG 执行图 + 节点详情

---

## Task Summary

| Phase | 内容 | Tasks | 新增依赖 |
|-------|------|-------|---------|
| 1 | Platform Architecture | PA-001 ~ PA-016 (16) | 无 |
| 2 | Index Domain Core | DP-001 ~ DP-031 (31) | PDFBox, Jsoup, flexmark, POI |
| 3 | DataSource & Connectors | DS-001 ~ SC-012 (27) | 飞书/语雀 SDK, JDBC |
| 4 | Kafka & DataSource API | KF-001 ~ API-DS-005 (9) | 无（复用 Kafka） |
| 5 | Query Domain Core | QRY-001 ~ QRY-038 (38) | 无 |
| 6 | Intent Router | IR-001 ~ IR-004 (4) | 无 |
| 7 | SchemaIndex & Text-to-SQL | SI-001 ~ SQL-008 (12) | 无 |
| 8 | Hybrid Path | HB-001 ~ HB-004 (4) | 无 |
| 9 | Knowledge Graph | KG-001 ~ KG-030 (30) | Neo4j (Phase 3) |
| 10 | DAG Engine | DAG-001 ~ DAG-007 (7) | 无（自研） |
| 11 | API & Access | API-001 ~ API-007 (7) | 无 |
| 12 | Security | SEC-001 ~ SEC-019 (19) | 无 |
| 13 | Storage & Infra | INF-001 ~ INF-009 (9) | pgvector, ES, Kafka, Redis |
| 14 | Observability | OBS-001 ~ OBS-036 (36) | OpenTelemetry, Prometheus |
| **Total** | | **191** | |

## Implementation Order

**Phase 1 (Foundation):** INF-001 → PA-001~016 → DP-001~031
**Phase 2 (Multi-Source):** DS-001~010 → SC-001~012 → KF-001~004 → API-DS-001~005
**Phase 3 (Core Query):** QRY-001~038 → IR-001~004（Pipeline Config 编排）
**Phase 4 (Advanced Query):** SI-001~004 → SQL-001~008 → HB-001~004
**Phase 5 (Knowledge Graph):** KG-001~030（分期实施）
**Phase 6 (DAG Engine):** DAG-001~007（替换固定拓扑为 DAG 编排）
**Phase 7 (API & Security):** API-001~007 → SEC-001~019
**Phase 8 (Production):** INF-002~009 → OBS-001~036
