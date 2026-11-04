<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { getAdminUsers, updateUserStatus } from '@/api/admin'

const store = useUserStore()
const loading = ref(false)
const actionId = ref('')
const errorMsg = ref('')
const successMsg = ref('')
const users = ref([])
const pendingUser = ref(null)
const statusReason = ref('')
const statusReasonError = ref('')

const filters = reactive({
  keyword: '',
  role: '',
  status: '',
})

const pager = reactive({
  page: 1,
  size: 10,
  total: 0,
})

const pages = computed(() => Math.max(1, Math.ceil(pager.total / pager.size)))

const roleOptions = [
  { label: '全部角色', value: '' },
  { label: '患者', value: 'PATIENT' },
  { label: '医生', value: 'DOCTOR' },
  { label: '管理员', value: 'ADMIN' },
]

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '正常', value: 'NORMAL' },
  { label: '禁用', value: 'DISABLED' },
]

const summary = computed(() => ({
  total: pager.total || users.value.length,
  normal: users.value.filter((user) => !isDisabled(user)).length,
  disabled: users.value.filter((user) => isDisabled(user)).length,
  admin: users.value.filter((user) => normalizeRole(user.role) === 'ADMIN').length,
}))

async function loadUsers() {
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const data = await getAdminUsers({
      keyword: filters.keyword || undefined,
      role: filters.role || undefined,
      status: filters.status || undefined,
      page: pager.page,
      size: pager.size,
    })
    users.value = normalizeResponseList(data)
    pager.total = Array.isArray(data) ? data.length : data?.total || users.value.length
  } catch (e) {
    errorMsg.value = e.message || '用户数据加载失败，请稍后重试'
    users.value = []
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

function search() {
  pager.page = 1
  loadUsers()
}

function resetFilters() {
  filters.keyword = ''
  filters.role = ''
  filters.status = ''
  search()
}

function prevPage() {
  if (pager.page <= 1) return
  pager.page -= 1
  loadUsers()
}

function nextPage() {
  if (pager.page >= pages.value) return
  pager.page += 1
  loadUsers()
}

function isDisabled(user) {
  return ['DISABLED', '禁用', '停用'].includes(user?.status)
}

function normalizeRole(role) {
  const map = { 患者: 'PATIENT', 医生: 'DOCTOR', 管理员: 'ADMIN' }
  return map[role] || role || ''
}

function roleText(role) {
  const map = { PATIENT: '患者', DOCTOR: '医生', ADMIN: '管理员' }
  return map[normalizeRole(role)] || role || '--'
}

function statusText(status) {
  const map = { NORMAL: '正常', ACTIVE: '正常', DISABLED: '禁用' }
  return map[status] || status || '--'
}

function isSelf(user) {
  return String(user?.id || user?.userId || '') === String(store.user?.userId || '')
}

function openConfirm(user) {
  errorMsg.value = ''
  successMsg.value = ''
  if (isSelf(user) && !isDisabled(user)) {
    errorMsg.value = '不能禁用当前登录管理员账号'
    return
  }
  statusReason.value = ''
  statusReasonError.value = ''
  pendingUser.value = user
}

function closeConfirm() {
  if (actionId.value) return
  pendingUser.value = null
  statusReason.value = ''
  statusReasonError.value = ''
}

async function confirmToggleStatus() {
  const user = pendingUser.value
  if (!user || actionId.value) return
  const nextStatus = isDisabled(user) ? 'NORMAL' : 'DISABLED'
  if (nextStatus === 'DISABLED' && !statusReason.value.trim()) {
    statusReasonError.value = '请填写禁用原因，便于后续追溯'
    return
  }
  actionId.value = String(user.id || user.userId)
  errorMsg.value = ''
  successMsg.value = ''
  try {
    await updateUserStatus(user.id || user.userId, {
      status: nextStatus,
      reason: statusReason.value.trim() || undefined,
    })
    successMsg.value = `${user.name || '该用户'}已${nextStatus === 'DISABLED' ? '禁用' : '启用'}`
    pendingUser.value = null
    await loadUsers()
  } catch (e) {
    errorMsg.value = e.message || '用户状态修改失败，请稍后重试'
  } finally {
    actionId.value = ''
  }
}

onMounted(loadUsers)
</script>

<template>
  <div class="users-view">
    <section class="page-hero" aria-labelledby="users-title">
      <div>
        <p class="eyebrow">User Administration</p>
        <h1 id="users-title">用户管理</h1>
        <p>查询用户账号、角色、状态与注册时间；禁用/启用操作提交前需要二次确认。</p>
      </div>
      <button class="refresh-btn" :disabled="loading" type="button" @click="loadUsers">
        {{ loading ? '刷新中' : '刷新列表' }}
      </button>
    </section>

    <section class="summary-grid" aria-label="用户概览">
      <article class="summary-card">
        <span>当前总数</span>
        <strong>{{ summary.total }}</strong>
      </article>
      <article class="summary-card">
        <span>正常账号</span>
        <strong>{{ summary.normal }}</strong>
      </article>
      <article class="summary-card">
        <span>禁用账号</span>
        <strong>{{ summary.disabled }}</strong>
      </article>
      <article class="summary-card">
        <span>管理员</span>
        <strong>{{ summary.admin }}</strong>
      </article>
    </section>

    <section class="filters" aria-label="用户筛选">
      <label>
        <span>关键词</span>
        <input v-model="filters.keyword" type="search" placeholder="姓名 / 手机号" @keyup.enter="search" />
      </label>
      <label>
        <span>角色</span>
        <select v-model="filters.role">
          <option v-for="option in roleOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
        </select>
      </label>
      <label>
        <span>状态</span>
        <select v-model="filters.status">
          <option v-for="option in statusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
        </select>
      </label>
      <div class="filter-actions">
        <button class="primary-btn" type="button" @click="search">查询</button>
        <button class="secondary-btn" type="button" @click="resetFilters">重置</button>
      </div>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>
    <p v-if="successMsg" class="notice success" role="status">{{ successMsg }}</p>

    <section class="table-panel" aria-label="用户列表">
      <div v-if="loading" class="empty-state">正在请求用户列表接口...</div>
      <div v-else-if="users.length === 0" class="empty-state">暂无符合条件的用户</div>

      <table v-else>
        <thead>
          <tr>
            <th>用户账号</th>
            <th>姓名</th>
            <th>手机号</th>
            <th>角色</th>
            <th>状态</th>
            <th>注册时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id || user.userId">
            <td>{{ user.username || user.account || user.id || user.userId || '--' }}</td>
            <td>{{ user.name || '--' }}</td>
            <td>{{ user.phone || '--' }}</td>
            <td>{{ roleText(user.role) }}</td>
            <td>
              <span class="status-tag" :class="{ disabled: isDisabled(user) }">
                {{ statusText(user.status) }}
              </span>
            </td>
            <td>{{ user.registerTime || user.createTime || user.createdAt || '--' }}</td>
            <td>
              <button class="link-btn" :disabled="!!actionId" type="button" @click="openConfirm(user)">
                {{ isDisabled(user) ? '启用' : '禁用' }}
              </button>
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

    <div v-if="pendingUser" class="modal-mask" role="presentation">
      <section class="confirm-panel" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
        <h2 id="confirm-title">确认{{ isDisabled(pendingUser) ? '启用' : '禁用' }}账号</h2>
        <p>
          将{{ isDisabled(pendingUser) ? '恢复' : '限制' }}
          <strong>{{ pendingUser.name || pendingUser.username || '该用户' }}</strong>
          的账号状态。禁用后用户将不能登录和创建预约。
        </p>
        <label v-if="!isDisabled(pendingUser)" class="reason-field">
          <span>禁用原因 <b aria-hidden="true">*</b></span>
          <textarea
            v-model="statusReason"
            maxlength="100"
            rows="4"
            placeholder="请说明禁用原因，例如：账号异常或资质信息需重新核验"
            @input="statusReasonError = ''"
            @blur="statusReasonError = statusReason.trim() ? '' : '请填写禁用原因，便于后续追溯'"
          />
          <small :class="{ error: statusReasonError }">
            {{ statusReasonError || `${statusReason.length}/100` }}
          </small>
        </label>
        <div class="confirm-actions">
          <button class="secondary-btn" :disabled="!!actionId" type="button" @click="closeConfirm">取消</button>
          <button class="danger-btn" :disabled="!!actionId" type="button" @click="confirmToggleStatus">
            {{ actionId ? '处理中...' : '确认提交' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.users-view {
  min-height: 100%;
}

.page-hero,
.summary-card,
.filters,
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
.link-btn {
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

.filters {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 160px 160px auto;
  gap: 12px;
  margin-top: 14px;
  padding: 14px;
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
  min-height: 42px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0 12px;
  background: #fff;
  color: var(--color-ink);
  outline: none;
}

.filters input:focus,
.filters select:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(14, 143, 134, 0.12);
}

.filter-actions {
  display: flex;
  gap: 8px;
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

.status-tag {
  display: inline-flex;
  border-radius: 999px;
  padding: 5px 10px;
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-size: 12px;
  font-weight: 800;
}

.status-tag.disabled {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.link-btn {
  border: 1.5px solid var(--color-border-strong);
  background: #fff;
  color: var(--color-primary-deep);
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
  width: min(420px, 100%);
  padding: 18px;
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

.reason-field {
  display: block;
  margin-top: 16px;
}

.reason-field span {
  display: block;
  margin-bottom: 7px;
  color: var(--color-ink-2);
  font-size: 13px;
  font-weight: 800;
}

.reason-field b,
.reason-field small.error {
  color: var(--color-danger);
}

.reason-field textarea {
  width: 100%;
  resize: vertical;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  color: var(--color-ink);
  font: inherit;
  outline: none;
}

.reason-field textarea:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(14, 143, 134, 0.12);
}

.reason-field small {
  display: block;
  margin-top: 5px;
  color: var(--color-ink-3);
  font-size: 12px;
}

.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

@media (max-width: 980px) {
  .summary-grid,
  .filters {
    grid-template-columns: 1fr 1fr;
  }

  .page-hero {
    align-items: stretch;
    flex-direction: column;
  }
}

@media (max-width: 620px) {
  .summary-grid,
  .filters {
    grid-template-columns: 1fr;
  }

  .page-hero,
  .filters {
    padding: 14px;
  }

  .filter-actions,
  .confirm-actions {
    flex-direction: column;
  }

  .pager {
    flex-direction: column;
  }
}
</style>
