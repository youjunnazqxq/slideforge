<template>
  <section class="profile-page">
    <section class="profile-card profile-card--hero">
      <div class="avatar">{{ initial }}</div>
      <div>
        <p>Local User</p>
        <h2>{{ userStore.username || 'SlideForge User' }}</h2>
        <span>当前账号用于本地 MVP 联调，后续接入数据库、JWT 和项目历史。</span>
      </div>
    </section>

    <section class="profile-card">
      <div class="profile-card__header">
        <h3>基础信息</h3>
        <el-tag>第一阶段</el-tag>
      </div>
      <div class="info-grid">
        <p>
          <strong>用户名</strong>
          <span>{{ userStore.username || '未登录' }}</span>
        </p>
        <p>
          <strong>邮箱</strong>
          <span>local-user@slideforge.app</span>
        </p>
        <p>
          <strong>账户类型</strong>
          <span>Local User</span>
        </p>
        <p>
          <strong>Token 状态</strong>
          <span>{{ userStore.token ? '已保存' : '未保存' }}</span>
        </p>
      </div>
    </section>

    <section class="profile-card">
      <div class="profile-card__header">
        <h3>使用偏好</h3>
        <el-button plain @click="router.push('/app/settings')">AI 设置</el-button>
      </div>
      <div class="preference-list">
        <p>
          <strong>默认语言</strong>
          <span>中文</span>
        </p>
        <p>
          <strong>PPT 风格</strong>
          <span>专业、克制、信息密度适中</span>
        </p>
        <p>
          <strong>画布比例</strong>
          <span>16:9 / 1280 x 720</span>
        </p>
        <p>
          <strong>AI 服务</strong>
          <span>{{ aiSettingsStore.isConfigured ? '已配置' : '未配置' }}</span>
        </p>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { useAiSettingsStore, useStore } from '@/stores'

const router = useRouter()
const userStore = useStore()
const aiSettingsStore = useAiSettingsStore()

const initial = computed(() => (userStore.username || 'S').slice(0, 1).toUpperCase())
</script>

<style scoped lang="scss">
.profile-page {
  display: grid;
  max-width: 980px;
  gap: 16px;
}

.profile-card {
  padding: 22px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.profile-card--hero {
  display: flex;
  align-items: center;
  gap: 18px;

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
    margin: 5px 0 8px;
    font-size: 26px;
  }

  span {
    color: #6b7280;
  }
}

.avatar {
  display: grid;
  width: 70px;
  height: 70px;
  place-items: center;
  border-radius: 8px;
  background: #111827;
  color: #ffffff;
  font-size: 26px;
  font-weight: 800;
}

.profile-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;

  h3 {
    margin: 0;
    font-size: 18px;
  }
}

.info-grid,
.preference-list {
  display: grid;
  gap: 12px;
}

.info-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.info-grid p,
.preference-list p {
  display: grid;
  gap: 6px;
  margin: 0;
  padding: 14px;
  border-radius: 8px;
  background: #f9fafb;

  strong {
    color: #6b7280;
    font-size: 12px;
  }

  span {
    color: #111827;
  }
}

@media (max-width: 720px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
