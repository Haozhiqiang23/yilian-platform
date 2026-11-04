import http from '@/utils/request'

/**
 * 认证模块接口
 * 后端地址：/api/v1（开发期由 vite proxy 转发）
 */

/** 登录：返回 { token, userId, role, name } */
export function login(data) {
  return http.post('/auth/login', data)
}

/** 注册（患者实名注册，回传 codeId 防止验证码错配） */
export function register(data) {
  return http.post('/auth/register', data)
}

/** 获取注册验证码：返回 { codeId, ttlSeconds } */
export function getVerifyCode(data) {
  return http.post('/auth/verify-code', data)
}

/** 校验验证码 */
export function checkVerifyCode(data) {
  return http.post('/auth/verify-code/check', data)
}

/** 退出登录 */
export function logout() {
  return http.post('/auth/logout')
}