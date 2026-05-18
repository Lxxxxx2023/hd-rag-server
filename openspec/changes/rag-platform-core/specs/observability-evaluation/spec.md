# Spec: Observability & Evaluation

## Overview

定义 RAG 中台的可观测性体系，包括链路追踪、指标监控、成本统计、质量评估和反馈回路。

## Requirements

### 链路追踪 (Tracing)

- **REQ-OBS-001**: 每条 RAG 请求生成唯一 trace_id，贯穿整个 DAG 执行链路
- **REQ-OBS-002**: DAG 每个算子节点对应一个 span，记录输入/输出/耗时/状态/成本
- **REQ-OBS-003**: 并行执行的节点（如 vector_search + keyword_search）以并行 span 呈现
- **REQ-OBS-004**: span 记录关键输入输出摘要（不记录完整 LLM 内容，避免敏感信息泄露）
- **REQ-OBS-005**: 导出 OpenTelemetry 兼容格式，对接 Jaeger / Grafana Tempo
- **REQ-OBS-006**: trace_id 返回给调用方，便于跨系统排查

### 指标监控 (Metrics)

- **REQ-OBS-007**: Platform 级指标：QPS 总量、错误率、P99 延迟、活跃 Service 数、Kafka lag
- **REQ-OBS-008**: Service 级指标：QPS、错误率、P99 延迟、日成本、活跃请求数
- **REQ-OBS-009**: Operator 级指标：各算子 P50/P99 延迟、token 消耗速率
- **REQ-OBS-010**: 指标导出 Prometheus 兼容格式

### 告警

- **REQ-OBS-011**: 告警规则 - Service 错误率 > 5%，级别 P1，通知 Service owner
- **REQ-OBS-012**: 告警规则 - P99 > 10s，级别 P2，通知平台运维
- **REQ-OBS-013**: 告警规则 - LLM API 可用性 < 99%，级别 P1，自动切换 fallback
- **REQ-OBS-014**: 告警规则 - Kafka lag > 1000，级别 P2，通知 + 自动扩展消费者
- **REQ-OBS-015**: 告警规则 - Service 日成本异常飙升，级别 P2

### 成本追踪

- **REQ-OBS-016**: 按调用链路分摊 LLM / Embedding / Rerank 成本到每个 Service
- **REQ-OBS-017**: 成本统计维度：Service × Model × 时间范围
- **REQ-OBS-018**: 本地部署模型（如 Rerank）按 GPU 资源占用比例折算成本
- **REQ-OBS-019**: 提供 Service 日/周/月成本报表

### 离线评估

- **REQ-OBS-020**: 内置评测数据集管理（CRUD），每条包含：question, expected_answer, relevant_chunk_ids, difficulty, category
- **REQ-OBS-021**: 评测集来源：人工标注 / 生产流量抽样 + 验证 / LLM 合成 + 人工审核
- **REQ-OBS-022**: 评估指标 — Faithfulness, Answer Relevance, Context Relevance, Context Recall（RAGAS 标准四维度）
- **REQ-OBS-023**: 评估支持 Retriever-only 模式（不跑 LLM，只评估检索召回）和 Full RAG 模式
- **REQ-OBS-024**: 评估执行：选定评测集 → 指定 Service → 跑评估 → 输出指标报告（支持历史对比）

### 线上采样评估

- **REQ-OBS-025**: 从生产流量中按比例抽样（默认 5%，可配置），自动触发 LLM-as-Judge 评估
- **REQ-OBS-026**: 抽样评估结果入库，低分 Case 自动标记待审核
- **REQ-OBS-027**: 采样评估的 LLM-as-Judge 调用不计入 Service 本身成本（平台承担）

### 用户反馈信号

- **REQ-OBS-028**: SDK/API 支持业务方上报用户反馈（点赞/点踩/复制/追问）
- **REQ-OBS-029**: 反馈信号与 trace_id 关联，可回溯完整链路
- **REQ-OBS-030**: 用户反馈仪表板：按 Service × 时间展示满意度趋势

### 反馈回路

- **REQ-OBS-031**: 低分评测 Case 和用户负反馈 Case 自动入库待审核
- **REQ-OBS-032**: 审核后归类根因：文档缺失 / 分块不当 / 检索遗漏 / LLM 幻觉 / 其他
- **REQ-OBS-033**: 根因分类关联到可执行改进项（补充文档 / 调整分块 / 优化检索 DAG）

### 查询调试

- **REQ-OBS-034**: 提供 Trace 查询界面，按 trace_id / Service / 时间范围检索
- **REQ-OBS-035**: 单 Trace 展示完整 DAG 执行图 + 每个节点的详情（输入/输出/耗时/状态）
- **REQ-OBS-036**: 支持"为什么没召回"分析——给定 query 和未命中的 document，展示检索得分和排序过程
