<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUnreadMessageCount } from '@/api/messages'
import AppLogo from '@/components/AppLogo.vue'
import MedicalAssistant from '@/components/patient/MedicalAssistant.vue'

// 患者端布局壳：移动优先
// 顶部（品牌 + 头像） + 内容区 + 底部 Tab（首页/预约/记录/消息/我的）
const route = useRoute()
const router = useRouter()
const store = useUserStore()
const unreadMessageCount = ref(0)

const tabs = [
  { name: 'patient-home', label: '首页', path: '/patient/home', icon: 'home' },
  { name: 'patient-appointments', label: '预约', path: '/patient/appointments', icon: 'calendar' },
  { name: 'patient-records', label: '记录', path: '/patient/records', icon: 'doc' },
  { name: 'patient-messages', label: '消息', path: '/patient/messages', icon: 'bell' },
  { name: 'patient-profile', label: '我的', path: '/patient/profile', icon: 'user' },
]

const activeTab = computed(() => route.name)
const unreadBadgeText = computed(() => unreadMessageCount.value > 99 ? '99+' : String(unreadMessageCount.value))

async function loadUnreadCount() {
  try {
    const data = await getUnreadMessageCount()
    unreadMessageCount.value = Math.max(0, Number(data?.unreadCount || 0))
  } catch {
    unreadMessageCount.value = 0
  }
}

function syncUnreadCount(event) {
  const count = Number(event.detail)
  if (Number.isFinite(count)) {
    unreadMessageCount.value = Math.max(0, count)
  } else {
    loadUnreadCount()
  }
}

watch(() => route.fullPath, loadUnreadCount, { immediate: true })
window.addEventListener('patient-unread-count-changed', syncUnreadCount)
onBeforeUnmount(() => window.removeEventListener('patient-unread-count-changed', syncUnreadCount))

function go(path) {
  if (path) router.push(path)
}
</script>

<template>
  <div class="patient-layout">
    <header class="pt-header">
      <router-link to="/patient/home" class="pt-brand">
        <span class="pt-brand-icon"><AppLogo /></span>
        <span class="pt-brand-name">医联智诊</span>
      </router-link>
      <router-link to="/patient/profile" class="pt-avatar">{{ store.user?.name?.[0] || '患' }}</router-link>
    </header>

    <main class="pt-main">
      <router-view />
    </main>

    <MedicalAssistant />

    <nav class="pt-tabbar">
      <button
        v-for="tab in tabs"
        :key="tab.name"
        class="pt-tab"
        :class="{ active: activeTab === tab.name }"
        :aria-label="tab.label"
        @click="go(tab.path)"
      >
        <span class="pt-tab-icon">
          <svg v-if="tab.icon === 'home'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 10.5L12 3l9 7.5" /><path d="M5 9.5V21h14V9.5" /></svg>
          <svg v-else-if="tab.icon === 'calendar'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" /></svg>
          <svg v-else-if="tab.icon === 'doc'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><path d="M14 2v6h6M9 13h6M9 17h6" /></svg>
          <svg v-else-if="tab.icon === 'bell'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.7 21a2 2 0 0 1-3.4 0" /></svg>
          <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="4" /><path d="M4 21v-1a7 7 0 0 1 14 0v1" /></svg>
        </span>
        <span
          v-if="tab.name === 'patient-messages' && unreadMessageCount > 0"
          class="pt-tab-badge"
          :aria-label="`${unreadMessageCount} 条未读消息`"
        >{{ unreadBadgeText }}</span>
        <span class="pt-tab-label">{{ tab.label }}</span>
      </button>
    </nav>
  </div>
</template>

<style scoped>
.patient-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}
.pt-header {
  height: var(--topbar-h);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 30;
}
.pt-brand {
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--color-ink-2);
  font-weight: 600;
  font-size: 16px;
}
.pt-brand-icon {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: linear-gradient(135deg, #14b8a6, #0b7a72);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.pt-brand-icon svg {
  width: 16px;
  height: 16px;
}
.pt-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.pt-main {
  flex: 1;
  padding-bottom: calc(var(--bottom-nav-h) + 16px);
}
.pt-tabbar {
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
  bottom: 0;
  width: 100%;
  max-width: 480px;
  height: var(--bottom-nav-h);
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(12px);
  border-top: 1px solid var(--color-border);
  display: flex;
  z-index: 40;
}
.pt-tab {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  border: none;
  background: none;
  color: var(--color-ink-3);
  font-size: 11px;
  transition: color 0.18s ease;
}
.pt-tab.active {
  color: var(--color-primary);
  font-weight: 600;
}
.pt-tab-badge {
  position: absolute;
  top: 6px;
  right: calc(50% - 18px);
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  background: var(--color-danger);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}
</style>
