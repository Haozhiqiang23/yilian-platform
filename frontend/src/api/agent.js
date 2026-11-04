import axios from 'axios'

const agentHttp = axios.create({
  baseURL: '/agent',
  // 当前 Agent 为非流式返回，需要等待千问完整生成回答后再响应。
  timeout: 180000,
})

agentHttp.interceptors.request.use((config) => {
  const token = localStorage.getItem('yilian_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

agentHttp.interceptors.response.use(
  (response) => {
    const body = response.data

    // Python Agent 与 Java 后端保持统一响应外层：{ code, message, data }。
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 200 && body.code !== 0) {
        return Promise.reject(new Error(body.message || '智能助手请求失败'))
      }

      const data = body.data || {}
      // 当前基础聊天接口返回 data.content；组件原有协议使用 message。
      return {
        ...data,
        message: data.message || data.content || '',
        type: data.type || 'TEXT',
      }
    }

    return body
  },
  (error) => {
    if (error.code === 'ECONNABORTED') {
      return Promise.reject(new Error('回答生成时间较长，请稍后重试'))
    }
    const message = error.response?.data?.message || '智能助手服务暂时不可用，请稍后重试'
    return Promise.reject(new Error(message))
  },
)

/**
 * 调用医联小智 Agent。
 * 认证信息由 request 拦截器自动从当前登录会话中附带。
 */
export function chatWithAssistant(data) {
  return agentHttp.post('/chat', data)
}

function normalizeAssistantResponse(data = {}) {
  return {
    ...data,
    message: data.message || data.content || '',
    type: data.type || 'TEXT',
  }
}

/**
 * SSE 流式输出：使用 fetch 读取 POST 响应体中的连续事件。
 * 原生 EventSource 只能方便地发送 GET，无法附带当前项目需要的请求体和 JWT，
 * 因此这里使用 fetch + ReadableStream，同时仍遵循 SSE 的事件格式。
 */
export async function chatWithAssistantStream(data, { onToken } = {}) {
  const token = localStorage.getItem('yilian_token')
  const response = await fetch('/agent/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(data),
  })

  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.message || '智能助手服务暂时不可用，请稍后重试')
  }
  if (!response.body) {
    throw new Error('当前浏览器无法读取流式回答')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let finalResponse = null

  function handleEvent(block) {
    let eventName = 'message'
    const dataLines = []

    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) eventName = line.slice(6).trim()
      if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart())
    }

    if (dataLines.length === 0) return
    const eventData = JSON.parse(dataLines.join('\n'))

    if (eventName === 'token') {
      onToken?.(eventData.content || '')
      return
    }
    if (eventName === 'done') {
      finalResponse = normalizeAssistantResponse(eventData)
      return
    }
    if (eventName === 'error') {
      throw new Error(eventData.message || '智能助手服务暂时不可用，请稍后重试')
    }
  }

  // SSE 流式输出：网络分块边界不一定等于事件边界，
  // 所以先累积字符串，再按照事件之间的空行逐个解析。
  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
    buffer = buffer.replaceAll('\r\n', '\n')

    let eventEnd = buffer.indexOf('\n\n')
    while (eventEnd !== -1) {
      const eventBlock = buffer.slice(0, eventEnd)
      buffer = buffer.slice(eventEnd + 2)
      if (eventBlock.trim()) handleEvent(eventBlock)
      eventEnd = buffer.indexOf('\n\n')
    }

    if (done) break
  }

  if (buffer.trim()) handleEvent(buffer)
  if (!finalResponse) throw new Error('智能助手未返回完整结果')
  return finalResponse
}

/**
 * 聊天历史恢复：读取当前登录患者指定会话的可见消息。
 */
export function getAssistantHistory(sessionId) {
  return agentHttp.get(`/chat/history/${encodeURIComponent(sessionId)}`)
}
