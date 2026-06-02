import request from '@/api'
import { servicePrefix } from '@/api/config/servicePrefix'

import type { ApiResponse } from '../interface'

// 登录接口入参，后续可根据真实后端字段调整。
export interface LoginParams {
  username: string
  password: string
}

// 登录成功后的核心数据，token 会写入 Pinia 用户仓库。
export interface LoginResult {
  username: string
  token: string
}

// 默认返回解包后的 data，即 LoginResult。
export function login(data: LoginParams) {
  return request.post<LoginResult, LoginParams>(`${servicePrefix.auth}/login`, data, {
    cancelRepeat: true,
    skipAuth: true,
  })
}

// 示例：需要完整后端响应结构时开启 returnRawResponse。
export function getLoginRawResponse(data: LoginParams) {
  return request.post<ApiResponse<LoginResult>, LoginParams>(`${servicePrefix.auth}/login`, data, {
    cancelRepeat: true,
    returnRawResponse: true,
    skipAuth: true,
  })
}
