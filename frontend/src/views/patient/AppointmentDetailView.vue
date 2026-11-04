<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAppointmentDetail } from '@/api/patient'

const route = useRoute()
const router = useRouter()

const appointmentId = computed(() => route.params.id)
const detail = ref(null)
const loading = ref(false)
const errorMsg = ref('')
const flowSteps = ['待支付', '已支付', '已签到', '接诊中', '已完成']

const statusText = computed(() => detail.value?.status || '未知状态')
const statusTip = computed(() => {
  const tips = {
    待支付: '请在支付时限内完成支付，超时后预约可能自动取消。',
    已支付: '预约已支付，请按预约时间提前到院并在规定时间内签到。',
    已签到: '您已完成签到，请前往候诊区域并留意排队叫号。',
    接诊中: '医生正在为您接诊，请配合完成本次诊疗。',
    已完成: '本次就诊已完成，可在诊疗记录中查看相关信息。',
    已取消: '该预约已取消，如仍需就诊请重新预约。',
  }
  return tips[statusText.value] || '请留意预约状态变化，并按页面提示完成后续操作。'
})
const flowIndex = computed(() => {
  const index = flowSteps.indexOf(statusText.value)
  return index >= 0 ? index : -1
})

function amountText(value) {
  return value === undefined || value === null ? '--' : `¥${Number(value).toFixed(2)}`
}

async function loadDetail() {
  loading.value = true
  errorMsg.value = ''
  try {
    detail.value = await getAppointmentDetail(appointmentId.value)
  } catch (e) {
    errorMsg.value = e.message || '预约详情加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)

function goPayment() {
  router.push(`/patient/appointments/${appointmentId.value}/pay`)
}

function goQueue() {
  router.push(`/patient/queue/${appointmentId.value}`)
}
</script>

<template>
  <div class="appointment-detail-view">
    <section class="page-hero" aria-labelledby="detail-title">
      <button class="back-btn" type="button" aria-label="返回我的预约" @click="router.push('/patient/appointments')">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M19 12H5" />
          <path d="m12 19-7-7 7-7" />
        </svg>
      </button>
      <div>
        <p class="eyebrow">Appointment Detail</p>
        <h1 id="detail-title">预约详情</h1>
        <p>展示预约、医生、支付、签到和排队进度。</p>
      </div>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>

    <section v-if="loading" class="waiting-panel" aria-label="正在加载预约详情">
      正在加载预约详情...
    </section>

    <template v-else-if="detail">
    <section class="waiting-panel" aria-label="预约编号">
      <div class="waiting-icon" aria-hidden="true">
        <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="4" width="18" height="18" rx="3" />
          <path d="M16 2v4M8 2v4M3 10h18M8 15h4M8 19h8" />
        </svg>
      </div>
      <span>预约编号</span>
      <strong>{{ appointmentId }}</strong>
      <p>{{ statusTip }}</p>
    </section>

    <section class="detail-card" aria-label="预约详情字段">
      <div class="card-head">
        <h2>详情字段</h2>
        <span class="status-tag">{{ statusText }}</span>
      </div>
      <dl>
        <div>
          <dt>预约状态</dt>
          <dd>{{ statusText }}</dd>
        </div>
        <div>
          <dt>就诊医生</dt>
          <dd>{{ detail.doctorName || '--' }}</dd>
        </div>
        <div>
          <dt>科室诊室</dt>
          <dd>{{ detail.department || '--' }}</dd>
        </div>
        <div>
          <dt>就诊时间</dt>
          <dd>{{ detail.visitDate || '--' }} {{ detail.startTime || '' }}<span v-if="detail.endTime"> - {{ detail.endTime }}</span></dd>
        </div>
        <div>
          <dt>支付金额</dt>
          <dd>{{ amountText(detail.amount ?? detail.fee) }}</dd>
        </div>
      </dl>
    </section>

    <section class="flow-card" aria-label="预约流程">
      <h2>预约流程</h2>
      <ol>
        <li v-for="(step, index) in flowSteps" :key="step" :class="{ active: index <= flowIndex }">
          <span aria-hidden="true"></span>
          {{ step }}
        </li>
      </ol>
    </section>

    </template>

    <div class="bottom-bar">
      <button class="secondary-btn" type="button" @click="goQueue">查看排队</button>
      <button class="primary-btn" type="button" @click="goPayment">去支付</button>
    </div>
  </div>
</template>

<style scoped>
.appointment-detail-view {
  min-height: 100%;
  padding: 14px 16px 96px;
}

.page-hero,
.waiting-panel,
.detail-card,
.flow-card,
.api-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
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

.waiting-panel {
  display: grid;
  justify-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 28px 18px;
  text-align: center;
}

.waiting-icon {
  display: grid;
  width: 62px;
  height: 62px;
  place-items: center;
  border-radius: var(--radius);
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
}

.waiting-panel span {
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 800;
}

.waiting-panel strong {
  color: var(--color-primary-deep);
  font-size: 28px;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.waiting-panel p,
.api-card p {
  max-width: 34rem;
  color: var(--color-ink-3);
  font-size: 12.5px;
  line-height: 1.7;
}

.detail-card,
.flow-card,
.api-card {
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
.flow-card h2,
.api-card h2 {
  color: var(--color-ink);
  font-size: 17px;
  font-weight: 800;
}

.status-tag {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 5px 10px;
  background: var(--color-warn-soft);
  color: var(--color-warn);
  font-size: 12px;
  font-weight: 800;
}

dl {
  display: grid;
  gap: 12px;
  margin-top: 12px;
}

dl div {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 10px;
}

dl div:last-child {
  border-bottom: none;
  padding-bottom: 0;
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

.flow-card ol {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
  padding: 0;
  list-style: none;
}

.flow-card li {
  display: grid;
  gap: 7px;
  justify-items: center;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 10px 6px;
  color: var(--color-ink-3);
  text-align: center;
  font-size: 12px;
  font-weight: 800;
}

.flow-card li span {
  width: 10px;
  height: 10px;
  border: 2px solid var(--color-border-strong);
  border-radius: 50%;
}

.flow-card li.active {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
}

.flow-card li.active span {
  border-color: var(--color-primary);
  background: var(--color-primary);
}

.bottom-bar {
  position: fixed;
  left: 50%;
  bottom: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: minmax(120px, 0.45fr) minmax(160px, 1fr);
  gap: 10px;
  width: min(480px, 100%);
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  transform: translateX(-50%);
  border-top: 1px solid var(--color-border);
  background: rgba(255, 255, 255, 0.96);
}

.secondary-btn,
.primary-btn {
  min-height: 42px;
  border-radius: var(--radius-sm);
  padding: 0 14px;
  font-size: 14px;
  font-weight: 800;
}

.secondary-btn {
  border: 1.5px solid var(--color-border-strong);
  background: #fff;
  color: var(--color-primary-deep);
}

.primary-btn {
  border: none;
  background: linear-gradient(90deg, var(--color-primary), var(--color-primary-deep));
  color: #fff;
}

@media (max-width: 720px) {
  .flow-card ol {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 520px) {
  dl div,
  .bottom-bar {
    grid-template-columns: 1fr;
  }

  dl div {
    flex-direction: column;
  }

  dd {
    text-align: left;
  }
}
</style>
