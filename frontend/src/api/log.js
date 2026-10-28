import request from './request'

// I7 操作日志查询（startTime/endTime 格式：yyyy-MM-dd HH:mm:ss）
export function listLogs(params) {
  return request.get('/recipe-logs', { params })
}
