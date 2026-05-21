# Proposal: Knowledge Graph

## Summary

为 RAG 平台引入知识图谱能力，分四期渐进式演进：Phase 1 架构预留（策略接口定义）、Phase 2 结构图谱（基于 ContentTree 的 Chunk 关系图 + 路径扩展检索）、Phase 3 实体图谱（LLM 抽取实体/关系 + Neo4j 存储 + 实体链接检索 + 图向量融合）、Phase 4 全局图谱（社区发现 + 社区摘要 + 全局问答）。KG 不新建独立 Domain，作为 Index Domain 的新增产出物 + Query Domain 的新增消费方式。

## User Stories

### 业务开发者 / 查询用户

- **US-1**: 作为**业务开发者**，我想通过**结构图谱扩展**检索上下文（如检索到一个 Chunk 后自动拉入其父/子/兄弟 Chunk），以便获取更完整的文档上下文。
- **US-2**: 作为**业务开发者**，我想通过**实体图谱**发现跨文档的实体关联和关系路径（如"A 公司的 CEO 是谁""哪些产品用了 XX 技术"），以便理解知识间的深层联系。
- **US-3**: 作为**业务开发者**，我想通过**图向量融合**同时获得语义相似度和图结构相关度的综合排序，以便提升复杂问题的检索质量。
- **US-4**: 作为**业务开发者**，我想通过**全局图谱**获得对整个知识库的宏观理解（如"知识库中讨论了哪些主要主题"），以便进行高层次的知识发现。

## User Actions（关键操作路径）

### 路径 A：结构图谱扩展

```
运营可选开启 KG（kg_expand.enabled = true）
→ 业务方查询 → document_search 路径 → 检索得到 Top-K Chunks
→ graph_expand: ContentTreeTraverser 基于 structurePath 计算 Chunk 间关系
→ 沿 PARENT_OF/SIBLING_OF/PREV_OF 拉入上下文 Chunk → 扩展后的 Chunk 列表
→ 继续 rerank → context_assemble → llm_generate
```

### 路径 B：实体图谱检索

```
运营开启实体抽取（Phase 3）→ 文档处理后触发 entity_extract + relation_extract
→ LLM 按 Chunk 抽取实体/关系三元组 → 写入 Neo4j
→ 业务方查询 → entity_link: 查询提及文本匹配图数据库实体
→ graph_traverse: Cypher 多跳遍历 → 获取关联实体/关系
→ graph_vector_fusion: 与向量检索结果融合 → 重排序 → 生成
```

## Motivation

RAG 的检索质量受限于"语义相似度 → Top K"的单范型。对于"XX 产品的上一级分类是什么""与 YY 相关的所有概念"这类需要结构关系和多跳推理的问题，单纯的向量检索无法有效回答。知识图谱补全了 RAG 对结构化关系理解和多跳推理的能力。

## Core Design Decisions

| # | 决策 | 说明 |
|---|------|------|
| 1 | KG 作为 Index/Query Domain 的延伸 | 不新建独立 Domain，策略接口分别在 domain/index/ 和 domain/query/ |
| 2 | 四期渐进式演进 | 架构预留 → 结构图谱 → 实体图谱 → 全局图谱，每期独立交付价值 |
| 3 | Phase 2 结构图谱零额外存储 | 基于 ContentTree 的 structurePath 就地计算 Chunk 关系 |
| 4 | 不绑定特定图数据库 | 通过 GraphRepository 接口抽象，Phase 3 提供 Neo4j 实现 |
| 5 | 不强制所有 Service 使用 KG | 可选开启，通过 Pipeline Config 的 kg_expand.enabled 控制 |

## Non-goals

- 不绑定特定图数据库（通过 GraphRepository 接口抽象）
- 不强制所有 Service 使用 KG（可选开启）
- Phase 1 不实现 LLM 抽取和 Neo4j 存储

## Dependencies

- [platform-foundation](../platform-foundation/proposal.md) — Service 配置
- [document-indexing](../document-indexing/proposal.md) — ContentTree、Chunk 结构
- [rag-query](../rag-query/proposal.md) — Pipeline Config（kg_expand 节点）、检索流程
