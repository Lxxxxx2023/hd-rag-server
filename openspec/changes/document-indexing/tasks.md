# Tasks: Document Indexing

## Schema: spec-driven | Progress: 0/31 tasks

---

## 1. Canonical Document Model (5 tasks)

- [ ] DP-001 定义 SourceType 枚举（WEB, FILE, DATABASE, API, CRAWL）与 NodeType 枚举（SECTION, PARAGRAPH, TABLE, LIST, CODE, IMAGE, QUOTE）
- [ ] DP-002 定义 ContentNode 实体 — 树形结构节点，含 type/heading/headingLevel/markdown/plainText/children
- [ ] DP-003 定义 SourcePosition 值对象 — 多源溯源定位（pageNumber, lineStart/End, sourcePointer）
- [ ] DP-004 定义 CanonicalDocument 聚合根 — metadata + ContentTree + statistics
- [ ] DP-005 定义 Document 聚合根与状态机（uploaded → parsing → parsed → chunking → chunked → embedding → ready）

## 2. Parser Strategy (3 tasks)

- [ ] DP-006 定义 IParserStrategy 策略接口 — canHandle(sourceType, mimeType) + parse(sourceInput) + probe(sourceInput)
- [ ] DP-007 定义 ParserRegistry 策略路由 — 精确 mimeType → SourceType 兜底 → probe 自动检测
- [ ] DP-008 定义文档清洗规则配置值对象 — CleaningRule（正则删除/替换/块类型过滤/位置过滤）

## 3. Chunking (5 tasks)

- [ ] DP-009 定义 IChunkingStrategy 分块策略接口 — chunk(CanonicalDocument, ChunkConfig) → List<Chunk>
- [ ] DP-010 实现 HeadingChunkStrategy — 按 SECTION 标题层级分块
- [ ] DP-011 实现 FAQChunkStrategy — 按 Q&A 模式检测分块
- [ ] DP-012 实现 FixedSizeChunkStrategy — token 计数超限时分块，段落边界优先
- [ ] DP-013 实现 AutoChunkStrategy — 根据 ContentTree 结构特征自动选择策略

## 4. Chunk Entity (1 task)

- [ ] DP-014 定义 Chunk 实体 — 保留 structurePath、sourcePositions 列表等元数据

## 5. Infrastructure — Parsers (5 tasks)

- [ ] DP-015 实现 PdfParser 适配器 — PDFBox 解析 PDF → CanonicalDocument
- [ ] DP-016 实现 HtmlParser 适配器 — Jsoup 解析 HTML → CanonicalDocument
- [ ] DP-017 实现 MarkdownParser 适配器 — flexmark 解析 Markdown → CanonicalDocument
- [ ] DP-018 实现 DocxParser 适配器 — Apache POI 解析 DOCX → CanonicalDocument
- [ ] DP-019 实现 PlainTextParser 适配器 — 纯文本兜底解析

## 6. Infrastructure — Cleaning (1 task)

- [ ] DP-020 实现文档清洗服务 — 遍历 ContentTree 标记/删除噪音节点

## 7. Infrastructure — Persistence & Embedding (3 tasks)

- [ ] DP-021 实现 EmbeddingService — 批量并发 + 失败重试
- [ ] DP-022 实现 DocumentRepository 及 MyBatis 持久化
- [ ] DP-023 实现 ChunkRepository — Service 级 chunk 存储

## 8. Infrastructure — Index Storage (4 tasks)

- [ ] DP-024 实现 pgvector 向量索引管理 (IVFFlat) 与写入适配器
- [ ] DP-025 实现 Elasticsearch 索引管理 — Chunk.plainText → ES BM25，按 Service 物理隔离
- [ ] DP-026 实现批量索引写入 — pgvector + ES 批量操作，单次最多 100 条
- [ ] DP-027 实现索引写入失败重试 — chunk 标记 failed，自动重试最多 3 次

## 9. Processing Pipeline (4 tasks)

- [ ] DP-028 实现 IndexPipeline 编排器（Case 层）— import→parse→clean→chunk→embed→index_write，状态存 DB
- [ ] DP-029 实现异步 Worker（@Async + ApplicationEvent）— 消费 DocImportedEvent，调用 IndexPipeline
- [ ] DP-030 实现文件夹导入 Case（FolderImportCase）— 递归遍历 + probe + 路由解析器
- [ ] DP-031 实现增量更新检测 — 仅重处理受影响的 chunks

---

## Task Summary

| 模块 | 内容 | Tasks |
|------|------|-------|
| Canonical Model | ContentTree / Document / SourcePosition | DP-001 ~ DP-005 (5) |
| Parser Strategy | IParserStrategy + Registry + CleaningRule | DP-006 ~ DP-008 (3) |
| Chunking | IChunkingStrategy 4 种实现 | DP-009 ~ DP-013 (5) |
| Chunk Entity | Chunk 实体定义 | DP-014 (1) |
| Parsers | PDF/HTML/MD/DOCX/TXT 适配器 | DP-015 ~ DP-019 (5) |
| Cleaning | ContentTree 噪音清理 | DP-020 (1) |
| Embedding | 批量向量化 + Repository | DP-021 ~ DP-023 (3) |
| Index Storage | pgvector + ES 双写 | DP-024 ~ DP-027 (4) |
| Pipeline | 编排器 + 异步 + 文件夹导入 + 增量 | DP-028 ~ DP-031 (4) |
| **Total** | | **31** |

## Implementation Order

1. DP-001~005 → Canonical Document 模型（数据基础）
2. DP-006~008 → 解析器策略接口与路由
3. DP-009~014 → 分块策略与实体
4. DP-015~020 → 解析器实现 + 清洗
5. DP-021~023 → Embedding + Repository
6. DP-024~027 → 索引写入
7. DP-028~031 → 编排器 + 异步 + 增量
