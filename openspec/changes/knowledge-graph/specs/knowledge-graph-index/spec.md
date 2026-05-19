## ADDED Requirements

### Requirement: IEntityExtractor 策略接口
系统 SHALL 在 domain/index/ 中定义 IEntityExtractor 策略接口，用于从 CanonicalDocument 或 Chunk 中抽取实体。

接口定义：
- `canHandle(SourceType) → boolean`: 判断是否支持该数据源类型
- `extract(CanonicalDocument, ContentNode) → List<Entity>`: 从指定文档节点抽取实体列表

Phase 1 仅定义接口，Phase 3 提供 LLM 实现。

#### Scenario: Phase 1 接口定义存在
- **WHEN** Index Domain 编译完成
- **THEN** IEntityExtractor 接口可用，但无实现类注册

### Requirement: IRelationExtractor 策略接口
系统 SHALL 在 domain/index/ 中定义 IRelationExtractor 策略接口，用于从已在实体列表中抽取关系。

接口定义：
- `extract(List<Entity>, ContentNode) → List<Relation>`: 基于已抽取的实体和上下文节点，抽取实体间关系

Phase 1 仅定义接口，Phase 3 提供 LLM 实现。

#### Scenario: Phase 1 接口定义存在
- **WHEN** Index Domain 编译完成
- **THEN** IRelationExtractor 接口可用，但无实现类注册

### Requirement: GraphRepository 存储抽象
系统 SHALL 在 infrastructure/graph/ 中定义 GraphRepository 抽象接口，用于图数据的持久化和查询。

接口定义：
- `saveEntities(String serviceId, List<Entity>) → void`
- `saveRelations(String serviceId, List<Relation>) → void`
- `query(GraphQuery) → GraphRetrievalResult`
- `deleteByService(String serviceId) → void`
- `deleteByDocument(String documentId) → void`

Phase 1 仅定义接口，Phase 3 提供 Neo4j 实现。

#### Scenario: Phase 1 graph 包存在
- **WHEN** Infrastructure 模块编译完成
- **THEN** infrastructure/graph/ 包存在，GraphRepository 接口可用，无实现

### Requirement: Chunk Relation Graph 结构图谱构建
系统 SHALL 在 Phase 2 提供基于 ContentTree 的 Chunk 关系图构建能力。

Chunk 关系类型：
- `PARENT_OF`: 父 Chunk → 子 Chunk（通过 structurePath 前缀判断）
- `SIBLING_OF`: 同一父节点下的兄弟 Chunk（按 structurePath 同级判断）
- `PREV_OF / NEXT_OF`: 同一父节点下的前后 Chunk（按 nodeId 顺序）

#### Scenario: 从 ContentTree 构建 Chunk 关系
- **WHEN** 文档已完成 Chunk 并保留 structurePath（如 "安装 > 环境准备 > JDK配置"）
- **THEN** 系统可计算 "JDK配置" Chunk 的父节点（"环境准备"）和兄弟节点（同级的其他步骤）

### Requirement: LLM 实体抽取算子（Phase 3）
系统 SHALL 在 Phase 3 提供基于 LLM 的实体抽取算子，实现 IEntityExtractor 接口。

抽取流程：
- 将 Chunk 内容传入 LLM，使用 few-shot prompt 引导抽取
- 支持按 Service 配置 schema 约束（限定 EntityType 和 RelationType）
- 抽取结果持久化到图数据库
- 支持增量更新（仅处理新增或变更的 Chunk）

#### Scenario: LLM 抽取实体和关系
- **WHEN** Service 启用 entity_extract 和 relation_extract 算子，且 Chunk 包含 "2024年3月 OpenAI 发布 GPT-4o"
- **THEN** LLM 抽取 Entity{name="OpenAI", type=ORG}、Entity{name="GPT-4o", type=PRODUCT} 和 Relation{predicate="released"}

### Requirement: KG 索引 DAG 节点注册
系统 SHALL 在 service_index_graph 的 DAG 配置中预留 KG 相关算子节点位。

预留节点：
- `entity_extract`: 实体抽取算子（Phase 3 启用）
- `relation_extract`: 关系抽取算子（Phase 3 启用）
- `graph_index`: 图索引写入算子（Phase 3 启用）

每个节点具备 `enabled: false` 默认值，Service 可按需启用。

#### Scenario: Service 未启用 KG 时索引流程不变
- **WHEN** Service 的索引 DAG 中 entity_extract 节点 enabled=false
- **THEN** DAG 执行器跳过该节点，索引流程与未引入 KG 前一致
