<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPatientConfig, getQueue, signIn } from '@/api/patient'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const signing = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const queue = ref(null)
const queuePollSeconds = ref(15)
let timer = null

const appointmentId = computed(() => route.params.appointmentId)
const status = computed(() => statusText(queue.value?.status || queue.value?.queueStatus))
const queueNo = computed(() => queue.value?.queueNo || queue.value?.queueNumber || '--')
const rank = computed(() => queue.value?.rank ?? queue.value?.waitingCount ?? queue.value?.aheadCount ?? '--')
const room = computed(() => queue.value?.roomName || queue.value?.clinicRoom || queue.value?.room || '暂未分配')
const doctor = computed(() => queue.value?.doctorName || queue.value?.doctor?.name || '暂无信息')
const department = computed(() => queue.value?.department || queue.value?.doctorDepartment || queue.value?.doctor?.department || '暂无信息')
const lastRefreshText = computed(() => queue.value?.updateTime || queue.value?.updatedAt || queue.value?.signTime || queue.value?.signInTime || '暂无更新时间')
const canSignIn = computed(() => !signing.value && !loading.value && !queue.value)

const flowSteps = ['已支付', '已签到', '排队等待', '接诊中', '已完成']

function statusText(value) {
  const map = {
    SIGNED: '已签到',
    '已签到': '已签到',
    WAITING: '排队等待',
    '等待接诊': '排队等待',
    QUEUING: '排队等待',
    CALLING: '叫号中',
    IN_PROGRESS: '接诊中',
    FINISHED: '已完成',
  }
  return map[value] || value || '等待签到'
}

function activeStep(label) {
  const index = {
    等待签到: 1,
    已支付: 1,
    已签到: 2,
    排队等待: 3,
    叫号中: 3,
    接诊中: 4,
    已完成: 5,
  }
  return (index[status.value] || 1) >= flowSteps.indexOf(label) + 1
}

function statusTone() {
  if (status.value === '已完成') return 'success'
  if (status.value === '接诊中' || status.value === '叫号中') return 'primary'
  if (status.value === '等待签到') return 'warning'
  return 'info'
}

async function refreshQueue(options = {}) {
  loading.value = true
  errorMsg.value = ''
  if (!options.keepSuccess) successMsg.value = ''
  try {
    queue.value = await getQueue(appointmentId.value)
  } catch (e) {
    const message = e.message || '排队信息加载失败，请稍后重试'
    if (!message.includes('排队记录不存在')) {
      errorMsg.value = message
    }
    queue.value = null
  } finally {
    loading.value = false
  }
}

async function submitSignIn() {
  signing.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const result = await signIn({ appointmentId: Number(appointmentId.value) })
    queue.value = { ...(queue.value || {}), ...result }
    successMsg.value = '签到成功，已进入排队'
    await refreshQueue({ keepSuccess: true })
  } catch (e) {
    const message = e.message || '签到失败，请稍后重试'
    if (message.includes('已签到') || message.includes('重复签到')) {
      successMsg.value = '已完成签到，正在刷新排队状态'
      await refreshQueue({ keepSuccess: true })
    } else {
      errorMsg.value = message
    }
  } finally {
    signing.value = false
  }
}

function startPolling() {
  stopPolling()
  timer = window.setInterval(() => {
    if (!loading.value && !signing.value) refreshQueue({ keepSuccess: true })
  }, queuePollSeconds.value * 1000)
}

async function loadQueueConfig() {
  try {
    const data = await getPatientConfig()
    const seconds = Number(data?.queuePollSeconds)
    if (Number.isFinite(seconds) && seconds > 0) {
      queuePollSeconds.value = Math.max(5, seconds)
    }
  } catch {
    // 配置读取失败时使用 15 秒默认值，不影响排队页面使用。
  }
}

function stopPolling() {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
}

onMounted(async () => {
  await loadQueueConfig()
  refreshQueue()
  startPolling()
})

onBeforeUnmount(stopPolling)
</script>

<template>
  <div class="queue-view">
    <section class="page-hero" aria-labelledby="queue-title">
      <button class="back-btn" type="button" aria-label="返回我的预约" @click="router.push('/patient/appointments')">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M19 12H5" />
          <path d="m12 19-7-7 7-7" />
        </svg>
      </button>
      <div>
        <p class="eyebrow">Queue Status</p>
        <h1 id="queue-title">排队等待</h1>
        <p>签到后查询当前排队状态，页面会按 {{ queuePollSeconds }} 秒间隔自动刷新。</p>
      </div>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>
    <p v-if="successMsg" class="notice success" role="status">{{ successMsg }}</p>

    <section class="queue-card" aria-label="排队核心信息">
      <div class="queue-main">
        <span class="status-tag" :class="`status-${statusTone()}`">{{ status }}</span>
        <span class="queue-label">排队号</span>
        <strong>{{ queueNo }}</strong>
        <p>前方等待 {{ rank }} 人</p>
      </div>
      <dl>
        <div>
          <dt>预约编号</dt>
          <dd>{{ appointmentId }}</dd>
        </div>
        <div>
          <dt>医生</dt>
          <dd>{{ doctor }}</dd>
        </div>
        <div>
          <dt>科室</dt>
          <dd>{{ department }}</dd>
        </div>
        <div>
          <dt>诊室</dt>
          <dd>{{ room }}</dd>
        </div>
        <div>
          <dt>最近更新</dt>
          <dd>{{ lastRefreshText }}</dd>
        </div>
      </dl>
    </section>

    <section class="flow-card" aria-label="就诊流程">
      <h2>就诊流程</h2>
      <ol>
        <li v-for="step in flowSteps" :key="step" :class="{ done: activeStep(step) }">
          <span aria-hidden="true"></span>
          {{ step }}
        </li>
      </ol>
    </section>

    <section class="tip-card" aria-label="排队提示">
      <h2>排队提示</h2>
      <p v-if="!queue && !loading">当前尚未生成排队记录，请在允许签到的时间内点击“签到入队”。</p>
      <p v-else>请留意排队号和叫号状态，候诊期间不要离开诊区；页面会自动刷新最新进度。</p>
    </section>

    <div class="bottom-bar">
      <button class="secondary-btn" :disabled="loading || signing" type="button" @click="refreshQueue">
        {{ loading ? '刷新中...' : '刷新排队' }}
      </button>
      <button class="primary-btn" :disabled="!canSignIn" type="button" @click="submitSignIn">
        {{ signing ? '签到中...' : '签到入队' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.queue-view {
  min-height: 100%;
  padding: 14px 16px 96px;
}

.page-hero,
.queue-card,
.flow-card,
.tip-card {
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

.queue-card {
  display: grid;
  grid-template-columns: minmax(180px, 0.8fr) minmax(0, 1fr);
  gap: 16px;
  margin-top: 12px;
  padding: 16px;
}

.queue-main {
  display: grid;
  align-content: center;
  justify-items: center;
  min-height: 210px;
  border-radius: var(--radius);
  background: linear-gradient(145deg, var(--color-primary-soft), #fff);
  text-align: center;
}

.queue-label {
  margin-top: 18px;
  color: var(--color-ink-3);
  font-size: 13px;
  font-weight: 800;
}

.queue-main strong {
  margin-top: 2px;
  color: var(--color-primary-deep);
  font-size: 56px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.queue-main p {
  margin-top: 10px;
  color: var(--color-ink-2);
  font-size: 14px;
  font-weight: 800;
}

.status-tag {
  border-radius: 999px;
  padding: 6px 11px;
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

.status-primary {
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
}

.status-success {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

dl {
  display: grid;
  align-content: center;
  gap: 12px;
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

.flow-card,
.tip-card {
  margin-top: 12px;
  padding: 16px;
}

.flow-card h2,
.tip-card h2 {
  color: var(--color-ink);
  font-size: 17px;
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

.flow-card li.done {
  border-color: rgba(32, 139, 116, 0.22);
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.flow-card li.done span {
  border-color: var(--color-accent);
  background: var(--color-accent);
}

.tip-card p {
  margin-top: 8px;
  color: var(--color-ink-3);
  font-size: 12.5px;
  line-height: 1.6;
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

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 720px) {
  .queue-card,
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
