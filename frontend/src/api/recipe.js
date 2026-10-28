import request from './request'

// I1 分页查询配方（status 默认 ENABLED）
export function listRecipes(params) {
  return request.get('/recipes', { params })
}

// I2 配方详情
export function getRecipe(id) {
  return request.get(`/recipes/${id}`)
}

// I3 新增配方
export function createRecipe(data) {
  return request.post('/recipes', data)
}

// I4 修改配方（data 携带 version）
export function updateRecipe(id, data) {
  return request.put(`/recipes/${id}`, data)
}

// I5 停用/启用
export function changeStatus(id, status) {
  return request.put(`/recipes/${id}/status`, { status })
}

// I6 匹配查询（供扫码上料调用）
export function matchRecipe(params) {
  return request.get('/recipes/match', { params })
}
