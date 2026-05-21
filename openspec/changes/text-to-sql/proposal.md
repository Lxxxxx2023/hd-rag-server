# Proposal: Text-to-SQL & Hybrid Query

## Summary

在 RAG 查询引擎基础上，新增结构化数据查询（Text-to-SQL）和混合检索（Hybrid）两条路径。Text-to-SQL 实现自然语言 → SQL 生成 → 安全校验 → 执行 → 格式化结果的全链路；Hybrid 路径并行执行文档检索和数据查询，合并两路结果后送入 LLM 生成。两条路径共享 Pipeline Config 的 data_query 段和 HYBRID 分叉逻辑。

## User Stories

### 业务开发者 / 查询用户

- **US-1**: 作为**业务开发者**，我想通过自然语言**查询数据库中的结构化数据**（Text-to-SQL），系统自动生成并执行只读 SQL，以便无需手写 SQL 即可获得数据洞察。
- **US-2**: 作为**业务开发者**，我想在查询结果中同时看到**文档内容 + 结构化数据**（混合检索），以便在复杂场景下一次获取完整信息（如"对比一下 Q3 的销售数据和产品文档中的目标"）。

## User Actions（关键操作路径）

### 路径 A：结构化数据查询（Text-to-SQL）

```
运营已接入 DATABASE DataSource（Schema 已索引）
→ 业务方发起自然语言查询："上个月销售额最高的前 10 个产品"
→ intent_detect 检测到时间范围+数值+统计词 → 判定为 DATA_QUERY
→ schema_retrieve: 语义检索 SchemaIndex，定位相关表和列
→ text_to_sql: query + Schema 上下文 → LLM 生成 SQL
→ sql_validate: AST 校验语法 + 安全校验(禁止 DML/DDL) + 自动追加 LIMIT
→ sql_execute: JDBC 执行（超时 5s）
→ result_format: ResultSet → Markdown 表格
→ llm_generate: 格式化结果 → 自然语言回答 → 返回
```

### 路径 B：混合检索（文档 + 数据）

```
业务方发起查询 → intent_detect 判定为 HYBRID
→ 并行执行:
    Path1: vector_search + keyword_search → fusion → rerank → context_assemble (文档)
    Path2: schema_retrieve → text_to_sql → sql_validate → sql_execute → result_format (数据)
→ context_merge: 合并两路结果，标注来源（"来自文档" / "来自数据库查询"），按 token 预算裁剪
→ prompt_render → llm_generate → citation_extract（双溯源）→ safety_filter
→ RAGResponse 同时包含 citations 和 dataResult
```

## Motivation

RAG 的检索质量受限于"语义相似度 → Top K"的单一范型。对于"上个月销售额最高的产品""对比近三月的用户增长"这类需要结构化查询的问题，文档检索无法回答。Text-to-SQL 补全了 RAG 平台对结构化数据的查询能力，Hybrid 路径让一次查询可以同时覆盖文档知识和数据事实。

## Core Design Decisions

| # | 决策 | 说明 |
|---|------|------|
| 1 | Text-to-SQL 独立 Query 路径 | schema_retrieve → text_to_sql → sql_validate → sql_execute → result_format，与文档检索路径解耦 |
| 2 | sql_validate 强制只读 | 禁止 DML/DDL，自动追加 LIMIT，使用只读数据库账号 |
| 3 | SchemaIndex 语义检索 | tableComment + column 描述向量化 → pgvector 检索，精准定位相关表 |
| 4 | Hybrid 并行执行 | 文档检索 + 数据查询并行，context_merge 合并标注来源 |
| 5 | 共用 Pipeline Config | data_query 段配置 Text-to-SQL，HYBRID 意图触发并行路径 |

## Non-goals

- 不支持 DML/DDL SQL 执行，sql_validate 强制只读校验
- 不实现跨数据库 JOIN 查询
- 不实现 Text-to-SQL 的 Agent 模式（多轮纠错、自修正）

## Dependencies

- [platform-foundation](../platform-foundation/proposal.md) — Service 配置
- [rag-query](../rag-query/proposal.md) — Pipeline Config 框架、intent_detect、llm_generate、citation_extract
- [datasource-ingestion](../datasource-ingestion/proposal.md) — DATABASE DataSource + SchemaIndex
