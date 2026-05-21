# Tasks: RAG Query Engine

## Schema: spec-driven | Progress: 0/56 tasks

> Phase 1 编排方式：Pipeline Config — 固定拓扑 + enable/disable 开关。Phase 2/3 升级为 DAG Engine。

---

## 1. Query Preprocessing (3 tasks)

- [ ] QRY-001 实现 query_rewrite 算子 — LLM 改写查询，可生成多个变体
- [ ] QRY-002 实现 term_expand 算子 — 同义词/上下位词扩展
- [ ] QRY-003 实现 hyde_generate 算子 — HyDE 假设文档生成

## 2. Retrieval Operators (5 tasks)

- [ ] QRY-004 定义 IRetriever 统一检索接口 — retrieve(QueryContext, RetrievalConfig) → List<RetrievedChunk>
- [ ] QRY-005 实现 VectorRetriever — 通过 IVectorRepository 查询 pgvector 语义检索
- [ ] QRY-006 实现 KeywordRetriever — 通过 IChunkRepository 查询 ES BM25 检索
- [ ] QRY-007 实现 HybridRetriever — vector + keyword 并行执行 + fusion
- [ ] QRY-008 实现 metadata_filter 算子 — 按文档元数据过滤

## 3. Fusion & Ranking (5 tasks)

- [ ] QRY-009 实现 rrf_fusion 算子 — Reciprocal Rank Fusion
- [ ] QRY-010 实现 linear_fusion 算子 — 线性加权融合
- [ ] QRY-011 Service 级别融合策略配置
- [ ] QRY-012 实现 rerank 算子 — Cross-encoder 精排
- [ ] QRY-013 实现 deduplicate 算子 — 内容相似度去重

## 4. Context Assembly (5 tasks)

- [ ] QRY-014 实现 Token 计数器 — 实时计算 chunk token 数
- [ ] QRY-015 实现 Top-N 截断策略
- [ ] QRY-016 实现上下文压缩策略
- [ ] QRY-017 实现动态预算分配策略
- [ ] QRY-018 实现引用标记自动生成 ([ref_1], [ref_2]...)

## 5. Prompt Template System (6 tasks)

- [ ] QRY-019 定义模板三层数据模型
- [ ] QRY-020 实现模板变量引擎 — {role}, {context}, {query}, {history} 等
- [ ] QRY-021 实现条件片段渲染
- [ ] QRY-022 实现平台预置模板初始化数据（客服/文档总结/代码审查/翻译）
- [ ] QRY-023 实现 Service 模板继承与覆盖
- [ ] QRY-024 实现 PromptTemplateRepository 及 Redis 缓存

## 6. LLM Call (4 tasks)

- [ ] QRY-025 实现 LLM Provider 抽象 — 对接外部 API
- [ ] QRY-026 实现多模型路由 — primary / fallback
- [ ] QRY-027 实现重试策略 — fixed / exponential backoff
- [ ] QRY-028 实现 SSE 流式输出适配器

## 7. Citation & Post-processing (4 tasks)

- [ ] QRY-029 实现引用溯源 — [ref_N] → chunk 原始信息映射
- [ ] QRY-030 实现 LLM 引用与 chunk 自动对齐
- [ ] QRY-031 实现 safety_filter 后处理算子 — PII/敏感词/prompt injection
- [ ] QRY-032 实现 format_convert 后处理算子 — Markdown/JSON/文本

## 8. Pipeline Config (Phase 1 编排) (3 tasks)

- [ ] QRY-033 实现 PipelineConfigRepository — JSONB 存储统一 4 段结构（routing + document_search + data_query + postprocessing）
- [ ] QRY-034 实现 QueryPipelineCase — 按 Pipeline Config 执行，含三路分叉（DOCUMENT_SEARCH / DATA_QUERY / HYBRID）
- [ ] QRY-035 实现 Pipeline Config API Controller — PUT/PATCH service-pipeline-config

## 9. Query Repository Interfaces (3 tasks)

- [ ] QRY-036 定义 IChunkSearchRepository — Query 域读取 Chunks 的接口
- [ ] QRY-037 定义 IVectorSearchRepository — Query 域读取 Vectors 的接口
- [ ] QRY-038 定义 ILLMProviderPort — LLM 外部 API 调用接口

---

## 10. Intent Router (4 tasks)

- [ ] IR-001 定义 QueryIntent 枚举 — DOCUMENT_SEARCH / DATA_QUERY / HYBRID
- [ ] IR-002 实现 intent_detect 强制路由器 — 统计词检测、时间范围+数值检测、DATABASE DataSource 存在性判断
- [ ] IR-003 实现 QueryRouter — 读取 intent_detect 输出，分发到对应路径
- [ ] IR-004 实现 query_router 和 kb_router — 运行时根据查询特征选择检索策略 + 匹配授权 KB

---

## 11. DAG Execution Engine — Infrastructure 层 (7 tasks) 【Phase 2/3】

> 位于 `infrastructure/dag/`，纯技术组件。Phase 1 不使用，Phase 2/3 替换 Pipeline Config。

- [ ] DAG-001 定义 DAG 核心模型 — Graph / Node / Edge / OperatorType
- [ ] DAG-002 实现 DAG 拓扑排序与执行调度器 — 无依赖节点并行，有依赖者串行
- [ ] DAG-003 实现 DAG Schema 校验器 — 上下游算子类型兼容性校验
- [ ] DAG-004 实现节点超时控制与降级策略执行 — skip / error / pass_through
- [ ] DAG-005 定义算子 Schema 基类 — 输入/输出类型声明
- [ ] DAG-006 实现算子注册表 — OperatorRegistry，Query 算子统一注册
- [ ] DAG-007 实现算子配置参数声明与校验

---

## 12. Runtime API & Access (7 tasks)

- [ ] API-001 实现 Runtime API Controller — /search + /rag + SSE
- [ ] API-002 实现 SSE 事件协议 — token / citation / data_result / error / done
- [ ] API-003 实现 Layer 3 参数覆盖逻辑
- [ ] API-004 更新 RAGResponse 模型 — 新增 queryType / dataResult 字段
- [ ] API-005 实现 Pipeline Config 序列化/反序列化 — routing.intent_detect 等字段的 JSONB 映射
- [ ] API-006 实现 Pipeline Config API Controller — PUT/PATCH service-pipeline-config 支持新结构验证
- [ ] API-007 实现 Java SDK — HTTP Client + SSE 解析 + 自动重试

---

## Task Summary

| 模块 | 内容 | Tasks |
|------|------|-------|
| Preprocessing | query_rewrite / term_expand / hyde | QRY-001 ~ QRY-003 (3) |
| Retrieval | IRetriever + Vector/Keyword/Hybrid + metadata_filter | QRY-004 ~ QRY-008 (5) |
| Fusion & Ranking | RRF/linear fusion + rerank + dedup | QRY-009 ~ QRY-013 (5) |
| Context Assembly | Token 计数 + 截断/压缩/动态预算 + 引用标记 | QRY-014 ~ QRY-018 (5) |
| Prompt Template | 三层模板 + 变量引擎 + 条件渲染 + 缓存 | QRY-019 ~ QRY-024 (6) |
| LLM Call | Provider + 多模型路由 + 重试 + SSE | QRY-025 ~ QRY-028 (4) |
| Citation & Post | 引用溯源 + 对齐 + safety_filter + format | QRY-029 ~ QRY-032 (4) |
| Pipeline Config | Repository + QueryPipelineCase + API | QRY-033 ~ QRY-035 (3) |
| Query Repositories | IChunkSearch / IVectorSearch / ILLMProviderPort | QRY-036 ~ QRY-038 (3) |
| Intent Router | QueryIntent + intent_detect + QueryRouter | IR-001 ~ IR-004 (4) |
| DAG Engine | Graph/Node/Edge + 拓扑排序 + 超时降级 + Registry | DAG-001 ~ DAG-007 (7) |
| Runtime API | Controller + SSE + Layer3 + RAGResponse + SDK | API-001 ~ API-007 (7) |
| **Total** | | **56** |

## Implementation Order

**Phase 1 (Pipeline Config):**
1. IR-001~004 → Intent Router（路由先行）
2. QRY-036~038 → Repository 接口定义
3. QRY-004~008 → 检索算子
4. QRY-009~013 → 融合与重排序
5. QRY-001~003 → 查询预处理
6. QRY-014~018 → 上下文组装
7. QRY-019~024 → Prompt 模板系统
8. QRY-025~028 → LLM 调用
9. QRY-029~032 → 引用溯源与后处理
10. QRY-033~035 → Pipeline Config 编排
11. API-001~007 → Runtime API + SDK

**Phase 2/3 (DAG Engine):**
12. DAG-001~007 → DAG 引擎（替换 PipelineConfig 为 GraphExecutor）
