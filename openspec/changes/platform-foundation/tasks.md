# Tasks: Platform Foundation

## Schema: spec-driven | Progress: 4/38 tasks

---

## 1. Domain Model (5 tasks)

- [ ] PA-001 定义 Service 聚合根与配置值对象
- [x] PA-002 定义 KnowledgeBase 聚合根与文档管理接口
- [ ] PA-003 定义 ServiceKBGrant 关联实体与授权类型枚举
- [ ] PA-004 实现 KB 删除授权检查领域服务
- [ ] PA-005 实现 Service 删除级联清理领域服务

## 2. Repository & Persistence (5 tasks)

- [ ] PA-006 实现 ServiceRepository 及 MyBatis 持久化
- [x] PA-007 实现 KnowledgeBaseRepository 及 MyBatis 持久化
- [ ] PA-008 实现 ServiceKBGrantRepository 及 MyBatis 持久化
- [ ] PA-009 实现授权生效/撤销触发 Index 处理链的领域事件
- [ ] PA-010 创建 raw_documents / service_chunks / service_vectors 表 DDL

## 3. Quota & Rate Limit (2 tasks)

- [ ] PA-011 实现 Service 配额值对象（QPS / 日请求量 / 存储配额）
- [ ] PA-012 实现配额校验领域服务

## 4. Application & API (4 tasks)

- [ ] PA-013 实现 ServiceManagementAppService (CRUD)
- [x] PA-014 实现 KnowledgeBaseManagementAppService (CRUD)
- [ ] PA-015 实现 GrantManagementAppService (授权/撤销)
- [ ] PA-016 实现 Service/KB Management API Controller

---

## 5. API Key Management (7 tasks)

- [ ] SEC-001 定义 ApiKey 聚合根与 Key 生成策略
- [ ] SEC-002 实现平台级 Key 创建与管理（pk_ 前缀 + RBAC 绑定）
- [ ] SEC-003 实现 Service 级 Key 创建与管理（sk_ 前缀）
- [ ] SEC-004 实现 Key 吊销 / 有效期 / 禁用
- [ ] SEC-005 实现 ApiKeyRepository — bcrypt 哈希存储
- [ ] SEC-006 实现 API Key 认证过滤器 — pk_ / sk_ 前缀路由
- [ ] SEC-007 实现 Service Key 跨 Service 访问拦截

## 6. Authorization & RBAC (5 tasks)

- [ ] SEC-008 实现 Service-KB 授权运行时校验
- [ ] SEC-009 实现内部调用 Token 认证
- [ ] SEC-010 定义 RBAC 角色模型 — admin / operator / viewer
- [ ] SEC-011 实现 RBAC 权限校验切面/拦截器
- [ ] SEC-012 实现 IP 白名单过滤器 — Service 级别配置

## 7. Audit Log (4 tasks)

- [ ] SEC-013 定义审计日志实体 — 操作人/时间/资源/操作/来源IP/结果
- [ ] SEC-014 实现审计日志 AOP 切面 — 自动记录所有 API 操作
- [ ] SEC-015 实现审计日志查询服务 — 按 Service/时间/操作类型
- [ ] SEC-016 实现审计日志按月分表 + 不可删除约束

---

## 8. Core Database Tables (3 tasks)

- [ ] INF-001 创建 MySQL 核心表结构（services, knowledge_bases, service_kb_grants, api_keys, audit_logs）— 部分完成：t_knowledge_base 已创建
- [ ] INF-002 配置 JSONB Service 配置字段
- [ ] INF-003 实现审计日志按月分表 DDL 与自动归档

## 9. Redis Cache (3 tasks)

- [ ] INF-004 实现 Service 配置缓存 — TTL 10 min
- [ ] INF-005 实现 API Key → Service 映射缓存 — TTL 5 min
- [ ] INF-006 实现 Service-KB 授权关系缓存 — TTL 5 min

---

## Task Summary

| 模块 | 内容 | Tasks |
|------|------|-------|
| Domain Model | Service/KB/Grant 聚合根与领域服务 | PA-001 ~ PA-005 (5) |
| Repository | MyBatis 持久化 + DDL | PA-006 ~ PA-010 (5) |
| Quota | 配额值对象与校验 | PA-011 ~ PA-012 (2) |
| Application | AppService + Controller | PA-013 ~ PA-016 (4) |
| API Key | Key 生成/存储/认证 | SEC-001 ~ SEC-007 (7) |
| RBAC | 授权校验 + 角色模型 | SEC-008 ~ SEC-012 (5) |
| Audit | 审计日志实体/切面/查询 | SEC-013 ~ SEC-016 (4) |
| DB Tables | 核心表 DDL | INF-001 ~ INF-003 (3) |
| Cache | Redis 缓存层 | INF-004 ~ INF-006 (3) |
| **Total** | | **38** |

## Implementation Order

1. INF-001 → 核心表结构（DDL 先行）
2. PA-001~005 → 领域模型
3. PA-006~010 → 持久化
4. PA-011~012 → 配额
5. PA-013~016 → API 层
6. SEC-001~007 → API Key
7. SEC-008~012 → RBAC
8. SEC-013~016 → 审计日志
9. INF-002~006 → 缓存与配置
