<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyExaminations, getMyRecords, submitExaminationResult } from '@/api/patient'
import ListPager from '@/components/common/ListPager.vue'

const router = useRouter()

const loading = ref(false)
const actionId = ref('')
const errorMsg = ref('')
const successMsg = ref('')
const records = ref([])
const activeRecord = ref(null)
const examinations = ref([])
const examinationLoading = ref(false)
const editingExaminationId = ref(null)
const resultForm = reactive({ result: '', remark: '' })
const examinationPage = ref(1)
const examinationPageSize = 10

const pager = reactive({
  page: 1,
  size: 10,
  total: 0,
})

const pages = computed(() => Math.max(1, Math.ceil(pager.total / pager.size)))
const finishedCount = computed(() => records.value.filter((item) => statusText(item) === '已完成').length)
const prescriptionCount = computed(() => records.value.filter((item) => item.prescriptionId).length)
const reportCount = computed(() => records.value.filter((item) => item.examinationId).length)
const pendingExaminations = computed(() => examinations.value.filter((item) => item.status !== '完成'))
const pagedExaminations = computed(() => {
  const start = (examinationPage.value - 1) * examinationPageSize
  return examinations.value.slice(start, start + examinationPageSize)
})

function recordIdOf(item) {
  return item.medicalRecordId || item.recordId || item.id
}

function doctorText(item) {
  return item.doctorName || item.doctor?.name || '等待医生信息'
}

function departmentText(item) {
  return item.department || item.doctorDepartment || item.doctor?.department || '科室待返回'
}

function visitTimeText(item) {
  return item.visitTime || item.visitDate || item.finishTime || item.createTime || '就诊时间待返回'
}

function diagnosisText(item) {
  return item.diagnosis || item.diseaseName || '诊断结果待返回'
}

function treatmentText(item) {
  return item.treatment || item.treatmentAdvice || '治疗建议待返回'
}

function statusText(item) {
  return item.status || '已完成'
}

function selectRecord(item) {
  activeRecord.value = item
}

async function loadRecords() {
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const data = await getMyRecords({
      page: pager.page,
      size: pager.size,
    })
    records.value = Array.isArray(data) ? data : data?.records || []
    pager.total = Array.isArray(data) ? data.length : data?.total || 0
    activeRecord.value = records.value[0] || null
  } catch (e) {
    errorMsg.value = e.message || '诊疗记录加载失败，请稍后重试'
    records.value = []
    pager.total = 0
    activeRecord.value = null
  } finally {
    loading.value = false
  }
}

async function loadExaminations() {
  examinationLoading.value = true
  try {
    const data = await getMyExaminations()
    examinations.value = Array.isArray(data) ? data : data?.records || []
    const maxPage = Math.max(1, Math.ceil(examinations.value.length / examinationPageSize))
    examinationPage.value = Math.min(examinationPage.value, maxPage)
  } catch (e) {
    errorMsg.value = e.message || '检查申请加载失败，请稍后重试'
  } finally {
    examinationLoading.value = false
  }
}

function examinationStatusText(status) {
  return {
    申请: '等待线下检查',
    待医生确认: '等待医生确认',
    需补充: '需要补充结果',
    完成: '报告已完成',
  }[status] || status
}

function openResultForm(item) {
  editingExaminationId.value = item.examinationId
  resultForm.result = item.status === '需补充' ? (item.result || '') : ''
  resultForm.remark = item.status === '需补充' ? '' : (item.remark || '')
}

function closeResultForm() {
  editingExaminationId.value = null
  resultForm.result = ''
  resultForm.remark = ''
}

async function submitResult(item) {
  if (!resultForm.result.trim() || actionId.value) return
  actionId.value = `examination-${item.examinationId}`
  errorMsg.value = ''
  successMsg.value = ''
  try {
    await submitExaminationResult(item.examinationId, {
      result: resultForm.result.trim(),
      remark: resultForm.remark.trim() || null,
    })
    successMsg.value = '检查结果已提交，请等待医生确认'
    closeResultForm()
    await loadExaminations()
  } catch (e) {
    errorMsg.value = e.message || '检查结果提交失败，请稍后重试'
  } finally {
    actionId.value = ''
  }
}

function showPrescription(item) {
  const id = item.prescriptionId || recordIdOf(item)
  router.push(`/patient/prescriptions/${id}`)
}

function showReport(item) {
  const id = item.examinationId || recordIdOf(item)
  router.push(`/patient/reports/${id}`)
}

function prevPage() {
  if (pager.page <= 1) return
  pager.page--
  loadRecords()
}

function nextPage() {
  if (pager.page >= pages.value) return
  pager.page++
  loadRecords()
}

onMounted(() => {
  loadRecords()
  loadExaminations()
})
</script>

<template>
  <div class="records-view">
    <header class="hero">
      <div>
        <p class="eyebrow">就诊档案</p>
        <h1>诊疗记录</h1>
        <p>查看自己的病历、处方和检查报告，医疗数据仅患者本人可见。</p>
      </div>
      <button class="refresh-btn" :disabled="loading" aria-label="刷新诊疗记录" @click="loadRecords">
        <svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12a9 9 0 0 1-15.5 6.2" />
          <path d="M3 12A9 9 0 0 1 18.5 5.8" />
          <path d="M18 2v4h-4" />
          <path d="M6 22v-4h4" />
        </svg>
      </button>
    </header>

    <section class="summary-grid" aria-label="诊疗记录概览">
      <div class="summary-card">
        <span>记录总数</span>
        <strong>{{ pager.total || records.length }}</strong>
      </div>
      <div class="summary-card success">
        <span>已完成</span>
        <strong>{{ finishedCount }}</strong>
      </div>
      <div class="summary-card warn">
        <span>可查处方</span>
        <strong>{{ prescriptionCount }}</strong>
      </div>
      <div class="summary-card info">
        <span>检查报告</span>
        <strong>{{ reportCount }}</strong>
      </div>
    </section>

    <p v-if="errorMsg" class="notice error" role="alert">{{ errorMsg }}</p>
    <p v-if="successMsg" class="notice success" role="status">{{ successMsg }}</p>

    <section class="examination-panel" aria-labelledby="patient-examinations-title">
      <div class="examination-head">
        <div>
          <p class="eyebrow">线下检查</p>
          <h2 id="patient-examinations-title">检查申请</h2>
          <p>完成线下检查并取得结果后，请在这里提交给开单医生确认。</p>
        </div>
        <button class="panel-refresh" :disabled="examinationLoading" type="button" @click="loadExaminations">
          {{ examinationLoading ? '刷新中...' : `待处理 ${pendingExaminations.length}` }}
        </button>
      </div>

      <p v-if="examinationLoading && !examinations.length" class="examination-state">正在加载检查申请...</p>
      <p v-else-if="!examinations.length" class="examination-state">暂无检查申请</p>
      <div v-else class="patient-examination-list">
        <article v-for="item in pagedExaminations" :key="item.examinationId" class="patient-examination-card">
          <div class="patient-examination-summary">
            <div>
              <div class="examination-title-row">
                <h3>{{ item.itemName }}</h3>
                <span :class="['examination-status', `status-${item.status}`]">{{ examinationStatusText(item.status) }}</span>
              </div>
              <p>{{ item.doctorName }} · {{ item.department }} · 申请编号 {{ item.examinationId }}</p>
              <small v-if="item.status === '需补充'">医生退回原因：{{ item.remark || '请补充更完整的检查结果' }}</small>
            </div>
            <button
              v-if="item.status === '申请' || item.status === '需补充'"
              class="result-entry-btn"
              type="button"
              @click="openResultForm(item)"
            >
              {{ item.status === '需补充' ? '补充检查结果' : '我已完成检查' }}
            </button>
            <button v-else-if="item.status === '完成'" class="result-entry-btn secondary" type="button" @click="showReport(item)">
              查看正式报告
            </button>
          </div>

          <form v-if="editingExaminationId === item.examinationId" class="patient-result-form" @submit.prevent="submitResult(item)">
            <label>
              <span>检查结果 <em>*</em></span>
              <textarea v-model="resultForm.result" maxlength="5000" rows="4" placeholder="请根据线下报告如实填写检查结果"></textarea>
            </label>
            <label>
              <span>补充说明</span>
              <textarea v-model="resultForm.remark" maxlength="2000" rows="2" placeholder="可填写报告结论、检查机构等信息"></textarea>
            </label>
            <p class="form-tip">提交内容由患者提供，仅供医生核对；医生确认后才会形成正式报告。</p>
            <div class="form-actions">
              <button type="button" @click="closeResultForm">取消</button>
              <button :disabled="!resultForm.result.trim() || !!actionId" type="submit">
                {{ actionId ? '提交中...' : '提交医生确认' }}
              </button>
            </div>
          </form>
        </article>
      </div>
      <ListPager v-model:page="examinationPage" :page-size="examinationPageSize" :total="examinations.length" :disabled="examinationLoading" />
    </section>

    <section class="records-layout">
      <div class="record-list" aria-label="诊疗记录列表">
        <article v-if="loading" class="skeleton-card" aria-label="正在加载诊疗记录">
          <span></span>
          <span></span>
          <span></span>
        </article>

        <div v-else-if="records.length === 0" class="empty-state">
          <svg aria-hidden="true" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <path d="M14 2v6h6M9 13h6M9 17h4" />
          </svg>
          <h2>暂无诊疗记录</h2>
          <p>完成接诊后，医生生成的诊疗记录会显示在这里。</p>
        </div>

        <button
          v-for="item in records"
          v-else
          :key="recordIdOf(item)"
          class="record-card"
          :class="{ active: recordIdOf(activeRecord || {}) === recordIdOf(item) }"
          @click="selectRecord(item)"
        >
          <span class="record-icon" aria-hidden="true">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 3h6l1 2h3a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1h3z" />
              <path d="M9 12h6M9 16h4" />
            </svg>
          </span>
          <span class="record-main">
            <span class="record-title">
              <strong>{{ doctorText(item) }}</strong>
              <em>{{ statusText(item) }}</em>
            </span>
            <span class="record-meta">{{ departmentText(item) }} · {{ visitTimeText(item) }}</span>
            <span class="record-diagnosis">{{ diagnosisText(item) }}</span>
          </span>
        </button>
      </div>

      <aside class="detail-panel" aria-label="诊疗记录详情">
        <template v-if="activeRecord">
          <div class="detail-head">
            <div>
              <p class="eyebrow">记录详情</p>
              <h2>{{ doctorText(activeRecord) }}</h2>
            </div>
            <span class="status-tag">{{ statusText(activeRecord) }}</span>
          </div>

          <dl class="detail-grid">
            <div>
              <dt>记录编号</dt>
              <dd>{{ recordIdOf(activeRecord) || '--' }}</dd>
            </div>
            <div>
              <dt>预约编号</dt>
              <dd>{{ activeRecord.appointmentId || '--' }}</dd>
            </div>
            <div>
              <dt>科室</dt>
              <dd>{{ departmentText(activeRecord) }}</dd>
            </div>
            <div>
              <dt>就诊时间</dt>
              <dd>{{ visitTimeText(activeRecord) }}</dd>
            </div>
          </dl>

          <section class="clinical-block">
            <h3>诊断结果</h3>
            <p>{{ diagnosisText(activeRecord) }}</p>
          </section>

          <section class="clinical-block">
            <h3>治疗建议</h3>
            <p>{{ treatmentText(activeRecord) }}</p>
          </section>

          <div class="actions">
            <button :disabled="!!actionId" @click="showPrescription(activeRecord)">
              {{ actionId === `prescription-${recordIdOf(activeRecord)}` ? '加载中...' : '查看处方' }}
            </button>
            <button :disabled="!!actionId" @click="showReport(activeRecord)">
              {{ actionId === `report-${recordIdOf(activeRecord)}` ? '加载中...' : '检查报告' }}
            </button>
          </div>

        </template>

        <div v-else class="empty-detail">
          <h2>请选择一条记录</h2>
          <p>左侧选择诊疗记录后可查看诊断、治疗建议、处方和检查报告。</p>
        </div>
      </aside>
    </section>

    <div v-if="records.length > 0" class="pager">
      <button :disabled="pager.page <= 1 || loading" @click="prevPage">上一页</button>
      <span>第 {{ pager.page }} / {{ pages }} 页</span>
      <button :disabled="pager.page >= pages || loading" @click="nextPage">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.records-view {
  min-height: 100%;
  padding: 16px 16px calc(var(--bottom-nav-h) + 24px);
}
.hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-radius: var(--radius-lg);
  padding: 20px;
  background: linear-gradient(135deg, #0e8f86, #16a34a);
  color: #fff;
  box-shadow: var(--shadow);
}
.eyebrow {
  font-size: 12px;
  opacity: 0.82;
}
.hero h1 {
  margin-top: 3px;
  font-size: 24px;
  line-height: 1.2;
}
.hero p:last-child {
  max-width: 29rem;
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.7;
  opacity: 0.9;
}
.refresh-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  border: 1px solid rgba(255, 255, 255, 0.42);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  transition: background 0.18s ease, opacity 0.18s ease;
}
.refresh-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.25);
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 14px 0;
}
.summary-card,
.record-card,
.empty-state,
.skeleton-card,
.detail-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}
.summary-card {
  min-height: 78px;
  padding: 12px;
}
.summary-card span {
  display: block;
  font-size: 12px;
  color: var(--color-ink-3);
}
.summary-card strong {
  display: block;
  margin-top: 6px;
  font-size: 24px;
  line-height: 1;
  color: var(--color-ink);
  font-variant-numeric: tabular-nums;
}
.summary-card.success strong {
  color: var(--color-accent);
}
.summary-card.warn strong {
  color: var(--color-warn);
}
.summary-card.info strong {
  color: var(--color-info);
}
.notice {
  margin-bottom: 12px;
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  font-size: 12.5px;
}
.notice.error {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}
.notice.success {
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
}
.records-layout {
  display: grid;
  grid-template-columns: minmax(280px, 0.95fr) minmax(320px, 1.2fr);
  gap: 14px;
  align-items: start;
}
.examination-panel {
  margin-bottom: 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 16px;
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}
.examination-head,
.patient-examination-summary,
.examination-title-row,
.form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.examination-head { align-items: flex-start; }
.examination-head h2 { margin-top: 2px; color: var(--color-ink); font-size: 18px; }
.examination-head > div > p:last-child { margin-top: 5px; color: var(--color-ink-3); font-size: 12px; }
.panel-refresh,
.result-entry-btn,
.form-actions button {
  min-height: 42px;
  border: 1.5px solid var(--color-primary);
  border-radius: var(--radius-sm);
  padding: 0 14px;
  background: var(--color-primary);
  color: #fff;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
}
.panel-refresh { flex-shrink: 0; background: #fff; color: var(--color-primary); }
.patient-examination-list { display: grid; gap: 10px; margin-top: 14px; }
.patient-examination-card { border: 1px solid var(--color-border); border-radius: var(--radius-sm); padding: 14px; background: var(--color-primary-soft); }
.patient-examination-summary > div { min-width: 0; }
.examination-title-row { justify-content: flex-start; flex-wrap: wrap; }
.examination-title-row h3 { color: var(--color-ink); font-size: 15px; }
.patient-examination-summary p,
.patient-examination-summary small { display: block; margin-top: 5px; color: var(--color-ink-3); font-size: 12px; overflow-wrap: anywhere; }
.patient-examination-summary small { color: var(--color-danger); font-weight: 700; }
.examination-status { border-radius: 999px; padding: 4px 8px; background: var(--color-warn-soft); color: var(--color-warn); font-size: 11px; font-weight: 800; }
.examination-status.status-待医生确认 { background: var(--color-info-soft); color: var(--color-info); }
.examination-status.status-完成 { background: var(--color-accent-soft); color: var(--color-accent); }
.examination-status.status-需补充 { background: var(--color-danger-soft); color: var(--color-danger); }
.result-entry-btn { flex-shrink: 0; }
.result-entry-btn.secondary { background: #fff; color: var(--color-primary); }
.patient-result-form { display: grid; gap: 12px; margin-top: 14px; border-top: 1px solid var(--color-border); padding-top: 14px; }
.patient-result-form label span { display: block; margin-bottom: 6px; color: var(--color-ink-2); font-size: 13px; font-weight: 800; }
.patient-result-form label em { color: var(--color-danger); font-style: normal; }
.patient-result-form textarea { width: 100%; resize: vertical; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); padding: 11px 12px; background: #fff; color: var(--color-ink); font: inherit; line-height: 1.6; outline: none; }
.patient-result-form textarea:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(14, 143, 134, .12); }
.form-tip { color: var(--color-ink-3); font-size: 12px; line-height: 1.6; }
.form-actions { justify-content: flex-end; }
.form-actions button:first-child { border-color: var(--color-border); background: #fff; color: var(--color-ink-2); }
.examination-state { margin-top: 14px; border-radius: var(--radius-sm); padding: 24px; background: var(--color-bg); color: var(--color-ink-3); text-align: center; font-size: 13px; }
.record-list {
  display: grid;
  gap: 10px;
}
.record-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  min-height: 96px;
  padding: 14px;
  text-align: left;
  color: inherit;
  transition: border-color 0.18s ease, background 0.18s ease;
}
.record-card.active {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
}
.record-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  border-radius: 14px;
  background: var(--color-bg);
  color: var(--color-primary);
}
.record-main {
  min-width: 0;
  flex: 1;
}
.record-title {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}
.record-title strong {
  color: var(--color-ink);
  font-size: 15px;
}
.record-title em,
.status-tag {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 3px 8px;
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-style: normal;
  font-size: 11.5px;
  font-weight: 800;
}
.record-meta,
.record-diagnosis {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-ink-3);
  overflow-wrap: anywhere;
}
.record-diagnosis {
  color: var(--color-ink-2);
  font-weight: 700;
}
.empty-state,
.empty-detail {
  display: grid;
  justify-items: center;
  gap: 8px;
  padding: 36px 18px;
  text-align: center;
  color: var(--color-ink-3);
}
.empty-state svg {
  color: var(--color-primary);
}
.empty-state h2,
.empty-detail h2 {
  font-size: 16px;
  color: var(--color-ink);
}
.empty-state p,
.empty-detail p {
  max-width: 24rem;
  font-size: 13px;
}
.skeleton-card {
  display: grid;
  gap: 12px;
  padding: 18px;
}
.skeleton-card span {
  height: 16px;
  border-radius: 999px;
  background: linear-gradient(90deg, #e0f4f2, #f5fffd, #e0f4f2);
}
.skeleton-card span:nth-child(1) {
  width: 54%;
}
.skeleton-card span:nth-child(2) {
  width: 82%;
}
.skeleton-card span:nth-child(3) {
  width: 68%;
}
.detail-panel {
  padding: 16px;
}
.detail-head,
.sub-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.detail-head h2 {
  margin-top: 2px;
  font-size: 18px;
  color: var(--color-ink);
}
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}
.detail-grid div {
  min-width: 0;
  border-radius: var(--radius-sm);
  padding: 10px;
  background: var(--color-bg);
}
.detail-grid dt,
.raw-detail dt {
  font-size: 11.5px;
  color: var(--color-ink-3);
}
.detail-grid dd,
.raw-detail dd {
  margin-top: 3px;
  color: var(--color-ink);
  font-size: 12.5px;
  font-weight: 700;
  overflow-wrap: anywhere;
}
.clinical-block {
  margin-top: 14px;
  border-radius: var(--radius-sm);
  padding: 13px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
}
.clinical-block h3 {
  font-size: 13px;
  color: var(--color-ink);
}
.clinical-block p {
  margin-top: 7px;
  color: var(--color-ink-2);
  font-size: 13px;
  line-height: 1.75;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}
.actions button,
.pager button,
.sub-head button {
  min-height: 40px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-ink-2);
  font-size: 13px;
  font-weight: 800;
  font-family: var(--font-sans);
  transition: opacity 0.18s ease, background 0.18s ease;
}
.actions button {
  flex: 1 1 130px;
}
.actions button:first-child {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}
.sub-detail {
  margin-top: 14px;
  border-radius: var(--radius);
  padding: 14px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
}
.sub-head h3 {
  color: var(--color-ink);
  font-size: 15px;
}
.sub-head button {
  min-height: 34px;
  padding: 0 11px;
}
.raw-detail {
  display: grid;
  gap: 9px;
  margin-top: 12px;
}
.raw-detail div {
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 9px;
}
.raw-detail div:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}
.pager button {
  padding: 0 14px;
}
.pager span {
  min-width: 96px;
  text-align: center;
  font-size: 12.5px;
  color: var(--color-ink-3);
}
@media (max-width: 860px) {
  .records-layout {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 620px) {
  .records-view {
    padding-inline: 12px;
  }
  .hero {
    padding: 18px;
  }
  .summary-grid,
  .detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .examination-head,
  .patient-examination-summary { align-items: stretch; flex-direction: column; }
  .panel-refresh,
  .result-entry-btn { width: 100%; }
}
@media (max-width: 420px) {
  .summary-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
