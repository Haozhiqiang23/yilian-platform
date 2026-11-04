import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const TOKEN_KEY = 'yilian_token'
const USER_KEY = 'yilian_user'

/**
 * 用户会话 Store
 * - token 与用户信息持久化到 localStorage
 * - 角色：patient（患者）/ doctor（医生）/ admin（管理员）
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref(JSON.parse(localStorage.getItem(USER_KEY) || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => user.value?.role || '')

  /** 登录成功写入会话 */
  function setSession(payload) {
    const { token: t, userId, role: r, name } = payload
    token.value = t || ''
    user.value = { userId, role: r, name }
    localStorage.setItem(TOKEN_KEY, token.value)
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  /** 更新用户信息（改名等） */
  function setUserInfo(partial) {
    user.value = { ...user.value, ...partial }
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  /** 退出登录：清空会话 */
  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, user, isLoggedIn, role, setSession, setUserInfo, logout }
})

/** 角色 → 登录后落地页映射（供路由守卫与登录页使用） */
export const HOME_BY_ROLE = {
  patient: '/patient/home',
  doctor: '/doctor/dashboard',
  admin: '/admin/home',
}