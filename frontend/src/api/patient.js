import http from '@/utils/request'

/** 患者相关接口 */

/** 获取患者端可感知的功能开关 */
export function getPatientConfig() {
  return http.get('/patients/config')
}

/** 获取患者个人信息 */
export function getProfile() {
  return http.get('/patients/profile')
}

/** 修改个人信息（手机号/地址）—— 接口清单 C1 */
export function updateProfile(data) {
  return http.put('/patients/profile', data)
}

/** 医生列表：departmentId / name / date 筛选（对应《API设计文档》/doctors） */
export function getDoctors(params) {
  return http.get('/doctors', { params })
}

/** 医生详情 */
export function getDoctor(id) {
  return http.get(`/doctors/${id}`)
}

/** 号源查询：doctorId / date */
export function getSources(params) {
  return http.get('/sources', { params })
}

/** 创建预约：{ sourceId } */
export function createAppointment(data) {
  return http.post('/appointments', data)
}

/** 我的预约列表 */
export function getMyAppointments(params) {
  return http.get('/appointments/my', { params })
}

/** 查询当前患者的预约详情 */
export function getAppointmentDetail(id) {
  return http.get(`/appointments/${id}`)
}

/** 取消预约（待支付可取消） */
export function cancelAppointment(id) {
  return http.put(`/appointments/${id}/cancel`)
}

/** 超过自助取消时限后申请管理员人工处理退款 */
export function contactAdminRefund(id, data) {
  return http.post(`/appointments/${id}/contact-refund`, data)
}

/** 创建支付订单：{ appointmentId } */
export function createPayment(data) {
  return http.post('/payments', data)
}

/** 模拟支付 */
export function pay(id) {
  return http.post(`/payments/${id}/pay`)
}

/** 查询支付订单 */
export function getPayment(id) {
  return http.get(`/payments/${id}`)
}

/** 查询预约已有的支付订单 */
export function getPaymentByAppointment(appointmentId) {
  return http.get(`/payments/by-appointment/${appointmentId}`)
}

/** 签到 */
export function signIn(data) {
  return http.post('/sign-ins', data)
}

/** 查询排队状态 */
export function getQueue(appointmentId) {
  return http.get(`/queues/${appointmentId}`)
}

/** 我的诊疗记录（分页） */
export function getMyRecords(params) {
  return http.get('/medical-records/my', { params })
}

/** 处方详情 */
export function getPrescription(id) {
  return http.get(`/prescriptions/${id}`)
}

/** 检查报告 */
export function getReport(id) {
  return http.get(`/examinations/${id}/report`)
}

/** 查询当前患者的检查申请 */
export function getMyExaminations() {
  return http.get('/examinations/my')
}

/** 提交线下取得的检查结果 */
export function submitExaminationResult(id, data) {
  return http.put(`/examinations/${id}/patient-result`, data)
}
