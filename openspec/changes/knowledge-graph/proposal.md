## Why

RAG 中台的检索质量受限于"语义相似度 → Top K"的单一范型，无法处理需要结构化关系理解、多跳推理或全局知识概览的场景。知识图谱（KG）作为向量检索和 BM25 之外的第三种检索方式，能补全 RAG 平台的能力矩阵。现在规划阶段是因为 KG 的扩展点需要在 Index/Query Domain 中预先预留，避免后续强行插入破坏现有分层结构。

## What Changes

- **types/ 新增 KG 数据结构**：Entity、Relation、GraphQuery、GraphRetrievalResult 等纯 POJO
- **domain/index/ 新增 KG 抽取策略接口**：IEntityExtractor、IRelationExtractor（Phase 1 仅接口，不实现）
- **domain/query/ 新增 KG 检索策略接口**：IGraphRetriever、IEntityLinker、IGraphExpander（Phase 1 仅接口）
- **infrastructure/graph/ 预留图存储包**：GraphRepository 抽象接口（Phase 1 package-info 级别）
- **Phase 2 结构图谱**：基于已有 ContentTree（章节层级/标题路径）构建 Chunk 关系图，Query 阶段按路径扩展上下文
- **Phase 3 实体图谱**：LLM 驱动的实体识别 + 关系抽取算子，Neo4j 图数据库存储，查询时实体链接 + 多跳遍历
- **Phase 4 全局图谱**：GraphRAG 社区发现 + 社区摘要，支持全局性问题回答

## Capabilities

### New Capabilities

- `knowledge-graph-types`: KG 相关共享类型定义（Entity, Relation, GraphQuery, GraphRetrievalResult），位于 types/ 模块
- `knowledge-graph-index`: Index Domain 新增的 KG 抽取能力——实体抽取、关系抽取、图索引写入算子策略接口，以及 Phase 2/3 的实现
- `knowledge-graph-query`: Query Domain 新增的 KG 检索能力——图扩展、实体链接、图遍历、图向量融合算子策略接口及实现

### Modified Capabilities

<!-- No existing spec requirements change. KG is purely additive. -->

## Impact

- **types/**：新增 4-6 个 POJO 类
- **domain/index/**：新增 2 个策略接口，Phase 2/3 各 2-3 个实现
- **domain/query/**：新增 3 个策略接口，Phase 2/3 各 2-3 个实现
- **infrastructure/**：新增 graph/ 包，Phase 3 引入 Neo4j 驱动依赖
- **case/**：新增 GraphExpansionCase、EntityExtractCase、GraphRAGCase 编排类
- **数据库**：Phase 3 引入 Neo4j（新中间件），Phase 2 无需新存储
- **DAG 配置**：service_index_graph 和 service_query_graph 的 JSONB 配置中预留 KG 相关算子节点位
