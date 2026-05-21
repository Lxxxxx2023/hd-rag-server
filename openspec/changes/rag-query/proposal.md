# Proposal: RAG Query Engine

## Summary

实现 RAG 平台的查询检索与 LLM 生成引擎（读路径）。核心链路：意图路由 → 向量+关键词混合检索 → 融合重排 → 上下文组装 → Prompt 模板渲染 → LLM 生成（SSE 流式）→ 引用溯源 → 后处理。Phase 1 使用 Pipeline Config（固定拓扑 + enable/disable），Phase 2/3 升级为 DAG 执行引擎支持自定义拓扑和 Agentic RAG。

这是 RAG 平台的核心价值交付模块——用户查询进来，带引用的答案出去。

## User Stories

### 业务开发者 / 查询用户

- **US-1**: 作为**业务开发者**，我想**用自然语言发起查询**，系统自动判断意图并返回**带引用溯源的答案**，以便我信任和验证生成结果。
- **US-2**: 作为**业务开发者**，我想**自定义 Prompt 模板**（在不修改平台默认模板的前提下），以便控制生成答案的风格和格式。
- **US-3**: 作为**业务开发者**，我想通过 SSE 流式接收生成结果（逐 token），以便给终端用户更好的响应体验。

## User Actions（关键操作路径）

### 路径 A：标准文档检索问答

```
业务方 POST /api/v1/services/{serviceId}/rag (stream=true)
→ intent_detect 判定为 DOCUMENT_SEARCH
→ 并行: vector_search(pgvector) + keyword_search(ES BM25)
→ RRF/linear fusion → rerank (Cross-encoder) → deduplicate
→ context_assemble (token 预算 + 引用标记) → prompt_render
→ llm_generate (SSE token 流) → citation_extract → safety_filter → done
```

### 路径 B：自定义 Prompt 模板

```
业务方创建 Service 级 Prompt 模板（继承平台预置 + 覆盖字段）
→ 模板存入 service_prompt_templates → Redis 缓存
→ 查询时: Layer 1(平台预置) → Layer 2(Service 自定义) → Layer 3(API 参数覆盖)
→ prompt_render 渲染最终 prompt → llm_generate
```

## Motivation

当前各业务线的检索和生成逻辑耦合在业务代码中，检索策略单一（仅向量相似度 Top-K），缺少统一的意图路由、融合重排、Prompt 管理和质量溯源能力。本模块将 RAG 的读路径标准化为可配置的 Pipeline，让业务方通过配置而非代码来优化检索和生成效果。

## Core Design Decisions

| # | 决策 | 说明 |
|---|------|------|
| 1 | Pipeline Config 统一 4 段结构 | routing + document_search + data_query + postprocessing（data_query 段在 text-to-sql change 中激活） |
| 2 | intent_detect 强制路由器 | 输出 DOCUMENT_SEARCH / DATA_QUERY / HYBRID，决定后续执行路径 |
| 3 | 检索策略并行执行 | vector_search + keyword_search 并行 → fusion 合并排序 |
| 4 | Prompt 三层模板 | 平台预置 → Service 自定义 → 调用时覆盖 |
| 5 | Pipeline Config (Phase 1) → DAG Engine (Phase 2/3) | Phase 1 固定拓扑 + enable/disable，Phase 2/3 自定义拓扑 + 条件分支 |
| 6 | IRetriever 统一检索接口 | VectorRetriever / KeywordRetriever / HybridRetriever 实现同一接口 |
| 7 | SSE 流式输出 | token / citation / error / done 四种事件类型 |

## Non-goals

- 不实现 Text-to-SQL 查询路径（见 text-to-sql change）
- 不实现混合检索的 data_query 分支（见 text-to-sql change）
- 不实现知识图谱扩展检索（见 knowledge-graph change）
- 不修改 ContentTree 节点类型

## Dependencies

- [platform-foundation](../platform-foundation/proposal.md) — Service 配置、授权校验、API Key 认证
- [document-indexing](../document-indexing/proposal.md) — Chunk/Vector 索引数据读取
