# Spec: API & Access Design — Runtime API

## Overview

定义 RAG 中台的 Runtime API 接口体系、SSE 事件协议、统一响应模型和 Java SDK。

## Requirements

### API 分层

- **REQ-API-001**: RAG Runtime API (REST + SSE) — 业务方调用，高频，延迟敏感
- **REQ-API-002**: 所有对外 API 统一使用 `/api/v1/` 前缀

### RAG Runtime API

- **REQ-API-010**: 纯检索接口 — `POST /api/v1/services/{serviceId}/search`
  - 执行检索 Pipeline（含 intent_detect 路由），返回 Doc[] + trace_id
  - 支持 metadata_filter 和 top_k 参数
- **REQ-API-011**: 完整 RAG 接口 — `POST /api/v1/services/{serviceId}/rag`
  - 执行完整 Pipeline（检索 + 生成），返回 RAGResponse
  - 支持历史对话传入
  - 支持 stream=true（SSE 流式）和 stream=false（一次性返回）
  - 支持 Layer 3 参数覆盖（temperature, max_tokens 等）
- **REQ-API-012**: SSE 流式输出遵循标准 SSE 协议：
  - `event: token` — 逐 token 推送
  - `event: citation` — 流结束后推送引用映射
  - `event: data_result` — 数据查询结果（DATA_QUERY / HYBRID 模式时）
  - `event: error` — 错误事件
  - `event: done` — 流结束标记

### Pipeline Config API

- **REQ-API-020**: `PUT /api/v1/services/{serviceId}/pipeline-config` — 更新 Pipeline Config（统一 4 段结构）

### RAGResponse 统一响应模型

- **REQ-API-030**: RAGResponse 包含以下字段：
  - `answer`: LLM 生成答案
  - `streaming`: SSE 模式下的 token 流
  - `queryType`: DOCUMENT_SEARCH | DATA_QUERY | HYBRID
  - `citations`: 文档引用映射
  - `dataResult`: DataQueryResult（DATA_QUERY / HYBRID 时填充）
  - `traceId`: 链路追踪 ID
  - `metadata`: ResponseMetadata（model, latency, tokenUsage 等）

### SDK

- **REQ-API-040**: 提供 Java SDK（独立 JAR），封装 HTTP Client + SSE 解析
- **REQ-API-041**: SDK 支持自动重试、超时配置、连接池管理
