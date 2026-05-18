# Proposal: RAG Platform Core

## Summary

设计并实现 RAG 中台服务的核心架构。该平台作为共享服务层，为多业务线提供统一的 RAG 能力，包括知识库管理、文档处理、检索与生成。

## Motivation

当前各业务线独立构建 RAG 能力，导致重复建设、数据孤岛、缺乏统一的质量标准和成本管控。RAG 中台将 RAG 能力抽象为共享服务，降低各业务线的接入成本，提供统一的观测和治理能力。

## Core Design Decisions

| # | 决策 | 说明 |
|---|------|------|
| 1 | Service 为独立租户单元 | 逻辑隔离，独立配置模型/chunk/检索/生成 |
| 2 | KB 独立于 Service | 通过 N:M 授权关联，KB 可复用 |
| 3 | Service 独立存储 chunk + embedding | 授权触发 Service 级 Pipeline |
| 4 | 统一 RAGGraph（DAG 可编排） | 检索 + 生成在同一 DAG，分期实现 |
| 5 | 算子自描述 Schema | 类型安全，编排前可校验 |
| 6 | 文档保留原始结构 | ContentTree 统一中间格式，Markdown 是渲染结果，分块策略操作 ContentTree |
| 7 | 多源数据统一摄入 | 通过 IParserStrategy + ParserRegistry 支持 PDF/HTML/Markdown/DOCX/TXT，后期扩展 Excel/JSON/代码/DB/图片OCR |
| 8 | Prompt 三层模板 | 平台预置 → Service 自定义 → 调用时覆盖 |
| 9 | 文件夹递归处理 | 由 Case 层 FolderImportCase 编排，递归遍历 + 类型检测 + 路由解析器 |

## Capability Domains

- **Platform Architecture**: Service/KB 资源模型、多租户、授权
- **Index Domain (写路径)**: 多源摄入 → 统一解析(CanonicalDocument/ContentTree) → 清洗 → 分块 → 向量化 → 写入索引。拥有 Documents、Chunks、Vectors。关注吞吐量和完整性。
- **Query Domain (读路径)**: 查询预处理 → 检索(混合/融合) → 重排序 → 上下文组装 → Prompt 模板 → LLM 生成 → 引用溯源 → 后处理。读取 Index 的数据。关注延迟和相关性。
- **DAG Execution Engine** (`infrastructure/dag/`): 纯技术编排组件，提供拓扑排序、并行调度、超时降级能力。不依赖任何 Domain，只依赖 types/ 中的数据结构。算子由 Index/Query Domain 定义，Case 层负责桥接

## Non-goals

- 不实现 LLM 推理引擎（对接外部 API）
- 不实现前端 UI（纯 API 服务）
- 不处理非结构化数据之外的数据源（如结构化数据库查询）
