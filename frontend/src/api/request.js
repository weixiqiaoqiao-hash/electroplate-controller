import axios from 'axios'
import { ElMessage } from 'element-plus'

// 统一业务错误对象：调用方可通过 code 区分场景（如 40901 范围重叠）
export class ApiError extends Error {
  constructor(code, message, data) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.data = data
  }
}

const instance = axios.create({
  baseURL: '/api',
  timeout: 15000
})

instance.interceptors.response.use(
  (response) => {
    const body = response.data
    // 204 无响应体（如删除字典）视为成功
    if (response.status === 204 || body === null || body === undefined || body === '') {
      return { code: 0, message: 'success', data: null }
    }
    if (body && body.code === 0) return body
    const message = (body && body.message) || '操作失败'
    ElMessage.error(message)
    return Promise.reject(new ApiError(body ? body.code : -1, message, body && body.data))
  },
  (error) => {
    const body = error.response && error.response.data
    const message =
      (body && body.message) ||
      (error.response ? `请求失败（HTTP ${error.response.status}）` : '无法连接后端服务，请确认服务已启动')
    ElMessage.error(message)
    return Promise.reject(new ApiError((body && body.code) || -1, message, body && body.data))
  }
)

export default instance
