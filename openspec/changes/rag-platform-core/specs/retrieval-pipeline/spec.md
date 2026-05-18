# Spec: Retrieval Pipeline

## Overview

定义可编排的检索 DAG 及其算子体系。

## Requirements

### DAG 编排

- **REQ-RP-001**: 检索 DAG 支持通过声明式配置定义算子节点和依赖关系
- **REQ-RP-002**: DAG 保存时进行 Schema 校验，确保上下游算子类型兼容
- **REQ-RP-003**: 无依赖关系节点并行执行
- **REQ-RP-004**: 每个节点独立配置超时和降级策略（skip / error / pass_through）

### 算子 Schema

- **REQ-RP-005**: 每个算子声明输入/输出 Schema，类型由算子类型唯一确定
- **REQ-RP-006**: DAG 编排界面/API 基于 Schema 校验节点兼容性
- **REQ-RP-007**: 新算子注册时需附带 Schema 定义和配置参数声明

### 预处理算子

- **REQ-RP-008**: query_rewrite — 使用 LLM 改写用户查询，可生成多个变体
- **REQ-RP-009**: intent_detect — 识别用户意图类别
- **REQ-RP-010**: term_expand — 对查询中的术语做同义词/上下位词扩展
- **REQ-RP-011**: hyde_generate — 生成假设文档用于检索（HyDE 策略）

### 检索算子

- **REQ-RP-012**: vector_search — 基于 embedding 的语义检索，支持配置 top_k 和相似度算法
- **REQ-RP-013**: keyword_search — 基于倒排索引的关键词检索（BM25），支持中文分词
- **REQ-RP-014**: metadata_filter — 基于文档元数据的过滤（类型/日期/状态/自定义字段）
- **REQ-RP-015**: 向量检索和关键词检索可独立启用/禁用

### 融合算子

- **REQ-RP-016**: rrf_fusion — Reciprocal Rank Fusion，可配置 k 值
- **REQ-RP-017**: linear_fusion — 线性加权融合，可配置各路权重
- **REQ-RP-018**: 融合策略由 Service 配置选择

### 排序算子

- **REQ-RP-019**: rerank — 使用 Cross-encoder 精排，可配置 top_k
- **REQ-RP-020**: rerank 模型支持自部署和外部 API 两种模式
- **REQ-RP-021**: deduplicate — 基于内容相似度去重，可配置阈值

### 降级策略

- **REQ-RP-022**: skip — 节点失败/超时时跳过，Pipeline 继续执行
- **REQ-RP-023**: error — 节点失败/超时时终止 Pipeline 并返回错误
- **REQ-RP-024**: pass_through — 节点失败时原样传递上游输出
- **REQ-RP-025**: 降级事件需记录到执行追踪中
