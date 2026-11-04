<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getDoctors, getMyAppointments, getProfile } from '@/api/patient'
import { getUnreadMessageCount } from '@/api/messages'

// 患者首页：渐变头部 + 搜索 + 待支付提醒 + 快捷服务 + 常用医生
const router = useRouter()
const store = useUserStore()

const greeting = ref('您好')
const pendingCount = ref(0)
const pendingAppointment = ref(null)
const displayName = ref('')
const unreadMessageCount = ref(0)
const doctors = ref([])
const doctorsLoading = ref(false)
const doctorsError = ref('')

const services = [
  { key: 'doctors', label: '医生查询', desc: '按科室预约挂号', to: '/patient/doctors', icon: 'stethoscope' },
  { key: 'appointments', label: '我的预约', desc: '预约与排队记录', to: '/patient/appointments', icon: 'calendar' },
  { key: 'queue', label: '排队签到', desc: '到院签到查看排队', to: '/patient/appointments', icon: 'queue' },
  { key: 'records', label: '诊疗记录', desc: '病历处方检查报告', to: '/patient/records', icon: 'doc' },
]

const doctorColors = [
  'linear-gradient(135deg,#14b8a6,#0b7a72)',
  'linear-gradient(135deg,#185fa5,#378add)',
  'linear-gradient(135deg,#d97706,#f59e0b)',
  'linear-gradient(135deg,#7c3aed,#9333ea)',
]

function hourGreeting() {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 18) return '下午好'
  return '晚上好'
}

async function loadPending() {
  try {
    const data = await getMyAppointments({ status: '待支付', page: 1, size: 1 })
    pendingCount.value = Number(data?.total || 0)
    pendingAppointment.value = data?.records?.[0] || null
  } catch {
    pendingCount.value = 0
    pendingAppointment.value = null
  }
}

function appointmentIdOf(item) {
  return item?.appointmentId || item?.id
}

function pendingTimeText(item) {
  if (!item) return ''
  const date = item.visitDate || item.date || ''
  const start = item.startTime || ''
  const end = item.endTime || ''
  const period = start && end ? `${start} - ${end}` : (item.timePeriod || '')
  return [date, period].filter(Boolean).join(' ')
}

function goPendingPayment() {
  const id = appointmentIdOf(pendingAppointment.value)
  router.push(id ? `/patient/appointments/${id}/pay` : '/patient/appointments')
}

async function loadProfile() {
  try {
    const data = await getProfile()
    const name = typeof data?.name === 'string' ? data.name.trim() : ''
    displayName.value = name || store.user?.name || '朋友'
    if (name) {
      store.setUserInfo({ name })
    }
  } catch {
    displayName.value = store.user?.name || '朋友'
  }
}

async function loadDoctors() {
  doctorsLoading.value = true
  doctorsError.value = ''
  try {
    const data = await getDoctors({ page: 1, size: 4, date: todayText() })
    const list = Array.isArray(data) ? data : (data?.records || data?.list || [])
    doctors.value = list.map((item, index) => ({
      ...item,
      id: item.doctorId || item.id,
      dept: item.department || item.departmentName || '科室待返回',
      remain: item.remainCount ?? item.remainingCount ?? 0,
      color: doctorColors[index % doctorColors.length],
    }))
  } catch (e) {
    doctorsError.value = e.message || '暂时无法加载常用医生'
    doctors.value = []
  } finally {
    doctorsLoading.value = false
  }
}

function todayText() {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

async function loadUnreadMessages() {
  try {
    const data = await getUnreadMessageCount()
    unreadMessageCount.value = Number(data?.unreadCount || 0)
  } catch {
    unreadMessageCount.value = 0
  }
}

onMounted(() => {
  greeting.value = hourGreeting()
  loadProfile()
  loadPending()
  loadUnreadMessages()
  loadDoctors()
})
</script>

<template>
  <div class="home-view">
    <div class="home-top">
      <div class="user-row">
        <span class="user-avatar">{{ displayName?.[0] || '患' }}</span>
        <button class="bell" aria-label="消息通知" type="button" @click="router.push('/patient/messages')">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.7 21a2 2 0 0 1-3.4 0" /></svg>
          <i v-if="unreadMessageCount > 0" class="bell-dot">{{ unreadMessageCount }}</i>
        </button>
      </div>
      <div class="user-hello">
        <h2 class="hello-name">{{ greeting }}，{{ displayName || '朋友' }}</h2>
        <p class="hello-sub">当前有 {{ pendingCount }} 个待支付预约</p>
      </div>
    </div>

    <div class="home-search">
      <div class="search-box">
        <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7" /><path d="M21 21l-4.3-4.3" /></svg>
        <input type="text" placeholder="搜索医生、科室或疾病" readonly @click="router.push('/patient/doctors')" />
        <button class="search-btn" @click="router.push('/patient/doctors')">搜索</button>
      </div>
    </div>

    <div v-if="pendingCount > 0" class="alert-banner">
      <span class="warn-ic">
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z" /><path d="M12 9v4M12 17h.01" /></svg>
      </span>
      <div class="warn-text">
        <p class="warn-t">您有 {{ pendingCount }} 笔预约待支付</p>
        <p class="warn-d">
          {{ pendingAppointment?.doctorName || '预约信息' }} ·
          {{ pendingTimeText(pendingAppointment) || '请尽快完成支付' }}
        </p>
      </div>
      <button class="go" @click="goPendingPayment">去支付</button>
    </div>

    <div class="home-body">
      <div class="section-title"><h3>快捷服务</h3></div>
      <div class="func-grid">
        <button v-for="s in services" :key="s.key" class="func-card" @click="router.push(s.to)">
          <span class="func-ic" :class="s.key">
            <svg v-if="s.icon === 'stethoscope'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4.8 2.3A.3.3 0 1 0 5 2H4a2 2 0 0 0-2 2v5a6 6 0 0 0 6 6 6 6 0 0 0 6-6V4a2 2 0 0 0-2-2h-1a.2.2 0 1 0 .3.3" /><path d="M8 15v1a6 6 0 0 0 6 6 6 6 0 0 0 6-6v-4" /><circle cx="20" cy="10" r="2" /></svg>
            <svg v-else-if="s.icon === 'calendar'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" /></svg>
            <svg v-else-if="s.icon === 'queue'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" /></svg>
            <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><path d="M14 2v6h6M9 13h6M9 17h6" /></svg>
          </span>
          <span v-if="s.badge" class="func-badge">{{ s.badge }}</span>
          <span class="func-t">{{ s.label }}</span>
          <span class="func-d">{{ s.desc }}</span>
        </button>
      </div>

      <div class="section-title with-more">
        <h3>常用医生</h3>
        <router-link class="more" to="/patient/doctors">查看全部</router-link>
      </div>
      <div v-if="doctorsLoading" class="state-card">正在加载医生信息...</div>
      <p v-else-if="doctorsError" class="notice danger">{{ doctorsError }}</p>
      <div v-else-if="doctors.length === 0" class="state-card">暂时没有可预约的医生</div>
      <div v-else class="doctors-scroll">
        <button v-for="d in doctors" :key="d.id" class="dr-card" type="button" @click="router.push(`/patient/doctors/${d.id}`)">
          <span class="dr-avatar" :style="{ background: d.color }">{{ d.name[0] }}</span>
          <span class="dr-name">{{ d.name }}</span>
          <span class="dr-meta">{{ d.dept }} · {{ d.title || '职称待返回' }}</span>
          <span class="dr-remain">{{ d.remain > 0 ? `查看号源 · 余号${d.remain}` : '查看详情' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-view {
  min-height: 100%;
  background: var(--color-bg);
}
.home-top {
  background: linear-gradient(160deg, #0e8f86 0%, #12a79b 55%, #16a34a 130%);
  padding: 18px 20px 40px;
  color: #fff;
  position: relative;
  overflow: hidden;
}
.home-top::before {
  content: '';
  position: absolute;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  top: -80px;
  right: -55px;
}
.home-top::after {
  content: '';
  position: absolute;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  bottom: -45px;
  left: 30px;
}
.user-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 1;
}
.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  border: 2px solid rgba(255, 255, 255, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}
.bell {
  position: relative;
  cursor: pointer;
  opacity: 0.92;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: inherit;
  appearance: none;
  transition: background-color 0.18s ease, transform 0.18s ease;
}
.bell:hover {
  background: rgba(255, 255, 255, 0.16);
  transform: translateY(-1px);
}
.bell:focus-visible {
  outline: 3px solid rgba(255, 255, 255, 0.72);
  outline-offset: 2px;
}
.bell-dot {
  position: absolute;
  top: -5px;
  right: -8px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  background: var(--color-danger);
  font-size: 10px;
  font-style: normal;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}
.user-hello {
  margin-top: 18px;
  position: relative;
  z-index: 1;
}
.hello-name {
  font-size: 20px;
  font-weight: 600;
}
.hello-sub {
  font-size: 12.5px;
  opacity: 0.92;
  margin-top: 5px;
}
.home-search {
  margin: -20px 16px 0;
  position: relative;
  z-index: 2;
}
.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 4px 6px 4px 14px;
  box-shadow: var(--shadow);
}
.search-box svg {
  color: var(--color-ink-3);
  flex-shrink: 0;
}
.search-box input {
  border: none;
  outline: none;
  flex: 1;
  font-size: 14px;
  font-family: var(--font-sans);
  color: var(--color-ink);
  background: transparent;
  padding: 10px 0;
  cursor: pointer;
}
.search-box input::placeholder {
  color: #9dbdb8;
}
.search-btn {
  border: none;
  background: linear-gradient(135deg, #14b8a6, #0b7a72);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  padding: 9px 16px;
  border-radius: 10px;
  cursor: pointer;
  font-family: var(--font-sans);
  transition: opacity 0.18s;
}
.search-btn:hover {
  opacity: 0.9;
}
.alert-banner {
  margin: 14px 16px 0;
  background: var(--color-warn-soft);
  border: 1px solid rgba(217, 119, 6, 0.22);
  border-radius: 14px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  gap: 11px;
}
.warn-ic {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: rgba(217, 119, 6, 0.14);
  color: var(--color-warn);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.warn-text {
  flex: 1;
  min-width: 0;
}
.warn-t {
  font-size: 13px;
  font-weight: 600;
  color: #92400e;
}
.warn-d {
  font-size: 11.5px;
  color: #b45309;
  margin-top: 2px;
}
.go {
  flex-shrink: 0;
  border: none;
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  padding: 8px 13px;
  border-radius: 9px;
  cursor: pointer;
  font-family: var(--font-sans);
}
.home-body {
  padding: 22px 16px calc(var(--bottom-nav-h) + 20px);
}
.section-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 13px;
}
.section-title h3 {
  font-size: 15.5px;
  font-weight: 600;
  color: var(--color-ink);
}
.with-more {
  margin-top: 24px;
}
.more {
  font-size: 12.5px;
  color: var(--color-ink-3);
}
.func-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.func-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 16px 15px;
  cursor: pointer;
  font-family: var(--font-sans);
  text-align: left;
  position: relative;
  overflow: hidden;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.func-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow);
}
.func-ic {
  width: 40px;
  height: 40px;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}
.func-ic.doctors {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}
.func-ic.appointments {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}
.func-ic.queue {
  background: var(--color-info-soft);
  color: var(--color-info);
}
.func-ic.records {
  background: var(--color-warn-soft);
  color: var(--color-warn);
}
.func-t {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-ink);
}
.func-d {
  font-size: 11.5px;
  color: var(--color-ink-3);
  margin-top: 4px;
  line-height: 1.5;
}
.func-badge {
  position: absolute;
  top: 13px;
  right: 13px;
  background: var(--color-danger);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  min-width: 17px;
  height: 17px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
}
.doctors-scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding: 4px 2px 8px;
  scrollbar-width: none;
}
.doctors-scroll::-webkit-scrollbar {
  display: none;
}
.dr-card {
  flex: 0 0 160px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 15px;
  cursor: pointer;
  font-family: var(--font-sans);
  text-align: left;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.dr-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow);
}
.dr-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 11px;
}
.dr-name {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-ink);
}
.dr-meta {
  font-size: 11px;
  color: var(--color-ink-3);
  margin-top: 3px;
}
.dr-remain {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 9px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--color-accent);
  background: var(--color-accent-soft);
  padding: 3px 8px;
  border-radius: 20px;
}
.dr-remain.short {
  color: var(--color-warn);
  background: var(--color-warn-soft);
}
</style>
