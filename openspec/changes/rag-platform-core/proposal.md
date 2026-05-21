# RAG Platform Core — 总览

> 本 change 已拆分为 7 个独立 change，每个可独立交付。此文档保留作为架构总览和子 change 索引。

## 子 Change 索引

按交付依赖顺序排列：

| # | Change | 用户角色 | 核心能力 | Tasks | 依赖 |
|---|--------|---------|---------|-------|------|
| 1 | [platform-foundation](../platform-foundation/proposal.md) | 平台管理员 | Service/KB 多租户、授权、API Key、RBAC、审计 | 38 | 无 |
| 2 | [document-indexing](../document-indexing/proposal.md) | 知识库运营 | 文档解析、分块、向量化、索引写入管道 | 31 | platform-foundation |
| 3 | [rag-query](../rag-query/proposal.md) | 业务开发者 | 意图路由、检索融合、Prompt 模板、LLM 生成、DAG | 56 | platform-foundation + document-indexing |
| 4 | [datasource-ingestion](../datasource-ingestion/proposal.md) | 知识库运营 | 五类数据源接入、增量同步、Webhook | 31 | document-indexing |
| 5 | [text-to-sql](../text-to-sql/proposal.md) | 业务开发者 | Text-to-SQL、混合检索（文档+数据） | 16 | datasource-ingestion + rag-query |
| 6 | [knowledge-graph](../knowledge-graph/proposal.md) | 业务开发者 | 四期渐进式 KG（结构/实体/全局图谱） | 30 | document-indexing + rag-query |
| 7 | [observability](../observability/proposal.md) | 运维工程师 | 链路追踪、指标告警、成本、评估、反馈回路 | 36 | 全部上层 change |
| **Total** | | | | **238** | |

> 原 monolithic change 含 191 tasks。拆分后共 238 tasks（增加了每个 change 自包含的胶水代码和独立 spec）。

## 全局用户故事地图

```
平台管理员:  US-1(创建Service) → US-2(创建KB+授权) → US-3(配额管控)
                ↓
知识库运营:  US-4(接入数据源) → US-5(同步触发) → US-6(文件夹导入)
                ↓                        US-7(同步状态查询)
                ↓                        US-8(自动索引管道)
                ↓
业务开发者:  US-9(自然语言查询+引用) → US-10(Text-to-SQL) → US-11(混合检索)
                ↓                         US-12(知识图谱)
                ↓                         US-13(自定义Prompt)
                ↓
运维工程师:  US-14(监控+成本) → US-15(评估+反馈回路)
```

## 全局架构

```
platform-foundation     — 骨架：Service / KB / 授权 / API Key / RBAC
document-indexing       — 写入：文档 → 解析 → 分块 → 向量化 → 索引
rag-query               — 读取：查询 → 意图路由 → 检索 → 生成 → 引用
datasource-ingestion    — 来源：URL / 飞书 / 语雀 / 数据库 / 文件 → 增量同步
text-to-sql             — 扩展：自然语言 → SQL → 执行 → 结果
knowledge-graph         — 扩展：实体抽取 → 图存储 → 图检索 → 图融合
observability           — 横切：追踪 / 指标 / 告警 / 成本 / 评估 / 反馈
```

## Implementation Order（全局交付节奏）

```
Phase 1 (MVP RAG):         platform-foundation → document-indexing → rag-query
Phase 2 (Multi-Source):    datasource-ingestion
Phase 3 (Advanced Query):  text-to-sql
Phase 4 (Knowledge Graph): knowledge-graph (分四期)
Phase 5 (Production):      observability
```

## 原设计文档

详细的技术设计（CanonicalDocument、ContentTree、ISourceConnector、Pipeline Config、DAG Engine 等）保留在 [design.md](design.md)。
