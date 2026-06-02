# Spec: Index Domain — Document Processing

## Overview

Index 域负责 RAG 系统的**文档处理管道**：文件导入 → 多格式解析 → TextSegment 列表 → 清洗 → 分块 → 向量化 → 索引写入（pgvector + Elasticsearch）。DataSource 多源接入与 SchemaIndex 部分见 datasource-ingestion change；知识图谱抽取部分见 knowledge-graph change。

## 1. 文档导入

- **REQ-DP-001**: 支持文件上传导入，首期支持 PDF、Markdown、HTML、DOCX、TXT 格式
- **REQ-DP-002**: 支持 API 直接传入文本内容导入（纯文本/JSON/Markdown）
- **REQ-DP-003**: 支持批量导入（一次上传多个文件）
- **REQ-DP-004**: 支持文件夹上传（.zip 或递归目录），自动识别内部文件类型并分别解析
- **REQ-DP-005**: 文件类型通过 mimeType + 扩展名 + 内容探测（probe）三重检测

## 2. 文档解析

- **REQ-DP-010**: 解析器采用策略模式（IParserStrategy），通过 ParserRegistry 根据 SourceType 和 mimeType 路由
- **REQ-DP-011**: 所有解析器产出 `List<TextSegment>` 扁平列表。每个 TextSegment 含 type（SegmentType 枚举）、level（heading 层级）、text（文本内容）、sourcePointer（溯源定位）
- **REQ-DP-012**: 解析器按格式能力尽力标注类型：Markdown/HTML/DOCX 识别 HEADING/TABLE/CODE/LIST/PARAGRAPH；PDF/TXT 仅输出 PARAGRAPH 类型。不强求所有格式达到同一结构化水平
- **REQ-DP-013**: 每个 TextSegment 保留溯源定位（sourcePointer）：PDF 页码、Markdown 行号、HTML CSS Selector 等
- **REQ-DP-014**: 解析失败需输出明确的错误信息（页码/行号/原因）
- **REQ-DP-015**: 解析器支持 probe() 方法进行文件内容自动检测

## 3. 文档清洗

- **REQ-DP-020**: 自动去除文档噪音（页眉页脚/水印/HTML导航栏），在 TextSegment 列表上按规则过滤
- **REQ-DP-021**: 支持按规则配置清洗策略（正则匹配删除/替换模式），清洗规则在 Service 级别配置

## 4. 分块策略

- **REQ-DP-030**: 分块由统一的 ChunkingService 完成，内部根据 segments 是否包含 HEADING 类型自动路由：有 heading → 按标题边界分块；无 heading → 按 token 滑动窗口分块。策略选择对用户透明
- **REQ-DP-031**: Heading 路径：以 HEADING segment 为分块边界，同 section 内 segments 合并为一个 chunk，heading 文本作为 chunk 前缀；超过 token 限制时按滑动窗口再切分
- **REQ-DP-032**: Fixed-size 路径：全部 segments 文本拼接，按 chunk_size 滑动窗口切分，步长 = chunk_size - chunk_overlap，优先在段落边界切分
- **REQ-DP-033**: TABLE 和 CODE 类型 segment 保持完整不被拆分（即使超长也作为独立 chunk）
- **REQ-DP-034**: 支持 Service 级别配置 chunk_size（token 数，默认 512）和 chunk_overlap（默认 50）
- **REQ-DP-035**: 分块结果保留结构元数据（所属 heading 标题路径、sourcePointer 列表）

## 5. 向量化与索引写入

### 向量化

- **REQ-DP-040**: 支持配置 embedding 模型（Service 级别）
- **REQ-DP-041**: 批量 embedding 支持并发 + 失败重试
- **REQ-DP-042**: 增量更新时仅向量化变更的 chunks

### 索引写入

- **REQ-IDX-001**: Chunk.text → Embedding API → 向量写入 pgvector
- **REQ-IDX-002**: Chunk.text → ES BM25 (按 Service 物理隔离：`idx_{serviceId}_chunks`)
- **REQ-IDX-003**: pgvector 和 ES 写入支持批量操作，单次最多 100 条
- **REQ-IDX-004**: 索引写入失败时标记 chunk 状态为 failed，支持自动重试（最多 3 次）

## 6. 处理状态与增量更新

- **REQ-DP-050**: 文档处理状态机：uploaded → parsing → parsed → chunking → chunked → embedding → ready
- **REQ-DP-051**: 每个状态节点可标记 failed，支持部分重试和断点恢复
- **REQ-DP-052**: 处理进度可查询（已处理 chunks / 总 chunks）
- **REQ-DP-053**: 文档更新时自动检测变更范围，仅重处理受影响的 chunks
- **REQ-DP-054**: 文档删除时级联清理关联的 chunks、vectors

## 7. 索引存储引擎

- **REQ-INF-001**: pgvector 作为主向量检索引擎，维度支持 1536 (3-small) 和 3072 (3-large)
- **REQ-INF-002**: 向量索引使用 IVFFlat，后期按需切换 HNSW
- **REQ-INF-003**: ES 只负责 BM25 关键词检索，不承担向量检索；中文分词 ik_max_word
- **REQ-INF-004**: 预留 IVectorRepository 抽象接口，支持后续 Milvus 切换

## 8. Index OperatorType

```
文档处理:
  parse, clean, chunk, embed, index_write
```
