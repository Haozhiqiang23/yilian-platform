import http from '@/utils/request'

/** 患者站内消息接口 */

/** 查询当前患者消息列表 */
export function getMessages(params) {
  return http.get('/messages', { params })
}

/** 查询当前患者未读消息数量 */
export function getUnreadMessageCount() {
  return http.get('/messages/unread-count')
}

/** 查询消息详情 */
export function getMessage(id) {
  return http.get(`/messages/${id}`)
}

/** 标记单条消息已读 */
export function markMessageRead(id) {
  return http.put(`/messages/${id}/read`)
}

/** 标记全部消息已读 */
export function markAllMessagesRead() {
  return http.put('/messages/read-all')
}
