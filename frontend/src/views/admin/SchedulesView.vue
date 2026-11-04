<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  getAdminSources,
  createAdminSource,
  getAdminDoctors,
  getSourceSuspensionPreview,
  updateAdminSource,
  updateSourceStatus,
} from '@/api/admin'

const loading = ref(false)
const actionId = ref('')
const errorMsg = ref('')
const successMsg = ref('')
const sources = ref([])
const pendingAdjust = ref(null)
const pendingStatus = ref(null)
const suspensionPreview = ref(null)
const previewLoading = ref(false)
const summary = reactive({
  total: 0,
  totalCount: 0,
  remainCount: 0,
  lowRemain: 0,
})

const filters = reactive({
  doctorName: '',
  date: '',
  status: '',
})

const pager = reactive({
  page: 1,
  size: 10,
  total: 0,
})

const adjustForm = reactive({
  visitDate: '',
  startTime: '',
  endTime: '',
  totalCount: '',
  reason: '',
})

const adjustErrors = reactive({
  visitDate: '',
  startTime: '',
  endTime: '',
  totalCount: '',
  reason: '',
})

const statusForm = reactive({ reason: '' })
const statusError = ref('')
const createLoading = ref(false)
const createDialogOpen = ref(false)
const createSubmitError = ref('')
const doctorLoading = ref(false)
const doctorOptions = ref([])
const createForm = reactive({
  doctorId: '',
  visitDate: '',
  startTime: '08:00',
  endTime: '12:00',
  totalCount: 20,
})
const createErrors = reactive({
  doctorId: '',
  visitDate: '',
  startTime: '',
  endTime: '',
  totalCount: '',
})

async function openCreateDialog() {
  createForm.doctorId = ''
  createForm.visitDate = todayText()
  createForm.startTime = '08:00'
  createForm.endTime = '12:00'
  createForm.totalCount = 20
  clearCreateErrors()
  createSubmitError.value = ''
  errorMsg.value = ''
  successMsg.value = ''
  createDialogOpen.value = true
  if (doctorOptions.value.length) return
  doctorLoading.value = true
  try {
    const data = await getAdminDoctors({ page: 1, size: 100, status: 'NORMAL' })
    doctorOptions.value = normalizeResponseList(data).filter(
      (doctor) => doctor.doctorStatus === '启用' && doctor.status === 'NORMAL',
    )
    if (!doctorOptions.value.length) {
      createErrors.doctorId = '暂无可排班的启用医生'
    }
  } catch (e) {
    createErrors.doctorId = e.message || '医生列表加载失败，请稍后重试'
  } finally {
    doctorLoading.value = false
  }
}

function closeCreateDialog() {
  if (createLoading.value) return
  createDialogOpen.value = false
}

function clearCreateErrors() {
  Object.keys(createErrors).forEach((key) => {
    createErrors[key] = ''
  })
}

function validateCreate() {
  clearCreateErrors()
  createSubmitError.value = ''
  if (!createForm.doctorId) createErrors.doctorId = '请选择出诊医生'
  if (!createForm.visitDate) createErrors.visitDate = '请选择出诊日期'
  else if (createForm.visitDate < todayText()) createErrors.visitDate = '出诊日期不能早于今天'
  if (!createForm.startTime) createErrors.startTime = '请选择开始时间'
  if (!createForm.endTime) createErrors.endTime = '请选择结束时间'
  if (createForm.startTime && createForm.endTime && createForm.startTime >= createForm.endTime) {
    createErrors.endTime = '结束时间必须晚于开始时间'
  } else if (createForm.visitDate === todayText() && createForm.endTime <= currentTimeText()) {
    createErrors.endTime = '不能新建已经结束的出诊时段'
  }
  if (!Number.isInteger(Number(createForm.totalCount)) || Number(createForm.totalCount) <= 0) {
    createErrors.totalCount = '请输入大于 0 的整数'
  }
  return !Object.values(createErrors).some(Boolean)
}

async function submitCreateSource() {
  if (createLoading.value || !validateCreate()) return
  createLoading.value = true
  errorMsg.value = ''
  try {
    const result = await createAdminSource({
      doctorId: Number(createForm.doctorId),
      visitDate: createForm.visitDate,
      startTime: createForm.startTime,
      endTime: createForm.endTime,
      totalCount: Number(createForm.totalCount),
    })
    createDialogOpen.value = false
    pager.page = 1
    await loadSources()
    successMsg.value = `号源 ${result?.sourceId || ''} 新建成功`
  } catch (e) {
    createSubmitError.value = e.message || '新建号源失败，请检查填写内容'
  } finally {
    createLoading.value = false
  }
}

const pages = computed(() => Math.max(1, Math.ceil(pager.total / pager.size)))
const selectedCreateDoctor = computed(() => doctorOptions.value.find(
  (doctor) => String(doctor.doctorId) === String(createForm.doctorId),
))
const createFee = computed(() => {
  if (selectedCreateDoctor.value?.title === '主任医师') return 50
  if (selectedCreateDoctor.value?.title === '副主任医师') return 30
  return 20
})

async function loadSources() {
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const data = await getAdminSources({
      doctorName: filters.doctorName || undefined,
      date: filters.date || undefined,
      status: filters.status || undefined,
      page: pager.page,
      size: pager.size,
    })
    sources.value = normalizeResponseList(data)
    pager.total = Array.isArray(data) ? data.length : data?.total || sources.value.length
    applyOverview(data?.overview)
  } catch (e) {
    errorMsg.value = e.message || '号源数据加载失败，请稍后重试'
    sources.value = []
    pager.total = 0
    applyOverview()
  } finally {
    loading.value = false
  }
}

/** 概览由后端基于全部筛选结果统计，不再根据当前分页列表计算。 */
function applyOverview(overview = {}) {
  summary.total = Number(overview?.currentSources || 0)
  summary.totalCount = Number(overview?.totalCount || 0)
  summary.remainCount = Number(overview?.remainCount || 0)
  summary.lowRemain = Number(overview?.lowRemain || 0)
}

function normalizeResponseList(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data?.rows)) return data.rows
  return []
}

function sourceIdOf(item) {
  return item?.sourceId || item?.id || ''
}

function remainCountOf(item) {
  return item?.remainCount ?? item?.remainingCount ?? item?.availableCount ?? 0
}

function dateOf(item) {
  return item?.date || item?.visitDate || item?.scheduleDate || '--'
}

function doctorText(item) {
  return item?.doctorName || item?.doctor?.name || item?.doctorId || '--'
}

function departmentText(item) {
  return item?.department || item?.departmentName || item?.doctor?.department || '--'
}

function timeText(item) {
  const start = item?.startTime || item?.start || ''
  const end = item?.endTime || item?.end || ''
  if (start && end) return `${start} - ${end}`
  return item?.timePeriod || item?.timeRange || item?.period || '--'
}

function statusText(item) {
  const status = item?.status
  return status || '正常'
}

function statusClass(item) {
  const status = statusText(item)
  return {
    warning: status === '低余量',
    full: status === '已约满',
    ended: status === '已结束',
    suspended: status === '已停诊',
  }
}

function isSuspended(item) {
  return statusText(item) === '已停诊'
}

function canManageStatus(item) {
  return statusText(item) !== '已结束'
}

function search() {
  pager.page = 1
  loadSources()
}

function prevPage() {
  if (pager.page <= 1) return
  pager.page -= 1
  loadSources()
}

function nextPage() {
  if (pager.page >= pages.value) return
  pager.page += 1
  loadSources()
}

function openAdjust(item) {
  pendingAdjust.value = item
  adjustForm.visitDate = String(dateOf(item) === '--' ? '' : dateOf(item))
  adjustForm.startTime = timeInputValue(item?.startTime) || periodStart(item?.timePeriod || item?.period)
  adjustForm.endTime = timeInputValue(item?.endTime) || periodEnd(item?.timePeriod || item?.period)
  adjustForm.totalCount = String(item.totalCount ?? '')
  adjustForm.reason = ''
  clearAdjustErrors()
  errorMsg.value = ''
  successMsg.value = ''
}

function closeAdjust() {
  if (actionId.value) return
  pendingAdjust.value = null
}

function validateAdjust() {
  clearAdjustErrors()
  const totalCount = Number(adjustForm.totalCount)
  const bookedCount = bookedCountOf(pendingAdjust.value)
  const scheduleChanged = isScheduleChanged()

  if (!adjustForm.visitDate) {
    adjustErrors.visitDate = '请选择出诊日期'
  } else if (adjustForm.visitDate < todayText()) {
    adjustErrors.visitDate = '出诊日期不能早于今天'
  }
  if (!adjustForm.startTime) {
    adjustErrors.startTime = '请选择开始时间'
  }
  if (!adjustForm.endTime) {
    adjustErrors.endTime = '请选择结束时间'
  }
  if (adjustForm.startTime && adjustForm.endTime && adjustForm.startTime >= adjustForm.endTime) {
    adjustErrors.endTime = '结束时间必须晚于开始时间'
  }
  if (!Number.isInteger(totalCount) || totalCount <= 0) {
    adjustErrors.totalCount = '请输入大于 0 的整数'
  } else if (totalCount < bookedCount) {
    adjustErrors.totalCount = `总号数不能小于已预约数量 ${bookedCount}`
  }
  if (scheduleChanged && !adjustForm.reason.trim()) {
    adjustErrors.reason = '调整出诊日期或时段时必须填写原因'
  } else if (adjustForm.reason.trim().length > 100) {
    adjustErrors.reason = '调整原因不能超过 100 个字符'
  }
  return !Object.values(adjustErrors).some(Boolean)
}

function clearAdjustErrors() {
  Object.keys(adjustErrors).forEach((key) => {
    adjustErrors[key] = ''
  })
}

function todayText() {
  const now = new Date()
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60_000)
  return local.toISOString().slice(0, 10)
}

function currentTimeText() {
  const now = new Date()
  return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
}

function bookedCountOf(item) {
  if (item?.bookedCount != null) return Math.max(0, Number(item.bookedCount))
  return Math.max(0, Number(item?.totalCount || 0) - Number(remainCountOf(item) || 0))
}

function isScheduleChanged() {
  if (!pendingAdjust.value) return false
  return adjustForm.visitDate !== dateOf(pendingAdjust.value)
    || adjustForm.startTime !== timeInputValue(pendingAdjust.value?.startTime)
    || adjustForm.endTime !== timeInputValue(pendingAdjust.value?.endTime)
}

function timeInputValue(value) {
  return value ? String(value).slice(0, 5) : ''
}

function periodStart(period) {
  return period === '上午' ? '08:00' : period === '下午' ? '14:00' : ''
}

function periodEnd(period) {
  return period === '上午' ? '12:00' : period === '下午' ? '18:00' : ''
}

async function confirmAdjust() {
  if (!pendingAdjust.value || actionId.value || !validateAdjust()) return
  const id = sourceIdOf(pendingAdjust.value)
  if (!id) {
    errorMsg.value = '当前号源信息不完整，请刷新列表后重试'
    return
  }
  actionId.value = String(id)
  errorMsg.value = ''
  successMsg.value = ''
  try {
    await updateAdminSource(id, {
      visitDate: adjustForm.visitDate,
      startTime: adjustForm.startTime,
      endTime: adjustForm.endTime,
      totalCount: Number(adjustForm.totalCount),
      reason: adjustForm.reason.trim() || undefined,
    })
    const message = `${doctorText(pendingAdjust.value)} ${dateOf(pendingAdjust.value)} 号源已提交调整`
    pendingAdjust.value = null
    await loadSources()
    successMsg.value = message
  } catch (e) {
    errorMsg.value = e.message || '号源调整失败，请检查填写内容'
  } finally {
    actionId.value = ''
  }
}

async function openStatusDialog(item) {
  if (!canManageStatus(item) || actionId.value) return
  pendingStatus.value = item
  suspensionPreview.value = null
  statusForm.reason = ''
  statusError.value = ''
  errorMsg.value = ''
  successMsg.value = ''
  if (isSuspended(item)) return

  const id = sourceIdOf(item)
  previewLoading.value = true
  try {
    suspensionPreview.value = await getSourceSuspensionPreview(id)
  } catch (e) {
    errorMsg.value = e.message || '停诊影响预览加载失败'
    pendingStatus.value = null
  } finally {
    previewLoading.value = false
  }
}

function closeStatusDialog() {
  if (actionId.value) return
  pendingStatus.value = null
  suspensionPreview.value = null
  statusError.value = ''
}

async function confirmStatusChange() {
  if (!pendingStatus.value || actionId.value || previewLoading.value) return
  const restoring = isSuspended(pendingStatus.value)
  const reason = statusForm.reason.trim()
  statusError.value = ''
  if (!restoring && !reason) {
    statusError.value = '停诊原因不能为空'
    return
  }
  if (reason.length > 100) {
    statusError.value = '原因不能超过 100 个字符'
    return
  }
  if (!restoring && suspensionPreview.value?.canSuspend === false) {
    statusError.value = suspensionPreview.value?.blockedReason || '当前号源不能停诊'
    return
  }

  const id = sourceIdOf(pendingStatus.value)
  actionId.value = String(id)
  try {
    const result = await updateSourceStatus(id, {
      status: restoring ? 'NORMAL' : 'SUSPENDED',
      reason: reason || undefined,
    })
    const message = restoring
      ? `号源 ${id} 已恢复`
      : `号源 ${id} 已停诊，已处理 ${result?.processedAppointmentCount || 0} 条预约`
    pendingStatus.value = null
    suspensionPreview.value = null
    await loadSources()
    successMsg.value = message
  } catch (e) {
    statusError.value = e.message || '号源状态修改失败'
  } finally {
    actionId.value = ''
  }
}

onMounted(loadSources)
</script>

<template>
  <div class="schedules-view">
    <section class="page-hero" aria-labelledby="schedules-title">
      <div>
        <p class="eyebrow">Source Administration</p>
        <h1 id="schedules-title">号源管理</h1>
        <p>按医生和日期查询号源，调整总号数时校验剩余号数不超过总号数。</p>
      </div>
      <button class="refresh-btn" :disabled="loading" type="button" @click="loadSources">
        {{ loading ? '刷新中' : '刷新列表' }}
      </button>
    </section>

    <section class="summary-grid" aria-label="号源概览">
      <article class="summary-card">
        <span>当前号源</span>
        <strong>{{ summary.total }}</strong>
      </article>
      <article class="summary-card">
        <span>总号数</span>
        <strong>{{ summary.totalCount }}</strong>
      </article>
      <article class="summary-card">
        <span>剩余号数</span>
        <strong>{{ summary.remainCount }}</strong>
      </article>
      <article class="summary-card warn-card">
        <span>低余量</span>
        <strong>{{ summary.lowRemain }}</strong>
      </article>
    </section>

    <section class="toolbar-panel" aria-label="号源筛选">
      <label>
        <span>医生姓名</span>
        <input v-model.trim="filters.doctorName" type="search" placeholder="输入医生姓名" @keyup.enter="search" />
      </label>
      <label>
        <span>出诊日期</span>
        <input v-model="filters.date" type="date" />
      </label>
      <label>
        <span>号源状态</span>
        <select v-model="filters.status">
          <option value="">全部状态</option>
          <option value="正常">正常</option>
          <option value="低余量">低余量</option>
          <option value="已约满">已约满</option>
          <option value="已停诊">已停诊</option>
          <option value="已结束">已结束</option>
        </select>
      </label>
      <button class="primary-btn" :disabled="loading" type="button" @click="search">查询</button>
      <button class="primary-btn" type="button" :disabled="createLoading" @click="openCreateDialog">
        新建号源
      </button>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>
    <p v-if="successMsg" class="notice success" role="status">{{ successMsg }}</p>

    <section class="table-panel" aria-label="号源列表">
      <div v-if="loading" class="empty-state">正在请求号源管理接口...</div>
      <div v-else-if="sources.length === 0" class="empty-state">当前筛选条件下暂无号源</div>
      <table v-else>
        <thead>
          <tr>
            <th>号源ID</th>
            <th>医生</th>
            <th>科室</th>
            <th>出诊日期</th>
            <th>时间段</th>
            <th>总号数</th>
            <th>剩余号数</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in sources" :key="sourceIdOf(item) || `${doctorText(item)}-${dateOf(item)}`">
            <td>{{ sourceIdOf(item) || '--' }}</td>
            <td>{{ doctorText(item) }}</td>
            <td>{{ departmentText(item) }}</td>
            <td>{{ dateOf(item) }}</td>
            <td>{{ timeText(item) }}</td>
            <td class="number-cell">{{ item.totalCount ?? '--' }}</td>
            <td class="number-cell" :class="{ warning: Number(remainCountOf(item)) <= 5 }">
              {{ remainCountOf(item) ?? '--' }}
            </td>
            <td>
              <span class="status-tag" :class="statusClass(item)">
                {{ statusText(item) }}
              </span>
            </td>
            <td>
              <div class="row-actions">
                <button class="link-btn" :disabled="!!actionId || isSuspended(item) || !canManageStatus(item)" type="button" @click="openAdjust(item)">
                  调整号源
                </button>
                <button
                  :class="isSuspended(item) ? 'restore-btn' : 'suspend-btn'"
                  :disabled="!!actionId || !canManageStatus(item)"
                  type="button"
                  @click="openStatusDialog(item)"
                >
                  {{ isSuspended(item) ? '恢复号源' : '停诊号源' }}
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <div class="pager" aria-label="分页">
      <button :disabled="pager.page <= 1 || loading" type="button" @click="prevPage">上一页</button>
      <span>第 {{ pager.page }} / {{ pages }} 页 · 共 {{ pager.total }} 条</span>
      <button :disabled="pager.page >= pages || loading" type="button" @click="nextPage">下一页</button>
    </div>

    <div v-if="createDialogOpen" class="modal-mask" role="presentation" @click.self="closeCreateDialog">
      <section class="confirm-panel create-panel" role="dialog" aria-modal="true" aria-labelledby="create-title">
        <h2 id="create-title">新建号源</h2>
        <p>选择出诊医生并设置具体日期、时间、号数和挂号费用。</p>
        <p v-if="createSubmitError" class="block-warning" role="alert">{{ createSubmitError }}</p>

        <label>
          <span>出诊医生（必填）</span>
          <select
            v-model="createForm.doctorId"
            :disabled="doctorLoading"
            :aria-invalid="!!createErrors.doctorId"
            aria-describedby="create-doctor-error"
          >
            <option value="">{{ doctorLoading ? '正在加载医生...' : '请选择医生' }}</option>
            <option v-for="doctor in doctorOptions" :key="doctor.doctorId" :value="doctor.doctorId">
              {{ doctor.name }} · {{ doctor.department }} · {{ doctor.title || '医师' }}
            </option>
          </select>
          <small v-if="createErrors.doctorId" id="create-doctor-error" class="field-error">{{ createErrors.doctorId }}</small>
        </label>

        <div class="adjust-grid">
          <label>
            <span>出诊日期（必填）</span>
            <input v-model="createForm.visitDate" type="date" :min="todayText()" :aria-invalid="!!createErrors.visitDate" />
            <small v-if="createErrors.visitDate" class="field-error">{{ createErrors.visitDate }}</small>
          </label>
          <label>
            <span>总号数（必填）</span>
            <input v-model.number="createForm.totalCount" type="number" min="1" step="1" inputmode="numeric" :aria-invalid="!!createErrors.totalCount" />
            <small v-if="createErrors.totalCount" class="field-error">{{ createErrors.totalCount }}</small>
          </label>
          <label>
            <span>开始时间（必填）</span>
            <input v-model="createForm.startTime" type="time" :aria-invalid="!!createErrors.startTime" />
            <small v-if="createErrors.startTime" class="field-error">{{ createErrors.startTime }}</small>
          </label>
          <label>
            <span>结束时间（必填）</span>
            <input v-model="createForm.endTime" type="time" :aria-invalid="!!createErrors.endTime" />
            <small v-if="createErrors.endTime" class="field-error">{{ createErrors.endTime }}</small>
          </label>
        </div>

        <label>
          <span>挂号费用</span>
          <input :value="`¥${createFee.toFixed(2)}`" type="text" disabled />
          <small class="input-helper">系统按医生职称自动设置，预约与支付订单将使用同一金额。</small>
        </label>

        <div class="confirm-actions">
          <button class="secondary-btn" :disabled="createLoading" type="button" @click="closeCreateDialog">取消</button>
          <button class="primary-btn" :disabled="createLoading || doctorLoading" type="button" @click="submitCreateSource">
            {{ createLoading ? '创建中...' : '确认创建' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="pendingAdjust" class="modal-mask" role="presentation">
      <section class="confirm-panel" role="dialog" aria-modal="true" aria-labelledby="adjust-title">
        <h2 id="adjust-title">调整号源</h2>
        <p>
          {{ doctorText(pendingAdjust) }} · {{ dateOf(pendingAdjust) }} · 当前剩余
          <strong>{{ remainCountOf(pendingAdjust) }}</strong>
          个号源 · 已预约 <strong>{{ bookedCountOf(pendingAdjust) }}</strong> 个
        </p>
        <p v-if="bookedCountOf(pendingAdjust) > 0" class="form-helper">
          当前号源已有预约，只能调整总号数，不能修改出诊日期和时段。
        </p>
        <div class="adjust-grid">
          <label>
            <span>出诊日期</span>
            <input
              v-model="adjustForm.visitDate"
              type="date"
              :min="todayText()"
              :disabled="bookedCountOf(pendingAdjust) > 0"
              :aria-invalid="!!adjustErrors.visitDate"
              aria-describedby="visit-date-error"
            />
            <small v-if="adjustErrors.visitDate" id="visit-date-error" class="field-error">
              {{ adjustErrors.visitDate }}
            </small>
          </label>
          <label>
            <span>出诊开始时间</span>
            <input
              v-model="adjustForm.startTime"
              type="time"
              :disabled="bookedCountOf(pendingAdjust) > 0"
              :aria-invalid="!!adjustErrors.startTime"
              aria-describedby="start-time-error"
            />
            <small v-if="adjustErrors.startTime" id="start-time-error" class="field-error">
              {{ adjustErrors.startTime }}
            </small>
          </label>
          <label>
            <span>出诊结束时间</span>
            <input
              v-model="adjustForm.endTime"
              type="time"
              :disabled="bookedCountOf(pendingAdjust) > 0"
              :aria-invalid="!!adjustErrors.endTime"
              aria-describedby="end-time-error"
            />
            <small v-if="adjustErrors.endTime" id="end-time-error" class="field-error">
              {{ adjustErrors.endTime }}
            </small>
          </label>
        </div>
        <label>
          <span>总号数</span>
          <input
            v-model="adjustForm.totalCount"
            type="number"
            min="1"
            inputmode="numeric"
            :aria-invalid="!!adjustErrors.totalCount"
            aria-describedby="total-count-error"
          />
        </label>
        <small v-if="adjustErrors.totalCount" id="total-count-error" class="field-error">
          {{ adjustErrors.totalCount }}
        </small>
        <label>
          <span>调整原因{{ isScheduleChanged() ? '（必填）' : '（选填）' }}</span>
          <textarea
            v-model="adjustForm.reason"
            maxlength="100"
            rows="3"
            placeholder="修改出诊日期或时段时，请说明调整原因"
            :aria-invalid="!!adjustErrors.reason"
            aria-describedby="adjust-reason-help adjust-reason-error"
          ></textarea>
          <small id="adjust-reason-help" class="input-helper">最多 100 个字符</small>
          <small v-if="adjustErrors.reason" id="adjust-reason-error" class="field-error">
            {{ adjustErrors.reason }}
          </small>
        </label>
        <div class="confirm-actions">
          <button class="secondary-btn" :disabled="!!actionId" type="button" @click="closeAdjust">取消</button>
          <button class="primary-btn" :disabled="!!actionId" type="button" @click="confirmAdjust">
            {{ actionId ? '提交中...' : '确认调整' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="pendingStatus" class="modal-mask" role="presentation">
      <section class="confirm-panel status-panel" role="dialog" aria-modal="true" aria-labelledby="status-title">
        <h2 id="status-title">{{ isSuspended(pendingStatus) ? '恢复号源' : '确认停诊' }}</h2>
        <p>
          {{ doctorText(pendingStatus) }} · {{ dateOf(pendingStatus) }} · {{ timeText(pendingStatus) }}
        </p>

        <div v-if="previewLoading" class="preview-loading" role="status">正在计算受影响预约...</div>
        <template v-else-if="!isSuspended(pendingStatus) && suspensionPreview">
          <div class="impact-grid" aria-label="停诊影响预览">
            <article><span>受影响预约</span><strong>{{ suspensionPreview.affectedAppointmentCount || 0 }}</strong></article>
            <article><span>待支付取消</span><strong>{{ suspensionPreview.pendingPaymentCount || 0 }}</strong></article>
            <article><span>已支付退款</span><strong>{{ suspensionPreview.estimatedRefundCount || 0 }}</strong></article>
            <article><span>需人工处理</span><strong>{{ Number(suspensionPreview.signedCount || 0) + Number(suspensionPreview.inConsultationCount || 0) + Number(suspensionPreview.refundingCount || 0) }}</strong></article>
          </div>
          <p v-if="suspensionPreview.canSuspend === false" class="block-warning" role="alert">
            {{ suspensionPreview.blockedReason || '当前号源不能自动停诊' }}
          </p>
          <p v-else class="form-helper">
            停诊后，待支付预约会自动取消；已支付预约将进入退款流程并通知患者。
          </p>
        </template>
        <p v-else class="form-helper">恢复后患者可以重新查询并预约该号源。</p>

        <label>
          <span>{{ isSuspended(pendingStatus) ? '恢复说明（选填）' : '停诊原因（必填）' }}</span>
          <textarea
            v-model="statusForm.reason"
            maxlength="100"
            rows="3"
            :placeholder="isSuspended(pendingStatus) ? '记录恢复原因' : '例如：医生临时请假'"
            :aria-invalid="!!statusError"
            aria-describedby="status-reason-help status-reason-error"
          ></textarea>
          <small id="status-reason-help" class="input-helper">最多 100 个字符</small>
          <small v-if="statusError" id="status-reason-error" class="field-error">{{ statusError }}</small>
        </label>
        <div class="confirm-actions">
          <button class="secondary-btn" :disabled="!!actionId" type="button" @click="closeStatusDialog">取消</button>
          <button
            :class="isSuspended(pendingStatus) ? 'primary-btn' : 'danger-btn'"
            :disabled="!!actionId || previewLoading || (!isSuspended(pendingStatus) && suspensionPreview?.canSuspend === false)"
            type="button"
            @click="confirmStatusChange"
          >
            {{ actionId ? '提交中...' : (isSuspended(pendingStatus) ? '确认恢复' : '确认停诊') }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.schedules-view {
  min-height: 100%;
}

.page-hero,
.summary-card,
.toolbar-panel,
.table-panel,
.confirm-panel {
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

.refresh-btn,
.primary-btn,
.secondary-btn,
.wait-btn,
.link-btn {
  min-height: 40px;
  border-radius: var(--radius-sm);
  padding: 0 13px;
  font-size: 13px;
  font-weight: 800;
}

.row-actions {
  display: flex;
  gap: 8px;
}

.suspend-btn,
.restore-btn,
.danger-btn {
  min-height: 40px;
  border-radius: var(--radius-sm);
  padding: 0 13px;
  font-size: 13px;
  font-weight: 800;
}

.suspend-btn,
.danger-btn {
  border: 1.5px solid var(--color-danger);
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.restore-btn {
  border: 1.5px solid var(--color-accent);
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.refresh-btn,
.primary-btn {
  border: 1.5px solid var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}

.secondary-btn,
.link-btn {
  border: 1.5px solid var(--color-border-strong);
  background: #fff;
  color: var(--color-primary-deep);
}

.wait-btn {
  border: 1.5px dashed var(--color-border-strong);
  background: var(--color-primary-soft);
  color: var(--color-ink-3);
}

button:disabled {
  opacity: 0.48;
  cursor: not-allowed;
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
  display: block;
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 700;
}

.summary-card strong {
  display: block;
  margin-top: 6px;
  color: var(--color-ink);
  font-size: 28px;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.warn-card strong {
  color: var(--color-warn);
}

.toolbar-panel {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 180px 150px auto auto;
  gap: 10px;
  align-items: end;
  margin-top: 14px;
  padding: 14px;
}

.toolbar-panel label span,
.confirm-panel label span {
  display: block;
  margin-bottom: 6px;
  color: var(--color-ink-2);
  font-size: 13px;
  font-weight: 800;
}

.toolbar-panel input,
.toolbar-panel select,
.confirm-panel input,
.confirm-panel select,
.confirm-panel textarea {
  width: 100%;
  min-height: 40px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0 12px;
  color: var(--color-ink);
  outline: none;
}

.confirm-panel textarea {
  min-height: 84px;
  padding-top: 10px;
  resize: vertical;
}

.toolbar-panel input:focus,
.toolbar-panel select:focus,
.confirm-panel input:focus,
.confirm-panel select:focus,
.confirm-panel textarea:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(14, 143, 134, 0.12);
}

.adjust-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 14px;
}

.adjust-grid label,
.adjust-grid label + label {
  margin-top: 0;
}

.form-helper {
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  background: var(--color-primary-soft);
  color: var(--color-ink-2) !important;
  line-height: 1.55;
}

.input-helper {
  display: block;
  margin-top: 6px;
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

.table-panel {
  margin-top: 14px;
  overflow: auto;
}

.empty-state {
  margin: 16px;
  border: 1px dashed var(--color-border-strong);
  border-radius: var(--radius-sm);
  padding: 32px 16px;
  color: var(--color-ink-3);
  text-align: center;
  font-size: 13px;
}

table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
}

th,
td {
  border-bottom: 1px solid var(--color-border);
  padding: 12px 14px;
  color: var(--color-ink-2);
  text-align: left;
  font-size: 13px;
  white-space: nowrap;
}

th {
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
  font-weight: 900;
}

tr:last-child td {
  border-bottom: none;
}

.number-cell {
  font-variant-numeric: tabular-nums;
}

.number-cell.warning {
  color: var(--color-warn);
  font-weight: 900;
}

.status-tag {
  display: inline-flex;
  border-radius: 999px;
  padding: 5px 10px;
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-size: 12px;
  font-weight: 800;
}

.status-tag.warning {
  background: var(--color-warn-soft, #fff4db);
  color: var(--color-warn);
}

.status-tag.full {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.status-tag.ended {
  background: var(--color-primary-soft);
  color: var(--color-ink-3);
}

.status-tag.suspended {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
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

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: grid;
  place-items: center;
  padding: 18px;
  background: rgba(11, 78, 74, 0.28);
}

.confirm-panel {
  width: min(440px, 100%);
  padding: 18px;
}

.status-panel {
  width: min(560px, 100%);
}

.create-panel {
  width: min(560px, 100%);
  max-height: calc(100vh - 36px);
  overflow-y: auto;
}

.preview-loading,
.block-warning {
  margin-top: 14px;
  border-radius: var(--radius-sm);
  padding: 12px;
  font-size: 13px;
  font-weight: 700;
}

.preview-loading {
  background: var(--color-primary-soft);
  color: var(--color-ink-2);
}

.block-warning {
  background: var(--color-danger-soft);
  color: var(--color-danger) !important;
}

.impact-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-top: 14px;
}

.impact-grid article {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 10px;
  background: var(--color-primary-soft);
}

.impact-grid span,
.impact-grid strong {
  display: block;
}

.impact-grid span {
  color: var(--color-ink-3);
  font-size: 12px;
}

.impact-grid strong {
  margin-top: 5px;
  color: var(--color-ink);
  font-size: 20px;
  font-variant-numeric: tabular-nums;
}

.confirm-panel h2 {
  color: var(--color-ink);
  font-size: 18px;
  font-weight: 800;
}

.confirm-panel p {
  margin-top: 10px;
  color: var(--color-ink-3);
  font-size: 13px;
}

.confirm-panel strong {
  color: var(--color-ink);
}

.confirm-panel label {
  display: block;
  margin-top: 14px;
}

.field-error {
  display: block;
  margin-top: 7px;
  color: var(--color-danger);
  font-size: 12.5px;
  font-weight: 700;
}

.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

@media (max-width: 980px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .page-hero {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-panel {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 620px) {
  .summary-grid,
  .toolbar-panel {
    grid-template-columns: 1fr;
  }

  .page-hero,
  .toolbar-panel {
    padding: 14px;
  }

  .confirm-actions,
  .pager {
    flex-direction: column;
  }

  .adjust-grid {
    grid-template-columns: 1fr;
  }

  .impact-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
