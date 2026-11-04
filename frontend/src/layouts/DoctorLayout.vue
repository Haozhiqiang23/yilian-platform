<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import AppLogo from '@/components/AppLogo.vue'

// 医生端布局壳：桌面优先，左侧菜单
const route = useRoute()
const router = useRouter()
const store = useUserStore()

const menus = [
  { name: 'doctor-dashboard', label: '工作台', path: '/doctor/dashboard', icon: 'grid' },
  { name: 'doctor-queues', label: '患者列表', path: '/doctor/queues', icon: 'queue' },
  { name: 'doctor-reports', label: '报告管理', path: '/doctor/reports', icon: 'doc' },
  { name: 'doctor-profile', label: '个人信息', path: '/doctor/profile', icon: 'user' },
]

const activeMenu = computed(() => route.name)

function logout() {
  store.logout()
  router.push('/login')
}
</script>

<template>
  <div class="doctor-layout">
    <aside class="dr-side">
      <router-link to="/doctor/dashboard" class="dr-brand">
        <span class="dr-brand-icon"><AppLogo /></span>
        <span class="dr-brand-name">医联智诊</span>
      </router-link>
      <nav class="dr-menu">
        <button
          v-for="menu in menus"
          :key="menu.name"
          class="dr-menu-item"
          :class="{ active: activeMenu === menu.name }"
          @click="router.push(menu.path)"
        >
          <svg v-if="menu.icon === 'grid'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></svg>
          <svg v-else-if="menu.icon === 'queue'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" /></svg>
          <svg v-else-if="menu.icon === 'doc'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><path d="M14 2v6h6M9 13h6M9 17h6" /></svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="4" /><path d="M4 21v-1a7 7 0 0 1 14 0v1" /></svg>
          <span>{{ menu.label }}</span>
        </button>
      </nav>
      <div class="dr-foot">
        <span class="dr-name">{{ store.user?.name || '医生' }}</span>
        <button class="dr-logout" @click="logout">退出登录</button>
      </div>
    </aside>
    <main class="dr-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.doctor-layout {
  display: flex;
  min-height: 100vh;
}
.dr-side {
  width: 200px;
  flex-shrink: 0;
  background: var(--color-surface);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  padding: 18px 12px;
  position: sticky;
  top: 0;
  height: 100vh;
}
.dr-brand {
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--color-ink-2);
  font-weight: 600;
  font-size: 15px;
  padding: 0 8px 18px;
  border-bottom: 1px solid var(--color-border);
}
.dr-brand-icon {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: linear-gradient(135deg, #14b8a6, #0b7a72);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.dr-brand-icon svg {
  width: 16px;
  height: 16px;
}
.dr-menu {
  flex: 1;
  padding-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.dr-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 12px;
  border: none;
  background: none;
  border-radius: var(--radius-sm);
  color: var(--color-ink-3);
  font-size: 14px;
  font-weight: 500;
  text-align: left;
  transition: all 0.18s ease;
}
.dr-menu-item:hover {
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
}
.dr-menu-item.active {
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
  font-weight: 600;
}
.dr-foot {
  border-top: 1px solid var(--color-border);
  padding-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.dr-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink-2);
}
.dr-logout {
  border: none;
  background: none;
  color: var(--color-ink-3);
  font-size: 12.5px;
  cursor: pointer;
}
.dr-logout:hover {
  color: var(--color-danger);
}
.dr-main {
  flex: 1;
  min-width: 0;
  padding: 24px;
}
</style>