# Spec: Generation Pipeline

## Overview

定义从检索结果到最终答案生成的流水线。

## Requirements

### 上下文组装

- **REQ-GP-001**: 接收检索 DAG 输出的 Doc[]，按 token 预算进行上下文组装
- **REQ-GP-002**: 支持三种上下文装配策略：Top-N 截断 / 上下文压缩 / 动态预算分配
- **REQ-GP-003**: token_budget 由 Service 配置，组装时实时计算 token 数
- **REQ-GP-004**: 上下文组装时自动为每个 chunk 生成引用标记（[ref_1], [ref_2]...）

### Prompt 模板系统

- **REQ-GP-005**: 三层模板体系：
  - Layer 1: 平台预置模板（客服/文档总结/代码审查/翻译）
  - Layer 2: Service 自定义模板（继承预置 + 覆盖 + 自定义变量）
  - Layer 3: API 调用时参数覆盖（temperature/max_tokens/模板变量）
- **REQ-GP-006**: 模板变量体系至少包含：{role}, {context}, {query}, {history}, {kb_names}, {date}, {service_name}, {custom_*}
- **REQ-GP-007**: 模板支持条件片段（如：有对话历史时展示，无则隐藏）

### LLM 调用

- **REQ-GP-008**: 支持多模型路由（Service 配置 primary 和 fallback 模型）
- **REQ-GP-009**: 支持流式输出（SSE chunk by chunk）
- **REQ-GP-010**: 故障转移：primary 不可用时自动切换 fallback
- **REQ-GP-011**: 重试策略：可配置最大重试次数和退避策略（fixed / exponential）
- **REQ-GP-012**: 支持配置 temperature、max_tokens、top_p 等生成参数

### 引用溯源

- **REQ-GP-013**: 输出结果必须包含引用映射：[ref_N] → chunk 原始信息（文件/章节/位置）
- **REQ-GP-014**: 引用源附带检索得分（vector_score, keyword_score, rerank_score）
- **REQ-GP-015**: 支持 LLM 引用与 chunk 的自动对齐（citation → context mapping）

### 后处理

- **REQ-GP-016**: safety_filter — 检测和过滤 PII 泄露、敏感词、prompt injection
- **REQ-GP-017**: fact_check — 答案断言与上下文一致性校验（可选，Service 级别开启）
- **REQ-GP-018**: format_convert — 输出格式转换：Markdown / JSON / 纯文本

### 流式输出

- **REQ-GP-019**: LLM 生成过程中实时推送 token 流
- **REQ-GP-020**: 引用映射和检索得分在流结束后一并返回（非流式）
