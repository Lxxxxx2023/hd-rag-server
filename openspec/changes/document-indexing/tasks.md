# Tasks: Document Indexing

## Schema: spec-driven | Progress: 0/26 tasks

---

## 1. TextSegment Model (4 tasks)

- [ ] DP-001 定义 SegmentType 枚举（HEADING, PARAGRAPH, TABLE, CODE, LIST, IMAGE, QUOTE）
- [ ] DP-002 定义 TextSegment 值对象 — type/level/text/sourcePointer
- [ ] DP-003 定义 ParseResult 聚合 — sourceType + mimeType + metadata + List<TextSegment>
- [ ] DP-004 定义 Document 聚合根与状态机（uploaded → parsing → parsed → chunking → chunked → embedding → ready）

## 2. Parser Strategy (3 tasks)

- [ ] DP-005 定义 IParserStrategy 策略接口 — canHandle(sourceType, mimeType) + parse(sourceInput) → List<TextSegment> + probe(sourceInput)
- [ ] DP-006 定义 ParserRegistry 策略路由 — 精确 mimeType → SourceType 兜底 → probe 自动检测
- [ ] DP-007 定义文档清洗规则配置值对象 — CleaningRule（正则删除/替换/段落类型过滤）

## 3. Chunking (2 tasks)

- [ ] DP-008 定义 ChunkConfig 值对象 — chunkSize(默认512) + chunkOverlap(默认50)
- [ ] DP-009 实现 ChunkingService — 自动路由：segments 含 HEADING 时按标题边界分块（同 section 合并，超长滑动窗口切分），无 HEADING 时按 token 滑动窗口分块（段落边界优先）；TABLE/CODE 保持完整不拆分

## 4. Chunk Entity (1 task)

- [ ] DP-010 定义 Chunk 实体 — text + headingPath + sourcePointers + metadata

## 5. Infrastructure — Parsers (5 tasks)

- [ ] DP-011 实现 PdfParser 适配器 — PDFBox 按页提取纯文本，每页输出为 PARAGRAPH segment
- [ ] DP-012 实现 HtmlParser 适配器 — Jsoup 解析 HTML，按 DOM 标签识别 HEADING/TABLE/CODE/PARAGRAPH
- [ ] DP-013 实现 MarkdownParser 适配器 — flexmark 解析，按语法识别 HEADING/TABLE/CODE/LIST/PARAGRAPH
- [ ] DP-014 实现 DocxParser 适配器 — Apache POI 解析，按 Word 样式识别 HEADING/TABLE/CODE/PARAGRAPH
- [ ] DP-015 实现 PlainTextParser 适配器 — 按空行分段，全部输出为 PARAGRAPH segment

## 6. Infrastructure — Cleaning (1 task)

- [ ] DP-016 实现文档清洗服务 — 遍历 List<TextSegment> 按规则过滤/替换噪音段落

## 7. Infrastructure — Persistence & Embedding (3 tasks)

- [ ] DP-017 实现 EmbeddingService — 批量并发 + 失败重试
- [ ] DP-018 实现 DocumentRepository 及 MyBatis 持久化
- [ ] DP-019 实现 ChunkRepository — Service 级 chunk 存储

## 8. Infrastructure — Index Storage (4 tasks)

- [ ] DP-020 实现 pgvector 向量索引管理 (IVFFlat) 与写入适配器
- [ ] DP-021 实现 Elasticsearch 索引管理 — Chunk.text → ES BM25，按 Service 物理隔离
- [ ] DP-022 实现批量索引写入 — pgvector + ES 批量操作，单次最多 100 条
- [ ] DP-023 实现索引写入失败重试 — chunk 标记 failed，自动重试最多 3 次

## 9. Processing Pipeline (3 tasks)

- [ ] DP-024 实现 IndexPipeline 编排器（Case 层）— import→parse→clean→chunk→embed→index_write，状态存 DB
- [ ] DP-025 实现异步 Worker（@Async + ApplicationEvent）— 消费 DocImportedEvent，调用 IndexPipeline
- [ ] DP-026 实现文件夹导入 Case（FolderImportCase）— 递归遍历 + probe + 路由解析器

---

## Task Summary

| 模块 | 内容 | Tasks |
|------|------|-------|
| TextSegment Model | TextSegment / ParseResult / Document | DP-001 ~ DP-004 (4) |
| Parser Strategy | IParserStrategy + Registry + CleaningRule | DP-005 ~ DP-007 (3) |
| Chunking | ChunkConfig + ChunkingService（自动路由） | DP-008 ~ DP-009 (2) |
| Chunk Entity | Chunk 实体定义 | DP-010 (1) |
| Parsers | PDF/HTML/MD/DOCX/TXT 适配器 | DP-011 ~ DP-015 (5) |
| Cleaning | 段落级噪音清理 | DP-016 (1) |
| Embedding | 批量向量化 + Repository | DP-017 ~ DP-019 (3) |
| Index Storage | pgvector + ES 双写 | DP-020 ~ DP-023 (4) |
| Pipeline | 编排器 + 异步 + 文件夹导入 | DP-024 ~ DP-026 (3) |
| **Total** | | **26** |

## Implementation Order

1. DP-001~004 → TextSegment 模型（数据基础）
2. DP-005~007 → 解析器策略接口与路由
3. DP-008~010 → 分块服务与 Chunk 实体
4. DP-011~016 → 解析器实现 + 清洗
5. DP-017~019 → Embedding + Repository
6. DP-020~023 → 索引写入
7. DP-024~026 → 编排器 + 异步
