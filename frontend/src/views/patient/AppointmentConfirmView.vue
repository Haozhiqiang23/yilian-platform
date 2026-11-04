<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createAppointment, getDoctor, getProfile } from '@/api/patient'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const errorMsg = ref('')
const patient = ref(null)
const doctor = ref(null)

const sourceId = computed(() => route.query.sourceId || '')
const doctorId = computed(() => route.query.doctorId || '')
const visitDate = computed(() => route.query.date || '--')
const startTime = computed(() => route.query.startTime || '')
const endTime = computed(() => route.query.endTime || '')
const visitTime = computed(() => {
  if (startTime.value && endTime.value) return `${startTime.value} - ${endTime.value}`
  return route.query.timePeriod || '--'
})

const sourceExpired = computed(() => {
  if (!visitDate.value || !endTime.value || visitDate.value === '--') return false
  const end = new Date(`${visitDate.value}T${endTime.value}`)
  return !Number.isNaN(end.getTime()) && Date.now() >= end.getTime()
})
const canSubmit = computed(() => Boolean(sourceId.value) && !sourceExpired.value && !submitting.value)

async function loadConfirmInfo() {
  loading.value = true
  errorMsg.value = ''
  try {
    const [profileData, doctorData] = await Promise.all([
      getProfile(),
      doctorId.value ? getDoctor(doctorId.value) : Promise.resolve(null),
    ])
    patient.value = profileData
    doctor.value = doctorData
  } catch (e) {
    errorMsg.value = e.message || '预约信息加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function maskPhone(phone) {
  const text = String(phone || '')
  if (text.length < 7) return text || '--'
  return `${text.slice(0, 3)}****${text.slice(-4)}`
}

function departmentText(item) {
  return item?.department || item?.departmentName || '--'
}

function priceText() {
  const price = route.query.price || route.query.fee
  return price ? `¥${Number(price).toFixed(2)}` : '费用待返回'
}

function appointmentErrorText(e) {
  const message = e?.message || ''
  if (message.includes('SOURCE_APPOINTMENT_EXPIRED') || message.includes('不能预约')) return '该号源已结束，不能再预约'
  if (message.includes('SOURCE_NOT_ENOUGH')) return '当前号源不足，请返回医生详情重新选择'
  if (message.includes('APPOINTMENT_EXIST')) return '已存在相同时间预约，请勿重复提交'
  return message || '预约提交失败，请稍后重试'
}

async function submitAppointment() {
  if (!sourceId.value) {
    errorMsg.value = '缺少 sourceId，请返回医生详情重新选择号源'
    return
  }
  submitting.value = true
  errorMsg.value = ''
  try {
    const result = await createAppointment({ sourceId: Number(sourceId.value) })
    const appointmentId = result?.appointmentId || result?.id
    if (!appointmentId) {
      errorMsg.value = '预约已提交，但后端未返回 appointmentId'
      return
    }
    router.push(`/patient/appointments/${appointmentId}/pay`)
  } catch (e) {
    errorMsg.value = appointmentErrorText(e)
  } finally {
    submitting.value = false
  }
}

onMounted(loadConfirmInfo)
</script>

<template>
  <div class="confirm-view">
    <section class="page-hero" aria-labelledby="confirm-title">
      <button class="back-btn" type="button" aria-label="返回医生详情" @click="router.back()">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M19 12H5" />
          <path d="m12 19-7-7 7-7" />
        </svg>
      </button>
      <div>
        <p class="eyebrow">Appointment Confirm</p>
        <h1 id="confirm-title">预约确认</h1>
        <p>请确认患者、医生和就诊时段，提交后将为您保留号源。</p>
      </div>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>
    <p v-else-if="sourceExpired" class="notice danger" role="alert">该号源已结束，不能再预约</p>

    <section v-if="loading" class="state-card">正在请求预约确认信息...</section>

    <template v-else>
      <section class="info-card" aria-label="患者信息">
        <div class="card-head">
          <h2>患者信息</h2>
          <span>实名信息</span>
        </div>
        <dl>
          <div>
            <dt>姓名</dt>
            <dd>{{ patient?.name || '--' }}</dd>
          </div>
          <div>
            <dt>手机号</dt>
            <dd>{{ maskPhone(patient?.phone) }}</dd>
          </div>
          <div>
            <dt>地址</dt>
            <dd>{{ patient?.address || '暂未填写' }}</dd>
          </div>
        </dl>
      </section>

      <section class="info-card" aria-label="预约信息">
        <div class="card-head">
          <h2>预约信息</h2>
          <span>号源编号 {{ sourceId || '--' }}</span>
        </div>
        <dl>
          <div>
            <dt>医生</dt>
            <dd>{{ doctor?.name || '--' }}</dd>
          </div>
          <div>
            <dt>科室</dt>
            <dd>{{ departmentText(doctor) }}</dd>
          </div>
          <div>
            <dt>职称</dt>
            <dd>{{ doctor?.title || '--' }}</dd>
          </div>
          <div>
            <dt>就诊日期</dt>
            <dd>{{ visitDate }}</dd>
          </div>
          <div>
            <dt>就诊时段</dt>
            <dd>{{ visitTime }}</dd>
          </div>
          <div>
            <dt>预约费用</dt>
            <dd>{{ priceText() }}</dd>
          </div>
        </dl>
      </section>

      <section class="flow-card" aria-label="预约流程">
        <h2>提交后流程</h2>
        <div class="flow-steps">
          <span>锁定号源</span>
          <span>创建预约</span>
          <span>扣减号源</span>
          <span>进入支付</span>
        </div>
      </section>
    </template>

    <div class="bottom-bar">
      <div class="selected-text">
            <span>预约操作</span>
            <strong>确认后进入支付</strong>
      </div>
      <button class="submit-btn" :disabled="!canSubmit" type="button" @click="submitAppointment">
        {{ submitting ? '提交中...' : sourceExpired ? '号源已结束' : '提交预约' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.confirm-view {
  min-height: 100%;
  padding: 14px 16px 96px;
}

.page-hero,
.info-card,
.flow-card,
.state-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}

.page-hero {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
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
  font-size: 22px;
  font-weight: 800;
}

.page-hero p {
  margin-top: 5px;
  color: var(--color-ink-3);
  font-size: 12.5px;
}

.notice {
  margin-top: 12px;
  border-radius: var(--radius-sm);
  padding: 11px 13px;
  font-size: 13px;
  font-weight: 700;
}

.notice.danger {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.state-card {
  margin-top: 12px;
  padding: 28px 16px;
  color: var(--color-ink-3);
  text-align: center;
  font-size: 13px;
}

.info-card,
.flow-card {
  margin-top: 12px;
  padding: 16px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 10px;
}

.card-head h2,
.flow-card h2 {
  color: var(--color-ink);
  font-size: 17px;
  font-weight: 800;
}

.card-head span {
  border-radius: 999px;
  padding: 4px 9px;
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
  font-size: 11.5px;
  font-weight: 800;
}

dl {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

dl div {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

dt {
  flex-shrink: 0;
  color: var(--color-ink-3);
  font-size: 12.5px;
  font-weight: 700;
}

dd {
  min-width: 0;
  color: var(--color-ink);
  text-align: right;
  font-size: 13px;
  font-weight: 800;
}

.flow-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.flow-steps span {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 10px 8px;
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
  text-align: center;
  font-size: 12px;
  font-weight: 800;
}

.bottom-bar {
  position: fixed;
  left: 50%;
  bottom: var(--bottom-nav-h, 64px);
  z-index: 20;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  width: min(480px, 100%);
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  transform: translateX(-50%);
  border-top: 1px solid var(--color-border);
  background: rgba(255, 255, 255, 0.96);
}

.selected-text {
  min-width: 0;
}

.selected-text span {
  display: block;
  color: var(--color-ink-3);
  font-size: 11.5px;
}

.selected-text strong {
  display: block;
  overflow: hidden;
  color: var(--color-ink);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.submit-btn {
  min-height: 42px;
  border: none;
  border-radius: var(--radius-sm);
  padding: 0 18px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-primary-deep));
  color: #fff;
  font-size: 14px;
  font-weight: 800;
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 620px) {
  .card-head,
  dl div,
  .bottom-bar {
    align-items: stretch;
  }

  .card-head,
  .bottom-bar {
    grid-template-columns: 1fr;
  }

  .card-head,
  dl div {
    flex-direction: column;
  }

  dd {
    text-align: left;
  }

  .flow-steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
