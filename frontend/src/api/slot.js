import request from './request'

// J1 槽位列表（status：ENABLED / DISABLED / ALL，默认 ENABLED；全量不分页）
export function listSlots(status) {
  return request.get('/slots', { params: { status } })
}

// J2 新增槽位
export function createSlot(data) {
  return request.post('/slots', data)
}

// J3 修改槽位配置（data 携带 version）
export function updateSlot(id, data) {
  return request.put(`/slots/${id}`, data)
}

// J4 删除槽位（物理删除，名称与序号即释放），成功 204
export function deleteSlot(id) {
  return request.delete(`/slots/${id}`)
}

// J5 批量保存时间（时间设定模式的统一保存）
export function updateSlotTimes(items) {
  return request.put('/slots/times', { items })
}

// J6 镀槽开关即时切换
export function switchSlot(id, switchOn) {
  return request.put(`/slots/${id}/switch`, { switchOn })
}
