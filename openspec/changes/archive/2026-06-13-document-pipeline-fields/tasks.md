# Tasks: 文档管道处理字段补充

## 1. Domain 层 — 枚举与实体

- [x] 1.1 新建 `DocumentStatus` 枚举 `rag-server-domain/.../ingestion/model/valobj/DocumentStatus.java`，包含 UPLOADED / PARSING / PARSED / CLEANING / CLEANED / CHUNKING / CHUNKED / EMBEDDING / EMBEDDED / INDEXING / READY / FAILED
- [x] 1.2 `KnowledgeDocumentEntity` 新增 6 个字段：`status` / `pipelineId` / `errorMessage` / `totalChunks` / `chunkCount` / `sourceType`

## 2. Infrastructure 层 — PO 同步

- [x] 2.1 `KnowledgeDocumentPO` 新增对应 6 个字段，添加 `@TableField` 注解

## 3. App 层 — 数据库迁移

- [x] 3.1 修改 `V3__create_t_knowledge_document.sql`：`file_size` 类型 `INT` → `BIGINT`，新增 `status` / `pipeline_id` / `error_message` / `total_chunks` / `chunk_count` / `source_type` 列

## 4. Domain 层 — Service 初始化默认值

- [x] 4.1 `KnowledgeDocumentServiceImpl.uploadDocument()` 中 builder 补充 `status` = `UPLOADED`、`sourceType` = `MANUAL_UPLOAD`、`fileSize`、`fileType` 的赋值
