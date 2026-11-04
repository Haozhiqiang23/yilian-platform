<script setup>
import { computed, onMounted, ref } from 'vue'
import { getDoctorProfile } from '@/api/doctor'
import { useUserStore } from '@/stores/user'

const store = useUserStore()
const profile = ref(null)
const loading = ref(false)
const errorMsg = ref('')

const doctor = computed(() => ({ ...(store.user || {}), ...(profile.value || {}) }))

const displayName = computed(() => doctor.value.name || '医生')
const accountStatus = computed(() => doctor.value.status || doctor.value.auditStatus || '状态未知')
const avatarText = computed(() => displayName.value.slice(0, 1))

const profileRows = computed(() => [
  { label: '姓名', value: doctor.value.name || '暂未获取' },
  { label: '用户ID', value: doctor.value.userId || '--' },
  { label: '当前角色', value: doctor.value.role || '--' },
  { label: '科室', value: doctor.value.departmentName || doctor.value.department || '暂未获取' },
  { label: '职称', value: doctor.value.title || '暂未获取' },
  { label: '账号状态', value: accountStatus.value },
])

const statusTone = computed(() => {
  if (['正常', '已通过', '启用', 'APPROVED', 'ACTIVE'].includes(accountStatus.value)) return 'success'
  if (['拒绝', '禁用', 'REJECTED', 'DISABLED'].includes(accountStatus.value)) return 'danger'
  return 'warning'
})

async function loadProfile() {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await getDoctorProfile()
    profile.value = data || {}

    // 登录角色在前端统一使用小写，不能被后端返回的 DOCTOR 覆盖。
    store.setUserInfo({
      ...data,
      role: store.user?.role || String(data?.role || '').toLowerCase(),
    })
  } catch (error) {
    errorMsg.value = error.message || '医生个人资料加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="profile-view">
    <section class="profile-hero" aria-labelledby="profile-title">
      <div class="doctor-identity">
        <span class="avatar" aria-hidden="true">{{ avatarText }}</span>
        <div>
          <p class="eyebrow">Doctor Profile</p>
          <h1 id="profile-title">{{ displayName }}</h1>
          <p>以下信息来自当前登录医生账号，展示内容以系统资料为准。</p>
        </div>
      </div>
      <span class="status-badge" :class="`status-${statusTone}`">{{ accountStatus }}</span>
    </section>

    <p v-if="loading" class="profile-notice">正在加载医生资料...</p>
    <p v-else-if="errorMsg" class="profile-notice profile-error">{{ errorMsg }}</p>

    <section class="content-grid">
      <article class="panel">
        <div class="panel-head">
          <div>
            <h2>基础资料</h2>
            <p>账号、科室和执业资料由系统统一维护，如有变更请联系管理员。</p>
          </div>
          <span class="readonly-badge">只读</span>
        </div>

        <dl class="info-grid">
          <div v-for="row in profileRows" :key="row.label">
            <dt>{{ row.label }}</dt>
            <dd>{{ row.value }}</dd>
          </div>
        </dl>
      </article>

      <article class="panel">
        <div class="panel-head">
          <div>
            <h2>简介信息</h2>
            <p>用于医生公开信息展示和患者预约参考。</p>
          </div>
          <span class="readonly-badge">只读</span>
        </div>

        <div class="bio-box">
          {{ doctor.introduction || doctor.description || '暂未填写医生简介。' }}
        </div>
      </article>
    </section>

    <section class="audit-panel" aria-labelledby="audit-title">
      <div>
        <h2 id="audit-title">账号审核提示</h2>
        <p>医生账号由管理员审核。若状态为待审核或被拒绝，前端仅展示提示，审核动作由管理员端处理。</p>
      </div>
      <div class="audit-steps">
        <span class="step active">提交资料</span>
        <span class="step" :class="{ active: statusTone !== 'warning' }">管理员审核</span>
        <span class="step" :class="{ active: statusTone === 'success' }">可正常接诊</span>
      </div>
    </section>

    <section class="waiting-panel" aria-labelledby="waiting-title">
      <h2 id="waiting-title">资料维护说明</h2>
      <div class="waiting-grid">
        <article>
          <span>医生本人资料查询</span>
          <strong>资料已同步</strong>
          <p>当前页面已展示姓名、科室、职称、简介和账号状态。</p>
        </article>
        <article>
          <span>医生资料维护</span>
          <strong>当前页面不主动编辑</strong>
          <p>当前资料以查看为主，如需修改请联系管理员处理。</p>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.profile-view {
  min-height: 100%;
  padding: 20px 18px 28px;
}

.profile-hero,
.panel,
.audit-panel,
.waiting-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}

.profile-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 22px;
}

.doctor-identity {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.avatar {
  display: grid;
  flex: 0 0 58px;
  width: 58px;
  height: 58px;
  place-items: center;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-size: 24px;
  font-weight: 900;
}

.eyebrow {
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 800;
}

.profile-hero h1 {
  margin-top: 4px;
  color: var(--color-ink);
  font-size: 24px;
  font-weight: 800;
}

.profile-hero p {
  margin-top: 6px;
  color: var(--color-ink-3);
  font-size: 13px;
}

.profile-notice {
  margin-top: 12px;
  border-radius: var(--radius-sm);
  background: var(--color-primary-soft);
  padding: 10px 12px;
  color: var(--color-primary-deep);
  font-size: 13px;
  font-weight: 700;
}

.profile-error {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.status-badge,
.readonly-badge,
.wait-badge {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
}

.status-success,
.readonly-badge {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.status-warning,
.wait-badge {
  background: var(--color-warn-soft);
  color: var(--color-warn);
}

.status-danger {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(300px, 0.75fr);
  gap: 14px;
  margin-top: 14px;
  align-items: start;
}

.panel,
.audit-panel,
.waiting-panel {
  padding: 16px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-head h2,
.audit-panel h2,
.waiting-panel h2 {
  color: var(--color-ink);
  font-size: 17px;
  font-weight: 800;
}

.panel-head p,
.audit-panel p {
  margin-top: 3px;
  color: var(--color-ink-3);
  font-size: 12px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.info-grid div,
.bio-box,
.waiting-grid article {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-primary-soft);
  padding: 12px;
}

.info-grid dt,
.waiting-grid span {
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 700;
}

.info-grid dd {
  margin-top: 4px;
  color: var(--color-ink);
  font-size: 14px;
  font-weight: 800;
  overflow-wrap: anywhere;
}

.bio-box {
  min-height: 144px;
  color: var(--color-ink-2);
  font-size: 13px;
  line-height: 1.7;
}

.audit-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  margin-top: 14px;
  align-items: center;
}

.audit-steps {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.step {
  border-radius: 999px;
  padding: 7px 11px;
  background: var(--color-bg);
  color: var(--color-ink-3);
  font-size: 12px;
  font-weight: 800;
}

.step.active {
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
}

.waiting-panel {
  margin-top: 14px;
}

.waiting-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.waiting-grid span,
.waiting-grid strong {
  display: block;
}

.waiting-grid strong {
  margin-top: 5px;
  color: var(--color-ink);
  font-size: 14px;
  overflow-wrap: anywhere;
}

.waiting-grid p {
  margin-top: 8px;
  color: var(--color-ink-3);
  font-size: 12px;
}

@media (max-width: 900px) {
  .content-grid,
  .audit-panel,
  .waiting-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .profile-view {
    padding: 16px 12px 24px;
  }

  .profile-hero,
  .panel,
  .audit-panel,
  .waiting-panel {
    padding: 14px;
  }

  .profile-hero,
  .panel-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
