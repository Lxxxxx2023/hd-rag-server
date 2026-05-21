# Spec: Platform Architecture

## Overview

定义 RAG 中台的核心资源模型和多租户架构。

## Requirements

### Service 管理

- **REQ-PA-001**: Service 是独立的租户单元，拥有独立的配置空间
- **REQ-PA-002**: Service 之间逻辑隔离，数据库层面通过 service_id 区分
- **REQ-PA-003**: 每个 Service 可独立配置：chunk 策略、embedding 模型、Pipeline Config（检索 + 生成 + KG）、LLM 模型
- **REQ-PA-004**: Service 支持 CRUD 操作，删除时级联清理关联的 chunks、vectors 和图数据

### KnowledgeBase 管理

- **REQ-PA-005**: KB 是独立的文档管理单元，独立于 Service 存在
- **REQ-PA-006**: KB 支持 CRUD 操作和文档批量管理
- **REQ-PA-007**: KB 删除时需检查是否存在 Service 授权关联，有则阻止并提示
- **REQ-PA-008**: 一个 KB 可拥有多个 DataSource（DataSource 详见 datasource-ingestion change）

### Service-KB 授权

- **REQ-PA-010**: KB 通过 N:M 授权关系关联到 Service
- **REQ-PA-011**: 授权类型：read（仅检索） / write（可写入文档）
- **REQ-PA-012**: 授权操作需记录审计日志（granted_at, granted_by）
- **REQ-PA-013**: 授权生效时触发 Service 级文档处理 Pipeline
- **REQ-PA-014**: 撤销授权时可选清理或保留该 Service 侧已处理的 chunks

### 隔离模型

- **REQ-PA-020**: raw_documents 在 KB 级别共享，是所有授权 Service 的公共只读源
- **REQ-PA-021**: service_chunks 和 service_vectors 在 Service 级别独立存储

### 配额与限流

- **REQ-PA-030**: 平台支持按 Service 设置请求配额（QPS / 日请求量）
- **REQ-PA-031**: 支持按 Service 设置存储配额（文档数 / 向量数 / 总大小）
