# Spec: API & Access Design

## Overview

定义 RAG 中台对外暴露的 API 接口体系、鉴权模型和接入方式。

## Requirements

### API 分层

- **REQ-API-001**: API 分为四条线：
  - Management API (RESTful) — 平台管理配置，低频操作
  - DataSource API (RESTful) — 数据源管理与同步
  - RAG Runtime API (REST + SSE) — 业务方调用，高频，延迟敏感
  - Internal API (gRPC) — 服务内部模块间调用
- **REQ-API-002**: 所有对外 API 统一使用 `/api/v1/` 前缀

### Management API

- **REQ-API-003**: Service CRUD — `POST/GET/PUT/DELETE /api/v1/services`
- **REQ-API-004**: KB CRUD — `POST/GET/PUT/DELETE /api/v1/knowledge-bases`
- **REQ-API-005**: Document 管理 — `POST/GET/DELETE /api/v1/knowledge-bases/{kbId}/documents`
- **REQ-API-006**: Grant 管理 — `POST/DELETE /api/v1/knowledge-bases/{kbId}/grants`
- **REQ-API-007**: Pipeline Config — `PUT /api/v1/services/{serviceId}/pipeline-config`（统一 4 段结构：routing + document_search + data_query + postprocessing）
- **REQ-API-008**: 查询 Service 已授权的 KB 列表 — `GET /api/v1/services/{serviceId}/knowledge-bases`

### DataSource Management API

- **REQ-API-010**: DataSource CRUD — `POST/GET/PUT/DELETE /api/v1/knowledge-bases/{kbId}/datasources`
- **REQ-API-011**: 连接测试 — `POST /api/v1/knowledge-bases/{kbId}/datasources/{dsId}/test-connection`，调用 Connector.testConnection()
- **REQ-API-012**: 手动触发同步 — `POST /api/v1/knowledge-bases/{kbId}/datasources/{dsId}/sync`（触发全量同步）
- **REQ-API-013**: 同步状态查询 — `GET /api/v1/knowledge-bases/{kbId}/datasources/{dsId}/sync-status`（返回 status / lastSyncAt / 统计数据）
- **REQ-API-014**: Webhook 接收端点 — `POST /api/v1/webhooks/feishu/{dsId}` 和 `POST /api/v1/webhooks/yuque/{dsId}`，验签后调用对应 Connector.handleWebhook()

### RAG Runtime API

- **REQ-API-020**: 纯检索接口 — `POST /api/v1/services/{serviceId}/search`
  - 执行检索 Pipeline（含 intent_detect 路由），返回 Doc[] + trace_id
  - 支持 metadata_filter 和 top_k 参数
- **REQ-API-021**: 完整 RAG 接口 — `POST /api/v1/services/{serviceId}/rag`
  - 执行完整 Pipeline（检索 + 生成），返回 RAGResponse
  - 支持历史对话传入
  - 支持 stream=true（SSE 流式）和 stream=false（一次性返回）
  - 支持 Layer 3 参数覆盖（temperature, max_tokens 等）
- **REQ-API-022**: SSE 流式输出遵循标准 SSE 协议：
  - `event: token` — 逐 token 推送
  - `event: citation` — 流结束后推送引用映射
  - `event: data_result` — 数据查询结果（DATA_QUERY / HYBRID 模式）
  - `event: error` — 错误事件
  - `event: done` — 流结束标记

### RAGResponse 统一响应模型

- **REQ-API-030**: RAGResponse 包含以下字段：
  - `answer`: LLM 生成答案
  - `streaming`: SSE 模式下的 token 流
  - `queryType`: DOCUMENT_SEARCH | DATA_QUERY | HYBRID（区分响应类型）
  - `citations`: 文档引用映射（queryType 为 DOCUMENT_SEARCH 或 HYBRID 时填充）
  - `dataResult`: DataQueryResult — 含 sql / dataSourceId / tableName / rowCount / columns / rows / truncated（queryType 为 DATA_QUERY 或 HYBRID 时填充）
  - `traceId`: 链路追踪 ID
  - `metadata`: ResponseMetadata（model, latency, tokenUsage 等）

### 鉴权

- **REQ-API-040**: 平台级 API Key (`pk_` 前缀) — 全局管理权限，绑定 RBAC 角色
- **REQ-API-041**: Service 级 API Key (`sk_` 前缀) — 绑定到特定 Service，仅能调用该 Service 的 Runtime API
- **REQ-API-042**: 内部调用 Token — 服务间 gRPC 互信，不暴露给外部
- **REQ-API-043**: Service 级 API Key 自动纳入该 Service 的配额管理

### SDK

- **REQ-API-050**: 提供 Java SDK（独立 JAR），封装 HTTP Client + SSE 解析
- **REQ-API-051**: SDK 支持自动重试、超时配置、连接池管理

### 技术栈

- **REQ-API-060**: Java 实现，Web 框架 Spring Boot 3 + WebFlux
- **REQ-API-061**: 向量存储 ES + pgvector，后期可升级到 Milvus
- **REQ-API-062**: DAG 执行引擎完全自研
- **REQ-API-063**: 消息队列 Kafka 用于文档处理异步任务
- **REQ-API-064**: 图数据库 Neo4j（Phase 3 引入）
