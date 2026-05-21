# Spec: Query Domain — Document Search & Generation

## Overview

Query 域负责 RAG 系统的**读路径（文档检索分支）**：接收用户查询 → 意图路由 → 混合检索 → 融合重排 → 上下文组装 → Prompt 渲染 → LLM 生成 → 引用溯源 → 后处理。Text-to-SQL 和 Hybrid 路径见 text-to-sql change；知识图谱扩展见 knowledge-graph change。

## Pipeline Config — 统一编排配置

Phase 1 使用统一的 4 段 Pipeline Config（routing + document_search + data_query + postprocessing），`document_search` 内部采用固定拓扑 + enable/disable 模式。`data_query` 段在 text-to-sql change 中激活。

### 设计原则

- **强制的 intent_detect**：不再是可选预处理，而是所有请求的入口路由器
- **按需启用**：每个阶段可通过 `enabled` 字段独立开关
- **4 段顶层结构**：routing / document_search / data_query / postprocessing
- **Phase 1 无 DAG 依赖**：编排逻辑由 `QueryPipelineCase` 直接顺序调用领域服务
- **Phase 2/3 升级为 DAG**：支持自定义拓扑、条件分支和 Agentic RAG

### Pipeline Config 结构 (document_search 部分)

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
    "context": { "tokenBudget": 4096, "includeCitations": true },
    "generation": { "model": "gpt-4o", "temperature": 0.3, "maxTokens": 1024,
                    "fallbackModel": "gpt-4o-mini" }
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
  │   ├─ [if enabled] rerank → deduplicate
  │   ├─ [if enabled] context_assemble + citation_mark
  │   └─ [if enabled] prompt_render → llm_generate (SSE)
  │
  ├─ DATA_QUERY: → 见 text-to-sql change
  ├─ HYBRID:     → 见 text-to-sql change
  │
  └─ [postprocessing] citation_extract → safety_filter → format_convert
```

## Requirements

### 意图路由（强制）

- **REQ-QRY-R01**: intent_detect 是强制执行的第一步，不可跳过。输出 QueryIntent（DOCUMENT_SEARCH / DATA_QUERY / HYBRID）
- **REQ-QRY-R02**: 判断规则：统计词/时间范围+数值 → DATA_QUERY；无 DATABASE DataSource → 强制 DOCUMENT_SEARCH；其余 → HYBRID
- **REQ-QRY-R03**: query_router — 运行时根据查询特征自动选择检索策略
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
```

- **REQ-QRY-004**: vector_search — 基于 embedding 的语义检索（pgvector）
- **REQ-QRY-005**: keyword_search — 基于 Elasticsearch 的关键词检索（BM25 + ik_max_word）
- **REQ-QRY-006**: metadata_filter — 基于文档元数据的过滤（类型/日期/状态/自定义字段）
- **REQ-QRY-007**: 向量检索和关键词检索可独立启用/禁用，支持并行调度

### 融合

- **REQ-QRY-008**: rrf_fusion — Reciprocal Rank Fusion，可配置 k 值
- **REQ-QRY-009**: linear_fusion — 线性加权融合，可配置各路权重
- **REQ-QRY-010**: 融合策略由 Service 配置选择

### 重排序与去重

- **REQ-QRY-011**: rerank — 使用 Cross-encoder 精排，可配置 top_k
- **REQ-QRY-012**: deduplicate — 基于内容相似度去重，可配置阈值

### 上下文组装

- **REQ-QRY-013**: 按 Service 配置的 token_budget 进行上下文组装，实时计算 token 数
- **REQ-QRY-014**: 支持三种上下文装配策略：Top-N 截断 / 上下文压缩 / 动态预算分配
- **REQ-QRY-015**: 上下文组装时自动为每个 chunk 生成引用标记（[ref_1], [ref_2]...）

### Prompt 模板系统

- **REQ-QRY-016**: 三层模板体系：平台预置 → Service 自定义 → API 调用覆盖
- **REQ-QRY-017**: 模板变量体系：{role}, {context}, {query}, {history}, {kb_names}, {date}, {service_name}, {custom_*}
- **REQ-QRY-018**: 模板支持条件片段（如：有对话历史时展示，无则隐藏）

### LLM 调用

- **REQ-QRY-019**: 支持多模型路由（primary 和 fallback 模型）
- **REQ-QRY-020**: 支持流式输出（SSE chunk by chunk）
- **REQ-QRY-021**: 故障转移：primary 不可用时自动切换 fallback
- **REQ-QRY-022**: 重试策略：可配置最大重试次数和退避策略（fixed / exponential）
- **REQ-QRY-023**: 支持配置 temperature、max_tokens、top_p 等生成参数

### 引用溯源

- **REQ-QRY-030**: 输出结果必须包含引用映射：[ref_N] → chunk 原始信息（document/章节/位置/sourcePointer）
- **REQ-QRY-031**: 引用源附带检索得分（vector_score, keyword_score, fusion_score, rerank_score）
- **REQ-QRY-032**: 支持 LLM 引用与 chunk 的自动对齐

### 后处理

- **REQ-QRY-040**: safety_filter — 检测和过滤 PII 泄露、敏感词、prompt injection
- **REQ-QRY-041**: format_convert — 输出格式转换：Markdown / JSON / 纯文本

### 降级策略

Phase 1 降级在 Pipeline 阶段级别生效。Phase 2/3 升级为 DAG 节点级降级。

- **REQ-QRY-050**: skip — 阶段失败/超时时跳过，Pipeline 继续执行
- **REQ-QRY-051**: error — 阶段失败/超时时终止 Pipeline 并返回错误
- **REQ-QRY-052**: pass_through — 阶段被禁用或失败时原样传递上游输出

## Query Domain 与 Index Domain 的交互

```
Query Domain                          Index Domain
─────────────                        ─────────────
kb_router ───────────读取──────────▶ IKBGrantRepository
VectorRetriever ─────读取──────────▶ IVectorRepository.query()
KeywordRetriever ────读取──────────▶ IChunkRepository.searchByKeyword()
metadata_filter ─────读取──────────▶ IChunkRepository.filterByMetadata()
citation ────────────读取──────────▶ IChunkRepository.findById(chunkId)
```

Query 域**不拥有**索引数据。所有数据读取通过 Index 域定义的 Repository 接口完成。
