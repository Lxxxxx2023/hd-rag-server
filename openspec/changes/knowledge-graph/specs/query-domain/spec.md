# Spec: Query Domain — Knowledge Graph Retrieval

## Overview

KG 是 Query Domain 的新增消费方式：在文档检索路径中增加 graph_expand（Phase 2）、entity_link + graph_traverse + graph_vector_fusion（Phase 3）、GlobalGraphSearcher（Phase 4）算子。策略接口定义在 domain/query/。

## 1. Query 侧检索策略接口

- **REQ-KG-Q01**: IGraphExpander — expand(List<Chunk> chunks, int expandDepth) → List<Chunk>（Phase 2 实现）
- **REQ-KG-Q02**: IGraphRetriever — retrieve(GraphQuery query) → GraphRetrievalResult（Phase 3 实现）
- **REQ-KG-Q03**: IEntityLinker — link(String mention, String context) → List<Entity>（Phase 3 实现）

## 2. 知识图谱扩展（文档检索路径内）

- **REQ-KG-Q10**: graph_expand — Phase 2 基于 ContentTree 结构关系扩展检索上下文（父/子/兄弟 Chunk），通过 Pipeline Config 的 `kg_expand.enabled` 控制，默认关闭
- **REQ-KG-Q11**: entity_link — Phase 3 查询提及文本 → 图数据库实体消歧匹配，返回候选实体列表
- **REQ-KG-Q12**: graph_traverse — Phase 3 基于 GraphQuery 执行 Cypher 多跳遍历，返回实体/关系及关联 Chunk
- **REQ-KG-Q13**: graph_vector_fusion — Phase 3 图检索结果与向量检索结果的融合：
  - graph_first: 图检索结果优先，向量结果补充
  - interleave: 交替排列
  - score_combine: 分数加权组合

## 3. 全局图谱检索（Phase 4）

- **REQ-KG-Q20**: LeidenCommunityDetector — 对 Neo4j 实体-关系图执行 Leiden 社区发现
- **REQ-KG-Q21**: CommunitySummarizer — LLM 生成社区摘要
- **REQ-KG-Q22**: GlobalGraphSearcher — 社区摘要匹配 + 关键实体返回
- **REQ-KG-Q23**: GraphRAGCase — 编排全局检索流程：社区匹配 → 摘要注入 → LLM 生成全局性答案

## 4. Pipeline Config 扩展

```json
{
  "document_search": {
    "kg_expand": { "enabled": false, "expandDepth": 1 }
  }
}
```

Phase 3 新增节点：
```json
{
  "document_search": {
    "kg": {
      "entity_link": { "enabled": false },
      "graph_traverse": { "enabled": false, "maxHops": 2 },
      "graph_vector_fusion": { "enabled": false, "strategy": "interleave" }
    }
  }
}
```

## 5. Query OperatorType (KG 相关)

```
graph_expand, entity_link, graph_traverse, graph_vector_fusion
```

## 6. Query Domain 数据读取依赖

```
graph_expand ─────────读取──────────▶ ContentTree（通过 IChunkRepository）
entity_link ──────────读取──────────▶ GraphRepository.query()
graph_traverse ───────读取──────────▶ GraphRepository.query()
```
