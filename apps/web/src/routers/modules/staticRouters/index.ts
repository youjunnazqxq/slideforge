import type { RouteRecordRaw } from 'vue-router'

export const staticRouters: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/app',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: {
      title: '登录',
      public: true,
    },
  },
  {
    path: '/app',
    name: 'Layout',
    component: () => import('@/layout/index.vue'),
    redirect: '/app/workspace',
    meta: {
      title: '工作台',
      requiresAuth: true,
    },
    children: [
      {
        path: 'workspace',
        name: 'Workspace',
        component: () => import('@/views/workspace/index.vue'),
        meta: {
          title: '工作台',
          requiresAuth: true,
        },
      },
      {
        path: 'ppt',
        name: 'PptGenerate',
        component: () => import('@/views/ppt/index.vue'),
        meta: {
          title: 'PPT 生成',
          requiresAuth: true,
        },
      },
      {
        path: 'one-page',
        name: 'OnePageWorkspace',
        component: () => import('@/views/ppt/index.vue'),
        meta: {
          title: '一页 PPT',
          requiresAuth: true,
        },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: {
          title: '用户信息',
          requiresAuth: true,
        },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/index.vue'),
        meta: {
          title: '设置',
          requiresAuth: true,
        },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/app',
  },
]
