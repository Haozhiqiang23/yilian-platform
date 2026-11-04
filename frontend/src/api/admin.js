import http from '@/utils/request'

/** 管理员端接口 */

/** 用户列表查询 */
export function getAdminUsers(params) {
  return http.get('/admin/users', { params })
}

/** 禁用/启用用户 */
export function updateUserStatus(id, data) {
  return http.put(`/admin/users/${id}/status`, data)
}

/** 查询待审核医生 */
export function getDoctorAudits(params) {
  return http.get('/admin/doctors/audit', { params })
}

/** 查询全部医生 */
export function getAdminDoctors(params) {
  return http.get('/admin/doctors', { params })
}

/** 查询医生管理详情 */
export function getAdminDoctorDetail(id) {
  return http.get(`/admin/doctors/${id}`)
}

/** 查询医生审核和状态操作历史 */
export function getAdminDoctorHistory(id, params) {
  return http.get(`/admin/doctors/${id}/history`, { params })
}

/** 审核医生 */
export function auditDoctor(id, data) {
  return http.put(`/admin/doctors/${id}/audit`, data)
}

/** 禁用/恢复医生账号 */
export function updateDoctorStatus(id, data) {
  return http.put(`/admin/doctors/${id}/status`, data)
}

/** 查询号源 */
export function getAdminSources(params) {
  return http.get('/admin/sources', { params })
}

/** 新建号源 */
export function createAdminSource(data) {
  return http.post('/admin/sources', data)
}

/** 调整号源 */
export function updateAdminSource(id, data) {
  return http.put(`/admin/sources/${id}`, data)
}

/** 查询停诊会影响的预约和退款数量 */
export function getSourceSuspensionPreview(id) {
  return http.get(`/admin/sources/${id}/suspension-preview`)
}

/** 停诊或恢复号源 */
export function updateSourceStatus(id, data) {
  return http.put(`/admin/sources/${id}/status`, data)
}

/** 查询退款 */
export function getRefunds(params) {
  return http.get('/admin/refunds', { params })
}

/** 处理退款 */
export function handleRefund(id, data) {
  return http.put(`/admin/refunds/${id}`, data)
}

/** 查询管理员队列 */
export function getAdminQueues(params) {
  return http.get('/admin/queues', { params })
}

/** 管理员异常调整等待队列顺序 */
export function adjustAdminQueue(queueId, data) {
  return http.put(`/admin/queues/${queueId}`, data)
}

/** 数据统计 */
export function getStatistics() {
  return http.get('/admin/statistics')
}

/** 操作日志 */
export function getAdminLogs(params) {
  return http.get('/admin/logs', { params })
}

/** 查询单条操作日志详情 */
export function getAdminLogDetail(id) {
  return http.get(`/admin/logs/${id}`)
}

/** 读取平台业务参数 */
export function getSystemConfig() {
  return http.get('/admin/config')
}

/** 保存平台业务参数 */
export function updateSystemConfig(data) {
  return http.put('/admin/config', data)
}
