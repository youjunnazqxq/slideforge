import request from '@/api'
import { servicePrefix } from '@/api/config/servicePrefix'

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

// 默认返回后端统一响应结构 ResultData<LoginResult>。
export function login(data: LoginParams) {
  return request.post<LoginResult>(servicePrefix.auth, data, {
    cancelRepeat: true,
    skipAuth: true,
  })
}
