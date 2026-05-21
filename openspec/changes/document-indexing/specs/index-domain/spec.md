# Spec: Index Domain — Document Processing

## Overview

Index 域负责 RAG 系统的**文档处理管道**：文件导入 → 多格式解析 → CanonicalDocument → 清洗 → 分块 → 向量化 → 索引写入（pgvector + Elasticsearch）。DataSource 多源接入与 SchemaIndex 部分见 datasource-ingestion change；知识图谱抽取部分见 knowledge-graph change。

## 1. 文档导入

- **REQ-DP-001**: 支持文件上传导入，首期支持 PDF、Markdown、HTML、DOCX、TXT 格式
- **REQ-DP-002**: 支持 API 直接传入文本内容导入（纯文本/JSON/Markdown）
- **REQ-DP-003**: 支持批量导入（一次上传多个文件）
- **REQ-DP-004**: 支持文件夹上传（.zip 或递归目录），自动识别内部文件类型并分别解析
- **REQ-DP-005**: 文件类型通过 mimeType + 扩展名 + 内容探测（probe）三重检测

## 2. 文档解析

- **REQ-DP-010**: 解析器采用策略模式（IParserStrategy），通过 ParserRegistry 根据 SourceType 和 mimeType 路由
- **REQ-DP-011**: 所有解析器产出一致的 CanonicalDocument 格式（ContentTree）
- **REQ-DP-012**: ContentTree 中每个节点保留溯源定位（sourcePointer）：PDF 页码、HTML CSS Selector、JSON Pointer 等
- **REQ-DP-013**: ContentTree 中每个节点预渲染 markdown 和 plainText 两份文本：markdown 用于 embedding，plainText 用于 BM25
- **REQ-DP-014**: 解析失败需输出明确的错误信息（页码/行号/原因）
- **REQ-DP-015**: 解析器支持 probe() 方法进行文件内容自动检测

## 3. 文档清洗

- **REQ-DP-020**: 自动去除文档噪音（页眉页脚/水印/HTML导航栏），噪音标记在 ContentNode 生成阶段完成
- **REQ-DP-021**: 支持按规则配置清洗策略（正则匹配删除/替换模式），清洗规则在 Service 级别配置

## 4. 分块策略

- **REQ-DP-030**: 分块策略操作 ContentTree，按节点类型和标题层级决策分块边界。TABLE 和 CODE 保持完整不被拆分
- **REQ-DP-031**: 按文档结构特征自动适配：标题层级丰富 → HeadingChunkStrategy / Q&A 模式 → FAQChunkStrategy / 否则 → FixedSizeChunkStrategy
- **REQ-DP-032**: 支持 Service 级别配置 chunk_size 和 chunk_overlap
- **REQ-DP-033**: 分块结果保留结构元数据（标题路径/页码/structurePath/sourcePointer 列表）

## 5. 向量化与索引写入

### 向量化

- **REQ-DP-040**: 支持配置 embedding 模型（Service 级别）
- **REQ-DP-041**: 批量 embedding 支持并发 + 失败重试
- **REQ-DP-042**: 增量更新时仅向量化变更的 chunks

### 索引写入

- **REQ-IDX-001**: Chunk.markdown → Embedding API → 向量写入 pgvector
- **REQ-IDX-002**: Chunk.plainText → ES BM25 (按 Service 物理隔离：`idx_{serviceId}_chunks`)
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
