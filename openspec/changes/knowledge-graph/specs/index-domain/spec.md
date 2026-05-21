# Spec: Index Domain — Knowledge Graph

## Overview

KG 是 Index Domain 的新增产出物：在文档处理管道中增加实体/关系抽取环节，产出 Entity/Relation 写入图数据库。策略接口定义在 domain/index/，实现在 infrastructure/。

## 1. 数据结构（types/ 纯 POJO）

- **REQ-KG-001**: Entity — entityId / name / type（PERSON, ORG, PRODUCT, DATE, LOCATION 等）/ aliases / props
- **REQ-KG-002**: Relation — relationId / subject / object / predicate / sourceChunkId（溯源）/ confidence（0.0-1.0）
- **REQ-KG-003**: GraphQuery — serviceId / entityIds / maxHops / relationTypes / maxResults
- **REQ-KG-004**: GraphRetrievalResult — entities / relations / relatedChunkIds / graphScore

## 2. Index 侧抽取策略接口

- **REQ-KG-010**: IEntityExtractor — canHandle(SourceType) → boolean + extract(CanonicalDocument, ContentNode) → List<Entity>（Phase 1 仅定义接口，Phase 3 提供 LLMEntityExtractor）
- **REQ-KG-011**: IRelationExtractor — extract(List<Entity>, ContentNode) → List<Relation>（Phase 1 仅定义接口，Phase 3 提供 LLMRelationExtractor）
- **REQ-KG-012**: LLM 抽取使用 few-shot prompt，支持按 Service 配置 schema 约束，抽取结果持久化到图数据库
- **REQ-KG-013**: 支持增量抽取 — 仅处理新增或变更的 Chunk

## 3. 图存储抽象

- **REQ-KG-020**: GraphRepository — saveEntities / saveRelations / query / deleteByService / deleteByDocument。Phase 1/2 无实现，Phase 3 Neo4j 实现

## 4. 结构图谱（Phase 2）

- **REQ-KG-030**: 基于 ContentTree 的 Chunk 关系图：PARENT_OF / SIBLING_OF / PREV_OF / NEXT_OF，通过 structurePath 计算，零额外存储
- **REQ-KG-031**: ContentTreeTraverser 基于 structurePath 计算 Chunk 间关系；GraphExpander 沿关系拉入上下文 Chunk

## 5. Index OperatorType (KG 相关)

```
entity_extract, relation_extract, graph_index
```
