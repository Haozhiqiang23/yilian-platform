<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createExamination, createPrescription, finishAppointment, getDoctorExaminations, getDoctorPrescriptions, getDoctorQueues, getPatientRecords, startAppointment } from '@/api/doctor'
import ListPager from '@/components/common/ListPager.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref('')
const errorMsg = ref('')
const successMsg = ref('')
const queues = ref([])
const selectedQueue = ref(null)
const createdRecordId = ref('')
const createdPrescriptionId = ref('')
const createdExaminationId = ref('')
const prescriptions = ref([])
const examinations = ref([])
const prescriptionPage = ref(1)
const examinationPage = ref(1)
const itemPageSize = 10
const pagedPrescriptions = computed(() => prescriptions.value.slice((prescriptionPage.value - 1) * itemPageSize, prescriptionPage.value * itemPageSize))
const pagedExaminations = computed(() => examinations.value.slice((examinationPage.value - 1) * itemPageSize, examinationPage.value * itemPageSize))

const form = reactive({
  diagnosis: '',
  treatment: '',
  medicineInfo: '',
  usageInfo: '',
  amount: '',
  examItemName: '',
})

const fieldErrors = reactive({
  diagnosis: '',
  treatment: '',
  medicineInfo: '',
  usageInfo: '',
  amount: '',
  examItemName: '',
})

const appointmentId = computed(() => String(route.params.appointmentId || ''))

const currentQueue = computed(() =>
  queues.value.find((item) => String(item.appointmentId || item.id || '') === appointmentId.value) || selectedQueue.value,
)

const currentStatus = computed(() => currentQueue.value?.status || currentQueue.value?.queueStatus || currentQueue.value?.appointmentStatus || '状态待确认')
const isCompleted = computed(() => currentStatus.value === '已完成')
const canStart = computed(() => currentStatus.value === '已签到')
const canSubmitRecord = computed(() => currentStatus.value === '接诊中' && !createdRecordId.value)

function normalizeResponseList(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data?.rows)) return data.rows
  return []
}

function ensureInitialForm(item) {
  createdRecordId.value = item?.medicalRecordId || item?.recordId || createdRecordId.value
  if (item?.diagnosis && !form.diagnosis) form.diagnosis = item.diagnosis
  if (item?.treatment && !form.treatment) form.treatment = item.treatment
}

async function loadExistingRecord(item) {
  if (createdRecordId.value || !item?.patientId || currentStatus.value !== '已完成') return
  try {
    const data = await getPatientRecords(item.patientId)
    const records = normalizeResponseList(data)
    const record = records.find((entry) =>
      String(entry.appointmentId || '') === appointmentId.value,
    )
    if (!record) return

    createdRecordId.value = record.medicalRecordId || record.recordId || record.id || ''
    createdPrescriptionId.value = record.prescriptionId || ''
    createdExaminationId.value = record.examinationId || ''
    if (record.diagnosis) form.diagnosis = record.diagnosis
    if (record.treatment) form.treatment = record.treatment
  } catch {
    // 队列数据已足够展示患者；历史记录回查失败时不清空当前页面。
  }
}

async function loadAppointment() {
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const data = await getDoctorQueues()
    queues.value = normalizeResponseList(data)
    selectedQueue.value = queues.value.find((item) => String(item.appointmentId || item.id || '') === appointmentId.value) || null
    ensureInitialForm(selectedQueue.value)
    await loadExistingRecord(selectedQueue.value)
    await loadCreatedItems()
  } catch (e) {
    errorMsg.value = e.message || '暂时无法加载接诊信息，请稍后重试'
    queues.value = []
    selectedQueue.value = null
  } finally {
    loading.value = false
  }
}

async function loadCreatedItems() {
  if (!createdRecordId.value) {
    prescriptions.value = []
    examinations.value = []
    return
  }
  const recordId = String(createdRecordId.value)
  try {
    const [prescriptionData, examinationData] = await Promise.all([
      getDoctorPrescriptions({ page: 1, size: 100 }),
      getDoctorExaminations({ page: 1, size: 100 }),
    ])
    prescriptions.value = normalizeResponseList(prescriptionData).filter(
      (item) => String(item.medicalRecordId || '') === recordId,
    )
    examinations.value = normalizeResponseList(examinationData).filter(
      (item) => String(item.medicalRecordId || '') === recordId,
    )
    prescriptionPage.value = Math.min(prescriptionPage.value, Math.max(1, Math.ceil(prescriptions.value.length / itemPageSize)))
    examinationPage.value = Math.min(examinationPage.value, Math.max(1, Math.ceil(examinations.value.length / itemPageSize)))
  } catch (e) {
    errorMsg.value = e.message || '已保存诊疗内容，但暂时无法刷新开具记录'
  }
}

function validateBasic() {
  fieldErrors.diagnosis = ''
  fieldErrors.treatment = ''

  if (!form.diagnosis.trim()) fieldErrors.diagnosis = '请填写初步诊断'
  if (!form.treatment.trim()) fieldErrors.treatment = '请填写治疗建议'

  return !fieldErrors.diagnosis && !fieldErrors.treatment
}

function validatePrescription() {
  fieldErrors.medicineInfo = ''
  fieldErrors.usageInfo = ''
  fieldErrors.amount = ''

  if (!form.medicineInfo.trim()) fieldErrors.medicineInfo = '请填写药品信息'
  if (!form.usageInfo.trim()) fieldErrors.usageInfo = '请填写用法用量'
  if (form.amount === '' || Number.isNaN(Number(form.amount))) fieldErrors.amount = '请填写处方金额'

  return !fieldErrors.medicineInfo && !fieldErrors.usageInfo && !fieldErrors.amount
}

function validateExamination() {
  fieldErrors.examItemName = ''
  if (!form.examItemName.trim()) fieldErrors.examItemName = '请填写检查项目'
  return !fieldErrors.examItemName
}

async function runAction(key, task, okText) {
  if (saving.value) return
  saving.value = key
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const result = await task()
    if (result?.medicalRecordId) createdRecordId.value = result.medicalRecordId
    if (result?.prescriptionId) createdPrescriptionId.value = result.prescriptionId
    if (result?.examinationId) createdExaminationId.value = result.examinationId
    await loadAppointment()
    successMsg.value = okText
    return result
  } catch (e) {
    errorMsg.value = e.message || '保存诊疗记录失败，请稍后重试'
    return null
  } finally {
    saving.value = ''
  }
}

async function startCurrent() {
  if (!appointmentId.value) return
  await runAction('start', () => startAppointment(appointmentId.value), '已开始接诊')
}

async function submitRecord() {
  if (!appointmentId.value || !validateBasic()) return
  await runAction(
    'record',
    () =>
      finishAppointment(appointmentId.value, {
        diagnosis: form.diagnosis.trim(),
        treatment: form.treatment.trim(),
      }),
    '诊疗记录已提交',
  )
}

async function submitPrescription() {
  if (!createdRecordId.value || !validatePrescription()) return
  const result = await runAction(
    'prescription',
    () =>
      createPrescription({
        medicalRecordId: Number(createdRecordId.value),
        medicineInfo: form.medicineInfo.trim(),
        usageInfo: form.usageInfo.trim(),
        amount: Number(form.amount),
      }),
    '处方创建成功，已显示在下方记录中',
  )
  if (result) {
    form.medicineInfo = ''
    form.usageInfo = ''
    form.amount = ''
  }
}

async function submitExamination() {
  if (!createdRecordId.value || !validateExamination()) return
  const result = await runAction(
    'examination',
    () =>
      createExamination({
        medicalRecordId: Number(createdRecordId.value),
        itemName: form.examItemName.trim(),
      }),
    '检查申请创建成功，已显示在下方记录中',
  )
  if (result) form.examItemName = ''
}

onMounted(loadAppointment)
</script>

<template>
  <div class="record-editor-view">
    <section class="page-hero" aria-labelledby="record-title">
      <button class="back-btn" type="button" aria-label="返回患者列表" @click="router.push('/doctor/queues')">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M19 12H5" />
          <path d="m12 19-7-7 7-7" />
        </svg>
      </button>
      <div>
        <p class="eyebrow">Clinical Record</p>
        <h1 id="record-title">诊疗记录填写</h1>
        <p>接诊过程中填写诊断和治疗建议，完成后可继续生成处方和检查申请。</p>
      </div>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>
    <p v-if="successMsg" class="notice success" role="status">{{ successMsg }}</p>

    <section class="summary-grid" aria-label="患者摘要">
      <article class="summary-card">
        <span>预约编号</span>
        <strong>{{ appointmentId || '--' }}</strong>
      </article>
      <article class="summary-card">
        <span>当前状态</span>
        <strong>{{ currentStatus }}</strong>
      </article>
      <article class="summary-card">
        <span>诊疗记录</span>
        <strong>{{ createdRecordId || '--' }}</strong>
      </article>
      <article class="summary-card">
        <span>处方 / 检查</span>
        <strong>{{ prescriptions.length }} / {{ examinations.length }}</strong>
      </article>
    </section>

    <section class="workspace-grid">
      <article class="panel">
        <div class="panel-head">
          <div>
            <h2>接诊与诊疗记录</h2>
            <p>在这里完成正式诊疗记录；记录保存后，本次接诊同步完成。</p>
          </div>
          <button class="refresh-btn" :disabled="loading" type="button" @click="loadAppointment">
            {{ loading ? '刷新中...' : '刷新患者' }}
          </button>
        </div>

        <dl class="info-grid">
          <div>
            <dt>预约状态</dt>
            <dd>{{ currentStatus }}</dd>
          </div>
          <div>
            <dt>患者姓名</dt>
            <dd>{{ currentQueue?.patientName || '患者姓名未提供' }}</dd>
          </div>
          <div>
            <dt>患者ID</dt>
            <dd>{{ currentQueue?.patientId || '--' }}</dd>
          </div>
          <div>
            <dt>排队号</dt>
            <dd>{{ currentQueue?.queueNo || currentQueue?.queueNumber || '--' }}</dd>
          </div>
        </dl>

        <div class="form-block">
          <h3>诊疗记录</h3>
          <label>
            <span>初步诊断</span>
            <textarea v-model="form.diagnosis" :disabled="isCompleted" rows="3" placeholder="例如：上呼吸道感染"></textarea>
            <small v-if="fieldErrors.diagnosis">{{ fieldErrors.diagnosis }}</small>
          </label>
          <label>
            <span>治疗建议</span>
            <textarea v-model="form.treatment" :disabled="isCompleted" rows="4" placeholder="例如：多喝水，注意休息"></textarea>
            <small v-if="fieldErrors.treatment">{{ fieldErrors.treatment }}</small>
          </label>
          <div class="button-row">
            <button v-if="canStart" class="primary-btn" :disabled="!!saving" type="button" @click="startCurrent">
              {{ saving === 'start' ? '开始中...' : '开始接诊' }}
            </button>
            <button v-if="canSubmitRecord" class="secondary-btn" :disabled="!!saving" type="button" @click="submitRecord">
              {{ saving === 'record' ? '提交中...' : '保存记录并完成接诊' }}
            </button>
            <span v-if="isCompleted" class="completed-note">诊疗记录已保存，本次接诊已完成</span>
          </div>
        </div>
      </article>

      <article class="panel">
        <div class="panel-head">
          <div>
            <h2>处方与检查</h2>
            <p>基于诊疗记录编号继续创建处方和检查申请。</p>
          </div>
          <span class="api-badge">诊疗后可开具</span>
        </div>

        <div class="form-block">
          <h3>处方</h3>
          <label>
            <span>药品信息</span>
            <textarea v-model="form.medicineInfo" rows="3" placeholder="药品名称、规格等"></textarea>
            <small v-if="fieldErrors.medicineInfo">{{ fieldErrors.medicineInfo }}</small>
          </label>
          <label>
            <span>用法用量</span>
            <textarea v-model="form.usageInfo" rows="3" placeholder="每日三次，饭后服用"></textarea>
            <small v-if="fieldErrors.usageInfo">{{ fieldErrors.usageInfo }}</small>
          </label>
          <label>
            <span>金额</span>
            <input v-model="form.amount" type="number" min="0" step="0.01" placeholder="50" />
            <small v-if="fieldErrors.amount">{{ fieldErrors.amount }}</small>
          </label>
          <button class="primary-btn" :disabled="saving === 'prescription' || !createdRecordId" type="button" @click="submitPrescription">
            {{ saving === 'prescription' ? '提交中...' : '生成处方' }}
          </button>
        </div>

        <div class="form-block">
          <h3>检查申请</h3>
          <label>
            <span>检查项目</span>
            <input v-model="form.examItemName" type="text" placeholder="如：血常规、胸部CT" />
            <small v-if="fieldErrors.examItemName">{{ fieldErrors.examItemName }}</small>
          </label>
          <button class="secondary-btn" :disabled="saving === 'examination' || !createdRecordId" type="button" @click="submitExamination">
            {{ saving === 'examination' ? '提交中...' : '创建检查申请' }}
          </button>
        </div>
      </article>
    </section>

    <section class="created-panel" aria-labelledby="created-items-title">
      <div class="panel-head">
        <div>
          <h2 id="created-items-title">本次已开具内容</h2>
          <p>展示当前诊疗记录下已创建的全部处方和检查申请。</p>
        </div>
        <button class="refresh-btn" :disabled="loading || !createdRecordId" type="button" @click="loadCreatedItems">
          刷新记录
        </button>
      </div>
      <div class="created-grid">
        <div class="created-group">
          <h3>处方 <span>{{ prescriptions.length }}</span></h3>
          <p v-if="!prescriptions.length" class="empty-note">暂未开具处方</p>
          <article v-for="item in pagedPrescriptions" :key="item.prescriptionId" class="created-item">
            <div><strong>{{ item.medicineInfo }}</strong><span>{{ item.status }}</span></div>
            <p>{{ item.usageInfo }}</p>
            <small>处方 #{{ item.prescriptionId }} · ¥{{ item.amount }}</small>
          </article>
          <ListPager v-model:page="prescriptionPage" :page-size="itemPageSize" :total="prescriptions.length" />
        </div>
        <div class="created-group">
          <h3>检查申请 <span>{{ examinations.length }}</span></h3>
          <p v-if="!examinations.length" class="empty-note">暂未创建检查申请</p>
          <article v-for="item in pagedExaminations" :key="item.examinationId" class="created-item">
            <div><strong>{{ item.itemName }}</strong><span>{{ item.status }}</span></div>
            <small>检查申请 #{{ item.examinationId }}</small>
          </article>
          <ListPager v-model:page="examinationPage" :page-size="itemPageSize" :total="examinations.length" />
        </div>
      </div>
    </section>

  </div>
</template>

<style scoped>
.record-editor-view {
  min-height: 100%;
  padding: 20px 18px 28px;
}

.page-hero,
.summary-card,
.panel,
.created-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}

.page-hero {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px;
}

.back-btn {
  display: grid;
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  place-items: center;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fff;
  color: var(--color-primary-deep);
}

.eyebrow {
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 800;
}

.page-hero h1 {
  margin-top: 3px;
  color: var(--color-ink);
  font-size: 24px;
  font-weight: 800;
}

.page-hero p {
  margin-top: 5px;
  color: var(--color-ink-3);
  font-size: 13px;
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
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 700;
}

.summary-card strong {
  display: block;
  margin-top: 6px;
  color: var(--color-ink);
  font-size: 24px;
  line-height: 1.2;
}

.workspace-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 14px;
  align-items: start;
}

.panel,
.created-panel {
  padding: 16px;
}

.created-panel {
  margin-top: 14px;
}

.created-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.created-group {
  min-width: 0;
  border-radius: var(--radius-sm);
  padding: 14px;
  background: var(--color-bg);
}

.created-group h3 {
  color: var(--color-ink);
  font-size: 15px;
}

.created-group h3 span {
  margin-left: 5px;
  color: var(--color-primary);
}

.created-item {
  margin-top: 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 11px 12px;
  background: var(--color-surface);
}

.created-item div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.created-item strong {
  min-width: 0;
  overflow-wrap: anywhere;
  color: var(--color-ink);
  font-size: 13px;
}

.created-item div span {
  flex-shrink: 0;
  color: var(--color-accent);
  font-size: 12px;
  font-weight: 800;
}

.created-item p,
.created-item small,
.empty-note {
  display: block;
  margin-top: 6px;
  color: var(--color-ink-3);
  font-size: 12px;
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

.api-badge {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 6px 10px;
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
  font-size: 12px;
  font-weight: 800;
}

.refresh-btn,
.primary-btn,
.secondary-btn {
  min-height: 40px;
  border-radius: var(--radius-sm);
  padding: 0 14px;
  font-size: 13px;
  font-weight: 800;
}

.refresh-btn,
.secondary-btn {
  border: 1.5px solid var(--color-border-strong);
  background: #fff;
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

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.info-grid div {
  border-radius: var(--radius-sm);
  padding: 12px;
  background: var(--color-primary-soft);
}

.info-grid dt {
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 700;
}

.info-grid dd {
  margin-top: 4px;
  color: var(--color-ink);
  font-size: 13px;
  font-weight: 800;
}

.form-block {
  margin-top: 14px;
  border-top: 1px solid var(--color-border);
  padding-top: 14px;
}

.form-block h3 {
  color: var(--color-ink);
  font-size: 15px;
  font-weight: 800;
}

.form-block label {
  display: block;
  margin-top: 10px;
}

.form-block span {
  display: block;
  margin-bottom: 6px;
  color: var(--color-ink-2);
  font-size: 13px;
  font-weight: 800;
}

.form-block input,
.form-block textarea {
  width: 100%;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 11px 12px;
  background: #fff;
  color: var(--color-ink);
  font: inherit;
  outline: none;
}

.form-block input:focus,
.form-block textarea:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(14, 143, 134, 0.12);
}

.form-block input:disabled,
.form-block textarea:disabled {
  background: var(--color-bg);
  color: var(--color-ink-2);
  cursor: not-allowed;
}

.form-block small {
  display: block;
  margin-top: 5px;
  color: var(--color-danger);
  font-size: 12px;
  font-weight: 700;
}

.button-row {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.button-row button {
  flex: 1;
}

.completed-note {
  flex: 1;
  border-radius: var(--radius-sm);
  padding: 11px 14px;
  background: var(--color-accent-soft);
  color: var(--color-accent);
  text-align: center;
  font-size: 13px;
  font-weight: 800;
}

@media (max-width: 900px) {
  .summary-grid,
  .workspace-grid,
  .created-grid,
  .info-grid {
    grid-template-columns: 1fr;
  }

  .page-hero {
    align-items: stretch;
    flex-direction: column;
  }
}

@media (max-width: 560px) {
  .record-editor-view {
    padding: 16px 12px 24px;
  }

  .page-hero,
  .panel,
  .created-panel {
    padding: 14px;
  }

  .button-row {
    flex-direction: column;
  }
}
</style>
