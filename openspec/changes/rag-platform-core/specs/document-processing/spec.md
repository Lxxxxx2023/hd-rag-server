# Spec: Document Processing

## Overview

定义文档从导入到索引的完整处理流水线。

## Requirements

### 文档导入

- **REQ-DP-001**: 支持文件上传导入，首期支持 PDF 和 Markdown 格式
- **REQ-DP-002**: 支持 API 直接传入文本内容导入
- **REQ-DP-003**: 支持批量导入（一次上传多个文件）

### 文档解析

- **REQ-DP-004**: 解析器保留原始文档结构（PDF：页码/段落/标题层级/表格；Markdown：标题层级/代码块/列表）
- **REQ-DP-005**: 解析输出的结构化内容存储于 raw_documents 表，为 KB 级共享
- **REQ-DP-006**: 解析失败需输出明确的错误信息（页码/行号/原因）

### 文档清洗

- **REQ-DP-007**: 自动去除文档噪音（页眉页脚/水印/HTML标签/导航栏）
- **REQ-DP-008**: 支持按规则配置清洗策略（去正则匹配/替换模式）

### 分块策略

- **REQ-DP-009**: 分块策略按文档类型自动适配：
  - 手册/技术文档 → 按标题层级分块
  - FAQ → 按 Q&A 对分块
  - Markdown/代码 → 按章节/代码块分块
- **REQ-DP-010**: 支持 Service 级别配置 chunk_size 和 chunk_overlap
- **REQ-DP-011**: 分块结果存储时保留结构元数据（标题路径/页码/位置索引）

### 向量化

- **REQ-DP-012**: 支持配置 embedding 模型（Service 级别）
- **REQ-DP-013**: 批量 embedding 支持并发 + 失败重试
- **REQ-DP-014**: 增量更新时仅向量化变更的 chunks

### 处理状态

- **REQ-DP-015**: 文档处理状态机：uploaded → parsing → parsed → chunking → chunked → embedding → ready
- **REQ-DP-016**: 每个状态节点可标记 failed，支持部分重试和断点恢复
- **REQ-DP-017**: 处理进度可查询（已处理 chunks / 总 chunks）

### 增量更新

- **REQ-DP-018**: 文档更新时自动检测变更范围，仅重处理受影响的 chunks
- **REQ-DP-019**: 文档删除时级联清理关联的 chunks 和 vectors
