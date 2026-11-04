<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getProfile, updateProfile } from '@/api/patient'

const store = useUserStore()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const loadedAt = ref('')

const profile = reactive({
  name: '',
  phone: '',
  idCard: '',
  address: '',
})

const form = reactive({
  phone: '',
  address: '',
})

const errors = reactive({
  phone: '',
  address: '',
})

const displayName = computed(() => profile.name || store.user?.name || '患者')
const avatarText = computed(() => displayName.value.slice(0, 1))
const hasChanges = computed(() => form.phone !== profile.phone || form.address !== profile.address)
const verificationText = computed(() => {
  if (loading.value) return '核验中'
  return profile.name && profile.idCard ? '已实名认证' : '实名信息待完善'
})

function nowText() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function syncForm() {
  form.phone = profile.phone
  form.address = profile.address
  errors.phone = ''
  errors.address = ''
}

function setProfile(data = {}) {
  profile.name = data.name || store.user?.name || ''
  profile.phone = data.phone || ''
  profile.idCard = data.idCard || data.idNo || ''
  profile.address = data.address || ''
  syncForm()
}

function isPhoneLike(value) {
  if (!value) return true
  if (value.includes('*')) return true
  return /^1[3-9]\d{9}$/.test(value)
}

function validate() {
  errors.phone = isPhoneLike(form.phone) ? '' : '请输入正确的 11 位手机号'
  errors.address = form.address.length <= 120 ? '' : '联系地址不能超过 120 个字符'
  return !errors.phone && !errors.address
}

async function loadProfile() {
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const data = await getProfile()
    setProfile(data)
    loadedAt.value = nowText()
  } catch (e) {
    errorMsg.value = e.message || '个人信息加载失败，请稍后重试'
    setProfile()
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (saving.value || !validate()) return
  saving.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const data = await updateProfile({
      phone: form.phone,
      address: form.address,
    })
    setProfile({
      ...profile,
      ...data,
      phone: data?.phone || form.phone,
      address: data?.address || form.address,
    })
    loadedAt.value = nowText()
    successMsg.value = '个人资料已保存'
  } catch (e) {
    errorMsg.value = e.message || '个人资料保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

function logout() {
  store.logout()
  router.replace('/login')
}

onMounted(loadProfile)
</script>

<template>
  <div class="profile-view">
    <header class="hero">
      <div class="avatar" aria-hidden="true">{{ avatarText }}</div>
      <div class="hero-main">
        <p class="eyebrow">个人中心</p>
        <h1>{{ displayName }}</h1>
        <p>维护手机号和联系地址，实名信息用于就医核验。</p>
      </div>
      <button class="refresh-btn" :disabled="loading || saving" aria-label="刷新个人信息" @click="loadProfile">
        <svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12a9 9 0 0 1-15.5 6.2" />
          <path d="M3 12A9 9 0 0 1 18.5 5.8" />
          <path d="M18 2v4h-4" />
          <path d="M6 22v-4h4" />
        </svg>
      </button>
    </header>

    <p v-if="errorMsg" class="notice error" role="alert">{{ errorMsg }}</p>
    <p v-if="successMsg" class="notice success" role="status">{{ successMsg }}</p>

    <section class="status-panel" aria-label="资料状态">
      <div>
        <span>认证状态</span>
        <strong>{{ verificationText }}</strong>
      </div>
      <div>
        <span>最近同步</span>
        <strong>{{ loadedAt || '--' }}</strong>
      </div>
      <div>
        <span>可编辑项</span>
        <strong>手机号 / 地址</strong>
      </div>
    </section>

    <section class="profile-grid">
      <article class="panel readonly-panel">
        <div class="section-head">
          <h2>实名信息</h2>
          <span>只读</span>
        </div>

        <div v-if="loading" class="skeleton">
          <i></i>
          <i></i>
          <i></i>
        </div>

        <dl v-else class="identity-list">
          <div>
            <dt>姓名</dt>
            <dd>{{ profile.name || '暂未登记' }}</dd>
          </div>
          <div>
            <dt>身份证号</dt>
            <dd>{{ profile.idCard || '暂未登记' }}</dd>
          </div>
          <div>
            <dt>手机号</dt>
            <dd>{{ profile.phone || '暂未登记' }}</dd>
          </div>
        </dl>
      </article>

      <article class="panel edit-panel">
        <div class="section-head">
          <h2>联系方式</h2>
          <span>可修改</span>
        </div>

        <form class="form" @submit.prevent="saveProfile">
          <label class="field" for="profile-phone">
            <span>手机号</span>
            <input
              id="profile-phone"
              v-model.trim="form.phone"
              type="tel"
              inputmode="tel"
              autocomplete="tel"
              placeholder="请输入 11 位手机号"
              :aria-invalid="!!errors.phone"
              aria-describedby="phone-help"
              @blur="validate"
            />
            <small id="phone-help" :class="{ invalid: errors.phone }">
              {{ errors.phone || '如手机号已脱敏，可保持原值；修改时请输入完整手机号。' }}
            </small>
          </label>

          <label class="field" for="profile-address">
            <span>联系地址</span>
            <textarea
              id="profile-address"
              v-model.trim="form.address"
              rows="5"
              maxlength="120"
              placeholder="请输入联系地址"
              :aria-invalid="!!errors.address"
              aria-describedby="address-help"
              @blur="validate"
            ></textarea>
            <small id="address-help" :class="{ invalid: errors.address }">
              {{ errors.address || `${form.address.length}/120，用于医院联系和就诊提醒。` }}
            </small>
          </label>

          <div class="actions">
            <button type="button" class="ghost-btn" :disabled="saving || !hasChanges" @click="syncForm">
              重置
            </button>
            <button type="submit" class="save-btn" :disabled="saving || !hasChanges">
              {{ saving ? '保存中...' : '保存资料' }}
            </button>
          </div>
        </form>
      </article>
    </section>

    <section class="logout-panel" aria-label="账号操作">
      <div>
        <h2>账号操作</h2>
        <p>退出当前患者账号，返回登录页。</p>
      </div>
      <button class="logout-btn" type="button" @click="logout">退出登录</button>
    </section>
  </div>
</template>

<style scoped>
.profile-view {
  min-height: 100%;
  padding: 16px 16px calc(var(--bottom-nav-h) + 24px);
}
.logout-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 16px;
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}
.logout-panel h2 {
  color: var(--color-ink);
  font-size: 16px;
  font-weight: 800;
}
.logout-panel p {
  margin-top: 5px;
  color: var(--color-ink-3);
  font-size: 12.5px;
}
.logout-btn {
  flex-shrink: 0;
  min-height: 40px;
  border: 1.5px solid rgba(220, 38, 38, 0.35);
  border-radius: var(--radius-sm);
  padding: 0 16px;
  background: var(--color-danger-soft);
  color: var(--color-danger);
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}
.hero {
  display: flex;
  align-items: center;
  gap: 14px;
  border-radius: var(--radius-lg);
  padding: 20px;
  background: linear-gradient(135deg, #0e8f86, #16a34a);
  color: #fff;
  box-shadow: var(--shadow);
}
.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.42);
  font-size: 22px;
  font-weight: 800;
}
.hero-main {
  flex: 1;
  min-width: 0;
}
.eyebrow {
  font-size: 12px;
  opacity: 0.82;
}
.hero h1 {
  margin-top: 2px;
  font-size: 24px;
  line-height: 1.2;
  overflow-wrap: anywhere;
}
.hero p:last-child {
  margin-top: 7px;
  font-size: 13px;
  line-height: 1.65;
  opacity: 0.9;
}
.refresh-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  border: 1px solid rgba(255, 255, 255, 0.42);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  transition: background 0.18s ease, opacity 0.18s ease;
}
.refresh-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.25);
}
.notice {
  margin-top: 12px;
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
.status-panel {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 14px 0;
}
.status-panel div,
.panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}
.status-panel div {
  min-height: 74px;
  padding: 12px;
}
.status-panel span {
  display: block;
  font-size: 12px;
  color: var(--color-ink-3);
}
.status-panel strong {
  display: block;
  margin-top: 6px;
  color: var(--color-ink);
  font-size: 15px;
  line-height: 1.35;
}
.profile-grid {
  display: grid;
  grid-template-columns: minmax(260px, 0.9fr) minmax(320px, 1.2fr);
  gap: 14px;
}
.panel {
  padding: 16px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}
.section-head h2 {
  font-size: 16px;
  color: var(--color-ink);
}
.section-head span {
  border-radius: 999px;
  padding: 4px 9px;
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
  font-size: 12px;
  font-weight: 800;
}
.readonly-panel .section-head span {
  background: var(--color-border);
  color: var(--color-ink-3);
}
.identity-list {
  display: grid;
  gap: 10px;
}
.identity-list div {
  border-radius: var(--radius-sm);
  padding: 12px;
  background: var(--color-bg);
}
.identity-list dt {
  font-size: 12px;
  color: var(--color-ink-3);
}
.identity-list dd {
  margin-top: 4px;
  color: var(--color-ink);
  font-size: 14px;
  font-weight: 700;
  overflow-wrap: anywhere;
}
.skeleton {
  display: grid;
  gap: 12px;
}
.skeleton i {
  height: 44px;
  border-radius: var(--radius-sm);
  background: linear-gradient(90deg, #e0f4f2, #f5fffd, #e0f4f2);
}
.form {
  display: grid;
  gap: 14px;
}
.field {
  display: grid;
  gap: 7px;
}
.field span {
  font-size: 13px;
  font-weight: 800;
  color: var(--color-ink-2);
}
.field input,
.field textarea {
  width: 100%;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 11px 12px;
  background: #fff;
  color: var(--color-ink);
  font: inherit;
  outline: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}
.field input {
  min-height: 44px;
}
.field textarea {
  resize: vertical;
  min-height: 118px;
}
.field input:focus,
.field textarea:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px rgba(14, 143, 134, 0.12);
}
.field small {
  color: var(--color-ink-3);
  font-size: 12px;
  line-height: 1.55;
}
.field small.invalid {
  color: var(--color-danger);
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.ghost-btn,
.save-btn {
  min-width: 104px;
  min-height: 42px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 800;
  font-family: var(--font-sans);
  transition: opacity 0.18s ease, background 0.18s ease;
}
.ghost-btn {
  border: 1.5px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-ink-2);
}
.save-btn {
  border: 1.5px solid var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}
button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
@media (max-width: 820px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 560px) {
  .profile-view {
    padding-inline: 12px;
  }
  .hero {
    align-items: flex-start;
    padding: 18px;
  }
  .avatar {
    width: 50px;
    height: 50px;
  }
  .status-panel {
    grid-template-columns: 1fr;
  }
  .actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
}
</style>
