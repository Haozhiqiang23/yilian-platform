<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getDoctorQueues, getPatientRecords, startAppointment } from '@/api/doctor'
import ListPager from '@/components/common/ListPager.vue'

const router = useRouter()
const loading = ref(false)
const actionId = ref('')
const errorMsg = ref('')
const successMsg = ref('')
const keyword = ref('')
const statusFilter = ref('全部')
const queues = ref([])
const history = ref(null)
const historyPatient = ref(null)
const page = ref(1)
const pageSize = 10
const historyPage = ref(1)
const historyPageSize = 10

const statusOptions = ['全部', '已签到', '接诊中', '已完成']

const statusMeta = {
  已签到: { text: '已签到', tone: 'ready' },
  接诊中: { text: '接诊中', tone: 'active' },
  已完成: { text: '已完成', tone: 'done' },
}

const normalizedQueues = computed(() =>
  queues.value.map((item, index) => {
    const appointmentId = appointmentIdOf(item)
    const status = item.status || item.queueStatus || item.appointmentStatus || '状态待确认'
    return {
      ...item,
      appointmentId,
      displayStatus: status,
      sortNo: item.queueNo || item.queueNumber || String(index + 1).padStart(2, '0'),
      appointmentTime: item.appointmentTime || item.scheduleTime || item.visitTime || '',
    }
  }),
)

const filteredQueues = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return normalizedQueues.value.filter((item) => {
    const matchedStatus = statusFilter.value === '全部' || item.displayStatus === statusFilter.value
    const searchText = [item.patientName, item.patientId, item.appointmentId, item.sortNo]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    return matchedStatus && (!word || searchText.includes(word))
  })
})
const pagedQueues = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredQueues.value.slice(start, start + pageSize)
})
const pagedHistory = computed(() => {
  const start = (historyPage.value - 1) * historyPageSize
  return (history.value || []).slice(start, start + historyPageSize)
})

watch([keyword, statusFilter], () => { page.value = 1 })

const overview = computed(() => ({
  total: normalizedQueues.value.length,
  ready: normalizedQueues.value.filter((item) => item.displayStatus === '已签到').length,
  active: normalizedQueues.value.filter((item) => item.displayStatus === '接诊中').length,
  done: normalizedQueues.value.filter((item) => item.displayStatus === '已完成').length,
}))

async function loadQueues() {
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const data = await getDoctorQueues()
    queues.value = normalizeResponseList(data)
  } catch (e) {
    errorMsg.value = e.message || '暂时无法加载患者列表，请稍后重试'
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
  return item.displayStatus === '已签到'
}

function canContinue(item) {
  return item.displayStatus === '接诊中'
}

function openRecordEditor(item) {
  router.push(`/doctor/records/${item.appointmentId}`)
}

async function handleStart(item) {
  if (!canStart(item) || actionId.value) return
  actionId.value = `start-${item.appointmentId}`
  errorMsg.value = ''
  successMsg.value = ''
  try {
    await startAppointment(item.appointmentId)
    successMsg.value = `已为 ${item.patientName || '当前患者'} 开始接诊`
    await loadQueues()
  } catch (e) {
    errorMsg.value = e.message || '开始接诊失败，请稍后重试'
  } finally {
    actionId.value = ''
  }
}

async function showHistory(item) {
  if (!item.patientId || actionId.value) return
  actionId.value = `history-${item.appointmentId}`
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const data = await getPatientRecords(item.patientId)
    history.value = normalizeResponseList(data)
    historyPage.value = 1
    historyPatient.value = item
  } catch (e) {
    errorMsg.value = e.message || '暂时无法加载患者历史记录，请稍后重试'
  } finally {
    actionId.value = ''
  }
}

function closeHistory() {
  history.value = null
  historyPatient.value = null
}

onMounted(loadQueues)
</script>

<template>
  <div class="queues-view">
    <section class="page-hero" aria-labelledby="queues-title">
      <div>
        <p class="eyebrow">Queue Management</p>
        <h1 id="queues-title">患者列表 / 排队页</h1>
        <p>这里只显示当前医生的预约患者，可按姓名、患者 ID 或预约号查询。</p>
      </div>
      <button class="refresh-btn" :disabled="loading" type="button" @click="loadQueues">
        {{ loading ? '刷新中' : '刷新列表' }}
      </button>
    </section>

    <section class="summary-grid" aria-label="队列概览">
      <article class="summary-card">
        <span>全部患者</span>
        <strong>{{ overview.total }}</strong>
      </article>
      <article class="summary-card">
        <span>已签到</span>
        <strong>{{ overview.ready }}</strong>
      </article>
      <article class="summary-card">
        <span>接诊中</span>
        <strong>{{ overview.active }}</strong>
      </article>
      <article class="summary-card">
        <span>已完成</span>
        <strong>{{ overview.done }}</strong>
      </article>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>
    <p v-if="successMsg" class="notice success" role="status">{{ successMsg }}</p>

    <section class="toolbar" aria-label="队列筛选">
      <label class="search-field">
        <span>搜索患者</span>
        <input v-model="keyword" type="search" placeholder="姓名 / 患者ID / 预约ID / 排队号" />
      </label>
      <div class="status-tabs" role="tablist" aria-label="状态筛选">
        <button
          v-for="status in statusOptions"
          :key="status"
          :class="{ active: statusFilter === status }"
          type="button"
          @click="statusFilter = status"
        >
          {{ status }}
        </button>
      </div>
    </section>

    <section v-if="history" class="history-panel" aria-labelledby="history-title">
      <div class="panel-head">
        <div>
          <h2 id="history-title">{{ historyPatient?.patientName || '患者' }}历史记录</h2>
          <p>查看该患者过往的诊疗记录</p>
        </div>
        <button type="button" @click="closeHistory">关闭</button>
      </div>

      <div v-if="history.length === 0" class="empty-state">该患者暂无历史诊疗记录</div>
      <div v-else class="history-list">
        <article v-for="record in pagedHistory" :key="record.id || record.recordId || record.medicalRecordId" class="history-card">
          <div>
            <strong>{{ record.diagnosis || '未记录诊断结果' }}</strong>
            <span>{{ record.visitTime || record.createTime || record.createdAt || '--' }}</span>
          </div>
          <p>{{ record.treatment || record.advice || record.description || '未记录治疗建议' }}</p>
        </article>
      </div>
      <ListPager v-model:page="historyPage" :page-size="historyPageSize" :total="history.length" />
    </section>

    <section class="queue-table" aria-label="患者排队列表">
      <div class="table-head">
        <span>排队号</span>
        <span>患者</span>
        <span>预约时间</span>
        <span>状态</span>
        <span>操作</span>
      </div>

      <div v-if="loading" class="empty-state">正在加载患者列表...</div>
      <div v-else-if="filteredQueues.length === 0" class="empty-state">没有找到匹配的患者</div>

      <article v-for="item in pagedQueues" v-else :key="item.appointmentId" class="queue-row">
        <div class="queue-no">{{ item.sortNo }}</div>
        <div class="patient-info">
          <strong>{{ item.patientName || '患者信息未提供' }}</strong>
          <span>患者ID：{{ item.patientId || '--' }}</span>
        </div>
        <div class="time-info">
          <strong>{{ item.appointmentTime || '--' }}</strong>
          <span>签到：{{ item.signInTime || '--' }}</span>
        </div>
        <div>
          <span class="status-tag" :class="`status-${statusTone(item.displayStatus)}`">
            {{ statusMeta[item.displayStatus]?.text || item.displayStatus }}
          </span>
        </div>
        <div class="row-actions">
          <button class="record-btn" :disabled="!item.appointmentId" type="button" @click="openRecordEditor(item)">
            填写记录
          </button>
          <button
            class="primary-btn"
            :disabled="!!actionId || !canStart(item)"
            type="button"
            @click="handleStart(item)"
          >
            {{ actionId === `start-${item.appointmentId}` ? '开始中...' : '开始接诊' }}
          </button>
          <button class="secondary-btn" :disabled="!canContinue(item)" type="button">
            {{ canContinue(item) ? '继续接诊' : '等待接诊' }}
          </button>
          <button
            class="ghost-btn"
            :disabled="!!actionId || !item.patientId"
            type="button"
            @click="showHistory(item)"
          >
            {{ actionId === `history-${item.appointmentId}` ? '查询中...' : '历史记录' }}
          </button>
        </div>
      </article>
      <ListPager v-model:page="page" :page-size="pageSize" :total="filteredQueues.length" :disabled="loading" />
    </section>
  </div>
</template>

<style scoped>
.queues-view {
  min-height: 100%;
  padding: 20px 18px 28px;
}

.page-hero,
.summary-card,
.toolbar,
.history-panel,
.queue-table {
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}

.page-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px;
}

.eyebrow {
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 800;
}

.page-hero h1 {
  margin-top: 4px;
  color: var(--color-ink);
  font-size: 24px;
  font-weight: 800;
}

.page-hero p {
  margin-top: 8px;
  color: var(--color-ink-3);
  font-size: 13px;
}

.refresh-btn,
.primary-btn,
.secondary-btn,
.ghost-btn,
.record-btn,
.history-panel button,
.status-tabs button {
  min-height: 40px;
  border-radius: var(--radius-sm);
  padding: 0 13px;
  font-size: 13px;
  font-weight: 800;
  transition: border-color 0.18s ease, background 0.18s ease, color 0.18s ease, opacity 0.18s ease;
}

.refresh-btn,
.secondary-btn,
.ghost-btn,
.history-panel button {
  border: 1.5px solid var(--color-border-strong);
  background: #fff;
  color: var(--color-primary-deep);
}

.primary-btn {
  border: 1.5px solid var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}

.record-btn {
  border: 1.5px solid var(--color-border-strong);
  background: #fff;
  color: var(--color-primary-deep);
}

button:disabled {
  opacity: 0.48;
  cursor: not-allowed;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.summary-card {
  padding: 16px;
}

.summary-card span {
  display: block;
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 700;
}

.summary-card strong {
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

.toolbar {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 14px;
  margin-top: 14px;
  padding: 14px;
}

.search-field {
  flex: 1;
  min-width: 260px;
}

.search-field span {
  display: block;
  margin-bottom: 6px;
  color: var(--color-ink-2);
  font-size: 13px;
  font-weight: 800;
}

.search-field input {
  width: 100%;
  min-height: 42px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0 12px;
  color: var(--color-ink);
  outline: none;
}

.search-field input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(14, 143, 134, 0.12);
}

.status-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.status-tabs button {
  border: 1.5px solid var(--color-border);
  background: #fff;
  color: var(--color-ink-2);
}

.status-tabs button.active {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
}

.history-panel {
  margin-top: 14px;
  padding: 16px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
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

.history-list {
  display: grid;
  gap: 10px;
}

.history-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 12px;
  background: var(--color-primary-soft);
}

.history-card div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.history-card strong {
  color: var(--color-ink);
  font-size: 14px;
}

.history-card span,
.history-card p {
  color: var(--color-ink-3);
  font-size: 12px;
}

.history-card p {
  margin-top: 8px;
  overflow-wrap: anywhere;
}

.queue-table {
  margin-top: 14px;
  overflow: hidden;
}

.table-head,
.queue-row {
  display: grid;
  grid-template-columns: 90px minmax(160px, 1fr) minmax(170px, 1fr) 110px minmax(300px, 1.4fr);
  gap: 12px;
  align-items: center;
}

.table-head {
  min-height: 48px;
  padding: 0 16px;
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
  font-size: 12px;
  font-weight: 900;
}

.queue-row {
  padding: 14px 16px;
  border-top: 1px solid var(--color-border);
  background: #fff;
}

.queue-no {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 50%;
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}

.patient-info strong,
.patient-info span,
.time-info strong,
.time-info span {
  display: block;
  overflow-wrap: anywhere;
}

.patient-info strong,
.time-info strong {
  color: var(--color-ink);
  font-size: 14px;
}

.patient-info span,
.time-info span {
  margin-top: 3px;
  color: var(--color-ink-3);
  font-size: 12px;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
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

.row-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.empty-state {
  margin: 16px;
  border: 1px dashed var(--color-border-strong);
  border-radius: var(--radius-sm);
  padding: 30px 16px;
  color: var(--color-ink-3);
  text-align: center;
  font-size: 13px;
}

@media (max-width: 1120px) {
  .table-head {
    display: none;
  }

  .queue-row {
    grid-template-columns: 54px minmax(0, 1fr);
    align-items: start;
  }

  .time-info,
  .queue-row > div:nth-child(4),
  .row-actions {
    grid-column: 2;
  }

  .row-actions {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .page-hero,
  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-field {
    min-width: 0;
  }
}

@media (max-width: 520px) {
  .queues-view {
    padding: 16px 12px 24px;
  }

  .page-hero,
  .toolbar,
  .history-panel {
    padding: 14px;
  }

  .summary-grid,
  .row-actions {
    grid-template-columns: 1fr;
  }

  .queue-row {
    grid-template-columns: 1fr;
  }

  .time-info,
  .queue-row > div:nth-child(4),
  .row-actions {
    grid-column: auto;
  }
}
</style>
