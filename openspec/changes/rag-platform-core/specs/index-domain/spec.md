# Spec: Index Domain (写路径)

## Overview

Index 域负责 RAG 系统的**写路径**：数据从外部进入系统，经过解析、清洗、分块、向量化，最终写入索引（pgvector + Elasticsearch）。它是数据的拥有者，Query 域通过其 Repository 接口读取数据。

核心职责：
- 多源数据摄入与格式检测
- 统一解析为 CanonicalDocument（ContentTree）
- 文档清洗与噪音过滤
- 按 Service 配置分块
- 向量化并写入索引
- 索引的生命周期管理（增量更新、级联删除）

## Requirements

### 文档导入

- **REQ-DP-001**: 支持文件上传导入，首期支持 PDF、Markdown、HTML、DOCX、TXT 格式，后期可扩展到 10+ 格式（Excel、JSON、代码文件、图片OCR 等）
- **REQ-DP-002**: 支持 API 直接传入文本内容导入（纯文本/JSON/Markdown）
- **REQ-DP-003**: 支持批量导入（一次上传多个文件）
- **REQ-DP-020**: 支持文件夹上传（.zip 或递归目录），自动识别内部文件类型并分别解析，文件夹路径保留为文档元数据
- **REQ-DP-021**: 文件类型通过 mimeType + 扩展名 + 内容探测（probe）三重检测，不依赖单一判断方式

### 文档解析

- **REQ-DP-004**: 解析器采用策略模式（IParserStrategy），通过 ParserRegistry 根据 SourceType 和 mimeType 路由到对应解析器，新增格式只需注册新策略
- **REQ-DP-005**: 所有解析器产出一致的 CanonicalDocument 格式（ContentTree），包含源元数据和结构化节点树
- **REQ-DP-006**: ContentTree 中每个节点保留溯源定位（sourcePointer），支持 PDF 页码、HTML CSS Selector、JSON Pointer、数据库 row_id 等多种定位方式
- **REQ-DP-007**: ContentTree 中每个节点预渲染 markdown 和 plainText 两份文本：markdown 用于 embedding，plainText 用于 BM25 全文检索
- **REQ-DP-008**: 解析失败需输出明确的错误信息（页码/行号/原因）
- **REQ-DP-009**: 解析器支持 probe() 方法进行文件内容自动检测，用于未识别格式的兜底判断

### 文档清洗

- **REQ-DP-010**: 自动去除文档噪音（页眉页脚/水印/HTML标签/导航栏），噪音标记在 ContentNode 生成阶段完成
- **REQ-DP-011**: 支持按规则配置清洗策略（正则匹配删除/替换模式），清洗规则在 Service 级别配置
- **REQ-DP-012**: 清洗后的 ContentTree 存储于 raw_documents 表，为 KB 级共享

### 分块策略

- **REQ-DP-013**: 分块策略操作 ContentTree（统一中间格式），按节点类型和标题层级决策分块边界，TABLE 和 CODE 类型节点保持完整不被拆分
- **REQ-DP-014**: 分块策略按文档结构特征自动适配：标题层级丰富 → 标题分块 / 检测 Q&A 模式 → Q&A 分块 / 否则 → 段落边界 + 固定大小
- **REQ-DP-015**: 支持 Service 级别配置 chunk_size 和 chunk_overlap
- **REQ-DP-016**: 分块结果存储时保留结构元数据（标题路径/页码/位置索引/sourcePointer 列表）

### 向量化

- **REQ-DP-017**: 支持配置 embedding 模型（Service 级别）
- **REQ-DP-018**: 批量 embedding 支持并发 + 失败重试
- **REQ-DP-019**: 增量更新时仅向量化变更的 chunks

### 索引写入

- **REQ-IDX-001**: Chunk 的 markdown 字段拼接后送入 Embedding API，向量写入 pgvector
- **REQ-IDX-002**: Chunk 的 plainText 字段送入 Elasticsearch BM25 索引（按 Service 物理隔离，命名格式 `idx_{serviceId}_chunks`）
- **REQ-IDX-003**: pgvector 和 ES 写入支持批量操作，单次最多 100 条
- **REQ-IDX-004**: 索引写入失败时标记 chunk 状态为 failed，支持自动重试（最多 3 次）

### 处理状态

- **REQ-DP-022**: 文档处理状态机：uploaded → parsing → parsed → chunking → chunked → embedding → ready
- **REQ-DP-023**: 每个状态节点可标记 failed，支持部分重试和断点恢复
- **REQ-DP-024**: 处理进度可查询（已处理 chunks / 总 chunks）

### 增量更新

- **REQ-DP-025**: 文档更新时自动检测变更范围，仅重处理受影响的 chunks
- **REQ-DP-026**: 文档删除时级联清理关联的 chunks 和 vectors
