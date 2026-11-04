<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  auditDoctor,
  getAdminDoctorDetail,
  getAdminDoctorHistory,
  getAdminDoctors,
  getDoctorAudits,
  updateDoctorStatus,
} from '@/api/admin'

const activeTab = ref('audit')
const loading = ref(false)
const actionId = ref('')
const errorMsg = ref('')
const successMsg = ref('')
const auditDoctors = ref([])
const managedDoctors = ref([])
const pendingAudit = ref(null)
const auditReason = ref('')
const auditReasonError = ref('')
const pendingStatus = ref(null)
const statusReason = ref('')
const statusReasonError = ref('')
const doctorDetail = ref(null)
const detailLoadingId = ref('')
const doctorHistory = ref([])
const historyLoading = ref(false)
const historyError = ref('')
const historyPager = reactive({ page: 1, size: 5, total: 0 })
const historyPages = computed(() => Math.max(1, Math.ceil(historyPager.total / historyPager.size)))

const filters = reactive({
  keyword: '',
  status: '',
})

const pager = reactive({
  page: 1,
  size: 10,
  total: 0,
})

const pages = computed(() => Math.max(1, Math.ceil(pager.total / pager.size)))

const summary = computed(() => ({
  pending: auditDoctors.value.length,
  doctors: managedDoctors.value.length,
  enabled: managedDoctors.value.filter((item) => !isDisabled(item)).length,
  disabled: managedDoctors.value.filter((item) => isDisabled(item)).length,
}))

async function loadCurrentTab() {
  if (activeTab.value === 'audit') {
    await loadAuditDoctors()
  } else {
    await loadManagedDoctors()
  }
}

async function loadAuditDoctors() {
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const data = await getDoctorAudits({ page: pager.page, size: pager.size })
    auditDoctors.value = normalizeResponseList(data)
    pager.total = Array.isArray(data) ? data.length : data?.total || auditDoctors.value.length
  } catch (e) {
    errorMsg.value = e.message || '待审核医生加载失败，请稍后重试'
    auditDoctors.value = []
    pager.total = 0
  } finally {
    loading.value = false
  }
}

async function loadManagedDoctors() {
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const data = await getAdminDoctors({
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      page: pager.page,
      size: pager.size,
    })
    managedDoctors.value = normalizeResponseList(data)
    pager.total = Array.isArray(data) ? data.length : data?.total || managedDoctors.value.length
  } catch (e) {
    errorMsg.value = e.message || '医生账号加载失败，请稍后重试'
    managedDoctors.value = []
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
  return item?.doctorId || item?.id || item?.userId || ''
}

function isDisabled(item) {
  return ['DISABLED', '禁用', '停用'].includes(item?.status)
}

function statusText(status) {
  const map = {
    PENDING: '待审核',
    PASS: '已通过',
    APPROVED: '已通过',
    REJECT: '已拒绝',
    REJECTED: '已拒绝',
    NORMAL: '正常',
    ACTIVE: '正常',
    DISABLED: '禁用',
  }
  return map[status] || status || '--'
}

function statusTone(status) {
  if (['PASS', 'APPROVED', 'NORMAL', 'ACTIVE', '已通过', '正常'].includes(status)) return 'success'
  if (['REJECT', 'REJECTED', 'DISABLED', '已拒绝', '禁用'].includes(status)) return 'danger'
  return 'warning'
}

function switchTab(tab) {
  activeTab.value = tab
  pager.page = 1
  pendingAudit.value = null
  loadCurrentTab()
}

function search() {
  pager.page = 1
  loadCurrentTab()
}

function prevPage() {
  if (pager.page <= 1) return
  pager.page -= 1
  loadCurrentTab()
}

function nextPage() {
  if (pager.page >= pages.value) return
  pager.page += 1
  loadCurrentTab()
}

function openAuditConfirm(item, result) {
  pendingAudit.value = { item, result }
  auditReason.value = ''
  auditReasonError.value = ''
  errorMsg.value = ''
  successMsg.value = ''
}

function closeAuditConfirm() {
  if (actionId.value) return
  pendingAudit.value = null
  auditReason.value = ''
  auditReasonError.value = ''
}

async function confirmAudit() {
  if (!pendingAudit.value || actionId.value) return
  const { item, result } = pendingAudit.value
  const id = doctorIdOf(item)
  if (!id) return
  const reason = auditReason.value.trim()
  if (result === 'REJECT' && !reason) {
    auditReasonError.value = '请填写拒绝理由，便于医生修改资料'
    return
  }
  actionId.value = `${result}-${id}`
  errorMsg.value = ''
  successMsg.value = ''
  try {
    await auditDoctor(id, {
      result,
      reason: result === 'REJECT' ? reason : undefined,
    })
    successMsg.value = `${item.name || '医生'}审核已${result === 'PASS' ? '通过' : '拒绝'}`
    pendingAudit.value = null
    await loadAuditDoctors()
  } catch (e) {
    errorMsg.value = e.message || '医生审核提交失败，请稍后重试'
  } finally {
    actionId.value = ''
  }
}

function openStatusConfirm(doctor) {
  const disabled = isDisabled(doctor)
  pendingStatus.value = {
    doctor,
    nextStatus: disabled ? 'NORMAL' : 'DISABLED',
  }
  statusReason.value = ''
  statusReasonError.value = ''
  errorMsg.value = ''
  successMsg.value = ''
}

function closeStatusConfirm() {
  if (actionId.value) return
  pendingStatus.value = null
  statusReason.value = ''
  statusReasonError.value = ''
}

async function confirmStatusChange() {
  if (!pendingStatus.value || actionId.value) return
  const { doctor, nextStatus } = pendingStatus.value
  const id = doctorIdOf(doctor)
  if (!id) return

  const reason = statusReason.value.trim()
  if (nextStatus === 'DISABLED' && !reason) {
    statusReasonError.value = '请填写禁用原因，便于后续追溯'
    return
  }

  actionId.value = `status-${id}`
  errorMsg.value = ''
  successMsg.value = ''
  try {
    await updateDoctorStatus(id, {
      status: nextStatus,
      reason: nextStatus === 'DISABLED' ? reason : undefined,
    })
    successMsg.value = `${doctor.name || '医生'}账号已${nextStatus === 'DISABLED' ? '禁用' : '恢复'}`
    pendingStatus.value = null
    await loadManagedDoctors()
  } catch (e) {
    errorMsg.value = e.message || '医生状态修改失败'
  } finally {
    actionId.value = ''
  }
}

async function openDoctorDetail(doctor) {
  const id = doctorIdOf(doctor)
  if (!id || detailLoadingId.value) return
  detailLoadingId.value = String(id)
  errorMsg.value = ''
  try {
    doctorDetail.value = await getAdminDoctorDetail(id)
    historyPager.page = 1
    await loadDoctorHistory()
  } catch (e) {
    errorMsg.value = e.message || '医生详情加载失败'
  } finally {
    detailLoadingId.value = ''
  }
}

function closeDoctorDetail() {
  doctorDetail.value = null
  doctorHistory.value = []
  historyError.value = ''
}

async function loadDoctorHistory() {
  if (!doctorDetail.value?.doctorId) return
  historyLoading.value = true
  historyError.value = ''
  try {
    const data = await getAdminDoctorHistory(doctorDetail.value.doctorId, {
      page: historyPager.page,
      size: historyPager.size,
    })
    doctorHistory.value = normalizeResponseList(data)
    historyPager.total = Array.isArray(data) ? data.length : data?.total || doctorHistory.value.length
  } catch (e) {
    historyError.value = e.message || '医生操作历史加载失败'
    doctorHistory.value = []
    historyPager.total = 0
  } finally {
    historyLoading.value = false
  }
}

function historyActionText(item) {
  const map = {
    PASS: '审核通过',
    APPROVE: '审核通过',
    REJECT: '审核拒绝',
    DISABLE: '禁用账号',
    RESTORE: '恢复账号',
  }
  return map[item?.action] || (item?.operationType === 'DOCTOR_REVIEW' ? '医生审核' : '状态变更')
}

function historyPrevPage() {
  if (historyPager.page <= 1 || historyLoading.value) return
  historyPager.page -= 1
  loadDoctorHistory()
}

function historyNextPage() {
  if (historyPager.page >= historyPages.value || historyLoading.value) return
  historyPager.page += 1
  loadDoctorHistory()
}

onMounted(loadCurrentTab)
</script>

<template>
  <div class="doctors-view">
    <section class="page-hero" aria-labelledby="doctors-title">
      <div>
        <p class="eyebrow">Doctor Administration</p>
        <h1 id="doctors-title">医生管理</h1>
        <p>处理医生资质审核，并查看医生账号状态；审核动作会写入管理员操作链路。</p>
      </div>
      <button class="refresh-btn" :disabled="loading" type="button" @click="loadCurrentTab">
        {{ loading ? '刷新中' : '刷新列表' }}
      </button>
    </section>

    <section class="summary-grid" aria-label="医生概览">
      <article class="summary-card">
        <span>待审核</span>
        <strong>{{ summary.pending }}</strong>
      </article>
      <article class="summary-card">
        <span>医生账号</span>
        <strong>{{ summary.doctors }}</strong>
      </article>
      <article class="summary-card">
        <span>正常账号</span>
        <strong>{{ summary.enabled }}</strong>
      </article>
      <article class="summary-card">
        <span>禁用账号</span>
        <strong>{{ summary.disabled }}</strong>
      </article>
    </section>

    <section class="tabs-panel" aria-label="医生管理视图">
      <div class="tabs" role="tablist" aria-label="医生管理标签">
        <button :class="{ active: activeTab === 'audit' }" type="button" @click="switchTab('audit')">待审核</button>
        <button :class="{ active: activeTab === 'manage' }" type="button" @click="switchTab('manage')">全部医生</button>
      </div>

      <div v-if="activeTab === 'manage'" class="filters">
        <label>
          <span>关键词</span>
          <input v-model="filters.keyword" type="search" placeholder="医生姓名 / 手机号" @keyup.enter="search" />
        </label>
        <label>
          <span>状态</span>
          <select v-model="filters.status">
            <option value="">全部状态</option>
            <option value="NORMAL">正常</option>
            <option value="DISABLED">禁用</option>
          </select>
        </label>
        <button class="primary-btn" type="button" @click="search">查询</button>
      </div>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>
    <p v-if="successMsg" class="notice success" role="status">{{ successMsg }}</p>

    <section class="table-panel" aria-label="医生列表">
      <div v-if="loading" class="empty-state">
        正在请求{{ activeTab === 'audit' ? '待审核医生' : '医生账号' }}接口...
      </div>

      <template v-else-if="activeTab === 'audit'">
        <div v-if="auditDoctors.length === 0" class="empty-state">暂无待审核医生</div>
        <table v-else>
          <thead>
            <tr>
              <th>医生ID</th>
              <th>姓名</th>
              <th>科室</th>
              <th>职称</th>
              <th>简介</th>
              <th>审核状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="doctor in auditDoctors" :key="doctorIdOf(doctor)">
              <td>{{ doctorIdOf(doctor) || '--' }}</td>
              <td>{{ doctor.name || '--' }}</td>
              <td>{{ doctor.department || doctor.departmentName || '--' }}</td>
              <td>{{ doctor.title || '--' }}</td>
              <td class="intro-cell">{{ doctor.introduction || doctor.description || '--' }}</td>
              <td>
                <span class="status-tag" :class="`status-${statusTone(doctor.auditStatus)}`">
                  {{ statusText(doctor.auditStatus || 'PENDING') }}
                </span>
              </td>
              <td>
                <div class="row-actions">
                  <button class="primary-btn" :disabled="!!actionId" type="button" @click="openAuditConfirm(doctor, 'PASS')">
                    通过
                  </button>
                  <button class="danger-btn" :disabled="!!actionId" type="button" @click="openAuditConfirm(doctor, 'REJECT')">
                    拒绝
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </template>

      <template v-else>
        <div v-if="managedDoctors.length === 0" class="empty-state">暂无符合条件的医生账号</div>
        <table v-else>
          <thead>
            <tr>
              <th>账号ID</th>
              <th>姓名</th>
              <th>手机号</th>
              <th>角色</th>
              <th>账号状态</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="doctor in managedDoctors" :key="doctor.id || doctor.userId">
              <td>{{ doctor.id || doctor.userId || '--' }}</td>
              <td>{{ doctor.name || '--' }}</td>
              <td>{{ doctor.phone || '--' }}</td>
              <td>医生</td>
              <td>
                <span class="status-tag" :class="`status-${statusTone(doctor.status)}`">
                  {{ statusText(doctor.status) }}
                </span>
              </td>
              <td>{{ doctor.registerTime || doctor.createTime || doctor.createdAt || '--' }}</td>
              <td>
                <div class="row-actions">
                  <button
                    class="secondary-btn"
                    :disabled="!!detailLoadingId"
                    type="button"
                    @click="openDoctorDetail(doctor)"
                  >
                    {{ detailLoadingId === String(doctorIdOf(doctor)) ? '加载中' : '查看详情' }}
                  </button>
                  <button
                    :class="isDisabled(doctor) ? 'secondary-btn' : 'danger-btn'"
                    :disabled="!!actionId"
                    type="button"
                    @click="openStatusConfirm(doctor)"
                  >
                    {{ isDisabled(doctor) ? '恢复账号' : '禁用账号' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </template>
    </section>

    <div class="pager" aria-label="分页">
      <button :disabled="pager.page <= 1 || loading" type="button" @click="prevPage">上一页</button>
      <span>第 {{ pager.page }} / {{ pages }} 页 · 共 {{ pager.total }} 条</span>
      <button :disabled="pager.page >= pages || loading" type="button" @click="nextPage">下一页</button>
    </div>

    <div v-if="pendingAudit" class="modal-mask" role="presentation">
      <section class="confirm-panel" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
        <h2 id="confirm-title">确认{{ pendingAudit.result === 'PASS' ? '通过' : '拒绝' }}医生审核</h2>
        <p>
          即将{{ pendingAudit.result === 'PASS' ? '通过' : '拒绝' }}
          <strong>{{ pendingAudit.item.name || '该医生' }}</strong>
          的资质审核，提交后将由后端记录管理员操作日志。
        </p>
        <label v-if="pendingAudit.result === 'REJECT'" class="audit-reason-field" for="audit-reason">
          <span>拒绝理由 <b aria-hidden="true">*</b></span>
          <textarea
            id="audit-reason"
            v-model="auditReason"
            :aria-describedby="auditReasonError ? 'audit-reason-error' : 'audit-reason-help'"
            :aria-invalid="Boolean(auditReasonError)"
            maxlength="100"
            placeholder="请说明资料不符合要求的具体原因"
            rows="3"
            @blur="auditReasonError = auditReason.trim() ? '' : '请填写拒绝理由，便于医生修改资料'"
            @input="auditReasonError = ''"
          ></textarea>
          <small v-if="auditReasonError" id="audit-reason-error" class="field-error" role="alert">
            {{ auditReasonError }}
          </small>
          <small v-else id="audit-reason-help" class="field-help">必填，最多 100 个字符</small>
        </label>
        <div class="confirm-actions">
          <button class="secondary-btn" :disabled="!!actionId" type="button" @click="closeAuditConfirm">取消</button>
          <button
            :class="pendingAudit.result === 'PASS' ? 'primary-btn' : 'danger-btn'"
            :disabled="!!actionId"
            type="button"
            @click="confirmAudit"
          >
            {{ actionId ? '处理中...' : '确认提交' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="pendingStatus" class="modal-mask" role="presentation">
      <section class="confirm-panel" role="dialog" aria-modal="true" aria-labelledby="status-confirm-title">
        <h2 id="status-confirm-title">
          确认{{ pendingStatus.nextStatus === 'DISABLED' ? '禁用' : '恢复' }}医生账号
        </h2>
        <p>
          {{ pendingStatus.nextStatus === 'DISABLED'
            ? '禁用后，该医生将不能登录或继续作为可用医生提供服务。'
            : '恢复后，该医生将重新获得登录和接诊权限。' }}
        </p>
        <label
          v-if="pendingStatus.nextStatus === 'DISABLED'"
          class="audit-reason-field"
          for="status-reason"
        >
          <span>禁用原因 <b aria-hidden="true">*</b></span>
          <textarea
            id="status-reason"
            v-model="statusReason"
            :aria-describedby="statusReasonError ? 'status-reason-error' : 'status-reason-help'"
            :aria-invalid="Boolean(statusReasonError)"
            maxlength="100"
            placeholder="请说明禁用该医生账号的具体原因"
            rows="3"
            @blur="statusReasonError = statusReason.trim() ? '' : '请填写禁用原因，便于后续追溯'"
            @input="statusReasonError = ''"
          ></textarea>
          <small v-if="statusReasonError" id="status-reason-error" class="field-error" role="alert">
            {{ statusReasonError }}
          </small>
          <small v-else id="status-reason-help" class="field-help">必填，最多 100 个字符</small>
        </label>
        <div class="confirm-actions">
          <button class="secondary-btn" :disabled="!!actionId" type="button" @click="closeStatusConfirm">
            取消
          </button>
          <button
            :class="pendingStatus.nextStatus === 'DISABLED' ? 'danger-btn' : 'primary-btn'"
            :disabled="!!actionId"
            type="button"
            @click="confirmStatusChange"
          >
            {{ actionId ? '处理中...' : '确认提交' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="doctorDetail" class="modal-mask" role="presentation" @click.self="closeDoctorDetail">
      <section class="confirm-panel detail-panel" role="dialog" aria-modal="true" aria-labelledby="doctor-detail-title">
        <div class="detail-heading">
          <div>
            <p class="eyebrow">Doctor Detail</p>
            <h2 id="doctor-detail-title">{{ doctorDetail.name || '医生详情' }}</h2>
          </div>
          <button class="secondary-btn" type="button" aria-label="关闭医生详情" @click="closeDoctorDetail">
            关闭
          </button>
        </div>

        <dl class="detail-grid">
          <div><dt>医生ID</dt><dd>{{ doctorDetail.doctorId }}</dd></div>
          <div><dt>用户ID</dt><dd>{{ doctorDetail.userId }}</dd></div>
          <div><dt>登录账号</dt><dd>{{ doctorDetail.username || '--' }}</dd></div>
          <div><dt>所属科室</dt><dd>{{ doctorDetail.department || '--' }}</dd></div>
          <div><dt>职称</dt><dd>{{ doctorDetail.title || '--' }}</dd></div>
          <div>
            <dt>账号状态</dt>
            <dd>
              <span class="status-tag" :class="`status-${statusTone(doctorDetail.accountStatus)}`">
                {{ statusText(doctorDetail.accountStatus) }}
              </span>
            </dd>
          </div>
          <div>
            <dt>医生状态</dt>
            <dd>{{ doctorDetail.doctorStatus || '--' }}</dd>
          </div>
          <div><dt>注册时间</dt><dd>{{ doctorDetail.createTime || '--' }}</dd></div>
          <div
            v-if="isDisabled({ status: doctorDetail.accountStatus }) || doctorDetail.doctorStatus === '停用'"
            class="detail-wide disabled-detail"
          >
            <dt>当前禁用信息</dt>
            <dd>
              <strong>原因：{{ doctorDetail.disableReason || '未记录具体原因' }}</strong>
              <span>禁用时间：{{ doctorDetail.disableTime || '--' }}</span>
              <span>
                操作管理员：{{ doctorDetail.disabledByName || '--' }}
                <template v-if="doctorDetail.disabledBy">（用户ID：{{ doctorDetail.disabledBy }}）</template>
              </span>
            </dd>
          </div>
          <div class="detail-wide">
            <dt>医生简介与擅长方向</dt>
            <dd>{{ doctorDetail.introduction || '暂未填写' }}</dd>
          </div>
        </dl>

        <section class="history-section" aria-labelledby="doctor-history-title">
          <div class="history-heading">
            <div>
              <h3 id="doctor-history-title">操作历史</h3>
              <p>共 {{ historyPager.total }} 条审核与状态变更记录</p>
            </div>
            <button class="secondary-btn" :disabled="historyLoading" type="button" @click="loadDoctorHistory">
              {{ historyLoading ? '加载中' : '刷新' }}
            </button>
          </div>

          <p v-if="historyError" class="notice danger" role="alert">{{ historyError }}</p>
          <div v-else-if="historyLoading" class="history-empty">正在加载操作历史...</div>
          <div v-else-if="doctorHistory.length === 0" class="history-empty">暂无操作历史</div>
          <ol v-else class="history-list">
            <li v-for="item in doctorHistory" :key="item.id">
              <div class="history-item-heading">
                <strong>{{ historyActionText(item) }}</strong>
                <time>{{ item.createTime || '--' }}</time>
              </div>
              <p v-if="item.reason">原因：{{ item.reason }}</p>
              <p>操作管理员：{{ item.operatorName || '--' }}（ID：{{ item.operatorId || '--' }}）</p>
              <span>结果：{{ item.result || '--' }}</span>
            </li>
          </ol>

          <div v-if="historyPager.total > historyPager.size" class="history-pager">
            <button :disabled="historyPager.page <= 1 || historyLoading" type="button" @click="historyPrevPage">上一页</button>
            <span>第 {{ historyPager.page }} / {{ historyPages }} 页</span>
            <button :disabled="historyPager.page >= historyPages || historyLoading" type="button" @click="historyNextPage">下一页</button>
          </div>
        </section>
      </section>
    </div>
  </div>
</template>

<style scoped>
.doctors-view {
  min-height: 100%;
}

.page-hero,
.summary-card,
.tabs-panel,
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
.danger-btn,
.tabs button {
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

.secondary-btn,
.tabs button {
  border: 1.5px solid var(--color-border-strong);
  background: #fff;
  color: var(--color-primary-deep);
}

.danger-btn {
  border: 1.5px solid var(--color-danger);
  background: var(--color-danger);
  color: #fff;
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

.tabs-panel {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 14px;
  margin-top: 14px;
  padding: 14px;
}

.tabs {
  display: flex;
  gap: 8px;
}

.tabs button.active {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
}

.filters {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 150px auto;
  gap: 10px;
  align-items: end;
}

.filters label span {
  display: block;
  margin-bottom: 6px;
  color: var(--color-ink-2);
  font-size: 13px;
  font-weight: 800;
}

.filters input,
.filters select {
  width: 100%;
  min-height: 40px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0 12px;
  color: var(--color-ink);
  outline: none;
}

.filters input:focus,
.filters select:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(14, 143, 134, 0.12);
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
  min-width: 900px;
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

.intro-cell {
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-tag {
  display: inline-flex;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 800;
}

.status-success {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.status-warning {
  background: var(--color-warn-soft);
  color: var(--color-warn);
}

.status-danger {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.row-actions {
  display: flex;
  gap: 8px;
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
  width: min(430px, 100%);
  padding: 18px;
}

.detail-panel {
  width: min(720px, 100%);
  max-height: calc(100vh - 36px);
  overflow: auto;
}

.detail-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.detail-grid > div {
  min-width: 0;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 12px;
  background: var(--color-primary-soft);
}

.detail-grid dt {
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 700;
}

.detail-grid dd {
  margin: 6px 0 0;
  color: var(--color-ink);
  font-size: 13px;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.detail-grid .detail-wide {
  grid-column: 1 / -1;
}

.detail-wide dd {
  white-space: pre-wrap;
  line-height: 1.7;
}

.disabled-detail {
  border-color: var(--color-danger) !important;
  background: var(--color-danger-soft) !important;
}

.disabled-detail dd,
.disabled-detail span {
  display: block;
}

.history-section {
  margin-top: 18px;
  border-top: 1px solid var(--color-border);
  padding-top: 18px;
}

.history-heading,
.history-item-heading,
.history-pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.history-heading h3 {
  color: var(--color-ink);
  font-size: 16px;
}

.history-heading p,
.history-list p,
.history-list span,
.history-item-heading time {
  color: var(--color-ink-3);
  font-size: 12px;
}

.history-list {
  display: grid;
  gap: 10px;
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
}

.history-list li {
  border-left: 3px solid var(--color-primary);
  border-radius: var(--radius-sm);
  padding: 12px;
  background: var(--color-primary-soft);
}

.history-item-heading strong {
  color: var(--color-ink);
  font-size: 13px;
}

.history-list p,
.history-list span {
  display: block;
  margin-top: 6px;
}

.history-empty {
  margin-top: 14px;
  border: 1px dashed var(--color-border-strong);
  border-radius: var(--radius-sm);
  padding: 20px;
  color: var(--color-ink-3);
  text-align: center;
  font-size: 13px;
}

.history-pager {
  justify-content: center;
  margin-top: 14px;
}

.history-pager button {
  min-height: 36px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0 12px;
  background: var(--color-surface);
  color: var(--color-ink-2);
  font-weight: 700;
}

.history-pager span {
  color: var(--color-ink-3);
  font-size: 12px;
}

.disabled-detail span {
  margin-top: 4px;
  color: var(--color-ink-2);
  font-weight: 600;
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

.audit-reason-field {
  display: block;
  margin-top: 16px;
}

.audit-reason-field > span {
  display: block;
  margin-bottom: 7px;
  color: var(--color-ink-2);
  font-size: 13px;
  font-weight: 800;
}

.audit-reason-field b,
.field-error {
  color: var(--color-danger);
}

.audit-reason-field textarea {
  width: 100%;
  min-height: 88px;
  resize: vertical;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  color: var(--color-ink);
  font: inherit;
  line-height: 1.5;
  outline: none;
}

.audit-reason-field textarea:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(14, 143, 134, 0.12);
}

.audit-reason-field textarea[aria-invalid='true'] {
  border-color: var(--color-danger);
}

.field-error,
.field-help {
  display: block;
  margin-top: 6px;
  font-size: 12px;
}

.field-help {
  color: var(--color-ink-3);
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

  .page-hero,
  .tabs-panel {
    align-items: stretch;
    flex-direction: column;
  }

  .filters {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .page-hero,
  .tabs-panel {
    padding: 14px;
  }

  .tabs,
  .row-actions,
  .confirm-actions,
  .pager {
    flex-direction: column;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-grid .detail-wide {
    grid-column: auto;
  }
}
</style>
