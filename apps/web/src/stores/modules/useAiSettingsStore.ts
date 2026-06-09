import { defineStore } from 'pinia'
import { computed, reactive, ref } from 'vue'

import {
  deleteAiKey,
  getAiSettings,
  testAiSettings,
  updateAiSettings,
  type AiSettingsResponse,
  type UpdateAiSettingsRequest,
} from '@/api/modules/settings'

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

export type SaveAiSettingsPayload = UpdateAiSettingsRequest

export const useAiSettingsStore = defineStore(
  'aiSettings',
  () => {
    const loading = ref(false)
    const saving = ref(false)
    const testing = ref(false)
    const deletingKey = ref(false)
    const errorMessage = ref('')

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

    async function loadSettings() {
      loading.value = true
      errorMessage.value = ''

      try {
        const response = await getAiSettings()
        applySettings(response.data)
      } catch (error) {
        errorMessage.value = getErrorMessage(error, '读取 AI 配置失败')
        throw error
      } finally {
        loading.value = false
      }
    }

    async function saveSettings(payload: SaveAiSettingsPayload) {
      saving.value = true
      errorMessage.value = ''

      try {
        const response = await updateAiSettings(normalizePayload(payload))
        applySettings(response.data)
        setTestResult('idle', '配置已保存。API Key 由后端加密保存，不会明文回显。')
      } catch (error) {
        errorMessage.value = getErrorMessage(error, '保存 AI 配置失败')
        throw error
      } finally {
        saving.value = false
      }
    }

    async function testConnectionWith(payload?: SaveAiSettingsPayload) {
      testing.value = true
      errorMessage.value = ''

      try {
        const response = await testAiSettings(payload ? normalizePayload(payload) : undefined)
        setTestResult(response.data.ok ? 'success' : 'failed', response.data.message)
      } catch (error) {
        const message = getErrorMessage(error, '测试连接失败')
        errorMessage.value = message
        setTestResult('failed', message)
        throw error
      } finally {
        testing.value = false
      }
    }

    async function clearApiKey() {
      deletingKey.value = true
      errorMessage.value = ''

      try {
        const response = await deleteAiKey()
        applySettings(response.data)
        setTestResult('idle', 'API Key 已删除。')
      } catch (error) {
        errorMessage.value = getErrorMessage(error, '删除 API Key 失败')
        throw error
      } finally {
        deletingKey.value = false
      }
    }

    function setTestResult(status: AiSettingsState['lastTestStatus'], message: string) {
      settings.lastTestStatus = status
      settings.lastTestMessage = message
    }

    function applySettings(response: AiSettingsResponse) {
      settings.provider = response.provider
      settings.baseUrl = response.baseUrl
      settings.model = response.model
      settings.apiKeyConfigured = response.apiKeyConfigured
      settings.apiKeyMask = response.apiKeyMask || ''
      settings.temperature = response.temperature ?? 0.7
      settings.maxTokens = response.maxTokens ?? 4096
    }

    function normalizePayload(payload: SaveAiSettingsPayload): UpdateAiSettingsRequest {
      const normalized: UpdateAiSettingsRequest = {
        provider: payload.provider,
        baseUrl: payload.baseUrl.trim(),
        model: payload.model.trim(),
        temperature: payload.temperature,
        maxTokens: payload.maxTokens,
      }

      if (payload.apiKey?.trim()) {
        normalized.apiKey = payload.apiKey.trim()
      }

      return normalized
    }

    function getErrorMessage(error: unknown, fallback: string) {
      if (error && typeof error === 'object' && 'message' in error) {
        const message = (error as { message?: unknown }).message

        if (typeof message === 'string' && message) {
          return message
        }
      }

      return fallback
    }

    return {
      deletingKey,
      errorMessage,
      loading,
      saving,
      settings,
      testing,
      isConfigured,
      clearApiKey,
      loadSettings,
      saveSettings,
      setTestResult,
      testConnectionWith,
    }
  },
  {
    persist: {
      key: 'slideforge:ai-settings',
      paths: ['settings'],
    },
  },
)
