---
trigger: glob
description: "HJO2OA 前端开发规范 — 目录结构、组件/路由/状态/样式规则、TypeScript 约定、接口调用与测试。适用于 React/TypeScript 前端代码的创建与编辑。"
globs:
  - "frontend/**/*.{ts,tsx,js,jsx}"
  - "frontend/**/*.{css,scss,less}"
---

# HJO2OA 前端开发规范

## 目录结构

```
frontend/
  apps/<app>/src/       → 可独立运行的应用
    app/                → 应用壳、全局 Provider
    routes/             → 路由定义与装配
    pages/              → 页面级容器
    features/           → 按业务特性拆分的可复用功能单元
    components/         → 跨页面复用 UI 组件
    services/           → 接口请求封装、DTO 转换
    hooks/              → 通用 React Hooks
    stores/             → 全局/跨页面状态
    types/              → 共享类型定义
    utils/              → 纯工具函数
    styles/             → 全局/主题样式
  packages/<pkg>/       → 跨应用复用的共享包
  public/               → 公共静态资源
```

- 新功能优先放 `apps/portal-web/src`，跨应用复用时提取到 `packages/`
- 禁止继续把所有逻辑堆到 `App.tsx`，逐步向上述结构收敛

## 组件与页面

- 页面负责组合，不堆放可复用业务逻辑；组件保持单一职责
- 组件 props 必须有明确类型定义，禁止未经约束的 `any` 作为默认方案

```tsx
interface UserCardProps {
  name: string
  email: string
  avatarUrl?: string
}
export function UserCard({ name, email, avatarUrl }: UserCardProps) {
  return (
    <div className="user-card">
      {avatarUrl && <img src={avatarUrl} alt={name} />}
      <h3>{name}</h3>
      <p>{email}</p>
    </div>
  )
}
```

- 表单/表格/卡片/弹窗等基础部件出现第二次复用需求时，提取到共享层

## 路由

- 路由统一用 `react-router-dom`，集中管理，不散落在页面内部
- 页面增长时引入路由模块拆分与懒加载
- 权限/登录态/布局切换在路由层或应用壳层处理

## 状态管理

- 本地状态优先用组件状态；跨页面/跨区域/需持久化时才引入全局 store
- 用户信息、身份上下文、主题、国际化、消息摘要 → 全局状态
- 页面私有筛选/弹窗开关/局部表单态 → 组件状态，不提升为全局
- 引入新状态库前确认项目已有统一方案，禁止并行引入多套状态管理库

## 接口调用

- 网络请求统一通过 `services/` 或共享请求层封装，禁止在页面/组件中散落
- 请求路径/请求头/错误码对齐 `docs/contracts/unified-api-contract.md`
- 正确传递身份上下文、租户、语言、时区、请求追踪信息
- 分页/过滤/排序参数复用统一序列化规则
- 写操作考虑幂等键和重复提交防护

## 样式

- 样式必须通过 Prettier + Stylelint 检查
- 类名清晰表达语义，避免无意义缩写
- 大段样式拆分到对应样式文件，避免组件内堆积内联样式
- 颜色/间距/圆角/阴影等 token 跨页面重复时提取为共享变量或主题层

## TypeScript 与代码风格

- 新增业务代码默认使用 TypeScript
- 导出类型、组件 props、服务返回值必须显式声明
- 格式以根目录 `.prettierrc.json` 为准：无分号、单引号、保留尾随逗号
- ESLint 告警提交前清零

## 测试

新增功能至少覆盖一种：组件渲染测试 / 页面交互测试 / Hook 测试 / 服务层测试

提交前：`npm run frontend:verify` 通过（含 lint + stylelint + typecheck + test + build + format:check）

## 用户体验

- 用户可见文案预留国际化演进空间
- 后端时间戳按 UTC 接收，按用户时区展示
- 错误提示映射后端错误码或统一语义，不透出底层异常文本
- 加载态/空态/错误态必须显式设计
