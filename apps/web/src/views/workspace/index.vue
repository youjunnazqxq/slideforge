<template>
  <section class="overview-page">
    <div class="hero-panel">
      <div>
        <p class="hero-panel__eyebrow">一页 PPT MVP</p>
        <h2>从需求对话到 Bento Grid SVG 的可控工作流</h2>
        <p>
          当前阶段优先跑通一页闭环：用户接入自己的 AI API，完成 brief、资料整理、
          页面策划稿和 SVG 预览。
        </p>
      </div>
      <el-button :icon="MagicStick" size="large" type="primary" @click="router.push('/app/one-page')">
        开始一页 PPT
      </el-button>
    </div>

    <div class="status-grid">
      <article v-for="item in statusCards" :key="item.title" class="status-card">
        <el-icon :class="item.tone"><component :is="item.icon" /></el-icon>
        <p>{{ item.label }}</p>
        <h3>{{ item.title }}</h3>
        <span>{{ item.description }}</span>
      </article>
    </div>

    <div class="content-grid">
      <section class="panel">
        <div class="panel__header">
          <div>
            <p>Workflow</p>
            <h3>一页生成流程</h3>
          </div>
          <el-tag type="success">MVP</el-tag>
        </div>
        <ol class="workflow-list">
          <li v-for="step in workflowSteps" :key="step">
            <span />
            {{ step }}
          </li>
        </ol>
      </section>

      <section class="panel">
        <div class="panel__header">
          <div>
            <p>AI Provider</p>
            <h3>用户 API 接入</h3>
          </div>
          <el-tag :type="aiSettingsStore.isConfigured ? 'success' : 'warning'">
            {{ aiSettingsStore.isConfigured ? '已配置' : '待配置' }}
          </el-tag>
        </div>
        <div class="provider-summary">
          <p>
            <strong>Provider</strong>
            <span>{{ aiSettingsStore.settings.provider }}</span>
          </p>
          <p>
            <strong>Base URL</strong>
            <span>{{ aiSettingsStore.settings.baseUrl || '未设置' }}</span>
          </p>
          <p>
            <strong>Model</strong>
            <span>{{ aiSettingsStore.settings.model || '未设置' }}</span>
          </p>
        </div>
        <el-button plain @click="router.push('/app/settings')">前往 AI 设置</el-button>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { Connection, DocumentChecked, MagicStick, Picture } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

import { useAiSettingsStore } from '@/stores'

const router = useRouter()
const aiSettingsStore = useAiSettingsStore()

const statusCards = [
  {
    label: 'Brief',
    title: '结构化需求',
    description: '从对话提取受众、场景、目标和核心结论。',
    icon: DocumentChecked,
    tone: 'blue',
  },
  {
    label: 'BYOK',
    title: '用户 API',
    description: '使用用户自己的 OpenAI-compatible 接口。',
    icon: Connection,
    tone: 'green',
  },
  {
    label: 'SVG',
    title: '16:9 预览',
    description: '生成 1280x720 Bento Grid SVG 页面。',
    icon: Picture,
    tone: 'purple',
  },
]

const workflowSteps = [
  '需求调研：AI 顾问追问目标观众、场景和核心结论。',
  '生成 brief：沉淀可编辑的结构化需求。',
  '资料整理：归纳可上屏素材，保留来源结构。',
  '页面策划：生成内容模块和 Bento Grid 布局意图。',
  'SVG 生成：输出可预览、可下载、可重试的页面。',
]
</script>

<style scoped lang="scss">
.overview-page {
  display: grid;
  gap: 20px;
}

.hero-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  min-height: 220px;
  padding: 32px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(16, 185, 129, 0.08)),
    #ffffff;

  h2 {
    max-width: 760px;
    margin: 8px 0 12px;
    color: #111827;
    font-size: 32px;
    line-height: 1.2;
  }

  p {
    max-width: 760px;
    margin: 0;
    color: #4b5563;
    line-height: 1.8;
  }
}

.hero-panel__eyebrow,
.panel__header p {
  margin: 0;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

.status-grid,
.content-grid {
  display: grid;
  gap: 16px;
}

.status-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.status-card,
.panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.status-card {
  padding: 20px;

  .el-icon {
    width: 36px;
    height: 36px;
    border-radius: 8px;
    font-size: 20px;
  }

  .blue {
    background: #eff6ff;
    color: #2563eb;
  }

  .green {
    background: #ecfdf5;
    color: #059669;
  }

  .purple {
    background: #f5f3ff;
    color: #7c3aed;
  }

  p {
    margin: 16px 0 4px;
    color: #6b7280;
    font-size: 12px;
    font-weight: 700;
  }

  h3 {
    margin: 0 0 8px;
    font-size: 18px;
  }

  span {
    color: #6b7280;
    font-size: 14px;
    line-height: 1.6;
  }
}

.content-grid {
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
}

.panel {
  padding: 22px;
}

.panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;

  h3 {
    margin: 5px 0 0;
    font-size: 18px;
  }
}

.workflow-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;

  li {
    display: flex;
    gap: 10px;
    color: #374151;
    line-height: 1.7;
  }

  span {
    width: 8px;
    height: 8px;
    flex: 0 0 auto;
    margin-top: 9px;
    border-radius: 999px;
    background: #2563eb;
  }
}

.provider-summary {
  display: grid;
  gap: 10px;
  margin-bottom: 18px;

  p {
    display: grid;
    gap: 4px;
    margin: 0;
  }

  strong {
    color: #6b7280;
    font-size: 12px;
  }

  span {
    overflow: hidden;
    color: #111827;
    font-size: 14px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

@media (max-width: 1100px) {
  .status-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .hero-panel {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
