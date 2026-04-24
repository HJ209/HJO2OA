---
trigger: always_on
description: "HJO2OA 项目通用约定 — 技术基线、阶段约束、协作规则与禁止项。适用于所有前后端开发任务。"
---

# HJO2OA 项目通用约定

## 技术基线

- 后端：Java 17 / Spring Boot 3.3.6 / MyBatis-Plus / SQL Server 2017 / Flyway / MapStruct / Lombok
- 前端：React 18 / TypeScript / Vite / react-router-dom / Vitest / npm workspaces
- 质量：后端 Checkstyle + JaCoCo + ArchUnit；前端 ESLint 9 + Prettier + Stylelint

## 阶段约束

- 前端统一 React 路线，禁止引入 Vue / JSP / 其他平行前端栈
- 数据库基线为 SQL Server，禁止新增 PostgreSQL/MySQL 专属语法作为主实现
- 数据库结构变更一律通过 Flyway migration 提交，禁止手工改库
- 前端新页面和公共能力优先在 `frontend/apps/portal-web` 沉淀
- 后端跨模块写联动优先事件驱动，禁止随意引入跨域强耦合同步写调用

## 协作规则

- 新增接口/事件/错误码前，先明确契约再并行开发（参考 `docs/contracts/unified-api-contract.md`）
- 后端返回 `data` 为契约模型，前端在 `services/` 或 `features/` 层做 ViewModel 转换
- 前端容忍后端新增可选字段和枚举值；后端不得无版本控制地删除前端已使用字段
- 联调问题优先基于 `requestId` 定位

## 禁止项

- 禁止在源码、配置或构建产物中硬编码密钥、Token、密码
- 禁止绕过统一响应体 `ApiResponse<T>`、统一错误码、统一请求头约定
- 禁止手工改数据库结构而不提交 Flyway migration
- 禁止在多个模块重复实现审计、身份、附件、字典等基础能力
- 禁止无契约评审即新增跨模块同步写调用
- 禁止未通过最小验证（后端 `mvn verify` / 前端 `npm run frontend:verify`）即合入主线

## 验证入口

- 后端：`mvn -B -pl <module-path> -am verify`
- 前端：`npm run frontend:verify`
