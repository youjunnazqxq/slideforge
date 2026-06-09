<template>
  <div class="app-shell" :class="{ 'app-shell--collapsed': sidebarCollapsed }">
    <aside class="app-shell__sidebar">
      <div class="sidebar-head">
      <RouterLink class="brand" to="/app/workspace">
        <span class="brand__mark">S</span>
        <span>
          <strong>SlideForge</strong>
          <small>AI PPT 工作台</small>
        </span>
      </RouterLink>

      <button
        class="sidebar-toggle"
        :aria-label="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
        type="button"
        @click="toggleSidebar"
      >
        <el-icon>
          <Expand v-if="sidebarCollapsed" />
          <Fold v-else />
        </el-icon>
      </button>
      </div>

      <nav class="nav-list">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          class="nav-list__item"
          :to="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <section class="sidebar-card">
        <p class="sidebar-card__eyebrow">当前阶段</p>
        <h2>一页闭环 MVP</h2>
        <p>先跑通需求、策划稿和 SVG 预览，再扩展多页导出。</p>
      </section>
    </aside>

    <div class="app-shell__body">
      <header class="topbar">
        <div>
          <p class="topbar__breadcrumb">SlideForge / {{ currentTitle }}</p>
          <h1>{{ currentTitle }}</h1>
        </div>

        <div class="topbar__actions">
          <el-button :icon="MagicStick" plain @click="goOnePage">一页 PPT</el-button>
          <el-dropdown trigger="click">
            <button class="user-chip" type="button">
              <span>{{ userInitial }}</span>
              <strong>{{ userStore.username || 'Local User' }}</strong>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/app/profile')">用户信息</el-dropdown-item>
                <el-dropdown-item @click="router.push('/app/settings')">AI 设置</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="app-shell__content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  DataAnalysis,
  Expand,
  Files,
  Fold,
  MagicStick,
  Setting,
  User,
  View,
} from '@element-plus/icons-vue'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useStore } from '@/stores'

const route = useRoute()
const router = useRouter()
const userStore = useStore()
const sidebarCollapsed = ref(localStorage.getItem('slideforge:sidebar-collapsed') === 'true')

const navItems = [
  {
    label: '工作台',
    path: '/app/workspace',
    icon: DataAnalysis,
  },
  {
    label: '一页 PPT',
    path: '/app/one-page',
    icon: MagicStick,
  },
  {
    label: '完整大纲',
    path: '/app/deck-outline',
    icon: Files,
  },
  {
    label: '预览项目',
    path: '/app/ppt',
    icon: View,
  },
  {
    label: '用户信息',
    path: '/app/profile',
    icon: User,
  },
  {
    label: 'AI 设置',
    path: '/app/settings',
    icon: Setting,
  },
]

const currentTitle = computed(() => {
  return typeof route.meta.title === 'string' ? route.meta.title : '工作台'
})

const userInitial = computed(() => {
  return (userStore.username || 'S').slice(0, 1).toUpperCase()
})

function goOnePage() {
  router.push('/app/one-page')
}

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('slideforge:sidebar-collapsed', String(sidebarCollapsed.value))
}

function handleLogout() {
  userStore.clearUser()
  router.replace('/login')
}
</script>

<style scoped lang="scss">
.app-shell {
  display: grid;
  min-height: 100vh;
  grid-template-columns: 248px minmax(0, 1fr);
  background: #f5f6f8;
  transition: grid-template-columns 0.18s ease;
}

.app-shell--collapsed {
  grid-template-columns: 76px minmax(0, 1fr);
}

.app-shell__sidebar {
  position: sticky;
  top: 0;
  display: flex;
  height: 100vh;
  flex-direction: column;
  gap: 22px;
  border-right: 1px solid #e5e7eb;
  background: #ffffff;
  padding: 20px 16px;
}

.sidebar-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 32px;
  align-items: start;
  gap: 8px;
  border-bottom: 1px solid #eef0f3;
  padding-bottom: 14px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  padding: 4px 0 0 6px;
  text-decoration: none;

  strong,
  small {
    display: block;
  }

  strong {
    color: #111827;
    font-size: 17px;
  }

  small {
    margin-top: 2px;
    color: #6b7280;
    font-size: 12px;
  }
}

.sidebar-toggle {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  color: #4b5563;
  cursor: pointer;

  &:hover {
    border-color: #bfdbfe;
    color: #1d4ed8;
  }
}

.brand__mark {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 8px;
  background: #2563eb;
  color: #ffffff;
  font-weight: 800;
}

.nav-list {
  display: grid;
  gap: 6px;
}

.nav-list__item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 40px;
  padding: 0 12px;
  border-radius: 8px;
  color: #4b5563;
  font-size: 14px;
  font-weight: 600;

  &:hover {
    background: #f3f4f6;
    color: #111827;
  }

  &.router-link-active {
    background: #eff6ff;
    color: #1d4ed8;
  }
}

.sidebar-card {
  margin-top: auto;
  padding: 14px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #eff6ff;

  h2 {
    margin: 6px 0;
    color: #1e3a8a;
    font-size: 15px;
  }

  p {
    margin: 0;
    color: #475569;
    font-size: 12px;
    line-height: 1.6;
  }
}

.sidebar-card__eyebrow {
  color: #2563eb !important;
  font-weight: 700;
}

.app-shell__body {
  min-width: 0;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  border-bottom: 1px solid #e5e7eb;
  background: rgba(255, 255, 255, 0.86);
  padding: 0 28px;
  backdrop-filter: blur(18px);

  h1 {
    margin: 3px 0 0;
    color: #111827;
    font-size: 20px;
  }
}

.topbar__breadcrumb {
  margin: 0;
  color: #6b7280;
  font-size: 12px;
}

.topbar__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 9px;
  height: 36px;
  padding: 0 10px 0 4px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #ffffff;
  color: #111827;
  cursor: pointer;

  span {
    display: grid;
    width: 28px;
    height: 28px;
    place-items: center;
    border-radius: 999px;
    background: #111827;
    color: #ffffff;
    font-size: 12px;
    font-weight: 700;
  }

  strong {
    max-width: 140px;
    overflow: hidden;
    font-size: 13px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.app-shell__content {
  padding: 24px 28px 32px;
}

.app-shell--collapsed {
  .app-shell__sidebar {
    align-items: center;
    padding: 20px 10px;
  }

  .sidebar-head {
    grid-template-columns: 1fr;
    justify-items: center;
    width: 100%;
    padding-bottom: 12px;
  }

  .brand {
    padding: 0;
  }

  .brand span:last-child,
  .nav-list__item span,
  .sidebar-card {
    display: none;
  }

  .nav-list {
    width: 100%;
  }

  .nav-list__item {
    justify-content: center;
    padding: 0;
  }
}

@media (max-width: 980px) {
  .app-shell {
    grid-template-columns: 76px minmax(0, 1fr);
  }

  .brand span:last-child,
  .nav-list__item span,
  .sidebar-card {
    display: none;
  }

  .app-shell__sidebar {
    padding: 16px 10px;
  }

  .nav-list__item {
    justify-content: center;
    padding: 0;
  }
}
</style>
