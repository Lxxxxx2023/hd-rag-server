# Proposal: RAG Platform Core

## Summary

设计并实现 RAG 中台服务的核心架构。该平台作为共享服务层，为多业务线提供统一的 RAG 能力，包括：多源数据接入与增量同步、文档处理与向量索引、混合检索与 Text-to-SQL 结构化查询、知识图谱检索，以及 LLM 生成与引用溯源。

## Motivation

当前各业务线独立构建 RAG 能力，导致重复建设、数据孤岛、缺乏统一的质量标准和成本管控。RAG 中台将 RAG 能力抽象为共享服务，降低各业务线的接入成本，提供统一的观测和治理能力。

同时，RAG 的检索质量受限于"语义相似度 → Top K"的单一范型，无法处理结构化关系理解、多跳推理或结构化数据查询场景。通过引入多数据源接入（URL/飞书/语雀/数据库/文件）、Text-to-SQL 路径和知识图谱检索，补全 RAG 平台的能力矩阵。

## Core Design Decisions

| # | 决策 | 说明 |
|---|------|------|
| 1 | Service 为独立租户单元 | 逻辑隔离，独立配置模型/chunk/检索/生成 |
| 2 | KB 独立于 Service | 通过 N:M 授权关联，KB 可复用 |
| 3 | Service 独立存储 chunk + embedding | 授权触发 Service 级 Pipeline |
| 4 | 统一 RAGGraph（DAG 可编排） | 检索 + 生成在同一 DAG，分期实现 |
| 5 | 算子自描述 Schema | 类型安全，编排前可校验 |
| 6 | 文档保留原始结构 | ContentTree 统一中间格式，Markdown 是渲染结果，分块策略操作 ContentTree |
| 7 | DataSource 连接层 | 在 KB 和 Document 之间增加连接配置与同步状态管理，ISourceConnector 策略接口统一 URL/飞书/语雀/数据库/文件五类数据源的接入与增量同步 |
| 8 | DATABASE 源双轨处理 | 文本列走 ContentTree → Chunk → Vector 路径；Schema 信息单独写入 SchemaIndex 供 Text-to-SQL 使用 |
| 9 | intent_detect 升级为强制路由器 | 输出 DOCUMENT_SEARCH / DATA_QUERY / HYBRID 三种意图，决定后续执行路径 |
| 10 | Text-to-SQL 独立 Query 路径 | schema_retrieve → text_to_sql → sql_validate → sql_execute → result_format，与文档检索路径解耦 |
| 11 | KG 作为 Index/Query Domain 的延伸 | 不新建独立 Domain，策略接口分别定义在 domain/index/ 和 domain/query/，四期渐进式演进（架构预留 → 结构图谱 → 实体图谱 → 全局图谱） |
| 12 | Prompt 三层模板 | 平台预置 → Service 自定义 → 调用时覆盖 |
| 13 | 文件夹递归处理 | 由 Case 层 FolderImportCase 编排，递归遍历 + 类型检测 + 路由解析器 |

## Capability Domains

- **Platform Architecture**: Service/KB 资源模型、多租户、授权、配额
- **DataSource Management（Index 侧）**: DataSource CRUD、连接测试、手动/定时/Webhook 三种同步触发、DataSourceDocument 增量同步映射
- **Source Connectors（Index 侧）**: UrlConnector、FeishuConnector、YuqueConnector、DatabaseConnector（JDBC 双轨）、FileConnector（复用现有 Parser）
- **Schema Indexing（Index 侧）**: DATABASE 类型专属，索引表结构/列注释/枚举值/关联关系/样本行，供 Text-to-SQL 使用
- **Index Domain（写路径）**: 多源摄入 → 统一解析(CanonicalDocument/ContentTree) → 清洗 → 分块 → 向量化 → 写入索引。拥有 Documents、Chunks、Vectors。关注吞吐量和完整性。
- **Query Domain（读路径）**: 意图路由 → 检索(混合/融合/图扩展) → 重排序 → 上下文组装 → Prompt 模板 → LLM 生成 → 引用溯源 → 后处理。关注延迟和相关性。
- **Text-to-SQL Path（Query 侧）**: schema_retrieve → text_to_sql → sql_validate → sql_execute → result_format，完整的数据查询执行链路
- **Hybrid Path（Query 侧）**: 文档检索与数据查询并行执行，context_merge 合并两路结果
- **Knowledge Graph（Index + Query 侧）**: 实体/关系抽取、图索引写入、实体链接、图遍历检索、图向量融合、社区发现与全局摘要
- **DAG Execution Engine** (`infrastructure/dag/`): 纯技术编排组件，提供拓扑排序、并行调度、超时降级能力。不依赖任何 Domain，只依赖 types/ 中的数据结构。
- **Observability**: 链路追踪、指标监控、告警、成本统计、离线评估、线上采样、用户反馈回路

## Non-goals

- 不实现 LLM 推理引擎（对接外部 API）
- 不实现前端 UI（纯 API 服务）
- 不实现多模态图片 Embedding（IMAGE 节点保留 OCR 文本）
- 不实现对话类数据源（客服记录、工单）的结构化建模
- 不实现 Connector 的配额/限流管理（依赖现有 Service 级配额体系）
- 不修改 ContentTree 节点类型（CALLOUT 节点映射到 QUOTE）
- 不支持 DML/DDL SQL 执行，sql_validate 强制只读校验
- 不绑定特定图数据库（通过 GraphRepository 接口抽象）
- 不强制所有 Service 使用 KG（可选开启）
