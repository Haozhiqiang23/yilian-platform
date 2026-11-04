import axios from 'axios'

/**
 * Axios 实例：统一 baseURL、超时与拦截器
 * - 响应拦截：统一解包 { code, message, data }
 * - 401：清会话跳登录；403：提示无权限
 */
const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

// 请求拦截：自动附带 token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('yilian_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：业务码统一处理
http.interceptors.response.use(
  (response) => {
    const body = response.data
    // 后端统一返回 { code, message, data }；code === 0 视为成功
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0 || body.code === 200) return body.data
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      localStorage.removeItem('yilian_token')
      localStorage.removeItem('yilian_user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    } else if (status === 403) {
      // 骨架阶段先 console，后续接入全局提示
      console.warn('无权限访问', error.response?.data?.message)
    }
    const msg = error.response?.data?.message || '网络异常，请稍后重试'
    return Promise.reject(new Error(msg))
  },
)

export default http