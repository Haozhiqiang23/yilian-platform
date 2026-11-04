<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDoctor, getSources } from '@/api/patient'
import ListPager from '@/components/common/ListPager.vue'

const route = useRoute()
const router = useRouter()

const loadingDoctor = ref(false)
const loadingSources = ref(false)
const errorMsg = ref('')
const sourceMsg = ref('')
const doctor = ref(null)
const sources = ref([])
const selectedSourceId = ref('')
const sourcePage = ref(1)
const sourcePageSize = 10

const filters = reactive({
  date: todayText(),
})

const selectedSource = computed(() => sources.value.find((item) => String(sourceIdOf(item)) === String(selectedSourceId.value)))

const availableCount = computed(() => sources.value.filter((item) => remainCountOf(item) > 0).length)

const totalRemain = computed(() => sources.value.reduce((sum, item) => sum + remainCountOf(item), 0))
const pagedSources = computed(() => sources.value.slice((sourcePage.value - 1) * sourcePageSize, sourcePage.value * sourcePageSize))

function todayText() {
  const date = new Date()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

async function loadDoctor() {
  loadingDoctor.value = true
  errorMsg.value = ''
  try {
    doctor.value = await getDoctor(route.params.id)
    await loadSources()
  } catch (e) {
    errorMsg.value = e.message || '医生信息加载失败，请稍后重试'
    doctor.value = null
    sources.value = []
  } finally {
    loadingDoctor.value = false
  }
}

async function loadSources() {
  loadingSources.value = true
  sourceMsg.value = ''
  selectedSourceId.value = ''
  sourcePage.value = 1
  try {
    const data = await getSources({
      doctorId: route.params.id,
      date: filters.date || undefined,
    })
    sources.value = normalizeResponseList(data)
    const firstAvailable = sources.value.find((item) => remainCountOf(item) > 0)
    selectedSourceId.value = firstAvailable ? String(sourceIdOf(firstAvailable)) : ''
  } catch (e) {
    sourceMsg.value = e.message || '号源查询失败，请稍后重试'
    sources.value = []
  } finally {
    loadingSources.value = false
  }
}

function normalizeResponseList(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data?.rows)) return data.rows
  return []
}

function doctorIdOf(item) {
  return item?.doctorId || item?.id || route.params.id
}

function departmentText(item) {
  return item?.department || item?.departmentName || '--'
}

function introText(item) {
  return item?.introduction || item?.description || '暂无医生简介'
}

function sourceIdOf(item) {
  return item?.sourceId || item?.id || ''
}

function visitDateOf(item) {
  return item?.visitDate || item?.date || filters.date || '--'
}

function timeText(item) {
  const start = item?.startTime || item?.start || ''
  const end = item?.endTime || item?.end || ''
  if (start && end) return `${start} - ${end}`
  return item?.timePeriod || item?.period || item?.timeRange || '--'
}

function remainCountOf(item) {
  return Number(item?.remainCount ?? item?.remainingCount ?? item?.availableCount ?? 0)
}

function priceText(item) {
  const price = item?.price ?? item?.fee
  return price === undefined || price === null ? '费用待返回' : `¥${Number(price).toFixed(2)}`
}

function avatarText(name) {
  return (name || '医').slice(0, 1)
}

function pickSource(item) {
  if (remainCountOf(item) <= 0) return
  selectedSourceId.value = String(sourceIdOf(item))
}

function goAppointmentConfirm() {
  if (!selectedSource.value) {
    sourceMsg.value = '请选择可预约号源后再继续'
    return
  }
  router.push({
    path: '/patient/appointments/confirm',
    query: {
      doctorId: doctorIdOf(doctor.value),
      sourceId: sourceIdOf(selectedSource.value),
      date: visitDateOf(selectedSource.value),
      startTime: selectedSource.value.startTime || '',
      endTime: selectedSource.value.endTime || '',
      fee: selectedSource.value.fee ?? '',
      timePeriod: selectedSource.value.timePeriod || '',
    },
  })
}

onMounted(loadDoctor)
</script>

<template>
  <div class="detail-view">
    <section class="page-hero" aria-labelledby="doctor-detail-title">
      <button class="back-btn" type="button" aria-label="返回医生列表" @click="router.push('/patient/doctors')">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M19 12H5" />
          <path d="m12 19-7-7 7-7" />
        </svg>
      </button>
      <div>
        <p class="eyebrow">Doctor Detail</p>
        <h1 id="doctor-detail-title">医生详情</h1>
        <p>查看医生资料和指定日期号源，选择可预约时段后进入预约确认。</p>
      </div>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>

    <section v-if="loadingDoctor" class="state-card">正在请求医生详情接口...</section>
    <section v-else-if="!doctor" class="state-card">暂未获取到医生信息，请稍后重试</section>

    <template v-else>
      <section class="doctor-card" aria-label="医生信息">
        <span class="avatar" aria-hidden="true">{{ avatarText(doctor.name) }}</span>
        <div class="doctor-main">
          <div class="name-line">
            <strong>{{ doctor.name || '--' }}</strong>
            <em>{{ doctor.title || '--' }}</em>
          </div>
          <div class="meta-line">{{ departmentText(doctor) }} · 医生ID {{ doctorIdOf(doctor) }}</div>
          <p>{{ introText(doctor) }}</p>
        </div>
      </section>

      <section class="summary-grid" aria-label="号源概览">
        <article class="summary-card">
          <span>当前日期</span>
          <strong>{{ filters.date || '--' }}</strong>
        </article>
        <article class="summary-card">
          <span>可约时段</span>
          <strong>{{ availableCount }}</strong>
        </article>
        <article class="summary-card">
          <span>剩余号源</span>
          <strong>{{ totalRemain }}</strong>
        </article>
      </section>

      <section class="filter-panel" aria-label="出诊日期筛选">
        <label>
          <span>出诊日期</span>
          <input v-model="filters.date" type="date" />
        </label>
        <button class="primary-btn" :disabled="loadingSources" type="button" @click="loadSources">
          {{ loadingSources ? '查询中' : '查询号源' }}
        </button>
      </section>

      <p v-if="sourceMsg" class="notice danger" role="alert">{{ sourceMsg }}</p>

      <section class="source-panel" aria-label="出诊安排">
        <div class="panel-head">
          <div>
            <h2>出诊安排</h2>
            <p>请选择可预约的出诊时段，余号为 0 时暂不可预约。</p>
          </div>
        </div>

        <div v-if="loadingSources" class="state-card">正在请求号源查询接口...</div>
        <div v-else-if="sources.length === 0" class="state-card">当前日期暂无可预约号源</div>
        <div v-else class="source-list">
          <button
            v-for="item in pagedSources"
            :key="sourceIdOf(item)"
            class="source-item"
            :class="{ active: String(sourceIdOf(item)) === String(selectedSourceId), disabled: remainCountOf(item) <= 0 }"
            :disabled="remainCountOf(item) <= 0"
            type="button"
            @click="pickSource(item)"
          >
            <span class="source-main">
              <strong>{{ visitDateOf(item) }} · {{ timeText(item) }}</strong>
              <small>号源ID {{ sourceIdOf(item) || '--' }} · {{ priceText(item) }}</small>
            </span>
            <span class="source-status" :class="{ empty: remainCountOf(item) <= 0 }">
              {{ remainCountOf(item) <= 0 ? '已约满' : `剩余 ${remainCountOf(item)}` }}
            </span>
          </button>
        </div>
        <ListPager v-model:page="sourcePage" :page-size="sourcePageSize" :total="sources.length" :disabled="loadingSources" />
      </section>

      <div class="bottom-bar">
        <div class="selected-text">
          <span>已选</span>
          <strong>{{ selectedSource ? `${visitDateOf(selectedSource)} ${timeText(selectedSource)}` : '请选择号源' }}</strong>
        </div>
        <button class="book-btn" :disabled="!selectedSource" type="button" @click="goAppointmentConfirm">
          立即预约
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.detail-view {
  min-height: 100%;
  padding: 14px 16px 96px;
}

.page-hero,
.doctor-card,
.summary-card,
.filter-panel,
.source-panel,
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

.doctor-card {
  display: flex;
  gap: 14px;
  margin-top: 12px;
  padding: 16px;
}

.avatar {
  display: grid;
  width: 58px;
  height: 58px;
  flex-shrink: 0;
  place-items: center;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-deep));
  color: #fff;
  font-size: 21px;
  font-weight: 900;
}

.doctor-main {
  min-width: 0;
  flex: 1;
}

.name-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.name-line strong {
  color: var(--color-ink);
  font-size: 17px;
}

.name-line em {
  color: var(--color-primary);
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.meta-line,
.doctor-main p {
  margin-top: 5px;
  color: var(--color-ink-3);
  font-size: 12.5px;
}

.doctor-main p {
  line-height: 1.8;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.summary-card {
  padding: 14px;
}

.summary-card span {
  display: block;
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 700;
}

.summary-card strong {
  display: block;
  margin-top: 5px;
  color: var(--color-ink);
  font-size: 20px;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.filter-panel {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) auto;
  gap: 10px;
  align-items: end;
  margin-top: 12px;
  padding: 14px;
}

.filter-panel label span {
  display: block;
  margin-bottom: 6px;
  color: var(--color-ink-2);
  font-size: 13px;
  font-weight: 800;
}

.filter-panel input {
  width: 100%;
  min-height: 40px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0 12px;
  color: var(--color-ink);
  outline: none;
}

.filter-panel input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(14, 143, 134, 0.12);
}

.primary-btn {
  min-height: 40px;
  border: 1.5px solid var(--color-primary);
  border-radius: var(--radius-sm);
  padding: 0 13px;
  background: var(--color-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.source-panel {
  margin-top: 12px;
  padding: 14px;
}

.panel-head h2 {
  color: var(--color-ink);
  font-size: 17px;
  font-weight: 800;
}

.panel-head p {
  margin-top: 4px;
  color: var(--color-ink-3);
  font-size: 12.5px;
}

.source-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.source-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 13px;
  background: #fff;
  text-align: left;
}

.source-item.active {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
}

.source-item.disabled {
  background: #f7fbfa;
}

.source-main {
  min-width: 0;
}

.source-main strong {
  display: block;
  color: var(--color-ink);
  font-size: 14px;
}

.source-main small {
  display: block;
  margin-top: 4px;
  color: var(--color-ink-3);
  font-size: 12px;
}

.source-status {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 5px 10px;
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-size: 12px;
  font-weight: 800;
}

.source-status.empty {
  background: var(--color-warn-soft);
  color: var(--color-warn);
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

.book-btn {
  min-height: 42px;
  border: none;
  border-radius: var(--radius-sm);
  padding: 0 18px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-primary-deep));
  color: #fff;
  font-size: 14px;
  font-weight: 800;
}

@media (max-width: 620px) {
  .summary-grid,
  .filter-panel {
    grid-template-columns: 1fr;
  }

  .doctor-card,
  .source-item,
  .bottom-bar {
    align-items: stretch;
  }

  .source-item {
    flex-direction: column;
  }

  .source-status {
    align-self: flex-start;
  }
}
</style>
