# Axios 封装设计

本文档记录 SlideForge 前端 Axios 请求层的目录结构和封装方案。当前阶段只修改设计文档，不直接编写实现代码。

## 1. 设计目标

Axios 请求层用于统一管理前端接口请求，避免在页面组件中重复处理 baseURL、请求头、token、错误状态、重复请求取消等逻辑。

第一阶段目标：

```text
1. 使用 TypeScript class 封装 Axios。
2. index.ts 作为 Axios 类封装入口。
3. config 目录存放基础请求配置和后端模块前缀。
4. helper 目录存放取消重复 Axios 请求的方法。
5. interface 目录存放请求、响应、配置等类型。
6. modules 目录存放业务模块 API。
7. 单独提供 HTTP 状态检测文件。
8. 使用 .env、.env.development、.env.production 管理环境变量。
```

## 2. 目录结构

Axios 相关代码统一放在 `src/api` 下，目录结构如下：

```text
apps/web/src/
  api/
    config/
      index.ts
      servicePrefix.ts
    helper/
      cancelRepeatRequest.ts
      checkStatus.ts
    interface/
      index.ts
      request.ts
      response.ts
    modules/
      user.ts
      auth.ts
    index.ts
```

说明：

- `api/index.ts`：核心文件，主要编写封装好的 Axios class。
- `api/config/`：存放请求基础配置和后端模块前缀。
- `api/helper/`：存放请求辅助方法，例如取消重复请求、检测 HTTP 状态。
- `api/interface/`：存放 TypeScript interface。
- `api/modules/`：存放具体业务接口模块，例如用户、登录、项目、PPT 生成。

## 3. 环境变量设计

项目需要建立三个环境变量文件：

```text
apps/web/
  .env
  .env.development
  .env.production
```

### 3.1 .env

`.env` 存放所有环境通用配置。

示例：

```text
VITE_APP_TITLE=SlideForge
```

### 3.2 .env.development

`.env.development` 存放开发环境配置。开发阶段后端默认主接口地址放在这里。

示例：

```text
VITE_API_BASE_URL=http://localhost:3000
```

### 3.3 .env.production

`.env.production` 存放生产环境配置。

示例：

```text
VITE_API_BASE_URL=https://api.slideforge.example.com
```

Axios 的默认请求地址应从：

```ts
import.meta.env.VITE_API_BASE_URL
```

读取。

## 4. config 目录设计

`config` 目录用于存放请求基础配置和后端模块前缀。

建议文件：

```text
api/config/
  index.ts
  servicePrefix.ts
```

### 4.1 基础 config 对象

`api/config/index.ts` 用于导出 Axios 基础配置对象。

设计方向：

```ts
const axiosConfig = {
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  withCredentials: false,
}
```

说明：

- `baseURL`：默认请求地址，从环境变量读取。
- `timeout`：请求超时时间，建议默认 `10000ms`。
- `withCredentials`：是否携带 cookie，第一阶段默认 `false`。

后续 `api/index.ts` 中封装 Axios class 时，应使用这个基础 config 对象创建 Axios 实例。

### 4.2 后端模块前缀

`api/config/servicePrefix.ts` 用于统一管理后端模块前缀。

示例：

```ts
export const servicePrefix = {
  auth: '/auth',
  user: '/user',
  project: '/project',
  ppt: '/ppt',
}
```

业务模块 API 不建议手写散落的前缀字符串，应从该文件读取。

例如：

```ts
const userApi = `${servicePrefix.user}/profile`
```

## 5. index.ts 类封装设计

`api/index.ts` 是 Axios 封装核心文件，主要编写请求类。

设计方向：

```ts
class Request {
  private instance

  constructor(config) {
    // 合并基础 config
    // 创建 axios 实例
    // 注册请求拦截器
    // 注册响应拦截器
  }

  request<T>(config): Promise<T> {}
  get<T>(url, config?): Promise<T> {}
  post<T>(url, data?, config?): Promise<T> {}
  put<T>(url, data?, config?): Promise<T> {}
  patch<T>(url, data?, config?): Promise<T> {}
  delete<T>(url, config?): Promise<T> {}
}
```

最终应导出一个默认请求实例：

```ts
const request = new Request(axiosConfig)

export default request
```

页面和业务模块不直接使用原生 `axios`，只使用封装后的 `request`。

## 6. 请求拦截器

请求拦截器在请求发送前执行。

需要处理：

```text
1. 注入 token。
2. 设置请求头。
3. 处理重复请求取消。
4. 统一处理请求配置。
```

### 6.1 token 注入

后续接入登录后，从 Pinia 用户仓库中读取 token。

规则：

```text
如果接口配置 skipAuth 为 true，不注入 token。
如果存在 token，则写入 Authorization。
```

推荐格式：

```text
Authorization: Bearer <token>
```

### 6.2 重复请求处理

请求发出前调用 `helper/cancelRepeatRequest.ts` 中的方法。

建议根据以下信息生成请求 key：

```text
method + url + params + data
```

如果短时间内存在完全相同的请求，则取消旧请求或取消新请求。第一阶段建议：

```text
取消旧请求，保留最新请求。
```

这样适合搜索、筛选、快速重复点击等场景。

## 7. 响应拦截器

响应拦截器在接口返回后执行。

需要处理：

```text
1. 移除当前请求的 pending 记录。
2. 检查 HTTP 状态。
3. 解析后端业务响应。
4. 处理业务错误码。
5. 统一返回 data。
```

### 7.1 HTTP 状态检测

HTTP 状态检测逻辑放在：

```text
api/helper/checkStatus.ts
```

该文件负责根据 HTTP status 返回对应提示。

建议覆盖：

```text
400：请求参数错误。
401：登录状态失效。
403：没有权限访问。
404：接口不存在。
408：请求超时。
500：服务器错误。
502：网关错误。
503：服务不可用。
504：网关超时。
```

`api/index.ts` 的响应错误拦截器中调用 `checkStatus`。

### 7.2 业务响应解析

后端建议统一响应格式：

```ts
{
  code: number
  message: string
  data: T
}
```

请求层成功时，默认只返回 `data`。

如果业务需要完整响应，可以后续增加配置：

```text
returnRawResponse: true
```

## 8. helper 目录设计

`helper` 存放请求辅助逻辑。

建议文件：

```text
api/helper/
  cancelRepeatRequest.ts
  checkStatus.ts
```

### 8.1 cancelRepeatRequest.ts

该文件用于取消重复 Axios 请求。

设计内容：

```text
1. 创建 pending 请求 Map。
2. 生成请求唯一 key。
3. 添加 pending 请求。
4. 移除 pending 请求。
5. 取消重复请求。
```

设计方向：

```ts
const pendingMap = new Map<string, AbortController>()
```

推荐使用 Axios 新版支持的 `AbortController`，不优先使用旧版 `CancelToken`。

请求流程：

```text
请求发出前：
  生成 requestKey
  如果 pendingMap 中存在相同 key，则 abort 旧请求
  创建新的 AbortController
  写入 config.signal
  存入 pendingMap

响应成功或失败后：
  根据 requestKey 从 pendingMap 中移除
```

### 8.2 checkStatus.ts

该文件专门检测 HTTP 状态码，并返回统一错误信息。

职责：

```text
1. 接收 HTTP status。
2. 根据 status 匹配提示文案。
3. 返回格式化后的错误信息。
```

不要把 HTTP 状态判断散落在页面组件或业务 API 文件中。

## 9. interface 目录设计

`interface` 目录用于存放请求层类型。

建议文件：

```text
api/interface/
  index.ts
  request.ts
  response.ts
```

### 9.1 request.ts

请求配置类型：

```ts
interface RequestConfig {
  skipAuth?: boolean
  cancelRepeat?: boolean
  returnRawResponse?: boolean
}
```

字段说明：

- `skipAuth`：是否跳过 token。
- `cancelRepeat`：是否开启重复请求取消。
- `returnRawResponse`：是否返回完整响应。

后续实现时，该类型应基于 Axios 原始请求配置扩展。

### 9.2 response.ts

响应类型：

```ts
interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}
```

错误类型：

```ts
interface ApiError {
  code?: number | string
  message: string
  status?: number
  url?: string
}
```

### 9.3 index.ts

统一导出 interface：

```ts
export * from './request'
export * from './response'
```

## 10. modules 目录设计

`modules` 目录用于放具体后端模块接口。

示例：

```text
api/modules/
  auth.ts
  user.ts
  project.ts
  ppt.ts
```

模块文件只负责声明业务接口，不创建 Axios 实例。

示例方向：

```ts
import request from '@/api'
import { servicePrefix } from '@/api/config/servicePrefix'

export function getUserProfile() {
  return request.get(`${servicePrefix.user}/profile`)
}
```

## 11. 第一阶段不做的事情

第一阶段只完成请求层基础能力，不做过度封装。

暂不处理：

```text
1. 复杂重试机制。
2. 请求队列。
3. 全局 loading 计数器。
4. 文件分片上传。
5. WebSocket。
6. AI API 真实调用。
```

## 12. 验收标准

后续实现 Axios 封装后，需要满足：

```text
1. src/api 目录结构符合本文档。
2. api/index.ts 使用 class 封装 Axios。
3. class 内部合并基础 config 对象。
4. 请求默认 baseURL 从 VITE_API_BASE_URL 读取。
5. 存在 .env、.env.development、.env.production。
6. config 目录统一管理后端模块前缀。
7. helper 中存在取消重复请求方法。
8. helper 中存在 HTTP 状态检测方法。
9. interface 中存在请求和响应类型。
10. modules 中只写业务接口，不直接创建 Axios 实例。
11. 构建命令可以正常通过。
```
