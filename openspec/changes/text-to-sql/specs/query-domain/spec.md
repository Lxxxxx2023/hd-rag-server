# Spec: Query Domain — Text-to-SQL & Hybrid Path

## Overview

在 RAG 查询引擎（rag-query change）基础上，新增两条查询子路径：Text-to-SQL 结构化数据查询和 Hybrid 混合检索。扩展 Pipeline Config 的 `data_query` 段和 `routing` 段的 `HYBRID` 分叉。

## Pipeline Config — data_query 段

```json
{
  "data_query": {
    "schema_retrieve": { "topK": 5 },
    "text_to_sql": {
      "model": "gpt-4o",
      "dialect": "mysql",
      "maxRetries": 2
    },
    "sql_validate": {
      "allowDML": false,
      "allowDDL": false,
      "maxRows": 1000
    },
    "sql_execute":   { "timeoutMs": 5000 },
    "result_format": { "maxCells": 500 },
    "generation":    { "model": "gpt-4o", "maxTokens": 1024 }
  }
}
```

## Requirements

### Text-to-SQL 数据查询路径

- **REQ-QRY-030**: schema_retrieve — 从 SchemaIndex 语义检索相关表和列，注入 dataSourceId + 候选表的 columns + relationships + sampleRows
- **REQ-QRY-031**: text_to_sql — 将 query + Schema 上下文送入 LLM 生成 SQL，支持最多 maxRetries 次重新生成（sql_validate 失败触发重试）
- **REQ-QRY-032**: sql_validate — SQL AST 语法校验 + 安全校验（禁止 DML/DDL）+ 自动追加 LIMIT maxRows
- **REQ-QRY-033**: sql_execute — 通过 JDBC 连接池执行 SQL，超时控制 timeoutMs，使用只读数据库账号
- **REQ-QRY-034**: result_format — ResultSet 转为 Markdown 表格，超出 maxCells 时截断并附注

### 混合路径

- **REQ-QRY-040**: context_merge — 合并文档检索 ChunkContext 与数据查询 DataQueryResult，标注来源（"来自文档 [ref_N]" / "来自数据库查询"），按 token 预算裁剪
- **REQ-QRY-041**: 混合路径并行调度 — Path1（文档检索）+ Path2（数据查询）并行执行，等待两路结果后进入 context_merge
- **REQ-QRY-042**: 混合路径引用溯源 — citation_extract 同时处理文档引用（[ref_N]）和数据来源（"来自 {tableName} 查询"）

### RAGResponse 扩展

```java
@Value public class DataQueryResult {
    String sql;
    String dataSourceId;
    String tableName;
    int rowCount;
    List<String> columns;
    List<Map<String, Object>> rows;
    boolean truncated;
}
```

### SQL 安全

- **REQ-SEC-SQL-01**: sql_validate 强制只读校验：禁止 DML（INSERT/UPDATE/DELETE）、DDL（CREATE/DROP/ALTER）、事务控制（BEGIN/COMMIT/ROLLBACK）
- **REQ-SEC-SQL-02**: sql_execute 使用只读数据库账号，权限限定为 SELECT
- **REQ-SEC-SQL-03**: SQL 执行超时控制 timeoutMs，默认 5s

### 执行模型

```
QueryPipelineCase
  │
  ├─ DATA_QUERY:
  │   ├─ schema_retrieve → text_to_sql → sql_validate → sql_execute → result_format
  │   └─ llm_generate
  │
  ├─ HYBRID:
  │   ├─ DOCUMENT_SEARCH + DATA_QUERY（并行）
  │   ├─ context_merge（合并两路，标注来源）
  │   └─ llm_generate
```

## Query OperatorType (新增)

```
Text-to-SQL:
  schema_retrieve, text_to_sql, sql_validate, sql_execute, result_format

混合:
  context_merge
```

## Query Domain 数据读取依赖

```
schema_retrieve ──────读取──────────▶ ISchemaIndexRepository.searchByQuery()
sql_execute ──────────执行──────────▶ IDatabaseExecutorPort
context_merge ───────读取──────────▶ IChunkRepository + DataQueryResult
```
