<script setup>
import { computed, onMounted, ref } from 'vue'
import { getMessages, getUnreadMessageCount, markAllMessagesRead, markMessageRead } from '@/api/messages'
import ListPager from '@/components/common/ListPager.vue'

const messages = ref([])
const loading = ref(false)
const actionLoading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const unreadCount = ref(0)
const page = ref(1)
const pageSize = 10
const total = ref(0)

const unreadMessages = computed(() => messages.value.filter((item) => !item.read))

function notifyUnreadCount() {
  window.dispatchEvent(new CustomEvent('patient-unread-count-changed', {
    detail: unreadCount.value,
  }))
}

function typeText(type) {
  return { APPOINTMENT: '预约', REMINDER: '提醒', SYSTEM: '系统' }[type] || '通知'
}

async function loadMessages() {
  loading.value = true
  errorMsg.value = ''
  try {
    const [list, count] = await Promise.all([
      getMessages({ page: page.value, size: pageSize }),
      getUnreadMessageCount(),
    ])
    messages.value = Array.isArray(list) ? list : list?.records || []
    total.value = Array.isArray(list) ? list.length : Number(list?.total || 0)
    unreadCount.value = Number(count?.unreadCount ?? unreadMessages.value.length)
    notifyUnreadCount()
  } catch (error) {
    errorMsg.value = error.message || '消息加载失败，请稍后重试'
    messages.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function readMessage(message) {
  if (message.read || actionLoading.value) return
  actionLoading.value = true
  try {
    const result = await markMessageRead(message.id)
    Object.assign(message, result || {}, { read: true })
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    notifyUnreadCount()
    successMsg.value = '消息已标记为已读'
  } catch (error) {
    errorMsg.value = error.message || '标记已读失败，请稍后重试'
  } finally {
    actionLoading.value = false
  }
}

async function readAll() {
  if (!unreadMessages.value.length || actionLoading.value) return
  actionLoading.value = true
  try {
    await markAllMessagesRead()
    messages.value.forEach((message) => { message.read = true })
    unreadCount.value = 0
    notifyUnreadCount()
    successMsg.value = '全部消息已读'
  } catch (error) {
    errorMsg.value = error.message || '全部标记已读失败，请稍后重试'
  } finally {
    actionLoading.value = false
  }
}

onMounted(loadMessages)

function changePage(value) {
  page.value = value
  loadMessages()
}
</script>

<template>
  <div class="messages-view">
    <section class="page-hero" aria-labelledby="messages-title">
      <div>
        <p class="eyebrow">Notifications</p>
        <h1 id="messages-title">消息通知</h1>
        <p>查看预约、就诊和系统相关通知。</p>
      </div>
      <div class="unread-summary">{{ unreadCount }} 条未读</div>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>
    <p v-if="successMsg" class="notice success" role="status">{{ successMsg }}</p>

    <section class="toolbar" aria-label="消息操作">
      <button class="secondary-btn" :disabled="loading" type="button" @click="loadMessages">刷新</button>
      <button class="primary-btn" :disabled="actionLoading || !unreadMessages.length" type="button" @click="readAll">全部已读</button>
    </section>

    <section v-if="loading" class="state-card">正在加载消息...</section>
    <section v-else-if="!messages.length" class="state-card empty-state">
      <h2>暂无消息</h2>
      <p>预约、支付或就诊产生的通知会显示在这里。</p>
    </section>
    <section v-else class="message-list" aria-label="消息列表">
      <article v-for="message in messages" :key="message.id" class="message-card" :class="{ unread: !message.read }">
        <div class="message-mark" aria-hidden="true"></div>
        <div class="message-content">
          <div class="message-head">
            <h2>{{ message.title }}</h2>
            <span class="type-tag">{{ typeText(message.type) }}</span>
          </div>
          <p>{{ message.content }}</p>
          <time>{{ message.createdAt || '时间待返回' }}</time>
        </div>
        <button v-if="!message.read" class="read-btn" :disabled="actionLoading" type="button" @click="readMessage(message)">标记已读</button>
        <span v-else class="read-label">已读</span>
      </article>
    </section>
    <ListPager :page="page" :page-size="pageSize" :total="total" :disabled="loading" @update:page="changePage" />
  </div>
</template>

<style scoped>
.messages-view { max-width: 920px; margin: 0 auto; padding: 20px 16px 110px; color: var(--color-ink-2); }
.page-hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; padding: 24px 0; }
.eyebrow { margin: 0 0 8px; color: var(--color-primary); font-size: 12px; letter-spacing: .12em; text-transform: uppercase; }
h1, h2, p { margin-top: 0; }
h1 { margin-bottom: 8px; font-size: clamp(26px, 5vw, 36px); color: var(--color-ink); }
.page-hero p:not(.eyebrow) { margin-bottom: 0; color: var(--color-ink-3); }
.unread-summary { padding: 10px 14px; border-radius: 999px; background: var(--color-primary-soft); color: var(--color-primary-deep); font-weight: 700; white-space: nowrap; }
.notice { padding: 12px 14px; border-radius: 10px; margin: 0 0 14px; }
.notice.danger { color: #a33a3a; background: #fff0f0; }
.notice.success { color: #177245; background: #edfbf3; }
.toolbar { display: flex; justify-content: flex-end; gap: 10px; margin-bottom: 14px; }
button { cursor: pointer; border: 0; border-radius: 9px; padding: 10px 14px; font: inherit; }
.primary-btn { background: var(--color-primary); color: white; }
.secondary-btn { background: var(--color-surface); border: 1px solid var(--color-border); color: var(--color-ink-2); }
button:disabled { cursor: not-allowed; opacity: .5; }
.state-card { padding: 42px 20px; text-align: center; border: 1px solid var(--color-border); border-radius: 14px; background: var(--color-surface); }
.state-card h2 { margin-bottom: 8px; color: var(--color-ink); }
.state-card p { margin-bottom: 0; color: var(--color-ink-3); }
.message-list { display: grid; gap: 12px; }
.message-card { display: flex; align-items: flex-start; gap: 12px; padding: 18px; border: 1px solid var(--color-border); border-radius: 14px; background: var(--color-surface); box-shadow: 0 8px 22px rgba(15, 75, 70, .04); }
.message-card.unread { border-color: rgba(20, 184, 166, .4); background: linear-gradient(120deg, rgba(20, 184, 166, .08), var(--color-surface)); }
.message-mark { width: 9px; height: 9px; flex: 0 0 auto; margin-top: 8px; border-radius: 50%; background: transparent; }
.unread .message-mark { background: var(--color-primary); box-shadow: 0 0 0 4px rgba(20, 184, 166, .13); }
.message-content { min-width: 0; flex: 1; }
.message-head { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.message-head h2 { margin: 0; color: var(--color-ink); font-size: 16px; }
.type-tag { padding: 3px 8px; border-radius: 999px; color: var(--color-primary-deep); background: var(--color-primary-soft); font-size: 12px; }
.message-content p { margin-bottom: 10px; line-height: 1.6; color: var(--color-ink-2); }
time, .read-label { color: var(--color-ink-3); font-size: 12px; white-space: nowrap; }
.read-btn { align-self: center; padding: 7px 10px; color: var(--color-primary-deep); background: transparent; white-space: nowrap; }
@media (max-width: 560px) { .page-hero { align-items: flex-start; flex-direction: column; } .message-card { padding: 14px; } .read-btn { font-size: 12px; } }
</style>
