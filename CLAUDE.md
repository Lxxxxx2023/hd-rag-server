# CLAUDE.md

减少 LLM 常见编码错误的行为准则。

## 1. 先思考再编码

**不要假设。不要隐藏困惑。明确列出权衡。**

实现之前：
- 明确说出你的假设。如果不确定，先问。
- 如果有多种解读方式，先列出来——不要默默选一个。
- 如果有更简单的方案，说出来。在有必要时提出异议。
- 如果有不清楚的地方，停下来。指出困惑所在。询问。

## 2. 简洁优先

**用最少的代码解决问题。不要写推测性代码。**

- 不添加未被要求的功能。
- 不为一次性使用的代码创建抽象。
- 不做未被要求的"灵活性"或"可配置性"设计。
- 不为不可能发生的场景添加错误处理。
- 如果你写了 200 行，但 50 行就能搞定——重写。

问自己："资深工程师会觉得这是过度设计吗？" 如果是，就简化。

## 3. 精确修改

**只改必须改的。只清理你自己造成的混乱。**

编辑已有代码时：
- 不要"顺便优化"相邻的代码、注释或格式。
- 不要重构没坏的东西。
- 匹配已有风格，即使你更倾向另一种写法。
- 如果发现不相关的死代码，提出来——但不要删。

当你自己的改动造成了遗留问题时：
- 删除**你的改动**导致不再使用的 import/变量/方法。
- 不要删除已有的死代码，除非明确要求。

检验标准：每一行改动都应该能追溯到用户的需求。

## 4. 目标驱动执行

**定义成功标准。循环验证直到通过。**

将任务转化为可验证的目标：
- "添加校验" → "先为非法输入写测试，再让测试通过"
- "修复 bug" → "先写复现测试，再修复"
- "重构 X" → "确保重构前后测试都通过"

对于多步骤任务，先列出简要计划：
```
1. [步骤] → 验证: [检查项]
2. [步骤] → 验证: [检查项]
3. [步骤] → 验证: [检查项]
```

强成功标准让你能独立闭环。弱标准（"让它能跑就行"）则需要持续澄清。

---

**这些准则生效的标志：** diff 中不必要的改动减少；因过度设计导致的重写减少；澄清问题出现在实现之前而非犯错之后。

## 5. 技术栈

| 类别 | 技术 | 版本 | 备注 |
|----------|-----------|---------|-------|
| 语言 | Java | 17 | -- |
| 框架 | Spring Boot | 3.4.3 | -- |
| ORM | MyBatis-Plus | 3.5.9 | `mybatis-plus-spring-boot3-starter` |
| 业务数据库 | MySQL | 9.1.0 |Druid 连接池 |
| 向量数据库 | PostgreSQL + pgvector | 0.1.6 |通过 Spring JdbcTemplate 访问 |
| 数据库迁移 | Flyway | 11.7.2 | `flyway-mysql`，脚本位于 `rag-server-app/src/main/resources/db/migration/` |
| 对象存储 | MinIO | 8.6.0 | Bucket: `rag` |
| AI 框架 | Spring AI | 1.1.7 | OpenAI 兼容 API |
| 认证 | Sa-Token + JWT | 1.43.0 | -- |
| 文档解析 | Apache Tika + flexmark + PDFBox | 3.2.3 / 0.64.8 / 3.0.5 | Tika 用于通用文档，flexmark 用于 Markdown，PDFBox 用于 PDF |
| 工具库 | Hutool + fastjson2 + Lombok | 5.8.37 / 2.0.43 / 1.18.36 | 雪花 ID 通过 `IdUtil.getSnowflakeNextIdStr()` 生成，BCrypt 通过 `hutool-crypto` |

## 6. 系统架构

### 6.1 模块结构（DDD 六边形架构）

```
Trigger ──→ API ──→ Domain ←── Infrastructure
                    ↑
                  Types
```

| 模块 | 层次 | 职责 |
|--------|-------|----------------|
| `rag-server-types` | 基础层 | 共享常量、`Result<T>` 统一响应、错误码、异常类、`GlobalExceptionHandler`、`UserContextHolder` |
| `rag-server-api` | API 层 | 摄入和用户领域的请求/响应 DTO |
| `rag-server-domain` | 领域层 | 实体、值对象、仓储接口（端口）、领域服务、流水线引擎 |
| `rag-server-infrastructure` | 适配层 | 仓储实现、MyBatis-Plus DAO/PO、MinIO 适配器、pgvector 写入器、配置类 |
| `rag-server-trigger` | 入站层 | REST 控制器（`@RestController`）、Servlet 过滤器、Job/MQ 占位 |
| `rag-server-app` | 装配层 | Spring Boot 入口（`RAGServerApplication`）、`@Configuration` Bean、Flyway 脚本、`application-*.yaml` |

依赖方向：**Trigger → API → Domain ← Infrastructure**。Domain 是核心，不依赖 Infrastructure。
