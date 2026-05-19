## Context

当前 RAG 平台基于 DDD 六边形架构，Index Domain（写路径）产出 Vector + BM25 索引，Query Domain（读路径）通过 Pipeline Config（Phase 1）→ DAG Engine（Phase 2/3）编排检索流程。检索范型仅限于语义相似度和关键词匹配。

KG 能力需要补充结构化关系检索。设计目标：**KG 不是第三个 Domain，而是 Index Domain 新增的产出物 + Query Domain 新增的消费方式**。

平台已有的 `CanonicalDocument → ContentTree → Chunk` 数据流天然包含了结构信息（章节层级、标题路径、溯源定位），这是 Phase 2 结构图谱的基础数据。

## Goals / Non-Goals

**Goals:**
- 在现有的 Index/Query Domain 中预留 KG 扩展点（策略接口 + 数据结构），不新建独立 Domain
- Phase 2 利用已有 ContentTree 结构构建 Chunk 关系图，零额外存储成本
- Phase 3 支持 LLM 驱动的实体关系抽取和图数据库存储
- Phase 4 支持 GraphRAG 风格的社区发现与全局摘要
- 所有 KG 能力 per-Service 可配置启用（通过 DAG 节点 enabled 开关）

**Non-Goals:**
- 不新建独立的 KG Domain
- Phase 1 不实现任何 KG 业务逻辑（仅接口定义）
- 不绑定特定图数据库（通过 Repository 接口抽象）
- 不强制所有 Service 使用 KG（可选开启）

## Decisions

### 1. KG 作为 Index/Query Domain 的延伸，而非独立 Domain

**选择**: KG 算子策略接口分别定义在 `domain/index/` 和 `domain/query/`，图存储实现在 `infrastructure/graph/`

**备选**: 新建 `domain/kg/` 作为第三个领域

**理由**: KG 的数据源头是 Index 路径的解析结果，消费方是 Query 路径的检索流程。独立的 KG Domain 需要额外的防腐层和跨域编排，而"新增接口 + 新增实现"复用现有的 Case 层桥接模式，与现有架构一致。

### 2. 四期渐进式演进

| Phase | 名称 | 核心能力 | 依赖 | 新中间件 |
|-------|------|---------|------|---------|
| 1 | 架构预留 | types POJO + 策略接口定义 | 无 | 无 |
| 2 | 结构图谱 | 基于 ContentTree 的 Chunk 关系图 + 路径扩展检索 | ContentTree（已有） | 无 |
| 3 | 实体图谱 | LLM 抽取实体/关系 + 图数据库存储 + 实体链接检索 | Phase 1 接口 + LLM | Neo4j |
| 4 | 全局图谱 | 社区发现 + 社区摘要 + 全局问答 | Phase 3 + Leiden | 无增量 |

**理由**: 每期产生独立价值，不阻塞后续。Phase 1 成本最低（~10 空文件），Phase 2 几乎免费（利用已有 ContentTree），Phase 3 才需要引入外部依赖。

### 3. 图数据库选型：Phase 3 倾向 Neo4j

**选择**: Phase 3 使用 Neo4j 作为图存储

**备选**: PostgreSQL AGE、ES Graph、MySQL JSONB

**理由**: Neo4j 的 Cypher 查询语言在图遍历和路径查询上的表达力显著优于 SQL/ES。AGE 有与 pgvector 同库运维的优势，但查询语法复杂度和社区成熟度不如 Neo4j。Phase 1/2 不需要图数据库，Phase 3 通过 `GraphRepository` 接口抽象避免绑定。

### 4. 实体抽取：LLM-first，规则兜底

**选择**: 默认使用 LLM few-shot prompt 抽取实体/关系，Service 可配置规则模板补充

**备选**: 专用 NER 模型（如 spaCy/OpenNLP）

**理由**: RAG 中台需要覆盖多领域，专用 NER 模型换领域即失效。LLM + few-shot prompt 零成本适配任意领域，且随着 LLM 能力提升，抽取精度自然增长。

### 5. Chunk 关系图基于 ContentTree 就地构建

**选择**: Phase 2 不新建存储，直接从 `raw_documents.structure`（ContentTree JSONB）和 `service_chunks.structurePath` 计算 Chunk 间关系

**备选**: 新建 chunk_relations 表存储预计算关系

**理由**: ContentTree 本身是 DAG，Chunk 的 structurePath 保留了路径信息。父子（同路径前缀）、兄弟（同父节点）、前后（同父下相邻）关系可实时计算。预计算表优化可后续按需引入，避免 Phase 2 引入不必要的存储成本。

## Risks / Trade-offs

- **[复杂度风险] Phase 3 LLM 抽取成本** → 每 Chunk 1 次 LLM 调用，大规模知识库耗时较长。缓解：按 Service 可选启用、支持增量更新仅处理变更 Chunk、抽取结果持久化避免重复
- **[依赖风险] Neo4j 新中间件运维** → Phase 3 才引入，有充足准备时间。GraphRepository 接口抽象允许后续切换图数据库
- **[精度风险] 实体消歧困难** → 同名词不同实体（"苹果"→水果 vs 公司）是经典难题。缓解：Phase 3 实体链接借助上下文 + 知识库范围约束进行局部消歧
- **[边界风险] 结构图谱可能过弱** → 仅基于 ContentTree 的关系可能不足以解决多跳推理问题。缓解：明确 Phase 2 的定位是"上下文扩展"而非"多跳推理"，后者的需求由 Phase 3 承接
