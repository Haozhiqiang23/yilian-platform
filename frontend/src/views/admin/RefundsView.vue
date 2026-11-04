<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getRefunds, handleRefund } from '@/api/admin'

const loading = ref(false)
const actionId = ref('')
const errorMsg = ref('')
const successMsg = ref('')
const refunds = ref([])
const pendingRefund = ref(null)
const processAction = ref('approve')

const filters = reactive({
  status: '',
  keyword: '',
})

const pager = reactive({
  page: 1,
  size: 10,
  total: 0,
})

const processForm = reactive({
  note: '',
})

const pages = computed(() => Math.max(1, Math.ceil(pager.total / pager.size)))

const summary = computed(() => ({
  total: refunds.value.length,
  applying: refunds.value.filter((item) => statusText(item.status) === '退款申请').length,
  success: refunds.value.filter((item) => statusText(item.status) === '退款成功').length,
  amount: refunds.value.reduce((sum, item) => sum + Number(item.amount || 0), 0).toFixed(2),
}))

async function loadRefunds() {
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const data = await getRefunds({
      status: filters.status || undefined,
      keyword: filters.keyword || undefined,
      page: pager.page,
      size: pager.size,
    })
    refunds.value = normalizeResponseList(data)
    pager.total = Array.isArray(data) ? data.length : data?.total || refunds.value.length
  } catch (e) {
    errorMsg.value = e.message || '退款记录加载失败，请稍后重试'
    refunds.value = []
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

function refundIdOf(item) {
  return item?.refundId || item?.id || ''
}

function statusText(status) {
  const map = {
    APPLYING: '退款申请',
    PENDING: '退款申请',
    PROCESSING: '退款处理中',
    SUCCESS: '退款成功',
    DONE: '退款成功',
    REFUNDED: '退款成功',
    REJECTED: '已拒绝',
    退款拒绝: '已拒绝',
  }
  return map[status] || status || '退款申请'
}

function statusTone(status) {
  const text = statusText(status)
  if (text === '退款成功') return 'success'
  if (text === '已拒绝') return 'danger'
  if (text === '退款处理中') return 'info'
  return 'warning'
}

function canProcess(item) {
  return statusText(item?.status) !== '退款成功' && statusText(item?.status) !== '已拒绝'
}

function canReject(item) {
  if (!canProcess(item)) return false

  // 新版接口返回布尔值；同时兼容 MySQL/MyBatis 可能返回的 1 或字符串 true。
  if (item?.canReject !== undefined && item?.canReject !== null) {
    return item.canReject === true || item.canReject === 1 || item.canReject === '1' || item.canReject === 'true'
  }

  // 兼容尚未重启的旧版后端：普通退款申请可以拒绝，医生停诊强制退款不能拒绝。
  return !String(item?.reason || '').startsWith('医生停诊：')
}

function rejectBlockedText(item) {
  if (item?.rejectBlockedReason) return item.rejectBlockedReason
  if (String(item?.reason || '').startsWith('医生停诊：')) return '医生停诊产生的强制退款不能拒绝'
  return '当前退款不能拒绝'
}

function search() {
  pager.page = 1
  loadRefunds()
}

function prevPage() {
  if (pager.page <= 1) return
  pager.page -= 1
  loadRefunds()
}

function nextPage() {
  if (pager.page >= pages.value) return
  pager.page += 1
  loadRefunds()
}

function openProcessConfirm(item, action) {
  pendingRefund.value = item
  processAction.value = action
  processForm.note = ''
  errorMsg.value = ''
  successMsg.value = ''
}

function closeProcessConfirm() {
  if (actionId.value) return
  pendingRefund.value = null
}

async function confirmProcess() {
  if (!pendingRefund.value || actionId.value) return
  const id = refundIdOf(pendingRefund.value)
  if (!id) {
    errorMsg.value = '当前退款申请信息不完整，请刷新列表后重试'
    return
  }
  if (processAction.value === 'reject' && !processForm.note) {
    errorMsg.value = '拒绝退款时必须填写拒绝理由'
    return
  }
  actionId.value = String(id)
  errorMsg.value = ''
  successMsg.value = ''
  try {
    await handleRefund(id, {
      status: processAction.value === 'approve' ? '退款成功' : '退款拒绝',
      note: processForm.note || undefined,
    })
    successMsg.value = processAction.value === 'approve'
      ? `退款申请 ${id} 已确认成功`
      : `退款申请 ${id} 已拒绝`
    pendingRefund.value = null
    await loadRefunds()
  } catch (e) {
    errorMsg.value = e.message || '退款处理失败，请稍后重试'
  } finally {
    actionId.value = ''
  }
}

onMounted(loadRefunds)
</script>

<template>
  <div class="refunds-view">
    <section class="page-hero" aria-labelledby="refunds-title">
      <div>
        <p class="eyebrow">Refund Administration</p>
        <h1 id="refunds-title">退款管理</h1>
        <p>查看退款申请并通过管理员接口处理退款成功，退款完成后由后端回写预约和号源状态。</p>
      </div>
      <button class="refresh-btn" :disabled="loading" type="button" @click="loadRefunds">
        {{ loading ? '刷新中' : '刷新列表' }}
      </button>
    </section>

    <section class="summary-grid" aria-label="退款概览">
      <article class="summary-card">
        <span>退款记录</span>
        <strong>{{ summary.total }}</strong>
      </article>
      <article class="summary-card warn-card">
        <span>退款申请</span>
        <strong>{{ summary.applying }}</strong>
      </article>
      <article class="summary-card">
        <span>退款成功</span>
        <strong>{{ summary.success }}</strong>
      </article>
      <article class="summary-card">
        <span>列表金额</span>
        <strong>¥{{ summary.amount }}</strong>
      </article>
    </section>

    <section class="toolbar-panel" aria-label="退款筛选">
      <label>
        <span>关键词</span>
        <input v-model.trim="filters.keyword" type="search" placeholder="患者 / 医生 / 预约ID" @keyup.enter="search" />
      </label>
      <label>
        <span>退款状态</span>
        <select v-model="filters.status">
          <option value="">全部状态</option>
          <option value="退款申请">退款申请</option>
          <option value="退款成功">退款成功</option>
          <option value="退款拒绝">已拒绝</option>
        </select>
      </label>
      <button class="primary-btn" :disabled="loading" type="button" @click="search">查询</button>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>
    <p v-if="successMsg" class="notice success" role="status">{{ successMsg }}</p>

    <section class="table-panel" aria-label="退款列表">
      <div v-if="loading" class="empty-state">正在请求退款管理接口...</div>
      <div v-else-if="refunds.length === 0" class="empty-state">暂无符合条件的退款记录</div>
      <table v-else>
        <thead>
          <tr>
            <th>退款ID</th>
            <th>预约ID</th>
            <th>患者</th>
            <th>医生</th>
            <th>金额</th>
            <th>申请原因</th>
            <th>状态</th>
            <th>申请时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in refunds" :key="refundIdOf(item) || item.appointmentId">
            <td>{{ refundIdOf(item) || '--' }}</td>
            <td>{{ item.appointmentId || item.orderNo || '--' }}</td>
            <td>{{ item.patientName || item.patient?.name || '--' }}</td>
            <td>{{ item.doctorName || item.doctor?.name || '--' }}</td>
            <td class="amount-cell">¥{{ Number(item.amount || 0).toFixed(2) }}</td>
            <td class="reason-cell">{{ item.reason || '--' }}</td>
            <td>
              <span class="status-tag" :class="`status-${statusTone(item.status)}`">
                {{ statusText(item.status) }}
              </span>
            </td>
            <td>{{ item.createTime || item.createdAt || '--' }}</td>
            <td>
              <div class="row-actions">
                <button class="link-btn" :disabled="!!actionId || !canProcess(item)" type="button" @click="openProcessConfirm(item, 'approve')">
                  {{ actionId === String(refundIdOf(item)) ? '处理中...' : '确认退款' }}
                </button>
                <span
                  class="reject-action"
                  :tabindex="canProcess(item) && !canReject(item) ? 0 : undefined"
                  :aria-label="canProcess(item) && !canReject(item) ? `拒绝退款不可用：${rejectBlockedText(item)}` : undefined"
                >
                  <button class="reject-btn" :disabled="!!actionId || !canReject(item)" type="button" @click="openProcessConfirm(item, 'reject')">
                    拒绝退款
                  </button>
                  <small v-if="canProcess(item) && !canReject(item)" class="blocked-reason" role="tooltip">
                    {{ rejectBlockedText(item) }}
                  </small>
                </span>
              </div>
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

    <div v-if="pendingRefund" class="modal-mask" role="presentation">
      <section class="confirm-panel" role="dialog" aria-modal="true" aria-labelledby="refund-confirm-title">
        <h2 id="refund-confirm-title">{{ processAction === 'approve' ? '确认退款成功' : '拒绝退款申请' }}</h2>
        <p>
          即将处理退款申请
          <strong>{{ refundIdOf(pendingRefund) || '--' }}</strong>
          ，金额
          <strong>¥{{ Number(pendingRefund.amount || 0).toFixed(2) }}</strong>
          。{{ processAction === 'approve' ? '提交后将取消预约并回补号源。' : '拒绝后预约将恢复为已支付状态。' }}
        </p>
        <label>
          <span>{{ processAction === 'approve' ? '处理备注（选填）' : '拒绝理由（必填）' }}</span>
          <textarea v-model.trim="processForm.note" rows="4" :placeholder="processAction === 'approve' ? '记录管理员处理说明' : '请说明拒绝退款的原因'"></textarea>
        </label>
        <div class="confirm-actions">
          <button class="secondary-btn" :disabled="!!actionId" type="button" @click="closeProcessConfirm">取消</button>
          <button :class="processAction === 'approve' ? 'primary-btn' : 'reject-btn'" :disabled="!!actionId" type="button" @click="confirmProcess">
            {{ actionId ? '提交中...' : (processAction === 'approve' ? '确认退款成功' : '确认拒绝') }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.refunds-view {
  min-height: 100%;
}

.page-hero,
.summary-card,
.toolbar-panel,
.table-panel,
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
.wait-btn,
.link-btn {
  min-height: 40px;
  border-radius: var(--radius-sm);
  padding: 0 13px;
  font-size: 13px;
  font-weight: 800;
}

.reject-btn {
  min-height: 40px;
  border: 1.5px solid #d97706;
  border-radius: var(--radius-sm);
  padding: 0 13px;
  background: #fff7ed;
  color: #b45309;
  font-size: 13px;
  font-weight: 800;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reject-action {
  position: relative;
  display: inline-flex;
  outline: none;
}

.blocked-reason {
  position: absolute;
  right: 0;
  bottom: calc(100% + 8px);
  z-index: 20;
  width: max-content;
  max-width: 240px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #7f1d1d;
  color: #fff;
  box-shadow: 0 8px 24px rgb(127 29 29 / 20%);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.45;
  white-space: normal;
  pointer-events: none;
  opacity: 0;
  visibility: hidden;
  transform: translateY(4px);
  transition: opacity 150ms ease, transform 150ms ease, visibility 150ms ease;
}

.blocked-reason::after {
  position: absolute;
  top: 100%;
  right: 24px;
  width: 0;
  height: 0;
  border: 6px solid transparent;
  border-top-color: #7f1d1d;
  content: '';
}

.reject-action:hover .blocked-reason,
.reject-action:focus-visible .blocked-reason,
.reject-action:focus-within .blocked-reason {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.reject-action:focus-visible {
  border-radius: var(--radius-sm);
  box-shadow: 0 0 0 3px rgb(13 148 136 / 22%);
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

.wait-btn {
  border: 1.5px dashed var(--color-border-strong);
  background: var(--color-primary-soft);
  color: var(--color-ink-3);
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
  font-size: 26px;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.warn-card strong {
  color: var(--color-warn);
}

.toolbar-panel {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 170px auto auto;
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
.confirm-panel textarea {
  width: 100%;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0 12px;
  color: var(--color-ink);
  outline: none;
}

.toolbar-panel input,
.toolbar-panel select {
  min-height: 40px;
}

.confirm-panel textarea {
  min-height: 96px;
  padding-top: 10px;
  resize: vertical;
}

.toolbar-panel input:focus,
.toolbar-panel select:focus,
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

.amount-cell {
  color: var(--color-ink);
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}

.reason-cell {
  max-width: 240px;
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

.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
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

  .confirm-actions,
  .pager {
    flex-direction: column;
  }
}
</style>
