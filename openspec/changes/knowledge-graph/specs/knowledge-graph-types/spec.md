## ADDED Requirements

### Requirement: Entity 数据模型
系统 SHALL 提供 Entity 纯数据结构，用于表示知识图谱中的实体节点。

Entity 包含以下字段：
- `entityId`: 全局唯一标识
- `name`: 实体名称
- `type`: 实体类型（PERSON, ORG, PRODUCT, DATE, LOCATION 等）
- `aliases`: 别名列表
- `props`: 扩展属性 Map

#### Scenario: 创建实体实例
- **WHEN** 从 Chunk 中抽取到实体 "OpenAI" 且类型为 ORG
- **THEN** 系统创建 Entity{name="OpenAI", type="ORG"} 实例，entityId 自动生成

### Requirement: Relation 数据模型
系统 SHALL 提供 Relation 纯数据结构，用于表示知识图谱中实体间的关系。

Relation 包含以下字段：
- `relationId`: 全局唯一标识
- `subject`: 主体实体引用
- `object`: 客体实体引用
- `predicate`: 关系谓词（如 "founded", "released", "works_at"）
- `sourceChunkId`: 来源 Chunk 标识（溯源）
- `confidence`: 抽取置信度（0.0-1.0）

#### Scenario: 创建关系实例
- **WHEN** 从 Chunk 中抽取到三元组 (OpenAI, released, GPT-4o) 且置信度 0.95
- **THEN** 系统创建 Relation{subject=OpenAI, object=GPT-4o, predicate="released", confidence=0.95} 实例

### Requirement: GraphQuery 数据模型
系统 SHALL 提供 GraphQuery 纯数据结构，用于 Query Domain 向图检索算子传递查询参数。

GraphQuery 包含以下字段：
- `serviceId`: 限定 Service 范围
- `entityIds`: 起始实体 ID 列表
- `maxHops`: 最大遍历跳数
- `relationTypes`: 限定关系类型（空=所有类型）
- `maxResults`: 最大返回结果数

#### Scenario: 构造图查询
- **WHEN** 检索请求需要对实体 "e001" 进行 2 跳遍历，仅关注 "produces" 类型关系
- **THEN** 系统创建 GraphQuery{entityIds=["e001"], maxHops=2, relationTypes=["produces"]}

### Requirement: GraphRetrievalResult 数据模型
系统 SHALL 提供 GraphRetrievalResult 纯数据结构，封装图检索的返回结果。

GraphRetrievalResult 包含以下字段：
- `entities`: 检索到的实体列表
- `relations`: 检索到的关系列表
- `relatedChunkIds`: 关联 Chunk ID 列表（用于与向量检索结果 fusion）
- `graphScore`: 图检索相关性分数

#### Scenario: 图检索返回结果
- **WHEN** 图谱检索完成并找到 3 个实体和 5 条关系，关联到 4 个 Chunk
- **THEN** 系统返回 GraphRetrievalResult 包含所有实体、关系、Chunk ID 及综合图分数
