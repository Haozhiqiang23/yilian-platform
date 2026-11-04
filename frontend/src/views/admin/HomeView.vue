<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getStatistics } from '@/api/admin'

const router = useRouter()
const loading = ref(false)
const errorMsg = ref('')
const stats = ref(null)

const statCards = computed(() => [
  {
    key: 'totalUsers',
    label: '用户总数',
    value: stats.value?.totalUsers ?? stats.value?.userCount,
    desc: '平台注册用户规模',
    tone: 'primary',
  },
  {
    key: 'totalDoctors',
    label: '医生总数',
    value: stats.value?.totalDoctors ?? stats.value?.doctorCount,
    desc: '医生账号与审核对象',
    tone: 'info',
  },
  {
    key: 'todayAppointments',
    label: '今日预约数',
    value: stats.value?.todayAppointments ?? stats.value?.todayAppointmentCount,
    desc: '今日预约业务量',
    tone: 'success',
  },
  {
    key: 'todayCompleted',
    label: '今日完成数',
    value: stats.value?.todayCompleted,
    desc: '今日完成就诊统计',
    tone: 'success',
  },
  {
    key: 'pendingRefundCount',
    label: '待退款数',
    value: stats.value?.pendingRefundCount,
    desc: '等待管理员审核处理',
    tone: 'warning',
  },
  {
    key: 'pendingAuditDoctorCount',
    label: '待审核医生',
    value: stats.value?.pendingAuditDoctorCount,
    desc: '医生资质待审核',
    tone: 'warning',
  },
])

const todoItems = computed(() => [
  {
    title: '医生审核',
    value: stats.value?.pendingAuditDoctorCount,
    path: '/admin/doctors',
    desc: '处理医生账号通过或拒绝',
  },
  {
    title: '退款处理',
    value: stats.value?.pendingRefundCount,
    path: '/admin/refunds',
    desc: '处理患者退款申请',
  },
  {
    title: '号源管理',
    value: null,
    path: '/admin/schedules',
    desc: '维护医生排班与可预约号源',
  },
])

const operationMetrics = computed(() => {
  const totalUsers = Number(stats.value?.totalUsers ?? stats.value?.userCount ?? 0)
  const totalDoctors = Number(stats.value?.totalDoctors ?? stats.value?.doctorCount ?? 0)
  const todayAppointments = Number(
    stats.value?.todayAppointments ?? stats.value?.todayAppointmentCount ?? 0,
  )
  const todayCompleted = Number(stats.value?.todayCompleted ?? 0)
  const pendingRefunds = Number(stats.value?.pendingRefundCount ?? 0)
  const pendingDoctors = Number(stats.value?.pendingAuditDoctorCount ?? 0)
  const pendingTotal = pendingRefunds + pendingDoctors

  return [
    {
      key: 'completion',
      label: '今日就诊完成率',
      value: todayCompleted,
      total: todayAppointments,
      percent: percentage(todayCompleted, todayAppointments),
      detail: todayAppointments
        ? `已完成 ${todayCompleted} / ${todayAppointments} 个预约`
        : '今日暂无预约记录',
      tone: 'success',
    },
    {
      key: 'doctorRatio',
      label: '医生账号占比',
      value: totalDoctors,
      total: totalUsers,
      percent: percentage(totalDoctors, totalUsers),
      detail: totalUsers
        ? `${totalDoctors} 名医生，共 ${totalUsers} 个平台账号`
        : '暂无账号统计数据',
      tone: 'primary',
    },
    {
      key: 'pending',
      label: '当前待办状态',
      value: pendingTotal,
      total: null,
      percent: pendingTotal ? 100 : 0,
      detail: pendingTotal
        ? `医生审核 ${pendingDoctors} 项，退款处理 ${pendingRefunds} 项`
        : '医生审核和退款申请均已处理完毕',
      tone: pendingTotal ? 'warning' : 'success',
    },
  ]
})

function percentage(value, total) {
  if (!total) return 0
  return Math.min(100, Math.round((value / total) * 100))
}

async function loadStatistics() {
  loading.value = true
  errorMsg.value = ''
  try {
    stats.value = await getStatistics()
  } catch (e) {
    errorMsg.value = e.message || '统计数据加载失败，请稍后重试'
    stats.value = null
  } finally {
    loading.value = false
  }
}

function displayValue(value) {
  return value ?? '--'
}

function go(path) {
  router.push(path)
}

onMounted(loadStatistics)
</script>

<template>
  <div class="admin-home">
    <section class="home-hero" aria-labelledby="admin-home-title">
      <div>
        <p class="eyebrow">Admin Overview</p>
        <h1 id="admin-home-title">后台首页</h1>
        <p>管理员端首页聚合用户、医生、预约和待处理业务，统计数据来自管理员端统计接口。</p>
      </div>
      <button class="refresh-btn" :disabled="loading" type="button" @click="loadStatistics">
        {{ loading ? '刷新中' : '刷新统计' }}
      </button>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>

    <div v-if="loading" class="loading-state">正在加载统计数据...</div>

    <template v-else>
      <section class="stats-grid" aria-label="平台统计">
        <article v-for="card in statCards" :key="card.key" class="stat-card" :class="`tone-${card.tone}`">
          <span>{{ card.label }}</span>
          <strong>{{ displayValue(card.value) }}</strong>
          <p>{{ card.desc }}</p>
        </article>
      </section>

      <section class="dashboard-grid">
        <article class="panel quick-panel">
          <div class="panel-head">
            <div>
              <h2>待办处理</h2>
              <p>从统计数据进入对应管理页面。</p>
            </div>
          </div>

          <button v-for="item in todoItems" :key="item.title" class="todo-row" type="button" @click="go(item.path)">
            <span>
              <strong>{{ item.title }}</strong>
              <small>{{ item.desc }}</small>
            </span>
            <em>{{ displayValue(item.value) }}</em>
          </button>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <h2>快捷入口</h2>
              <p>管理员端以表格和操作按钮为主，适合高频审核与维护。</p>
            </div>
          </div>

          <div class="quick-grid">
            <button type="button" @click="go('/admin/users')">用户管理</button>
            <button type="button" @click="go('/admin/doctors')">医生管理</button>
            <button type="button" @click="go('/admin/schedules')">号源管理</button>
            <button type="button" @click="go('/admin/logs')">操作日志</button>
          </div>
        </article>
      </section>

      <section class="panel operation-panel" aria-labelledby="operation-title">
        <div class="panel-head operation-head">
          <div>
            <h2 id="operation-title">运营概览</h2>
            <p>根据当前统计数据快速判断今日业务进度与待办情况。</p>
          </div>
          <span class="data-status">数据已更新</span>
        </div>

        <div class="metric-grid">
          <article v-for="metric in operationMetrics" :key="metric.key" class="metric-item">
            <div class="metric-title">
              <span>{{ metric.label }}</span>
              <strong v-if="metric.total !== null">{{ metric.percent }}%</strong>
              <strong v-else>{{ metric.value }} 项</strong>
            </div>
            <div
              class="progress-track"
              role="progressbar"
              :aria-label="metric.label"
              aria-valuemin="0"
              aria-valuemax="100"
              :aria-valuenow="metric.percent"
            >
              <span
                class="progress-value"
                :class="`tone-${metric.tone}`"
                :style="{ width: `${metric.percent}%` }"
              />
            </div>
            <p>{{ metric.detail }}</p>
          </article>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.admin-home {
  min-height: 100%;
}

.home-hero,
.stat-card,
.panel,
.loading-state {
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}

.home-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 22px;
}

.eyebrow {
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 800;
}

.home-hero h1 {
  margin-top: 4px;
  color: var(--color-ink);
  font-size: 24px;
  font-weight: 800;
}

.home-hero p {
  max-width: 680px;
  margin-top: 8px;
  color: var(--color-ink-3);
  font-size: 13px;
}

.refresh-btn {
  min-height: 42px;
  border: 1.5px solid var(--color-primary);
  border-radius: var(--radius-sm);
  padding: 0 16px;
  background: var(--color-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
}

.refresh-btn:disabled {
  opacity: 0.48;
  cursor: not-allowed;
}

.notice {
  margin-top: 14px;
  border-radius: var(--radius-sm);
  padding: 11px 13px;
  font-size: 13px;
  font-weight: 700;
}

.notice.danger {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.loading-state {
  margin-top: 14px;
  padding: 36px 16px;
  color: var(--color-ink-3);
  text-align: center;
  font-size: 13px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.stat-card {
  position: relative;
  overflow: hidden;
  padding: 16px;
}

.stat-card::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: var(--color-primary);
}

.stat-card.tone-info::before {
  background: var(--color-info);
}

.stat-card.tone-success::before {
  background: var(--color-accent);
}

.stat-card.tone-warning::before {
  background: var(--color-warn);
}

.stat-card span {
  display: block;
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 700;
}

.stat-card strong {
  display: block;
  margin-top: 6px;
  color: var(--color-ink);
  font-size: 30px;
  line-height: 1.18;
  font-variant-numeric: tabular-nums;
}

.stat-card p {
  margin-top: 6px;
  color: var(--color-ink-3);
  font-size: 12px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 14px;
  margin-top: 14px;
  align-items: stretch;
}

.panel {
  padding: 16px;
}

.dashboard-grid > .panel {
  min-height: 100%;
}

.panel-head {
  margin-bottom: 12px;
}

.panel-head h2 {
  color: var(--color-ink);
  font-size: 17px;
  font-weight: 800;
}

.panel-head p {
  margin-top: 3px;
  color: var(--color-ink-3);
  font-size: 12px;
}

.todo-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  min-height: 66px;
  margin-top: 10px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 12px;
  background: #fff;
  color: var(--color-ink);
  text-align: left;
}

.todo-row:hover,
.quick-grid button:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
}

.todo-row strong,
.todo-row small {
  display: block;
}

.todo-row strong {
  font-size: 14px;
}

.todo-row small {
  margin-top: 3px;
  color: var(--color-ink-3);
  font-size: 12px;
}

.todo-row em {
  min-width: 38px;
  color: var(--color-primary-deep);
  font-size: 22px;
  font-style: normal;
  font-weight: 900;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(2, minmax(66px, 1fr));
  gap: 10px;
}

.quick-panel {
  display: flex;
  flex-direction: column;
}

.quick-panel .quick-grid {
  flex: 1;
}

.quick-grid button {
  min-height: 66px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fff;
  color: var(--color-ink-2);
  font-size: 13px;
  font-weight: 800;
}

.operation-panel {
  margin-top: 14px;
}

.operation-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.data-status {
  flex: none;
  border-radius: 999px;
  padding: 6px 10px;
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-size: 12px;
  font-weight: 800;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.metric-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 14px;
  background: var(--color-primary-soft);
}

.metric-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--color-ink-2);
  font-size: 13px;
  font-weight: 800;
}

.metric-title strong {
  color: var(--color-primary-deep);
  font-size: 17px;
  font-variant-numeric: tabular-nums;
}

.progress-track {
  height: 8px;
  margin-top: 13px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--color-border);
}

.progress-value {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--color-primary);
}

.progress-value.tone-success {
  background: var(--color-accent);
}

.progress-value.tone-warning {
  background: var(--color-warn);
}

.metric-item p {
  margin-top: 10px;
  color: var(--color-ink-3);
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 980px) {
  .stats-grid,
  .dashboard-grid,
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .home-hero {
    align-items: stretch;
    flex-direction: column;
  }
}

@media (max-width: 560px) {
  .home-hero,
  .panel {
    padding: 14px;
  }

  .stats-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
