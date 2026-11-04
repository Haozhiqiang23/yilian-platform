<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getDoctorQueues, startAppointment } from '@/api/doctor'

const router = useRouter()

const loading = ref(false)
const actionId = ref('')
const errorMsg = ref('')
const successMsg = ref('')
const selectedId = ref('')
const queues = ref([])

const statusMeta = {
  已签到: { label: '已签到', tone: 'ready' },
  接诊中: { label: '接诊中', tone: 'active' },
  已完成: { label: '已完成', tone: 'done' },
}

const normalizedQueues = computed(() =>
  queues.value.map((item, index) => {
    const id = appointmentIdOf(item)
    const status = item.status || item.queueStatus || item.appointmentStatus || '状态待确认'
    return {
      ...item,
      appointmentId: id,
      displayStatus: status,
      sortNo: item.queueNo || item.queueNumber || String(index + 1).padStart(2, '0'),
    }
  }),
)

const selectedQueue = computed(() =>
  normalizedQueues.value.find((item) => item.appointmentId === selectedId.value) || normalizedQueues.value[0],
)

const overview = computed(() => {
  const total = normalizedQueues.value.length
  const active = normalizedQueues.value.filter((item) => item.displayStatus === '接诊中').length
  const done = normalizedQueues.value.filter((item) => item.displayStatus === '已完成').length
  const pending = normalizedQueues.value.filter((item) => item.displayStatus === '已签到').length
  return { total, pending, active, done }
})

watch(normalizedQueues, (list) => {
  if (!list.length) {
    selectedId.value = ''
    return
  }
  if (!list.some((item) => item.appointmentId === selectedId.value)) {
    selectedId.value = list[0].appointmentId
  }
})

async function loadQueues() {
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const data = await getDoctorQueues()
    queues.value = normalizeResponseList(data)
  } catch (e) {
    errorMsg.value = e.message || '暂时无法加载今日患者队列，请稍后重试'
    queues.value = []
  } finally {
    loading.value = false
  }
}

function normalizeResponseList(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data?.rows)) return data.rows
  return []
}

function appointmentIdOf(item) {
  return String(item?.appointmentId || item?.id || '')
}

function statusTone(status) {
  return statusMeta[status]?.tone || 'wait'
}

function canStart(item) {
  return item?.displayStatus === '已签到'
}

async function runAction(id, task, message) {
  if (actionId.value) return
  actionId.value = id
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const result = await task()
    successMsg.value = result?.medicalRecordId
      ? `${message}，诊疗记录编号：${result.medicalRecordId}`
      : message
    await loadQueues()
    return result
  } catch (e) {
    errorMsg.value = e.message || '操作未完成，请稍后重试'
    return null
  } finally {
    actionId.value = ''
  }
}

function handleSelect(item) {
  selectedId.value = item.appointmentId
}

async function handleStart(item) {
  if (!canStart(item)) return
  const result = await runAction(
    `start-${item.appointmentId}`,
    () => startAppointment(item.appointmentId),
    '已开始接诊',
  )
  if (result !== null) openRecord(item)
}

function openRecord(item) {
  router.push(`/doctor/records/${item.appointmentId}`)
}

onMounted(loadQueues)
</script>

<template>
  <div class="dashboard-view">
    <section class="hero-panel" aria-labelledby="dashboard-title">
      <div>
        <p class="eyebrow">Doctor Workspace</p>
        <h1 id="dashboard-title">医生工作台</h1>
        <p class="hero-desc">查看今日预约患者，完成签到后的接诊与诊疗记录。</p>
      </div>
      <button class="refresh-btn" :disabled="loading" type="button" @click="loadQueues">
        {{ loading ? '刷新中' : '刷新队列' }}
      </button>
    </section>

    <section class="overview-grid" aria-label="今日概览">
      <article class="metric-card">
        <span>今日预约</span>
        <strong>{{ overview.total }}</strong>
      </article>
      <article class="metric-card">
        <span>待接诊</span>
        <strong>{{ overview.pending }}</strong>
      </article>
      <article class="metric-card">
        <span>接诊中</span>
        <strong>{{ overview.active }}</strong>
      </article>
      <article class="metric-card">
        <span>已完成</span>
        <strong>{{ overview.done }}</strong>
      </article>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>
    <p v-if="successMsg" class="notice success" role="status">{{ successMsg }}</p>

    <section class="workspace-grid">
      <div class="queue-panel">
        <div class="section-head">
          <div>
            <h2>待接诊队列</h2>
            <p>显示当前医生的今日预约患者，按排队顺序接诊</p>
          </div>
          <span class="count-badge">{{ normalizedQueues.length }} 条</span>
        </div>

        <div v-if="loading" class="empty-state">正在加载今日患者...</div>
        <div v-else-if="normalizedQueues.length === 0" class="empty-state">今天暂无待接诊患者</div>

        <button
          v-for="item in normalizedQueues"
          v-else
          :key="item.appointmentId"
          class="queue-item"
          :class="{ active: selectedQueue?.appointmentId === item.appointmentId }"
          type="button"
          @click="handleSelect(item)"
        >
          <span class="queue-no">{{ item.sortNo }}</span>
          <span class="queue-main">
            <strong>{{ item.patientName || '患者姓名未提供' }}</strong>
            <small>{{ item.appointmentTime || item.scheduleTime || item.visitTime || '就诊时间待确认' }}</small>
          </span>
          <span class="status-tag" :class="`status-${statusTone(item.displayStatus)}`">
            {{ statusMeta[item.displayStatus]?.label || item.displayStatus }}
          </span>
        </button>
      </div>

      <aside class="detail-panel" aria-label="当前接诊">
        <template v-if="selectedQueue">
          <div class="section-head">
            <div>
              <h2>当前患者</h2>
              <p>预约编号：{{ selectedQueue.appointmentId || '--' }}</p>
            </div>
            <span class="status-tag" :class="`status-${statusTone(selectedQueue.displayStatus)}`">
              {{ selectedQueue.displayStatus }}
            </span>
          </div>

          <div class="patient-card">
            <div>
              <span>患者姓名</span>
              <strong>{{ selectedQueue.patientName || '患者信息未提供' }}</strong>
            </div>
            <div>
              <span>患者ID</span>
              <strong>{{ selectedQueue.patientId || '--' }}</strong>
            </div>
            <div>
              <span>预约时间</span>
              <strong>{{ selectedQueue.appointmentTime || selectedQueue.scheduleTime || selectedQueue.visitTime || '就诊时间待确认' }}</strong>
            </div>
            <div>
              <span>排队号</span>
              <strong>{{ selectedQueue.sortNo }}</strong>
            </div>
          </div>

          <div class="record-guide">
            <h3>诊疗记录</h3>
            <p v-if="selectedQueue.displayStatus === '已签到'">开始接诊后，将进入诊疗记录页面填写诊断和治疗建议。</p>
            <p v-else-if="selectedQueue.displayStatus === '接诊中'">患者正在接诊，请进入诊疗记录页面完成记录并结束接诊。</p>
            <p v-else>本次接诊已完成，可进入诊疗记录页面查看记录或继续开具处方、检查。</p>
          </div>

          <div class="actions">
            <button
              v-if="canStart(selectedQueue)"
              class="primary-btn"
              :disabled="!!actionId"
              type="button"
              @click="handleStart(selectedQueue)"
            >
              {{ actionId === `start-${selectedQueue.appointmentId}` ? '开始中...' : '开始接诊' }}
            </button>
            <button
              v-else
              class="secondary-btn"
              :disabled="!!actionId"
              type="button"
              @click="openRecord(selectedQueue)"
            >
              {{ selectedQueue.displayStatus === '已完成' ? '查看诊疗记录' : '填写诊疗记录' }}
            </button>
          </div>
        </template>
        <div v-else class="empty-state">请选择左侧患者查看接诊信息</div>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.dashboard-view {
  min-height: 100%;
  padding: 20px 18px 28px;
}

.hero-panel,
.queue-panel,
.detail-panel,
.metric-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}

.hero-panel {
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

.hero-panel h1 {
  margin-top: 4px;
  color: var(--color-ink);
  font-size: 24px;
  font-weight: 800;
}

.hero-desc {
  max-width: 620px;
  margin-top: 8px;
  color: var(--color-ink-3);
  font-size: 13px;
}

.refresh-btn,
.primary-btn,
.secondary-btn {
  min-height: 42px;
  border-radius: var(--radius-sm);
  padding: 0 16px;
  font-weight: 800;
  font-size: 13px;
  transition: border-color 0.18s ease, background 0.18s ease, color 0.18s ease, opacity 0.18s ease;
}

.refresh-btn,
.secondary-btn {
  border: 1.5px solid var(--color-border-strong);
  background: var(--color-surface);
  color: var(--color-primary-deep);
}

.primary-btn {
  border: 1.5px solid var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}

.refresh-btn:disabled,
.primary-btn:disabled,
.secondary-btn:disabled {
  opacity: 0.48;
  cursor: not-allowed;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.metric-card {
  padding: 16px;
}

.metric-card span {
  display: block;
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 700;
}

.metric-card strong {
  display: block;
  margin-top: 6px;
  color: var(--color-ink);
  font-size: 28px;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
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

.notice.success {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.95fr) minmax(420px, 1.35fr);
  gap: 14px;
  margin-top: 14px;
  align-items: start;
}

.queue-panel,
.detail-panel {
  padding: 16px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-head h2 {
  color: var(--color-ink);
  font-size: 17px;
  font-weight: 800;
}

.section-head p {
  margin-top: 3px;
  color: var(--color-ink-3);
  font-size: 12px;
}

.count-badge,
.status-tag {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
}

.count-badge {
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
}

.status-ready {
  background: var(--color-info-soft);
  color: var(--color-info);
}

.status-active {
  background: var(--color-warn-soft);
  color: var(--color-warn);
}

.status-done {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.status-wait {
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
}

.empty-state {
  border: 1px dashed var(--color-border-strong);
  border-radius: var(--radius-sm);
  padding: 28px 16px;
  color: var(--color-ink-3);
  text-align: center;
  font-size: 13px;
}

.queue-item {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  align-items: center;
  width: 100%;
  min-height: 72px;
  margin-top: 10px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 12px;
  background: #fff;
  color: var(--color-ink);
  text-align: left;
}

.queue-item.active {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
}

.queue-no {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 50%;
  background: var(--color-surface);
  color: var(--color-primary-deep);
  font-size: 12px;
  font-weight: 900;
}

.queue-main {
  min-width: 0;
}

.queue-main strong,
.queue-main small {
  display: block;
  overflow-wrap: anywhere;
}

.queue-main strong {
  font-size: 14px;
}

.queue-main small {
  margin-top: 2px;
  color: var(--color-ink-3);
  font-size: 12px;
}

.patient-card {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.patient-card div {
  border-radius: var(--radius-sm);
  background: var(--color-primary-soft);
  padding: 12px;
}

.patient-card span,
.patient-card strong {
  display: block;
}

.patient-card span {
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 700;
}

.patient-card strong {
  margin-top: 4px;
  color: var(--color-ink);
  font-size: 14px;
  overflow-wrap: anywhere;
}

.record-guide {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 14px;
  background: var(--color-primary-soft);
}

.record-guide h3 {
  color: var(--color-ink);
  font-size: 15px;
  font-weight: 800;
}

.record-guide p {
  margin-top: 6px;
  color: var(--color-ink-3);
  font-size: 12.5px;
  line-height: 1.7;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

.actions button {
  flex: 1;
}

@media (max-width: 980px) {
  .overview-grid,
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .hero-panel {
    align-items: stretch;
    flex-direction: column;
  }
}

@media (max-width: 560px) {
  .dashboard-view {
    padding: 16px 12px 24px;
  }

  .hero-panel,
  .queue-panel,
  .detail-panel {
    padding: 14px;
  }

  .patient-card {
    grid-template-columns: 1fr;
  }

  .queue-item {
    grid-template-columns: 38px minmax(0, 1fr);
  }

  .queue-item .status-tag {
    grid-column: 2;
    justify-self: start;
    margin-top: 8px;
  }

  .actions {
    flex-direction: column;
  }
}
</style>
