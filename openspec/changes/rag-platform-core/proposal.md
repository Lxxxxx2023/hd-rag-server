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
| 6 | 文档保留原始结构 | 分块策略按文档类型自动适配 |
| 7 | 首期支持 PDF + Markdown | 后续扩展更多格式 |
| 8 | Prompt 三层模板 | 平台预置 → Service 自定义 → 调用时覆盖 |

## Capability Domains

- **Platform Architecture**: Service/KB 资源模型、多租户、授权
- **Document Processing**: 导入 → 解析 → 清洗 → 分块 → 向量化 → 索引
- **Retrieval Pipeline**: DAG 编排检索、混合检索、重排序、降级
- **Generation Pipeline**: 上下文组装、Prompt 模板、LLM 调用、引用溯源

## Non-goals

- 不实现 LLM 推理引擎（对接外部 API）
- 不实现前端 UI（纯 API 服务）
- 不处理非结构化数据之外的数据源（如结构化数据库查询）
