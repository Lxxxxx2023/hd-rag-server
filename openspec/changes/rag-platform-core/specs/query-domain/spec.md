# Spec: Query Domain (读路径)

## Overview

Query 域负责 RAG 系统的**读路径**：接收用户查询，经过预处理、检索、融合、重排序、上下文组装、Prompt 构建、LLM 生成，最终返回带引用的答案。它不拥有数据，通过 Index 域的 Repository 接口读取 Chunks 和 Vectors。

核心职责：
- 查询预处理（改写/意图/扩展/HyDE）
- 混合检索（语义向量 + BM25 关键词）与融合
- 重排序与去重
- 上下文组装与 Token 预算管理
- Prompt 模板渲染与 LLM 调用
- 引用溯源与后处理

## Pipeline Config — Phase 1 编排方式

Phase 1 使用 **Pipeline Config** 而非 DAG 引擎来编排查询流程。Pipeline Config 是一个固定拓扑 + 可配置参数的轻量方案。

### 设计原则

- **固定拓扑**：执行顺序固定为 rewrite → route → search → rerank → assemble → generate
- **按需启用**：每个阶段可通过 `enabled` 字段独立开关
- **参数可配**：每个阶段支持 Service 级参数覆盖（top_k、threshold 等）
- **无 DAG 依赖**：编排逻辑由 `QueryPipelineCase` 直接顺序调用领域服务，不引入 `infrastructure/dag/`

### 配置结构

```json
{
  "pipeline": {
    "rewrite": {
      "enabled": true,
      "params": { "variants": 3, "strategy": "hyde" }
    },
    "route": {
      "enabled": true,
      "params": { "mode": "auto", "kb_scope": "authorized", "fallback_strategy": "hybrid" }
    },
    "search": {
      "enabled": true,
      "params": { "vector_top_k": 20, "keyword_top_k": 20, "parallel": true }
    },
    "rerank": {
      "enabled": true,
      "params": { "top_k": 10, "model": "bge-reranker-v2-m3" }
    },
    "assemble": {
      "enabled": true,
      "params": { "strategy": "top_n", "token_budget": 4096 }
    },
    "generate": {
      "enabled": true,
      "params": { "stream": true, "temperature": 0.7 }
    }
  }
}
```

### 执行模型

```
QueryPipelineCase
  │
  ├─ [if rewrite.enabled] → query_rewrite / hyde_generate
  ├─ [if route.enabled]   → query_router → kb_router（决定检索策略 + 目标 KB）
  ├─ [if search.enabled]  → IRetriever.retrieve() ∥ vector ∥ keyword → fusion
  ├─ [if rerank.enabled]  → rerank → deduplicate
  ├─ [if assemble.enabled] → context_assemble + citation_mark
  └─ [if generate.enabled] → prompt_render → LLM call (SSE)
```

每个阶段的输出作为下一阶段的输入，阶段被禁用时上游输出原样透传（pass_through）。

### 与 DAG Engine 的关系

| 维度 | Pipeline Config (Phase 1) | DAG Engine (Phase 2/3) |
|------|--------------------------|------------------------|
| 拓扑 | 固定线性 | 自定义 DAG |
| 条件分支 | 不支持 | 支持（基于上游结果路由） |
| 并行调度 | search 阶段内置并行 | 通用并行调度 |
| 降级策略 | 阶段级 skip / error | 节点级 skip / error / pass_through |
| 实现量 | ~200 行 | ~2000 行 |
| 适用场景 | 常规 RAG 问答 | 条件分支、Agentic RAG、多路召回 |

Phase 2/3 引入 DAG Engine（`infrastructure/dag/`）后，Pipeline Config 仍可作为默认配置保留，DAG 作为高级自定义选项。

## Requirements

### 查询预处理

- **REQ-QRY-001**: query_rewrite — 使用 LLM 改写用户查询，可生成多个变体
- **REQ-QRY-002**: intent_detect — 识别用户意图类别
- **REQ-QRY-003**: term_expand — 对查询中的术语做同义词/上下位词扩展
- **REQ-QRY-004**: hyde_generate — 生成假设文档用于检索（HyDE 策略）

### 查询路由

Router 是 LlamaIndex 揭示的一个关键缺口。在查询预处理和检索之间，Router 决定用哪些 KB、哪种检索策略来处理当前查询。当前设计将检索策略选择放在 Service 配置中，但缺少**运行时**根据查询特征动态路由的能力。

- **REQ-QRY-004-R1**: query_router — 运行时根据查询特征自动选择检索策略：
  - 事实性查询（日期/数字/专有名词密集）→ 侧重 keyword_search（BM25 精确匹配）
  - 概念性查询（抽象/解释类表述）→ 侧重 vector_search（语义理解）
  - 综合查询 → hybrid（多路召回 + 高权重 fusion）
  - 支持按意图类别、查询长度、关键词密度等特征自动决策，也可由 Service 配置强制指定策略
- **REQ-QRY-004-R2**: kb_router — 按用户意图匹配已授权的 KB，支持多 KB 并发检索。路由结果作为 metadata_filter 的输入，决定检索范围。单 KB / 多 KB / 全 KB 三种模式。

### 检索

所有检索器统一实现 `IRetriever` 接口（策略模式）：

```java
interface IRetriever {
    List<RetrievedChunk> retrieve(QueryContext ctx, RetrievalConfig config);
}
// VectorRetriever, KeywordRetriever, HybridRetriever 实现
```

统一接口的好处：新增检索策略（图检索、SQL 检索、外部 API 检索等）只需注册新实现，不影响 Pipeline 拓扑；同时 Router 可以通过 `IRetriever` 统一调用所有检索器。

- **REQ-QRY-005**: vector_search — 基于 embedding 的语义检索，实现 `IRetriever` 接口，通过 Index 域的向量存储接口查询
- **REQ-QRY-006**: keyword_search — 基于 Elasticsearch 的关键词检索（BM25 + ik_max_word 中文分词），实现 `IRetriever` 接口
- **REQ-QRY-007**: metadata_filter — 基于文档元数据的过滤（类型/日期/状态/自定义字段）
- **REQ-QRY-008**: 向量检索和关键词检索可独立启用/禁用，支持并行调度

### 融合

- **REQ-QRY-009**: rrf_fusion — Reciprocal Rank Fusion，可配置 k 值
- **REQ-QRY-010**: linear_fusion — 线性加权融合，可配置各路权重
- **REQ-QRY-011**: 融合策略由 Service 配置选择

### 重排序与去重

- **REQ-QRY-012**: rerank — 使用 Cross-encoder 精排，可配置 top_k，支持自部署和外部 API 两种模式
- **REQ-QRY-013**: deduplicate — 基于内容相似度去重，可配置阈值

### 上下文组装

- **REQ-QRY-014**: 接收检索输出，按 Service 配置的 token_budget 进行上下文组装，实时计算 token 数
- **REQ-QRY-015**: 支持三种上下文装配策略：Top-N 截断 / 上下文压缩 / 动态预算分配
- **REQ-QRY-016**: 上下文组装时自动为每个 chunk 生成引用标记（[ref_1], [ref_2]...）

### Prompt 模板系统

- **REQ-QRY-017**: 三层模板体系：
  - Layer 1: 平台预置模板（客服/文档总结/代码审查/翻译）
  - Layer 2: Service 自定义模板（继承预置 + 覆盖 + 自定义变量）
  - Layer 3: API 调用时参数覆盖（temperature/max_tokens/模板变量）
- **REQ-QRY-018**: 模板变量体系至少包含：{role}, {context}, {query}, {history}, {kb_names}, {date}, {service_name}, {custom_*}
- **REQ-QRY-019**: 模板支持条件片段（如：有对话历史时展示，无则隐藏）

### LLM 调用

- **REQ-QRY-020**: 支持多模型路由（Service 配置 primary 和 fallback 模型）
- **REQ-QRY-021**: 支持流式输出（SSE chunk by chunk），生成过程中实时推送 token 流
- **REQ-QRY-022**: 故障转移：primary 不可用时自动切换 fallback
- **REQ-QRY-023**: 重试策略：可配置最大重试次数和退避策略（fixed / exponential）
- **REQ-QRY-024**: 支持配置 temperature、max_tokens、top_p 等生成参数

### 引用溯源

- **REQ-QRY-025**: 输出结果必须包含引用映射：[ref_N] → chunk 原始信息（document/章节/位置/sourcePointer）
- **REQ-QRY-026**: 引用源附带检索得分（vector_score, keyword_score, fusion_score, rerank_score）
- **REQ-QRY-027**: 支持 LLM 引用与 chunk 的自动对齐（citation → context mapping）

### 后处理

- **REQ-QRY-028**: safety_filter — 检测和过滤 PII 泄露、敏感词、prompt injection
- **REQ-QRY-029**: format_convert — 输出格式转换：Markdown / JSON / 纯文本

### 降级策略

Phase 1 降级在 Pipeline 阶段（stage）级别生效，每个阶段的降级策略在 Pipeline Config 中配置。Phase 2/3 升级为 DAG 节点级降级。

- **REQ-QRY-030**: skip — 阶段失败/超时时跳过，Pipeline 继续执行
- **REQ-QRY-031**: error — 阶段失败/超时时终止 Pipeline 并返回错误
- **REQ-QRY-032**: pass_through — 阶段被禁用或失败时原样传递上游输出
- **REQ-QRY-033**: 降级事件需记录到执行追踪中

## Query Domain 与 Index Domain 的交互

```
Query Domain                          Index Domain
─────────────                        ─────────────
                                     Document (聚合根)
                                     Chunk (实体)
                                     Vector (在 pgvector 中)

kb_router ───────────读取──────────▶ IKBGrantRepository（获取授权 KB 列表）
IRetriever.retrieve()
  ├─ VectorRetriever ──读取────────▶ IVectorRepository.query()
  ├─ KeywordRetriever ──读取───────▶ IChunkRepository.searchByKeyword()
  └─ HybridRetriever ──读取────────▶ 上述两者 + fusion
metadata_filter ──────读取──────────▶ IChunkRepository.filterByMetadata()
citation ────────────读取────────────▶ IChunkRepository.findById(chunkId)
```

Query 域**不拥有**索引数据。所有的数据读取都通过 Index 域定义的 Repository 接口（`domain/index/adapter/repository/`）完成，由 Infrastructure 层实现。
`IRetriever` 接口统一了所有检索策略，Router 通过该接口调用具体检索器，实现检索策略的运行时切换。
