<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getSystemConfig, updateSystemConfig } from '@/api/admin'

const loading = ref(false)
const saving = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const updateTime = ref('')

const form = reactive({
  signInAheadMinutes: 30,
  queuePollSeconds: 15,
  appointmentCancelHours: 2,
  refundEnabled: true,
})

const errors = reactive({
  signInAheadMinutes: '',
  queuePollSeconds: '',
  appointmentCancelHours: '',
})

const configCards = computed(() => [
  { label: '签到提前时间', value: `${form.signInAheadMinutes} 分钟`, tone: 'primary' },
  { label: '队列轮询间隔', value: `${form.queuePollSeconds} 秒`, tone: 'info' },
  { label: '预约取消限制', value: `${form.appointmentCancelHours} 小时`, tone: 'warn' },
  { label: '退款申请入口', value: form.refundEnabled ? '启用' : '停用', tone: form.refundEnabled ? 'success' : 'danger' },
])

function validateNumber(key, min, max, message) {
  const value = Number(form[key])
  if (!Number.isInteger(value) || value < min || value > max) {
    errors[key] = message
  } else {
    errors[key] = ''
  }
}

function validateForm() {
  validateNumber('signInAheadMinutes', 1, 180, '请输入 1 到 180 分钟的整数')
  validateNumber('queuePollSeconds', 5, 60, '请输入 5 到 60 秒的整数')
  validateNumber('appointmentCancelHours', 0, 72, '请输入 0 到 72 小时的整数')
  return !Object.values(errors).some(Boolean)
}

function applyConfig(data) {
  form.signInAheadMinutes = data.signInAheadMinutes
  form.queuePollSeconds = data.queuePollSeconds
  form.appointmentCancelHours = data.appointmentCancelHours
  form.refundEnabled = data.refundEnabled
  updateTime.value = data.updateTime || ''
}

async function loadConfig() {
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    applyConfig(await getSystemConfig())
  } catch (e) {
    errorMsg.value = e.message || '系统配置加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function saveConfig() {
  successMsg.value = ''
  if (!validateForm()) {
    errorMsg.value = '请先修正配置项后再提交'
    return
  }
  saving.value = true
  errorMsg.value = ''
  try {
    const data = await updateSystemConfig({ ...form })
    applyConfig(data)
    successMsg.value = '系统配置保存成功，新的参数已生效'
  } catch (e) {
    errorMsg.value = e.message || '系统配置保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

function resetDefaults() {
  form.signInAheadMinutes = 30
  form.queuePollSeconds = 15
  form.appointmentCancelHours = 2
  form.refundEnabled = true
  Object.keys(errors).forEach((key) => {
    errors[key] = ''
  })
  successMsg.value = '已恢复推荐默认值，点击“保存配置”后生效'
  errorMsg.value = ''
}

onMounted(loadConfig)
</script>

<template>
  <div class="config-view">
    <section class="page-hero" aria-labelledby="config-title">
      <div>
        <p class="eyebrow">System Settings</p>
        <h1 id="config-title">系统配置</h1>
        <p>维护签到、预约取消、排队和退款相关业务参数，保存后由后端统一持久化并记录操作日志。</p>
      </div>
      <button class="refresh-btn" :disabled="loading || saving" type="button" @click="saveConfig">
        {{ saving ? '保存中...' : loading ? '加载中...' : '保存配置' }}
      </button>
    </section>

    <section class="summary-grid" aria-label="配置概览">
      <article v-for="item in configCards" :key="item.label" class="summary-card" :class="`tone-${item.tone}`">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>
    <p v-if="successMsg" class="notice success" role="status">{{ successMsg }}</p>

    <section class="settings-panel" aria-label="业务参数配置">
      <div class="panel-head">
        <div>
          <h2>业务参数</h2>
          <p>页面打开时读取数据库中的正式配置，输入不合法时不会提交。</p>
        </div>
        <button class="secondary-btn" :disabled="loading || saving" type="button" @click="resetDefaults">恢复默认</button>
      </div>

      <div class="settings-grid">
        <label>
          <span>签到提前时间（分钟）</span>
          <input v-model.number="form.signInAheadMinutes" type="number" min="1" max="180" :disabled="loading || saving" :aria-invalid="!!errors.signInAheadMinutes" aria-describedby="sign-in-error" @blur="validateNumber('signInAheadMinutes', 1, 180, '请输入 1 到 180 分钟的整数')" />
          <small v-if="errors.signInAheadMinutes" id="sign-in-error">{{ errors.signInAheadMinutes }}</small>
        </label>
        <label>
          <span>队列轮询间隔（秒）</span>
          <input v-model.number="form.queuePollSeconds" type="number" min="5" max="60" :disabled="loading || saving" :aria-invalid="!!errors.queuePollSeconds" aria-describedby="queue-error" @blur="validateNumber('queuePollSeconds', 5, 60, '请输入 5 到 60 秒的整数')" />
          <small v-if="errors.queuePollSeconds" id="queue-error">{{ errors.queuePollSeconds }}</small>
        </label>
        <label>
          <span>预约取消限制（小时）</span>
          <input v-model.number="form.appointmentCancelHours" type="number" min="0" max="72" :disabled="loading || saving" :aria-invalid="!!errors.appointmentCancelHours" aria-describedby="cancel-error" @blur="validateNumber('appointmentCancelHours', 0, 72, '请输入 0 到 72 小时的整数')" />
          <small v-if="errors.appointmentCancelHours" id="cancel-error">{{ errors.appointmentCancelHours }}</small>
        </label>
      </div>
    </section>

    <section class="settings-panel" aria-label="功能开关">
      <div class="panel-head">
        <div>
          <h2>功能开关</h2>
          <p>开关状态由数据库统一保存，管理员的每次保存都会进入操作日志。</p>
        </div>
      </div>

      <div class="toggle-list">
        <label class="toggle-row">
          <span>
            <strong>退款申请入口</strong>
            <small>控制患者端是否展示退款申请入口</small>
          </span>
          <input v-model="form.refundEnabled" :disabled="loading || saving" type="checkbox" />
        </label>
      </div>
    </section>

    <section class="sync-panel" aria-label="配置同步状态">
      <div>
        <span>配置来源</span>
        <strong>后端数据库</strong>
      </div>
      <div>
        <span>最后更新时间</span>
        <strong>{{ updateTime || (loading ? '正在读取...' : '暂无记录') }}</strong>
      </div>
    </section>
  </div>
</template>

<style scoped>
.config-view {
  min-height: 100%;
}

.page-hero,
.summary-card,
.settings-panel,
.sync-panel {
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
  max-width: 720px;
  margin-top: 8px;
  color: var(--color-ink-3);
  font-size: 13px;
}

.refresh-btn,
.secondary-btn {
  min-height: 40px;
  border-radius: var(--radius-sm);
  padding: 0 13px;
  font-size: 13px;
  font-weight: 800;
}

.refresh-btn {
  border: 1.5px solid var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}

.secondary-btn {
  border: 1.5px solid var(--color-border-strong);
  background: #fff;
  color: var(--color-primary-deep);
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
  font-size: 22px;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.tone-warn strong {
  color: var(--color-warn);
}

.tone-info strong {
  color: var(--color-info);
}

.tone-success strong {
  color: var(--color-accent);
}

.tone-danger strong {
  color: var(--color-danger);
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

.settings-panel,
.sync-panel {
  margin-top: 14px;
  padding: 16px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.panel-head h2 {
  color: var(--color-ink);
  font-size: 18px;
  font-weight: 800;
}

.panel-head p {
  margin-top: 4px;
  color: var(--color-ink-3);
  font-size: 13px;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.settings-grid label span {
  display: block;
  margin-bottom: 6px;
  color: var(--color-ink-2);
  font-size: 13px;
  font-weight: 800;
}

.settings-grid input {
  width: 100%;
  min-height: 40px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0 12px;
  color: var(--color-ink);
  outline: none;
}

.settings-grid input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(14, 143, 134, 0.12);
}

.settings-grid small {
  display: block;
  margin-top: 7px;
  color: var(--color-danger);
  font-size: 12.5px;
  font-weight: 700;
}

.toggle-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 13px;
}

.toggle-row strong {
  display: block;
  color: var(--color-ink);
  font-size: 14px;
}

.toggle-row small {
  display: block;
  margin-top: 3px;
  color: var(--color-ink-3);
  font-size: 12.5px;
}

.toggle-row input {
  width: 42px;
  height: 24px;
  accent-color: var(--color-primary);
  flex-shrink: 0;
}

.sync-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.sync-panel > div {
  border: 1px dashed var(--color-border-strong);
  border-radius: var(--radius-sm);
  padding: 13px;
  background: var(--color-primary-soft);
}

.sync-panel span {
  display: block;
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 800;
}

.sync-panel strong {
  display: block;
  margin-top: 5px;
  color: var(--color-primary-deep);
  font-size: 14px;
}

@media (max-width: 980px) {
  .summary-grid,
  .settings-grid,
  .sync-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .page-hero,
  .panel-head {
    align-items: stretch;
    flex-direction: column;
  }
}

@media (max-width: 620px) {
  .summary-grid,
  .settings-grid,
  .sync-panel {
    grid-template-columns: 1fr;
  }

  .page-hero,
  .settings-panel,
  .sync-panel {
    padding: 14px;
  }
}
</style>
