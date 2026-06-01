# Tasks: Document Indexing

## Schema: spec-driven | Progress: 0/27 tasks

---

## 1. TextSegment Model (4 tasks)

- [ ] DP-001 定义 SegmentType 枚举（HEADING, PARAGRAPH, TABLE, CODE, LIST, IMAGE, QUOTE）
- [ ] DP-002 定义 TextSegment 值对象 — text/type/level/sourcePointer/metadata
- [ ] DP-003 定义 ParseResult 聚合 — sourceType + mimeType + metadata + List<TextSegment>
- [ ] DP-004 定义 Document 聚合根与状态机（uploaded → parsing → parsed → chunking → chunked → embedding → ready）

## 2. Parser Strategy (3 tasks)

- [ ] DP-005 定义 IParserStrategy 策略接口 — canHandle(sourceType, mimeType) + parse(sourceInput) → List<TextSegment> + probe(sourceInput)
- [ ] DP-006 定义 ParserRegistry 策略路由 — 精确 mimeType → SourceType 兜底 → probe 自动检测
- [ ] DP-007 定义文档清洗规则配置值对象 — CleaningRule（正则删除/替换/段落类型过滤）

## 3. Chunking (3 tasks)

- [ ] DP-008 定义 IChunkingStrategy 分块策略接口 — chunk(List<TextSegment>, ChunkConfig) → List<Chunk>
- [ ] DP-009 实现 HeadingBoundaryChunkStrategy — 以 HEADING segment 为分块边界，同 section 内 segments 合并，超长按 token 滑动窗口切分
- [ ] DP-010 实现 FixedSizeChunkStrategy — 无 heading 时按 token 计数分块，段落边界优先

## 4. Chunk Entity (1 task)

- [ ] DP-011 定义 Chunk 实体 — text + headingPath + sourcePointers + metadata

## 5. Infrastructure — Parsers (5 tasks)

- [ ] DP-012 实现 PdfParser 适配器 — PDFBox 按页提取文本 → List<TextSegment>（每页为 PARAGRAPH segment）
- [ ] DP-013 实现 HtmlParser 适配器 — Jsoup 解析 HTML → List<TextSegment>
- [ ] DP-014 实现 MarkdownParser 适配器 — flexmark 解析 Markdown → List<TextSegment>
- [ ] DP-015 实现 DocxParser 适配器 — Apache POI 解析 DOCX → List<TextSegment>
- [ ] DP-016 实现 PlainTextParser 适配器 — 纯文本按段落分割

## 6. Infrastructure — Cleaning (1 task)

- [ ] DP-017 实现文档清洗服务 — 遍历 List<TextSegment> 按规则过滤/替换噪音段落

## 7. Infrastructure — Persistence & Embedding (3 tasks)

- [ ] DP-018 实现 EmbeddingService — 批量并发 + 失败重试
- [ ] DP-019 实现 DocumentRepository 及 MyBatis 持久化
- [ ] DP-020 实现 ChunkRepository — Service 级 chunk 存储

## 8. Infrastructure — Index Storage (4 tasks)

- [ ] DP-021 实现 pgvector 向量索引管理 (IVFFlat) 与写入适配器
- [ ] DP-022 实现 Elasticsearch 索引管理 — Chunk.text → ES BM25，按 Service 物理隔离
- [ ] DP-023 实现批量索引写入 — pgvector + ES 批量操作，单次最多 100 条
- [ ] DP-024 实现索引写入失败重试 — chunk 标记 failed，自动重试最多 3 次

## 9. Processing Pipeline (3 tasks)

- [ ] DP-025 实现 IndexPipeline 编排器（Case 层）— import→parse→clean→chunk→embed→index_write，状态存 DB
- [ ] DP-026 实现异步 Worker（@Async + ApplicationEvent）— 消费 DocImportedEvent，调用 IndexPipeline
- [ ] DP-027 实现文件夹导入 Case（FolderImportCase）— 递归遍历 + probe + 路由解析器

---

## Task Summary

| 模块 | 内容 | Tasks |
|------|------|-------|
| TextSegment Model | TextSegment / ParseResult / Document | DP-001 ~ DP-004 (4) |
| Parser Strategy | IParserStrategy + Registry + CleaningRule | DP-005 ~ DP-007 (3) |
| Chunking | IChunkingStrategy 2 种实现 | DP-008 ~ DP-010 (3) |
| Chunk Entity | Chunk 实体定义 | DP-011 (1) |
| Parsers | PDF/HTML/MD/DOCX/TXT 适配器 | DP-012 ~ DP-016 (5) |
| Cleaning | 段落级噪音清理 | DP-017 (1) |
| Embedding | 批量向量化 + Repository | DP-018 ~ DP-020 (3) |
| Index Storage | pgvector + ES 双写 | DP-021 ~ DP-024 (4) |
| Pipeline | 编排器 + 异步 + 文件夹导入 | DP-025 ~ DP-027 (3) |
| **Total** | | **27** |

## Implementation Order

1. DP-001~004 → TextSegment 模型（数据基础）
2. DP-005~007 → 解析器策略接口与路由
3. DP-008~011 → 分块策略与 Chunk 实体
4. DP-012~017 → 解析器实现 + 清洗
5. DP-018~020 → Embedding + Repository
6. DP-021~024 → 索引写入
7. DP-025~027 → 编排器 + 异步
