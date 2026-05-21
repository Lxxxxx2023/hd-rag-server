# Tasks: Observability & Quality Evaluation

## Schema: spec-driven | Progress: 0/36 tasks

---

## 1. Tracing (5 tasks)

- [ ] OBS-001 实现 trace_id 生成器 — 每个 RAG 请求唯一 trace
- [ ] OBS-002 实现 DAG Span 拦截器 — 每个算子节点自动记录 span
- [ ] OBS-003 实现 Span 输入输出摘要记录 — 不记录完整 LLM 内容
- [ ] OBS-004 实现 OpenTelemetry 导出器 — 对接 Jaeger / Grafana Tempo
- [ ] OBS-005 trace_id 返回调用方 — 加入 API Response Header

## 2. Metrics (4 tasks)

- [ ] OBS-006 实现 Platform 级指标收集 — QPS/错误率/P99/活跃Service/Kafka lag
- [ ] OBS-007 实现 Service 级指标收集 — QPS/错误率/P99/日成本/活跃请求
- [ ] OBS-008 实现 Operator 级指标收集 — P50/P99 延迟/token 消耗
- [ ] OBS-009 实现 Prometheus 指标导出

## 3. Alerts (6 tasks)

- [ ] OBS-010 实现告警规则引擎 — 阈值检测 + 级别分类
- [ ] OBS-011 配置 Service 错误率 > 5% 告警 (P1)
- [ ] OBS-012 配置 P99 > 10s 告警 (P2)
- [ ] OBS-013 配置 LLM API 可用性 < 99% 告警 + 自动切换 fallback (P1)
- [ ] OBS-014 配置 Kafka lag > 1000 告警 + 自动扩展消费者 (P2)
- [ ] OBS-015 配置 Service 日成本异常飙升告警 (P2)

## 4. Cost Tracking (4 tasks)

- [ ] OBS-016 实现 LLM / Embedding / Rerank 调用成本记录
- [ ] OBS-017 实现成本分摊计算 — 按 Service × Model × 时间
- [ ] OBS-018 实现本地模型 GPU 成本折算
- [ ] OBS-019 实现 Service 日/周/月成本报表 API

## 5. Offline Evaluation (5 tasks)

- [ ] OBS-020 实现评测数据集 CRUD — question/expected_answer/relevant_chunks/difficulty/category
- [ ] OBS-021 实现评测集来源管理 — 人工标注 / 生产抽样 / LLM 合成
- [ ] OBS-022 实现 RAGAS 四维度评估指标 — Faithfulness, Answer Relevance, Context Relevance, Context Recall
- [ ] OBS-023 实现 Retriever-only 评估模式
- [ ] OBS-024 实现 Full RAG 评估模式
- [ ] OBS-025 实现评估执行引擎 — 选定评测集 + 指定 Service → 指标报告 + 历史对比

## 6. Online Sampling (4 tasks)

- [ ] OBS-026 实现生产流量比例抽样器 — 默认 5% 可配置
- [ ] OBS-027 实现 LLM-as-Judge 自动评估 — 抽样结果入库
- [ ] OBS-028 实现低分 Case 自动标记待审核
- [ ] OBS-029 采样评估 LLM 调用不计入 Service 成本

## 7. User Feedback (3 tasks)

- [ ] OBS-030 实现用户反馈上报 API — 点赞/点踩/复制/追问 + trace_id 关联
- [ ] OBS-031 实现反馈仪表板数据聚合 — 按 Service × 时间满意度趋势

## 8. Feedback Loop (3 tasks)

- [ ] OBS-032 实现低分 Case 自动入库待审核
- [ ] OBS-033 实现根因归类模型 — 文档缺失/分块不当/检索遗漏/LLM 幻觉/其他
- [ ] OBS-034 实现根因 → 可执行改进项关联

## 9. Trace Query (2 tasks)

- [ ] OBS-035 实现 Trace 查询 API — 按 trace_id / Service / 时间范围
- [ ] OBS-036 实现单 Trace 详情 — DAG 执行图 + 节点详情

---

## Task Summary

| 模块 | 内容 | Tasks |
|------|------|-------|
| Tracing | trace_id + Span + OpenTelemetry 导出 | OBS-001 ~ OBS-005 (5) |
| Metrics | Platform/Service/Operator + Prometheus | OBS-006 ~ OBS-009 (4) |
| Alerts | 告警规则引擎 + 5 条默认规则 | OBS-010 ~ OBS-015 (6) |
| Cost | 成本记录 + 分摊 + 报表 | OBS-016 ~ OBS-019 (4) |
| Offline Eval | 评测集 CRUD + RAGAS 四维度 + 评估引擎 | OBS-020 ~ OBS-025 (6) |
| Online Sampling | 流量抽样 + LLM-as-Judge + 低分标记 | OBS-026 ~ OBS-029 (4) |
| User Feedback | 反馈上报 API + 仪表板数据 | OBS-030 ~ OBS-031 (2) |
| Feedback Loop | 低分入库 + 根因归类 + 改进项关联 | OBS-032 ~ OBS-034 (3) |
| Trace Query | Trace 查询 API + 执行图详情 | OBS-035 ~ OBS-036 (2) |
| **Total** | | **36** |

## Implementation Order

1. OBS-001~005 → Tracing（链路先行，为其他模块提供 trace_id）
2. OBS-006~009 → Metrics
3. OBS-010~015 → Alerts
4. OBS-016~019 → Cost Tracking
5. OBS-020~025 → Offline Evaluation
6. OBS-026~029 → Online Sampling
7. OBS-030~031 → User Feedback
8. OBS-032~034 → Feedback Loop
9. OBS-035~036 → Trace Query
