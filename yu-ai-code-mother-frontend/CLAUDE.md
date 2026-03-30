[根目录](../CLAUDE.md) > **yu-ai-code-mother-frontend**

---

# yu-ai-code-mother-frontend 模块文档

## 变更记录 (Changelog)

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-03-28 | 更新 | 确认文档完整性，检查模块覆盖率 |
| 2026-03-26 | 更新 | 补充 appController API、权限控制流程、stores 说明 |
| 2026-03-12 21:20:45 | 初始化 | 创建前端模块文档 |

---

## 模块职责

前端模块负责提供用户交互界面，主要包括：
- 用户注册与登录
- 用户管理（管理员功能）
- 应用管理（创建/查看/部署）
- AI 代码流式生成展示（SSE）

---

## 入口与启动

`src/main.ts` 负责：
- 创建 Vue 应用实例
- 注册 Pinia、Vue Router、Ant Design Vue
- 导入 `src/access.ts` 初始化全局路由守卫

```bash
npm install
npm run openapi2ts   # 需后端运行，生成 src/api/
npm run dev          # http://localhost:5173
npm run build
npm run lint
npm run format
npm run type-check
```

---

## 路由

| 路径 | 组件 | 权限 | 说明 |
|------|------|------|------|
| `/` | `HomePage.vue` | 公开 | 首页 |
| `/user/login` | `UserLoginPage.vue` | 公开 | 登录 |
| `/user/register` | `UserRegisterPage.vue` | 公开 | 注册 |
| `/admin/userManage` | `UserManagePage.vue` | admin | 用户管理 |

路由守卫在 `src/access.ts`：首次加载时调用 `loginUserStore.fetchLoginUser()` 获取登录态；`/admin` 路径校验 `userRole === 'admin'`，否则跳转登录页。

---

## API 接口（src/api/）

由 `npm run openapi2ts` 从后端 OpenAPI 文档自动生成，勿手动修改。

### appController.ts

| 函数 | 方法 | 路径 | 说明 |
|------|------|------|------|
| `addApp` | POST | `/app/add` | 创建应用 |
| `deleteApp` | POST | `/app/delete` | 删除应用 |
| `updateApp` | POST | `/app/update` | 更新应用 |
| `listMyAppByPage` | GET | `/app/my/list/page` | 我的应用分页 |
| `listFeaturedAppByPage` | GET | `/app/featured/list/page` | 精选应用分页 |
| `deployApp` | POST | `/app/deploy` | 部署应用 |
| `chatToGenCode` | GET | `/app/chat-to-gen-code` | 流式生成代码（SSE） |
| `adminDeleteApp` | POST | `/app/admin/delete` | 管理员删除 |
| `adminUpdateApp` | POST | `/app/admin/update` | 管理员更新 |
| `listAppByPage` | POST | `/app/list/page/vo` | 管理员分页查询 |

### userController.ts

| 函数 | 方法 | 路径 | 说明 |
|------|------|------|------|
| `userRegister` | POST | `/user/register` | 注册 |
| `userLogin` | POST | `/user/login` | 登录 |
| `userLogout` | POST | `/user/logout` | 退出 |
| `getLoginUser` | GET | `/user/get/login` | 获取当前用户 |
| `updateUser` | POST | `/user/update` | 更新个人信息 |
| `listUserVOByPage` | POST | `/user/list/page/vo` | 分页查询（管理员）|

---

## 状态管理（src/stores/）

### loginUser.ts

```ts
useLoginUserStore()  // Pinia store
  .loginUser         // ref<API.LoginUserVO>，默认 { userName: '未登录' }
  .fetchLoginUser()  // 从后端获取登录态
  .setLoginUser()    // 手动更新登录用户
```

---

## HTTP 请求封装（src/request.ts）

基于 Axios 封装，统一配置：
- `baseURL`：后端接口地址
- 请求/响应拦截器
- 自动携带 Cookie（`withCredentials: true`）

---

## 关键依赖与配置

| 文件 | 说明 |
|------|------|
| `vite.config.ts` | Vite 构建配置（代理、别名 `@`）|
| `openapi2ts.config.ts` | 指定 OpenAPI 文档地址和输出目录 |
| `eslint.config.ts` | ESLint 规则 |
| `tsconfig.json` | TypeScript 配置 |

---

## 数据模型（来自后端 OpenAPI 类型）

`src/api/typings.d.ts` 包含所有请求/响应类型，主要：
- `API.LoginUserVO`：当前登录用户（id、userName、userAvatar、userRole）
- `API.AppVO`：应用视图对象（id、appName、cover、codeGenType、deployKey 等）
- `API.AppAddRequest` / `API.AppUpdateRequest`：应用创建/更新请求
- `API.BaseResponseLong` / `API.BaseResponseBoolean`：统一响应包装

---

## 测试与质量

- 暂无自动化测试，建议添加 Vitest + Vue Test Utils
- ESLint（`eslint-plugin-vue` + `@vue/eslint-config-typescript`）
- Prettier（格式化）
- `vue-tsc`（TypeScript 类型检查）

---

## 目录结构

```
yu-ai-code-mother-frontend/
├── src/
│   ├── api/                    # openapi2ts 自动生成
│   │   ├── appController.ts
│   │   ├── userController.ts
│   │   ├── healthController.ts
│   │   ├── index.ts
│   │   └── typings.d.ts
│   ├── components/
│   │   ├── GlobalHeader.vue
│   │   └── GlobalFooter.vue
│   ├── layouts/
│   │   └── BasicLayout.vue
│   ├── pages/
│   │   ├── HomePage.vue
│   │   ├── admin/UserManagePage.vue
│   │   └── user/UserLoginPage.vue + UserRegisterPage.vue
│   ├── router/index.ts
│   ├── stores/loginUser.ts
│   ├── access.ts               # 全局路由守卫
│   ├── request.ts              # Axios 封装
│   ├── main.ts
│   └── App.vue
├── openapi2ts.config.ts
├── vite.config.ts
├── tsconfig.json
├── eslint.config.ts
└── package.json
```

---

## 常见问题 (FAQ)

**Q: 修改后端接口后前端类型不同步？**
A: 重新运行 `npm run openapi2ts`，会覆盖 `src/api/` 下所有自动生成文件。

**Q: 如何新增页面？**
A: 在 `src/pages/` 创建 `.vue` 文件，在 `src/router/index.ts` 添加路由，若需权限控制在 `src/access.ts` 中添加规则。

**Q: 如何调用 SSE 流式接口？**
A: `chatToGenCode` 接口返回 `text/event-stream`，需使用 `EventSource` 或 fetch + ReadableStream 处理，不能直接用 axios。
