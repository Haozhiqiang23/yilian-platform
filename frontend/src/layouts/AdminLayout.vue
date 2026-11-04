<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import AppLogo from '@/components/AppLogo.vue'

// 管理员端布局壳：桌面优先，左侧菜单
const route = useRoute()
const router = useRouter()
const store = useUserStore()

const menus = [
  { name: 'admin-home', label: '后台首页', path: '/admin/home', icon: 'grid' },
  { name: 'admin-users', label: '用户管理', path: '/admin/users', icon: 'users' },
  { name: 'admin-doctors', label: '医生管理', path: '/admin/doctors', icon: 'stethoscope' },
  { name: 'admin-schedules', label: '号源管理', path: '/admin/schedules', icon: 'calendar' },
  { name: 'admin-refunds', label: '退款管理', path: '/admin/refunds', icon: 'wallet' },
  { name: 'admin-queues', label: '排队管理', path: '/admin/queues', icon: 'queue' },
  { name: 'admin-logs', label: '操作日志', path: '/admin/logs', icon: 'doc' },
  { name: 'admin-config', label: '系统配置', path: '/admin/config', icon: 'settings' },
]

const activeMenu = computed(() => route.name)

function logout() {
  store.logout()
  router.push('/login')
}
</script>

<template>
  <div class="admin-layout">
    <aside class="ad-side">
      <router-link to="/admin/home" class="ad-brand">
        <span class="ad-brand-icon"><AppLogo /></span>
        <span>医联智诊 · 管理端</span>
      </router-link>
      <nav class="ad-menu">
        <button
          v-for="menu in menus"
          :key="menu.name"
          class="ad-menu-item"
          :class="{ active: activeMenu === menu.name }"
          @click="router.push(menu.path)"
        >
          <svg v-if="menu.icon === 'grid'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></svg>
          <svg v-else-if="menu.icon === 'users'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" /></svg>
          <svg v-else-if="menu.icon === 'stethoscope'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4.8 2.3A.3.3 0 1 0 5 2H4a2 2 0 0 0-2 2v5a6 6 0 0 0 6 6 6 6 0 0 0 6-6V4a2 2 0 0 0-2-2h-1a.2.2 0 1 0 .3.3" /><path d="M8 15v1a6 6 0 0 0 6 6 6 6 0 0 0 6-6v-4" /><circle cx="20" cy="10" r="2" /></svg>
          <svg v-else-if="menu.icon === 'calendar'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" /></svg>
          <svg v-else-if="menu.icon === 'wallet'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12V7H5a2 2 0 0 1 0-4h14v4" /><path d="M3 5v14a2 2 0 0 0 2 2h16v-5" /><path d="M18 12a2 2 0 0 0 0 4h4v-4z" /></svg>
          <svg v-else-if="menu.icon === 'queue'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 6h13" /><path d="M8 12h13" /><path d="M8 18h13" /><path d="M3 6h.01" /><path d="M3 12h.01" /><path d="M3 18h.01" /></svg>
          <svg v-else-if="menu.icon === 'settings'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12.22 2h-.44a2 2 0 0 0-2 2l-.1.82a2 2 0 0 1-2.9 1.2l-.72-.42a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.62.5a2 2 0 0 1 0 3.12l-.62.5a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.72-.42a2 2 0 0 1 2.9 1.2l.1.82a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2l.1-.82a2 2 0 0 1 2.9-1.2l.72.42a2 2 0 0 0 2.73-.73l.22-.38a2 2 0 0 0-.73-2.73l-.62-.5a2 2 0 0 1 0-3.12l.62-.5a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.72.42a2 2 0 0 1-2.9-1.2l-.1-.82a2 2 0 0 0-2-2Z" /><circle cx="12" cy="12" r="3" /></svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><path d="M14 2v6h6" /></svg>
          <span>{{ menu.label }}</span>
        </button>
      </nav>
      <div class="ad-foot">
        <span class="ad-name">{{ store.user?.name || '管理员' }}</span>
        <button class="ad-logout" @click="logout">退出登录</button>
      </div>
    </aside>
    <main class="ad-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
}
.ad-side {
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
.ad-brand {
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--color-ink-2);
  font-weight: 600;
  font-size: 14.5px;
  padding: 0 8px 18px;
  border-bottom: 1px solid var(--color-border);
}
.ad-brand-icon {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: linear-gradient(135deg, #14b8a6, #0b7a72);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.ad-brand-icon svg {
  width: 16px;
  height: 16px;
}
.ad-menu {
  flex: 1;
  padding-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.ad-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10.5px 12px;
  border: none;
  background: none;
  border-radius: var(--radius-sm);
  color: var(--color-ink-3);
  font-size: 14px;
  font-weight: 500;
  text-align: left;
  transition: all 0.18s ease;
}
.ad-menu-item:hover {
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
}
.ad-menu-item.active {
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
  font-weight: 600;
}
.ad-foot {
  border-top: 1px solid var(--color-border);
  padding-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.ad-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink-2);
}
.ad-logout {
  border: none;
  background: none;
  color: var(--color-ink-3);
  font-size: 12.5px;
  cursor: pointer;
}
.ad-logout:hover {
  color: var(--color-danger);
}
.ad-main {
  flex: 1;
  min-width: 0;
  padding: 24px;
  background: var(--color-bg);
}
</style>
