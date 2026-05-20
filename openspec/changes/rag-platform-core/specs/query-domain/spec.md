# Spec: Query Domain (读路径)

## Overview

Query 域负责 RAG 系统的**读路径**：接收用户查询，经过意图路由、检索、融合、重排序、上下文组装、Prompt 构建、LLM 生成，最终返回带引用的答案。支持文档检索、Text-to-SQL 数据查询、混合查询和知识图谱检索四条子路径。Query 域不拥有数据，通过 Index 域的 Repository 接口读取 Chunks、Vectors、SchemaIndex 和图数据。

## Pipeline Config — 统一编排配置

Phase 1 使用统一的 4 段 Pipeline Config（routing + document_search + data_query + postprocessing），`document_search` 内部采用固定拓扑 + enable/disable 的模式。

### 设计原则

- **强制的 intent_detect**：不再是可选预处理，而是所有请求的入口路由器
- **按需启用**：每个阶段可通过 `enabled` 字段独立开关
- **4 段顶层结构**：routing / document_search / data_query / postprocessing，每段独立配置
- **Phase 1 无 DAG 依赖**：编排逻辑由 `QueryPipelineCase` 直接顺序调用领域服务
- **Phase 2/3 升级为 DAG**：支持自定义拓扑、条件分支和 Agentic RAG

### Pipeline Config 结构

```json
{
  "routing": {
    "intent_detect": { "model": "gpt-4o-mini" },
    "default": "document_search"
  },
  "document_search": {
    "preprocessing": {
      "query_rewrite": { "enabled": true, "variants": 3 },
      "hyde_generate": { "enabled": false }
    },
    "search": {
      "vector_search":  { "enabled": true, "topK": 20, "similarity": "cosine" },
      "keyword_search": { "enabled": true, "topK": 10 },
      "metadata_filter":{ "enabled": false }
    },
    "fusion":  { "strategy": "rrf", "rrf_k": 60 },
    "rerank":  { "enabled": true, "topK": 5, "model": "bge-reranker-v2" },
    "kg_expand": { "enabled": false, "expandDepth": 1 },
    "context": { "tokenBudget": 4096, "includeCitations": true },
    "generation": { "model": "gpt-4o", "temperature": 0.3, "maxTokens": 1024,
                    "fallbackModel": "gpt-4o-mini" }
  },
  "data_query": {
    "schema_retrieve": { "topK": 5 },
    "text_to_sql": { "model": "gpt-4o", "dialect": "mysql", "maxRetries": 2 },
    "sql_validate": { "allowDML": false, "allowDDL": false, "maxRows": 1000 },
    "sql_execute":   { "timeoutMs": 5000 },
    "result_format": { "maxCells": 500 },
    "generation":    { "model": "gpt-4o", "maxTokens": 1024 }
  },
  "postprocessing": {
    "citation_extract": { "enabled": true },
    "safety_filter":    { "enabled": true, "piiDetection": true }
  }
}
```

### 执行模型

```
QueryPipelineCase
  │
  ├─ [routing] intent_detect → QueryIntent（强制）
  │
  ├─ DOCUMENT_SEARCH:
  │   ├─ [if enabled] query_rewrite / hyde_generate
  │   ├─ [if enabled] vector_search ∥ keyword_search ∥ metadata_filter → fusion
  │   ├─ [if enabled] kg_expand（结构图谱扩展）
  │   ├─ [if enabled] rerank → deduplicate
  │   ├─ [if enabled] context_assemble + citation_mark
  │   └─ [if enabled] prompt_render → llm_generate (SSE)
  │
  ├─ DATA_QUERY:
  │   ├─ schema_retrieve → text_to_sql → sql_validate → sql_execute → result_format
  │   └─ llm_generate
  │
  ├─ HYBRID:
  │   ├─ ∥ DOCUMENT_SEARCH + DATA_QUERY（并行）
  │   ├─ context_merge（合并两路，标注来源）
  │   └─ llm_generate
  │
  └─ [postprocessing] citation_extract → safety_filter → format_convert
```

---

## Requirements

### 意图路由（强制）

- **REQ-QRY-R01**: intent_detect 是强制执行的第一步，不可跳过。根据 query 特征 + Service 关联的 DataSource 类型输出 QueryIntent（DOCUMENT_SEARCH / DATA_QUERY / HYBRID）
- **REQ-QRY-R02**: 判断规则：统计词/时间范围+数值 → DATA_QUERY；Service 无 DATABASE DataSource → 强制 DOCUMENT_SEARCH；文档语气词 → DOCUMENT_SEARCH；其余 → HYBRID
- **REQ-QRY-R03**: query_router — 运行时根据查询特征自动选择检索策略：事实性查询（日期/数字/专有名词密集）→ 侧重 keyword_search；概念性查询（抽象/解释类）→ 侧重 vector_search；综合查询 → hybrid
- **REQ-QRY-R04**: kb_router — 按用户意图匹配已授权的 KB，支持单/多/全 KB 三种模式

### 查询预处理

- **REQ-QRY-001**: query_rewrite — 使用 LLM 改写用户查询，可生成多个变体
- **REQ-QRY-002**: term_expand — 对查询中的术语做同义词/上下位词扩展
- **REQ-QRY-003**: hyde_generate — 生成假设文档用于检索（HyDE 策略）

### 检索

所有检索器统一实现 `IRetriever` 接口（策略模式）：

```java
interface IRetriever {
    List<RetrievedChunk> retrieve(QueryContext ctx, RetrievalConfig config);
}
// VectorRetriever, KeywordRetriever, HybridRetriever 实现
```

- **REQ-QRY-004**: vector_search — 基于 embedding 的语义检索，实现 `IRetriever` 接口
- **REQ-QRY-005**: keyword_search — 基于 Elasticsearch 的关键词检索（BM25 + ik_max_word 中文分词），实现 `IRetriever` 接口
- **REQ-QRY-006**: metadata_filter — 基于文档元数据的过滤（类型/日期/状态/自定义字段）
- **REQ-QRY-007**: 向量检索和关键词检索可独立启用/禁用，支持并行调度

### 融合

- **REQ-QRY-008**: rrf_fusion — Reciprocal Rank Fusion，可配置 k 值
- **REQ-QRY-009**: linear_fusion — 线性加权融合，可配置各路权重
- **REQ-QRY-010**: 融合策略由 Service 配置选择

### 重排序与去重

- **REQ-QRY-011**: rerank — 使用 Cross-encoder 精排，可配置 top_k，支持自部署和外部 API 两种模式
- **REQ-QRY-012**: deduplicate — 基于内容相似度去重，可配置阈值

### 知识图谱扩展（文档检索路径内）

- **REQ-QRY-013**: graph_expand — Phase 2 基于 ContentTree 结构关系扩展检索上下文（父/子/兄弟 Chunk），通过 Pipeline Config 的 `kg_expand.enabled` 控制，默认关闭
- **REQ-QRY-014**: entity_link — Phase 3 查询提及文本 → 图数据库实体消歧匹配，返回候选实体列表
- **REQ-QRY-015**: graph_traverse — Phase 3 基于 GraphQuery 执行 Cypher 多跳遍历，返回实体/关系及关联 Chunk
- **REQ-QRY-016**: graph_vector_fusion — Phase 3 图检索结果与向量检索结果的融合（graph_first / interleave / score_combine）

### 上下文组装

- **REQ-QRY-017**: 接收检索输出，按 Service 配置的 token_budget 进行上下文组装，实时计算 token 数
- **REQ-QRY-018**: 支持三种上下文装配策略：Top-N 截断 / 上下文压缩 / 动态预算分配
- **REQ-QRY-019**: 上下文组装时自动为每个 chunk 生成引用标记（[ref_1], [ref_2]...）

### Prompt 模板系统

- **REQ-QRY-020**: 三层模板体系：平台预置 → Service 自定义 → API 调用覆盖
- **REQ-QRY-021**: 模板变量体系：{role}, {context}, {query}, {history}, {kb_names}, {date}, {service_name}, {custom_*}
- **REQ-QRY-022**: 模板支持条件片段（如：有对话历史时展示，无则隐藏）

### LLM 调用

- **REQ-QRY-023**: 支持多模型路由（Service 配置 primary 和 fallback 模型）
- **REQ-QRY-024**: 支持流式输出（SSE chunk by chunk）
- **REQ-QRY-025**: 故障转移：primary 不可用时自动切换 fallback
- **REQ-QRY-026**: 重试策略：可配置最大重试次数和退避策略（fixed / exponential）
- **REQ-QRY-027**: 支持配置 temperature、max_tokens、top_p 等生成参数

### Text-to-SQL 数据查询路径

- **REQ-QRY-030**: schema_retrieve — 从 SchemaIndex 语义检索相关表和列，注入 dataSourceId + 候选表的 columns + relationships + sampleRows
- **REQ-QRY-031**: text_to_sql — 将 query + Schema 上下文送入 LLM 生成 SQL，支持最多 maxRetries 次重新生成（sql_validate 失败触发重试）
- **REQ-QRY-032**: sql_validate — SQL AST 语法校验 + 安全校验（禁止 DML/DDL）+ 自动追加 LIMIT maxRows
- **REQ-QRY-033**: sql_execute — 通过 JDBC 连接池执行 SQL，超时控制 timeoutMs，返回 ResultSet
- **REQ-QRY-034**: result_format — ResultSet 转为 Markdown 表格，超出 maxCells 时截断并附注

### 混合路径

- **REQ-QRY-040**: context_merge — 合并文档检索 ChunkContext 与数据查询 DataQueryResult，标注来源（"来自文档 [ref_N]" / "来自数据库查询"），按 token 预算裁剪
- **REQ-QRY-041**: 混合路径并行调度 — Path1（文档检索）+ Path2（数据查询）并行执行，等待两路结果后进入 context_merge
- **REQ-QRY-042**: 混合路径引用溯源 — citation_extract 同时处理文档引用（[ref_N]）和数据来源（"来自 {tableName} 查询"）

### 引用溯源

- **REQ-QRY-050**: 输出结果必须包含引用映射：[ref_N] → chunk 原始信息（document/章节/位置/sourcePointer）
- **REQ-QRY-051**: 引用源附带检索得分（vector_score, keyword_score, fusion_score, rerank_score）
- **REQ-QRY-052**: 支持 LLM 引用与 chunk 的自动对齐（citation → context mapping）

### 后处理

- **REQ-QRY-060**: safety_filter — 检测和过滤 PII 泄露、敏感词、prompt injection
- **REQ-QRY-061**: format_convert — 输出格式转换：Markdown / JSON / 纯文本

### 降级策略

Phase 1 降级在 Pipeline 阶段级别生效。Phase 2/3 升级为 DAG 节点级降级。

- **REQ-QRY-070**: skip — 阶段失败/超时时跳过，Pipeline 继续执行
- **REQ-QRY-071**: error — 阶段失败/超时时终止 Pipeline 并返回错误
- **REQ-QRY-072**: pass_through — 阶段被禁用或失败时原样传递上游输出
- **REQ-QRY-073**: 降级事件需记录到执行追踪中

## Query Domain 与 Index Domain 的交互

```
Query Domain                          Index Domain
─────────────                        ─────────────
                                     Document (聚合根)
                                     Chunk (实体)
                                     Vector (在 pgvector 中)
                                     SchemaIndex (schema_index 表)
                                     Entity / Relation (图数据库)

kb_router ───────────读取──────────▶ IKBGrantRepository
IRetriever.retrieve()
  ├─ VectorRetriever ──读取────────▶ IVectorRepository.query()
  ├─ KeywordRetriever ──读取───────▶ IChunkRepository.searchByKeyword()
  └─ HybridRetriever ──读取────────▶ 上述两者 + fusion
metadata_filter ──────读取──────────▶ IChunkRepository.filterByMetadata()
schema_retrieve ──────读取──────────▶ ISchemaIndexRepository.searchByQuery()
graph_expand ─────────读取──────────▶ ContentTree（通过 IChunkRepository）
entity_link ──────────读取──────────▶ GraphRepository.query()
graph_traverse ───────读取──────────▶ GraphRepository.query()
citation ────────────读取────────────▶ IChunkRepository.findById(chunkId)
```

Query 域**不拥有**索引数据。所有的数据读取都通过 Index 域定义的 Repository 接口完成，由 Infrastructure 层实现。
