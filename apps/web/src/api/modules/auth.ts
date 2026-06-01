import request from '@/api'
import { servicePrefix } from '@/api/config/servicePrefix'

import type { ApiResponse } from '../interface'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  username: string
  token: string
}

export function login(data: LoginParams) {
  return request.post<LoginResult, LoginParams>(`${servicePrefix.auth}/login`, data, {
    cancelRepeat: true,
    skipAuth: true,
  })
}

export function getLoginRawResponse(data: LoginParams) {
  return request.post<ApiResponse<LoginResult>, LoginParams>(`${servicePrefix.auth}/login`, data, {
    cancelRepeat: true,
    returnRawResponse: true,
    skipAuth: true,
  })
}
