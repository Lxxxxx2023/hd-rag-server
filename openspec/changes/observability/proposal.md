# Proposal: Observability & Quality Evaluation

## Summary

为 RAG 平台建立完整的可观测性和质量评估体系。包括：全链路追踪（OpenTelemetry + trace_id）、三层指标监控（Platform/Service/Operator 级）、告警规则引擎、LLM/Embedding/Rerank 成本按 Service 分摊、RAGAS 四维度离线评估、线上流量 LLM-as-Judge 采样评估、用户反馈信号采集与分析、以及低分 Case 自动入库 → 根因归类 → 可执行改进项的完整反馈回路。

## User Stories

### 运维工程师

- **US-1**: 作为**运维工程师**，我想查看**检索命中率、生成质量、延迟分布和成本统计**（按 Service × Model × 时间），以便持续优化服务质量和控制运营成本。
- **US-2**: 作为**运维工程师**，我想通过**离线评估和线上采样反馈回路**，对比不同配置的检索/生成效果，以便数据驱动地迭代 RAG 策略。
- **US-3**: 作为**运维工程师**，我想配置**告警规则**（错误率 > 5%、P99 > 10s、LLM API 可用性 < 99%），以便第一时间发现和响应线上故障。

### 业务开发者

- **US-4**: 作为**业务开发者**，我想通过 trace_id 回溯每次查询的完整执行链路（意图 → 检索 → 融合 → 重排 → LLM → 引用），以便排查"为什么没召回"或"为什么这么回答"的问题。

### 平台管理员

- **US-5**: 作为**平台管理员**，我想查看各 Service 的日/周/月**成本报表**，以便进行资源规划和成本优化。

## User Actions（关键操作路径）

### 路径 A：查询质量诊断

```
业务方报告查询结果不理想 → 运维获取该请求的 trace_id
→ 查询 Trace API 获取完整 DAG 执行图 → 查看各节点 span（检索得分/重排结果/LLM prompt）
→ 定位问题环节（如：检索未命中/分块不当/LLM 幻觉）
→ 调整配置后重新评估 → 对比历史指标确认改善
```

### 路径 B：评估驱动的策略迭代

```
运维创建评测数据集（人工标注 + 线上抽样 + LLM 合成）
→ 选评测集 + 指定 Service → 运行离线评估 → 获取 RAGAS 四维度指标
→ 对比历史评估结果 → 确定改进方向（分块策略/检索配置/Prompt 模板）
→ 调整配置 → 重新评估 → 指标提升 → 发布上线
→ 线上 5% 流量自动抽样评估 → 低分 Case 自动入库 → 根因归类
```

### 路径 C：故障告警与响应

```
LLM API 可用性下降 → P1 告警触发 → 自动切换 fallback 模型
→ 运维收到通知 → 查看 Service 级指标面板 → 确认影响范围
→ 联系 LLM Provider → 解决后切回 primary → 关闭告警
```

## Motivation

RAG 系统是"灰盒"——检索和生成由多个策略算子编排而成，质量受分块策略、检索配置、Prompt 模板、模型选择等多因素影响。没有可观测性，出了问题只能"靠猜"；没有评估体系，优化就是"随机试"。本模块为 RAG 平台建立从监控 → 评估 → 反馈 → 改进的完整质量闭环。

## Core Design Decisions

| # | 决策 | 说明 |
|---|------|------|
| 1 | trace_id 贯穿全链路 | 每个 RAG 请求生成唯一 trace_id，返回给调用方，可跨系统排查 |
| 2 | 三级指标体系 | Platform 级（全局健康）/ Service 级（租户粒度）/ Operator 级（算子性能） |
| 3 | OpenTelemetry 兼容导出 | 对接 Jaeger / Grafana Tempo，不绑定特定后端 |
| 4 | RAGAS 四维度评估 | Faithfulness / Answer Relevance / Context Relevance / Context Recall |
| 5 | 线上线下双评估 | 离线评估（评测集）+ 线上采样（LLM-as-Judge，默认 5% 流量） |
| 6 | 完整反馈回路 | 低分 Case 自动入库 → 根因归类 → 可执行改进项 |
| 7 | 成本按 Service 分摊 | LLM / Embedding / Rerank 调用成本按 Service × Model × 时间精细化分摊 |

## Non-goals

- 不实现前端 dashboard UI（仅提供 API + Prometheus/Grafana 对接能力）
- 不实现多租户成本计费系统（仅统计和报表）
- 不自动执行改进项（仅建议）

## Dependencies

- [platform-foundation](../platform-foundation/proposal.md) — Service 资源模型
- [rag-query](../rag-query/proposal.md) — Query Pipeline 执行链路（trace 打点目标）
- [document-indexing](../document-indexing/proposal.md) — Index Pipeline 执行链路
