import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'

import { setupRouterGuard } from './guard'
import { staticRouters } from './modules/staticRouters'

const routerMode = import.meta.env.VITE_ROUTER_MODE
const createHistory = routerMode === 'hash' ? createWebHashHistory : createWebHistory

const router = createRouter({
  history: createHistory(import.meta.env.BASE_URL),
  routes: staticRouters,
})

setupRouterGuard(router)

export default router
