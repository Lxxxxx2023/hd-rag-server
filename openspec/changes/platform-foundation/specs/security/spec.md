# Spec: Security & Access Control

## Overview

定义 RAG 中台的用户认证、API Key 管理、RBAC 权限控制、审计日志和网络安全。

## Requirements

### 用户认证

- **REQ-SEC-000**: 用户通过用户名+密码登录，密码使用 BCrypt 哈希存储
- **REQ-SEC-000a**: 认证成功后返回 JWT Token，后续请求通过 `Authorization: Bearer <token>` 携带
- **REQ-SEC-000b**: 用户注册时自动登录并返回 Token
- **REQ-SEC-000c**: 请求链路中通过 `StpUtil.getLoginIdAsString()` 获取当前用户ID，替代硬编码审计字段
- **REQ-SEC-000d**: 用户上下文基于 Sa-Token 实现，JWT 模式，Token 有效期 24 小时

### API Key 管理

- **REQ-SEC-001**: 平台级 API Key (`pk_` 前缀) — 全局管理权限，绑定 RBAC 角色
- **REQ-SEC-002**: Service 级 API Key (`sk_` 前缀) — 绑定到特定 Service，仅能访问该 Service 的 Runtime API
- **REQ-SEC-003**: 内部调用 Token — 服务间 gRPC 互信，不暴露给外部
- **REQ-SEC-004**: API Key 存储使用 bcrypt 哈希，不可逆
- **REQ-SEC-005**: API Key 创建时返回一次完整 Key，后续仅展示前缀 + 末4位
- **REQ-SEC-006**: API Key 支持有效期设置、手动禁用和吊销
- **REQ-SEC-007**: Service 可拥有多把 API Key，支持无缝轮换

### RBAC (平台级别)

- **REQ-SEC-008**: admin — 全局管理权限（Service/KB CRUD, 授权管理, API Key 管理）
- **REQ-SEC-009**: operator — 只读 + 监控告警配置 + 文档管理
- **REQ-SEC-010**: viewer — 只读所有资源 + 监控面板

### 访问控制

- **REQ-SEC-011**: Service Key 只能调用本 Service 的 Runtime API，不可跨 Service 访问
- **REQ-SEC-012**: 检索和生成前校验 Service 对所涉及 KB 的授权关系
- **REQ-SEC-013**: 文档上传前校验操作者对该 KB 的 write 权限

### 审计日志

- **REQ-SEC-020**: 所有 API 操作记录审计日志，包含：操作人/时间/资源类型/资源ID/操作类型/来源IP/结果
- **REQ-SEC-021**: 审计日志不可篡改、不可删除（合规要求）
- **REQ-SEC-022**: 审计日志支持按 Service、时间范围、操作类型检索

### 网络安全

- **REQ-SEC-030**: 支持 IP 白名单限制（可选，Service 级别配置）
- **REQ-SEC-031**: 所有外部 API 强制 HTTPS
- **REQ-SEC-032**: 对内 gRPC 支持 mTLS（可选）
