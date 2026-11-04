<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createPayment, getPayment, getPaymentByAppointment, pay } from '@/api/patient'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const paying = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const payment = ref(null)
const secondsLeft = ref(15 * 60)
let timer = null

const appointmentId = computed(() => route.params.id)
const paymentId = computed(() => payment.value?.paymentId || payment.value?.id || '')
const isPaid = computed(() => statusText(payment.value?.status) === '支付成功')
const isClosed = computed(() => statusText(payment.value?.status) === '已关闭')
const isTimeout = computed(() => secondsLeft.value <= 0)
const canPay = computed(() => Boolean(paymentId.value) && !isPaid.value && !isClosed.value && !isTimeout.value && !paying.value)

const minutesText = computed(() => {
  const minutes = Math.floor(secondsLeft.value / 60)
  const seconds = secondsLeft.value % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

async function initPayment() {
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const data = await createPayment({ appointmentId: Number(appointmentId.value) })
    payment.value = data
    startTimer()
  } catch (e) {
    if (String(e?.message || '').includes('PAYMENT_EXIST') || String(e?.message || '').includes('已存在支付订单')) {
      try {
        payment.value = await getPaymentByAppointment(appointmentId.value)
        startTimer()
        return
      } catch (queryError) {
        errorMsg.value = paymentErrorText(queryError, '已有支付订单查询失败')
      }
    } else {
      errorMsg.value = paymentErrorText(e, '创建支付订单失败')
    }
  } finally {
    loading.value = false
  }
}

async function refreshPayment() {
  if (!paymentId.value) {
    errorMsg.value = '未获取到支付订单，请返回预约列表重新进入'
    return
  }
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    payment.value = await getPayment(paymentId.value)
    startTimer()
  } catch (e) {
    errorMsg.value = paymentErrorText(e, '支付订单查询失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function submitPay() {
  if (!canPay.value) return
  paying.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const result = await pay(paymentId.value)
    payment.value = {
      ...payment.value,
      ...result,
      status: result?.status || '支付成功',
    }
    successMsg.value = paymentErrorText(result, '支付成功，预约状态已更新')
    stopTimer()
  } catch (e) {
    const text = paymentErrorText(e, '支付失败，请稍后重试')
    if (text.includes('已支付') || text.includes('已完成')) {
      payment.value = { ...payment.value, status: '支付成功' }
      successMsg.value = '订单已完成支付'
      stopTimer()
    } else {
      errorMsg.value = text
    }
  } finally {
    paying.value = false
  }
}

function paymentErrorText(value, fallback) {
  const message = value?.message || ''
  if (message.includes('PAYMENT_ALREADY_SUCCESS')) return '订单已支付，请勿重复提交'
  if (message.includes('PAYMENT_NOT_EXIST')) return '支付订单不存在，请返回预约列表重新进入'
  return message || fallback
}

function statusText(status) {
  const map = {
    PENDING: '待支付',
    WAITING: '待支付',
    SUCCESS: '支付成功',
    PAID: '支付成功',
    CLOSED: '已关闭',
  }
  return map[status] || status || '待支付'
}

function statusTone(status) {
  if (statusText(status) === '支付成功') return 'success'
  if (statusText(status) === '已关闭') return 'danger'
  return 'warning'
}

function amountText(amount) {
  return amount === undefined || amount === null ? '--' : `¥${Number(amount).toFixed(2)}`
}

function startTimer() {
  stopTimer()
  updateSecondsLeft()
  if (secondsLeft.value <= 0 || isPaid.value || isClosed.value) {
    if (secondsLeft.value <= 0 && paymentId.value && !isPaid.value && !isClosed.value) refreshPayment()
    return
  }
  timer = window.setInterval(() => {
    updateSecondsLeft()
    if (secondsLeft.value <= 0) {
      stopTimer()
      if (!isPaid.value) errorMsg.value = '支付已超时，订单已取消，号源已释放'
      if (paymentId.value && !isPaid.value) refreshPayment()
    }
  }, 1000)
}

function updateSecondsLeft() {
  const expiresAt = payment.value?.expiresAt
  if (!expiresAt) {
    secondsLeft.value = 0
    return
  }
  const deadline = new Date(expiresAt).getTime()
  secondsLeft.value = Number.isFinite(deadline)
    ? Math.max(0, Math.ceil((deadline - Date.now()) / 1000))
    : 0
}

function stopTimer() {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
}

onMounted(initPayment)
onBeforeUnmount(stopTimer)
</script>

<template>
  <div class="payment-view">
    <section class="page-hero" aria-labelledby="payment-title">
      <button class="back-btn" type="button" aria-label="返回我的预约" @click="router.push('/patient/appointments')">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M19 12H5" />
          <path d="m12 19-7-7 7-7" />
        </svg>
      </button>
      <div>
        <p class="eyebrow">Payment</p>
        <h1 id="payment-title">预约支付</h1>
        <p>创建支付订单后完成模拟支付，支付成功会同步更新预约状态。</p>
      </div>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>
    <p v-if="successMsg" class="notice success" role="status">{{ successMsg }}</p>

    <section v-if="loading && !payment" class="state-card">正在创建支付订单...</section>

    <template v-else>
      <section class="pay-card" aria-label="支付订单">
        <div class="card-head">
          <div>
            <h2>订单信息</h2>
            <p>请核对预约编号、订单号和支付金额</p>
          </div>
          <span class="status-tag" :class="`status-${statusTone(payment?.status)}`">{{ statusText(payment?.status) }}</span>
        </div>

        <dl>
          <div>
            <dt>预约编号</dt>
            <dd>{{ appointmentId }}</dd>
          </div>
          <div>
            <dt>支付编号</dt>
            <dd>{{ paymentId || '--' }}</dd>
          </div>
          <div>
            <dt>订单号</dt>
            <dd>{{ payment?.orderNo || '--' }}</dd>
          </div>
          <div>
            <dt>支付金额</dt>
            <dd class="amount">{{ amountText(payment?.amount) }}</dd>
          </div>
        </dl>
      </section>

      <section class="countdown-card" aria-label="支付倒计时">
        <span>支付剩余时间</span>
        <strong>{{ isPaid ? '已完成' : minutesText }}</strong>
        <p>{{ isTimeout && !isPaid ? '订单已超时，请返回预约列表刷新状态' : '请在 15 分钟内完成支付' }}</p>
      </section>

      <section class="flow-card" aria-label="支付流程">
        <h2>支付流程</h2>
        <div class="flow-steps">
          <span>检查订单状态</span>
          <span>模拟支付</span>
          <span>更新支付状态</span>
          <span>更新预约状态</span>
        </div>
        <div class="inline-actions">
          <button class="secondary-btn" :disabled="loading || !paymentId" type="button" @click="refreshPayment">刷新订单</button>
          <button class="pay-btn" :disabled="!canPay" type="button" @click="submitPay">
            {{ paying ? '支付中...' : isPaid ? '已支付' : '立即支付' }}
          </button>
        </div>
      </section>
    </template>

    <div class="bottom-bar">
      <button class="secondary-btn" :disabled="loading || !paymentId" type="button" @click="refreshPayment">刷新订单</button>
      <button class="pay-btn" :disabled="!canPay" type="button" @click="submitPay">
        {{ paying ? '支付中...' : isPaid ? '已支付' : '立即支付' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.payment-view {
  min-height: 100%;
  padding: 14px 16px 96px;
}

.page-hero,
.pay-card,
.countdown-card,
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

.notice.success {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.state-card {
  margin-top: 12px;
  padding: 28px 16px;
  color: var(--color-ink-3);
  text-align: center;
  font-size: 13px;
}

.pay-card,
.countdown-card,
.flow-card {
  margin-top: 12px;
  padding: 16px;
}

.card-head {
  display: flex;
  align-items: flex-start;
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

.card-head p {
  margin-top: 4px;
  color: var(--color-ink-3);
  font-size: 12px;
}

.status-tag {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 800;
}

.status-warning {
  background: var(--color-warn-soft);
  color: var(--color-warn);
}

.status-success {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.status-danger {
  background: var(--color-danger-soft);
  color: var(--color-danger);
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

.amount {
  color: var(--color-primary-deep);
  font-size: 20px;
  font-variant-numeric: tabular-nums;
}

.countdown-card {
  text-align: center;
}

.countdown-card span {
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 800;
}

.countdown-card strong {
  display: block;
  margin-top: 4px;
  color: var(--color-warn);
  font-size: 34px;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.countdown-card p {
  margin-top: 5px;
  color: var(--color-ink-3);
  font-size: 12.5px;
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

.inline-actions {
  display: grid;
  grid-template-columns: minmax(120px, 0.45fr) minmax(160px, 1fr);
  gap: 10px;
  margin-top: 14px;
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
.pay-btn {
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

.pay-btn {
  border: none;
  background: linear-gradient(90deg, var(--color-primary), var(--color-primary-deep));
  color: #fff;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 620px) {
  .card-head,
  dl div {
    flex-direction: column;
  }

  dd {
    text-align: left;
  }

  .flow-steps,
  .inline-actions,
  .bottom-bar {
    grid-template-columns: 1fr;
  }
}
</style>
