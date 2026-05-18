# Spec: API & Access Design

## Overview

定义 RAG 中台对外暴露的 API 接口体系、鉴权模型和接入方式。

## Requirements

### API 分层

- **REQ-API-001**: API 分为三条线：
  - Management API (RESTful) — 平台管理配置，低频操作
  - RAG Runtime API (REST + SSE) — 业务方调用，高频，延迟敏感
  - Internal API (gRPC) — 服务内部模块间调用
- **REQ-API-002**: Management API 和 Runtime API 统一使用 `/api/v1/` 前缀

### Management API

- **REQ-API-003**: Service CRUD — `POST/GET/PUT/DELETE /api/v1/services`
- **REQ-API-004**: KB CRUD — `POST/GET/PUT/DELETE /api/v1/knowledge-bases`
- **REQ-API-005**: Document 管理 — `POST/GET/DELETE /api/v1/knowledge-bases/{kbId}/documents`
- **REQ-API-006**: Grant 管理 — `POST/DELETE /api/v1/knowledge-bases/{kbId}/grants`
- **REQ-API-007**: DAG 配置 — `PUT /api/v1/services/{serviceId}/retrieval-graph`, `PUT .../generation-graph`
- **REQ-API-008**: 查询 Service 已授权的 KB 列表 — `GET /api/v1/services/{serviceId}/knowledge-bases`

### RAG Runtime API

- **REQ-API-009**: 纯检索接口 — `POST /api/v1/services/{serviceId}/search`
  - 执行检索 DAG，返回 Doc[] + trace_id
  - 支持 metadata_filter 和 top_k 参数
- **REQ-API-010**: 完整 RAG 接口 — `POST /api/v1/services/{serviceId}/rag`
  - 执行完整 RAGGraph（检索 + 生成）
  - 支持历史对话传入
  - 支持 stream=true（SSE 流式）和 stream=false（一次性返回）
  - 支持 Layer 3 参数覆盖（temperature, max_tokens 等）
- **REQ-API-011**: SSE 流式输出遵循标准 SSE 协议：
  - `event: token` — 逐 token 推送
  - `event: citation` — 流结束后推送引用映射
  - `event: error` — 错误事件
  - `event: done` — 流结束标记

### 鉴权

- **REQ-API-012**: 平台级 API Key (`pk_` 前缀) — 全局管理权限
- **REQ-API-013**: Service 级 API Key (`sk_` 前缀) — 绑定到特定 Service，仅能调用该 Service 的 Runtime API
- **REQ-API-014**: 内部调用 Token — 服务间 gRPC 互信
- **REQ-API-015**: Service 级 API Key 自动纳入该 Service 的配额管理

### SDK

- **REQ-API-016**: 提供 Java SDK（独立 JAR），封装 HTTP Client + SSE 解析
- **REQ-API-017**: SDK 支持自动重试、超时配置、连接池管理

### 技术栈

- **REQ-API-018**: Java 实现，Web 框架 Spring Boot 3 + WebFlux
- **REQ-API-019**: 向量存储 ES + pgvector，后期可升级到 Milvus
- **REQ-API-020**: DAG 执行引擎完全自研
- **REQ-API-021**: 消息队列用于文档处理异步任务（Kafka / 本地异步可降级）
