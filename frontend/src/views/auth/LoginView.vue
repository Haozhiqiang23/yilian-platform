<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore, HOME_BY_ROLE } from '@/stores/user'
import AppLogo from '@/components/AppLogo.vue'
import { login } from '@/api/auth'

// 登录页（三端统一入口）
// 骨架阶段：后端未就绪时使用"演示登录"（任意账号密码可登录，可选角色）
const router = useRouter()
const route = useRoute()
const store = useUserStore()

const form = reactive({
  account: '',
  password: '',
  remember: true,
})


const loading = ref(false)
const errorMsg = ref('')

// 字段级错误：账号错误提示在账号下方，密码错误提示在密码下方
const errors = reactive({ account: '', password: '' })

function validate() {
  errors.account = form.account.trim() ? '' : '请输入账号/手机号'
  errors.password = form.password ? '' : '请输入密码'
  return !errors.account && !errors.password
}

async function handleLogin() {
  if (!validate() || loading.value) return
  loading.value = true
  errorMsg.value = ''
  try {
    // 等后端：POST /api/v1/auth/login → { token, userId, role, name }
    const data = await login({ phone: form.account, password: form.password })
    // 后端返回角色为大写（PATIENT/DOCTOR/ADMIN），统一转小写与路由分区对齐
    const role = String(data.role || '').toLowerCase()
    store.setSession({ ...data, role })
    // 跳转登录前目标页，无则按角色落地
    const redirect = route.query.redirect
    router.push(redirect || HOME_BY_ROLE[role])
  } catch (e) {
    errorMsg.value = e.message || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-wrap">
    <div class="login-card-area">
      <div class="login-card">
        <div class="brand-row">
          <div class="brand-icon"><AppLogo /></div>
          <div>
            <div class="brand-name">医联智诊平台</div>
            <div class="brand-sub">YiLian Smart Clinic</div>
          </div>
        </div>

        <h1 class="login-title">欢迎回来</h1>
        <p class="login-desc">登录后为您提供预约挂号、排队签到与诊疗记录一站式服务</p>

        <div class="field">
          <label for="account">手机号 / 账号</label>
          <input
            id="account"
            v-model="form.account"
            type="text"
            placeholder="请输入手机号"
            autocomplete="username"
          />
          <p v-if="errors.account" class="field-error" role="alert">{{ errors.account }}</p>
        </div>

        <div class="field">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            @keyup.enter="handleLogin"
          />
          <p v-if="errors.password" class="field-error" role="alert">{{ errors.password }}</p>
        </div>

        <p v-if="errorMsg" class="form-error" role="alert">{{ errorMsg }}</p>


        <div class="row-between">
          <label class="check">
            <input v-model="form.remember" type="checkbox" />
            记住账号
          </label>
          <a class="link" href="javascript:void(0)">忘记密码?</a>
        </div>

        <button class="btn-login" :disabled="loading" @click="handleLogin">
          {{ loading ? '登录中…' : '登 录' }}
        </button>

        <div class="divider">或</div>
        <p class="register-line">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
        </p>

        <div class="roles-tip">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2l8 4v6c0 5-3.5 8.5-8 10-4.5-1.5-8-5-8-10V6l8-4z" /><path d="M9 12l2 2 4-4" /></svg>
          <span>三角色统一入口：患者 · 医生 · 管理员登录后自动跳转对应工作台。</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(600px 420px at 12% 8%, rgba(20, 184, 166, 0.16), transparent 60%),
    radial-gradient(520px 380px at 92% 6%, rgba(22, 163, 74, 0.13), transparent 60%),
    radial-gradient(500px 420px at 85% 95%, rgba(14, 143, 134, 0.1), transparent 60%),
    var(--color-bg);
  padding: 40px 22px;
}
.login-card {
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: var(--radius-lg);
  padding: 34px 32px 28px;
  box-shadow: var(--shadow-lg);
}
.brand-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}
.brand-icon {
  width: 44px;
  height: 44px;
  border-radius: 13px;
  background: linear-gradient(135deg, #14b8a6, #0e8f86);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 20px rgba(14, 143, 134, 0.28);
}
.brand-icon svg {
  width: 22px;
  height: 22px;
}
.brand-name {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: var(--color-ink);
}
.brand-sub {
  font-size: 12px;
  color: var(--color-ink-3);
  margin-top: 3px;
}
.login-title {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--color-ink);
}
.login-desc {
  font-size: 13px;
  color: var(--color-ink-3);
  margin-bottom: 24px;
  line-height: 1.6;
}
.field {
  margin-bottom: 15px;
}
.field label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 7px;
  color: var(--color-ink-2);
}
.field input {
  width: 100%;
  padding: 12px 14px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 15px;
  background: rgba(255, 255, 255, 0.9);
  color: var(--color-ink);
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.field input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px rgba(14, 143, 134, 0.14);
}
.field input::placeholder {
  color: #9dbdb8;
}
.form-error {
  color: var(--color-danger);
  font-size: 12.5px;
  margin: -4px 0 12px;
}
.field-error {
  color: var(--color-danger);
  font-size: 12px;
  margin-top: 6px;
}
.row-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 0 18px;
  font-size: 12.5px;
}
.check {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--color-ink-3);
  cursor: pointer;
  user-select: none;
}
.check input {
  width: 15px;
  height: 15px;
  accent-color: var(--color-primary);
  cursor: pointer;
}
.link {
  color: var(--color-primary);
  font-size: 12.5px;
  font-weight: 500;
}
.btn-login {
  width: 100%;
  padding: 13.5px;
  border: none;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, #14b8a6, #0b7a72);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  font-family: var(--font-sans);
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(14, 143, 134, 0.32);
  transition: transform 0.18s ease, box-shadow 0.18s ease, opacity 0.18s ease;
}
.btn-login:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 26px rgba(14, 143, 134, 0.4);
}
.btn-login:disabled {
  opacity: 0.75;
  cursor: not-allowed;
}
.divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 20px 0 16px;
  color: var(--color-ink-3);
  font-size: 12px;
}
.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--color-border);
}
.register-line {
  text-align: center;
  font-size: 13.5px;
  color: var(--color-ink-3);
}
.register-line a {
  color: var(--color-primary);
  font-weight: 600;
  margin-left: 4px;
}
.roles-tip {
  margin-top: 20px;
  padding: 11px 13px;
  border-radius: var(--radius-sm);
  background: var(--color-primary-soft);
  font-size: 12px;
  color: var(--color-ink-2);
  line-height: 1.7;
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.roles-tip svg {
  flex-shrink: 0;
  margin-top: 2px;
  color: var(--color-primary);
}
</style>

