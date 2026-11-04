<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  cancelAppointment,
  contactAdminRefund,
  getMyAppointments,
  getPatientConfig,
  signIn,
} from '@/api/patient'

const router = useRouter()

const loading = ref(false)
const actionId = ref('')
const errorMsg = ref('')
const successMsg = ref('')
const status = ref('')
const appointments = ref([])
const queueMap = reactive({})
const cancelTarget = ref(null)
const refundContactTarget = ref(null)
const refundReason = ref('')
const refundReasonError = ref('')
const configLoaded = ref(false)
const refundEnabled = ref(false)
const appointmentCancelHours = ref(0)
const signInAheadMinutes = ref(0)

const pager = reactive({
  page: 1,
  // 患者自己的预约数量通常不会很多，全部 tab 一次加载完整列表，
  // 避免待支付、已完成等状态被分页隐藏，必须切换状态才能找到。
  size: 100,
  total: 0,
})

const tabs = [
  { label: '全部', value: '' },
  { label: '待支付', value: '待支付' },
  { label: '已支付', value: '已支付' },
  { label: '已完成', value: '已完成' },
  { label: '已取消', value: '取消' },
]

const statusMeta = {
  待支付: { label: '待支付', tone: 'warn', step: 1 },
  已支付: { label: '已支付', tone: 'info', step: 2 },
  已签到: { label: '已签到', tone: 'info', step: 3 },
  接诊中: { label: '接诊中', tone: 'primary', step: 4 },
  已完成: { label: '已完成', tone: 'success', step: 5 },
  退款中: { label: '退款处理中', tone: 'warn', step: 2 },
  取消: { label: '已取消', tone: 'muted', step: 0 },
  已取消: { label: '已取消', tone: 'muted', step: 0 },
  爽约: { label: '爽约', tone: 'danger', step: 0 },
}

const flowSteps = ['待支付', '已支付', '已签到', '接诊中', '已完成']

const pages = computed(() => Math.max(1, Math.ceil(pager.total / pager.size)))

const summary = computed(() => {
  const base = {
    total: pager.total || appointments.value.length,
    pending: 0,
    active: 0,
    finished: 0,
  }

  appointments.value.forEach((item) => {
    const s = normalizeStatus(item.status)
    if (s === '待支付') base.pending++
    if (s === '已支付' || s === '已签到' || s === '接诊中') base.active++
    if (s === '已完成') base.finished++
  })

  return base
})

function normalizeStatus(value) {
  return String(value || '').trim()
}

function metaOf(value) {
  return statusMeta[normalizeStatus(value)] || { label: value || '未知状态', tone: 'muted', step: 0 }
}

function appointmentIdOf(item) {
  return item.appointmentId || item.id
}

function doctorText(item) {
  return item.doctorName || item.doctor?.name || '等待医生信息'
}

function departmentText(item) {
  return item.department || item.doctorDepartment || item.doctor?.department || '科室待返回'
}

function visitTimeText(item) {
  const date = item.visitDate || item.date || ''
  const start = item.startTime || ''
  const end = item.endTime || ''
  const period = start && end ? `${start} - ${end}` : (item.timePeriod || item.period || item.visitTime || '')
  return [date, period].filter(Boolean).join(' ') || item.createTime || '就诊时间待返回'
}

function amountText(item) {
  const amount = item.amount ?? item.fee ?? item.price
  return amount || amount === 0 ? `¥${amount}` : '待返回'
}

function canPay(item) {
  return normalizeStatus(item.status) === '待支付'
}

function canCancel(item) {
  return ['待支付', '已支付'].includes(normalizeStatus(item.status))
}

function isPaid(item) {
  return normalizeStatus(item.status) === '已支付'
}

function visitStartTime(item) {
  const date = item.visitDate || item.date
  if (!date) return null
  const hour = item.startTime || ((item.timePeriod || item.period || '').includes('上午') ? '08:00:00' : '14:00:00')
  const value = new Date(`${date}T${hour}`)
  return Number.isNaN(value.getTime()) ? null : value
}

function isPastCancellationDeadline(item) {
  const visitStart = visitStartTime(item)
  if (!visitStart || appointmentCancelHours.value <= 0) return false
  const deadline = visitStart.getTime() - appointmentCancelHours.value * 60 * 60 * 1000
  return Date.now() > deadline
}

function isCancelDisabled(item) {
  return !!actionId.value
    || (isPaid(item) && (!configLoaded.value || !refundEnabled.value))
    || isPastCancellationDeadline(item)
}

function cancelButtonText(item) {
  if (isPastCancellationDeadline(item)) return '已超过可取消时间'
  if (isPaid(item) && configLoaded.value && !refundEnabled.value) return '退款入口已关闭'
  if (isPaid(item) && !configLoaded.value) return '正在读取退款配置'
  return '取消预约'
}

function cancelButtonHint(item) {
  if (isPastCancellationDeadline(item)) return `需在就诊前至少${appointmentCancelHours.value}小时取消`
  if (!isPaid(item)) return '取消待支付预约'
  if (!configLoaded.value) return '正在读取退款申请配置'
  if (!refundEnabled.value) return '退款申请入口已关闭，请联系医院工作人员'
  return '取消已支付预约并申请退款'
}

function canContactAdminRefund(item) {
  return isPaid(item)
    && configLoaded.value
    && (isPastCancellationDeadline(item) || !refundEnabled.value)
}

function refundContactDescription(item) {
  if (isPastCancellationDeadline(item)) {
    return '预约已超过自助取消时限。请填写退款原因，提交后由管理员人工审核，处理结果会通过消息通知您。'
  }
  if (configLoaded.value && !refundEnabled.value) {
    return '当前自助退款入口已关闭。请填写退款原因，提交后由管理员人工审核，处理结果会通过消息通知您。'
  }
  return '如需由管理员协助处理退款，请填写退款原因。提交后将进入人工审核，处理结果会通过消息通知您。'
}

function canSign(item) {
  return normalizeStatus(item.status) === '已支付'
}

function isSignDisabled(item) {
  if (!configLoaded.value) return true
  const visitStart = visitStartTime(item)
  if (!visitStart || signInAheadMinutes.value <= 0) return false
  return Date.now() < visitStart.getTime() - signInAheadMinutes.value * 60 * 1000
}

function signButtonText(item) {
  if (!configLoaded.value) return '正在读取签到配置'
  return isSignDisabled(item) ? '未到签到时间' : '签到'
}

function signButtonHint(item) {
  if (!configLoaded.value) return '正在读取签到配置'
  if (isSignDisabled(item)) return `请在就诊前${signInAheadMinutes.value}分钟内签到`
  return '进行签到'
}

function canQueue(item) {
  return ['已支付', '已签到', '接诊中'].includes(normalizeStatus(item.status))
}

function canViewRecord(item) {
  return normalizeStatus(item.status) === '已完成'
}

async function loadAppointments() {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await getMyAppointments({
      status: status.value || undefined,
      page: pager.page,
      size: pager.size,
    })
    appointments.value = Array.isArray(data) ? data : data?.records || []
    pager.total = Array.isArray(data) ? data.length : data?.total || 0
  } catch (e) {
    errorMsg.value = e.message || '预约列表加载失败，请稍后重试'
    appointments.value = []
    pager.total = 0
  } finally {
    loading.value = false
  }
}

async function loadPatientConfig() {
  configLoaded.value = false
  try {
    const data = await getPatientConfig()
    refundEnabled.value = data?.refundEnabled === true
    appointmentCancelHours.value = Number(data?.appointmentCancelHours || 0)
    signInAheadMinutes.value = Number(data?.signInAheadMinutes || 0)
  } catch (e) {
    // 配置读取失败时按关闭处理，避免界面误导患者可以提交退款。
    refundEnabled.value = false
  } finally {
    configLoaded.value = true
  }
}

async function runAction(id, task, options = {}) {
  if (actionId.value) return
  actionId.value = id
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const result = await task()
    if (options.success) successMsg.value = options.success
    if (options.reload !== false) await loadAppointments()
    return result
  } catch (e) {
    errorMsg.value = e.message || '操作失败，请稍后重试'
    return null
  } finally {
    actionId.value = ''
  }
}

function changeStatus(nextStatus) {
  status.value = nextStatus
  pager.page = 1
  loadAppointments()
}

function prevPage() {
  if (pager.page <= 1) return
  pager.page--
  loadAppointments()
}

function nextPage() {
  if (pager.page >= pages.value) return
  pager.page++
  loadAppointments()
}

function askCancel(item) {
  if (isCancelDisabled(item)) return
  cancelTarget.value = item
}

function openRefundContact(item) {
  refundContactTarget.value = item
  refundReason.value = ''
  refundReasonError.value = ''
}

function closeRefundContact() {
  if (actionId.value) return
  refundContactTarget.value = null
  refundReason.value = ''
  refundReasonError.value = ''
}

async function submitRefundContact() {
  const reason = refundReason.value.trim()
  if (reason.length < 5 || reason.length > 100) {
    refundReasonError.value = '请填写 5 至 100 个字符的退款原因'
    return
  }

  const item = refundContactTarget.value
  if (!item) return
  const id = appointmentIdOf(item)
  const result = await runAction(
    `refund-contact-${id}`,
    () => contactAdminRefund(id, { reason }),
    { success: '退款申请已提交，请留意管理员处理通知' },
  )
  if (result) closeRefundContact()
}

async function confirmCancel() {
  const item = cancelTarget.value
  if (!item) return
  const id = appointmentIdOf(item)
  await runAction(`cancel-${id}`, () => cancelAppointment(id), { success: '预约取消请求已提交' })
  cancelTarget.value = null
}

function handlePay(item) {
  router.push(`/patient/appointments/${appointmentIdOf(item)}/pay`)
}

async function handleSignIn(item) {
  const id = appointmentIdOf(item)
  await runAction(
    `sign-${id}`,
    async () => {
      const result = await signIn({ appointmentId: id })
      queueMap[id] = { ...(queueMap[id] || {}), ...result }
    },
    { success: '签到成功，已进入排队' },
  )
}

async function handleQueue(item) {
  const id = appointmentIdOf(item)
  router.push(`/patient/queue/${id}`)
}

function goAppointmentDetail(item) {
  const id = appointmentIdOf(item)
  router.push(`/patient/appointments/${id}`)
}

function goRecords() {
  router.push('/patient/records')
}

onMounted(() => {
  loadPatientConfig()
  loadAppointments()
})
</script>

<template>
  <div class="appointments-view">
    <header class="hero">
      <div>
        <p class="eyebrow">患者服务</p>
        <h1>我的预约</h1>
        <p class="hero-desc">查看预约状态，完成支付、签到、排队和诊疗记录查询。</p>
      </div>
      <button class="refresh-btn" :disabled="loading" aria-label="刷新预约列表" @click="loadAppointments">
        <svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12a9 9 0 0 1-15.5 6.2" />
          <path d="M3 12A9 9 0 0 1 18.5 5.8" />
          <path d="M18 2v4h-4" />
          <path d="M6 22v-4h4" />
        </svg>
      </button>
    </header>

    <section class="summary-grid" aria-label="预约概览">
      <div class="summary-card">
        <span>全部预约</span>
        <strong>{{ summary.total }}</strong>
      </div>
      <div class="summary-card warn">
        <span>待支付</span>
        <strong>{{ summary.pending }}</strong>
      </div>
      <div class="summary-card info">
        <span>进行中</span>
        <strong>{{ summary.active }}</strong>
      </div>
      <div class="summary-card success">
        <span>已完成</span>
        <strong>{{ summary.finished }}</strong>
      </div>
    </section>

    <nav class="tabs" aria-label="预约状态筛选">
      <button
        v-for="tab in tabs"
        :key="tab.label"
        :class="{ on: status === tab.value }"
        :aria-pressed="status === tab.value"
        @click="changeStatus(tab.value)"
      >
        {{ tab.label }}
      </button>
    </nav>

    <p v-if="errorMsg" class="notice error" role="alert">{{ errorMsg }}</p>
    <p v-if="successMsg" class="notice success" role="status">{{ successMsg }}</p>

    <section class="appointment-list" aria-label="预约列表">
      <article v-if="loading" class="skeleton-card" aria-label="正在加载预约">
        <span></span>
        <span></span>
        <span></span>
      </article>

      <div v-else-if="appointments.length === 0" class="empty-state">
        <svg aria-hidden="true" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <rect x="3" y="4" width="18" height="18" rx="3" />
          <path d="M16 2v4M8 2v4M3 10h18M8 15h4" />
        </svg>
        <h2>暂无预约记录</h2>
        <p>当前筛选状态下没有预约记录。</p>
        <button @click="router.push('/patient/doctors')">去预约挂号</button>
      </div>

      <article
        v-for="item in appointments"
        v-else
        :key="appointmentIdOf(item)"
        class="appointment-card"
      >
        <div class="card-main">
          <div class="doctor-mark" aria-hidden="true">{{ doctorText(item).slice(0, 1) }}</div>
          <div class="card-content">
            <div class="card-title">
              <div>
                <h2>{{ doctorText(item) }}</h2>
                <p>{{ departmentText(item) }}</p>
              </div>
              <span class="status-tag" :class="metaOf(item.status).tone">{{ metaOf(item.status).label }}</span>
            </div>

            <dl class="info-list">
              <div>
                <dt>就诊时间</dt>
                <dd>{{ visitTimeText(item) }}</dd>
              </div>
              <div>
                <dt>预约编号</dt>
                <dd>{{ appointmentIdOf(item) || '--' }}</dd>
              </div>
              <div>
                <dt>费用</dt>
                <dd>{{ amountText(item) }}</dd>
              </div>
            </dl>

            <ol class="flow" aria-label="预约流程">
              <li
                v-for="(step, index) in flowSteps"
                :key="step"
                :class="{ done: metaOf(item.status).step >= index + 1 }"
              >
                <span></span>
                {{ step }}
              </li>
            </ol>

            <div v-if="queueMap[appointmentIdOf(item)]" class="queue-box">
              <strong>排队号 {{ queueMap[appointmentIdOf(item)].queueNo || '--' }}</strong>
              <span>状态：{{ queueMap[appointmentIdOf(item)].status || '--' }}</span>
              <span>前方等待：{{ queueMap[appointmentIdOf(item)].rank ?? '--' }} 人</span>
            </div>

            <div class="actions">
              <button @click="goAppointmentDetail(item)">查看详情</button>
              <button v-if="canPay(item)" class="primary" :disabled="!!actionId" @click="handlePay(item)">
                去支付
              </button>
              <button
                v-if="canSign(item)"
                class="primary"
                :disabled="!!actionId || isSignDisabled(item)"
                :title="signButtonHint(item)"
                :aria-label="signButtonHint(item)"
                @click="handleSignIn(item)"
              >
                {{ actionId === `sign-${appointmentIdOf(item)}` ? '签到中...' : signButtonText(item) }}
              </button>
              <button v-if="canQueue(item)" :disabled="!!actionId" @click="handleQueue(item)">
                {{ actionId === `queue-${appointmentIdOf(item)}` ? '查询中...' : '查看排队' }}
              </button>
              <button v-if="canViewRecord(item)" @click="goRecords">查看记录</button>
              <button
                v-if="canCancel(item) && !canContactAdminRefund(item)"
                class="danger"
                :disabled="isCancelDisabled(item)"
                :title="cancelButtonHint(item)"
                :aria-label="cancelButtonHint(item)"
                @click="askCancel(item)"
              >
                {{ cancelButtonText(item) }}
              </button>
              <button
                v-if="canContactAdminRefund(item)"
                class="refund-contact"
                :disabled="!!actionId"
                @click="openRefundContact(item)"
              >
                联系管理员退款
              </button>
            </div>
          </div>
        </div>
      </article>
    </section>

    <div v-if="appointments.length > 0" class="pager">
      <button :disabled="pager.page <= 1 || loading" @click="prevPage">上一页</button>
      <span>第 {{ pager.page }} / {{ pages }} 页</span>
      <button :disabled="pager.page >= pages || loading" @click="nextPage">下一页</button>
    </div>

    <div v-if="cancelTarget" class="dialog-mask" role="presentation" @click.self="cancelTarget = null">
      <section class="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="cancel-title">
        <h2 id="cancel-title">取消预约</h2>
        <p>
          确认取消
          <b>{{ doctorText(cancelTarget) }}</b>
          的预约吗？{{ isPaid(cancelTarget) ? '已支付预约会进入退款流程。' : '待支付预约将直接取消。' }}
        </p>
        <div class="dialog-actions">
          <button :disabled="!!actionId" @click="cancelTarget = null">再想想</button>
          <button class="danger-fill" :disabled="!!actionId" @click="confirmCancel">
            {{ actionId ? '提交中...' : '确认取消' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="refundContactTarget" class="dialog-mask" role="presentation" @click.self="closeRefundContact">
      <section class="confirm-dialog refund-dialog" role="dialog" aria-modal="true" aria-labelledby="refund-contact-title">
        <h2 id="refund-contact-title">联系管理员退款</h2>
        <p>
          {{ refundContactDescription(refundContactTarget) }}
        </p>
        <div class="refund-summary">
          <span>{{ doctorText(refundContactTarget) }}</span>
          <strong>预约编号 {{ appointmentIdOf(refundContactTarget) }}</strong>
        </div>
        <label for="refund-reason">退款原因 <em>必填</em></label>
        <textarea
          id="refund-reason"
          v-model="refundReason"
          maxlength="100"
          rows="4"
          placeholder="请说明无法按时就诊的原因"
          :aria-invalid="Boolean(refundReasonError)"
          aria-describedby="refund-reason-help"
          @input="refundReasonError = ''"
        ></textarea>
        <div id="refund-reason-help" class="field-help" :class="{ error: refundReasonError }">
          <span>{{ refundReasonError || '管理员将根据说明审核退款申请' }}</span>
          <span>{{ refundReason.length }}/100</span>
        </div>
        <div class="dialog-actions">
          <button :disabled="!!actionId" @click="closeRefundContact">取消</button>
          <button class="primary-fill" :disabled="!!actionId" @click="submitRefundContact">
            {{ actionId ? '提交中...' : '提交退款申请' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.appointments-view {
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
.hero-desc {
  max-width: 25rem;
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
.summary-card {
  min-height: 78px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 12px;
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
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
.summary-card.warn strong {
  color: var(--color-warn);
}
.summary-card.info strong {
  color: var(--color-info);
}
.summary-card.success strong {
  color: var(--color-accent);
}
.tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 2px 0 10px;
  scrollbar-width: none;
}
.tabs::-webkit-scrollbar {
  display: none;
}
.tabs button {
  min-width: 72px;
  min-height: 40px;
  flex: 0 0 auto;
  border: 1.5px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-surface);
  color: var(--color-ink-2);
  font-size: 13px;
  font-weight: 700;
  transition: border-color 0.18s ease, background 0.18s ease;
}
.tabs button.on {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
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
.pay-result {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 12px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius);
  padding: 14px;
  background: var(--color-surface);
}
.pay-result span {
  display: block;
  font-size: 12px;
  color: var(--color-ink-3);
}
.pay-result strong {
  display: block;
  margin-top: 2px;
  font-size: 16px;
  color: var(--color-accent);
}
.pay-result p {
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-ink-3);
}
.pay-result button,
.empty-state button,
.pager button,
.actions button,
.dialog-actions button {
  min-height: 40px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-ink-2);
  font-size: 13px;
  font-weight: 700;
  font-family: var(--font-sans);
  transition: opacity 0.18s ease, background 0.18s ease;
}
.pay-result button {
  padding: 0 14px;
}
.appointment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.empty-state,
.skeleton-card,
.appointment-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}
.empty-state {
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
.empty-state h2 {
  font-size: 16px;
  color: var(--color-ink);
}
.empty-state p {
  max-width: 22rem;
  font-size: 13px;
}
.empty-state button {
  margin-top: 6px;
  padding: 0 16px;
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: #fff;
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
.appointment-card {
  padding: 16px;
}
.card-main {
  display: flex;
  gap: 13px;
}
.doctor-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-deep));
  color: #fff;
  font-size: 17px;
  font-weight: 800;
}
.card-content {
  flex: 1;
  min-width: 0;
}
.card-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.card-title h2 {
  font-size: 16px;
  line-height: 1.3;
  color: var(--color-ink);
  overflow-wrap: anywhere;
}
.card-title p {
  margin-top: 2px;
  font-size: 12.5px;
  color: var(--color-ink-3);
}
.status-tag {
  align-self: flex-start;
  flex-shrink: 0;
  border-radius: 999px;
  padding: 4px 9px;
  background: var(--color-border);
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 800;
}
.status-tag.warn {
  background: var(--color-warn-soft);
  color: var(--color-warn);
}
.status-tag.info {
  background: var(--color-info-soft);
  color: var(--color-info);
}
.status-tag.primary {
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
}
.status-tag.success {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}
.status-tag.danger {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}
.info-list {
  display: grid;
  grid-template-columns: 1.4fr 1fr 0.8fr;
  gap: 10px;
  margin-top: 14px;
}
.info-list div {
  min-width: 0;
  border-radius: var(--radius-sm);
  padding: 9px 10px;
  background: var(--color-bg);
}
.info-list dt {
  font-size: 11.5px;
  color: var(--color-ink-3);
}
.info-list dd {
  margin-top: 2px;
  color: var(--color-ink);
  font-size: 12.5px;
  font-weight: 700;
  overflow-wrap: anywhere;
}
.flow {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
  margin-top: 14px;
}
.flow li {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  color: var(--color-ink-3);
  font-size: 11px;
  text-align: center;
}
.flow span {
  width: 100%;
  height: 4px;
  border-radius: 999px;
  background: var(--color-border);
}
.flow li.done {
  color: var(--color-primary-deep);
  font-weight: 700;
}
.flow li.done span {
  background: var(--color-primary);
}
.queue-box {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
  margin-top: 14px;
  border-radius: var(--radius-sm);
  padding: 10px;
  background: var(--color-info-soft);
  color: var(--color-ink-2);
  font-size: 12px;
}
.queue-box strong {
  color: var(--color-info);
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}
.actions button {
  min-width: 92px;
  padding: 0 13px;
}
.actions button.primary {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}
.actions button.danger {
  border-color: rgba(220, 38, 38, 0.25);
  background: var(--color-danger-soft);
  color: var(--color-danger);
}
.actions button.refund-contact {
  border-color: var(--color-warn);
  background: var(--color-warn-soft);
  color: var(--color-warn);
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
.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  padding: 18px;
  background: rgba(8, 47, 43, 0.45);
}
.confirm-dialog {
  width: min(100%, 380px);
  border-radius: var(--radius-lg);
  padding: 20px;
  background: var(--color-surface);
  box-shadow: var(--shadow-lg);
}
.confirm-dialog h2 {
  font-size: 18px;
  color: var(--color-ink);
}
.confirm-dialog p {
  margin-top: 9px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--color-ink-3);
}
.confirm-dialog b {
  color: var(--color-ink);
}
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}
.dialog-actions button {
  padding: 0 15px;
}
.dialog-actions .danger-fill {
  border-color: var(--color-danger);
  background: var(--color-danger);
  color: #fff;
}
.dialog-actions .primary-fill {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}
.refund-summary {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin: 14px 0;
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  background: var(--color-primary-soft);
  color: var(--color-ink-2);
  font-size: 12.5px;
}
.refund-dialog label {
  display: block;
  margin-bottom: 7px;
  color: var(--color-ink);
  font-size: 13px;
  font-weight: 700;
}
.refund-dialog label em {
  margin-left: 4px;
  color: var(--color-danger);
  font-size: 11px;
  font-style: normal;
}
.refund-dialog textarea {
  width: 100%;
  resize: vertical;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
  padding: 11px 12px;
  background: var(--color-surface);
  color: var(--color-ink);
  font: inherit;
  line-height: 1.6;
}
.refund-dialog textarea:focus {
  outline: 3px solid var(--color-primary-soft);
  border-color: var(--color-primary);
}
.field-help {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 6px;
  color: var(--color-ink-3);
  font-size: 11.5px;
}
.field-help.error {
  color: var(--color-danger);
}
@media (max-width: 720px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .card-main {
    display: block;
  }
  .doctor-mark {
    margin-bottom: 10px;
  }
  .info-list,
  .queue-box {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 420px) {
  .appointments-view {
    padding-inline: 12px;
  }
  .hero {
    padding: 18px;
  }
  .flow {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .actions button {
    flex: 1 1 calc(50% - 4px);
  }
}
</style>
