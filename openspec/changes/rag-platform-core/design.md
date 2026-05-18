# Design: RAG Platform Core

## Architecture Overview

### Two Core Domains: Index & Query

RAG 系统的自然边界不是"文档"和"流水线"，而是**写路径（Index）**和**读路径（Query）**。DAG 编排引擎不是领域，而是 Infrastructure 层的技术组件。

```
┌──────────────────────────────────────────────────────────────────────┐
│                          RAG Platform                                 │
│                                                                      │
│  ┌─────────────────────────────┐  ┌─────────────────────────────┐    │
│  │   Index Domain (写路径)      │  │   Query Domain (读路径)      │    │
│  │                             │  │                             │    │
│  │  数据摄入 → 解析 → 分块      │  │  查询改写 → 检索 → 重排序     │    │
│  │       ↓                    │  │       ↓                    │    │
│  │  向量化 → 写入索引           │  │  上下文组装 → LLM 生成       │    │
│  │                             │  │                             │    │
│  │  拥有: Documents, Chunks,    │  │  读取: Chunks, Vectors      │    │
│  │        Vectors, Index        │  │  (通过 Index 的 Repository) │    │
│  │                             │  │                             │    │
│  │  关注: 吞吐量, 完整性        │  │  关注: 延迟, 相关性, 质量     │    │
│  └──────────────┬──────────────┘  └──────────────┬──────────────┘    │
│                 │                                │                   │
│                 └────────────┬───────────────────┘                   │
│                              │                                       │
│                    ┌─────────┴─────────┐                             │
│                    │    Case 层 (编排)  │                             │
│                    │  跨域流程串联      │                             │
│                    └─────────┬─────────┘                             │
│                              │                                       │
│                              ▼                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                   Infrastructure 层                            │  │
│  │                                                               │  │
│  │  ┌──────────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │  │
│  │  │  dag/        │  │  dao/    │  │ gateway/ │  │  redis/  │  │  │
│  │  │  DAG 执行引擎 │  │          │  │          │  │          │  │  │
│  │  │              │  │          │  │          │  │          │  │  │
│  │  │ GraphExecutor│  │          │  │          │  │          │  │  │
│  │  │ TopoSort     │  │          │  │          │  │          │  │  │
│  │  │ NodeScheduler│  │          │  │          │  │          │  │  │
│  │  └──────────────┘  └──────────┘  └──────────┘  └──────────┘  │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                      types/  (共享类型)                        │  │
│  │  GraphDefinition, NodeConfig, OperatorType, Chunk, ...        │  │
│  └───────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
```

### 分层架构与 DAG 的归属

```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│  Trigger 层    HTTP Controller / MQ Listener / Job           │
│                      │                                       │
│                      ▼                                       │
│  Case 层        跨域编排，组装 DAG                            │
│                 │                                            │
│                 │  1. 从 DB 读取 GraphDefinition (JSONB)      │
│                 │  2. OperatorRegistry 解析 node → Domain    │
│                 │     Service Bean                           │
│                 │  3. 调用 infra/dag/GraphExecutor.execute() │
│                 │                                            │
│      ┌──────────┴──────────┐                                 │
│      │                     │                                 │
│      ▼                     ▼                                 │
│  ┌──────────────┐   ┌──────────────┐                         │
│  │ Index Domain │   │ Query Domain │                         │
│  │              │   │              │                         │
│  │ 定义"做什么"  │   │ 定义"做什么"  │                         │
│  │ 算子策略接口  │   │ 算子策略接口  │                         │
│  │ IChunkStrategy│   │ ISearchOp    │                         │
│  │ IParserStrategy│  │ IRerankOp    │                         │
│  │ ...          │   │ ...          │                         │
│  └──────┬───────┘   └──────┬───────┘                         │
│         │                  │                                  │
│         └────────┬─────────┘                                  │
│                  │                                            │
│                  ▼                                            │
│  Infrastructure 层                                           │
│  ┌─────────────────────────────────────┐                     │
│  │  dag/  (纯技术编排，不涉及业务)       │                     │
│  │  GraphExecutor, TopologicalSorter   │                     │
│  │  NodeScheduler, DegradationHandler  │                     │
│  │                                     │                     │
│  │  只依赖 types/ 中的数据结构          │                     │
│  │  不依赖任何 Domain                   │                     │
│  └─────────────────────────────────────┘                     │
│                                                              │
│  types/                                                      │
│  GraphDefinition, NodeConfig, EdgeDefinition, OperatorType   │
│  (纯数据结构，JSONB 序列化用，不含任何行为)                    │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

**依赖方向**:

```
types/  ←  domain/index/  ←  case/
types/  ←  domain/query/  ←  case/
types/  ←  infrastructure/dag/  ←  case/

case/  组装 infra/dag/ 的 GraphExecutor + domain/ 的算子 Bean
domain/ 不依赖 infrastructure/dag/（Domain 只定义算子策略接口）
infrastructure/dag/ 不依赖 domain/（纯编排，通过 types/ 中的 NodeConfig 获取算子名称）
```

**各层职责**:

| 层 | Dag 相关内容 | 职责 |
|---|------------|------|
| `types/` | `GraphDefinition`, `NodeConfig`, `EdgeDefinition`, `OperatorType` | 纯数据结构，JSONB 序列化 |
| `domain/**/` | `IOperator` 策略接口（各算子实现是 Domain Service） | 业务逻辑，与编排无关 |
| `infrastructure/dag/` | `GraphExecutor`, `TopologicalSorter`, `NodeScheduler` | 技术编排，拓扑排序/超时/降级 |
| `case/` | 从配置组装 DAG 实例，桥接 domain 算子和 infra 执行器 | 跨域编排 |

## Data Parser Architecture (数据解析器)

### Design Rationale

RAG 中台的数据来源是多样的：网页、文件（PDF/DOCX/Markdown/Excel）、数据库表、API 接口响应、JSON、代码文件等。解析器架构需要解决三个核心问题：

1. **异构数据统一**：不同来源的数据经过解析器后，产出统一的中间格式，分块策略只需理解一种模型
2. **可扩展**：新增数据源只需注册新的解析策略，不影响现有解析器和分块逻辑
3. **结构保留**：解析后的中间格式需保留原始结构信息（标题层级、表格、代码块、页码等），用于溯源引用和智能分块

### 核心设计决策：选择 Canonical Document Model 而非 Unified Markdown

| 方案 | 说明 | 判断 |
|------|------|------|
| 统一 Markdown | 所有来源解析为 Markdown 字符串 | ❌ 丢失页码/URL/表格结构等溯源信息，分块策略无法感知结构 |
| 各格式保留原结构 | 每种格式独立处理 | ❌ 分块策略数量爆炸（N种格式 × M种策略） |
| **Canonical Document Model** | 统一 ContentTree 中间格式 + Markdown 渲染 | ✅ 分块策略统一，源信息不丢失，Markdown 供 LLM 消费 |

**核心原则：Markdown 是渲染结果（给 LLM 读的），ContentTree 是结构中间层（给分块策略决策用的）。两者共存。**

### Canonical Document Model

#### DDD 模型分类

| 类 | DDD 类型 | 所在包 | 说明 |
|---|---------|--------|------|
| `CanonicalDocument` | **Aggregate Root** | `model/aggregate/` | 解析器产出的统一文档模型，拥有 ContentTree 的整体生命周期 |
| `ContentNode` | **Entity** | `model/entity/` | 树形结构节点，有 nodeId 标识，生命周期依附于 CanonicalDocument |
| `DocumentMetadata` | **Value Object** | `model/valobj/` | 不可变，通过属性值相等判断 |
| `SourcePosition` | **Value Object** | `model/valobj/` | 不可变，溯源定位信息 |
| `DocumentStatistics` | **Value Object** | `model/valobj/` | 不可变，解析后计算的统计信息 |
| `SourceType` | **Enum VO** | `model/valobj/` | WEB, FILE, DATABASE, API, CRAWL |
| `NodeType` | **Enum VO** | `model/valobj/` | ROOT, SECTION, PARAGRAPH, TABLE, LIST_ITEM, CODE, IMAGE, QUOTE |

#### CanonicalDocument（聚合根）

```java
/**
 * 解析器产出的统一文档模型。
 * 一个 CanonicalDocument 对应 raw_documents 表中的一行，
 * structure 字段以 JSONB 存储整个 ContentTree。
 */
public class CanonicalDocument {

    /** 文档唯一标识（对应 raw_documents.id） */
    private String documentId;

    /** 源元数据 */
    private DocumentMetadata metadata;

    /** 结构化内容树 */
    private ContentNode root;

    /** 解析后统计 */
    private DocumentStatistics statistics;

    // === 聚合根行为 ===

    /** 遍历整棵树，对每个节点执行回调 */
    public void walkTree(Consumer<ContentNode> visitor) {
        walkRecursive(root, visitor);
    }

    /** 按类型过滤所有节点（叶子遍历） */
    public List<ContentNode> findNodesByType(NodeType type) {
        List<ContentNode> result = new ArrayList<>();
        walkTree(node -> { if (node.getType() == type) result.add(node); });
        return result;
    }

    /** 获取所有叶子节点的 markdown 拼接（不分块版本） */
    public String toFullMarkdown() {
        StringBuilder sb = new StringBuilder();
        walkTree(node -> {
            if (node.isLeaf() && node.getMarkdown() != null) {
                sb.append(node.getMarkdown()).append("\n\n");
            }
        });
        return sb.toString();
    }

    /** 指定节点为根的 heading 路径 */
    public String getHeadingPath(ContentNode node) {
        // 从根到 node 遍历，收集每个 SECTION 祖先的 heading
        ...
    }
}
```

#### DocumentMetadata（值对象）

```java
/**
 * 文档源元数据。不可变值对象。
 */
@Value  // Lombok: all fields final, equals/hashCode by value
public class DocumentMetadata {

    /** 数据来源类型 */
    SourceType sourceType;

    /** 原始来源定位：文件路径 / URL / 数据库连接串 / API endpoint */
    String sourceURI;

    /** MIME 类型 */
    String mimeType;

    /** 文档标题（解析器从内容中提取或用户指定） */
    String title;

    /** 文件夹上传时的相对路径，单文件上传为 null */
    @Nullable String folderPath;

    /** 数据采集时间 */
    LocalDateTime capturedAt;

    /** 源特定扩展元数据，如:
     *  - DB: { "connectionName": "orders_db", "tableName": "orders", "queryHash": "abc123" }
     *  - Web: { "crawlerVersion": "2.1", "originalEncoding": "UTF-8", "fetchDurationMs": 320 }
     *  - API: { "responseStatusCode": 200, "contentType": "application/json" }
     */
    Map<String, Object> custom;
}
```

#### ContentNode（实体 — 递归树节点）

```java
/**
 * 内容树节点。实体具备唯一标识（nodeId），支持递归子节点。
 * 非叶子节点（SECTION、LIST）不持有 markdown/plainText 内容，
 * 其 markdown/plainText 由子节点拼接而成。
 */
public class ContentNode {

    /** 节点唯一标识（document 内唯一），如 "n01", "n01-02", "n01-02-03" */
    private String nodeId;

    /** 节点类型 */
    private NodeType type;

    // === 结构信息（非叶子节点主要字段） ===

    /** 该节点下最近的标题文本。仅 SECTION 节点填写 */
    @Nullable private String heading;

    /** 标题层级：0=根节点, 1=h1, 2=h2, ..., 6=h6 */
    private int headingLevel;

    // === 内容信息（叶子节点主要字段） ===

    /** 预渲染 Markdown 文本。用于：
     *  1. 拼接后送入 Embedding API
     *  2. 拼接后作为 LLM 上下文
     *
     *  null = 非叶子节点（内容由子节点拼接）
     */
    @Nullable private String markdown;

    /** 纯文本（去 Markdown 标记）。用于 ES BM25 全文索引。
     *  null = 非叶子节点
     */
    @Nullable private String plainText;

    // === 溯源信息 ===

    /** 源文件中的位置信息 */
    @Nullable private SourcePosition position;

    /** 节点级元数据，按类型不同含义不同：
     *  TABLE:  { "columns": ["Name","Type","Desc"], "rowCount": 15, "hasHeader": true }
     *  CODE:   { "language": "python", "startLine": 10, "endLine": 68 }
     *  IMAGE:  { "altText": "Architecture diagram", "width": 800, "height": 600, "ocrText": "..." }
     *  SECTION: { "isAnchored": true, "anchorId": "installation" }
     */
    private Map<String, Object> metadata;

    /** 子节点列表 */
    private List<ContentNode> children;

    // === 行为 ===

    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }

    public void addChild(ContentNode child) {
        if (children == null) children = new ArrayList<>();
        children.add(child);
    }

    /** 获取从根到此节点的 heading 路径 */
    public List<String> getHeadingPath() {
        // 从根开始遍历，收集每个 SECTION 祖先的 heading
        ...
    }

    /** 递归计算节点树的总 token 数 */
    public int estimateTokens() {
        ...
    }
}
```

#### SourcePosition（值对象）

```java
/**
 * 溯源定位信息。任一字段均可为 null，按 SourceType 不同启用不同字段组合。
 */
@Value
public class SourcePosition {

    /** PDF 页码（1-based） */
    @Nullable Integer pageNumber;

    /** 源文件行号范围 */
    @Nullable Integer lineStart;
    @Nullable Integer lineEnd;

    /** 源文件字符偏移 */
    @Nullable Integer charOffset;

    /**
     * 通用溯源指针，格式按 SourceType 不同：
     *
     *   Web/HTML:   "body > main > div#content > p:nth-of-type(3)"
     *   Markdown:   "L120-L145"
     *   Excel:      "Sheet1!A1:D10"
     *   Database:   "db.orders.row_c3f8a1b2"
     *   JSON:       "$.data.items[0].description"
     *   Code:       "example.py:42-68"
     *   PDF:        "page:15;block:7"
     */
    @Nullable String sourcePointer;
}
```

#### DocumentStatistics（值对象）

```java
@Value
public class DocumentStatistics {

    /** 纯文本总字符数 */
    int totalChars;

    /** tiktoken 预估总 token 数 */
    int totalTokens;

    /** 各类型节点数量分布 */
    Map<NodeType, Integer> nodeTypeCounts;
}
```

#### 枚举值对象

```java
public enum SourceTypeEnumVO {
    WEB     ("web",     "网页"),
    FILE    ("file",    "文件"),
    DATABASE("database","数据库"),
    API     ("api",     "API接口"),
    CRAWL   ("crawl",   "爬虫采集");

    private final String code;
    private final String info;
}

public enum NodeTypeEnumVO {
    ROOT       ("root",       "根节点"),
    SECTION    ("section",    "章节/标题区段"),
    PARAGRAPH  ("paragraph",  "段落"),
    TABLE      ("table",      "表格"),
    LIST_ITEM  ("list_item",  "列表项"),
    CODE       ("code",       "代码块"),
    IMAGE      ("image",      "图片"),
    QUOTE      ("quote",      "引用块");
}
```

### ContentTree 的 JSONB 存储结构

`raw_documents.structure` 列以 JSONB 存储整个树：

```json
{
  "nodeId": "root",
  "type": "ROOT",
  "headingLevel": 0,
  "children": [
    {
      "nodeId": "n01",
      "type": "SECTION",
      "heading": "Getting Started",
      "headingLevel": 1,
      "position": { "lineStart": 1, "sourcePointer": "L1-L2" },
      "children": [
        {
          "nodeId": "n01-01",
          "type": "PARAGRAPH",
          "markdown": "This guide will help you set up the RAG platform in **5 minutes**.",
          "plainText": "This guide will help you set up the RAG platform in 5 minutes.",
          "position": { "lineStart": 3, "lineEnd": 4, "sourcePointer": "L3-L4" }
        },
        {
          "nodeId": "n01-02",
          "type": "SECTION",
          "heading": "Prerequisites",
          "headingLevel": 2,
          "position": { "lineStart": 6, "sourcePointer": "L6-L7" },
          "children": [
            {
              "nodeId": "n01-02-01",
              "type": "LIST_ITEM",
              "markdown": "- JDK 17 or later",
              "plainText": "JDK 17 or later",
              "position": { "lineStart": 8, "sourcePointer": "L8" }
            },
            {
              "nodeId": "n01-02-02",
              "type": "LIST_ITEM",
              "markdown": "- Maven 3.8+",
              "plainText": "Maven 3.8+",
              "position": { "lineStart": 9, "sourcePointer": "L9" }
            }
          ]
        },
        {
          "nodeId": "n01-03",
          "type": "CODE",
          "markdown": "```bash\nmvn clean install\n```",
          "plainText": "mvn clean install",
          "position": { "lineStart": 11, "lineEnd": 13, "sourcePointer": "L11-L13" },
          "metadata": { "language": "bash", "startLine": 11, "endLine": 13 }
        }
      ]
    },
    {
      "nodeId": "n02",
      "type": "SECTION",
      "heading": "API Reference",
      "headingLevel": 1,
      "position": { "lineStart": 15, "sourcePointer": "L15-L16" },
      "children": [
        {
          "nodeId": "n02-01",
          "type": "TABLE",
          "markdown": "| Endpoint | Method | Description |\n|---------|--------|-------------|\n| /search | POST | Semantic search |\n| /rag | POST | Full RAG pipeline |",
          "plainText": "Endpoint Method Description\n/search POST Semantic search\n/rag POST Full RAG pipeline",
          "position": { "lineStart": 17, "lineEnd": 20, "sourcePointer": "L17-L20" },
          "metadata": { "columns": ["Endpoint", "Method", "Description"], "rowCount": 2, "hasHeader": true }
        }
      ]
    }
  ]
}
```

### 分块策略如何消费 ContentTree

分块策略遍历 ContentTree，在两类边界处切分：

```
遍历规则:
  - 遇到 SECTION (headingLevel >= 2) → 新 chunk 边界
  - 遇到 TABLE / CODE → 当前 chunk 收容不下则另起新 chunk（节点完整性优先）
  - 遇到 PARAGRAPH / LIST_ITEM → 累加到当前 chunk，token 计数超 chunkSize 时切分
  - chunk_overlap: 在新 chunk 开头附加前一个 chunk 最后 N 个 token（句子边界对齐）
```

### ContentTree 如何支持分块和检索

```
ContentTree (分块策略读取结构层次，做决策)
     │
     │  "这个 SECTION 下面有 3 个 PARAGRAPH + 1 个 CODE"
     │  "TABLE 不能拆分，要整体保留"
     │  "headingLevel >= 2 = chunk 边界"
     │
     ▼
  生成 Chunk
     │
     │  Chunk.content = 拼接 chunk 内各节点的 markdown → Embedding API
     │  Chunk.plainText = 拼接 chunk 内各节点的 plainText → ES BM25 索引
     │  Chunk.structurePath = "Getting Started > Prerequisites"
     │  Chunk.sourcePositions = [L3-L4, L8, L9, L11-L13] → 引用溯源
     │
     ▼
  Chunk.content ──▶ Embedding API ──▶ pgvector
  Chunk.plainText ──▶ ES Index
  Chunk.structurePath ──▶ Chunk metadata (用于过滤/展示)
```

### Parser Strategy Architecture

采用策略模式（符合 DDD Domain 层设计规范）：

```
Domain: document/service/parse/

IParserStrategy (策略接口)
├── canHandle(SourceType, mimeType) → boolean
├── parse(SourceInput) → CanonicalDocument
└── probe(SourceInput) → confidence: float    // 文件类型自动检测

ParserRegistry (策略路由)
├── register(IParserStrategy)
└── resolve(SourceType, mimeType) → IParserStrategy
    ├── 1. 精确 mimeType 匹配
    ├── 2. SourceType 兜底匹配
    └── 3. probe() 自动检测 (未知格式时，遍历所有策略)

首期实现的解析器：
├── PdfParser          → CanonicalDocument     (Apache PDFBox)
├── HtmlParser         → CanonicalDocument     (Jsoup)
├── MarkdownParser     → CanonicalDocument     (flexmark-java)
├── DocxParser         → CanonicalDocument     (Apache POI)
├── PlainTextParser    → CanonicalDocument     (fallback)
│
后期扩展：
├── ExcelParser        → CanonicalDocument     (Apache POI)
├── JsonParser         → CanonicalDocument     (Jackson 结构化展开)
├── CodeFileParser     → CanonicalDocument     (按类/函数/代码块解析)
├── DatabaseRowParser  → CanonicalDocument     (JDBC ResultSet → TABLE)
└── ImageOcrParser     → CanonicalDocument     (Tesseract/多模态模型)
```

### 文件夹上传处理

文件夹不是一个"文档"，而是一个**递归容器**，由 Case 层编排处理：

```
FolderImportCase (Case 层)
├── 1. 解压/递归遍历文件夹
├── 2. 收集所有文件列表
├── 3. 对每个文件：
│   ├── probe() 检测真实类型（不仅依赖扩展名）
│   ├── ParserRegistry.resolve() → 路由到对应解析器
│   ├── parse() → CanonicalDocument（metadata.folderPath 记录目录路径）
│   └── 一个物理文件 = 一个 Document 实体
├── 4. 所有文件归属同一个 KB
├── 5. 支持的文件：.pdf .docx .md .html .txt .json .xml .csv .xlsx .py .java .js .ts .go
│   跳过的文件：二进制(.zip .tar .gz .exe)、图片(.png .jpg 首期跳过，后续加 OCR)
└── 6. 逐个创建 raw_documents 行，触发处理流水线
```

### 不同数据源 → ContentTree 映射

| 来源 | 解析器 | 映射规则 | sourcePointer 示例 |
|------|--------|---------|-------------------|
| PDF 技术手册 | PdfParser | 标题样式 → SECTION，内文 → PARAGRAPH，表格区域 → TABLE | `"page:15"` |
| HTML 网页 | HtmlParser | `<h1>-<h6>` → SECTION，`<article>` → 根，`<nav>/<footer>` → 噪音标记 | `"div#main-content"` |
| Markdown | MarkdownParser | `##` → SECTION，` ``` ` → CODE，列表 → LIST | `"L120-L145"` |
| DOCX 文档 | DocxParser | 样式 "Heading 1" → SECTION，正文 → PARAGRAPH | `"page:8"` |
| Excel 表格 | ExcelParser | Sheet → SECTION，数据区域 → TABLE | `"Sheet1!A1:D100"` |
| JSON API 响应 | JsonParser | key → SECTION 层级，数组 → TABLE | `"$.data.items[0]"` |
| 数据库表行 | DatabaseRowParser | 列名 → TABLE header，行 → rows | `"db.orders.row_c3f8"` |
| 代码文件 | CodeFileParser | class/function → SECTION，docstring → PARAGRAPH，body → CODE | `"example.py:42-68"` |

## Resource Model

### Index Domain（写路径 — 数据拥有者）

```
  Platform
  ├── Service (租户单元)
  │   ├── id, name
  │   ├── chunk_config (chunkSize, chunkOverlap, strategy)
  │   ├── embedding_config (model, dimensions, batchSize)
  │   └── quota (maxDocuments, maxVectors, maxStorageGB)
  │
  ├── KnowledgeBase
  │   ├── id, name, owner
  │   ├── documents[]
  │   └── ServiceKBGrant[] (N:M 授权)
  │
  ├── Document
  │   ├── id, kb_id
  │   ├── title, fileType
  │   ├── rawContent (原始字节/文本)
  │   ├── structure (CanonicalDocument JSONB — ContentTree)
  │   └── status (uploaded → parsing → parsed → chunking → chunked → embedding → ready)
  │
  ├── ServiceChunk (Service 级独立)
  │   ├── chunkId, serviceId, documentId
  │   ├── content (markdown 渲染文本 → Embedding)
  │   ├── plainText (纯文本 → ES BM25)
  │   ├── tokenCount
  │   ├── structurePath ("Chapter 2 > Installation")
  │   └── sourcePositions: [{page: 3, sourcePointer: "L120-L145"}, ...]
  │
  └── ServiceVector (Service 级独立)
      ├── chunkId, serviceId
      ├── vector: float[]
      └── model: String  (text-embedding-3-small / text-embedding-3-large / ...)
```

### Query Domain（读路径 — 数据消费者）

```
  SearchRequest
  ├── query: String                 // 原始查询
  ├── serviceId: String
  ├── topK: int                     // 返回结果数
  ├── filters: Map<String, Object>  // 元数据过滤
  └── override: Map                 // Layer 3 参数覆盖

  RetrievedChunk
  ├── chunkId, documentId
  ├── content: String               // markdown 内容
  ├── structurePath: String
  ├── sourcePositions: List         // 溯源定位
  ├── scores: {
  │     vectorScore: float?,
  │     keywordScore: float?,
  │     fusionScore: float?,
  │     rerankScore: float?
  │   }
  └── citationRef: String           // [ref_1], [ref_2]...

  RAGResponse
  ├── answer: String                // LLM 生成答案
  ├── streaming: SSE                // 流式模式下的 token 流
  ├── citations: Map<String, Citation>
  │     // "[ref_1]": { chunk, document, score, sourcePosition }
  ├── traceId: String
  └── metadata: { model, latency, tokenUsage, ... }
```

### DAG 配置数据结构（定义在 types/，存储为 JSONB）

```
  GraphDefinition = { id, serviceId, nodes: NodeConfig[], edges: EdgeDefinition[] }

  NodeConfig = {
    nodeId: string,
    operatorType: OperatorType,           // 对应 Domain 层的算子 Bean 名称
    config: OperatorConfig,               // 算子特定参数
    deps: string[],                       // 依赖节点 ID 列表
    timeout: ms,
    onFailure: skip | error | pass_through
  }

  EdgeDefinition = { from: nodeId, to: nodeId }

  OperatorType 分类:
    索引算子（Index Domain 定义）:
      parse, clean, chunk, embed, index_write
    查询算子（Query Domain 定义）:
      query_rewrite, intent_detect, term_expand, hyde_generate
      vector_search, keyword_search, metadata_filter
      rrf_fusion, linear_fusion, rerank, deduplicate
      context_assemble, context_compress
      prompt_build, llm_generate
      citation_extract, safety_filter, format_convert
```

### Query Pipeline：两种设计方式

Query 路径的执行流有两种设计方式。**Phase 1 用 Pipeline Config，Phase 2/3 按需升级到 DAG Engine。**

#### 决策表：何时用哪种

| 维度 | Pipeline Config（Phase 1） | DAG Engine（Phase 2/3） |
|------|---------------------------|------------------------|
| 拓扑 | 固定，不可变 | 自定义，可任意组合 |
| 算子启用 | enable/disable 开关 | 动态添加/移除节点 |
| 并行 | 固定的 search 阶段 3 路并行 | 任意节点可并行 |
| 分支 | 不支持条件跳转 | 支持 onFailure / condition 跳转 |
| 环路 | 不支持 | 支持（如 fact_check 失败 → 二次检索） |
| Agent | 不支持 LLM 自主决策 | 支持（LLM 节点决定下个节点） |
| 配置复杂度 | 1 层 JSON，约 30 行 | 多层嵌套 Graph，约 100+ 行 |
| 实施成本 | 0 行 DAG 引擎代码 | ~2000 行拓扑排序/调度/降级代码 |
| 适用场景 | "80% 的 Service 用标准流程" | "特殊 Service 定制复杂流程" |

#### 设计一：Pipeline Config（Phase 1）

**核心思路**：执行顺序固定，用户只控制每个阶段的 `enabled` 和参数。

```
固定执行拓扑（不可改变）：

  query_rewrite ──▶ ┌─ vector_search ──┐
                    ├─ keyword_search ──┤──▶ fusion ──▶ rerank? ──▶ context ──▶ llm ──▶ postprocess
                    └─ metadata_filter ─┘
    (可选)               (可独立启用/禁用)          (可选)    (策略/预算)  (模型/参数)  (安全/引用)
```

**配置结构**（存储为 `service_query_pipeline` JSONB）：

```json
{
  "preprocessing": {
    "query_rewrite":  { "enabled": true,  "variants": 3 },
    "hyde_generate":  { "enabled": false },
    "intent_detect":  { "enabled": false }
  },
  "search": {
    "vector_search":   { "enabled": true,  "topK": 20,  "similarity": "cosine" },
    "keyword_search":  { "enabled": true,  "topK": 10 },
    "metadata_filter": { "enabled": false, "rules": [] }
  },
  "fusion": {
    "strategy": "rrf",
    "rrf_k": 60
  },
  "rerank": {
    "enabled": true,
    "topK": 5,
    "model": "bge-reranker-v2",
    "apiUrl": "http://rerank.internal:8080/rerank"
  },
  "context": {
    "strategy": "top_n",
    "tokenBudget": 4096,
    "includeCitations": true
  },
  "generation": {
    "model": "gpt-4o",
    "temperature": 0.3,
    "maxTokens": 1024,
    "fallbackModel": "gpt-4o-mini",
    "retry": { "maxRetries": 2, "backoff": "exponential" }
  },
  "postprocessing": {
    "citation_extract": { "enabled": true },
    "safety_filter":    { "enabled": true,  "piiDetection": true }
  }
}
```

**执行引擎**：不需要单独的 DAG 执行器，一个 `QueryPipelineExecutor`（约 200 行代码）即可：

```java
// Case 层
public class QueryPipelineCase implements IQueryPipelineCase {

    public RAGResponse execute(SearchRequest request, ServicePipelineConfig config) {
        // 1. Preprocessing（线性）
        Query rewritten = config.preprocessing.query_rewrite.enabled
            ? queryRewriteService.rewrite(request.query, config.preprocessing.query_rewrite.variants)
            : request.query;

        // 2. Search（并行）
        List<CompletableFuture<List<Chunk>>> futures = new ArrayList<>();
        if (config.search.vector_search.enabled)
            futures.add(async(() -> vectorSearchService.search(rewritten, config.search.vector_search)));
        if (config.search.keyword_search.enabled)
            futures.add(async(() -> keywordSearchService.search(rewritten, config.search.keyword_search)));
        if (config.search.metadata_filter.enabled)
            futures.add(async(() -> metadataFilterService.filter(request.filters)));

        // 3. Fusion
        List<Chunk> fused = fusionService.fuse(
            futures.stream().map(CompletableFuture::join).toList(),
            config.fusion);

        // 4. Rerank (optional)
        List<Chunk> ranked = config.rerank.enabled
            ? rerankService.rerank(fused, config.rerank)
            : fused;

        // 5. Context + 6. Generate + 7. Postprocess
        String context = contextAssembleService.assemble(ranked, config.context);
        String answer = llmService.generate(context, rewritten, config.generation);
        return postProcessService.process(answer, ranked, config.postprocessing);
    }
}
```

**不需要 DAG 执行器的理由**：
- 拓扑固定，不需要拓扑排序
- 并行点固定（只有 search 阶段），不需要通用调度器
- 没有条件跳转，不需要动态路由
- 总共约 200 行编排代码，比引入一个 DAG 引擎（~2000 行）便宜 10 倍

#### 设计二：DAG Engine（Phase 2/3，按需引入）

**触发条件**：以下任一场景出现时引入 DAG 引擎：

1. **条件跳转**：fact_check 失败 → 回退到 search → 重新生成
2. **多 Service 需要不同拓扑**：客服 bot 和代码审查 bot 的流程完全不同
3. **Agentic RAG**：LLM 自主决策是否检索、检索什么、检索几次

**数据模型**（位于 `types/`）：

```
  GraphDefinition = { id, serviceId, nodes: NodeConfig[], edges: EdgeDefinition[] }

  NodeConfig = {
    nodeId: string,
    operatorType: OperatorType,    // 对应 Domain 层的算子 Bean 名称
    config: OperatorConfig,        // 算子特定参数
    deps: string[],                // 依赖节点 ID 列表
    timeout: ms,
    onFailure: skip | error | pass_through
  }

  EdgeDefinition = { from: nodeId, to: nodeId }
```

**DAG 执行引擎**（位于 `infrastructure/dag/`）：

```
  infrastructure/dag/
  ├── GraphExecutor         — 拓扑排序 + 调度执行
  ├── TopologicalSorter     — 依赖解析，检测环路
  ├── NodeScheduler         — 无依赖节点并行执行
  ├── DegradationHandler    — 超时 / 失败降级策略
  └── SchemaValidator       — 上下游算子类型兼容校验
```

**架构关系**：

```
  types/  ←  infrastructure/dag/  (只依赖数据结构)
  domain/query/service/  算子策略接口  (不依赖 DAG)
  case/  桥接: 读 GraphDefinition → resolve Domain Bean → GraphExecutor.execute()
```

### Index 路径的编排

Index 路径 100% 线性，**不需要 DAG 也不需要 Pipeline Config**，直接由 Kafka 事件链驱动：

```
  doc.imported → doc.parsed → doc.chunked → doc.embedded → doc.indexed
```

Kafka Consumer Group 天然提供：顺序保证、失败重试、断点续跑、水平扩展。不需要额外的编排层。

### Operator Categories（Domain 层算子策略接口）

算子分为两类，由各自 Domain 定义：

```
  Index Domain 算子 (domain/index/service/):
    parse(IParserStrategy), clean(IDocumentCleaner),
    chunk(IChunkingStrategy), embed(IEmbeddingService),
    index_write(IIndexWriter)

  Query Domain 算子 (domain/query/service/):
    预处理:   query_rewrite, intent_detect, hyde_generate
    检索:     vector_search, keyword_search, metadata_filter
    融合:     rrf_fusion, linear_fusion
    排序:     rerank, deduplicate
    上下文:   context_assemble, context_compress
    生成:     prompt_build, llm_generate
    后处理:   citation_extract, safety_filter, format_convert
```

### 渐进式演进路线

```
  Phase 1 (现在)
  ──────────────
  Index: Kafka 事件链（零额外代码）
  Query: Pipeline Config（约 200 行 PipelineExecutor）
  存储:  service_query_pipeline JSONB 列

          ↓  当有 Service 需要自定义拓扑时

  Phase 2 (按需)
  ──────────────
  引入 infrastructure/dag/
  Query Path 增加 GraphExecutor 能力
  存储:  service_query_pipeline 可以是 PipelineConfig 或 GraphDefinition
        （通过 type 字段区分: "pipeline" | "dag"）

          ↓  当有 LLM 自主决策场景时

  Phase 3 (Agentic RAG)
  ──────────────
  DAG 支持 LLM 节点的 "next" 输出
  支持环路（检索 → 生成 → 校验 → 检索...）
```

## Storage Model

```
  ─── Index Domain 拥有的表 ───
  raw_documents       — KB 级共享，原始文档 + 解析后的 CanonicalDocument (ContentTree JSONB)
  service_chunks      — Service 级独立，按 Service 的分块配置切割
  service_vectors     — Service 级独立 (pgvector)，按 Service 的 embedding 模型向量化

  ─── Query Domain 读取的表 ───
  (query 通过 index 的 repository 接口访问上述表，不直接拥有存储)

  ─── Service 配置表（存储为 JSONB，由 Case 层加载后组装 DAG）───
  service_index_graph  — Service 级，索引 DAG 图定义（parse→chunk→embed 编排）
  service_query_graph  — Service 级，查询 DAG 图定义（rewrite→search→rerank→generate 编排）
  service_prompt_templates — Service 级，Prompt 模板配置

  ─── Platform 级表 ───
  services            — Service 定义
  knowledge_bases     — KB 定义
  service_kb_grants   — KB↔Service 授权
```

## Prompt Template System

```
  Layer 1: 平台预置模板   — 通用场景模板（客服、文档总结、代码审查...）
  Layer 2: Service 自定义 — 继承预置 + 覆盖字段 + 自定义变量
  Layer 3: API 调用覆盖   — 每次请求可临时覆盖部分参数
```
