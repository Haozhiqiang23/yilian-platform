<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { getVerifyCode, register } from '@/api/auth'

// 注册页：手机号 → 验证码 → 实名信息 → 密码
// 等后端：验证码 POST /auth/verify-code；注册 POST /auth/register（接口清单 3.2/3.3）
const router = useRouter()

const form = reactive({
  phone: '',
  code: '',
  idCard: '',
  name: '',
  password: '',
  confirm: '',
})

const codeId = ref('') // 验证码标识（防错配，注册提交时回传）
const countdown = ref(0)
const loading = ref(false)
const errorMsg = ref('')

let timer = null

function isPhone(v) {
  return /^1[3-9]\d{9}$/.test(v)
}
function isIdCard(v) {
  return /^\d{17}[\dXx]$/.test(v)
}

function validate() {
  errorMsg.value = ''
  if (!isPhone(form.phone)) return (errorMsg.value = '请输入正确的 11 位手机号')
  if (!form.code) return (errorMsg.value = '请输入验证码')
  if (!isIdCard(form.idCard)) return (errorMsg.value = '请输入正确的 18 位身份证号')
  if (!form.name.trim()) return (errorMsg.value = '请输入姓名')
  if (form.password.length < 6) return (errorMsg.value = '密码至少 6 位')
  if (form.password !== form.confirm) return (errorMsg.value = '两次输入的密码不一致')
  return true
}

async function sendCode() {
  if (!isPhone(form.phone)) {
    errorMsg.value = '请先输入正确的手机号'
    return
  }
  if (countdown.value > 0) return
  try {
    errorMsg.value = ''
    // 等后端：发送验证码 → 返回 { codeId, ttlSeconds }，codeId 在注册时回传防错配
    const data = await getVerifyCode({ phone: form.phone })
    codeId.value = data?.codeId || ''
    countdown.value = data?.ttlSeconds > 60 ? 60 : data?.ttlSeconds || 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    errorMsg.value = e.message || '验证码发送失败，请稍后重试'
  }
}

async function handleRegister() {
  if (!validate() || loading.value) return
  loading.value = true
  try {
    // 等后端：POST /auth/register → { phone, verifyCode, codeId, idCard, name, password }
    await register({
      phone: form.phone,
      verifyCode: form.code,
      codeId: codeId.value,
      idCard: form.idCard,
      name: form.name,
      password: form.password,
    })
    errorMsg.value = ''
    router.push('/login')
  } catch (e) {
    errorMsg.value = e.message || '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-wrap">
    <div class="register-card">
      <div class="reg-head">
        <h1>创建账号</h1>
        <p>手机号验证码两步注册，实名信息用于就医</p>
      </div>

      <div class="field">
        <label for="phone">手机号</label>
        <input
          id="phone"
          v-model="form.phone"
          type="tel"
          maxlength="11"
          placeholder="请输入 11 位手机号"
        />
      </div>

      <div class="field">
        <label for="code">验证码</label>
        <div class="code-row">
          <input id="code" v-model="form.code" type="text" maxlength="6" placeholder="6 位验证码" />
          <button class="btn-code" :disabled="countdown > 0" @click="sendCode">
            {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
          </button>
        </div>
      </div>

      <div class="field">
        <label for="idCard">身份证号</label>
        <input id="idCard" v-model="form.idCard" type="text" maxlength="18" placeholder="用于实名认证" />
      </div>

      <div class="field">
        <label for="name">姓名</label>
        <input id="name" v-model="form.name" type="text" placeholder="请输入真实姓名" />
      </div>

      <div class="field">
        <label for="regPwd">密码</label>
        <input id="regPwd" v-model="form.password" type="password" placeholder="至少 6 位" />
      </div>

      <div class="field">
        <label for="regPwd2">确认密码</label>
        <input id="regPwd2" v-model="form.confirm" type="password" placeholder="再次输入密码" />
      </div>

      <p v-if="errorMsg" class="form-error" role="alert">{{ errorMsg }}</p>

      <button class="btn-register" :disabled="loading" @click="handleRegister">
        {{ loading ? '注册中…' : '注 册' }}
      </button>

      <p class="login-line">
        已有账号？
        <router-link to="/login">去登录</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
.register-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(600px 420px at 90% 10%, rgba(20, 184, 166, 0.14), transparent 60%),
    radial-gradient(520px 380px at 5% 90%, rgba(22, 163, 74, 0.12), transparent 60%),
    var(--color-bg);
  padding: 40px 22px;
}
.register-card {
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: var(--radius-lg);
  padding: 32px 30px 26px;
  box-shadow: var(--shadow-lg);
}
.reg-head {
  margin-bottom: 22px;
}
.reg-head h1 {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-ink);
  margin-bottom: 5px;
}
.reg-head p {
  font-size: 12.5px;
  color: var(--color-ink-3);
}
.field {
  margin-bottom: 14px;
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
.code-row {
  display: flex;
  gap: 10px;
}
.code-row input {
  flex: 1;
}
.btn-code {
  flex-shrink: 0;
  padding: 0 14px;
  border: 1.5px solid var(--color-primary);
  border-radius: var(--radius-sm);
  background: var(--color-primary-soft);
  color: var(--color-primary-deep);
  font-size: 13px;
  font-weight: 600;
  font-family: var(--font-sans);
  cursor: pointer;
  transition: opacity 0.18s ease;
  white-space: nowrap;
}
.btn-code:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.form-error {
  color: var(--color-danger);
  font-size: 12.5px;
  margin: -2px 0 12px;
}
.btn-register {
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
  transition: opacity 0.18s ease;
}
.btn-register:disabled {
  opacity: 0.75;
  cursor: not-allowed;
}
.login-line {
  text-align: center;
  font-size: 13.5px;
  color: var(--color-ink-3);
  margin-top: 18px;
}
.login-line a {
  color: var(--color-primary);
  font-weight: 600;
  margin-left: 4px;
}
</style>
