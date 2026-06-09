import request from '@/api'
import { servicePrefix } from '@/api/config/servicePrefix'

export interface AiSettingsResponse {
  provider: string
  baseUrl: string
  apiKeyConfigured: boolean
  apiKeyMask?: string
  model: string
  temperature?: number
  maxTokens?: number
}

export interface UpdateAiSettingsRequest {
  provider: string
  baseUrl: string
  apiKey?: string
  model: string
  temperature?: number
  maxTokens?: number
}

export interface AiConnectionTestResponse {
  ok: boolean
  message: string
}

export function getAiSettings() {
  return request.get<AiSettingsResponse>(`${servicePrefix.settings}/ai`, undefined, {
    cancelRepeat: true,
  })
}

export function updateAiSettings(data: UpdateAiSettingsRequest) {
  return request.put<AiSettingsResponse>(`${servicePrefix.settings}/ai`, data)
}

export function testAiSettings(data?: UpdateAiSettingsRequest) {
  return request.post<AiConnectionTestResponse>(`${servicePrefix.settings}/ai/test`, data, {
    cancelRepeat: true,
  })
}

export function deleteAiKey() {
  return request.delete<AiSettingsResponse>(`${servicePrefix.settings}/ai/key`)
}
