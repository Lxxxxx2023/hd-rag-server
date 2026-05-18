# Tasks: RAG Platform Core

## Schema: spec-driven | Progress: 0/97 tasks

---

## 1. Platform Architecture (16 tasks)

### 1.1 Domain Model
- [ ] PA-001 定义 Service 聚合根与配置值对象 (REQ-PA-001, REQ-PA-003)
- [ ] PA-002 定义 KnowledgeBase 聚合根与文档管理接口 (REQ-PA-005, REQ-PA-006)
- [ ] PA-003 定义 ServiceKBGrant 关联实体与授权类型枚举 (REQ-PA-008, REQ-PA-009)
- [ ] PA-004 实现 KB 删除授权检查领域服务 (REQ-PA-007)
- [ ] PA-005 实现 Service 删除级联清理领域服务 (REQ-PA-004)

### 1.2 Repository & Persistence
- [ ] PA-006 实现 ServiceRepository 及 MyBatis 持久化 (REQ-PA-001, REQ-PA-002)
- [ ] PA-007 实现 KnowledgeBaseRepository 及 MyBatis 持久化 (REQ-PA-005)
- [ ] PA-008 实现 ServiceKBGrantRepository 及 MyBatis 持久化 (REQ-PA-008, REQ-PA-010)
- [ ] PA-009 实现授权生效/撤销触发 Index 处理链的领域事件 (REQ-PA-011, REQ-PA-012)
- [ ] PA-010 创建 raw_documents / service_chunks / service_vectors 表结构 (REQ-PA-013, REQ-PA-014)

### 1.3 Quota & Rate Limit
- [ ] PA-011 实现 Service 配额值对象（QPS / 日请求量 / 存储配额） (REQ-PA-015, REQ-PA-016)
- [ ] PA-012 实现配额校验领域服务 — 超配额拒绝写入 (REQ-INF-025, REQ-INF-026)

### 1.4 Application & API
- [ ] PA-013 实现 ServiceManagementAppService (CRUD) (REQ-API-003)
- [ ] PA-014 实现 KnowledgeBaseManagementAppService (CRUD) (REQ-API-004)
- [ ] PA-015 实现 GrantManagementAppService (授权/撤销) (REQ-API-006)
- [ ] PA-016 实现 Service → Management API Controller (REQ-API-003 ~ REQ-API-006)

---

## 2. Index Domain — 写路径 (31 tasks)

> Index 域拥有数据（Documents, Chunks, Vectors），Query 域通过其 Repository 接口读取。

### 2.1 Domain Model — Canonical Document
- [ ] DP-001 定义 SourceType 枚举值对象（WEB, FILE, DATABASE, API, CRAWL）与 NodeType 枚举（SECTION, PARAGRAPH, TABLE, LIST, CODE, IMAGE, QUOTE）
- [ ] DP-002 定义 ContentNode 实体 — 树形结构节点，含 type/heading/headingLevel/markdown/plainText/children (REQ-DP-006, REQ-DP-007)
- [ ] DP-003 定义 SourcePosition 值对象 — 多源溯源定位（pageNumber, lineStart/End, sourcePointer）(REQ-DP-006)
- [ ] DP-004 定义 CanonicalDocument 聚合根 — metadata + ContentTree + statistics (REQ-DP-005)
- [ ] DP-005 定义 Document 聚合根与状态机（uploaded → parsing → parsed → chunking → chunked → embedding → ready）(REQ-DP-022, REQ-DP-023)

### 2.2 Domain Model — Parser Strategy
- [ ] DP-006 定义 IParserStrategy 策略接口 — canHandle(sourceType, mimeType) + parse(sourceInput) + probe(sourceInput) (REQ-DP-004, REQ-DP-009)
- [ ] DP-007 定义 ParserRegistry 策略路由 — register + resolve（精确 mimeType → SourceType 兜底 → probe 自动检测）(REQ-DP-004)
- [ ] DP-008 定义文档清洗规则配置值对象 — CleaningRule（正则删除/替换/块类型过滤/位置过滤）(REQ-DP-010, REQ-DP-011)

### 2.3 Domain Model — Chunking
- [ ] DP-009 定义 IChunkingStrategy 分块策略接口 — chunk(CanonicalDocument, ChunkConfig) → List<Chunk>，操作 ContentTree (REQ-DP-013)
- [ ] DP-010 实现 HeadingChunkStrategy — 按 SECTION 标题层级分块 (REQ-DP-014)
- [ ] DP-011 实现 FAQChunkStrategy — 按 Q&A 模式检测分块 (REQ-DP-014)
- [ ] DP-012 实现 FixedSizeChunkStrategy — token 计数超限时分块，段落边界优先 (REQ-DP-014)
- [ ] DP-013 实现 AutoChunkStrategy — 根据 ContentTree 结构特征自动选择上述策略 (REQ-DP-014)
- [ ] DP-014 定义 Chunk 实体 — 保留 structurePath、sourcePositions 列表等元数据 (REQ-DP-016)

### 2.4 Infrastructure — Parsers
- [ ] DP-015 实现 PdfParser 适配器 — PDFBox 解析 PDF → CanonicalDocument，提取页码/段落/标题/表格 (REQ-DP-001)
- [ ] DP-016 实现 HtmlParser 适配器 — Jsoup 解析 HTML → CanonicalDocument，<h1>-<h6> → SECTION，标记 <nav>/<footer> 为噪音 (REQ-DP-001)
- [ ] DP-017 实现 MarkdownParser 适配器 — flexmark 解析 Markdown → CanonicalDocument，## → SECTION，``` → CODE (REQ-DP-001)
- [ ] DP-018 实现 DocxParser 适配器 — Apache POI 解析 DOCX → CanonicalDocument，样式 Heading → SECTION (REQ-DP-001)
- [ ] DP-019 实现 PlainTextParser 适配器 — 纯文本兜底解析，按段落/空行分块 (REQ-DP-001)
- [ ] DP-020 实现文档清洗服务 — 遍历 ContentTree 标记/删除噪音节点（页眉页脚/水印/HTML导航）(REQ-DP-010, REQ-DP-011)

### 2.5 Infrastructure — Persistence & Embedding
- [ ] DP-021 实现 EmbeddingService — 批量并发 + 失败重试（Chunk.markdown 送入 Embedding API）(REQ-DP-017, REQ-DP-018)
- [ ] DP-022 实现 DocumentRepository 及 MyBatis 持久化 — raw_documents 表存储 CanonicalDocument (REQ-DP-012)
- [ ] DP-023 实现 ChunkRepository — Service 级 chunk 存储，含 structurePath + sourcePositions JSON 字段 (REQ-PA-014)

### 2.6 Infrastructure — Index Storage
- [ ] DP-028 实现 pgvector 向量索引管理 (IVFFlat) 与写入适配器 — Chunk.markdown → Embedding → pgvector (REQ-IDX-001, REQ-INF-005, REQ-INF-006)
- [ ] DP-029 实现 Elasticsearch 索引管理 — Chunk.plainText → ES BM25 索引，按 Service 物理隔离 (REQ-IDX-002, REQ-INF-011)
- [ ] DP-030 实现批量索引写入 — pgvector + ES 批量操作，单次最多 100 条 (REQ-IDX-003)
- [ ] DP-031 实现索引写入失败重试 — chunk 标记 failed，自动重试最多 3 次 (REQ-IDX-004)

### 2.7 Processing Pipeline
- [ ] DP-024 实现 IndexProcessingSaga — 编排 import→parse→clean→chunk→embed→index_write (REQ-DP-022)
- [ ] DP-025 实现 Kafka 消息驱动处理链（doc.imported → doc.parsed → doc.chunked → doc.embedded → doc.indexed）(REQ-INF-016 ~ REQ-INF-019)
- [ ] DP-026 实现文件夹导入 Case（FolderImportCase）— 递归遍历 + probe 类型检测 + 路由解析器 + 批量创建 Document (REQ-DP-020)
- [ ] DP-027 实现增量更新检测 — 仅重处理受影响的 chunks (REQ-DP-025, REQ-DP-026)

---

## 3. Query Domain — 读路径 (46 tasks)

> Query 域不拥有数据。通过 Index 域的 Repository 接口（IChunkRepository, IVectorRepository）读取 Chunks 和 Vectors。
>
> **Phase 1 编排方式：Pipeline Config** — 固定执行拓扑（rewrite → search → rerank → assemble → generate），各阶段可独立 enable/disable + 参数配置。由 `QueryPipelineCase` 顺序执行，无需 DAG 引擎。
> **Phase 2/3 升级为 DAG Engine** — 支持自定义拓扑、条件分支、Agentic RAG。DAG 任务见 Section 4。

### 3.1 Query Preprocessing Operators
- [ ] QRY-001 实现 query_rewrite 算子 — LLM 改写查询，可生成多个变体 (REQ-QRY-001)
- [ ] QRY-002 实现 intent_detect 算子 — 识别用户意图类别 (REQ-QRY-002)
- [ ] QRY-003 实现 term_expand 算子 — 同义词/上下位词扩展 (REQ-QRY-003)
- [ ] QRY-004 实现 hyde_generate 算子 — HyDE 假设文档生成 (REQ-QRY-004)
- [ ] QRY-004a 实现 query_router 算子 — 根据查询特征自动选择检索策略（事实→keyword / 概念→vector / 综合→hybrid）(REQ-QRY-004-R1)
- [ ] QRY-004b 实现 kb_router 算子 — 意图→授权 KB 匹配，支持单/多/全 KB 三种模式 (REQ-QRY-004-R2)

### 3.2 Retrieval Operators
- [ ] QRY-005 实现 vector_search 算子 — 通过 IVectorRepository 查询 pgvector 语义检索 (REQ-QRY-005)
- [ ] QRY-006 实现 keyword_search 算子 — 通过 IChunkRepository 查询 ES BM25 检索 (REQ-QRY-006)
- [ ] QRY-007 实现 metadata_filter 算子 — 通过 IChunkRepository 按文档元数据过滤 (REQ-QRY-007)
- [ ] QRY-008 实现混合检索并行调度 — vector + keyword 并行执行 (REQ-QRY-008)
- [ ] QRY-008a 定义 IRetriever 统一检索接口 — retrieve(QueryContext, RetrievalConfig) → List<RetrievedChunk>，VectorRetriever / KeywordRetriever / HybridRetriever 实现 (REQ-QRY-005 ~ REQ-QRY-008)

### 3.3 Fusion & Ranking Operators
- [ ] QRY-009 实现 rrf_fusion 算子 — Reciprocal Rank Fusion (REQ-QRY-009)
- [ ] QRY-010 实现 linear_fusion 算子 — 线性加权融合 (REQ-QRY-010)
- [ ] QRY-011 Service 级别融合策略配置 (REQ-QRY-011)
- [ ] QRY-012 实现 rerank 算子 — Cross-encoder 精排，支持自部署/外部 API (REQ-QRY-012)
- [ ] QRY-013 实现 deduplicate 算子 — 内容相似度去重 (REQ-QRY-013)

### 3.4 Context Assembly
- [ ] QRY-014 实现 Token 计数器 — 实时计算 chunk token 数 (REQ-QRY-014)
- [ ] QRY-015 实现 Top-N 截断策略 (REQ-QRY-015)
- [ ] QRY-016 实现上下文压缩策略 (REQ-QRY-015)
- [ ] QRY-017 实现动态预算分配策略 (REQ-QRY-015)
- [ ] QRY-018 实现引用标记自动生成 ([ref_1], [ref_2]...) (REQ-QRY-016)

### 3.5 Prompt Template System
- [ ] QRY-019 定义模板三层数据模型 (REQ-QRY-017)
- [ ] QRY-020 实现模板变量引擎 — 解析 {role}, {context}, {query}, {history} 等 (REQ-QRY-018)
- [ ] QRY-021 实现条件片段渲染 (REQ-QRY-019)
- [ ] QRY-022 实现平台预置模板初始化数据 (客服/文档总结/代码审查/翻译) (REQ-QRY-017 Layer 1)
- [ ] QRY-023 实现 Service 模板继承与覆盖 (REQ-QRY-017 Layer 2)
- [ ] QRY-024 实现 PromptTemplateRepository 及 Redis 缓存 (REQ-INF-021)

### 3.6 LLM Call
- [ ] QRY-025 实现 LLM Provider 抽象 — 对接外部 API (REQ-QRY-020)
- [ ] QRY-026 实现多模型路由 — primary / fallback (REQ-QRY-020, REQ-QRY-022)
- [ ] QRY-027 实现重试策略 — fixed / exponential backoff (REQ-QRY-023)
- [ ] QRY-028 实现 SSE 流式输出适配器 (REQ-QRY-021)

### 3.7 Citation & Post-processing
- [ ] QRY-029 实现引用溯源 — [ref_N] → chunk 原始信息映射 (REQ-QRY-025, REQ-QRY-026)
- [ ] QRY-030 实现 LLM 引用与 chunk 自动对齐 (REQ-QRY-027)
- [ ] QRY-031 实现 safety_filter 后处理算子 — PII/敏感词/prompt injection (REQ-QRY-028)
- [ ] QRY-032 实现 format_convert 后处理算子 — Markdown/JSON/文本 (REQ-QRY-029)

### 3.8 Application — Pipeline Config (Phase 1)
- [ ] QRY-033 实现 PipelineConfigRepository — JSONB 存储 Service 级 Pipeline 配置（各阶段 enable/disable + 参数）(REQ-INF-002)
- [ ] QRY-034 实现 QueryPipelineCase — 按 Pipeline Config 顺序执行固定拓扑（rewrite→search→rerank→assemble→generate），无 DAG 依赖 (REQ-API-009, REQ-API-010)
- [ ] QRY-035 实现 Pipeline Config API Controller — PUT/PATCH service-pipeline-config (REQ-API-007)

### 3.9 Query Domain Repository Interfaces (定义在 domain/query/adapter/)
- [ ] QRY-036 定义 IChunkSearchRepository — Query 域读取 Chunks 的接口（keyword search, metadata filter, findById）
- [ ] QRY-037 定义 IVectorSearchRepository — Query 域读取 Vectors 的接口（semantic search, batch query）
- [ ] QRY-038 定义 ILLMProviderPort — LLM 外部 API 调用接口

---

## 4. DAG Execution Engine — Infrastructure 层 (7 tasks) 【Phase 2/3】

> DAG 执行引擎位于 `infrastructure/dag/`，是纯技术组件。它不依赖任何 Domain 层，只依赖 `types/` 中的数据结构（GraphDefinition, NodeConfig 等）。算子策略接口由 Index/Query Domain 层定义，Case 层负责桥接。
>
> **注意：Phase 1 不使用 DAG 引擎。** Index 路径使用 Kafka event chain，Query 路径使用 Pipeline Config（Section 3.8）。DAG 引擎在 Phase 2/3 引入，用于支持自定义拓扑、条件分支和 Agentic RAG。

### 4.1 DAG Core Model
- [ ] DAG-001 定义 DAG 核心模型 — Graph / Node / Edge / OperatorType (REQ-RP-001)
- [ ] DAG-002 实现 DAG 拓扑排序与执行调度器 — 无依赖节点并行，有依赖者串行 (REQ-RP-003)
- [ ] DAG-003 实现 DAG Schema 校验器 — 上下游算子类型兼容性校验 (REQ-RP-002, REQ-RP-006)
- [ ] DAG-004 实现节点超时控制与降级策略执行 — skip / error / pass_through (REQ-QRY-030 ~ REQ-QRY-032)

### 4.2 Operator Registry
- [ ] DAG-005 定义算子 Schema 基类 — 输入/输出类型声明 (REQ-RP-005)
- [ ] DAG-006 实现算子注册表 — OperatorRegistry，支持 Index 算子和 Query 算子统一注册 (REQ-RP-007)
- [ ] DAG-007 实现算子配置参数声明与校验 (REQ-RP-007)

---

## 5. API & Access (7 tasks)

### 5.1 Runtime API
- [ ] API-001 实现 Runtime API Controller — /search + /rag + SSE (REQ-API-009, REQ-API-010)
- [ ] API-002 实现 SSE 事件协议 — token / citation / error / done (REQ-API-011)
- [ ] API-003 实现 Layer 3 参数覆盖逻辑 (REQ-API-010)

### 5.2 SDK
- [ ] API-004 实现 Java SDK — HTTP Client + SSE 解析 (REQ-API-016)
- [ ] API-005 实现 SDK 自动重试 / 超时 / 连接池 (REQ-API-017)

### 5.3 gRPC Internal
- [ ] API-006 定义 Internal gRPC proto — 模块间调用接口 (REQ-API-001)
- [ ] API-007 实现 gRPC Server 与 Client (REQ-API-001)

---

## 6. Security (17 tasks)

### 6.1 API Key Management
- [ ] SEC-001 定义 ApiKey 聚合根与 Key 生成策略 (REQ-SEC-004, REQ-SEC-005)
- [ ] SEC-002 实现平台级 Key 创建与管理 (pk_ 前缀 + RBAC 绑定) (REQ-SEC-001, REQ-SEC-008)
- [ ] SEC-003 实现 Service 级 Key 创建与管理 (sk_ 前缀) (REQ-SEC-002, REQ-SEC-007)
- [ ] SEC-004 实现 Key 吊销 / 有效期 / 禁用 (REQ-SEC-006)
- [ ] SEC-005 实现 ApiKeyRepository — bcrypt 哈希存储 (REQ-SEC-004)

### 6.2 Authentication & Authorization
- [ ] SEC-006 实现 API Key 认证过滤器 — pk_ / sk_ 前缀路由 (REQ-API-012, REQ-API-013)
- [ ] SEC-007 实现 Service Key 跨 Service 访问拦截 (REQ-SEC-011)
- [ ] SEC-008 实现 Service-KB 授权运行时校验 (REQ-SEC-012, REQ-SEC-013)
- [ ] SEC-009 实现内部调用 Token 认证 (REQ-API-014, REQ-SEC-003)

### 6.3 RBAC
- [ ] SEC-010 定义 RBAC 角色模型 — admin / operator / viewer (REQ-SEC-008 ~ REQ-SEC-010)
- [ ] SEC-011 实现 RBAC 权限校验切面/拦截器 (REQ-SEC-008 ~ REQ-SEC-010)

### 6.4 Audit Log
- [ ] SEC-012 定义审计日志实体 — 操作人/时间/资源/操作/来源IP/结果 (REQ-SEC-015)
- [ ] SEC-013 实现审计日志 AOP 切面 — 自动记录所有 API 操作 (REQ-SEC-015)
- [ ] SEC-014 实现审计日志查询服务 — 按 Service/时间/操作类型 (REQ-SEC-017)
- [ ] SEC-015 实现审计日志按月分表 + 不可删除约束 (REQ-INF-003, REQ-SEC-016)

### 6.5 Network Security
- [ ] SEC-016 实现 IP 白名单过滤器 — Service 级别配置 (REQ-SEC-018)
- [ ] SEC-017 配置强制 HTTPS + gRPC mTLS 支持 (REQ-SEC-019, REQ-SEC-020)

---

## 7. Storage & Infrastructure (9 tasks)

### 7.1 Database
- [ ] INF-001 创建 PostgreSQL 核心表结构 (services, knowledge_bases, documents, service_kb_grants, api_keys, chunks, audit_logs) (REQ-INF-001)
- [ ] INF-002 配置 JSONB Service/DAG 配置字段 (REQ-INF-002)
- [ ] INF-003 实现审计日志按月分表 DDL 与自动归档 (REQ-INF-003)

### 7.2 Vector & Search
- [ ] INF-004 配置 pgvector 扩展 + IVFFlat 索引 + probes 参数 (REQ-INF-004 ~ REQ-INF-006)
- [ ] INF-005 实现多 embedding 模型维度共存 (REQ-INF-005, REQ-INF-007)
- [ ] INF-006 预留向量存储抽象接口 — 支持后续 Milvus 切换 (REQ-INF-009)

### 7.3 Message Queue
- [ ] INF-007 配置 Kafka Topics (doc.imported / doc.parsed / doc.chunked / doc.embedded / doc.indexed / doc.failed) (REQ-INF-017)
- [ ] INF-008 实现死信队列 + 3 次重试消费者 (REQ-INF-019)

### 7.4 Cache
- [ ] INF-009 实现 Redis 缓存层 — Service 配置 / Prompt 模板 / Embedding / API Key / 授权关系 (REQ-INF-020 ~ REQ-INF-024)

---

## 8. Observability & Evaluation (36 tasks)

### 8.1 Tracing
- [ ] OBS-001 实现 trace_id 生成器 — 每个 RAG 请求唯一 trace (REQ-OBS-001)
- [ ] OBS-002 实现 DAG Span 拦截器 — 每个算子节点自动记录 span (REQ-OBS-002, REQ-OBS-003)
- [ ] OBS-003 实现 Span 输入输出摘要记录 — 不记录完整 LLM 内容 (REQ-OBS-004)
- [ ] OBS-004 实现 OpenTelemetry 导出器 — 对接 Jaeger / Grafana Tempo (REQ-OBS-005)
- [ ] OBS-005 trace_id 返回调用方 — 加入 API Response Header (REQ-OBS-006)

### 8.2 Metrics
- [ ] OBS-006 实现 Platform 级指标收集 — QPS/错误率/P99/活跃Service/Kafka lag (REQ-OBS-007)
- [ ] OBS-007 实现 Service 级指标收集 — QPS/错误率/P99/日成本/活跃请求 (REQ-OBS-008)
- [ ] OBS-008 实现 Operator 级指标收集 — P50/P99 延迟/token 消耗 (REQ-OBS-009)
- [ ] OBS-009 实现 Prometheus 指标导出 (REQ-OBS-010)

### 8.3 Alerts
- [ ] OBS-010 实现告警规则引擎 — 阈值检测 + 级别分类 (REQ-OBS-011)
- [ ] OBS-011 配置 Service 错误率 > 5% 告警 (P1) (REQ-OBS-011)
- [ ] OBS-012 配置 P99 > 10s 告警 (P2) (REQ-OBS-012)
- [ ] OBS-013 配置 LLM API 可用性 < 99% 告警 + 自动切换 fallback (P1) (REQ-OBS-013)
- [ ] OBS-014 配置 Kafka lag > 1000 告警 + 自动扩展消费者 (P2) (REQ-OBS-014)
- [ ] OBS-015 配置 Service 日成本异常飙升告警 (P2) (REQ-OBS-015)

### 8.4 Cost Tracking
- [ ] OBS-016 实现 LLM / Embedding / Rerank 调用成本记录 (REQ-OBS-016)
- [ ] OBS-017 实现成本分摊计算 — 按 Service × Model × 时间 (REQ-OBS-017)
- [ ] OBS-018 实现本地模型 GPU 成本折算 (REQ-OBS-018)
- [ ] OBS-019 实现 Service 日/周/月成本报表 API (REQ-OBS-019)

### 8.5 Offline Evaluation
- [ ] OBS-020 实现评测数据集 CRUD — question/expected_answer/relevant_chunks/difficulty/category (REQ-OBS-020)
- [ ] OBS-021 实现评测集来源管理 — 人工标注 / 生产抽样 / LLM 合成 (REQ-OBS-021)
- [ ] OBS-022 实现 RAGAS 四维度评估指标 — Faithfulness, Answer Relevance, Context Relevance, Context Recall (REQ-OBS-022)
- [ ] OBS-023 实现 Retriever-only 评估模式 (REQ-OBS-023)
- [ ] OBS-024 实现 Full RAG 评估模式 (REQ-OBS-023)
- [ ] OBS-025 实现评估执行引擎 — 选定评测集 + 指定 Service → 指标报告 + 历史对比 (REQ-OBS-024)

### 8.6 Online Sampling
- [ ] OBS-026 实现生产流量比例抽样器 — 默认 5% 可配置 (REQ-OBS-025)
- [ ] OBS-027 实现 LLM-as-Judge 自动评估 — 抽样结果入库 (REQ-OBS-025, REQ-OBS-026)
- [ ] OBS-028 实现低分 Case 自动标记待审核 (REQ-OBS-026)
- [ ] OBS-029 采样评估 LLM 调用不计入 Service 成本 (REQ-OBS-027)

### 8.7 User Feedback
- [ ] OBS-030 实现用户反馈上报 API — 点赞/点踩/复制/追问 + trace_id 关联 (REQ-OBS-028, REQ-OBS-029)
- [ ] OBS-031 实现反馈仪表板数据聚合 — 按 Service × 时间满意度趋势 (REQ-OBS-030)

### 8.8 Feedback Loop
- [ ] OBS-032 实现低分 Case 自动入库待审核 (REQ-OBS-031)
- [ ] OBS-033 实现根因归类模型 — 文档缺失/分块不当/检索遗漏/LLM 幻觉/其他 (REQ-OBS-032)
- [ ] OBS-034 实现根因 → 可执行改进项关联 (REQ-OBS-033)

### 8.9 Trace Query
- [ ] OBS-035 实现 Trace 查询 API — 按 trace_id / Service / 时间范围 (REQ-OBS-034)
- [ ] OBS-036 实现单 Trace 详情 — DAG 执行图 + 节点详情 (输入/输出/耗时/状态) (REQ-OBS-035)

---

## Implementation Order

**Phase 1 (Foundation):** INF-001 → PA-001~010 → DP-001~014 → DP-015~023 → DP-028~031
**Phase 2 (Core Pipeline — Pipeline Config):** DP-024~027 → QRY-001~035（Pipeline Config 编排，无 DAG）
**Phase 3 (Advanced Pipeline — DAG Engine):** DAG-001~007（infrastructure/dag/，替换固定拓扑为 DAG 编排）
**Phase 4 (API & Security):** SEC-001~017 → API-001~007
**Phase 5 (Production Readiness):** INF-002~009 → OBS-001~036

## Directory Mapping

| 组件 | 代码目录 | Spec |
|------|---------|------|
| Platform Architecture | `domain/` (shared) | platform-architecture |
| **Index Domain** | `domain/index/` | index-domain |
| **Query Domain** | `domain/query/` | query-domain |
| **DAG Execution Engine** | `infrastructure/dag/` | (in design.md) |
| DAG Data Structures | `types/` (GraphDefinition, NodeConfig, ...) | (in design.md) |
| API & Access | `api/`, `trigger/` | api-design |
| Security | `domain/`, `trigger/http/` | security |
| Storage & Infra | `infrastructure/` | storage-infrastructure |
| Observability | cross-cutting | observability-evaluation |
