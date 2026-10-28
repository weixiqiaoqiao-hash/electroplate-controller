import request from './request'

// K1 分页查询生产记录（feedTimeStart/feedTimeEnd/batchNo/rackNo 均可空）
export function listProdRecords(params) {
  return request.get('/prod-records', { params })
}

// K3 新增生产记录（扫码上料「发送」预留，本期页面无入口）
export function createProdRecord(data) {
  return request.post('/prod-records', data)
}
