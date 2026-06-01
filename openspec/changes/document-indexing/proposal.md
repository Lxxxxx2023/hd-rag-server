# Proposal: Document Indexing

## Summary

实现 RAG 平台的文档处理管道（写路径）。核心链路：文件上传 → 多格式解析（PDF/HTML/Markdown/DOCX/TXT）→ 统一 TextSegment 列表 → 清洗 → 按 Service 配置分块 → 向量化 → 写入索引（pgvector + Elasticsearch）。这是 RAG 平台的数据基础，所有检索能力依赖此模块产出的索引数据。

## User Stories

### 知识库运营

- **US-1**: 作为**知识库运营人员**，我想上传文档后，系统自动完成**解析 → 清洗 → 分块 → 向量化 → 写入索引**，以便我无需关心底层处理流程。
- **US-2**: 作为**知识库运营人员**，我想查看文档处理进度（已处理 chunks / 总 chunks），以便了解索引进度。
- **US-3**: 作为**知识库运营人员**，我想导入整个文件夹（含嵌套子目录），系统自动识别文件类型并路由到对应解析器，以便批量上传存量文档。

## User Actions（关键操作路径）

### 路径 A：单文档索引

```
运营上传文件 → mimeType+扩展名+probe 三重检测 → ParserRegistry 路由解析器
→ 解析为 List<TextSegment>（扁平语义段落列表）→ 清洗噪音段落
→ 按 heading 边界 + token 限制分块 → 批量向量化 (Embedding API)
→ 双写索引 (pgvector + ES BM25) → Document 状态 = READY → 发布 document.indexed 事件
```

### 路径 B：文件夹批量导入

```
运营上传文件夹 → FolderImportCase 递归遍历 → 对每个文件: probe 检测类型
→ 路由到 PdfParser/HtmlParser/MarkdownParser/DocxParser/PlainTextParser
→ 各文件独立走路径 A 的处理管道 → 全部完成后汇总处理统计
```

## Motivation

当前各业务线独立建设文档处理能力，解析器、分块策略、向量化逻辑重复实现。本模块将文档到索引的全链路标准化，产出统一的 TextSegment 中间格式和分块策略体系，为上层检索提供高质量的数据基础。

## Core Design Decisions

| # | 决策 | 说明 |
|---|------|------|
| 1 | TextSegment 扁平列表作为统一中间格式 | 放弃树形 ContentTree，改为扁平的 `List<TextSegment>` 列表。每个 segment 是一个独立语义单元（标题/段落/表格/代码块等），携带类型、层级、溯源信息。**理由**：树结构对 PDF 等非结构化文档适配成本高且收益低，切块时仍需展平；扁平列表降低解析器实现门槛，切块逻辑变为线性扫描 |
| 2 | 分块基于 heading 边界 + token 限制 | heading 类型的 segment 作为天然分块边界；同一 section 内的 segments 合并，超长则按 token 滑动窗口切分；TABLE/CODE segment 保持完整不拆分 |
| 3 | 策略模式路由解析器 | ParserRegistry 按 mimeType → SourceType → probe() 优先级路由 |
| 4 | 编排器负责流程，DB 负责状态 | 处理管道由 Case 层编排器直接调用，状态存 DB。Kafka 仅发布 document.indexed 业务事件 |
| 5 | Workload 路由：入口线程判断 | HTTP 请求线程 → 小文件(< 10MB)同步/大文件异步；定时任务/Webhook → 一律异步 |
| 6 | 双索引写入 | Chunk.markdown → Embedding → pgvector；Chunk.plainText → ES BM25 |
| 7 | 增量更新检测 | 文档更新时自动检测变更范围，仅重处理受影响的 chunks |

## Non-goals

- 不实现 DataSource 连接器（见 datasource-ingestion change）
- 不实现图片多模态 Embedding（IMAGE 节点保留 OCR 文本）
- 不实现对话类数据源的结构化建模

## Dependencies

- [platform-foundation](../platform-foundation/proposal.md) — 依赖 Service/KB 资源模型、raw_documents 表
