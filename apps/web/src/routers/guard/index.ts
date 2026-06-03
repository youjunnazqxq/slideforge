import type { Router } from 'vue-router'

import NProgress from '@/config/nprogress'
import { useStore } from '@/stores'

const appTitle = import.meta.env.VITE_APP_TITLE || 'SlideForge'

function setDocumentTitle(title?: unknown) {
  document.title = typeof title === 'string' && title ? title : appTitle
}

export function setupRouterGuard(router: Router) {
  router.beforeEach((to) => {
    NProgress.start()
    setDocumentTitle(to.meta.title)

    const userStore = useStore()
    const hasToken = Boolean(userStore.token)

    if (to.path === '/login' && hasToken) {
      return '/app'
    }

    if (to.meta.requiresAuth && !hasToken) {
      return {
        path: '/login',
        query: {
          redirect: to.fullPath,
        },
      }
    }

    return true
  })

  router.afterEach(() => {
    NProgress.done()
  })

  router.onError(() => {
    NProgress.done()
  })
}
