# Proposal: Platform Foundation

## Summary

搭建 RAG 中台的多租户基础架构。实现 Service 资源隔离、KnowledgeBase 文档管理单元、Service-KB N:M 授权体系、API Key 鉴权与 RBAC 角色控制，以及核心数据库表结构。这是整个 RAG 平台的骨架，所有后续 change 依赖此基础。

## User Stories

### 平台管理员

- **US-1**: 作为**平台管理员**，我想**创建和配置 Service**（指定模型、分块策略、检索参数），以便为不同业务线提供隔离的 RAG 服务实例。
- **US-2**: 作为**平台管理员**，我想**创建 KB 并授权给多个 Service**，以便跨业务复用已处理的知识资产。
- **US-3**: 作为**平台管理员**，我想**设置配额和使用限制**（QPS / 日请求量 / 存储配额），以便管控各业务线的资源消耗和成本。
- **US-4**: 作为**平台管理员**，我想**创建和管理 API Key**（平台级 pk_ / Service 级 sk_），以便安全地对外暴露 API 访问能力。
- **US-5**: 作为**平台管理员**，我想查看所有 API 操作的**审计日志**，以便满足合规要求和安全溯源。

## User Actions（关键操作路径）

### 路径 A：开通新业务线

```
平台管理员创建 Service → 配置模型/分块/检索参数 → 创建 API Key (sk_前缀)
→ 创建 KB → 授权 KB 给 Service → 设置配额 → 业务方拿到 Key 开始接入
```

### 路径 B：安全管理

```
平台管理员创建 API Key → 绑定 RBAC 角色 → Key 分发给对应角色人员
→ 所有操作自动记录审计日志 → 管理员按 Service/时间/操作类型检索审计记录
```

## Motivation

RAG 中台需要服务多个业务线，每业务线需要独立的配置空间、数据隔离和访问控制。这是平台的零层基础，没有它，文档索引、检索、数据源接入等能力都无法在多租户下安全运行。

## Core Design Decisions

| # | 决策 | 说明 |
|---|------|------|
| 1 | Service 为独立租户单元 | 逻辑隔离，独立配置模型/chunk/检索/生成 |
| 2 | KB 独立于 Service | 通过 N:M 授权关联，KB 可复用 |
| 3 | Service 独立存储 chunk + embedding | 授权触发 Service 级 Pipeline |
| 4 | API Key 双级体系 | pk_ 平台级 + sk_ Service 级，bcrypt 哈希存储 |
| 5 | RBAC 三角色 | admin（全局管理）/ operator（读写+监控）/ viewer（只读） |
| 6 | 审计日志不可删除 | 按月分表，AOP 自动记录所有 API 操作 |

## Non-goals

- 不实现 OAuth/OIDC 第三方登录（仅 API Key）
- 不实现多级租户层级（如 Organization → Service）
- 不实现计费系统（仅配额管控）

## Dependencies

无 —— 这是平台基础层，所有其他 change 依赖此模块。
