import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore, HOME_BY_ROLE } from '@/stores/user'

/**
 * 路由守卫（体验层权限控制）
 * - 真实权限以后端 401/403 为准
 * - 未登录访问受保护区 → 跳 /login
 * - 角色越权访问 → 跳回本角色首页
 */
export function setupGuards(router) {
  router.beforeEach((to) => {
    const store = useUserStore()

    // 公开页（登录/注册）直接放行
    if (to.meta.public) return true

    // 未登录 → 登录页
    if (!store.isLoggedIn) {
      return { path: '/login', query: to.fullPath !== '/' ? { redirect: to.fullPath } : {} }
    }

    // 已登录访问登录页 → 回到本角色首页
    if (to.path === '/login') {
      return HOME_BY_ROLE[store.role] || '/patient/home'
    }

    // 角色分区校验
    if (to.meta.role && to.meta.role !== store.role) {
      return HOME_BY_ROLE[store.role] || '/patient/home'
    }

    return true
  })
}

export default setupGuards