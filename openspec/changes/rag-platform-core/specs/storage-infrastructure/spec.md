# Spec: Storage & Infrastructure

## Overview

定义 RAG 中台的数据存储模型、检索引擎选型、消息队列和缓存策略。

## Requirements

### 数据库 (PostgreSQL)

- **REQ-INF-001**: PostgreSQL 存储所有关系数据：services, knowledge_bases, data_sources, datasource_documents, schema_index, raw_documents, service_chunks, service_kb_grants, api_keys, audit_logs
- **REQ-INF-002**: Service 配置和 Pipeline/DAG 配置使用 JSONB 类型存储，灵活扩展
- **REQ-INF-003**: 审计日志按月分表，自动归档

### 新增表

```sql
-- DataSource：数据源连接配置与同步状态
CREATE TABLE data_sources (
    id              VARCHAR(36) PRIMARY KEY,
    kb_id           VARCHAR(36) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    type            VARCHAR(32) NOT NULL,   -- URL|FEISHU|YUQUE|DATABASE|FILE
    config          JSONB NOT NULL,         -- 各类型连接配置（加密字段在应用层加解密）
    sync_strategy   VARCHAR(32) NOT NULL,   -- ONCE|SCHEDULED|WEBHOOK
    sync_schedule   VARCHAR(64),            -- cron 表达式
    status          VARCHAR(32) NOT NULL,
    last_sync_at    TIMESTAMP,
    error_message   TEXT,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);

-- DataSourceDocument：外部 ID → 内部 Document 增量同步映射
CREATE TABLE datasource_documents (
    datasource_id       VARCHAR(36) NOT NULL,
    external_id         VARCHAR(512) NOT NULL,
    document_id         VARCHAR(36) NOT NULL,
    external_version    VARCHAR(255),
    last_synced_at      TIMESTAMP NOT NULL,
    PRIMARY KEY (datasource_id, external_id)
);

-- SchemaIndex：数据库表结构（DATABASE 类型 DataSource 专属）
CREATE TABLE schema_index (
    id              VARCHAR(36) PRIMARY KEY,
    datasource_id   VARCHAR(36) NOT NULL,
    table_name      VARCHAR(255) NOT NULL,
    table_comment   TEXT,
    columns         JSONB NOT NULL,         -- List<ColumnInfo>
    relationships   JSONB,                  -- List<TableRelationship>
    sample_rows     JSONB,                  -- List<Map>
    indexed_at      TIMESTAMP NOT NULL,
    UNIQUE (datasource_id, table_name)
);
```

### 现有表变更

```sql
ALTER TABLE raw_documents ADD COLUMN datasource_id VARCHAR(36);
```

### 向量检索 (pgvector)

- **REQ-INF-004**: pgvector 作为主向量检索引擎，负责语义相似度检索
- **REQ-INF-005**: 向量维度支持 1536 (text-embedding-3-small) 和 3072 (text-embedding-3-large)
- **REQ-INF-006**: 向量索引使用 IVFFlat，后期按需切换 HNSW
- **REQ-INF-007**: 支持多 embedding 模型共存（按 Service 配置选择）
- **REQ-INF-008**: pgvector 查询参数（probes, lists）可按 Service 级别微调
- **REQ-INF-009**: 后期可升级到 Milvus，接口抽象层预留切换能力

### 全文检索 (Elasticsearch)

- **REQ-INF-010**: ES 只负责 BM25 关键词检索，不承担向量检索职责
- **REQ-INF-011**: ES 索引按 Service 物理隔离，命名格式 `idx_{service_id}_chunks`
- **REQ-INF-012**: 中文分词使用 ik_max_word
- **REQ-INF-013**: ES 存储字段：chunk_id, doc_id, kb_id, content (text), title, metadata (object)

### SchemaIndex 向量存储

- **REQ-INF-014**: SchemaIndex 的 tableComment + column 描述向量化后存入 pgvector，与 Chunk 向量共用同一 pgvector 实例但通过 service_id 前缀隔离（如 `schema_{serviceId}`）
- **REQ-INF-015**: schema_retrieve 调用 ISchemaIndexRepository.searchByQuery() 时走 pgvector 语义检索

### 混合检索

- **REQ-INF-016**: 混合检索时 pgvector 和 ES 并行查询，由融合算子合并排序
- **REQ-INF-017**: 各引擎返回结果均附带原始得分（vector 距离 / BM25 分数），供融合算子使用

### 消息队列 (Kafka)

- **REQ-INF-018**: 使用 Kafka 作为文档处理异步任务队列
- **REQ-INF-019**: Topic 全集：
  - `doc.imported` — 文档上传完成，触发解析
  - `doc.parsed` — 解析完成，触发分块
  - `doc.chunked` — 分块完成，触发向量化
  - `doc.embedded` — 向量化完成，触发索引写入
  - `doc.indexed` — 索引写入完成
  - `doc.failed` — 处理失败，触发重试/告警
  - `datasource.sync.requested` — 触发数据源同步
  - `datasource.fetched` — Connector 拉取完成，触发解析管道
  - `schema.indexed` — SchemaIndex 写入完成
  - `datasource.sync.completed` — DataSource 同步完成（含统计信息）
- **REQ-INF-020**: 消费者支持水平扩展，按 partition 并行处理
- **REQ-INF-021**: 失败重试使用死信队列，最多重试 3 次

### 缓存 (Redis)

- **REQ-INF-022**: Service 配置缓存 — TTL 10 min
- **REQ-INF-023**: Prompt 模板缓存 — TTL 30 min
- **REQ-INF-024**: Embedding 结果缓存 — 按文本 hash 永久缓存
- **REQ-INF-025**: API Key → Service 映射缓存 — TTL 5 min
- **REQ-INF-026**: Service-KB 授权关系缓存 — TTL 5 min

### 存储配额

- **REQ-INF-027**: Service 级存储配额：文档总数、向量总数、存储总大小
- **REQ-INF-028**: 超配额时拒绝写入，返回明确提示
