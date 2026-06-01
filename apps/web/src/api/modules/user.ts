import request from '@/api'
import { servicePrefix } from '@/api/config/servicePrefix'

export interface UserProfile {
  id: string
  username: string
  email?: string
}

export function getUserProfile() {
  return request.get<UserProfile>(`${servicePrefix.user}/profile`, {
    cancelRepeat: true,
  })
}
