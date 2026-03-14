[根目录](../CLAUDE.md) > **yu-ai-code-mother-frontend**

---

# yu-ai-code-mother-frontend 模块文档

## 变更记录 (Changelog)

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-03-12 21:20:45 | 初始化 | 创建前端模块文档 |

---

## 模块职责

前端模块负责提供用户交互界面，主要包括：
- 用户注册与登录
- 用户管理（管理员功能）
- AI 代码生成界面
- 健康检查展示

---

## 入口与启动

### 入口文件

`src/main.ts` - Vue 应用入口，负责：
- 创建 Vue 应用实例
- 注册 Pinia 状态管理
- 注册 Vue Router 路由
- 注册 Ant Design Vue 组件库
- 初始化权限控制（access.ts）

### 启动命令

```bash
# 开发环境
npm run dev

# 构建生产版本
npm run build

# 生成 API 接口代码（需后端运行）
npm run openapi2ts
```

### 开发服务器

- 默认地址：`http://localhost:5173`
- 自动热更新

---

## 对外接口

### 页面路由

| 路由路径 | 页面名称 | 组件文件 | 说明 |
|---------|---------|---------|------|
| `/` | 首页 | `HomePage.vue` | 项目首页 |
| `/user/login` | 用户登录 | `UserLoginPage.vue` | 用户登录页面 |
| `/user/register` | 用户注册 | `UserRegisterPage.vue` | 用户注册页面 |
| `/admin/userManage` | 用户管理 | `UserManagePage.vue` | 管理员用户管理页面（需管理员权限） |

### API 接口

位于 `src/api/` 目录，由 `openapi2ts` 自动生成：

| 文件 | 说明 |
|------|------|
| `index.ts` | API 导出入口 |
| `userController.ts` | 用户相关 API |
| `healthController.ts` | 健康检查 API |
| `typings.d.ts` | 类型定义 |

---

## 关键依赖与配置

### package.json 核心依赖

```json
{
  "dependencies": {
    "ant-design-vue": "^4.2.6",  // UI 组件库
    "axios": "^1.11.0",          // HTTP 客户端
    "pinia": "^3.0.3",           // 状态管理
    "vue": "^3.5.17",            // Vue 框架
    "vue-router": "^4.5.1"       // 路由
  },
  "devDependencies": {
    "@umijs/openapi": "^1.13.15", // OpenAPI 代码生成
    "typescript": "~5.8.0",        // TypeScript
    "vite": "^7.0.0",             // 构建工具
    "vue-tsc": "^2.2.10"          // Vue TypeScript 检查
  }
}
```

### 配置文件

| 文件 | 说明 |
|------|------|
| `vite.config.ts` | Vite 构建配置，设置路径别名 `@` |
| `openapi2ts.config.ts` | API 代码生成配置 |
| `eslint.config.ts` | ESLint 配置 |
| `tsconfig.json` | TypeScript 配置 |

### API 生成配置

```typescript
// openapi2ts.config.ts
export default {
  requestLibPath: "import request from '@/request'",
  schemaPath: 'http://localhost:8123/api/v3/api-docs',
  serversPath: './src',
}
```

---

## 数据模型

### 状态管理 (Pinia)

位于 `src/stores/` 目录：

| 文件 | 说明 |
|------|------|
| `loginUser.ts` | 登录用户状态管理 |

### 类型定义

API 相关类型定义由 `openapi2ts` 自动生成在 `src/api/typings.d.ts`。

---

## 测试与质量

### 代码检查

```bash
# ESLint 检查并自动修复
npm run lint

# Prettier 格式化
npm run format

# TypeScript 类型检查
npm run type-check
```

### 测试状态

- 暂无自动化测试
- 建议添加：Vitest + @vue/test-utils

---

## 常见问题 (FAQ)

### Q: 如何生成 API 接口代码？

A: 确保后端服务运行在 `http://localhost:8123`，然后执行：
```bash
npm run openapi2ts
```

### Q: 如何添加新页面？

A:
1. 在 `src/pages/` 目录创建 Vue 组件
2. 在 `src/router/index.ts` 添加路由配置
3. 如需权限控制，在 `src/access.ts` 添加相应逻辑

### Q: 如何调用后端 API？

A: 使用自动生成的 API 接口：
```typescript
import { userController } from '@/api'

// 调用登录接口
const result = await userController.userLogin({
  userAccount: 'xxx',
  userPassword: 'xxx'
})
```

---

## 相关文件清单

| 文件路径 | 说明 |
|---------|------|
| `package.json` | 依赖配置 |
| `vite.config.ts` | Vite 配置 |
| `src/main.ts` | 应用入口 |
| `src/App.vue` | 根组件 |
| `src/router/index.ts` | 路由配置 |
| `src/stores/loginUser.ts` | 用户状态 |
| `src/request.ts` | Axios 封装 |
| `src/access.ts` | 权限控制 |
| `src/api/*.ts` | API 接口（自动生成） |
| `src/pages/*.vue` | 页面组件 |
| `src/components/*.vue` | 公共组件 |
| `src/layouts/*.vue` | 布局组件 |

---

## 目录结构

```
yu-ai-code-mother-frontend/
├── src/
│   ├── api/                    # API 接口（自动生成）
│   ├── components/             # 公共组件
│   │   ├── GlobalFooter.vue    # 全局页脚
│   │   └── GlobalHeader.vue    # 全局页头
│   ├── layouts/                # 布局组件
│   │   └── BasicLayout.vue     # 基础布局
│   ├── pages/                  # 页面组件
│   │   ├── HomePage.vue        # 首页
│   │   ├── admin/              # 管理员页面
│   │   │   └── UserManagePage.vue
│   │   └── user/               # 用户页面
│   │       ├── UserLoginPage.vue
│   │       └── UserRegisterPage.vue
│   ├── router/                 # 路由配置
│   ├── stores/                 # Pinia 状态管理
│   ├── access.ts               # 权限控制
│   ├── main.ts                 # 应用入口
│   ├── request.ts              # HTTP 请求封装
│   └── App.vue                 # 根组件
├── eslint.config.ts            # ESLint 配置
├── openapi2ts.config.ts        # API 生成配置
├── package.json                # 依赖配置
├── tsconfig.json               # TypeScript 配置
└── vite.config.ts              # Vite 配置
```