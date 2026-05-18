# Design: RAG Platform Core

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                         RAG Platform                             │
│                                                                  │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐ │
│  │  Platform   │  │  Document  │  │ Retrieval  │  │ Generation │ │
│  │Architecture │  │ Processing │  │  Pipeline  │  │  Pipeline  │ │
│  └──────┬─────┘  └──────┬─────┘  └──────┬─────┘  └──────┬─────┘ │
│         │               │               │               │       │
│         └───────────────┴───────────────┴───────────────┘       │
│                                 │                                │
│                        ┌────────┴────────┐                       │
│                        │   Unified RAG   │                       │
│                        │   DAG Engine    │                       │
│                        └─────────────────┘                       │
└──────────────────────────────────────────────────────────────────┘
```

## Resource Model

```
  Platform
  ├── Service (租户单元)
  │   ├── id, name
  │   ├── chunk_config, embedding_config
  │   ├── retrieval_graph (DAG)
  │   ├── generation_graph (DAG)
  │   ├── prompt_template
  │   └── model_config (primary, fallback, temperature, retry)
  │
  ├── KnowledgeBase
  │   ├── id, name, owner
  │   ├── documents[]
  │   └── ServiceKBGrant[] (N:M 授权)
  │       ├── service_id
  │       ├── kb_id
  │       └── permission (read / write)
  │
  └── Document
      ├── id, kb_id
      ├── title, file_type (pdf / md)
      ├── raw_content, structure (保留原始结构)
      └── status (uploaded → parsing → parsed → ready)
```

## RAGGraph DAG Engine

### Core Concepts

统一 DAG 编排引擎，检索和生成算子可在同一个 Graph 中组合：

```
  Graph = { nodes: Node[], edges: Edge[] }

  Node = {
    id: string,
    operator: OperatorType,
    config: OperatorConfig,
    deps: string[],        // 依赖节点 ID 列表
    on_failure: skip | error | pass_through,
    timeout: ms
  }

  Edge = implicit (via deps) — 上游节点完成 → 输出 → 下游节点输入
```

### Execution Model

- 拓扑排序确定执行顺序
- 无依赖节点并行执行
- 超时和降级策略按节点配置
- Schema 校验在 DAG 保存时完成（类型安全）

### Operator Categories

```
  预处理算子:    query_rewrite, intent_detect, term_expand, translate, hyde_generate
  检索算子:      vector_search, keyword_search, bm25_search, metadata_filter
  融合算子:      rrf_fusion, linear_fusion
  排序算子:      rerank, deduplicate
  上下文算子:    context_assemble, context_compress, context_truncate
  生成算子:      prompt_build, llm_generate
  后处理算子:    citation_extract, fact_check, safety_filter, format_convert
```

### Implementation Phases

| Phase | Scope |
|-------|-------|
| Phase 1 | Level 0-1: 线性 DAG，无环。标准 RAG 流程（改写→检索→融合→重排→生成→引用） |
| Phase 2 | Level 2: 条件跳转。事实校验失败 → 二次检索 → 重新生成 |
| Phase 3 | Level 3: Agentic RAG。LLM 自主决策何时检索、检索什么 |

## Storage Model

```
  raw_documents       — KB 级共享，原始文档 + 解析后的结构化内容
  service_chunks      — Service 级独立，按 Service 的分块配置切割
  service_vectors     — Service 级独立，按 Service 的 embedding 模型向量化
  service_retrieval   — Service 级，检索图定义和算子配置
  service_generation  — Service 级，生成图定义和模板配置
```

## Prompt Template System

```
  Layer 1: 平台预置模板   — 通用场景模板（客服、文档总结、代码审查...）
  Layer 2: Service 自定义 — 继承预置 + 覆盖字段 + 自定义变量
  Layer 3: API 调用覆盖   — 每次请求可临时覆盖部分参数
```
