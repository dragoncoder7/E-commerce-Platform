import axios from 'axios'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
const client = axios.create({ baseURL: '/api/store', timeout: 15000 })
client.interceptors.request.use(config => {
  const token = localStorage.getItem('store:token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
client.interceptors.response.use(response => {
  if (response.data.code !== 200) {
    if (response.data.code === 401 && !response.config.url?.endsWith('/login')) {
      localStorage.removeItem('store:token')
      window.dispatchEvent(new Event('store:unauthorized'))
    }
    ElMessage.error(response.data.message || '请求失败')
    return Promise.reject(new Error(response.data.message))
  }
  return response
}, error => { ElMessage.error('网络异常，请稍后重试'); return Promise.reject(error) })
export const api = {
  get: async <T>(url: string, params?: object): Promise<T> => (await client.get(url, { params })).data.data,
  post: async <T>(url: string, data?: unknown): Promise<T> => (await client.post(url, data)).data.data,
  put: async <T>(url: string, data?: unknown): Promise<T> => (await client.put(url, data)).data.data,
  delete: async <T>(url: string): Promise<T> => (await client.delete(url)).data.data,
}
export interface Page<T> { records: T[]; total: number; current: number; size: number }
