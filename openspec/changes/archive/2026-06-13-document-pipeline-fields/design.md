# Design: 文档管道处理字段补充

## Context

当前 `KnowledgeDocumentEntity` 仅记录文档元数据（名称、大小、类型、URL），是纯数据容器。根据 `document-indexing` 提案中的管道状态机规划（REQ-DP-050: `uploaded → parsing → parsed → chunking → chunked → embedding → ready`），Document 需要承载运行时状态和进度信息。本次变更是管道编排器实现的**前置字段铺设**，不涉及管道执行逻辑。

### 当前实体结构

```java
// rag-server-domain/.../entity/KnowledgeDocumentEntity.java
public class KnowledgeDocumentEntity {
    private String id, kbId, docName, fileType, fileUrl;
    private Long fileSize;        // PO 中为 INT，需修正
    private String createBy, updateBy;
    private LocalDateTime createTime, updateTime;
}
```

### 目标：Document 即管道处理对象

根据讨论结论，Document 自身即为管道流转载体，不额外创建 ProcessingTask 实体。管道编排器读取 Document → 执行步骤 → 更新 status → 外部查询进度。

## Goals / Non-Goals

**Goals:**
- 为 `KnowledgeDocumentEntity` 新增 6 个管道处理字段
- 定义 `DocumentStatus` 枚举（状态机）
- DB V3 migration 中修正 `file_size` 类型为 `BIGINT`
- 新增字段与现有 DDD 分层结构一致

**Non-Goals:**
- 不实现状态机流转逻辑（属于 IndexPipeline）
- 不创建 PipelineEntity（后续提案）
- 不修改 Controller 层 API（DTO 适配后续按需）

## Decisions

### 1. 字段放置：Entity + PO 同步新增

所有新字段同时添加到 Domain Entity 和 Infra PO，保持两层一一对应。

| 字段 | Java类型 | DB类型 | 说明 |
|------|---------|--------|------|
| `status` | `String` | `VARCHAR(32) NOT NULL DEFAULT 'UPLOADED'` | 状态机当前状态 |
| `pipelineId` | `String` | `CHAR(32) NULL` | 关联 PipelineEntity，NULL 表示使用 KB 默认 |
| `errorMessage` | `String` | `TEXT NULL` | 最近一次失败原因 |
| `totalChunks` | `Integer` | `INT NULL` | 分块总数，分块完成后设置 |
| `chunkCount` | `Integer` | `INT NULL DEFAULT 0` | 已完成数 |
| `sourceType` | `String` | `VARCHAR(32) NOT NULL DEFAULT 'MANUAL_UPLOAD'` | 文档来源 |

### 2. DocumentStatus 枚举

```java
// rag-server-domain/.../model/valobj/DocumentStatus.java
public enum DocumentStatus {
    UPLOADED("已上传"),
    PARSING("解析中"),
    PARSED("解析完成"),
    CLEANING("清洗中"),
    CLEANED("清洗完成"),
    CHUNKING("分块中"),
    CHUNKED("分块完成"),
    EMBEDDING("向量化中"),
    EMBEDDED("向量化完成"),
    INDEXING("索引写入中"),
    READY("就绪"),
    FAILED("失败");
}
```

当前设计文档中的状态粒度（parsing→parsed→chunking→chunked→...）比实际管道步骤（parse→clean→chunk→embed→store）更细。本次按完整粒度定义枚举，未来 IndexPipeline 实现时可按需使用。

### 3. sourceType vs DataSourceType

`sourceType` 描述**单个文档**的来源标记，与 DataSource 层的 `DataSourceType`（连接器类型）是不同概念：

```
DataSourceType（DataSource 层）:  URL | FEISHU | YUQUE | DATABASE | FILE
  描述连接器的技术类型

sourceType（Document 层）:  MANUAL_UPLOAD | FEISHU | YUQUE | URL | DATABASE
  描述这个文档是怎么进来的
```

两者值相近但用途不同。Document.sourceType 用于追溯和展示，不参与管道路由。

### 4. 分层归属

```
rag-server-domain/
  ingestion/
    model/
      entity/
        KnowledgeDocumentEntity.java    ← 新增 6 字段
      valobj/
        DocumentStatus.java             ← 新增枚举

rag-server-infrastructure/
  dao/
    po/
      KnowledgeDocumentPO.java         ← 新增 6 字段 + 注解

rag-server-app/
  db/migration/
    V3__create_t_knowledge_document.sql ← 修正 INT→BIGINT, 加列
```

### 5. 暂不调整 DTO

当前 `KnowledgeDocumentUploadReqDTO` / `KnowledgeDocumentRespDTO` 是上传场景专用，状态/进度等字段属于查询响应。后续查询接口（如 `GET /documents/{id}/status`）单独定义 DTO，本次不变更现有 DTO。

## Risks / Trade-offs

**[Risk] status 用 VARCHAR 而非 TINYINT 索引**
→ Mitigation: 当前文档量级下 VARCHAR 性能足够。状态字段查询频率低（非热点路径），后续数据量大时可加索引或迁移为 TINYINT。VARCHAR 可读性好，调试友好。

**[Risk] totalChunks / chunkCount 仅在分块阶段有值**
→ Mitigation: 可空字段（NULL），分块前为 NULL。进度查询时判空后展示"分块未开始"。字段含义在接口文档中明确。

**[Risk] pipelineId 为 NULL 时如何处理**
→ Mitigation: NULL 表示使用 KnowledgeBase 的默认管道。KB 创建时自动创建默认管道 → 无 NULL 场景。保留 NULL 作为降级兜底。
