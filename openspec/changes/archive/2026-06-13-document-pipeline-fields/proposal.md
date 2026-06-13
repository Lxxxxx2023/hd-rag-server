# Proposal: 文档管道处理字段补充

## Summary

为 `KnowledgeDocumentEntity` 补充管道处理所需的核心字段，使 Document 实体成为管道流转的载体对象。将当前仅存储文档元数据的实体扩展为具备状态机驱动能力、管道关联能力、处理进度可观测能力的领域聚合根。这是后续实现 IndexPipeline 编排器和 PipelineEntity 配置实体的前置依赖。

## User Stories

### 知识库运营人员

- **US-1**: 作为**知识库运营人员**，我想在文档列表中看到每个文档的处理状态（解析中/分块中/索引中/就绪/失败），以便了解管道处理进度。
- **US-2**: 作为**知识库运营人员**，我想知道文档当前处理到第几步、已完成多少 chunks，以便估算剩余处理时间。
- **US-3**: 作为**知识库运营人员**，我想看到文档处理失败时的错误原因（如"PDF 第15页解析失败"），以便快速定位和修复问题。
- **US-4**: 作为**知识库运营人员**，我想知道文档来自哪个数据源（手动上传/飞书/URL/数据库），以便追溯文档来源。

## User Actions（关键操作路径）

### 路径 A：手动上传文档 → 处理 → 查看状态

```
运营上传文档 → OSS 存储 → 创建 Document 记录（status=UPLOADED, sourceType=MANUAL_UPLOAD）
→ IndexPipeline 读取 Document → 逐步推进状态 → 进度实时更新
→ 运营查询文档详情 → 看到 status=READY, chunkCount/totalChunks=完成
```

### 路径 B：处理失败 → 查看错误 → 重试

```
Document 处理到 CHUNKING 步骤失败 → status=FAILED, errorMessage="chunk_size 过小导致空块"
→ 运营查看文档详情 → 看到错误原因 → 修改管道配置 → 重新触发处理
```

## Motivation

当前 `KnowledgeDocumentEntity` 仅有文件元数据字段（名称、大小、类型、URL），缺少管道处理所需的运行时状态字段。设计文档中已规划了完整的状态机（`uploaded → parsing → parsed → ... → ready`）和进度查询需求，但实体层面尚未适配。补充这些字段后，Document 实体即可直接作为管道编排器的处理对象，无需额外创建 Task 实体。

## Core Design Decisions

| # | 决策 | 说明 |
|---|------|------|
| 1 | Document 即处理对象 | 不单独创建 ProcessingTask 实体。Document 自身承载处理状态，状态机推进时更新 status 字段。避免 1:1 实体冗余 |
| 2 | sourceType 区分来源 | 枚举值 `MANUAL_UPLOAD` / `FEISHU` / `YUQUE` / `URL` / `DATABASE`。不同来源统一通过 Document 进入管道，管道路由不依赖来源类型 |
| 3 | chunkCount + totalChunks 双字段 | 分块完成后设置 totalChunks，每处理完一个 chunk 递增 chunkCount。进度 = chunkCount/totalChunks |
| 4 | errorMessage 存失败信息 | 仅存最近一次失败的原因，不保留历史错误记录。简单有效 |
| 5 | file_size INT → BIGINT | 当前 INT 上限 2GB，大 PDF / ZIP 包可能溢出，修正为 BIGINT |
| 6 | status 用 VARCHAR(32) | 枚举值字符串存储，可读性好。后续如需迁移为 tinyint 索引优化，成本低 |

## Non-goals

- 不实现 PipelineEntity 实体（本次仅补充 Document 字段，PipelineEntity 后续单独提案）
- 不实现 IndexPipeline 编排器（本次仅铺设字段基础）
- 不实现状态机流转逻辑（本次仅新增字段和枚举定义，流转逻辑在 IndexPipeline 中实现）
- 不修改 KnowledgeBaseEntity 的 searchSet/segmentSet（解耦为 PipelineEntity 后续单独提案）

## Dependencies

- [platform-foundation](../platform-foundation/proposal.md) — KB 资源模型、数据库基础设施
- [document-indexing](../document-indexing/proposal.md) — 文档处理管道与状态机规划（REQ-DP-050）

## Impact

- **Entity**: `KnowledgeDocumentEntity` 新增 6 个字段
- **PO**: `KnowledgeDocumentPO` 同步新增字段
- **DB Migration**: V3 migration 新增列 + `file_size` 类型修正为 BIGINT（若已有数据则需 ALTER TABLE，当前为未上线的全新表，直接改 CREATE TABLE）
- **DTO**: `KnowledgeDocumentRespDTO` 等响应 DTO 后续按需新增（本次不涉及）
- **API**: 无新增 API，现有上传接口不变
