# Proposal: Multi-DataSource Index & Query Redesign

## Summary

重新设计 RAG 中台的 Index Domain 和 Query Domain，补齐两个核心缺口：在 Index 侧引入 DataSource 连接层，支持 URL、飞书、语雀、数据库、文件五类数据源的统一接入与增量同步；在 Query 侧引入意图路由与 Text-to-SQL 执行路径，使平台能够支持智能问数场景，而不仅限于文档语义检索。

## Motivation

当前设计将知识库入口限定为文件上传，但企业知识实际散落在飞书/语雀文档、内部系统网页、数据库表等多种来源中，用户需要手动下载再上传，体验差且知识难以保持更新。

同时，以数据库为数据源的业务方若想构建智能问数能力，当前 Query Domain 的单一文档检索路径无法满足需求——"上个月退款金额最高的客户是谁"这类问题需要的是生成并执行 SQL，而非向量检索。

## Core Design Decisions

| # | 决策 | 说明 |
|---|------|------|
| 1 | 引入 DataSource 实体层 | 在 KnowledgeBase 和 Document 之间增加连接配置与同步状态管理，KB 通过 DataSource 聚合多种来源 |
| 2 | ISourceConnector 策略接口 | 替代现有 IDocumentLoad，统一 fetchAll / fetchUpdated / handleWebhook 三种拉取方式，新增数据源只需注册新策略 |
| 3 | DataSourceDocument 映射表 | 记录外部 ID → 内部 Document 映射与版本号，支持增量同步与 Webhook 精准更新 |
| 4 | DATABASE 源双轨处理 | 文本列走现有 ContentTree → Chunk → Vector 路径；Schema 信息单独写入 SchemaIndex，供 Text-to-SQL 使用 |
| 5 | intent_detect 升级为强制路由器 | 从可选预处理步骤变为强制执行的路径分叉点，输出 DOCUMENT_SEARCH / DATA_QUERY / HYBRID 三种意图 |
| 6 | Text-to-SQL 作为独立 Query 路径 | schema_retrieve → text_to_sql → sql_validate → sql_execute → result_format，与文档检索路径完全解耦 |
| 7 | Pipeline Config 扩展为双路径结构 | routing + document_search + data_query + postprocessing 四段，每段独立配置，互不干扰 |
| 8 | RAGResponse 扩展支持数据查询结果 | 新增 queryType 字段区分响应类型，citations 和 dataResult 按 queryType 按需填充 |

## Capability Domains

- **DataSource Management（Index 侧新增）**: DataSource CRUD、连接测试、手动/定时/Webhook 三种同步触发、DataSourceDocument 增量同步映射
- **Source Connectors（Index 侧新增）**: UrlConnector（HTTP + Jsoup）、FeishuConnector（开放平台 API）、YuqueConnector（语雀 API）、DatabaseConnector（JDBC 双轨）、FileConnector（复用现有 Parser）
- **Schema Indexing（Index 侧新增）**: DATABASE 类型专属，索引表结构/列注释/枚举值/关联关系/样本行，供 Text-to-SQL 使用
- **Intent Routing（Query 侧重构）**: intent_detect 升级为强制路由器，识别 DOCUMENT_SEARCH / DATA_QUERY / HYBRID
- **Text-to-SQL Path（Query 侧新增）**: schema_retrieve → text_to_sql → sql_validate → sql_execute → result_format，完整的数据查询执行链路
- **Hybrid Path（Query 侧新增）**: 文档检索与数据查询并行执行，context_merge 合并两路结果，支持双溯源响应

## Non-goals

- 不实现多模态图片 Embedding（IMAGE 节点仍保留 OCR 文本，不引入 CLIP 向量）
- 不实现对话类数据源（客服记录、工单）的结构化建模，此类数据仍走 PlainTextParser 扁平处理
- 不实现 Connector 的配额/限流管理（依赖现有 Service 级配额体系）
- 不修改 ContentTree 节点类型（CALLOUT 节点映射到 QUOTE，暂不新增节点类型）
- 不支持 DML/DDL SQL 执行，sql_validate 强制只读校验
