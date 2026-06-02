import request from '@/api'
import { servicePrefix } from '@/api/config/servicePrefix'

// 用户资料接口返回结构。
export interface UserProfile {
  id: string
  username: string
  email?: string
}

// 获取当前登录用户信息。开启 cancelRepeat，避免重复触发相同查询。
export function getUserProfile() {
  return request.get<UserProfile>(`${servicePrefix.user}/profile`, {
    cancelRepeat: true,
  })
}
