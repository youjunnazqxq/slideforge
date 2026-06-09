import { defineStore } from 'pinia'
import { computed, reactive } from 'vue'

export interface AiSettingsState {
  provider: string
  baseUrl: string
  model: string
  apiKeyMask: string
  apiKeyConfigured: boolean
  temperature: number
  maxTokens: number
  lastTestStatus: 'idle' | 'success' | 'failed'
  lastTestMessage: string
}

export interface SaveAiSettingsPayload {
  provider: string
  baseUrl: string
  model: string
  apiKey?: string
  temperature: number
  maxTokens: number
}

function maskApiKey(apiKey: string) {
  const trimmed = apiKey.trim()

  if (!trimmed) {
    return ''
  }

  const prefix = trimmed.slice(0, 3)
  const suffix = trimmed.slice(-4)

  return `${prefix}****${suffix}`
}

export const useAiSettingsStore = defineStore(
  'aiSettings',
  () => {
    const settings = reactive<AiSettingsState>({
      provider: 'openai-compatible',
      baseUrl: 'https://api.openai.com/v1',
      model: '',
      apiKeyMask: '',
      apiKeyConfigured: false,
      temperature: 0.7,
      maxTokens: 4096,
      lastTestStatus: 'idle',
      lastTestMessage: '',
    })

    const isConfigured = computed(() => {
      return Boolean(settings.baseUrl && settings.model && settings.apiKeyConfigured)
    })

    function saveSettings(payload: SaveAiSettingsPayload) {
      settings.provider = payload.provider
      settings.baseUrl = payload.baseUrl.trim()
      settings.model = payload.model.trim()
      settings.temperature = payload.temperature
      settings.maxTokens = payload.maxTokens

      if (payload.apiKey?.trim()) {
        settings.apiKeyConfigured = true
        settings.apiKeyMask = maskApiKey(payload.apiKey)
      }
    }

    function setTestResult(status: AiSettingsState['lastTestStatus'], message: string) {
      settings.lastTestStatus = status
      settings.lastTestMessage = message
    }

    function clearApiKey() {
      settings.apiKeyConfigured = false
      settings.apiKeyMask = ''
      settings.lastTestStatus = 'idle'
      settings.lastTestMessage = ''
    }

    return {
      settings,
      isConfigured,
      saveSettings,
      setTestResult,
      clearApiKey,
    }
  },
  {
    persist: {
      key: 'slideforge:ai-settings',
      paths: ['settings'],
    },
  },
)
