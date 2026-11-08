/**
 * 统一请求封装（参考 vue-element-admin / pure-admin 等成熟方案的通用范式）
 *
 * 能力：
 * 1. baseURL / 超时 / 请求头统一注入，自动携带 Bearer Token
 * 2. 重复请求自动取消（相同 method + url + params + data）
 * 3. 业务码统一判定：0 / 200 视为成功并解包 data
 * 4. 401 统一登出并跳转登录页，其余错误码统一 ElMessage 提示
 * 5. 网络异常 / 超时 / 5xx 兜底文案，可单次请求关闭自动提示
 * 6. 支持 blob 下载
 *
 * 后端联调时只需调整 SUCCESS_CODES 与 401 判定，即可直接对接。
 */
import axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
} from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from './auth'
import type { ApiResult } from '@/types/api'

declare module 'axios' {
  interface AxiosRequestConfig {
    /** 出现错误时是否自动弹提示，默认 true */
    showError?: boolean
    /** 是否允许重复请求，默认 false（后发起的会取消先发起的） */
    allowRepeat?: boolean
    /** 是否自动携带 token，默认 true */
    withToken?: boolean
  }
}

/** 业务成功码 */
const SUCCESS_CODES = [0, 200]
/** 登录失效业务码 */
const UNAUTHORIZED_CODE = 401

const STATUS_MESSAGE: Record<number, string> = {
  400: '请求参数有误',
  401: '登录状态已失效，请重新登录',
  403: '没有访问权限，请联系管理员',
  404: '请求的资源不存在',
  405: '请求方法不被允许',
  408: '请求超时',
  409: '数据已存在或状态冲突',
  413: '上传内容过大',
  422: '数据校验未通过',
  429: '请求过于频繁，请稍后再试',
  500: '服务器内部错误',
  501: '服务未实现',
  502: '网关异常',
  503: '服务暂不可用',
  504: '网关超时',
}

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 20_000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
})

/* -------------------------------------------------------------------------- */
/*                                 重复请求处理                                */
/* -------------------------------------------------------------------------- */

const pendingMap = new Map<string, AbortController>()

function safeStringify(value: unknown): string {
  if (value === null || value === undefined) return ''
  if (typeof value === 'string') return value
  try {
    return JSON.stringify(value)
  } catch {
    return String(value)
  }
}

function requestKey(config: AxiosRequestConfig): string {
  const method = (config.method ?? 'get').toLowerCase()
  return [method, config.url ?? '', safeStringify(config.params), safeStringify(config.data)].join('|')
}

function addPending(config: AxiosRequestConfig): void {
  if (config.allowRepeat) return
  const key = requestKey(config)
  const previous = pendingMap.get(key)
  if (previous) previous.abort()
  const controller = new AbortController()
  config.signal = controller.signal
  pendingMap.set(key, controller)
}

function removePending(config: AxiosRequestConfig): void {
  const key = requestKey(config)
  if (pendingMap.has(key)) pendingMap.delete(key)
}

/* -------------------------------------------------------------------------- */
/*                                 登录失效处理                                */
/* -------------------------------------------------------------------------- */

let redirecting = false

function handleUnauthorized(): void {
  removeToken()
  if (redirecting) return
  redirecting = true
  ElMessage.warning('登录状态已失效，请重新登录')
  const { pathname, search, hash } = window.location
  const current = `${pathname}${search}${hash}`
  const redirect = current.startsWith('/login') ? '' : `?redirect=${encodeURIComponent(current)}`
  // 使用整页跳转，彻底清空 store / 已注册的动态路由
  window.setTimeout(() => window.location.replace(`/login${redirect}`), 300)
}

function isApiResult(payload: unknown): payload is ApiResult {
  return typeof payload === 'object' && payload !== null && 'code' in payload
}

function pickMessage(payload: Partial<ApiResult> | undefined, fallback: string): string {
  return payload?.message || payload?.msg || fallback
}

/** 拦截器内部已解包，此处仅用于抹平 axios 的类型约束 */
function unwrap<T>(data: T): AxiosResponse<T> {
  return data as unknown as AxiosResponse<T>
}

/* -------------------------------------------------------------------------- */
/*                                    拦截器                                   */
/* -------------------------------------------------------------------------- */

instance.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token && config.withToken !== false) {
      config.headers.set('Authorization', `Bearer ${token}`)
    }
    addPending(config)
    return config
  },
  (error) => Promise.reject(error),
)

instance.interceptors.response.use(
  (response: AxiosResponse) => {
    const { config } = response
    removePending(config)

    // 文件流直接返回原始响应
    if (config.responseType === 'blob' || config.responseType === 'arraybuffer') {
      return response
    }

    const payload: unknown = response.data
    // 非标准结构（第三方接口、静态资源等）原样返回
    if (!isApiResult(payload)) return response

    if (SUCCESS_CODES.includes(Number(payload.code))) {
      return unwrap(payload.data)
    }

    const message = pickMessage(payload, '请求处理失败')

    if (Number(payload.code) === UNAUTHORIZED_CODE) {
      handleUnauthorized()
      return Promise.reject(new Error(message))
    }

    if (config.showError !== false) ElMessage.error(message)
    return Promise.reject(new Error(message))
  },
  (error: AxiosError<ApiResult>) => {
    const config = error.config as AxiosRequestConfig | undefined
    if (config) removePending(config)

    // 主动取消的请求不提示
    if (axios.isCancel(error) || error.code === 'ERR_CANCELED') {
      return Promise.reject(error)
    }

    const status = error.response?.status
    const serverPayload = error.response?.data
    let message: string

    if (!error.response) {
      message =
        error.code === 'ECONNABORTED' || /timeout/i.test(error.message)
          ? '请求超时，请稍后重试'
          : '网络连接异常，请检查网络后重试'
    } else {
      message = pickMessage(serverPayload, STATUS_MESSAGE[status ?? 0] || `请求失败（HTTP ${status}）`)
    }

    if (status === 401) {
      handleUnauthorized()
      return Promise.reject(new Error(message))
    }

    if (config?.showError !== false) ElMessage.error(message)
    return Promise.reject(new Error(message))
  },
)

/* -------------------------------------------------------------------------- */
/*                                 对外方法                                   */
/* -------------------------------------------------------------------------- */

/**
 * 统一出口：拦截器已把 { code, message, data } 解包成 data，
 * 这里把返回值断言为业务数据类型（兼容 axios 新版的泛型包装）
 */
function request<T>(config: AxiosRequestConfig): Promise<T> {
  return instance.request<ApiResult<T>, T>(config) as unknown as Promise<T>
}

export const http = {
  request,

  get<T = unknown>(
    url: string,
    params?: Record<string, unknown>,
    config?: AxiosRequestConfig,
  ): Promise<T> {
    return request<T>({ url, method: 'GET', params, ...config })
  },

  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ url, method: 'POST', data, ...config })
  },

  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ url, method: 'PUT', data, ...config })
  },

  patch<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ url, method: 'PATCH', data, ...config })
  },

  delete<T = unknown>(
    url: string,
    params?: Record<string, unknown>,
    config?: AxiosRequestConfig,
  ): Promise<T> {
    return request<T>({ url, method: 'DELETE', params, ...config })
  },

  /** 通用下载：后端返回文件流时使用 */
  async download(
    url: string,
    params?: Record<string, unknown>,
    filename = '',
  ): Promise<void> {
    const response = await instance.request<Blob, AxiosResponse<Blob>>({
      url,
      method: 'GET',
      params,
      responseType: 'blob',
      timeout: 60_000,
    })
    const blob = response.data
    const link = document.createElement('a')
    const objectUrl = URL.createObjectURL(blob)
    link.href = objectUrl
    link.download = filename || `download-${Date.now()}`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(objectUrl)
  },
}

export { instance as httpInstance }
export default http
