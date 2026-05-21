# Tasks: Knowledge Graph

## Schema: spec-driven | Progress: 0/30 tasks

---

## 1. Phase 1 — 架构预留 (8 tasks)

### types/ POJOs
- [ ] KG-001 在 types/ 中创建 Entity 实体数据类（entityId, name, type, aliases, props）
- [ ] KG-002 在 types/ 中创建 Relation 关系数据类（relationId, subject, object, predicate, sourceChunkId, confidence）
- [ ] KG-003 在 types/ 中创建 GraphQuery 查询入参类（serviceId, entityIds, maxHops, relationTypes, maxResults）
- [ ] KG-004 在 types/ 中创建 GraphRetrievalResult 检索结果类（entities, relations, relatedChunkIds, graphScore）

### domain 策略接口
- [ ] KG-005 在 domain/index/ 中定义 IEntityExtractor 策略接口（canHandle + extract）
- [ ] KG-006 在 domain/index/ 中定义 IRelationExtractor 策略接口（extract 方法签名）
- [ ] KG-007 在 domain/query/ 中定义 IGraphExpander / IGraphRetriever / IEntityLinker 策略接口

### infrastructure 抽象
- [ ] KG-008 创建 infrastructure/graph/ 包，定义 GraphRepository 抽象接口（saveEntities, saveRelations, query, deleteByService, deleteByDocument），Phase 1 无实现

---

## 2. Phase 2 — 结构图谱 (7 tasks)

- [ ] KG-009 实现 ContentTreeTraverser — 基于 structurePath 计算 Chunk 的 PARENT_OF / SIBLING_OF / PREV_OF 关系
- [ ] KG-010 实现 GraphExpander（IGraphExpander 实现）— 给定 Chunk 列表和 expandDepth，沿 ContentTree 关系拉入父/子/兄弟 Chunk
- [ ] KG-011 实现 GraphExpansionCase — 在 Query 流程中，检索结果 → graph_expand → 扩展后的 Chunk 列表
- [ ] KG-012 在 Pipeline Config 的 document_search.kg_expand 中新增 graph_expand 节点（默认 enabled=false）
- [ ] KG-013 编写结构图谱单元测试：验证父子/兄弟关系计算正确
- [ ] KG-014 编写结构图谱集成测试：验证 graph_expand 算子插入 Query Pipeline 后检索结果正确扩展
- [ ] KG-015 验证 graph_expand 对已有查询流程无影响（enabled=false 时路径不变）

---

## 3. Phase 3 — 实体图谱 (10 tasks)

- [ ] KG-016 引入 spring-boot-starter-data-neo4j 依赖，添加 Neo4jConfig 配置类
- [ ] KG-017 实现 Neo4jGraphRepository（GraphRepository 的 Neo4j 实现）— 实体/关系 CRUD + Cypher 遍历
- [ ] KG-018 实现 LLMEntityExtractor（IEntityExtractor 实现）— few-shot prompt + LLM 调用
- [ ] KG-019 实现 LLMRelationExtractor（IRelationExtractor 实现）— 基于已抽取实体 + 上下文抽取关系三元组
- [ ] KG-020 在 Index 处理链中注册 entity_extract / relation_extract / graph_index 算子节点
- [ ] KG-021 实现 EntityLinker（IEntityLinker 实现）— 查询提及文本 → 图数据库实体消歧匹配
- [ ] KG-022 实现 GraphRetriever（IGraphRetriever 实现）— 基于 GraphQuery 执行 Cypher 多跳遍历
- [ ] KG-023 实现 GraphVectorFusion — graph_first / interleave / score_combine 三种融合策略
- [ ] KG-024 在 Query 流程中注册 entity_link / graph_traverse / graph_vector_fusion 算子节点
- [ ] KG-025 编写实体图谱端到端测试：文档导入 → LLM 抽取 → Neo4j 写入 → 实体链接检索 → 图向量融合

---

## 4. Phase 4 — 全局图谱 (5 tasks)

- [ ] KG-026 实现 LeidenCommunityDetector — 对 Neo4j 中实体-关系图执行 Leiden 社区发现
- [ ] KG-027 实现 CommunitySummarizer — LLM 生成社区摘要
- [ ] KG-028 社区摘要存储到 Neo4j（作为 Community 节点属性）
- [ ] KG-029 实现 GlobalGraphSearcher — 基于社区摘要的全局搜索
- [ ] KG-030 实现 GraphRAGCase — 编排全局检索流程：社区匹配 → 摘要注入 → LLM 生成全局性答案

---

## Task Summary

| Phase | 内容 | Tasks | 新增依赖 |
|-------|------|-------|---------|
| 1 | 架构预留 — types POJO + 策略接口 | KG-001 ~ KG-008 (8) | 无 |
| 2 | 结构图谱 — ContentTree 关系 + graph_expand | KG-009 ~ KG-015 (7) | 无 |
| 3 | 实体图谱 — Neo4j + LLM 抽取 + 图检索 | KG-016 ~ KG-025 (10) | Neo4j |
| 4 | 全局图谱 — 社区发现 + 摘要 + 全局问答 | KG-026 ~ KG-030 (5) | Phase 3 |
| **Total** | | **30** | |

## Implementation Order

Phase 1 → Phase 2 → Phase 3 → Phase 4（严格顺序，每期独立交付价值）
