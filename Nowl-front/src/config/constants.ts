/**
 * 业务配置常量
 */
import { PAGE_CONSTANTS } from '@/constants'

// 分页配置
export const PAGINATION = {
  DEFAULT_PAGE_SIZE: PAGE_CONSTANTS.DEFAULT_PAGE_SIZE,
  DEFAULT_PAGE_NUM: PAGE_CONSTANTS.DEFAULT_PAGE_NUM,
} as const

// 搜索配置
export const SEARCH = {
  HOT_WORDS_COUNT: 10,
  SUGGESTION_MIN_LENGTH: 1,
  DEBOUNCE_DELAY: 500, // 防抖延迟（毫秒）
} as const

// 图片上传配置
export const IMAGE_UPLOAD = {
  MAX_COUNT: 9,
  MAX_SIZE: 10, // MB
  ALLOWED_TYPES: ['image/jpeg', 'image/png', 'image/gif', 'image/webp'],
} as const

// WebSocket 配置
export const WEBSOCKET = {
  HEARTBEAT_INTERVAL: 30000, // 心跳间隔 30秒
  RECONNECT_INTERVAL: 1000, // 初始重连间隔 1秒
  MAX_RECONNECT_ATTEMPTS: 5, // 最大重连次数
} as const

// 时间格式
export const DATE_FORMAT = {
  DATE_ONLY: 'YYYY-MM-DD',
  DATE_TIME: 'YYYY-MM-DD HH:mm:ss',
  TIME_ONLY: 'HH:mm:ss',
} as const

// API 超时配置
export const API = {
  TIMEOUT: 15000, // 请求超时 15秒
  RETRY_COUNT: 3, // 重试次数
} as const
