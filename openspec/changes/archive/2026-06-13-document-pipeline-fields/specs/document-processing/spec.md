# Spec: Document Processing Fields

## Overview

定义 `KnowledgeDocumentEntity` 的管道处理字段，使 Document 实体能够作为摄入管道的处理对象，承载处理状态、进度信息和来源标记。

## ADDED Requirements

### Requirement: 文档处理状态跟踪

系统 SHALL 在 `KnowledgeDocumentEntity` 上维护处理状态字段 `status`，支持以下状态值：

```
UPLOADED → PARSING → PARSED → CLEANING → CLEANED → CHUNKING → CHUNKED → EMBEDDING → EMBEDDED → INDEXING → READY
                                                                                                      ↓
                                                                                                   FAILED
```

任意非终态节点均可转为 FAILED。READY 和 FAILED 为终态。

#### Scenario: 文档创建时初始状态为 UPLOADED

- **WHEN** 系统创建一条 KnowledgeDocument 记录
- **THEN** `status` 字段 SHALL 自动设为 `UPLOADED`

#### Scenario: 管道处理中状态逐步推进

- **WHEN** IndexPipeline 对文档执行解析步骤
- **THEN** `status` SHALL 更新为 `PARSING`，解析完成后 SHALL 更新为 `PARSED`

#### Scenario: 处理失败时标记 FAILED

- **WHEN** 任一步骤抛出异常
- **THEN** `status` SHALL 变为 `FAILED`，`errorMessage` SHALL 记录异常信息

---

### Requirement: 文档来源标记

系统 SHALL 在 `KnowledgeDocumentEntity` 上维护 `sourceType` 字段，记录文档的数据来源类型。

支持以下来源类型：
- `MANUAL_UPLOAD` — 用户通过 HTTP 接口手动上传
- `FEISHU` — 飞书文档拉取
- `YUQUE` — 语雀文档拉取
- `URL` — URL 爬取
- `DATABASE` — 数据库表同步

#### Scenario: 手动上传文档标记为 MANUAL_UPLOAD

- **WHEN** 用户通过 `/api/v1/knowledge-bases/{kbId}/documents/upload` 上传文件
- **THEN** Document 的 `sourceType` SHALL 为 `MANUAL_UPLOAD`

#### Scenario: DataSource 同步文档标记对应来源

- **WHEN** FeishuConnector 拉取飞书文档并创建 Document 记录
- **THEN** Document 的 `sourceType` SHALL 为 `FEISHU`

---

### Requirement: 管道关联

系统 SHALL 在 `KnowledgeDocumentEntity` 上维护 `pipelineId` 字段，关联处理该文档的 PipelineEntity。

`pipelineId` 可为 NULL，NULL 表示使用 KnowledgeBase 的默认管道。

#### Scenario: 文档关联指定管道

- **WHEN** 创建 Document 时指定了 `pipelineId`
- **THEN** 管道编排器 SHALL 使用该 pipeline 的节点配置处理此文档

#### Scenario: pipelineId 为 NULL 时使用默认管道

- **WHEN** Document 的 `pipelineId` 为 NULL
- **THEN** 管道编排器 SHALL 使用所属 KnowledgeBase 的默认管道

---

### Requirement: 处理进度跟踪

系统 SHALL 在 `KnowledgeDocumentEntity` 上维护 `totalChunks` 和 `chunkCount` 字段，用于展示处理进度。

- `totalChunks`：分块步骤完成后设置，表示文档被切分成的总块数
- `chunkCount`：每完成一个 chunk 的向量化+写入后递增

进度百分比 = `chunkCount / totalChunks`（仅当两者均非 NULL 时有效）。

#### Scenario: 分块完成时设置总数

- **WHEN** 分块步骤完成，产出 N 个 TextChunk
- **THEN** `totalChunks` SHALL 设为 N

#### Scenario: 向量化逐步推进已完成数

- **WHEN** 一个 chunk 完成向量化并写入索引
- **THEN** `chunkCount` SHALL 递增 1

#### Scenario: 分块前进度字段为 NULL

- **WHEN** Document 状态为 `UPLOADED` 或 `PARSING` 或 `PARSED` 或 `CLEANING` 或 `CLEANED`
- **THEN** `totalChunks` 和 `chunkCount` SHALL 为 NULL

---

### Requirement: 错误信息记录

系统 SHALL 在 `KnowledgeDocumentEntity` 上维护 `errorMessage` 字段，记录最近一次处理失败的原因。

#### Scenario: 处理失败时记录错误

- **WHEN** 管道处理步骤抛出异常导致 status 变为 FAILED
- **THEN** `errorMessage` SHALL 包含步骤名称和异常消息，如 "CHUNKING: chunk_size 过小导致空块"

#### Scenario: 重试成功时清空错误信息

- **WHEN** 失败的 Document 被重新处理且执行成功
- **THEN** `errorMessage` SHALL 清空为 NULL

---

### Requirement: 文件大小字段类型

系统 SHALL 使用 `BIGINT` 类型存储 `file_size` 字段，支持超过 2GB 的文件大小记录。

#### Scenario: 大文件大小正确存储

- **WHEN** 上传一个 3GB 的文件
- **THEN** `fileSize` SHALL 正确存储为 3221225472（3 * 1024^3）而不会溢出
