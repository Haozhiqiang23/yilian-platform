<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getDoctors } from '@/api/patient'

const router = useRouter()

const loading = ref(false)
const errorMsg = ref('')
const doctors = ref([])

const filters = reactive({
  departmentId: '',
  name: '',
  date: '',
})

const pager = reactive({
  page: 1,
  size: 10,
  total: 0,
})

const pages = computed(() => Math.max(1, Math.ceil(pager.total / pager.size)))

const summary = computed(() => ({
  total: doctors.value.length,
  departments: new Set(doctors.value.map((item) => item.department || item.departmentName).filter(Boolean)).size,
  available: doctors.value.filter((item) => remainCountOf(item) > 0).length,
}))

const departmentOptions = computed(() => {
  const map = new Map()
  doctors.value.forEach((item) => {
    const id = item.departmentId || item.department || item.departmentName
    const name = item.department || item.departmentName || id
    if (id && name) map.set(String(id), name)
  })
  return Array.from(map, ([value, label]) => ({ value, label }))
})

async function loadDoctors() {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await getDoctors({
      departmentId: filters.departmentId || undefined,
      name: filters.name || undefined,
      date: filters.date || undefined,
      page: pager.page,
      size: pager.size,
    })
    doctors.value = normalizeResponseList(data)
    pager.total = Array.isArray(data) ? data.length : data?.total || doctors.value.length
  } catch (e) {
    errorMsg.value = e.message || '医生列表加载失败，请稍后重试'
    doctors.value = []
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

function doctorIdOf(item) {
  return item?.doctorId || item?.id || ''
}

function departmentText(item) {
  return item?.department || item?.departmentName || '--'
}

function remainCountOf(item) {
  return Number(item?.remainCount ?? item?.remainingCount ?? item?.sourceCount ?? item?.availableSourceCount ?? 0)
}

function avatarText(name) {
  return (name || '医').slice(0, 1)
}

function search() {
  pager.page = 1
  loadDoctors()
}

function resetFilters() {
  filters.departmentId = ''
  filters.name = ''
  filters.date = ''
  pager.page = 1
  loadDoctors()
}

function prevPage() {
  if (pager.page <= 1) return
  pager.page -= 1
  loadDoctors()
}

function nextPage() {
  if (pager.page >= pages.value) return
  pager.page += 1
  loadDoctors()
}

function goDetail(item) {
  const id = doctorIdOf(item)
  if (!id) {
    errorMsg.value = '当前医生信息不完整，暂时无法查看详情'
    return
  }
  router.push(`/patient/doctors/${id}`)
}

onMounted(loadDoctors)
</script>

<template>
  <div class="doctors-view">
    <section class="page-hero" aria-labelledby="doctors-title">
      <div>
        <p class="eyebrow">Find Doctors</p>
        <h1 id="doctors-title">医生列表</h1>
        <p>按科室、医生姓名和出诊日期筛选医生，选择医生后进入详情页查看号源。</p>
      </div>
      <button class="refresh-btn" :disabled="loading" type="button" @click="loadDoctors">
        {{ loading ? '刷新中' : '刷新列表' }}
      </button>
    </section>

    <section class="summary-grid" aria-label="医生概览">
      <article class="summary-card">
        <span>当前医生</span>
        <strong>{{ summary.total }}</strong>
      </article>
      <article class="summary-card">
        <span>涉及科室</span>
        <strong>{{ summary.departments }}</strong>
      </article>
      <article class="summary-card">
        <span>可预约</span>
        <strong>{{ summary.available }}</strong>
      </article>
    </section>

    <section class="filter-panel" aria-label="医生筛选">
      <label>
        <span>医生姓名</span>
        <input v-model.trim="filters.name" type="search" placeholder="输入医生姓名" @keyup.enter="search" />
      </label>
      <label>
        <span>科室</span>
        <select v-model="filters.departmentId">
          <option value="">全部科室</option>
          <option v-for="item in departmentOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
      <label>
        <span>出诊日期</span>
        <input v-model="filters.date" type="date" />
      </label>
      <button class="primary-btn" :disabled="loading" type="button" @click="search">查询</button>
      <button class="secondary-btn" :disabled="loading" type="button" @click="resetFilters">重置</button>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>

    <section class="doctor-list" aria-label="医生列表">
      <div v-if="loading" class="empty-state">正在请求医生列表接口...</div>
      <div v-else-if="doctors.length === 0" class="empty-state">暂时没有符合条件的医生</div>
      <button v-for="item in doctors" v-else :key="doctorIdOf(item)" class="doctor-card" type="button" @click="goDetail(item)">
        <span class="avatar" aria-hidden="true">{{ avatarText(item.name) }}</span>
        <span class="doctor-main">
          <span class="name-line">
            <strong>{{ item.name || '--' }}</strong>
            <em>{{ item.title || '--' }}</em>
          </span>
          <span class="meta-line">{{ departmentText(item) }}</span>
          <span class="intro-line">{{ item.introduction || item.description || '暂未填写医生简介' }}</span>
        </span>
        <span class="right-meta">
          <span class="source-tag" :class="{ empty: remainCountOf(item) <= 0 }">
            {{ remainCountOf(item) > 0 ? `${remainCountOf(item)} 个号源` : '号源待确认' }}
          </span>
          <span class="detail-text">查看详情</span>
        </span>
      </button>
    </section>

    <div class="pager" aria-label="分页">
      <button :disabled="pager.page <= 1 || loading" type="button" @click="prevPage">上一页</button>
      <span>第 {{ pager.page }} / {{ pages }} 页 · 共 {{ pager.total }} 条</span>
      <button :disabled="pager.page >= pages || loading" type="button" @click="nextPage">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.doctors-view {
  min-height: 100%;
  padding: 14px 16px 24px;
}

.page-hero,
.summary-card,
.filter-panel,
.doctor-list {
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
  padding: 18px;
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
  max-width: 620px;
  margin-top: 8px;
  color: var(--color-ink-3);
  font-size: 13px;
}

.refresh-btn,
.primary-btn,
.secondary-btn {
  min-height: 40px;
  border-radius: var(--radius-sm);
  padding: 0 13px;
  font-size: 13px;
  font-weight: 800;
}

.refresh-btn,
.primary-btn {
  border: 1.5px solid var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}

.secondary-btn {
  border: 1.5px solid var(--color-border-strong);
  background: #fff;
  color: var(--color-primary-deep);
}

button:disabled {
  opacity: 0.48;
  cursor: not-allowed;
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
  font-size: 24px;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.filter-panel {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 150px 160px auto auto;
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

.filter-panel input,
.filter-panel select {
  width: 100%;
  min-height: 40px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0 12px;
  color: var(--color-ink);
  outline: none;
}

.filter-panel input:focus,
.filter-panel select:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(14, 143, 134, 0.12);
}

.notice {
  margin-top: 12px;
  border-radius: var(--radius-sm);
  padding: 11px 13px;
  background: var(--color-danger-soft);
  color: var(--color-danger);
  font-size: 13px;
  font-weight: 700;
}

.doctor-list {
  display: grid;
  gap: 0;
  margin-top: 12px;
  overflow: hidden;
}

.empty-state {
  margin: 14px;
  border: 1px dashed var(--color-border-strong);
  border-radius: var(--radius-sm);
  padding: 32px 16px;
  color: var(--color-ink-3);
  text-align: center;
  font-size: 13px;
}

.doctor-card {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  border: none;
  border-bottom: 1px solid var(--color-border);
  padding: 14px;
  background: #fff;
  text-align: left;
}

.doctor-card:last-child {
  border-bottom: none;
}

.doctor-card:hover,
.doctor-card:focus-visible {
  background: var(--color-primary-soft);
}

.avatar {
  display: grid;
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  place-items: center;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-deep));
  color: #fff;
  font-size: 17px;
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
  font-size: 15px;
}

.name-line em {
  color: var(--color-primary);
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.meta-line,
.intro-line {
  display: block;
  margin-top: 3px;
  color: var(--color-ink-3);
  font-size: 12.5px;
}

.intro-line {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.right-meta {
  display: grid;
  justify-items: end;
  gap: 6px;
  flex-shrink: 0;
}

.source-tag {
  border-radius: 999px;
  padding: 5px 10px;
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-size: 12px;
  font-weight: 800;
}

.source-tag.empty {
  background: var(--color-warn-soft);
  color: var(--color-warn);
}

.detail-text {
  color: var(--color-ink-3);
  font-size: 12px;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 14px;
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

@media (max-width: 820px) {
  .page-hero {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-panel {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 620px) {
  .summary-grid,
  .filter-panel {
    grid-template-columns: 1fr;
  }

  .doctor-card {
    align-items: flex-start;
  }

  .right-meta {
    align-self: stretch;
  }

  .pager {
    flex-direction: column;
  }
}
</style>
