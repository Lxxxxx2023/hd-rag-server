# Spec: Storage & Infrastructure

## Overview

定义 RAG 中台的数据存储模型、检索引擎选型、消息队列和缓存策略。

## Requirements

### 数据库 (PostgreSQL)

- **REQ-INF-001**: PostgreSQL 存储所有关系数据：services, knowledge_bases, documents, service_kb_grants, api_keys, chunks, audit_logs
- **REQ-INF-002**: Service 配置和 DAG 配置使用 JSONB 类型存储，灵活扩展
- **REQ-INF-003**: 审计日志按月分表，自动归档

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

### 混合检索

- **REQ-INF-014**: 混合检索时 pgvector 和 ES 并行查询，由融合算子合并排序
- **REQ-INF-015**: 各引擎返回结果均附带原始得分（vector 距离 / BM25 分数），供融合算子使用

### 消息队列 (Kafka)

- **REQ-INF-016**: 使用 Kafka 作为文档处理异步任务队列
- **REQ-INF-017**: Topic 设计：
  - `doc.imported` — 文档上传完成，触发解析
  - `doc.parsed` — 解析完成，触发分块
  - `doc.chunked` — 分块完成，触发向量化
  - `doc.failed` — 处理失败，触发重试/告警
- **REQ-INF-018**: 消费者支持水平扩展，按 partition 并行处理
- **REQ-INF-019**: 失败重试使用死信队列，最多重试 3 次

### 缓存 (Redis)

- **REQ-INF-020**: Service 配置缓存 — TTL 10 min
- **REQ-INF-021**: Prompt 模板缓存 — TTL 30 min
- **REQ-INF-022**: Embedding 结果缓存 — 按文本 hash 永久缓存
- **REQ-INF-023**: API Key → Service 映射缓存 — TTL 5 min
- **REQ-INF-024**: Service-KB 授权关系缓存 — TTL 5 min

### 存储配额

- **REQ-INF-025**: Service 级存储配额：文档总数、向量总数、存储总大小
- **REQ-INF-026**: 超配额时拒绝写入，返回明确提示
