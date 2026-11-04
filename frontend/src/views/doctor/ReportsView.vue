<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  confirmExaminationResult,
  createExamination,
  getDoctorExaminations,
  getExaminationReport,
  rejectExaminationResult,
} from '@/api/doctor'
import ListPager from '@/components/common/ListPager.vue'

const loading = ref('')
const errorMsg = ref('')
const successMsg = ref('')
const createdExamination = ref(null)
const report = ref(null)
const examinations = ref([])
const listLoading = ref(false)
const reviewId = ref(null)
const rejectReason = ref('')
const confirmItem = ref(null)
const listPage = ref(1)
const listPageSize = 10
const pagedExaminations = computed(() => {
  const start = (listPage.value - 1) * listPageSize
  return examinations.value.slice(start, start + listPageSize)
})

const examination = reactive({
  medicalRecordId: '',
  itemName: '',
})

const query = reactive({
  examinationId: '',
})

const fieldErrors = reactive({
  examination: {},
  query: {},
})

const overview = computed(() => [
  { label: '检查申请', value: examinations.value.length, desc: '当前医生创建的检查申请' },
  { label: '待医生确认', value: examinations.value.filter((item) => item.status === '待医生确认').length, desc: '患者已提交结果，等待医生核对' },
  { label: '已完成', value: examinations.value.filter((item) => item.status === '完成').length, desc: '可直接查看检查报告' },
])

function normalizeResponseList(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.list)) return data.list
  return []
}

function formatTime(value) {
  if (!value) return '--'
  return String(value).replace('T', ' ')
}

function statusClass(status) {
  if (status === '完成') return 'completed'
  if (status === '待医生确认') return 'review'
  if (status === '需补充') return 'revision'
  if (status === '执行中') return 'running'
  return 'pending'
}

async function confirmResult(item) {
  if (loading.value) return
  loading.value = `confirm-${item.examinationId}`
  errorMsg.value = ''
  successMsg.value = ''
  try {
    await confirmExaminationResult(item.examinationId)
    successMsg.value = `检查申请 #${item.examinationId} 已确认并生成正式报告`
    confirmItem.value = null
    await loadExaminations()
  } catch (e) {
    errorMsg.value = e.message || '检查结果确认失败，请稍后重试'
  } finally {
    loading.value = ''
  }
}

function openConfirm(item) {
  confirmItem.value = item
}

function closeConfirm() {
  if (!loading.value) confirmItem.value = null
}

function openReject(item) {
  reviewId.value = item.examinationId
  rejectReason.value = ''
}

async function rejectResult(item) {
  if (!rejectReason.value.trim() || loading.value) return
  loading.value = `reject-${item.examinationId}`
  errorMsg.value = ''
  successMsg.value = ''
  try {
    await rejectExaminationResult(item.examinationId, { reason: rejectReason.value.trim() })
    successMsg.value = `检查申请 #${item.examinationId} 已退回患者补充`
    reviewId.value = null
    rejectReason.value = ''
    await loadExaminations()
  } catch (e) {
    errorMsg.value = e.message || '退回操作失败，请稍后重试'
  } finally {
    loading.value = ''
  }
}

async function loadExaminations() {
  listLoading.value = true
  try {
    const data = await getDoctorExaminations({ page: 1, size: 100 })
    examinations.value = normalizeResponseList(data)
    const maxPage = Math.max(1, Math.ceil(examinations.value.length / listPageSize))
    listPage.value = Math.min(listPage.value, maxPage)
  } catch (e) {
    errorMsg.value = e.message || '检查申请列表加载失败，请稍后重试'
  } finally {
    listLoading.value = false
  }
}

function validateExamination() {
  fieldErrors.examination = {}
  if (!examination.medicalRecordId) {
    fieldErrors.examination.medicalRecordId = '请填写诊疗记录ID'
  }
  if (!examination.itemName.trim()) {
    fieldErrors.examination.itemName = '请填写检查项目'
  }
  return Object.keys(fieldErrors.examination).length === 0
}

function validateQuery() {
  fieldErrors.query = {}
  if (!query.examinationId) {
    fieldErrors.query.examinationId = '请填写检查申请ID'
  }
  return Object.keys(fieldErrors.query).length === 0
}

async function submitExamination() {
  if (loading.value || !validateExamination()) return
  loading.value = 'examination'
  errorMsg.value = ''
  successMsg.value = ''
  createdExamination.value = null
  try {
    createdExamination.value = await createExamination({
      medicalRecordId: Number(examination.medicalRecordId),
      itemName: examination.itemName.trim(),
    })
    successMsg.value = '检查申请已提交，请等待检查结果'
    examination.itemName = ''
    await loadExaminations()
  } catch (e) {
    errorMsg.value = e.message || '提交检查申请失败，请稍后重试'
  } finally {
    loading.value = ''
  }
}

async function viewReport(item) {
  query.examinationId = item.examinationId
  await queryReport()
}

onMounted(loadExaminations)

async function queryReport() {
  if (loading.value || !validateQuery()) return
  loading.value = 'report'
  errorMsg.value = ''
  successMsg.value = ''
  report.value = null
  try {
    report.value = await getExaminationReport(query.examinationId)
    successMsg.value = '检查报告已返回'
  } catch (e) {
    errorMsg.value = e.message || '暂时无法加载检查报告，请稍后重试'
  } finally {
    loading.value = ''
  }
}
</script>

<template>
  <div class="reports-view">
    <section class="page-hero" aria-labelledby="reports-title">
      <div>
        <p class="eyebrow">Report Center</p>
        <h1 id="reports-title">报告管理</h1>
        <p>提交检查申请后，可在检查完成后查看对应报告。</p>
      </div>
    </section>

    <section class="overview-grid" aria-label="报告概览">
      <article v-for="item in overview" :key="item.label" class="overview-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.desc }}</p>
      </article>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>
    <p v-if="successMsg" class="notice success" role="status">{{ successMsg }}</p>

    <section class="work-grid">
      <form class="panel" @submit.prevent="submitExamination">
        <div class="panel-head">
          <div>
            <h2>创建检查申请</h2>
            <p>根据诊疗记录为患者提交检查申请。</p>
          </div>
          <span class="api-badge">创建申请</span>
        </div>

        <label class="field">
          <span>诊疗记录ID</span>
          <input
            v-model="examination.medicalRecordId"
            :aria-describedby="fieldErrors.examination.medicalRecordId ? 'record-id-error' : undefined"
            min="1"
            type="number"
            placeholder="请输入诊疗记录编号"
          />
          <small v-if="fieldErrors.examination.medicalRecordId" id="record-id-error" class="field-error">
            {{ fieldErrors.examination.medicalRecordId }}
          </small>
        </label>

        <label class="field">
          <span>检查项目</span>
          <input
            v-model="examination.itemName"
            :aria-describedby="fieldErrors.examination.itemName ? 'item-name-error' : undefined"
            type="text"
            placeholder="如：血常规、胸部CT"
          />
          <small v-if="fieldErrors.examination.itemName" id="item-name-error" class="field-error">
            {{ fieldErrors.examination.itemName }}
          </small>
        </label>

        <button class="submit-btn" :disabled="!!loading" type="submit">
          {{ loading === 'examination' ? '提交中...' : '提交检查申请' }}
        </button>

        <div v-if="createdExamination" class="result-card">
          <span>申请返回</span>
          <strong>检查申请ID：{{ createdExamination.examinationId || createdExamination.id || '--' }}</strong>
        </div>
      </form>

      <form class="panel" @submit.prevent="queryReport">
        <div class="panel-head">
          <div>
            <h2>查看检查报告</h2>
            <p>输入检查申请编号，查看自己患者的检查报告。</p>
          </div>
          <span class="api-badge">查询报告</span>
        </div>

        <label class="field">
          <span>检查申请ID</span>
          <input
            v-model="query.examinationId"
            :aria-describedby="fieldErrors.query.examinationId ? 'examination-id-error' : undefined"
            min="1"
            type="number"
            placeholder="请输入检查申请编号"
          />
          <small v-if="fieldErrors.query.examinationId" id="examination-id-error" class="field-error">
            {{ fieldErrors.query.examinationId }}
          </small>
        </label>

        <button class="submit-btn secondary" :disabled="!!loading" type="submit">
          {{ loading === 'report' ? '查询中...' : '查询报告' }}
        </button>

        <article v-if="report" class="report-card">
          <div class="report-top">
            <div>
              <span>报告结果</span>
              <strong>{{ report.result || '报告结果尚未生成' }}</strong>
            </div>
            <em>{{ report.status || '完成' }}</em>
          </div>
          <dl>
            <div>
              <dt>检查项目</dt>
              <dd>{{ report.itemName || report.examinationItem || '--' }}</dd>
            </div>
            <div>
              <dt>医生</dt>
              <dd>{{ report.doctorName || '--' }}</dd>
            </div>
            <div>
              <dt>生成时间</dt>
              <dd>{{ report.createTime || report.createdAt || '--' }}</dd>
            </div>
          </dl>
        </article>
      </form>
    </section>

    <section class="waiting-panel" aria-labelledby="waiting-title">
      <div class="panel-head">
        <div>
          <h2 id="waiting-title">检查申请记录</h2>
          <p>提交后的检查会立即出现在这里，检查完成后可查看报告。</p>
        </div>
        <button class="refresh-btn" :disabled="listLoading" type="button" @click="loadExaminations">
          {{ listLoading ? '刷新中...' : '刷新列表' }}
        </button>
      </div>
      <p v-if="listLoading && !examinations.length" class="list-state">正在加载检查申请...</p>
      <p v-else-if="!examinations.length" class="list-state">暂无检查申请</p>
      <div v-else class="examination-list">
        <article v-for="item in pagedExaminations" :key="item.examinationId" class="examination-item">
          <div class="examination-main">
            <div class="item-title">
              <strong>{{ item.itemName }}</strong>
              <span :class="['status-badge', statusClass(item.status)]">{{ item.status }}</span>
            </div>
            <p>{{ item.patientName }} · 检查申请 #{{ item.examinationId }}</p>
            <small>提交时间：{{ formatTime(item.createTime) }}</small>
            <div v-if="item.status === '待医生确认'" class="submitted-result">
              <span>患者提交的检查结果</span>
              <p>{{ item.result }}</p>
              <small v-if="item.remark">补充说明：{{ item.remark }}</small>
            </div>
          </div>
          <div v-if="item.status === '待医生确认'" class="review-actions">
            <button class="report-action" :disabled="!!loading" type="button" @click="openConfirm(item)">
              {{ loading === `confirm-${item.examinationId}` ? '确认中...' : '确认报告' }}
            </button>
            <button class="report-action danger" :disabled="!!loading" type="button" @click="openReject(item)">退回补充</button>
          </div>
          <button v-else-if="item.status === '完成'" class="report-action" :disabled="loading === 'report'" type="button" @click="viewReport(item)">
            查看报告
          </button>
          <span v-else class="passive-status">
            {{ item.status === '执行中' ? '检查进行中' : item.status === '需补充' ? '等待患者补充' : '等待患者检查' }}
          </span>
          <form v-if="reviewId === item.examinationId" class="reject-form" @submit.prevent="rejectResult(item)">
            <label :for="`reject-${item.examinationId}`">退回原因</label>
            <textarea :id="`reject-${item.examinationId}`" v-model="rejectReason" maxlength="500" rows="2" placeholder="请说明需要患者补充的内容"></textarea>
            <div>
              <button type="button" @click="reviewId = null">取消</button>
              <button :disabled="!rejectReason.trim() || !!loading" type="submit">确认退回</button>
            </div>
          </form>
        </article>
      </div>
      <ListPager v-model:page="listPage" :page-size="listPageSize" :total="examinations.length" :disabled="listLoading" />
    </section>

    <Teleport to="body">
      <div v-if="confirmItem" class="dialog-backdrop" @click.self="closeConfirm" @keydown.esc="closeConfirm">
        <section
          class="confirm-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="confirm-report-title"
          aria-describedby="confirm-report-description"
        >
          <div class="dialog-icon" aria-hidden="true">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 12l2 2 4-4" />
              <circle cx="12" cy="12" r="9" />
            </svg>
          </div>
          <div class="dialog-copy">
            <p class="dialog-eyebrow">确认检查报告</p>
            <h2 id="confirm-report-title">确认患者提交的结果无误？</h2>
            <p id="confirm-report-description">
              检查项目“{{ confirmItem.itemName }}”确认后将生成正式报告，患者可立即查看，此操作不可撤销。
            </p>
          </div>
          <div class="dialog-summary">
            <span>患者</span>
            <strong>{{ confirmItem.patientName }}</strong>
            <span>检查申请</span>
            <strong>#{{ confirmItem.examinationId }}</strong>
          </div>
          <div class="dialog-actions">
            <button :disabled="!!loading" type="button" @click="closeConfirm">暂不确认</button>
            <button autofocus :disabled="!!loading" type="button" @click="confirmResult(confirmItem)">
              {{ loading === `confirm-${confirmItem.examinationId}` ? '正在确认...' : '确认并生成报告' }}
            </button>
          </div>
        </section>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.reports-view {
  min-height: 100%;
  padding: 20px 18px 28px;
}

.page-hero,
.overview-card,
.panel,
.waiting-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}

.page-hero {
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
  max-width: 680px;
  margin-top: 8px;
  color: var(--color-ink-3);
  font-size: 13px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.overview-card {
  padding: 16px;
}

.overview-card span,
.result-card span,
.report-top span,
.waiting-grid span {
  display: block;
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 700;
}

.overview-card strong {
  display: block;
  margin-top: 6px;
  color: var(--color-ink);
  font-size: 24px;
  line-height: 1.25;
}

.overview-card p {
  margin-top: 5px;
  color: var(--color-ink-3);
  font-size: 12px;
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

.work-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 14px;
  align-items: start;
}

.panel,
.waiting-panel {
  padding: 16px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
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

.api-badge,
.wait-badge {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
}

.refresh-btn,
.report-action {
  min-height: 38px;
  border: 1.5px solid var(--color-primary);
  border-radius: var(--radius-sm);
  padding: 0 14px;
  background: #fff;
  color: var(--color-primary);
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.refresh-btn:disabled,
.report-action:disabled {
  border-color: var(--color-border);
  color: var(--color-ink-3);
  cursor: not-allowed;
  opacity: 0.72;
}

.api-badge {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.wait-badge {
  background: var(--color-warn-soft);
  color: var(--color-warn);
}

.field {
  display: block;
  margin-bottom: 13px;
}

.field span {
  display: block;
  margin-bottom: 6px;
  color: var(--color-ink-2);
  font-size: 13px;
  font-weight: 800;
}

.field input {
  width: 100%;
  min-height: 42px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0 12px;
  background: #fff;
  color: var(--color-ink);
  font: inherit;
  outline: none;
}

.field input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(14, 143, 134, 0.12);
}

.field-error {
  display: block;
  margin-top: 5px;
  color: var(--color-danger);
  font-size: 12px;
  font-weight: 700;
}

.submit-btn {
  width: 100%;
  min-height: 42px;
  border: 1.5px solid var(--color-primary);
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
}

.submit-btn.secondary {
  border-color: var(--color-accent);
  background: var(--color-accent);
}

.submit-btn:disabled {
  opacity: 0.48;
  cursor: not-allowed;
}

.result-card,
.report-card,
.waiting-grid article {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-primary-soft);
}

.result-card {
  margin-top: 13px;
  padding: 12px;
}

.result-card strong {
  display: block;
  margin-top: 4px;
  color: var(--color-ink);
  font-size: 14px;
}

.report-card {
  margin-top: 13px;
  padding: 14px;
}

.report-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.report-top strong {
  display: block;
  margin-top: 4px;
  color: var(--color-ink);
  font-size: 16px;
  overflow-wrap: anywhere;
}

.report-top em {
  border-radius: 999px;
  padding: 6px 10px;
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.report-card dl {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.report-card dt {
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 700;
}

.report-card dd {
  margin-top: 3px;
  color: var(--color-ink);
  font-size: 13px;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.waiting-panel {
  margin-top: 14px;
}

.list-state {
  border-radius: var(--radius-sm);
  padding: 28px 16px;
  background: var(--color-primary-soft);
  color: var(--color-ink-3);
  text-align: center;
  font-size: 13px;
}

.examination-list {
  display: grid;
  gap: 10px;
}

.examination-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 14px;
  background: var(--color-primary-soft);
}

.examination-main {
  min-width: 0;
}

.item-title {
  display: flex;
  align-items: center;
  gap: 9px;
}

.item-title strong {
  color: var(--color-ink);
  font-size: 15px;
}

.status-badge {
  border-radius: 999px;
  padding: 4px 8px;
  font-size: 11px;
  font-weight: 800;
}

.status-badge.pending {
  background: var(--color-warn-soft);
  color: var(--color-warn);
}

.status-badge.running {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.status-badge.completed {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.status-badge.review {
  background: var(--color-info-soft);
  color: var(--color-info);
}

.status-badge.revision {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.submitted-result {
  margin-top: 10px;
  border-left: 3px solid var(--color-info);
  padding: 8px 10px;
  background: #fff;
}

.submitted-result span {
  color: var(--color-info);
  font-size: 12px;
  font-weight: 800;
}

.submitted-result p {
  margin-top: 5px;
  color: var(--color-ink);
  line-height: 1.65;
  white-space: pre-wrap;
}

.review-actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
}

.report-action.danger {
  border-color: var(--color-danger);
  color: var(--color-danger);
}

.passive-status {
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  padding: 10px 14px;
  background: #fff;
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 800;
}

.reject-form {
  flex: 1 0 100%;
  display: grid;
  gap: 7px;
  border-top: 1px solid var(--color-border);
  padding-top: 12px;
}

.reject-form label {
  color: var(--color-ink-2);
  font-size: 13px;
  font-weight: 800;
}

.reject-form textarea {
  width: 100%;
  resize: vertical;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  color: var(--color-ink);
  font: inherit;
  outline: none;
}

.reject-form textarea:focus {
  border-color: var(--color-danger);
  box-shadow: 0 0 0 3px rgba(220, 38, 38, .1);
}

.reject-form > div {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.reject-form button {
  min-height: 38px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0 13px;
  background: #fff;
  color: var(--color-ink-2);
  font: inherit;
  font-size: 12px;
  font-weight: 800;
}

.reject-form button[type='submit'] {
  border-color: var(--color-danger);
  background: var(--color-danger);
  color: #fff;
}

.dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(8, 45, 42, .48);
  backdrop-filter: blur(3px);
}

.confirm-dialog {
  width: min(440px, 100%);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 22px;
  background: var(--color-surface);
  box-shadow: 0 24px 64px rgba(8, 45, 42, .24);
}

.dialog-icon {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.dialog-copy {
  margin-top: 14px;
}

.dialog-eyebrow {
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 800;
}

.dialog-copy h2 {
  margin-top: 4px;
  color: var(--color-ink);
  font-size: 20px;
  line-height: 1.35;
}

.dialog-copy > p:last-child {
  margin-top: 8px;
  color: var(--color-ink-3);
  font-size: 13px;
  line-height: 1.7;
}

.dialog-summary {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 8px 14px;
  margin-top: 16px;
  border-radius: var(--radius-sm);
  padding: 12px;
  background: var(--color-bg);
}

.dialog-summary span {
  color: var(--color-ink-3);
  font-size: 12px;
}

.dialog-summary strong {
  color: var(--color-ink);
  font-size: 13px;
  text-align: right;
}

.dialog-actions {
  display: grid;
  grid-template-columns: 1fr 1.35fr;
  gap: 10px;
  margin-top: 18px;
}

.dialog-actions button {
  min-height: 44px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fff;
  color: var(--color-ink-2);
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.dialog-actions button:last-child {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}

.dialog-actions button:focus-visible {
  outline: 3px solid rgba(14, 143, 134, .24);
  outline-offset: 2px;
}

.dialog-actions button:disabled {
  cursor: not-allowed;
  opacity: .52;
}

@media (prefers-reduced-motion: no-preference) {
  .confirm-dialog {
    animation: dialog-in .16s ease-out;
  }

  @keyframes dialog-in {
    from { opacity: 0; transform: translateY(8px) scale(.98); }
    to { opacity: 1; transform: translateY(0) scale(1); }
  }
}

.examination-main p,
.examination-main small {
  display: block;
  margin-top: 5px;
  color: var(--color-ink-3);
  font-size: 12px;
}

.waiting-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.waiting-grid article {
  padding: 14px;
}

.waiting-grid strong {
  display: block;
  margin-top: 5px;
  color: var(--color-ink);
  font-size: 14px;
  overflow-wrap: anywhere;
}

.waiting-grid p {
  margin-top: 8px;
  color: var(--color-ink-3);
  font-size: 12px;
}

@media (max-width: 900px) {
  .overview-grid,
  .work-grid,
  .waiting-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .reports-view {
    padding: 16px 12px 24px;
  }

  .page-hero,
  .panel,
  .waiting-panel {
    padding: 14px;
  }

  .panel-head,
  .report-top,
  .examination-item {
    flex-direction: column;
  }

  .report-action {
    width: 100%;
  }

  .review-actions {
    width: 100%;
  }

  .report-card dl {
    grid-template-columns: 1fr;
  }
}
</style>
