# Design: Multi-DataSource Index & Query Redesign

## 1. 整体架构变化

### 现有架构缺口

```
现有 Index 路径（只支持文件上传）：
  文件上传 → IDocumentLoad → CanonicalDocument → Chunk → Vector + ES

现有 Query 路径（只有一条文档检索路径）：
  query_rewrite → vector_search + keyword_search → fusion → rerank → llm_generate
```

### 重新设计后的整体结构

```
                      KnowledgeBase
                           │
               ┌───────────┴──────────┐
               │                      │
          DataSource[]          ServiceKBGrant[]
          (新增)                       │
               │                      │
     ┌─────────┴──────────┐       Service
     │  ISourceConnector  │      (租户单元)
     │  URL / 飞书 / 语雀  │           │
     │  数据库 / 文件       │      ServiceChunk[]
     └─────────┬──────────┘      ServiceVector[]
               │
          ┌────┴────┐
          │文档类    │数据库类
          ▼         ▼
     Document[]  SchemaIndex[]
     ContentTree  (schema_index)
     (JSONB)

                ↑ INDEX DOMAIN
─────────────────────────────────────────────
                ↓ QUERY DOMAIN

          SearchRequest
               │
          intent_detect（强制路由器）
               │
     ┌─────────┼──────────┐
     │         │          │
DOCUMENT    DATA_QUERY   HYBRID
  Search       │          │
     │    (Text-to-SQL)  (双路并行)
     │         │          │
  Chunk[]  ResultSet    合并
     │         │          │
     └─────────┴──────────┘
               │
          llm_generate
               │
          RAGResponse
    (citations + dataResult)
```

---

## 2. Index Domain 重新设计

### 2.1 DataSource 实体

在 KnowledgeBase 和 Document 之间新增 DataSource 层，管理连接配置与同步状态。

```java
/**
 * 数据源实体。一个 KB 可拥有多个 DataSource，每个 DataSource 管理一类外部数据来源。
 * 对应 data_sources 表。
 */
public class DataSource {
    private String id;
    private String kbId;
    private String name;
    private DataSourceType type;       // URL | FEISHU | YUQUE | DATABASE | FILE

    /** 各类型连接配置，JSONB 存储，按 type 反序列化为对应 Config 对象 */
    private DataSourceConfig config;

    private SyncStrategy syncStrategy; // ONCE | SCHEDULED | WEBHOOK
    private String syncSchedule;       // cron 表达式，仅 SCHEDULED 有效
    private DataSourceStatus status;   // CONNECTED | SYNCING | ERROR
    private Instant lastSyncAt;
    private String errorMessage;
}
```

#### 各类型 Config（值对象，JSONB 存储）

```java
// URL 数据源
@Value public class UrlSourceConfig implements DataSourceConfig {
    String url;
    int depth;                   // 爬取深度，0=仅此页
    String scope;                // URL 前缀约束，防止爬出范围
    boolean renderJs;            // 是否需要 Playwright 无头浏览器渲染
    List<String> excludePatterns;// 忽略的 URL pattern
}

// 飞书数据源
@Value public class FeishuSourceConfig implements DataSourceConfig {
    String spaceId;
    @Nullable String folderToken; // null = 整个空间
    String appId;
    String appSecret;             // 存储时加密
    boolean webhookEnabled;
}

// 语雀数据源
@Value public class YuqueSourceConfig implements DataSourceConfig {
    String namespace;             // user/org slug
    @Nullable String bookSlug;   // null = 整个空间
    String token;                 // 存储时加密
    @Nullable String webhookSecret;
}

// 数据库数据源
@Value public class DatabaseSourceConfig implements DataSourceConfig {
    String connectionString;      // 存储时加密
    String username;
    String password;              // 存储时加密
    List<TableConfig> tables;
}

@Value public class TableConfig {
    String tableName;
    List<String> textColumns;     // 语义索引列，走 ContentTree → Vector
    List<String> filterColumns;   // 元数据过滤列，存为 Chunk.metadata
    List<String> ignoreColumns;
    String titleTemplate;         // 如 "{name}" 生成每行 Document 标题
    @Nullable String where;       // 过滤条件，如 "status = 'active'"
    String syncColumn;            // 增量同步依赖列，如 "updated_at"
}

// 文件上传数据源
@Value public class FileSourceConfig implements DataSourceConfig {
    String storagePrefix;         // 对象存储路径前缀
}
```

---

### 2.2 DataSourceDocument（增量同步映射表）

```java
/**
 * 外部文档 ID → 内部 Document ID 的映射。
 * 支持增量同步：通过比对 externalVersion 判断是否需要重新处理。
 * 对应 datasource_documents 表。
 */
@Value public class DataSourceDocument {
    String dataSourceId;
    String externalId;       // 飞书 pageToken / URL / 语雀 docId / DB row PK
    String documentId;       // 内部 raw_documents.id
    String externalVersion;  // 飞书 version / URL ETag / 语雀 updated_at / DB syncColumn 值
    Instant lastSyncedAt;
}
```

---

### 2.3 SchemaIndex（DATABASE 类型专属）

```java
/**
 * 数据库表结构索引。供 Text-to-SQL 时注入 Schema 上下文。
 * 对应 schema_index 表，JSON 字段存储列信息与关联关系。
 */
public class SchemaIndex {
    private String dataSourceId;
    private String tableName;
    private String tableComment;
    private List<ColumnInfo> columns;
    private List<TableRelationship> relationships;
    private List<Map<String, Object>> sampleRows; // 3-5 行样本数据
    private Instant indexedAt;
}

@Value public class ColumnInfo {
    String name;
    String type;                   // "VARCHAR(255)", "DECIMAL(10,2)" 等
    String comment;
    @Nullable List<String> enumValues; // 枚举列的所有取值
    boolean nullable;
}

@Value public class TableRelationship {
    String fk;                     // 本表外键列名
    String refTable;               // 关联表名
    String refColumn;              // 关联列名
}
```

---

### 2.4 ISourceConnector 策略接口

```java
/**
 * 数据源连接器策略接口。每种数据源类型对应一个实现。
 * 新增数据源只需实现此接口并注册到 SourceConnectorRegistry。
 */
public interface ISourceConnector {
    DataSourceType getType();

    /** 全量拉取：首次同步或手动触发全量重建 */
    List<RawSourceDocument> fetchAll(DataSource source);

    /** 增量拉取：只拉取 since 之后变更的文档 */
    List<RawSourceDocument> fetchUpdated(DataSource source, Instant since);

    /** 处理 Webhook 推送（飞书/语雀文档更新实时通知） */
    List<RawSourceDocument> handleWebhook(DataSource source, WebhookPayload payload);

    /** 连接测试：用户填写配置后验证连通性 */
    ConnectionTestResult testConnection(DataSourceConfig config);
}

/** Connector 的产出物，交给 IParserStrategy 继续处理 */
@Value public class RawSourceDocument {
    String externalId;
    String externalVersion;
    SourceType sourceType;
    String mimeType;
    byte[] rawContent;            // 原始字节，或 null（对 URL/API 类型，Connector 内部拉取）
    @Nullable String rawText;     // 纯文本内容（URL/API 返回文本时直接提供）
    Map<String, Object> metadata; // 来源特有元数据（飞书的 pageToken、URL 的 httpStatus 等）
}
```

各类型实现：

| 实现类 | 核心依赖 | 说明 |
|--------|---------|------|
| `UrlConnector` | HttpClient + Jsoup | 按 depth/scope 爬取，可选 Playwright JS 渲染 |
| `FeishuConnector` | 飞书开放平台 API | OAuth 2.0，Block 结构直接映射，支持 Webhook |
| `YuqueConnector` | 语雀 API | Personal Token，支持 Markdown 导出 |
| `DatabaseConnector` | JDBC | 按 TableConfig 拉取，textColumns 走文档路径，schema 走 SchemaIndex 路径 |
| `FileConnector` | 对象存储 SDK | 读取文件字节，交给 IParserStrategy，复用现有 Parser 实现 |

---

### 2.5 更新后的 Index 管道

#### 文档类数据源（URL / 飞书 / 语雀 / 文件）

```
DataSource
    │
    ▼
ISourceConnector.fetchAll() / fetchUpdated() / handleWebhook()
    │  产出 RawSourceDocument[]
    ▼  Kafka: datasource.fetched
IParserStrategy.parse(RawSourceDocument) → CanonicalDocument (ContentTree)
    │
    ▼  Kafka: doc.parsed
IDocumentCleaner.clean()
    │
    ▼  Kafka: doc.cleaned
IChunkingStrategy.chunk() → Chunk[]
    │
    ▼  Kafka: doc.chunked
IEmbeddingService.embed() → Vector[]
    │
    ▼  Kafka: doc.embedded
IIndexWriter.write() → pgvector + ES
    │
    ▼  Status: ready
```

#### 数据库数据源（DATABASE）分流

```
DatabaseConnector.fetch()
    │
    ├── textColumns ──────→ RawSourceDocument（每行一个）
    │                             └→ 进入上方文档管道
    │
    └── schema ────────────→ SchemaIndexWriter.write()
                                  └→ schema_index 表
                                  └→ Kafka: schema.indexed
```

---

### 2.6 新增 Kafka Topics

```
现有:  doc.imported → doc.parsed → doc.chunked → doc.embedded → doc.indexed → doc.failed

新增:
  datasource.sync.requested  — 触发数据源同步（定时 / 手动 / Webhook 入口）
  datasource.fetched         — Connector 拉取完成，触发解析管道
  schema.indexed             — SchemaIndex 写入完成
  datasource.sync.completed  — 整个 DataSource 同步完成（含统计信息）
```

---

### 2.7 新增 Index OperatorType

```
现有:  parse, clean, chunk, embed, index_write

新增:
  datasource_fetch      — ISourceConnector.fetchAll / fetchUpdated（进入 Kafka 事件链）
  datasource_sync       — 定时任务触发增量同步，发送 datasource.sync.requested 事件
  schema_index_write    — SchemaIndex 写入（仅 DATABASE 类型 DataSource 触发）
```

---

## 3. Query Domain 重新设计

### 3.1 意图路由：intent_detect 升级为强制路由器

```java
public enum QueryIntent {
    DOCUMENT_SEARCH,   // 文档语义检索，走 Path 1
    DATA_QUERY,        // 结构化数据查询，走 Path 2（Text-to-SQL）
    HYBRID             // 需要同时检索文档和查询数据，走 Path 3
}
```

`intent_detect` 不再是可选的预处理步骤，而是强制执行的第一步，其输出决定后续路径。判断逻辑：

| 特征 | 意图 |
|------|------|
| 问题含统计词（最多/最少/总计/平均/排名）| DATA_QUERY |
| 问题含时间范围 + 数值（上个月/近30天 + 金额/数量）| DATA_QUERY |
| Service 没有关联 DATABASE 类型 DataSource | 强制 DOCUMENT_SEARCH |
| 问题含"文档说""根据规定""政策是"等文档语气词 | DOCUMENT_SEARCH |
| 其余模糊情形 | HYBRID（由 Pipeline Config 的 `default` 字段兜底）|

---

### 3.2 三条执行路径

#### Path 1：文档检索（现有路径，结构不变）

```
query_rewrite（可选）
    ↓
PARALLEL:
  vector_search  (pgvector, IVectorSearchRepository)
  keyword_search (ES BM25, IChunkSearchRepository)
  metadata_filter（可选）
    ↓
fusion (RRF / linear)
    ↓
rerank（可选，cross-encoder）→ deduplicate
    ↓
context_assemble（token 预算管理 + 引用标记生成）
    ↓
prompt_render → llm_generate（SSE 流式）
    ↓
citation_extract → safety_filter
```

#### Path 2：数据查询（新增 Text-to-SQL 路径）

```
schema_retrieve
    — 从 SchemaIndex 语义检索相关表和列（topK 个 TableConfig 候选）
    — 注入：dataSourceId、候选表的 columns + relationships + sampleRows
    ↓
text_to_sql
    — 将 query + Schema 上下文送入 LLM，生成 SQL
    — 支持最多 maxRetries 次重新生成（sql_validate 失败后触发重试）
    ↓
sql_validate
    — 语法校验：解析 SQL AST，检查语法合法性
    — 安全校验：禁止 DML（INSERT/UPDATE/DELETE）、DDL（CREATE/DROP/ALTER）
    — 结果集限制：自动追加 LIMIT maxRows（若 SQL 无 LIMIT）
    ↓
sql_execute
    — 通过 JDBC 连接池执行 SQL
    — 超时控制：timeoutMs
    — 返回 ResultSet（列名 + 行数据列表）
    ↓
result_format
    — ResultSet 转为 Markdown 表格（LLM 可读格式）
    — 超出 maxCells 时截断并附注"结果已截断"
    ↓
llm_generate
    — 将格式化结果作为上下文，生成自然语言回答
```

#### Path 3：混合路径（新增）

```
PARALLEL（Path 1 和 Path 2 同时执行）:
  ├── 文档检索 → RetrievedChunk[]
  └── 数据查询 → ResultSet + SQL
    ↓
context_merge
    — 合并两路结果，标注来源（"来自文档 [ref_1]" / "来自数据库查询"）
    — 按 token 预算裁剪，优先保留 rerankScore 最高的 chunk
    ↓
llm_generate（统一 Prompt 渲染 → LLM 调用）
    ↓
citation_extract（文档引用 + 数据来源双溯源）→ safety_filter
```

---

### 3.3 更新后的 Pipeline Config 结构

```json
{
  "routing": {
    "intent_detect": { "model": "gpt-4o-mini" },
    "default": "document_search"
  },

  "document_search": {
    "preprocessing": {
      "query_rewrite": { "enabled": true, "variants": 3 },
      "hyde_generate": { "enabled": false }
    },
    "search": {
      "vector_search":  { "enabled": true, "topK": 20, "similarity": "cosine" },
      "keyword_search": { "enabled": true, "topK": 10 },
      "metadata_filter":{ "enabled": false }
    },
    "fusion":  { "strategy": "rrf", "rrf_k": 60 },
    "rerank":  { "enabled": true, "topK": 5, "model": "bge-reranker-v2" },
    "context": { "tokenBudget": 4096, "includeCitations": true },
    "generation": { "model": "gpt-4o", "temperature": 0.3, "maxTokens": 1024,
                    "fallbackModel": "gpt-4o-mini" }
  },

  "data_query": {
    "schema_retrieve": { "topK": 5 },
    "text_to_sql": {
      "model": "gpt-4o",
      "dialect": "mysql",
      "maxRetries": 2
    },
    "sql_validate": {
      "allowDML": false,
      "allowDDL": false,
      "maxRows": 1000
    },
    "sql_execute":   { "timeoutMs": 5000 },
    "result_format": { "maxCells": 500 },
    "generation":    { "model": "gpt-4o", "maxTokens": 1024 }
  },

  "postprocessing": {
    "citation_extract": { "enabled": true },
    "safety_filter":    { "enabled": true, "piiDetection": true }
  }
}
```

---

### 3.4 更新后的响应模型

```java
public class RAGResponse {
    String answer;
    Flux<String> streaming;           // SSE 模式下的 token 流

    QueryType queryType;              // 新增：DOCUMENT_SEARCH | DATA_QUERY | HYBRID

    // 文档检索结果（queryType = DOCUMENT_SEARCH 或 HYBRID 时填充）
    @Nullable Map<String, Citation> citations;

    // 数据查询结果（queryType = DATA_QUERY 或 HYBRID 时填充）
    @Nullable DataQueryResult dataResult;

    String traceId;
    ResponseMetadata metadata;        // model, latency, tokenUsage 等
}

@Value public class DataQueryResult {
    String sql;                       // 实际执行的 SQL
    String dataSourceId;
    String tableName;
    int rowCount;
    List<String> columns;
    List<Map<String, Object>> rows;   // 截断后的结果集
    boolean truncated;                // 是否因 maxCells 截断
}
```

---

### 3.5 新增 Query OperatorType

```
现有:
  query_rewrite, intent_detect, term_expand, hyde_generate
  vector_search, keyword_search, metadata_filter
  rrf_fusion, linear_fusion, rerank, deduplicate
  context_assemble, context_compress
  prompt_build, llm_generate
  citation_extract, safety_filter, format_convert

新增（Text-to-SQL 路径）:
  schema_retrieve     — 从 SchemaIndex 语义检索相关表结构
  text_to_sql         — NL → SQL 生成（注入 Schema 上下文）
  sql_validate        — SQL 语法 + 安全校验
  sql_execute         — JDBC 执行，返回 ResultSet
  result_format       — ResultSet → Markdown 表格（LLM 上下文）

新增（混合路径）:
  context_merge       — 合并文档检索 + 数据查询两路上下文
```

---

## 4. 存储模型变更

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
-- raw_documents：新增 datasource_id 外键，追踪文档来源
ALTER TABLE raw_documents ADD COLUMN datasource_id VARCHAR(36);
```

---

## 5. 新增 Management API

```
DataSource CRUD:
  POST   /api/v1/knowledge-bases/{kbId}/datasources
  GET    /api/v1/knowledge-bases/{kbId}/datasources
  PUT    /api/v1/knowledge-bases/{kbId}/datasources/{dsId}
  DELETE /api/v1/knowledge-bases/{kbId}/datasources/{dsId}

DataSource 操作:
  POST   /api/v1/knowledge-bases/{kbId}/datasources/{dsId}/test-connection
  POST   /api/v1/knowledge-bases/{kbId}/datasources/{dsId}/sync          -- 手动触发全量同步
  GET    /api/v1/knowledge-bases/{kbId}/datasources/{dsId}/sync-status   -- 查询同步进度

Webhook 接收端点:
  POST   /api/v1/webhooks/feishu/{dsId}
  POST   /api/v1/webhooks/yuque/{dsId}
```
