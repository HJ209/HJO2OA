---
trigger: glob
description: "HJO2OA 后端开发规范 — DDD 分层、包结构、API 契约、领域规则、持久化、事件、安全与测试。适用于 Java/Spring Boot 后端代码的创建与编辑。"
globs:
  - "**/*.java"
  - "**/pom.xml"
  - "**/src/main/resources/db/migration/**"
---

# HJO2OA 后端开发规范

## 模块与分层

子模块默认包结构：

```
com.hjo2oa.<domain>.<subdomain>/
  interfaces/    → Controller, DTO, 事件监听器
  application/   → ApplicationService, Command, Query, Result, Assembler
  domain/        → 实体, 值对象, 聚合, 领域服务, 领域事件, Repository 接口
  infrastructure/→ Repository 实现, Mapper, 外部适配器
```

依赖方向：`interfaces → application → domain ← infrastructure`

- `domain` 不得依赖 `interfaces` 和具体 `infrastructure` 实现
- `infrastructure → application` 仅限实现应用层端口时使用
- `HJO2OA-Bootstrap` 只做启动装配，不承载业务规则
- 共享能力沉淀在 `HJO2OA-Shared` 或基础设施子模块，不复制到业务模块

## Controller 与 API

- 路径统一 `/api/v1/{module-prefix}/...`
- 返回统一响应体 `ApiResponse<T>`，使用 `@UseSharedWebContract` + `ResponseMetaFactory`
- 入参校验用 `jakarta.validation`，禁止直接返回数据库实体或 Mapper 对象

```java
@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/personalization")
public class PersonalizationController {
    @GetMapping("/profile")
    public ApiResponse<PersonalizationProfileView> getProfile() { ... }
}
```

## 应用层

- 用例编排收敛在 ApplicationService，事务边界收敛在应用层
- 跨模块同步调用仅限只读查询，禁止串联多个模块写操作
- 对外暴露结果使用显式 `Result` / `View` 类型，禁止 `Map<String, Object>` 作为主方案

## 领域层

- 领域对象对自身合法性负责，通过构造校验/工厂方法/业务方法维持约束
- 领域事件只表达"已经发生"的业务事实，不表达命令
- 时间使用 `Instant` + UTC 语义
- 禁止在领域层写入 Controller 逻辑、SQL 或第三方 SDK 调用

## 持久化与数据库

- 数据库变更只通过 Flyway migration 提交，放 `db/migration/sqlserver/`
- 禁止 `ddl-auto` 或手工改库
- MyBatis-Plus 仅在 `infrastructure` 层使用，Mapper 不得泄漏到 `interfaces`/`domain`
- Repository 接口定义在 `domain`，实现放 `infrastructure`
- 时间字段默认 UTC；幂等/版本冲突写入必须显式建模唯一键或版本号

## 异常与日志

- 业务规则失败 → `422 BUSINESS_RULE_VIOLATION` 或模块级错误码
- 参数校验失败 → 统一校验错误结构
- 未捕获异常由统一异常处理转换，禁止将栈信息暴露给前端
- 日志关联 `requestId`、`tenantId`、关键业务主键；禁止打印密码/Token/密钥全文
- 写操作进入审计日志链路

## 安全与事件

- 接口默认纳入统一认证授权体系，身份/租户头部以 API 契约为准
- 数据权限判定复用 `01-组织与权限` 域能力，禁止绕过身份上下文伪造信息
- 跨模块写联动通过事件总线；消费方必须幂等，不能假设事件只投递一次
- 事件命名/载荷/版本遵循 `docs/contracts/unified-event-contract.md`

## 命名约定

| 类型 | 后缀 | 示例 |
|------|------|------|
| Controller | `Controller` | `PersonalizationController` |
| 请求 DTO | `Request` | `SaveOverlayRequest` |
| 命令对象 | `Command` | `RefreshIdentityContextCommand` |
| 应用服务(写) | `ApplicationService` | `PersonalizationProfileApplicationService` |
| 仓储接口 | `Repository` | `IdentityContextSessionRepository` |
| 领域视图 | `View` | `IdentityContextView` |

## 测试

新增功能至少覆盖一种：领域单元测试 / 应用服务测试 / Controller 契约测试 / Repository 适配测试 / ArchUnit 架构约束测试

提交前：目标模块测试通过 + 根工程 `mvn verify` 不破 + Checkstyle 通过 + 新接口有 OpenAPI 暴露
