# Spec: Storage & Infrastructure (Core Tables)

## Overview

定义 RAG 中台的核心数据库表结构和缓存策略。存储引擎选型（pgvector / ES / Kafka / Redis）的完整配置见 document-indexing 和 rag-query 的对应 spec。

## Requirements

### 数据库 (PostgreSQL)

- **REQ-INF-001**: PostgreSQL 存储所有关系数据：services, knowledge_bases, service_kb_grants, api_keys, audit_logs
- **REQ-INF-002**: Service 配置使用 JSONB 类型存储，灵活扩展
- **REQ-INF-003**: 审计日志按月分表，自动归档

### 核心表 DDL

```sql
CREATE TABLE services (
    id          VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    config      JSONB NOT NULL,    -- chunk/embedding/LLM 配置
    quota       JSONB,             -- QPS/日请求量/存储配额
    status      VARCHAR(32) NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);

CREATE TABLE knowledge_bases (
    id          VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    config      JSONB,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);

CREATE TABLE service_kb_grants (
    service_id      VARCHAR(36) NOT NULL,
    kb_id           VARCHAR(36) NOT NULL,
    grant_type      VARCHAR(16) NOT NULL,  -- read / write
    granted_by      VARCHAR(255),
    granted_at      TIMESTAMP NOT NULL,
    PRIMARY KEY (service_id, kb_id)
);

CREATE TABLE api_keys (
    id          VARCHAR(36) PRIMARY KEY,
    key_hash    VARCHAR(255) NOT NULL UNIQUE,
    key_prefix  VARCHAR(12) NOT NULL,       -- pk_ 或 sk_ + 前4位
    key_suffix  VARCHAR(4) NOT NULL,        -- 末4位，用于展示
    type        VARCHAR(16) NOT NULL,       -- platform / service
    service_id  VARCHAR(36),                -- 仅 sk_ 类型
    role        VARCHAR(32),                -- 仅 pk_ 类型：admin/operator/viewer
    expires_at  TIMESTAMP,
    disabled    BOOLEAN DEFAULT FALSE,
    created_by  VARCHAR(255),
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);

CREATE TABLE audit_logs_202605 (
    id          VARCHAR(36) PRIMARY KEY,
    operator    VARCHAR(255),
    action      VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64),
    resource_id VARCHAR(36),
    source_ip   VARCHAR(45),
    result      VARCHAR(16),         -- success / failure
    detail      JSONB,
    created_at  TIMESTAMP NOT NULL
);
```

### 缓存 (Redis)

- **REQ-INF-010**: Service 配置缓存 — TTL 10 min
- **REQ-INF-011**: API Key → Service 映射缓存 — TTL 5 min
- **REQ-INF-012**: Service-KB 授权关系缓存 — TTL 5 min
