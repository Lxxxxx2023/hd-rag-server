# Proposal: Multi-Source Data Ingestion

## Summary

实现 RAG 平台的多源数据接入与增量同步系统。在 KnowledgeBase 和 Document 之间新增 DataSource 连接层，通过策略模式统一五类数据源（URL / 飞书 / 语雀 / 数据库 / 文件）的接入，支持手动触发、定时调度和 Webhook 推送三种同步方式，以及基于版本比对的增量同步机制。

## User Stories

### 知识库运营

- **US-1**: 作为**知识库运营人员**，我想**接入多种数据源**（URL / 飞书 / 语雀 / 数据库 / 本地文件），以便汇聚分散在各处的知识到一个 KB。
- **US-2**: 作为**知识库运营人员**，我想**手动触发 / 配置定时 / 接收 Webhook 自动同步**数据源，以便索引内容保持时效性。
- **US-3**: 作为**知识库运营人员**，我想**查看每次同步的状态和失败明细**（新增/更新/删除数量、错误日志），以便快速定位和修复同步问题。
- **US-4**: 作为**知识库运营人员**，我想在首次配置数据源时**测试连接**是否可用，以便在正式同步前发现问题。

## User Actions（关键操作路径）

### 路径 A：接入飞书文档并保持同步

```
运营创建飞书 DataSource → 填写 AppId/AppSecret → 测试连接 → 连接成功
→ 手动触发首次全量同步 → FeishuConnector 分页拉取 space 下所有文档元数据
→ 比对 DataSourceDocument 版本 → 新增文档进入文档处理管道
→ 配置定时同步（如每天凌晨 2 点）+ 开启 Webhook → 文档变更实时推送
```

### 路径 B：接入数据库

```
运营创建 DATABASE DataSource → 配置 JDBC 连接 + TableConfig（textColumns/filterColumns/syncColumn）
→ 测试连接 → DatabaseConnector 拉取 table schema + sampleRows
→ Schema 元数据写入 schema_index 表（供 Text-to-SQL 使用）
→ 表数据按行包装为 RawSourceDocument → 进入文档处理管道
→ 增量同步：按 syncColumn 检测变更行
```

### 路径 C：文件夹导入

```
运营上传文件夹 → FolderImportCase 递归遍历 + probe 类型检测
→ 路由到对应的 Parser → 各文件独立走文档处理管道
```

## Motivation

当前各业务线的数据源接入逻辑与业务代码耦合，每新增一种数据源需要重复实现连接、同步、错误处理等基础能力。本模块将数据接入抽象为 DataSource 实体 + ISourceConnector 策略接口，新增数据源类型只需实现一个 Connector，同步逻辑和状态管理由 Case 层统一编排。同时流式拉取（fetchMetadata + fetchContent）避免大数据源 OOM。

## Core Design Decisions

| # | 决策 | 说明 |
|---|------|------|
| 1 | DataSource 连接层 | 在 KB 和 Document 之间增加连接配置与同步状态管理 |
| 2 | Connector 流式拉取 | fetchMetadata + fetchContent 游标/分页模式，避免大数据源 OOM |
| 3 | DataSourceDocument 增量映射表 | externalId + externalVersion 版本比对，仅处理变更文档 |
| 4 | Pipeline 编排器，非 Kafka 链 | 处理管道由 Case 层编排器直接调用，状态存 DB。Kafka 仅发业务事件 |
| 5 | DATABASE 源双轨处理 | 文本列走 ContentTree → Vector 路径；Schema 信息单独写入 SchemaIndex |
| 6 | ISourceConnector 策略接口 | 统一 URL/飞书/语雀/数据库/文件五类数据源接入 |
| 7 | DataSourceType ≠ SourceType | DataSourceType 描述连接器类型，SourceType 描述单个文档来源 |
| 8 | Workload 路由：入口线程判断 | HTTP 请求线程 → 小文件同步/大文件异步；定时/Webhook → 一律异步 |

## Non-goals

- 不实现 Connector 的配额/限流管理（依赖 Service 级配额体系）
- 不实现对话类数据源（客服记录、工单）的结构化建模
- 不修改 ContentTree 节点类型

## Dependencies

- [platform-foundation](../platform-foundation/proposal.md) — KB 资源模型、Service 配置
- [document-indexing](../document-indexing/proposal.md) — 文档解析管道（Connector 产出 RawSourceDocument 后进入该管道）
