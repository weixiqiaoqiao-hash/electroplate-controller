import request from './request'

// I8 字典列表（type：CATEGORY / SPEC）
export function listDicts(type) {
  return request.get('/dicts', { params: { type } })
}

// I9 新增字典项
export function addDict(data) {
  return request.post('/dicts', data)
}

// I10 删除字典项
export function deleteDict(id) {
  return request.delete(`/dicts/${id}`)
}
