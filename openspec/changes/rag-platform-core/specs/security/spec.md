# Spec: Security & Access Control

## Overview

定义 RAG 中台的鉴权、授权、API Key 管理、DataSource 安全、审计和网络安全。

## Requirements

### API Key 管理

- **REQ-SEC-001**: 平台级 API Key (`pk_` 前缀) — 全局管理权限，绑定 RBAC 角色
- **REQ-SEC-002**: Service 级 API Key (`sk_` 前缀) — 绑定到特定 Service，仅能访问该 Service 的 Runtime API
- **REQ-SEC-003**: 内部调用 Token — 服务间 gRPC 互信，不暴露给外部
- **REQ-SEC-004**: API Key 存储使用 bcrypt/argon2 哈希，不可逆
- **REQ-SEC-005**: API Key 创建时返回一次完整 Key，后续仅展示前缀 + 末4位
- **REQ-SEC-006**: API Key 支持有效期设置、手动禁用和吊销
- **REQ-SEC-007**: Service 可拥有多把 API Key，支持无缝轮换

### RBAC (平台级别)

- **REQ-SEC-008**: admin — 全局管理权限（Service/KB CRUD, 授权管理, API Key 管理, DataSource 管理）
- **REQ-SEC-009**: operator — 只读 + 监控告警配置 + 文档管理
- **REQ-SEC-010**: viewer — 只读所有资源 + 监控面板

### 访问控制

- **REQ-SEC-011**: Service Key 只能调用本 Service 的 Runtime API，不可跨 Service 访问
- **REQ-SEC-012**: 检索和生成前校验 Service 对所涉及 KB 的授权关系
- **REQ-SEC-013**: 文档上传前校验操作者对该 KB 的 write 权限
- **REQ-SEC-014**: 生成响应中的 PII 脱敏（可选，Service 级别配置）

### DataSource 安全

- **REQ-SEC-020**: DataSourceConfig 中的敏感字段（appSecret / token / password / connectionString）在持久化时使用 AES-256 加密，读取时解密；加密密钥通过 KMS 或环境变量注入，不写入配置文件
- **REQ-SEC-021**: 飞书 Webhook 验证 X-Lark-Signature 签名，语雀 Webhook 验证 HMAC-SHA256 签名，验签失败返回 401
- **REQ-SEC-022**: DataSource API 的响应中不返回解密后的敏感字段值（appSecret / token / password 返回 `"****"`）

### 审计日志

- **REQ-SEC-030**: 所有 API 操作记录审计日志，包含：操作人/时间/资源类型/资源ID/操作类型/来源IP/结果
- **REQ-SEC-031**: 审计日志不可篡改、不可删除（合规要求）
- **REQ-SEC-032**: 审计日志支持按 Service、时间范围、操作类型检索

### SQL 安全

- **REQ-SEC-040**: sql_validate 强制只读校验：禁止 DML（INSERT/UPDATE/DELETE）、DDL（CREATE/DROP/ALTER）、事务控制（BEGIN/COMMIT/ROLLBACK）
- **REQ-SEC-041**: sql_execute 使用只读数据库账号，权限限定为 SELECT
- **REQ-SEC-042**: SQL 执行超时控制（timeoutMs），默认 5s

### 网络安全

- **REQ-SEC-050**: 支持 IP 白名单限制（可选，Service 级别配置）
- **REQ-SEC-051**: 所有外部 API 强制 HTTPS
- **REQ-SEC-052**: 对内 gRPC 支持 mTLS（可选）
