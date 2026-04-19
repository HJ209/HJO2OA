# Bootstrap-启动装配-模块功能

## 模块定位

`HJO2OA-Bootstrap` 是 HJO2OA 后端的统一启动模块和最终可部署单元，负责把 `HJO2OA-Shared` 与各业务模块按既定边界装配为一个可运行的 Spring Boot 应用。

它承担的是**启动入口、依赖装配、全局组件注册、环境配置加载和运行时 Profile 切换**职责，不承担任何业务主数据和业务规则。

## 模块目标

- 提供单一启动入口，保证一期按“模块化单体”形态落地。
- 通过 Maven Profile 控制默认装配、一期后置装配和二期预留装配范围。
- 通过 Spring Profile 管理 `local/dev/test/prod` 环境差异，避免环境配置散落到各模块。
- 把 Shared 的 Web、安全、租户、审计、消息和持久化能力统一注册到运行容器。

## 边界约束

- `Bootstrap` 负责装配，不拥有业务主数据，也不定义业务接口契约。
- `Bootstrap` 可以依赖 `Shared` 和各业务模块 jar，但业务模块不得依赖 `Bootstrap`。
- Maven Profile 只负责**构建期/打包期依赖闭包**，Spring Profile 只负责**运行期环境配置**，两者不得混用。
- `Bootstrap` 默认只装配一期最小闭环子模块，不得在默认构建中提前引入一期后置或二期模块。

## 启动装配范围

### 默认装配

| 装配范围 | 模块/子模块 | 说明 |
|----------|-------------|------|
| 共享层 | `HJO2OA-Shared` | 统一横切能力与基础约定 |
| `00-平台基础设施` | `event-bus`、`audit`、`cache`、`config`、`data-i18n`、`dictionary`、`error-code`、`attachment`、`i18n`、`timezone`、`tenant`、`scheduler`、`security` | 一期运行底座 |
| `01-组织与权限` | `org-structure`、`position-assignment`、`person-account`、`role-resource-auth`、`data-permission`、`identity-context` | 一期身份与权限闭环 |
| `02-流程与表单` | `process-definition`、`process-instance`、`action-engine`、`form-metadata`、`form-renderer`、`todo-center` | 一期流程与待办闭环 |
| `04-门户与工作台` | `widget-config`、`aggregation-api`、`portal-home` | 一期工作台入口 |
| `06-消息移动与生态` | `message-center`、`event-subscription`、`channel-sender`、`mobile-support` | 一期消息与移动链路 |

### 一期后置装配

通过 `bootstrap-with-phase1-deferred` Profile 显式接入：

- `01-org-sync-audit`
- `02-process-monitor`
- `04-portal-designer`
- `04-portal-model`
- `04-personalization`
- `06-ecosystem`

### 二期预留装配

通过 `bootstrap-with-phase2-reserved` Profile 显式接入：

- `03-内容与知识`：`content-storage`、`content-lifecycle`、`category-management`、`content-permission`、`content-search`、`content-statistics`
- `05-协同办公应用`：`document-mgmt`、`meeting-mgmt`、`schedule-task`、`attendance`、`bulletin-fileshare`、`contract-asset`、`cross-app-linkage`
- `07-数据服务与集成`：`open-api`、`connector`、`data-sync`、`data-service`、`report`、`governance`

## Profile 策略

### Maven Profile

| Profile | 用途 | 说明 |
|---------|------|------|
| 默认无 Profile | 一期最小闭环 | 仅装配 `00/01/02/04/06` 必做清单 |
| `bootstrap-with-phase1-deferred` | 一期增强/后置接入 | 仅在项目明确需要时接入一期后置子模块 |
| `bootstrap-with-phase2-reserved` | 二期模块接入 | 显式引入 `03/05/07` 二期模块 |

### Spring Profile

| Profile | 配置文件 | 说明 |
|---------|----------|------|
| 默认 | `application.yml` | 公共基础配置与默认值 |
| `local` | `application-local.yml` | 本地开发、调试日志、本地依赖地址 |
| `dev` | `application-dev.yml` | 开发环境连接信息 |
| `test` | `application-test.yml` | 测试环境连接信息 |
| `prod` | `application-prod.yml` | 生产环境连接信息与更保守的日志级别 |

## 环境配置范围

`Bootstrap` 统一维护以下配置分组：

- `server.*`
- `spring.datasource.*`
- `spring.data.redis.*`
- `spring.rabbitmq.*`
- `mybatis-plus.*`
- `hjo2oa.tenant.*`
- `hjo2oa.security.*`
- `hjo2oa.audit.*`
- `hjo2oa.messaging.*`
- `hjo2oa.storage.*`
- `hjo2oa.cache.*`

这些配置只描述运行参数，不改变业务模块的数据所有权和边界。

## 与各模块关系

| 模块 | 关系 | 说明 |
|------|------|------|
| `Shared` | 直接依赖 | 提供全局共享能力和统一约定 |
| `00-平台基础设施` | 默认装配 | 提供租户、审计、事件、缓存、配置、安全等底座实现 |
| `01-组织与权限` | 默认装配 | 为安全链路和身份上下文提供主数据与业务能力 |
| `02-流程与表单` | 默认装配 | 提供流程、表单、待办闭环能力 |
| `03-内容与知识` | 二期预留 | 仅在二期 Profile 中装配 |
| `04-门户与工作台` | 默认装配 | 提供首页、聚合接口与工作台入口 |
| `05-协同办公应用` | 二期预留 | 仅在二期 Profile 中装配 |
| `06-消息移动与生态` | 默认装配 | 提供消息、提醒、渠道和移动端支持 |
| `07-数据服务与集成` | 二期预留 | 仅在二期 Profile 中装配 |

## 一期交付边界

- 一期必须交付一个可启动的 Spring Boot 主应用和完整的 `application*.yml` 分环境配置骨架。
- 一期默认装配范围只覆盖 `00/01/02/04/06` 必做子模块，不包含一期后置和二期模块。
- 一期允许通过 Maven Profile 预留后置和二期接入点，但不得把这些能力变成默认依赖。
- 一期 `Bootstrap` 只负责单体形态启动装配，不拆分多运行时主程序，不建设多应用集群编排策略。

## 模块目录索引

| 目录 | 说明 |
|------|------|
| `src/main/java/com/hjo2oa/bootstrap/` | Spring Boot 启动入口与后续全局装配代码 |
| `src/main/resources/application.yml` | 默认公共配置 |
| `src/main/resources/application-local.yml` | 本地环境配置 |
| `src/main/resources/application-dev.yml` | 开发环境配置 |
| `src/main/resources/application-test.yml` | 测试环境配置 |
| `src/main/resources/application-prod.yml` | 生产环境配置 |
