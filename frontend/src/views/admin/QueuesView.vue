<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { adjustAdminQueue, getAdminQueues } from '@/api/admin'

const loading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const queues = ref([])
const pendingAdjust = ref(null)
const adjusting = ref(false)

function todayText() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const pager = reactive({
  page: 1,
  size: 10,
  total: 0,
})

const filters = reactive({
  doctorName: '',
  date: todayText(),
  status: '',
})

const adjustForm = reactive({
  position: '',
  reason: '',
})

const adjustErrors = reactive({
  position: '',
  reason: '',
})

const summary = computed(() => ({
  total: pager.total,
  waiting: queues.value.filter((item) => statusText(item.status) === '等待接诊').length,
  active: queues.value.filter((item) => statusText(item.status) === '接诊中').length,
  adjusted: queues.value.filter((item) => item.adjusted).length,
}))

const pages = computed(() => Math.max(1, Math.ceil(pager.total / pager.size)))

function queueIdOf(item) {
  return item?.queueId || item?.id || item?.appointmentId || ''
}

function statusText(status) {
  const map = {
    WAITING: '等待接诊',
    SIGNED: '已签到',
    PROCESSING: '接诊中',
    DONE: '已完成',
    COMPLETED: '已完成',
  }
  return map[status] || status || '等待接诊'
}

function statusTone(status) {
  const text = statusText(status)
  if (text === '已完成') return 'success'
  if (text === '接诊中') return 'info'
  return 'warning'
}

async function requestQueues() {
  loading.value = true
  successMsg.value = ''
  errorMsg.value = ''
  try {
    const data = await getAdminQueues({
      doctorName: filters.doctorName || undefined,
      date: filters.date || undefined,
      status: filters.status || undefined,
      page: pager.page,
      size: pager.size,
    })
    queues.value = Array.isArray(data?.records) ? data.records : []
    pager.total = Number(data?.total || 0)
  } catch (e) {
    queues.value = []
    pager.total = 0
    errorMsg.value = e.message || '管理员排队列表加载失败'
  } finally {
    loading.value = false
  }
}

function search() {
  pager.page = 1
  requestQueues()
}

function prevPage() {
  if (pager.page <= 1 || loading.value) return
  pager.page -= 1
  requestQueues()
}

function nextPage() {
  if (pager.page >= pages.value || loading.value) return
  pager.page += 1
  requestQueues()
}

function canAdjust(item) {
  return statusText(item?.status) === '等待接诊' && item?.visitDate === todayText()
}

function adjustBlockedReason(item) {
  if (statusText(item?.status) !== '等待接诊') return '只有等待接诊的队列可以调整顺序'
  if (item?.visitDate !== todayText()) return '历史队列仅供查看，不能调整顺序'
  return ''
}

function openAdjust(item) {
  pendingAdjust.value = item
  adjustForm.position = String(item.position || item.rank || '')
  adjustForm.reason = ''
  adjustErrors.position = ''
  adjustErrors.reason = ''
  errorMsg.value = ''
  successMsg.value = ''
}

function closeAdjust() {
  pendingAdjust.value = null
}

function validateAdjust() {
  adjustErrors.position = ''
  adjustErrors.reason = ''
  const position = Number(adjustForm.position)
  if (!Number.isInteger(position) || position <= 0) {
    adjustErrors.position = '请输入大于 0 的整数位置'
  }
  if (!adjustForm.reason.trim()) {
    adjustErrors.reason = '调整原因不能为空'
  }
  return !adjustErrors.position && !adjustErrors.reason
}

async function submitAdjust() {
  if (!validateAdjust()) return
  const queueId = queueIdOf(pendingAdjust.value)
  adjusting.value = true
  successMsg.value = ''
  errorMsg.value = ''
  try {
    const result = await adjustAdminQueue(queueId, {
      position: Number(adjustForm.position),
      reason: adjustForm.reason.trim(),
    })
    pendingAdjust.value = null
    successMsg.value = `队列 ${queueId} 已从第 ${result.oldPosition} 位调整到第 ${result.newPosition} 位`
    await requestQueues()
  } catch (e) {
    errorMsg.value = e.message || '调整队列失败'
  } finally {
    adjusting.value = false
  }
}

onMounted(requestQueues)
</script>

<template>
  <div class="queues-view">
    <section class="page-hero" aria-labelledby="queues-title">
      <div>
        <p class="eyebrow">Queue Administration</p>
        <h1 id="queues-title">排队管理</h1>
        <p>管理员查看各医生队列并处理异常调序；调整原因必填，所有操作写入日志审计。</p>
      </div>
      <button class="refresh-btn" :disabled="loading" type="button" @click="requestQueues">
        {{ loading ? '刷新中' : '刷新列表' }}
      </button>
    </section>

    <section class="summary-grid" aria-label="排队概览">
      <article class="summary-card">
        <span>队列记录</span>
        <strong>{{ summary.total }}</strong>
      </article>
      <article class="summary-card">
        <span>等待中</span>
        <strong>{{ summary.waiting }}</strong>
      </article>
      <article class="summary-card">
        <span>接诊中</span>
        <strong>{{ summary.active }}</strong>
      </article>
      <article class="summary-card warn-card">
        <span>已调整</span>
        <strong>{{ summary.adjusted }}</strong>
      </article>
    </section>

    <section class="toolbar-panel" aria-label="排队筛选">
      <label>
        <span>医生姓名</span>
        <input v-model.trim="filters.doctorName" type="search" placeholder="输入医生姓名" @keyup.enter="search" />
      </label>
      <label>
        <span>日期</span>
        <input v-model="filters.date" type="date" />
      </label>
      <label>
        <span>状态</span>
        <select v-model="filters.status">
          <option value="">全部状态</option>
          <option value="WAITING">等待中</option>
          <option value="PROCESSING">接诊中</option>
          <option value="DONE">已完成</option>
        </select>
      </label>
      <button class="primary-btn" :disabled="loading" type="button" @click="search">查询</button>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>
    <p v-if="successMsg" class="notice success" role="status">{{ successMsg }}</p>

    <section class="table-panel" aria-label="排队列表">
      <div v-if="loading" class="empty-state">正在请求管理员排队接口...</div>
      <div v-else-if="queues.length === 0" class="empty-state">暂无符合条件的排队记录</div>
      <table v-else>
        <thead>
          <tr>
            <th>队列ID</th>
            <th>医生</th>
            <th>患者</th>
            <th>排队号</th>
            <th>当前顺序</th>
            <th>状态</th>
            <th>签到时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in queues" :key="queueIdOf(item)">
            <td>{{ queueIdOf(item) || '--' }}</td>
            <td>{{ item.doctorName || '--' }}</td>
            <td>{{ item.patientName || '--' }}</td>
            <td>{{ item.queueNo || '--' }}</td>
            <td class="number-cell">{{ item.position || item.rank || '--' }}</td>
            <td>
              <span class="status-tag" :class="`status-${statusTone(item.status)}`">
                {{ statusText(item.status) }}
              </span>
            </td>
            <td>{{ item.signInTime || '--' }}</td>
            <td>
              <button
                class="link-btn"
                :disabled="!canAdjust(item)"
                :title="adjustBlockedReason(item)"
                type="button"
                @click="openAdjust(item)"
              >
                异常调序
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

    <div v-if="pendingAdjust" class="modal-mask" role="presentation">
      <section class="confirm-panel" role="dialog" aria-modal="true" aria-labelledby="adjust-title">
        <h2 id="adjust-title">异常调整排队顺序</h2>
        <p>
          队列
          <strong>{{ queueIdOf(pendingAdjust) || '--' }}</strong>
          将调整到新位置，提交后需要后端记录管理员、时间和原因。
        </p>
        <label>
          <span>新位置</span>
          <input v-model="adjustForm.position" type="number" min="1" inputmode="numeric" :aria-invalid="!!adjustErrors.position" />
        </label>
        <small v-if="adjustErrors.position" class="field-error">{{ adjustErrors.position }}</small>
        <label>
          <span>调整原因</span>
          <textarea v-model.trim="adjustForm.reason" rows="4" placeholder="必填，用于审计日志" :aria-invalid="!!adjustErrors.reason"></textarea>
        </label>
        <small v-if="adjustErrors.reason" class="field-error">{{ adjustErrors.reason }}</small>
        <div class="confirm-actions">
          <button class="secondary-btn" :disabled="adjusting" type="button" @click="closeAdjust">取消</button>
          <button class="primary-btn" :disabled="adjusting" type="button" @click="submitAdjust">
            {{ adjusting ? '调整中...' : '提交调整' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.queues-view {
  min-height: 100%;
}

.page-hero,
.summary-card,
.toolbar-panel,
.table-panel,
.wait-panel,
.confirm-panel {
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
.link-btn {
  min-height: 40px;
  border-radius: var(--radius-sm);
  padding: 0 13px;
  font-size: 13px;
  font-weight: 800;
}

.refresh-btn,
.primary-btn,
.link-btn {
  border: 1.5px solid var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}

.secondary-btn {
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

.warn-card strong {
  color: var(--color-warn);
}

.toolbar-panel {
  display: grid;
  grid-template-columns: minmax(200px, 1fr) 170px 160px auto;
  gap: 10px;
  align-items: end;
  margin-top: 14px;
  padding: 14px;
}

.toolbar-panel label span,
.confirm-panel label span {
  display: block;
  margin-bottom: 6px;
  color: var(--color-ink-2);
  font-size: 13px;
  font-weight: 800;
}

.toolbar-panel input,
.toolbar-panel select,
.confirm-panel input,
.confirm-panel textarea {
  width: 100%;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0 12px;
  color: var(--color-ink);
  outline: none;
}

.toolbar-panel input,
.toolbar-panel select,
.confirm-panel input {
  min-height: 40px;
}

.confirm-panel textarea {
  min-height: 96px;
  padding-top: 10px;
  resize: vertical;
}

.toolbar-panel input:focus,
.toolbar-panel select:focus,
.confirm-panel input:focus,
.confirm-panel textarea:focus {
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
  min-width: 980px;
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

.number-cell {
  font-variant-numeric: tabular-nums;
}

.status-tag {
  display: inline-flex;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 800;
}

.status-warning {
  background: var(--color-warn-soft);
  color: var(--color-warn);
}

.status-info {
  background: var(--color-info-soft);
  color: var(--color-info);
}

.status-success {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.wait-panel {
  margin-top: 14px;
  padding: 16px;
}

.wait-panel h2 {
  color: var(--color-ink);
  font-size: 18px;
  font-weight: 800;
}

.wait-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.wait-grid.single-wait {
  grid-template-columns: 1fr;
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

.wait-grid article {
  border: 1px dashed var(--color-border-strong);
  border-radius: var(--radius-sm);
  padding: 13px;
  background: var(--color-primary-soft);
}

.wait-grid span {
  display: block;
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 800;
}

.wait-grid strong {
  display: block;
  margin-top: 5px;
  color: var(--color-primary-deep);
  font-size: 14px;
}

.wait-grid p {
  margin-top: 7px;
  color: var(--color-ink-3);
  font-size: 12.5px;
}

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: grid;
  place-items: center;
  padding: 18px;
  background: rgba(11, 78, 74, 0.28);
}

.confirm-panel {
  width: min(460px, 100%);
  padding: 18px;
}

.confirm-panel h2 {
  color: var(--color-ink);
  font-size: 18px;
  font-weight: 800;
}

.confirm-panel p {
  margin-top: 10px;
  color: var(--color-ink-3);
  font-size: 13px;
}

.confirm-panel strong {
  color: var(--color-ink);
}

.confirm-panel label {
  display: block;
  margin-top: 14px;
}

.field-error {
  display: block;
  margin-top: 7px;
  color: var(--color-danger);
  font-size: 12.5px;
  font-weight: 700;
}

.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

@media (max-width: 980px) {
  .summary-grid,
  .wait-grid {
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
  .toolbar-panel,
  .wait-grid {
    grid-template-columns: 1fr;
  }

  .page-hero,
  .toolbar-panel {
    padding: 14px;
  }

  .confirm-actions {
    flex-direction: column;
  }

  .pager {
    flex-direction: column;
  }
}
</style>
