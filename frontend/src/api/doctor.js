import http from '@/utils/request'

/** 医生端接口 */

/** 查询当前登录医生的个人资料 */
export function getDoctorProfile() {
  return http.get('/doctor/profile')
}

/** 查询待接诊患者 */
export function getDoctorQueues() {
  return http.get('/doctor/queues')
}

/** 开始接诊 */
export function startAppointment(id) {
  return http.put(`/doctor/appointments/${id}/start`)
}

/** 完成接诊并生成诊疗记录 */
export function finishAppointment(id, data) {
  return http.put(`/doctor/appointments/${id}/finish`, data)
}

/** 查看患者历史记录 */
export function getPatientRecords(patientId) {
  return http.get(`/doctor/medical-records/${patientId}`)
}

/** 创建处方 */
export function createPrescription(data) {
  return http.post('/prescriptions', data)
}

/** 查询当前医生开具的处方 */
export function getDoctorPrescriptions(params = {}) {
  return http.get('/doctor/prescriptions', { params })
}

/** 创建检查申请 */
export function createExamination(data) {
  return http.post('/examinations', data)
}

/** 查询当前医生创建的检查申请 */
export function getDoctorExaminations(params = {}) {
  return http.get('/doctor/examinations', { params })
}

/** 确认患者提交的检查结果 */
export function confirmExaminationResult(id) {
  return http.put(`/doctor/examinations/${id}/confirm`)
}

/** 退回患者补充检查结果 */
export function rejectExaminationResult(id, data) {
  return http.put(`/doctor/examinations/${id}/reject`, data)
}

/** 查询检查报告 */
export function getExaminationReport(id) {
  return http.get(`/examinations/${id}/report`)
}
