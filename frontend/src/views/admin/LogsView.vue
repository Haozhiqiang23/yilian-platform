<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { getAdminLogDetail, getAdminLogs } from '@/api/admin'

const loading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const logs = ref([])
const detailLoading = ref(false)
const selectedLog = ref(null)
const closeDetailButton = ref(null)
const detailTrigger = ref(null)

const filters = reactive({
  keyword: '',
  operation: '',
  startDate: '',
  endDate: '',
})

const pager = reactive({
  page: 1,
  size: 10,
  total: 0,
})

const pages = computed(() => Math.max(1, Math.ceil(pager.total / pager.size)))

const summary = computed(() => {
  const list = logs.value
  return {
    total: list.length,
    refund: list.filter((item) => operationText(item).includes('退款')).length,
    doctor: list.filter((item) => operationText(item).includes('医生')).length,
    queue: list.filter((item) => operationText(item).includes('队列') || operationText(item).includes('排队')).length,
  }
})

async function loadLogs() {
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const data = await getAdminLogs({
      keyword: filters.keyword || undefined,
      operation: filters.operation || undefined,
      startDate: filters.startDate || undefined,
      endDate: filters.endDate || undefined,
      page: pager.page,
      size: pager.size,
    })
    logs.value = normalizeResponseList(data)
    pager.total = Array.isArray(data) ? data.length : data?.total || logs.value.length
  } catch (e) {
    errorMsg.value = e.message || '操作日志加载失败，请稍后重试'
    logs.value = []
    pager.total = 0
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

function logIdOf(item, index) {
  return item?.id || item?.logId || `${item?.operator || 'log'}-${item?.time || index}`
}

function operationText(item) {
  const type = item?.operation || item?.operationType || item?.action || item?.content
  const map = {
    DOCTOR_REVIEW: '医生审核',
    DOCTOR_STATUS: '医生状态修改',
    USER_STATUS: '用户状态修改',
    SOURCE_ADJUST: '号源调整',
    SOURCE_STATUS: '号源状态修改',
    REFUND_PROCESS: '退款处理',
    QUEUE_ADJUST: '队列调整',
    CONSULTATION: '接诊操作',
    LOGIN: '登录操作',
  }
  return map[type] || type || '--'
}

function operatorText(item) {
  return item?.operator || item?.operatorName || item?.adminName || '--'
}

function roleText(role) {
  const map = {
    ADMIN: '管理员',
    DOCTOR: '医生',
    PATIENT: '患者',
    SYSTEM: '系统',
  }
  return map[role] || role || '管理员'
}

function resultText(result) {
  const map = {
    SUCCESS: '成功',
    FAIL: '失败',
    FAILED: '失败',
  }
  return map[result] || result || '成功'
}

function resultTone(result) {
  return ['FAIL', 'FAILED', '失败'].includes(result) ? 'danger' : 'success'
}

function targetTypeText(type) {
  const map = {
    DOCTOR: '医生',
    SOURCE: '号源',
    REFUND: '退款申请',
    QUEUE: '排队记录',
    USER: '用户',
  }
  return map[type] || type || '--'
}

function actionText(action) {
  const map = {
    PASS: '审核通过',
    REJECT: '拒绝',
    DISABLE: '禁用',
    RESTORE: '恢复',
    UPDATE: '调整',
    SUSPEND: '停诊',
    REORDER: '调整顺序',
    APPROVE: '确认退款',
  }
  return map[action] || action || '--'
}

function search() {
  pager.page = 1
  loadLogs()
}

function resetFilters() {
  filters.keyword = ''
  filters.operation = ''
  filters.startDate = ''
  filters.endDate = ''
  pager.page = 1
  loadLogs()
}

function prevPage() {
  if (pager.page <= 1) return
  pager.page -= 1
  loadLogs()
}

function nextPage() {
  if (pager.page >= pages.value) return
  pager.page += 1
  loadLogs()
}

async function openDetail(item, event) {
  detailLoading.value = true
  errorMsg.value = ''
  detailTrigger.value = event?.currentTarget || null
  try {
    selectedLog.value = await getAdminLogDetail(item.id || item.logId)
    await nextTick()
    closeDetailButton.value?.focus()
  } catch (e) {
    errorMsg.value = e.message || '操作日志详情加载失败'
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  selectedLog.value = null
  nextTick(() => detailTrigger.value?.focus())
}

function handleEscape(event) {
  if (event.key === 'Escape' && selectedLog.value) closeDetail()
}

onMounted(() => {
  loadLogs()
  document.addEventListener('keydown', handleEscape)
})

onBeforeUnmount(() => document.removeEventListener('keydown', handleEscape))
</script>

<template>
  <div class="logs-view">
    <section class="page-hero" aria-labelledby="logs-title">
      <div>
        <p class="eyebrow">Audit Logs</p>
        <h1 id="logs-title">操作日志</h1>
        <p>查询管理员关键操作记录，用于医生审核、退款处理、状态调整和队列调整的审计追踪。</p>
      </div>
      <button class="refresh-btn" :disabled="loading" type="button" @click="loadLogs">
        {{ loading ? '刷新中' : '刷新列表' }}
      </button>
    </section>

    <section class="summary-grid" aria-label="日志概览">
      <article class="summary-card">
        <span>当前日志</span>
        <strong>{{ summary.total }}</strong>
      </article>
      <article class="summary-card">
        <span>退款相关</span>
        <strong>{{ summary.refund }}</strong>
      </article>
      <article class="summary-card">
        <span>医生审核</span>
        <strong>{{ summary.doctor }}</strong>
      </article>
      <article class="summary-card warn-card">
        <span>队列调整</span>
        <strong>{{ summary.queue }}</strong>
      </article>
    </section>

    <section class="toolbar-panel" aria-label="日志筛选">
      <label>
        <span>关键词</span>
        <input v-model.trim="filters.keyword" type="search" placeholder="操作人 / 操作内容" @keyup.enter="search" />
      </label>
      <label>
        <span>操作类型</span>
        <select v-model="filters.operation">
          <option value="">全部类型</option>
          <option value="DOCTOR_REVIEW">审核医生</option>
          <option value="REFUND_PROCESS">处理退款</option>
          <option value="QUEUE_ADJUST">调整队列</option>
          <option value="STATUS">状态修改</option>
        </select>
      </label>
      <label>
        <span>开始日期</span>
        <input v-model="filters.startDate" type="date" />
      </label>
      <label>
        <span>结束日期</span>
        <input v-model="filters.endDate" type="date" />
      </label>
      <button class="primary-btn" :disabled="loading" type="button" @click="search">查询</button>
      <button class="secondary-btn" :disabled="loading" type="button" @click="resetFilters">重置</button>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>
    <p v-if="successMsg" class="notice success" role="status">{{ successMsg }}</p>

    <section class="table-panel" aria-label="操作日志列表">
      <div v-if="loading" class="empty-state">正在请求操作日志接口...</div>
      <div v-else-if="logs.length === 0" class="empty-state">暂无符合条件的操作日志</div>
      <table v-else>
        <thead>
          <tr>
            <th>日志ID</th>
            <th>操作人</th>
            <th>角色</th>
            <th>操作类型</th>
            <th>操作对象</th>
            <th>结果</th>
            <th>操作内容</th>
            <th>操作时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, index) in logs" :key="logIdOf(item, index)">
            <td>{{ item.id || item.logId || '--' }}</td>
            <td>{{ operatorText(item) }}</td>
            <td>{{ roleText(item.role || item.operatorRole) }}</td>
            <td>{{ operationText(item) }}</td>
            <td>{{ item.target || item.targetName || item.targetId || '--' }}</td>
            <td>
              <span class="status-tag" :class="`status-${resultTone(item.result || item.status)}`">
                {{ resultText(item.result || item.status) }}
              </span>
            </td>
            <td class="content-cell" :title="item.reason || item.content || item.detail || ''">
              {{ item.reason || item.content || item.detail || '--' }}
            </td>
            <td>{{ item.time || item.createTime || item.createdAt || '--' }}</td>
            <td>
              <button class="detail-btn" :disabled="detailLoading" type="button" @click="openDetail(item, $event)">
                查看详情
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <div class="pager" aria-label="分页">
      <button :disabled="pager.page <= 1 || loading" type="button" @click="prevPage">上一页</button>
      <span>第 {{ pager.page }} / {{ pages }} 页 · 共 {{ pager.total }} 条</span>
      <button :disabled="pager.page >= pages || loading" type="button" @click="nextPage">下一页</button>
    </div>

    <div v-if="selectedLog" class="modal-mask" role="presentation" @click.self="closeDetail">
      <section
        class="detail-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="log-detail-title"
        @keydown.tab.prevent="closeDetailButton?.focus()"
      >
        <div class="detail-header">
          <div>
            <p class="eyebrow">Audit Detail</p>
            <h2 id="log-detail-title">操作日志详情</h2>
          </div>
          <button ref="closeDetailButton" class="close-btn" type="button" aria-label="关闭操作日志详情" @click="closeDetail">
            关闭
          </button>
        </div>

        <div class="business-summary">
          <span>{{ targetTypeText(selectedLog.targetType) }}</span>
          <strong>{{ selectedLog.targetName || selectedLog.target || '未找到业务对象' }}</strong>
          <p>{{ selectedLog.businessInfo || selectedLog.target || '暂无关联业务信息' }}</p>
        </div>

        <dl class="detail-grid">
          <div><dt>日志 ID</dt><dd class="id-value">{{ selectedLog.id }}</dd></div>
          <div><dt>操作时间</dt><dd>{{ selectedLog.createTime || '--' }}</dd></div>
          <div><dt>操作人</dt><dd>{{ operatorText(selectedLog) }}</dd></div>
          <div><dt>操作角色</dt><dd>{{ roleText(selectedLog.operatorRole) }}</dd></div>
          <div><dt>操作类型</dt><dd>{{ operationText(selectedLog) }}</dd></div>
          <div><dt>具体动作</dt><dd>{{ actionText(selectedLog.action) }}</dd></div>
          <div v-if="selectedLog.patientName"><dt>患者</dt><dd>{{ selectedLog.patientName }}</dd></div>
          <div v-if="selectedLog.doctorName"><dt>医生</dt><dd>{{ selectedLog.doctorName }}</dd></div>
          <div v-if="selectedLog.appointmentId"><dt>预约 ID</dt><dd class="id-value">{{ selectedLog.appointmentId }}</dd></div>
          <div><dt>目标类型</dt><dd>{{ targetTypeText(selectedLog.targetType) }}</dd></div>
          <div><dt>目标 ID</dt><dd class="id-value">{{ selectedLog.targetId || '--' }}</dd></div>
          <div class="detail-wide"><dt>操作原因</dt><dd>{{ selectedLog.reason || '未填写原因' }}</dd></div>
          <div><dt>处理结果</dt><dd>{{ resultText(selectedLog.result) }}</dd></div>
        </dl>
      </section>
    </div>
  </div>
</template>

<style scoped>
.logs-view {
  min-height: 100%;
}

.page-hero,
.summary-card,
.toolbar-panel,
.table-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}

.page-hero {
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

.page-hero h1 {
  margin-top: 4px;
  color: var(--color-ink);
  font-size: 24px;
  font-weight: 800;
}

.page-hero p {
  max-width: 720px;
  margin-top: 8px;
  color: var(--color-ink-3);
  font-size: 13px;
}

.refresh-btn,
.primary-btn,
.secondary-btn,
.detail-btn,
.close-btn {
  min-height: 40px;
  border-radius: var(--radius-sm);
  padding: 0 13px;
  font-size: 13px;
  font-weight: 800;
}

.refresh-btn,
.primary-btn {
  border: 1.5px solid var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}

.secondary-btn {
  border: 1.5px solid var(--color-border-strong);
  background: #fff;
  color: var(--color-primary-deep);
}

.detail-btn,
.close-btn {
  border: 1.5px solid var(--color-border-strong);
  background: #fff;
  color: var(--color-primary-deep);
}

.detail-btn:focus-visible,
.close-btn:focus-visible {
  outline: 3px solid rgba(14, 143, 134, 0.24);
  outline-offset: 2px;
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

.warn-card strong {
  color: var(--color-warn);
}

.toolbar-panel {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 150px 150px 150px auto auto;
  gap: 10px;
  align-items: end;
  margin-top: 14px;
  padding: 14px;
}

.toolbar-panel label span {
  display: block;
  margin-bottom: 6px;
  color: var(--color-ink-2);
  font-size: 13px;
  font-weight: 800;
}

.toolbar-panel input,
.toolbar-panel select {
  width: 100%;
  min-height: 40px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0 12px;
  color: var(--color-ink);
  outline: none;
}

.toolbar-panel input:focus,
.toolbar-panel select:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(14, 143, 134, 0.12);
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

.table-panel {
  margin-top: 14px;
  overflow: auto;
}

.empty-state {
  margin: 16px;
  border: 1px dashed var(--color-border-strong);
  border-radius: var(--radius-sm);
  padding: 32px 16px;
  color: var(--color-ink-3);
  text-align: center;
  font-size: 13px;
}

table {
  width: 100%;
  min-width: 1080px;
  border-collapse: collapse;
}

th,
td {
  border-bottom: 1px solid var(--color-border);
  padding: 12px 14px;
  color: var(--color-ink-2);
  text-align: left;
  font-size: 13px;
  white-space: nowrap;
}

th {
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
  font-weight: 900;
}

tr:last-child td {
  border-bottom: none;
}

.content-cell {
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-tag {
  display: inline-flex;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 800;
}

.status-success {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.status-danger {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}

.pager button {
  min-height: 38px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0 13px;
  background: var(--color-surface);
  color: var(--color-ink-2);
  font-weight: 800;
}

.pager span {
  min-width: 160px;
  color: var(--color-ink-3);
  text-align: center;
  font-size: 12.5px;
}

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: grid;
  place-items: center;
  padding: 18px;
  background: rgba(11, 78, 74, 0.32);
}

.detail-panel {
  width: min(720px, 100%);
  max-height: min(760px, calc(100dvh - 36px));
  overflow: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 20px;
  background: var(--color-surface);
  box-shadow: 0 24px 64px rgba(11, 78, 74, 0.22);
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.detail-header h2 {
  margin-top: 4px;
  color: var(--color-ink);
  font-size: 20px;
}

.business-summary {
  margin-top: 18px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
  padding: 16px;
  background: var(--color-primary-soft);
}

.business-summary span {
  display: inline-flex;
  border-radius: 999px;
  padding: 4px 9px;
  background: var(--color-surface);
  color: var(--color-primary-deep);
  font-size: 12px;
  font-weight: 800;
}

.business-summary strong {
  display: block;
  margin-top: 10px;
  color: var(--color-ink);
  font-size: 18px;
  line-height: 1.45;
}

.business-summary p {
  margin-top: 6px;
  color: var(--color-ink-3);
  font-size: 13px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 18px;
}

.detail-grid > div {
  min-width: 0;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 12px;
  background: var(--color-primary-soft);
}

.detail-grid .detail-wide {
  grid-column: 1 / -1;
}

.detail-grid dt {
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 800;
}

.detail-grid dd {
  margin: 6px 0 0;
  color: var(--color-ink);
  font-size: 13px;
  font-weight: 700;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.detail-grid .id-value {
  font-variant-numeric: tabular-nums;
}

@media (max-width: 1180px) {
  .toolbar-panel {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .page-hero {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-panel {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 620px) {
  .summary-grid,
  .toolbar-panel {
    grid-template-columns: 1fr;
  }

  .page-hero,
  .toolbar-panel {
    padding: 14px;
  }

  .pager {
    flex-direction: column;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-grid .detail-wide {
    grid-column: auto;
  }
}
</style>
