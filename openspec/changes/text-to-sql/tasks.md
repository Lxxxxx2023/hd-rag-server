# Tasks: Text-to-SQL & Hybrid Query

## Schema: spec-driven | Progress: 0/16 tasks

---

## 1. SchemaIndex 查询 (4 tasks)

- [ ] SI-001 定义 ISchemaIndexRepository — Query 域读取 SchemaIndex 的接口
- [ ] SI-002 实现 SchemaIndex 语义检索 — query 与 tableComment + columnInfo 向量相似度检索
- [ ] SI-003 实现 schema_retrieve 算子 — 封装 ISchemaIndexRepository，产出 Schema 上下文字符串
- [ ] SI-004 实现 SchemaIndex 的 pgvector 索引写入 — tableComment + column 描述向量化

## 2. Text-to-SQL 算子 (5 tasks)

- [ ] SQL-001 实现 text_to_sql 算子 — query + Schema 上下文 → LLM 生成 SQL，maxRetries 次重试
- [ ] SQL-002 实现 sql_validate 算子 — SQL AST 语法校验；拒绝 DML/DDL；自动追加 LIMIT
- [ ] SQL-003 实现 sql_execute 算子 — 通过 IDatabaseExecutorPort 执行 SQL，超时控制 timeoutMs
- [ ] SQL-004 实现 IDatabaseExecutorPort 接口与 JDBC 适配器 — 连接池管理，按 dataSourceId 路由
- [ ] SQL-005 实现 result_format 算子 — ResultSet → Markdown 表格；超出 maxCells 截断

## 3. Text-to-SQL 路径集成 (3 tasks)

- [ ] SQL-006 实现 Text-to-SQL 专用 Prompt 模板 — Schema 上下文注入格式
- [ ] SQL-007 实现 DATA_QUERY 路径的 QueryPipelineCase 分支 — schema_retrieve → text_to_sql → sql_validate → sql_execute → result_format → llm_generate
- [ ] SQL-008 实现 Text-to-SQL 错误处理 — sql_validate 失败触发重试；sql_execute 超时/异常返回错误信息

## 4. Hybrid Path (4 tasks)

- [ ] HB-001 实现 context_merge 算子 — 合并文档检索 ChunkContext 与数据查询 DataQueryResult，标注来源
- [ ] HB-002 实现混合路径并行调度 — Path1（文档检索）+ Path2（数据查询）并行执行
- [ ] HB-003 实现混合路径引用溯源 — citation_extract 同时处理文档引用和数据来源
- [ ] HB-004 实现 HYBRID 模式的 QueryPipelineCase 分支 — 并行 + context_merge + llm_generate + 双溯源

---

## Task Summary

| 模块 | 内容 | Tasks |
|------|------|-------|
| SchemaIndex Query | Repository + 语义检索 + schema_retrieve + 向量写入 | SI-001 ~ SI-004 (4) |
| Text-to-SQL | text_to_sql / sql_validate / sql_execute / result_format / DatabaseExecutorPort | SQL-001 ~ SQL-005 (5) |
| SQL Path Integration | Prompt 模板 + PipelineCase 分支 + 错误处理 | SQL-006 ~ SQL-008 (3) |
| Hybrid Path | context_merge + 并行调度 + 双溯源 + PipelineCase 分支 | HB-001 ~ HB-004 (4) |
| **Total** | | **16** |

## Implementation Order

1. SI-001 / SQL-004 → Repository 接口 + DatabaseExecutorPort（底层依赖先行）
2. SI-002~004 → SchemaIndex 语义检索 + 向量写入
3. SQL-001~003, SQL-005 → Text-to-SQL 算子
4. SQL-006~008 → 路径集成
5. HB-001~004 → Hybrid 路径
