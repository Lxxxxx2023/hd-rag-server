# Design: RAG Platform Core

## Architecture Overview

### Two Core Domains: Index & Query

RAG 系统的自然边界是**写路径（Index）**和**读路径（Query）**。DAG 编排引擎是 Infrastructure 层的技术组件。

```
┌──────────────────────────────────────────────────────────────────────┐
│                          RAG Platform                                 │
│                                                                      │
│  ┌─────────────────────────────┐  ┌─────────────────────────────┐    │
│  │   Index Domain (写路径)      │  │   Query Domain (读路径)      │    │
│  │                             │  │                             │    │
│  │  DataSource 接入 → 解析      │  │  意图路由 → 检索 → 重排序     │    │
│  │       ↓                    │  │       ↓                    │    │
│  │  分块 → 向量化 → 写入索引    │  │  上下文组装 → LLM 生成       │    │
│  │       ↓                    │  │       ↓                    │    │
│  │  实体/关系抽取 → 图索引      │  │  图扩展/实体链接/图遍历       │    │
│  │                             │  │                             │    │
│  │  拥有: Documents, Chunks,    │  │  读取: Chunks, Vectors,      │    │
│  │        Vectors, GraphIndex   │  │        SchemaIndex, Graph    │    │
│  └──────────────┬──────────────┘  └──────────────┬──────────────┘    │
│                 │                                │                   │
│                 └────────────┬───────────────────┘                   │
│                              │                                       │
│                    ┌─────────┴─────────┐                             │
│                    │    Case 层 (编排)  │                             │
│                    └─────────┬─────────┘                             │
│                              │                                       │
│  ┌───────────────────────────┴───────────────────────────────────┐  │
│  │                   Infrastructure 层                            │  │
│  │  dag/ | dao/ | gateway/ | redis/ | graph/                     │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                      types/  (共享类型)                        │  │
│  └───────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
```

### 分层依赖方向

```
types/  ←  domain/index/  ←  case/
types/  ←  domain/query/  ←  case/
types/  ←  infrastructure/dag/  ←  case/
types/  ←  infrastructure/graph/  ←  case/

case/  组装 infra/ 的执行器 + domain/ 的算子 Bean
domain/ 不依赖 infrastructure/（Domain 只定义策略接口）
infrastructure/ 不依赖 domain/（纯技术组件，通过 types/ 获取配置）
```

### 各层职责

| 层 | 职责 |
|---|------|
| `types/` | `GraphDefinition`, `NodeConfig`, `EdgeDefinition`, `OperatorType`, `Entity`, `Relation`, `GraphQuery` 等纯数据结构 |
| `domain/index/` | 写路径策略接口：`IParserStrategy`、`IChunkingStrategy`、`ISourceConnector`、`IEntityExtractor`、`IRelationExtractor` |
| `domain/query/` | 读路径策略接口：`IRetriever`、`IRerankOp`、`IGraphExpander`、`IGraphRetriever`、`IEntityLinker` |
| `infrastructure/dag/` | `GraphExecutor`, `TopologicalSorter`, `NodeScheduler` — 纯技术编排 |
| `infrastructure/graph/` | `GraphRepository` 抽象接口（Phase 3 Neo4j 实现） |
| `case/` | 跨域编排：从配置组装 DAG，桥接 domain 算子和 infra 执行器 |

---

## Data Parser Architecture

### Canonical Document Model

所有数据源经过解析后产出一致的 `CanonicalDocument` 格式。核心原则：**Markdown 是渲染结果（给 LLM 读的），ContentTree 是结构中间层（给分块策略决策用的）**。

#### DDD 模型分类

| 类 | DDD 类型 | 说明 |
|---|---------|------|
| `CanonicalDocument` | Aggregate Root | 解析器产出的统一文档模型，拥有 ContentTree 的整体生命周期 |
| `ContentNode` | Entity | 树形结构节点，有 nodeId 标识 |
| `DocumentMetadata` | Value Object | 不可变，源元数据 |
| `SourcePosition` | Value Object | 不可变，溯源定位信息 |
| `DocumentStatistics` | Value Object | 不可变，解析后统计 |
| `SourceType` | Enum VO | WEB, FILE, DATABASE, API, CRAWL |
| `NodeType` | Enum VO | ROOT, SECTION, PARAGRAPH, TABLE, LIST_ITEM, CODE, IMAGE, QUOTE |

#### ContentNode 结构

```java
public class ContentNode {
    private String nodeId;           // 节点唯一标识
    private NodeType type;           // 节点类型
    @Nullable private String heading;     // 仅 SECTION 节点
    private int headingLevel;             // 0=根, 1=h1, ..., 6=h6
    @Nullable private String markdown;    // 预渲染 Markdown（叶子节点）
    @Nullable private String plainText;   // 纯文本（叶子节点）
    @Nullable private SourcePosition position;
    private Map<String, Object> metadata; // 节点级元数据
    private List<ContentNode> children;
}
```

#### SourcePosition（溯源定位）

```java
@Value public class SourcePosition {
    @Nullable Integer pageNumber;    // PDF 页码
    @Nullable Integer lineStart;     // 源文件行号
    @Nullable Integer lineEnd;
    @Nullable String sourcePointer;  // 通用溯源指针: "L120-L145" / "Sheet1!A1:D10" / "example.py:42-68"
}
```

#### 不同数据源 → ContentTree 映射

| 来源 | 解析器 | 映射规则 | sourcePointer 示例 |
|------|--------|---------|-------------------|
| PDF | PdfParser (PDFBox) | 标题样式 → SECTION，内文 → PARAGRAPH | `"page:15"` |
| HTML | HtmlParser (Jsoup) | `<h1>-<h6>` → SECTION，`<nav>/<footer>` → 噪音标记 | `"div#main-content"` |
| Markdown | MarkdownParser (flexmark) | `##` → SECTION，` ``` ` → CODE | `"L120-L145"` |
| DOCX | DocxParser (POI) | 样式 "Heading 1" → SECTION | `"page:8"` |
| TXT | PlainTextParser | 按段落/空行分块（fallback） | `"L1-L50"` |

### Parser Strategy Architecture

```java
interface IParserStrategy {
    boolean canHandle(SourceType sourceType, String mimeType);
    CanonicalDocument parse(SourceInput input);
    float probe(SourceInput input);  // 内容自动检测，返回置信度
}
```

ParserRegistry 路由顺序：精确 mimeType 匹配 → SourceType 兜底 → probe() 自动检测。

---

## DataSource & Multi-Source Ingestion

### DataSource 实体

在 KnowledgeBase 和 Document 之间新增 DataSource 层，管理连接配置与同步状态。一个 KB 可拥有多个 DataSource。

```java
public class DataSource {
    private String id;
    private String kbId;
    private String name;
    private DataSourceType type;       // URL | FEISHU | YUQUE | DATABASE | FILE
    private DataSourceConfig config;   // JSONB，按 type 反序列化
    private SyncStrategy syncStrategy; // ONCE | SCHEDULED | WEBHOOK
    private String syncSchedule;       // cron 表达式，仅 SCHEDULED 有效
    private DataSourceStatus status;   // CONNECTED | SYNCING | ERROR
    private Instant lastSyncAt;
    private String errorMessage;
}
```

#### DataSourceType 枚举

| 类型 | 说明 | 与 SourceType 的关系 |
|------|------|---------------------|
| `URL` | 网页抓取 | 对应 SourceType.WEB |
| `FEISHU` | 飞书文档 | 对应 SourceType.API |
| `YUQUE` | 语雀文档 | 对应 SourceType.API |
| `DATABASE` | 数据库表 | 对应 SourceType.DATABASE |
| `FILE` | 文件上传 | 对应 SourceType.FILE |

> **DataSourceType ≠ SourceType**：DataSourceType 描述连接器类型（如何获取数据），SourceType 描述单个文档的来源（文档从哪来）。一个 DataSource 产出的 Document 会带有对应的 SourceType 标记。

#### 各类型 Config（值对象，JSONB 存储）

```java
// URL 数据源
@Value public class UrlSourceConfig implements DataSourceConfig {
    String url; int depth; String scope; boolean renderJs;
    List<String> excludePatterns;
}
// 飞书数据源
@Value public class FeishuSourceConfig implements DataSourceConfig {
    String spaceId; @Nullable String folderToken;
    String appId; String appSecret;  // 存储时加密
    boolean webhookEnabled;
}
// 语雀数据源
@Value public class YuqueSourceConfig implements DataSourceConfig {
    String namespace; @Nullable String bookSlug;
    String token;  // 存储时加密
    @Nullable String webhookSecret;
}
// 数据库数据源
@Value public class DatabaseSourceConfig implements DataSourceConfig {
    String connectionString;  // 存储时加密
    String username; String password;  // 存储时加密
    List<TableConfig> tables;
}
@Value public class TableConfig {
    String tableName;
    List<String> textColumns;     // 语义索引列 → ContentTree → Vector
    List<String> filterColumns;   // 元数据过滤列 → Chunk.metadata
    List<String> ignoreColumns;
    String titleTemplate;         // 如 "{name}" 生成每行 Document 标题
    @Nullable String where;       // 如 "status = 'active'"
    String syncColumn;            // 增量同步依赖列
}
// 文件上传数据源
@Value public class FileSourceConfig implements DataSourceConfig {
    String storagePrefix;
}
```

### ISourceConnector 策略接口

Connector 不一次性返回全量数据，改用**游标/分页模式**。先拉取轻量级元数据（externalId + version），比对增量后再按需拉取单篇内容。避免大数据源（如飞书 5000 篇、DB 百万行）撑爆内存。

```java
public interface ISourceConnector {
    DataSourceType getType();

    // 分页拉取元数据（只含 externalId + version + title，体积极小）
    @Value public class FetchResult {
        List<DocumentMeta> documents;
        @Nullable String nextCursor;   // 飞书 page_token / DB offset / URL depth
        boolean hasMore;
    }
    @Value public class DocumentMeta {
        String externalId;
        String externalVersion;
        @Nullable String title;
        @Nullable Long contentLength;
    }

    FetchResult fetchMetadata(DataSource source, @Nullable String cursor);

    // 按需拉取单篇内容，只有确认需要处理（版本变化）才调用
    RawSourceDocument fetchContent(DataSource source, String externalId);

    // Webhook 推送：传入 payload 和回调，Connector 按需调用回调拉取
    void handleWebhook(DataSource source, WebhookPayload payload,
                       Consumer<RawSourceDocument> handler);

    ConnectionTestResult testConnection(DataSourceConfig config);
}

@Value public class RawSourceDocument {
    String externalId;
    String externalVersion;
    SourceType sourceType;
    @Nullable String mimeType;
    byte[] rawContent;
    Map<String, Object> metadata;
}
```

**Connector 不负责 judge 哪些文档需要处理** — 版本比对由 Case 层的 `DataSourceSyncCase` 负责。Connector 的职责仅为：拉取外部数据，产出 `RawSourceDocument`。

各类型实现：

| 实现类 | 核心依赖 | fetchMetadata 方式 | fetchContent 方式 |
|--------|---------|--------------------|---------------------|
| `UrlConnector` | HttpClient + Jsoup | 按 depth BFS 渐进爬取，每层为一个 page | Jsoup 抓取单页正文 |
| `FeishuConnector` | 飞书开放平台 API | API 分页 `page_token`，每页 100 条节点元数据 | API 获取单篇 doc 的 Markdown |
| `YuqueConnector` | 语雀 API | API 分页 `offset`，每页 100 条文档摘要 | API 导出单篇 Markdown |
| `DatabaseConnector` | JDBC | `COUNT(*)` + `LIMIT/OFFSET` 分页拉取主键和增量列值 | `SELECT textColumns WHERE pk = ?` 单行拉取 |
| `FileConnector` | 对象存储 SDK | 列目录 + 文件元数据（size/mtime），按 marker 分页 | 读取单文件字节 |

### DataSourceDocument（增量同步映射表）

```java
@Value public class DataSourceDocument {
    String dataSourceId;
    String externalId;       // 飞书 pageToken / URL / 语雀 docId / DB row PK
    String documentId;       // 内部 raw_documents.id
    String externalVersion;  // 来源版本号
    Instant lastSyncedAt;
}
```

### SchemaIndex（DATABASE 类型专属）

```java
public class SchemaIndex {
    private String dataSourceId;
    private String tableName;
    private String tableComment;
    private List<ColumnInfo> columns;
    private List<TableRelationship> relationships;
    private List<Map<String, Object>> sampleRows;  // 3-5 行样本数据
    private Instant indexedAt;
}
```

## Index Pipeline Architecture

### 核心原则：编排器负责流程，DB 负责状态，Kafka 仅发业务事件

处理管道（parse → clean → chunk → embed → index）的流程控制在 **Case 层的编排器**中完成，不由 Kafka 串联。Kafka 只用于发布最终的业务成功/失败事件，通知外部系统。

**理由：**
- **状态管理单一真相来源** — 文档处理状态存 DB，排查时一条 SQL 即可，无需跨 Topic 追踪 offset
- **流程演进灵活** — 加/减/插步骤改编排器代码即可，不需要新增 Topic 和 Consumer
- **Phase 2/3 DAG Engine 无缝替换** — 编排器从线性 Pipeline 换成 DAG GraphExecutor，对外接口不变，Kafka 代码零改动
- **批量处理自然** — 编排器可以攒 100 条再批量调 Embedding API，Kafka Consumer 则需要额外攒批逻辑

```
       Case 层                                 Domain 层
═════════════════════════════════════════════════════════════════

  IndexPipeline                           IParser         IChunking       IEmbedding     IIndexWriter
  (编排器)      doc.status                .parse()        .chunk()        .embed()       .write()
     │         = PARSING                                                
     ├──▶ ① ──────────────────────────────────────────────────────────────────────────────────▶
     │         = CHUNKING                                                    ║
     ├──▶ ②  ...... ──────────────────────────────────────────────────────▶   每个步骤:
     │         = EMBEDDING                                                       更新 DB 状态
     ├──▶ ③  ......  ...... ─────────────────────────────────────────────────▶   处理异常
     │         = INDEXING                                                       重试
     ├──▶ ④  ......  ......  ...... ─────────────────────────────────────────▶
     │         = READY
     └──▶ ⑤ ──▶ 发布 document.indexed 事件（Kafka）──▶ Query 域感知
```

### Workload-Based Execution Strategy

不预测数据量大小（预测不了——URL 不知道多少页、DB 不知道多少行），只判断一件事：**调用方是否在等同步返回**。

```
入口触发                    执行策略                    说明
────────────────────────────────────────────────────────────────
上传 1 个小文件            同步直通                     在线等结果，直接返回
  (HTTP, file < 10MB)

上传大文件 / 批量文件      写 DB + 返回 202             返回 taskId，轮询进度
  (HTTP, file >= 10MB)    → 后台线程池处理

DataSource 手动同步        写 SynkTask + 返回 202        返回 taskId，轮询进度
  (HTTP)                  → Spring @Async 消费

Webhook 推送              验证签名 → 写 DB → 200         Connector 回调拉取
  (HTTP, 立即回 200)      → @Async 处理

定时任务                   写 SynkTask                    离线批量处理
  (Scheduler 线程)        → @Async 消费
```

```java
// Case 层的路由逻辑（伪代码）
public class DocumentLoadCase {

    public Object execute(RawSourceDocument source, TriggerType trigger) {
        if (trigger == TriggerType.HTTP_FILE_UPLOAD) {
            if (source.getContentSize() < SINGLE_FILE_THRESHOLD) {
                return pipeline.runSync(source);  // 同步跑，返回完整 Document
            } else {
                documentRepository.createPending(source);
                applicationEventPublisher.publish(new DocImportedEvent(source.getId()));
                return TaskResponse(202, "处理中", source.getId());  // 立即返回
            }
        }
        // DataSource 同步、Webhook、定时任务 — 一律异步
        documentRepository.createPending(source);
        applicationEventPublisher.publish(new DocImportedEvent(source.getId()));
        return TaskResponse(202, "已触发", source.getId());
    }
}
```

### IndexPipeline（线性编排器，Case 层）

```java
public class IndexPipeline {

    private final IParserStrategy parser;
    private final IDocumentCleaner cleaner;
    private final IChunkingStrategy chunker;
    private final IEmbeddingService embedder;
    private final IIndexWriter indexWriter;
    private final DocumentRepository documentRepo;
    private final ApplicationEventPublisher events;

    // 同步模式：单文档完整处理，HTTP 线程直接跑
    public DocumentResult runSync(RawSourceDocument rawDoc) {
        Document doc = Document.from(rawDoc);
        documentRepo.save(doc);
        return run(doc);
    }

    // 异步模式：后台线程池 / @Async 调用，逐文档推进
    public void runAsync(String docId) {
        Document doc = documentRepo.findById(docId);
        try {
            run(doc);
        } catch (Exception e) {
            doc.setStatus(FAILED);
            doc.setErrorMessage(e.getMessage());
            documentRepo.save(doc);
        }
    }

    private DocumentResult run(Document doc) {
        step(doc, PARSING,  () -> { doc.setContent(parser.parse(doc.getRawContent())); });
        step(doc, CLEANING, () -> cleaner.clean(doc.getContent()));
        step(doc, CHUNKING, () -> { doc.setChunks(chunker.chunk(doc.getContent(), config)); });
        step(doc, EMBEDDING,() -> { doc.setVectors(embedder.embedBatch(doc.getChunks())); });
        step(doc, INDEXING, () -> indexWriter.bulkWrite(doc.getChunks()));

        doc.setStatus(READY);
        documentRepo.save(doc);

        // 仅最终成功发 Kafka 业务事件
        events.publishEvent(new DocumentIndexedEvent(doc.getId(), doc.getKbId()));
        return DocumentResult.from(doc);
    }

    private void step(Document doc, Status status, Runnable action) {
        doc.setStatus(status);
        documentRepo.save(doc);  // 状态落库，断点可恢复
        action.run();
    }
}
```

### 异步 Worker：Spring @Async + ApplicationEvent

```
Phase 1 异步机制：Spring 内置的 ApplicationEvent + @Async

  @EventListener
  @Async
  public void onDocImported(DocImportedEvent event) {
      pipeline.runAsync(event.getDocId());
  }

优点: 无额外中间件，进程内解耦，与 Kafka 消费者代码结构一致
升级: Phase 2 需要跨服务消费时，@EventListener → @KafkaListener，业务逻辑复用
```

### 批量处理：Poller / 攒批

```java
// 对于 Embedding 和 Index Write 等外部 API 调用，编排器自然攒批
public class IndexPipeline {

    // 单文档时：embedBatch 传入单元素 List
    public DocumentResult runSync(RawSourceDocument rawDoc) { ... }

    // 批量时：攒够 100 篇，一次 Embedding API 调用
    @EventListener
    @Async
    public void onBatchImported(BatchImportedEvent event) {
        List<Document> docs = event.getDocumentIds().stream()
            .map(documentRepo::findById).collect(toList());
        
        // 批量 Embedding（一次 HTTP 调用 100 条文本）
        List<Chunk> allChunks = docs.stream()
            .flatMap(d -> d.getChunks().stream()).collect(toList());
        Map<String, float[]> vectors = embedder.embedBatch(allChunks);
        
        // 批量写入索引
        indexWriter.bulkWrite(docs);
        
        events.publishEvent(new BatchIndexedEvent(docIds));
    }
}
```

### 数据库数据源（DATABASE）分流

```
DatabaseConnector.fetchMetadata(cursor)
    │
    ├── textColumns ──→ 每条 RowMeta 确认版本变化后
    │     fetchContent(externalId) → RawSourceDocument → 进入文档管道
    │
    └── schema → SchemaIndexWriter.write() → schema_index 表
```

### Index OperatorType 全集

```
parse, clean, chunk, embed, index_write          — 文档处理
datasource_fetch                                  — 数据源元数据拉取
datasource_sync                                   — 同步编排（拉取→比对→发布）
schema_index_write                               — Schema 索引写入
entity_extract, relation_extract, graph_index    — KG 抽取与写入（Phase 2/3）
```

---

## Query Domain Design

### Intent Router（强制路由器）

`intent_detect` 是强制执行的第一步，输出决定后续路径：

```java
public enum QueryIntent {
    DOCUMENT_SEARCH,   // Path 1: 文档语义检索
    DATA_QUERY,        // Path 2: Text-to-SQL
    HYBRID             // Path 3: 双路并行
}
```

判断逻辑：

| 特征 | 意图 |
|------|------|
| 问题含统计词（最多/最少/总计/平均/排名）| DATA_QUERY |
| 问题含时间范围 + 数值（上个月/近30天 + 金额/数量）| DATA_QUERY |
| Service 没有关联 DATABASE 类型 DataSource | 强制 DOCUMENT_SEARCH |
| 问题含文档语气词（"文档说""根据规定""政策是"）| DOCUMENT_SEARCH |
| 其余模糊情形 | HYBRID |

### 三条执行路径

#### Path 1: 文档检索（包含 KG 扩展）

```
query_rewrite（可选）
    ↓
PARALLEL:
  vector_search  (pgvector)
  keyword_search (ES BM25)
  metadata_filter（可选）
    ↓
fusion (RRF / linear)
    ↓
[if kg enabled] graph_expand — 基于 ContentTree 结构扩展上下文
    ↓
rerank（可选）→ deduplicate
    ↓
context_assemble（token 预算 + 引用标记）
    ↓
prompt_render → llm_generate（SSE）
    ↓
citation_extract → safety_filter
```

#### Path 2: Text-to-SQL（新增）

```
schema_retrieve — 从 SchemaIndex 语义检索相关表和列
    ↓
text_to_sql — LLM 生成 SQL（maxRetries 次重试）
    ↓
sql_validate — 语法校验 + 安全校验（禁止 DML/DDL）+ 自动追加 LIMIT
    ↓
sql_execute — JDBC 执行（timeoutMs 控制）
    ↓
result_format — ResultSet → Markdown 表格
    ↓
llm_generate — 格式化结果 → 自然语言回答
```

#### Path 3: Hybrid（新增）

```
PARALLEL:
  ├── Path 1（文档检索）
  └── Path 2（数据查询）
    ↓
context_merge — 合并两路结果，标注来源，按 token 预算裁剪
    ↓
llm_generate → citation_extract（双溯源）→ safety_filter
```

### Unified Pipeline Config

统一采用 4 段结构。`document_search` 段内部沿用固定拓扑 + enable/disable 的模式。

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
    "kg_expand": { "enabled": false, "expandDepth": 1 },
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

#### Phase 1 vs DAG Engine 演进

| 维度 | Pipeline Config (Phase 1) | DAG Engine (Phase 2/3) |
|------|---------------------------|------------------------|
| 拓扑 | 固定 4 段结构 | 自定义，可任意组合 |
| 算子启用 | enable/disable 开关 | 动态添加/移除节点 |
| 并行 | document_search 内置 + Hybrid 双路 | 任意节点可并行 |
| 分支 | 仅 routing 阶段 3 路分叉 | 支持 onFailure / condition 跳转 |
| 环路 | 不支持 | 支持（如 fact_check 失败 → 二次检索） |
| 实现量 | ~300 行 PipelineExecutor | ~2000 行 DAG 引擎 |

Phase 2/3 引入 DAG Engine 后，Pipeline Config 可作为默认配置保留，DAG 作为高级自定义选项。

### Query OperatorType 全集

```
预处理:
  query_rewrite, intent_detect, term_expand, hyde_generate

路由:
  query_router, kb_router

检索:
  vector_search, keyword_search, metadata_filter

融合:
  rrf_fusion, linear_fusion

排序:
  rerank, deduplicate

上下文:
  context_assemble, context_compress

生成:
  prompt_build, llm_generate

后处理:
  citation_extract, safety_filter, format_convert

Text-to-SQL（新增）:
  schema_retrieve, text_to_sql, sql_validate, sql_execute, result_format

混合（新增）:
  context_merge

KG 检索（新增）:
  graph_expand, entity_link, graph_traverse, graph_vector_fusion
```

### RAGResponse（统一版）

```java
public class RAGResponse {
    String answer;
    Flux<String> streaming;           // SSE 模式下的 token 流

    QueryType queryType;              // DOCUMENT_SEARCH | DATA_QUERY | HYBRID

    // 文档检索结果（queryType = DOCUMENT_SEARCH 或 HYBRID 时填充）
    @Nullable Map<String, Citation> citations;

    // 数据查询结果（queryType = DATA_QUERY 或 HYBRID 时填充）
    @Nullable DataQueryResult dataResult;

    String traceId;
    ResponseMetadata metadata;        // model, latency, tokenUsage 等
}

@Value public class DataQueryResult {
    String sql;
    String dataSourceId;
    String tableName;
    int rowCount;
    List<String> columns;
    List<Map<String, Object>> rows;   // 截断后的结果集
    boolean truncated;
}
```

---

## Knowledge Graph Design

KG 不是第三个 Domain，而是 Index Domain 新增的产出物 + Query Domain 新增的消费方式。

### 四期渐进式演进

| Phase | 名称 | 核心能力 | 依赖 | 新中间件 |
|-------|------|---------|------|---------|
| 1 | 架构预留 | types POJO + 策略接口定义 | 无 | 无 |
| 2 | 结构图谱 | 基于 ContentTree 的 Chunk 关系图 + 路径扩展检索 | ContentTree（已有） | 无 |
| 3 | 实体图谱 | LLM 抽取实体/关系 + Neo4j 存储 + 实体链接检索 | Phase 1 接口 + LLM | Neo4j |
| 4 | 全局图谱 | 社区发现 + 社区摘要 + 全局问答 | Phase 3 + Leiden | 无增量 |

### KG 数据结构

```java
// types/ 中的纯 POJO
public class Entity {
    String entityId, name, type;     // type: PERSON, ORG, PRODUCT, DATE, LOCATION...
    List<String> aliases;
    Map<String, Object> props;
}
public class Relation {
    String relationId;
    String subject, object;          // 实体引用
    String predicate;                // "founded", "released", "works_at"...
    String sourceChunkId;            // 溯源
    double confidence;               // 0.0-1.0
}
public class GraphQuery {
    String serviceId;
    List<String> entityIds;
    int maxHops;
    List<String> relationTypes;
    int maxResults;
}
public class GraphRetrievalResult {
    List<Entity> entities;
    List<Relation> relations;
    List<String> relatedChunkIds;
    double graphScore;
}
```

### KG 策略接口

```java
// domain/index/
interface IEntityExtractor {
    boolean canHandle(SourceType type);
    List<Entity> extract(CanonicalDocument doc, ContentNode node);
}
interface IRelationExtractor {
    List<Relation> extract(List<Entity> entities, ContentNode node);
}

// domain/query/
interface IGraphExpander {
    List<Chunk> expand(List<Chunk> chunks, int expandDepth);
}
interface IGraphRetriever {
    GraphRetrievalResult retrieve(GraphQuery query);
}
interface IEntityLinker {
    List<Entity> link(String mention, String context);
}

// infrastructure/graph/
interface GraphRepository {
    void saveEntities(String serviceId, List<Entity> entities);
    void saveRelations(String serviceId, List<Relation> relations);
    GraphRetrievalResult query(GraphQuery query);
    void deleteByService(String serviceId);
    void deleteByDocument(String documentId);
}
```

### Phase 2: 结构图谱

基于 ContentTree 就地构建 Chunk 关系图，零额外存储：

- Chunk 关系类型：`PARENT_OF`、`SIBLING_OF`、`PREV_OF / NEXT_OF`
- `ContentTreeTraverser`：基于 `structurePath` 计算 Chunk 间关系
- `GraphExpander`：沿 ContentTree 关系拉入父/子/兄弟 Chunk 扩展上下文

### Phase 3: 实体图谱

- **LLMEntityExtractor** + **LLMRelationExtractor**：few-shot prompt + LLM 按 Chunk 抽取
- **Neo4jGraphRepository**：GraphRepository 的 Neo4j 实现
- **EntityLinker**：查询提及文本 → 图数据库实体消歧匹配
- **GraphVectorFusion**：graph_first / interleave / score_combine 三种融合策略

### Phase 4: 全局图谱

- **LeidenCommunityDetector**：对 Neo4j 中实体-关系图执行 Leiden 社区发现
- **CommunitySummarizer**：LLM 生成社区摘要
- **GlobalGraphSearcher**：社区摘要匹配 + 关键实体返回

---

## Resource & Storage Model

### Index Domain 拥有的表

```
raw_documents       — KB 级共享，原始文档 + CanonicalDocument (ContentTree JSONB)
                      └── 新增 datasource_id 外键
data_sources        — DataSource 连接配置与同步状态
datasource_documents — 外部 ID → 内部 Document 增量同步映射
schema_index        — 数据库表结构（DATABASE 类型 DataSource 专属）
service_chunks      — Service 级独立，按 Service 的分块配置切割
service_vectors     — Service 级独立 (pgvector)
```

### Query Domain 读取的表

Query 通过 Index 的 Repository 接口访问上述表，不直接拥有存储。

### Service 配置表（JSONB）

```
service_index_graph  — Service 级，索引 DAG 图定义
service_query_graph  — Service 级，Pipeline Config（Phase 1）/ GraphDefinition（Phase 2/3）
service_prompt_templates — Service 级，Prompt 模板配置
```

### Platform 级表

```
services            — Service 定义
knowledge_bases     — KB 定义
service_kb_grants   — KB↔Service 授权
api_keys            — API Key 管理
audit_logs          — 审计日志（按月分表）
```

### Kafka Topics 全集

Kafka 仅用于广播**对外有意义的业务事件**，不用于内部处理管道的流程控制。

```
业务事件（外部关心）:
  document.indexed          — 文档索引完成（含 chunks/vectors），Query 域可检索
  datasource.sync.completed — DataSource 同步结束（含新增/更新/删除统计）
  schema.indexed            — SchemaIndex 写入完成

Design principle:
  处理管道状态流转（parse→clean→chunk→embed→index）由 IndexPipeline 编排器控制，状态存 DB。
  Kafka 不参与流程编排，不表达"内部走到哪一步"。
```

### 新增数据库表

```sql
CREATE TABLE data_sources (
    id VARCHAR(36) PRIMARY KEY,
    kb_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,   -- URL|FEISHU|YUQUE|DATABASE|FILE
    config JSONB NOT NULL,
    sync_strategy VARCHAR(32) NOT NULL,   -- ONCE|SCHEDULED|WEBHOOK
    sync_schedule VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    last_sync_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE datasource_documents (
    datasource_id VARCHAR(36) NOT NULL,
    external_id VARCHAR(512) NOT NULL,
    document_id VARCHAR(36) NOT NULL,
    external_version VARCHAR(255),
    last_synced_at TIMESTAMP NOT NULL,
    PRIMARY KEY (datasource_id, external_id)
);

CREATE TABLE schema_index (
    id VARCHAR(36) PRIMARY KEY,
    datasource_id VARCHAR(36) NOT NULL,
    table_name VARCHAR(255) NOT NULL,
    table_comment TEXT,
    columns JSONB NOT NULL,
    relationships JSONB,
    sample_rows JSONB,
    indexed_at TIMESTAMP NOT NULL,
    UNIQUE (datasource_id, table_name)
);

ALTER TABLE raw_documents ADD COLUMN datasource_id VARCHAR(36);

CREATE TABLE sync_tasks (
    id VARCHAR(36) PRIMARY KEY,
    datasource_id VARCHAR(36) NOT NULL,
    trigger_type VARCHAR(16) NOT NULL,  -- MANUAL | SCHEDULED | WEBHOOK
    status VARCHAR(16) NOT NULL,        -- RUNNING | COMPLETED | FAILED
    total_count INT DEFAULT 0,
    processed_count INT DEFAULT 0,
    added_count INT DEFAULT 0,
    updated_count INT DEFAULT 0,
    deleted_count INT DEFAULT 0,
    error_log JSONB,                    -- [{docId, error}, ...]
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);
```

### Management API 全集

```
Service CRUD:
  POST/GET/PUT/DELETE /api/v1/services

KB CRUD:
  POST/GET/PUT/DELETE /api/v1/knowledge-bases

Document 管理:
  POST/GET/DELETE /api/v1/knowledge-bases/{kbId}/documents
  GET  /api/v1/knowledge-bases/{kbId}/documents/{docId}/status  — 单文档处理进度
  GET  /api/v1/knowledge-bases/{kbId}/documents/status?ids=...  — 批量查询文档处理状态

Grant 管理:
  POST/DELETE /api/v1/knowledge-bases/{kbId}/grants

DataSource CRUD:
  POST/GET/PUT/DELETE /api/v1/knowledge-bases/{kbId}/datasources

DataSource 操作:
  POST /api/v1/knowledge-bases/{kbId}/datasources/{dsId}/test-connection
  POST /api/v1/knowledge-bases/{kbId}/datasources/{dsId}/sync
  GET  /api/v1/knowledge-bases/{kbId}/datasources/{dsId}/sync-status

Pipeline Config:
  PUT /api/v1/services/{serviceId}/pipeline-config

RAG Runtime:
  POST /api/v1/services/{serviceId}/search
  POST /api/v1/services/{serviceId}/rag

Webhook 接收:
  POST /api/v1/webhooks/feishu/{dsId}
  POST /api/v1/webhooks/yuque/{dsId}
```

---

## Prompt Template System

```
Layer 1: 平台预置模板   — 通用场景（客服/文档总结/代码审查/翻译）
Layer 2: Service 自定义 — 继承预置 + 覆盖字段 + 自定义变量
Layer 3: API 调用覆盖   — 每次请求可临时覆盖部分参数
```

模板变量体系：`{role}`, `{context}`, `{query}`, `{history}`, `{kb_names}`, `{date}`, `{service_name}`, `{custom_*}`，支持条件片段渲染。

---

## 渐进式演进路线

```
Phase 1 (现在)
──────────────
Index: Kafka 事件链 + DataSource Connector 策略体系
Query: Pipeline Config（~300 行 PipelineExecutor）
KG:    策略接口预留（types + 空接口，零实现）
存储:  PostgreSQL + pgvector + ES + Kafka

        ↓  当有 Service 需要自定义拓扑时

Phase 2 (按需)
──────────────
引入 infrastructure/dag/
Query Path 增加 GraphExecutor 能力
KG:    结构图谱（基于 ContentTree 的 Chunk 关系图）

        ↓  当需要结构化关系检索时

Phase 3 (实体图谱)
──────────────
Neo4j 图数据库 + LLM 实体关系抽取
DAG 支持 LLM 节点的 "next" 输出（Agentic RAG）
支持环路（检索 → 生成 → 校验 → 检索...）

        ↓  当需要全局知识理解时

Phase 4 (全局图谱)
──────────────
Leiden 社区发现 + 社区摘要 + 全局问答
```
