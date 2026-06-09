<template>
  <section class="settings-page">
    <header class="settings-header">
      <div>
        <p>BYOK AI Provider</p>
        <h2>接入你的 AI API</h2>
        <span>SlideForge 通过后端代理调用模型，前端只展示脱敏状态，不保存明文 Key。</span>
      </div>
      <el-tag :type="aiSettingsStore.isConfigured ? 'success' : 'warning'" size="large">
        {{ aiSettingsStore.isConfigured ? '已配置' : '未完成配置' }}
      </el-tag>
    </header>

    <el-form v-loading="aiSettingsStore.loading" class="settings-form" label-position="top">
      <div class="settings-form__grid">
        <el-form-item label="Provider">
          <el-select v-model="form.provider">
            <el-option label="OpenAI Compatible" value="openai-compatible" />
          </el-select>
        </el-form-item>

        <el-form-item label="Base URL">
          <el-input v-model="form.baseUrl" placeholder="https://api.openai.com/v1" />
        </el-form-item>

        <el-form-item label="API Key">
          <el-input
            v-model="form.apiKey"
            placeholder="保存后不明文回显"
            show-password
            type="password"
          />
          <p v-if="aiSettingsStore.settings.apiKeyConfigured" class="field-tip">
            当前 Key：{{ aiSettingsStore.settings.apiKeyMask }}
          </p>
        </el-form-item>

        <el-form-item label="默认模型">
          <el-input v-model="form.model" placeholder="例如 gpt-4.1 / deepseek-chat" />
        </el-form-item>

        <el-form-item label="Temperature">
          <el-input-number v-model="form.temperature" :max="1.5" :min="0" :step="0.1" />
        </el-form-item>

        <el-form-item label="Max Tokens">
          <el-input-number v-model="form.maxTokens" :max="12000" :min="1000" :step="500" />
        </el-form-item>
      </div>

      <div class="settings-actions">
        <el-button :loading="aiSettingsStore.saving" type="primary" @click="save">
          保存配置
        </el-button>
        <el-button :loading="aiSettingsStore.testing" plain @click="testConnection">
          测试连接
        </el-button>
        <el-button
          v-if="aiSettingsStore.settings.apiKeyConfigured"
          :loading="aiSettingsStore.deletingKey"
          text
          type="danger"
          @click="clearKey"
        >
          删除 Key
        </el-button>
      </div>

      <el-alert
        v-if="aiSettingsStore.settings.lastTestMessage"
        :closable="false"
        :title="aiSettingsStore.settings.lastTestMessage"
        :type="aiSettingsStore.settings.lastTestStatus === 'success' ? 'success' : 'warning'"
      />

      <el-alert
        v-if="aiSettingsStore.errorMessage"
        :closable="false"
        :title="aiSettingsStore.errorMessage"
        type="error"
      />
    </el-form>

    <section class="security-panel">
      <h3>安全规则</h3>
      <ul>
        <li>API Key 输入框使用 password 类型，保存后只显示后端返回的脱敏文本。</li>
        <li>后端使用加密字段保存 Key，不在接口、日志或前端持久化中返回明文。</li>
        <li>AI 生成流程统一通过后端代理调用，前端不直接请求模型供应商。</li>
      </ul>
    </section>
  </section>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive } from 'vue'

import { useAiSettingsStore } from '@/stores'

const aiSettingsStore = useAiSettingsStore()

const form = reactive({
  provider: aiSettingsStore.settings.provider,
  baseUrl: aiSettingsStore.settings.baseUrl,
  apiKey: '',
  model: aiSettingsStore.settings.model,
  temperature: aiSettingsStore.settings.temperature,
  maxTokens: aiSettingsStore.settings.maxTokens,
})

onMounted(async () => {
  try {
    await aiSettingsStore.loadSettings()
    syncFormFromStore()
  } catch {
    ElMessage.error(aiSettingsStore.errorMessage || '读取 AI 配置失败')
  }
})

async function save() {
  try {
    await aiSettingsStore.saveSettings(form)
    form.apiKey = ''
    syncFormFromStore()
    ElMessage.success('AI 配置已保存')
  } catch {
    ElMessage.error(aiSettingsStore.errorMessage || '保存 AI 配置失败')
  }
}

async function testConnection() {
  try {
    await aiSettingsStore.testConnectionWith(form)
  } catch {
    ElMessage.error(aiSettingsStore.errorMessage || '测试连接失败')
  }
}

async function clearKey() {
  try {
    await aiSettingsStore.clearApiKey()
    form.apiKey = ''
    ElMessage.success('API Key 已删除')
  } catch {
    ElMessage.error(aiSettingsStore.errorMessage || '删除 API Key 失败')
  }
}

function syncFormFromStore() {
  form.provider = aiSettingsStore.settings.provider
  form.baseUrl = aiSettingsStore.settings.baseUrl
  form.model = aiSettingsStore.settings.model
  form.temperature = aiSettingsStore.settings.temperature
  form.maxTokens = aiSettingsStore.settings.maxTokens
}
</script>

<style scoped lang="scss">
.settings-page {
  display: grid;
  max-width: 1040px;
  gap: 18px;
}

.settings-header,
.settings-form,
.security-panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.settings-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 24px;

  p,
  h2,
  span {
    margin: 0;
  }

  p {
    color: #2563eb;
    font-size: 12px;
    font-weight: 800;
  }

  h2 {
    margin: 6px 0 8px;
    font-size: 24px;
  }

  span {
    color: #6b7280;
  }
}

.settings-form {
  display: grid;
  gap: 18px;
  padding: 24px;
}

.settings-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 18px;
}

.field-tip {
  margin: 8px 0 0;
  color: #6b7280;
  font-size: 12px;
}

.settings-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.security-panel {
  padding: 22px;

  h3 {
    margin: 0 0 12px;
    font-size: 18px;
  }

  ul {
    display: grid;
    gap: 8px;
    margin: 0;
    padding-left: 18px;
    color: #4b5563;
    line-height: 1.7;
  }
}

@media (max-width: 820px) {
  .settings-form__grid {
    grid-template-columns: 1fr;
  }
}
</style>
