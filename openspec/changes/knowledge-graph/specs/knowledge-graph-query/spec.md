## ADDED Requirements

### Requirement: IGraphExpander 策略接口
系统 SHALL 在 domain/query/ 中定义 IGraphExpander 策略接口，用于基于 ContentTree 结构关系扩展检索上下文。

接口定义：
- `expand(List<Chunk>, int expandDepth) → List<Chunk>`: 基于已检索到的 Chunk，沿 ContentTree 关系扩展父/子/兄弟 Chunk

Phase 1 仅定义接口，Phase 2 基于 ContentTree 实现。

#### Scenario: Phase 2 结构扩展检索上下文
- **WHEN** 检索返回 Chunk "JDK配置"（structurePath="安装 > 环境准备 > JDK配置"），expandDepth=1
- **THEN** IGraphExpander 拉入父节点 Chunk "环境准备" 和兄弟节点 Chunk "Maven配置"

### Requirement: IGraphRetriever 策略接口
系统 SHALL 在 domain/query/ 中定义 IGraphRetriever 策略接口，用于从图数据库检索实体和关系。

接口定义：
- `retrieve(GraphQuery) → GraphRetrievalResult`: 执行图遍历并返回实体、关系及关联 Chunk

Phase 1 仅定义接口，Phase 3 基于 Neo4j 实现（通过 GraphRepository）。

#### Scenario: Phase 3 多跳图遍历
- **WHEN** GraphQuery 指定 entityIds=["OpenAI"], maxHops=2
- **THEN** IGraphRetriever 返回 OpenAI 的 1-hop 邻居（如 "GPT-4o"）和 2-hop 邻居（如 "Microsoft"），以及关联 Chunk ID

### Requirement: IEntityLinker 策略接口
系统 SHALL 在 domain/query/ 中定义 IEntityLinker 策略接口，用于将查询中的文本提及链接到知识图谱中的实体。

接口定义：
- `link(String mention, String context) → List<Entity>`: 根据提及文本和上下文消歧，返回候选实体列表（按置信度排序）

Phase 1 仅定义接口，Phase 3 实现。

#### Scenario: 实体链接消歧
- **WHEN** 查询中包含 "苹果" 且上下文提及 "发布新版本"、"操作系统"
- **THEN** IEntityLinker 返回 Entity{name="苹果公司", type=ORG}，而非 Entity{name="苹果", type=FRUIT}

### Requirement: Graph-Vector Fusion 融合算子
系统 SHALL 在 Phase 3 提供图检索结果与向量检索结果的融合能力。

融合方式：
- `graph_first`: 图检索结果优先，向量结果补充
- `interleave`: 两种结果交替排列
- `score_combine`: 将 graphScore 与 vectorScore 加权合并排序

#### Scenario: 图-向量融合检索
- **WHEN** 执行了一次查询，向量检索返回 20 条 Chunk，图检索返回 10 条关联 Chunk，融合策略为 interleave
- **THEN** 系统交替排列两路结果，去重后返回融合列表

### Requirement: KG 检索 DAG 节点注册
系统 SHALL 在 service_query_graph 的 DAG 配置中预留 KG 相关算子节点位。

预留节点：
- `graph_expand`: 结构图谱扩展（Phase 2 启用）
- `entity_link`: 实体链接算子（Phase 3 启用）
- `graph_traverse`: 图遍历检索算子（Phase 3 启用）
- `graph_vector_fusion`: 图-向量融合算子（Phase 3 启用）

每个节点具备 `enabled: false` 默认值，Service 可按需启用。

#### Scenario: Service 启用结构图谱扩展
- **WHEN** Service 的查询 DAG 中 graph_expand 节点 enabled=true 且 expandDepth=1
- **THEN** 在 fusion 之前执行 graph_expand，扩展每个检索结果的父/子/兄弟 Chunk
