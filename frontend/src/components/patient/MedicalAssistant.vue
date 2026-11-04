<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  chatWithAssistant,
  chatWithAssistantStream,
  getAssistantHistory,
} from '@/api/agent'

const SESSION_STORAGE_KEY = 'madp_patient_assistant_session_id'
// SSE 流式输出（显示层）：模型片段到达过快时，按字符平滑展示。
// 这只控制页面显示速度，真实回答仍然由后端 SSE 持续传输。
const STREAM_CHARACTER_INTERVAL_MS = 18

function createSessionId() {
  return `patient-${Date.now()}`
}

function getOrCreateSessionId() {
  const savedSessionId = localStorage.getItem(SESSION_STORAGE_KEY)
  if (savedSessionId) return savedSessionId

  const newSessionId = createSessionId()
  localStorage.setItem(SESSION_STORAGE_KEY, newSessionId)
  return newSessionId
}

const router = useRouter()
const open = ref(false)
const loading = ref(false)
const input = ref('')
// 聊天历史恢复：刷新页面后继续使用原来的 session_id。
const threadId = ref(getOrCreateSessionId())
const errorMessage = ref('')
const messages = ref([])

const quickActions = [
  { label: '帮我找医生', message: '帮我找医生' },
  { label: '预约挂号', message: '我想预约挂号' },
  { label: '查看我的预约', message: '查看我的预约' },
  { label: '查看排队进度', message: '查看我的排队进度' },
]

function openAssistant(message = '') {
  open.value = true
  if (message) sendMessage(message)
}

function closeAssistant() {
  open.value = false
}

function resetConversation() {
  threadId.value = createSessionId()
  localStorage.setItem(SESSION_STORAGE_KEY, threadId.value)
  messages.value = []
  errorMessage.value = ''
}

async function restoreConversation() {
  try {
    const response = await getAssistantHistory(threadId.value)
    const historyMessages = Array.isArray(response?.messages) ? response.messages : []

    // 聊天历史恢复：后端只返回患者和助手可见的文字消息。
    messages.value = historyMessages.map((item, index) => ({
      id: `history-${index}`,
      role: item.role,
      content: item.content,
      response: null,
    }))

    // 待确认恢复：如果 LangGraph 仍停在人工确认节点，重新显示确认卡片。
    if (response?.pending_confirmation) {
      appendMessage(
        'assistant',
        response.pending_confirmation.content,
        response.pending_confirmation,
      )
    }
    await scrollToBottom()
  } catch (error) {
    // 历史恢复失败不阻止患者开始新对话，但给出可见提示。
    errorMessage.value = error.message || '历史对话暂时无法恢复。'
  }
}

function appendMessage(role, content, response = null) {
  const newMessage = {
    id: `${Date.now()}-${messages.value.length}`,
    role,
    content,
    response,
  }
  messages.value.push(newMessage)

  // SSE 流式输出（Vue 响应式）：不能返回 push 前创建的普通对象。
  // 必须从 ref 数组中重新取得 Vue 已代理的对象，后续逐字修改 content 时，
  // 页面才会跟随每一个字符重新渲染，而不是最后一次性显示完整答案。
  return messages.value[messages.value.length - 1]
}

async function scrollToBottom() {
  await nextTick()
  const container = document.querySelector('[data-assistant-messages]')
  if (container) container.scrollTop = container.scrollHeight
}

async function sendMessage(message = input.value) {
  const content = String(message || '').trim()
  if (!content || loading.value) return

  input.value = ''
  errorMessage.value = ''
  appendMessage('user', content)
  loading.value = true
  await scrollToBottom()

  let streamingMessage = null
  let pendingStreamText = ''
  let typingTask = null

  async function typePendingText() {
    while (pendingStreamText.length > 0) {
      if (!streamingMessage) streamingMessage = appendMessage('assistant', '')

      // SSE 流式输出（显示层）：每次取一个完整 Unicode 字符，
      // 避免中文、表情等字符在页面上被拆成乱码。
      const characters = Array.from(pendingStreamText)
      streamingMessage.content += characters[0]
      pendingStreamText = characters.slice(1).join('')
      await scrollToBottom()
      await new Promise((resolve) => {
        window.setTimeout(resolve, STREAM_CHARACTER_INTERVAL_MS)
      })
    }

    typingTask = null
  }

  function enqueueStreamText(token) {
    pendingStreamText += token
    if (!typingTask) typingTask = typePendingText()
  }

  try {
    const response = await chatWithAssistantStream(
      {
        message: content,
        session_id: threadId.value,
      },
      {
        // SSE 流式输出：首次收到文字时创建助手气泡，后续片段持续追加。
        // 用户无需等待千问完成整段回答，就能立即看到生成过程。
        onToken(token) {
          if (!token) return
          enqueueStreamText(token)
        },
      },
    )

    // SSE 流式输出（显示层）：后端 done 事件可能先于打字队列展示完成，
    // 等待当前队列清空后再挂载确认卡片或页面跳转动作。
    if (typingTask) await typingTask

    const finalContent = response?.message || '助手暂时没有返回内容。'
    if (!streamingMessage) streamingMessage = appendMessage('assistant', '')

    // SSE 流式输出（显示层）：部分模型只流出很少的 token，随后在 done
    // 事件中返回完整答案。不能直接用完整答案覆盖气泡，否则会出现“先显示
    // 一个字，然后整段突然出现”的现象。
    if (finalContent.startsWith(streamingMessage.content)) {
      const remainingContent = finalContent.slice(streamingMessage.content.length)
      if (remainingContent) enqueueStreamText(remainingContent)
    } else {
      // 流式片段与最终答案不一致时，以最终结果为准并重新平滑展示。
      streamingMessage.content = ''
      enqueueStreamText(finalContent)
    }

    if (typingTask) await typingTask
    // done 事件中的结构化数据负责渲染确认卡片、支付按钮和排队按钮。
    streamingMessage.response = response
  } catch (error) {
    const message = error.message || '抱歉，我暂时无法连接服务，请稍后再试。'
    appendMessage('assistant', message)
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

async function submitAction(action) {
  if (loading.value) return
  errorMessage.value = ''
  loading.value = true
  try {
    const response = await chatWithAssistant({
      message: null,
      threadId: threadId.value,
      action,
    })
    threadId.value = response?.threadId || threadId.value
    appendMessage('assistant', response?.message || '助手暂时没有返回内容。', response)
  } catch (error) {
    const message = error.message || '抱歉，这项操作暂时无法完成，请稍后再试。'
    appendMessage('assistant', message)
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

async function submitDecision(decision) {
  if (loading.value) return

  const pendingMessage = [...messages.value]
    .reverse()
    .find((item) => item.response?.requires_confirmation && !item.confirmationResolved)

  if (!pendingMessage) return

  pendingMessage.confirmationResolved = true
  errorMessage.value = ''
  loading.value = true

  try {
    const response = await chatWithAssistant({
      decision,
      session_id: threadId.value,
    })
    appendMessage('assistant', response?.message || '助手暂时没有返回内容。', response)
  } catch (error) {
    pendingMessage.confirmationResolved = false
    const message = error.message || '抱歉，这项操作暂时无法完成，请稍后再试。'
    appendMessage('assistant', message)
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

function responseType(message) {
  if (message.response?.requires_confirmation) return 'CONFIRM'
  return message.response?.type || 'TEXT'
}

function sourceList(message) {
  return message.response?.data?.sources || message.response?.data?.records || []
}

function doctorList(message) {
  return message.response?.data?.records || []
}

function chooseSource(source) {
  return submitAction({ type: 'SELECT_SOURCE', sourceId: source.sourceId || source.id })
}

function confirmBooking(confirmed) {
  return submitDecision(confirmed ? 'approve' : 'reject')
}

function pendingActionName(message) {
  return message.response?.pending_action?.name || ''
}

function confirmationTitle(message) {
  const actionName = pendingActionName(message)
  if (actionName === 'cancel_appointment') return '请确认是否取消这条预约'
  if (actionName === 'sign_in') return '请确认是否签到并进入排队'
  return '请确认后继续创建预约'
}

function confirmationButtonText(message) {
  const actionName = pendingActionName(message)
  if (actionName === 'cancel_appointment') return '确认取消预约'
  if (actionName === 'sign_in') return '确认签到'
  return '确认预约'
}

function goToPayment(message) {
  const path = message.response?.action?.path
  if (path) {
    router.push(path)
    closeAssistant()
  }
}

function openActionPage(message) {
  const path = message.response?.action?.path
  if (path) {
    router.push(path)
    closeAssistant()
  }
}

onMounted(restoreConversation)

defineExpose({ openAssistant })
</script>

<template>
  <button class="assistant-fab" type="button" aria-label="打开医联小智" @click="openAssistant()">
    <span aria-hidden="true">✦</span>
    <small>医联小智</small>
  </button>

  <div class="assistant-backdrop" :class="{ visible: open }" @click="closeAssistant" />

  <aside class="assistant-drawer" :class="{ visible: open }" aria-label="医联小智" :aria-hidden="!open">
    <header class="assistant-header">
      <div class="assistant-identity">
        <div class="assistant-header-avatar" aria-hidden="true">智</div>
        <div>
          <p class="assistant-kicker">PATIENT CARE</p>
          <h2>医联小智</h2>
          <span class="assistant-status"><i aria-hidden="true" /> 在线 · 帮你完成就医查询与预约</span>
        </div>
      </div>
      <div class="assistant-header-actions">
        <button type="button" aria-label="新建对话" title="新建对话" @click="resetConversation">＋</button>
        <button type="button" aria-label="关闭助手" title="关闭助手" @click="closeAssistant">×</button>
      </div>
    </header>

    <section class="assistant-messages" data-assistant-messages aria-live="polite">
      <div v-if="messages.length === 0" class="assistant-message assistant">
        <div class="message-avatar" aria-hidden="true">智</div>
        <div class="message-content">
          <span class="message-sender">医联小智</span>
          <div class="message-bubble welcome-bubble">
            <strong>你好，我是医联小智</strong>
            <span>我可以帮你查询医生、号源、预约和排队进度。你可以直接输入问题，也可以选择下方的快捷提问。</span>
          </div>
        </div>
      </div>

      <article v-for="item in messages" :key="item.id" class="assistant-message" :class="item.role">
        <div class="message-avatar" aria-hidden="true">{{ item.role === 'assistant' ? '智' : '我' }}</div>
        <div class="message-content">
          <span class="message-sender">{{ item.role === 'assistant' ? '医联小智' : '我' }}</span>
          <div class="message-bubble">{{ item.content }}</div>

          <div v-if="item.role === 'assistant' && responseType(item) === 'DOCTOR_LIST'" class="assistant-data-card">
            <button v-for="doctor in doctorList(item)" :key="doctor.doctorId || doctor.id" type="button" class="doctor-option" @click="sendMessage(`查询${doctor.name}的号源`)">
              <span class="doctor-avatar">{{ doctor.name?.slice(0, 1) || '医' }}</span>
              <span><strong>{{ doctor.name }}</strong><small>{{ doctor.department }} · {{ doctor.title }}</small></span>
              <span class="card-arrow">›</span>
            </button>
          </div>

          <div v-if="item.role === 'assistant' && responseType(item) === 'SOURCE_LIST'" class="assistant-data-card">
            <button v-for="source in sourceList(item)" :key="source.sourceId || source.id" type="button" class="source-option" @click="chooseSource(source)">
              <span><strong>{{ source.startTime && source.endTime ? `${source.startTime} - ${source.endTime}` : (source.timePeriod || '预约时段') }}</strong><small>{{ source.visitDate || source.date || '日期待返回' }}</small></span>
              <span class="remain">余号 {{ source.remainCount ?? '--' }}</span>
            </button>
          </div>

          <div v-if="item.role === 'assistant' && responseType(item) === 'CONFIRM' && !item.confirmationResolved" class="assistant-data-card confirm-card">
            <p>{{ confirmationTitle(item) }}</p>
            <div class="confirm-actions"><button type="button" class="confirm-primary" @click="confirmBooking(true)">{{ confirmationButtonText(item) }}</button><button type="button" @click="confirmBooking(false)">再想想</button></div>
          </div>

          <div v-if="item.role === 'assistant' && item.response?.action?.type === 'GO_TO_PAYMENT'" class="assistant-data-card confirm-card">
            <p>预约已经创建，请在规定时间内完成支付。</p>
            <div class="confirm-actions"><button type="button" class="confirm-primary" @click="goToPayment(item)">前往支付</button></div>
          </div>

          <div v-if="item.role === 'assistant' && item.response?.action?.type === 'VIEW_APPOINTMENTS'" class="assistant-data-card confirm-card">
            <p>预约状态已经更新，你可以前往预约列表查看。</p>
            <div class="confirm-actions"><button type="button" class="confirm-primary" @click="openActionPage(item)">查看我的预约</button></div>
          </div>

          <div v-if="item.role === 'assistant' && item.response?.action?.type === 'VIEW_QUEUE'" class="assistant-data-card confirm-card">
            <p>签到已经完成，你可以查看实时排队进度。</p>
            <div class="confirm-actions"><button type="button" class="confirm-primary" @click="openActionPage(item)">查看排队进度</button></div>
          </div>
        </div>
      </article>

      <div v-if="loading" class="assistant-message assistant assistant-loading-row" aria-label="医联小智正在回复">
        <div class="message-avatar" aria-hidden="true">智</div>
        <div class="message-content">
          <span class="message-sender">医联小智</span>
          <div class="message-bubble assistant-loading"><span /><span /><span /></div>
        </div>
      </div>
      <p v-if="errorMessage" class="assistant-error" role="alert">{{ errorMessage }}</p>
    </section>

    <div class="assistant-quick-actions">
      <button v-for="action in quickActions" :key="action.label" type="button" :disabled="loading" @click="sendMessage(action.message)">{{ action.label }}</button>
    </div>
    <p class="assistant-disclaimer">内容仅供健康参考，不能替代医生诊断。</p>
    <form class="assistant-composer" @submit.prevent="sendMessage()">
      <textarea v-model="input" rows="1" :disabled="loading" aria-label="输入问题" placeholder="请输入你的问题…" @keydown.enter.exact.prevent="sendMessage()" />
      <button type="submit" :disabled="loading || !input.trim()" aria-label="发送消息">↑</button>
    </form>
  </aside>
</template>

<style scoped>
.assistant-fab{position:fixed;right:18px;bottom:calc(var(--bottom-nav-h,64px) + 18px);z-index:20;width:62px;height:62px;border:0;border-radius:20px;background:linear-gradient(145deg,#18b5a5,#087c76);color:#fff;box-shadow:0 12px 26px rgba(10,128,119,.28);cursor:pointer;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:1px;transition:transform .18s ease}.assistant-fab:hover{transform:translateY(-2px)}.assistant-fab span{font-size:22px;line-height:1}.assistant-fab small{font-size:9px;font-weight:600}
.assistant-backdrop{position:fixed;inset:0;z-index:29;background:rgba(17,50,55,.34);backdrop-filter:blur(2px);opacity:0;pointer-events:none;transition:opacity .24s ease}.assistant-backdrop.visible{opacity:1;pointer-events:auto}
.assistant-drawer{position:fixed;top:50%;left:50%;z-index:30;width:min(520px,calc(100vw - 40px));height:min(760px,calc(100dvh - 48px));border:1px solid rgba(217,234,231,.9);border-radius:24px;background:var(--color-surface,#fff);box-shadow:0 28px 80px rgba(24,64,70,.24);transform:translate(-50%,-46%) scale(.96);opacity:0;pointer-events:none;overflow:hidden;transition:transform .28s ease,opacity .22s ease;display:flex;flex-direction:column}.assistant-drawer.visible{transform:translate(-50%,-50%) scale(1);opacity:1;pointer-events:auto}
.assistant-header{padding:18px 20px;border-bottom:1px solid var(--color-border,#d9eae7);display:flex;justify-content:space-between;align-items:center;background:#fff}.assistant-identity{display:flex;align-items:center;gap:12px;min-width:0}.assistant-header-avatar,.message-avatar{display:grid;place-items:center;flex:0 0 auto;background:linear-gradient(145deg,#21b5a7,#087c76);color:#fff;font-weight:700}.assistant-header-avatar{width:46px;height:46px;border-radius:15px;font-size:18px}.assistant-kicker{margin:0 0 2px;color:var(--color-primary,#0d9488);font-size:10px;letter-spacing:.16em;font-weight:700}.assistant-header h2{margin:0;color:var(--color-ink,#173b43);font-size:20px}.assistant-header .assistant-status{display:flex;align-items:center;gap:5px;margin-top:3px;color:var(--color-ink-3,#6b8586);font-size:11px}.assistant-status i{width:7px;height:7px;border-radius:50%;background:#22b573;box-shadow:0 0 0 3px #e4f8ee}.assistant-header-actions{display:flex;gap:8px}.assistant-header-actions button{width:44px;height:44px;border:1px solid var(--color-border,#d9eae7);border-radius:12px;background:var(--color-bg,#f4faf9);color:var(--color-ink,#173b43);font-size:20px;cursor:pointer}.assistant-header-actions button:hover{border-color:#9bd8cf;background:#edf9f7}
.assistant-messages{flex:1;min-height:0;overflow-y:auto;padding:22px 20px;background:linear-gradient(180deg,#f5fbfa 0%,#fbfefd 70%,#fff 100%);scroll-behavior:smooth}.assistant-message{width:100%;margin-bottom:20px;display:flex;align-items:flex-start;gap:9px}.assistant-message.user{flex-direction:row-reverse}.message-avatar{width:32px;height:32px;border-radius:10px;font-size:12px;box-shadow:0 4px 10px rgba(13,148,136,.14)}.assistant-message.user .message-avatar{background:#dcebea;color:#315e60;box-shadow:none}.message-content{min-width:0;max-width:calc(82% - 41px);display:flex;flex-direction:column;align-items:flex-start}.assistant-message.user .message-content{align-items:flex-end}.message-sender{margin:0 3px 5px;color:#728b8c;font-size:11px}.message-bubble{max-width:100%;padding:11px 14px;border:1px solid #d8ede9;border-radius:5px 17px 17px 17px;background:#fff;color:var(--color-ink,#173b43);box-shadow:0 4px 14px rgba(24,64,70,.05);font-size:14px;line-height:1.65;white-space:pre-wrap;overflow-wrap:anywhere}.assistant-message.user .message-bubble{border-color:var(--color-primary,#0d9488);border-radius:17px 5px 17px 17px;background:var(--color-primary,#0d9488);color:#fff;box-shadow:0 5px 14px rgba(13,148,136,.18)}.welcome-bubble strong,.welcome-bubble span{display:block}.welcome-bubble strong{margin-bottom:5px}.welcome-bubble span{color:#526f72}
.assistant-data-card{width:100%;margin-top:8px;border:1px solid var(--color-border,#d9eae7);border-radius:14px;background:#fff;overflow:hidden;box-shadow:0 5px 16px rgba(24,64,70,.06)}.doctor-option,.source-option{width:100%;min-height:52px;padding:11px 12px;border:0;border-bottom:1px solid #edf5f3;background:#fff;display:flex;align-items:center;gap:10px;text-align:left;color:var(--color-ink,#173b43);cursor:pointer}.doctor-option:last-child,.source-option:last-child{border-bottom:0}.doctor-option:hover,.source-option:hover{background:#f2fbf9}.doctor-avatar{width:32px;height:32px;border-radius:10px;background:#dff6f1;color:var(--color-primary,#0d9488);display:grid;place-items:center;font-weight:700}.doctor-option strong,.source-option strong{display:block;font-size:13px}.doctor-option small,.source-option small{display:block;margin-top:3px;color:var(--color-ink-3,#6b8586);font-size:11px}.card-arrow{margin-left:auto;font-size:22px;color:var(--color-primary,#0d9488)}.source-option{justify-content:space-between}.remain{font-size:11px;color:#b45309;background:#fff5df;padding:5px 7px;border-radius:8px}.confirm-card{padding:13px}.confirm-card p{margin:0 0 10px;font-size:12px}.confirm-actions{display:flex;gap:8px}.confirm-actions button{flex:1;min-height:44px;border:1px solid var(--color-border,#d9eae7);border-radius:9px;background:#fff;color:var(--color-ink,#173b43);cursor:pointer}.confirm-actions .confirm-primary{background:var(--color-primary,#0d9488);border-color:var(--color-primary,#0d9488);color:#fff}
.assistant-loading-row{margin-bottom:4px}.assistant-loading{min-width:58px;display:flex;align-items:center;justify-content:center;gap:5px}.assistant-loading span{width:7px;height:7px;border-radius:50%;background:var(--color-primary,#0d9488);animation:pulse 1s infinite}.assistant-loading span:nth-child(2){animation-delay:.15s}.assistant-loading span:nth-child(3){animation-delay:.3s}.assistant-error{margin:10px 0 0;padding:10px 12px;border-radius:10px;background:#fff0f0;color:#b42318;font-size:12px;line-height:1.5}
.assistant-quick-actions{display:flex;gap:8px;overflow-x:auto;padding:12px 20px 3px;background:#fff;scrollbar-width:none}.assistant-quick-actions::-webkit-scrollbar{display:none}.assistant-quick-actions button{min-height:36px;white-space:nowrap;padding:7px 12px;border:1px solid #bfe5df;border-radius:999px;background:#fff;color:var(--color-primary,#0d9488);font-size:12px;cursor:pointer}.assistant-quick-actions button:hover{background:#edf9f7}.assistant-quick-actions button:disabled{opacity:.5;cursor:not-allowed}.assistant-disclaimer{margin:7px 20px 0;color:#8da3a4;font-size:10px;line-height:1.4}.assistant-composer{display:flex;align-items:flex-end;gap:9px;padding:11px 20px 18px;background:#fff}.assistant-composer textarea{min-width:0;flex:1;min-height:48px;max-height:112px;padding:12px 14px;border:1px solid var(--color-border,#d9eae7);border-radius:15px;outline:none;resize:none;color:var(--color-ink,#173b43);background:#fbfefd;font:inherit;font-size:14px;line-height:1.55}.assistant-composer textarea:focus{border-color:var(--color-primary,#0d9488);box-shadow:0 0 0 3px rgba(13,148,136,.12)}.assistant-composer button{width:48px;height:48px;border:0;border-radius:14px;background:var(--color-primary,#0d9488);color:#fff;font-size:20px;cursor:pointer}.assistant-composer button:disabled{opacity:.42;cursor:not-allowed}
button:focus-visible,textarea:focus-visible{outline:3px solid rgba(13,148,136,.24);outline-offset:2px}@keyframes pulse{0%,100%{opacity:.3;transform:translateY(1px)}50%{opacity:1;transform:translateY(-1px)}}
@media(prefers-reduced-motion:reduce){.assistant-fab,.assistant-backdrop,.assistant-drawer{transition:none}.assistant-messages{scroll-behavior:auto}.assistant-loading span{animation:none}}
@media(max-width:560px){.assistant-drawer{width:calc(100vw - 20px);height:calc(100dvh - 20px);border-radius:20px}.assistant-header{padding:14px}.assistant-header-avatar{display:none}.assistant-header-actions button{width:42px;height:42px}.assistant-messages{padding:18px 14px}.message-content{max-width:calc(88% - 41px)}.assistant-quick-actions{padding-inline:14px}.assistant-disclaimer{margin-inline:14px}.assistant-composer{padding:10px 14px 14px}.assistant-fab{right:14px}}
</style>
