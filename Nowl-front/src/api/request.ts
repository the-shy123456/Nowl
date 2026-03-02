import axios, {
  AxiosHeaders,
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { useUserStore } from '@/stores/user'
import { showUnauthorized, showError } from '@/utils/modal'
import { refreshToken as refreshTokenApi } from '@/api/modules/user'
import { AUTH_API } from '@/config/apiPaths'
import { normalizeMediaData } from '@/utils/media'

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '/api').trim()
const CSRF_COOKIE_NAME = 'csrf_token'
const CSRF_HEADER_NAME = 'X-CSRF-TOKEN'
const CSRF_PROTECTED_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

const readCookie = (name: string): string => {
  if (typeof document === 'undefined' || !name) {
    return ''
  }

  const cookiePairs = document.cookie ? document.cookie.split('; ') : []
  for (const pair of cookiePairs) {
    const index = pair.indexOf('=')
    if (index <= 0) {
      continue
    }
    const key = decodeURIComponent(pair.slice(0, index))
    if (key !== name) {
      continue
    }
    return decodeURIComponent(pair.slice(index + 1))
  }
  return ''
}

// 创建axios实例
const service: AxiosInstance = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    'X-Requested-With': 'XMLHttpRequest',
  },
  withCredentials: true, // 重要：允许携带Cookie（包括HttpOnly Cookie）
})

// Token刷新状态管理
let isRefreshing = false
let requestsQueue: Array<{
  resolve: () => void
  reject: (error: Error) => void
}> = []

type BusinessError = Error & {
  code?: number
  isBusinessError?: boolean
  config?: AxiosRequestConfig
  response?: AxiosResponse
}

type RetryableRequestConfig = AxiosRequestConfig & {
  _retry?: boolean
}

/**
 * 处理Token刷新
 * 从HttpOnly Cookie读取Refresh Token（浏览器自动发送）
 */
const handleTokenRefresh = async (): Promise<void> => {
  try {
    // 刷新成功后由后端Set-Cookie写入新access_token/refresh_token
    await refreshTokenApi()
  } catch (error) {
    // 刷新失败，需要重新登录
    throw error
  }
}

/**
 * 将请求加入队列等待Token刷新
 */
const addToQueue = (): Promise<void> => {
  return new Promise((resolve, reject) => {
    requestsQueue.push({ resolve, reject })
  })
}

/**
 * 处理队列中的所有请求
 */
const processQueue = (success: boolean, error?: Error): void => {
  requestsQueue.forEach(({ resolve, reject }) => {
    if (success) {
      resolve()
    } else if (error) {
      reject(error)
    }
  })
  requestsQueue = []
}

/**
 * 判断当前请求是否为刷新Token接口
 */
const isRefreshRequest = (config?: AxiosRequestConfig): boolean => {
  const url = config?.url
  if (!url || typeof url !== 'string') {
    return false
  }
  return url.includes(AUTH_API.REFRESH) || url.endsWith('/auth/refresh-token')
}

const AUTH_PAGE_PATHS = ['/login', '/register', '/forgot-password']

const isAuthPage = (): boolean => {
  if (typeof window === 'undefined') {
    return false
  }
  const pathname = window.location.pathname
  return AUTH_PAGE_PATHS.some((path) => pathname === path || pathname.startsWith(`${path}/`))
}

const redirectToLoginIfNeeded = () => {
  if (typeof window === 'undefined' || isAuthPage()) {
    return
  }
  window.location.href = '/login'
}

/**
 * 统一处理未登录/令牌过期（兼容HTTP 401和业务码401）
 */
const handleUnauthorized = async (error: BusinessError) => {
  const originalRequest = (error.config || error.response?.config) as RetryableRequestConfig | undefined

  if (!originalRequest) {
    return Promise.reject(error)
  }

  // 刷新接口本身失败，或者同一个请求重试过仍失败：直接登录失效处理
  if (isRefreshRequest(originalRequest) || originalRequest._retry) {
    const userStore = useUserStore()
    await userStore.logout({ notifyServer: false })

    if (!isAuthPage()) {
      const confirmed = await showUnauthorized('登录已过期，请重新登录')
      if (confirmed) {
        redirectToLoginIfNeeded()
      }
    }
    return Promise.reject(error)
  }

  // 如果已有刷新任务，当前请求排队等待
  if (isRefreshing) {
    return addToQueue().then(() => {
      const retryRequest: RetryableRequestConfig = {
        ...originalRequest,
        _retry: true,
      }
      return service(retryRequest)
    })
  }

  isRefreshing = true
  try {
    await handleTokenRefresh()
    processQueue(true)

    const retryRequest: RetryableRequestConfig = {
      ...originalRequest,
      _retry: true,
    }
    return service(retryRequest)
  } catch (refreshError) {
    processQueue(false, refreshError as Error)

    const userStore = useUserStore()
    await userStore.logout({ notifyServer: false })

    if (!isAuthPage()) {
      const confirmed = await showUnauthorized('登录已过期，请重新登录')
      if (confirmed) {
        redirectToLoginIfNeeded()
      }
    }
    return Promise.reject(refreshError)
  } finally {
    isRefreshing = false
  }
}

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 认证完全依赖HttpOnly Cookie，避免在前端可读区域保存token。
    const method = String(config.method || 'GET').toUpperCase()
    if (CSRF_PROTECTED_METHODS.has(method)) {
      const csrfToken = readCookie(CSRF_COOKIE_NAME)
      if (csrfToken) {
        const headers = AxiosHeaders.from(config.headers)
        headers.set(CSRF_HEADER_NAME, csrfToken)
        config.headers = headers
      }
    }
    return config
  },
  (error) => {
    console.error('请求错误：', error)
    return Promise.reject(error)
  },
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data

    // 如果返回的状态码不是200，则判断为错误
    if (res.code !== 200) {
      // 创建错误对象，包含后端返回的错误信息
      const error = new Error(res.message || '请求失败') as BusinessError
      error.code = res.code
      error.isBusinessError = true // 标记为业务错误
      error.config = response.config
      error.response = response

      // 注意：这里是响应成功分支，reject 后不会进入同一个拦截器的 onRejected
      // 所以业务码 401 必须在这里直接触发续期逻辑
      if (Number(error.code) === 401) {
        return handleUnauthorized(error)
      }

      // 刷新接口本身返回业务错误（如1008）时，不弹通用错误，由上层续期流程接管
      if (isRefreshRequest(response.config)) {
        return Promise.reject(error)
      }

      showError(error.message, '操作失败')
      return Promise.reject(error)
    }

    // 统一规范化媒体URL后返回 data 字段，简化调用方使用
    return normalizeMediaData(res.data)
  },
  async (error) => {
    console.error('响应错误：', error)

    if (error.response) {
      if (error.response.status === 401) {
        return handleUnauthorized(error as BusinessError)
      }

      // 尝试从后端响应中提取错误信息
      const backendMessage = error.response.data?.message

      switch (error.response.status) {
        case 403:
          // 优先使用后端返回的错误信息
          showError(backendMessage || '您没有权限访问该资源', '权限不足')
          break
        case 404:
          showError(backendMessage || '请求的资源不存在', '找不到资源')
          break
        case 500:
          showError(backendMessage || '服务器开小差了，请稍后重试', '服务器错误')
          break
        default:
          // 优先使用后端返回的错误信息
          showError(backendMessage || `请求失败 (${error.response.status})`, '网络错误')
      }
    } else if (error.message?.includes('timeout')) {
      showError('请求超时，请检查网络连接', '超时')
    } else if (error.message?.includes('Network Error')) {
      showError('网络连接失败，请检查网络设置', '网络错误')
    } else if (error.message) {
      // 其他错误（如业务逻辑错误）
      showError(error.message, '操作失败')
    }

    return Promise.reject(error)
  },
)

// 封装请求方法，直接返回 data 字段
export const request = {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.get(url, config)
  },

  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config)
  },

  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return service.put(url, data, config)
  },

  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, config)
  },
}

export default service
