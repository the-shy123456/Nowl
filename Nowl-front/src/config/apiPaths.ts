/**
 * API路径常量定义
 * 集中管理所有后端API路径，便于维护和重构
 */

// 基础路径
const API_BASE = {
  AUTH: '/auth',
  USER: '/user',
  GOODS: '/goods',
  CATEGORY: '/category',
  ORDER: '/order',
  ERRAND: '/errand',
  CHAT: '/chat',
  DISPUTE: '/dispute',
  REVIEW: '/review',
  NOTICE: '/notice',
  SEARCH: '/search',
  RECOMMEND: '/recommend',
  FILE: '/file',
  ADMIN: '/admin',
  AI: '/ai',
} as const;

// 认证相关
export const AUTH_API = {
  LOGIN: `${API_BASE.AUTH}/login`,
  REGISTER: `${API_BASE.AUTH}/register`,
  LOGOUT: `${API_BASE.AUTH}/logout`,
  SEND_SMS: `${API_BASE.AUTH}/send-sms`,
  RESET_PASSWORD: `${API_BASE.AUTH}/reset-password`,
  CAPTCHA: `${API_BASE.AUTH}/captcha`,
  REFRESH: `${API_BASE.AUTH}/refresh-token`,
} as const;

// 用户相关
export const USER_API = {
  INFO: `${API_BASE.USER}/info`,
  UPDATE_INFO: `${API_BASE.USER}/info`,
  RUNNER_APPLY: `${API_BASE.USER}/runnable/apply`,
  GET_USER: (userId: number | string) => `${API_BASE.USER}/${userId}`,
  FOLLOW: (userId: number | string) => `${API_BASE.USER}/follow/${userId}`,
  UNFOLLOW: (userId: number | string) => `${API_BASE.USER}/follow/${userId}`,
  CHECK_FOLLOW: (userId: number | string) => `${API_BASE.USER}/follow/check/${userId}`,
  FOLLOWING: (userId: number | string) => `${API_BASE.USER}/${userId}/following`,
  FOLLOWERS: (userId: number | string) => `${API_BASE.USER}/${userId}/followers`,
} as const;

// 商品相关
export const GOODS_API = {
  LIST: API_BASE.GOODS,
  DETAIL: (productId: number | string) => `${API_BASE.GOODS}/${productId}`,
  PUBLISH: API_BASE.GOODS,
  UPDATE: (productId: number | string) => `${API_BASE.GOODS}/${productId}`,
  DELETE: (productId: number | string) => `${API_BASE.GOODS}/${productId}`,
  OFFSHELF: (productId: number | string) => `${API_BASE.GOODS}/${productId}/offshelf`,
  COLLECT: (productId: number | string) => `${API_BASE.GOODS}/${productId}/collect`,
  UNCOLLECT: (productId: number | string) => `${API_BASE.GOODS}/${productId}/collect`,
  MY_GOODS: `${API_BASE.GOODS}/my`,
  MY_COLLECTIONS: `${API_BASE.GOODS}/collections`,
} as const;

// 商品分类相关
export const CATEGORY_API = {
  TREE: `${API_BASE.CATEGORY}/tree`,
  LIST: `${API_BASE.CATEGORY}/list`,
} as const;

// 订单相关
export const ORDER_API = {
  CREATE: API_BASE.ORDER,
  DETAIL: (orderId: number | string) => `${API_BASE.ORDER}/${orderId}`,
  LIST: `${API_BASE.ORDER}/my`,
  CANCEL: (orderId: number | string) => `${API_BASE.ORDER}/${orderId}/cancel`,
  REFUND_APPLY: (orderId: number | string) => `${API_BASE.ORDER}/${orderId}/refund`,
  REFUND_PROCESS: (orderId: number | string) => `${API_BASE.ORDER}/${orderId}/refund/process`,
  CONFIRM: (orderId: number | string) => `${API_BASE.ORDER}/${orderId}/confirm`,
  PAY: (orderId: number | string) => `${API_BASE.ORDER}/${orderId}/pay`,
  DELIVER: (orderId: number | string) => `${API_BASE.ORDER}/${orderId}/deliver`,
  DISPUTE: `${API_BASE.ORDER}/dispute`,
} as const;

// 跑腿相关
export const ERRAND_API = {
  LIST: `${API_BASE.ERRAND}/list`,
  DETAIL: (taskId: number | string) => `${API_BASE.ERRAND}/${taskId}`,
  PUBLISH: API_BASE.ERRAND,
  UPDATE: (taskId: number | string) => `${API_BASE.ERRAND}/${taskId}`,
  DELETE: (taskId: number | string) => `${API_BASE.ERRAND}/${taskId}`,
  ACCEPT: (taskId: number | string) => `${API_BASE.ERRAND}/${taskId}/accept`,
  CONFIRM: (taskId: number | string) => `${API_BASE.ERRAND}/${taskId}/confirm`,
  MY_PUBLISHED: `${API_BASE.ERRAND}/my/published`,
  MY_ACCEPTED: `${API_BASE.ERRAND}/my/accepted`,
} as const;

// 聊天相关
export const CHAT_API = {
  SEND: `${API_BASE.CHAT}/send`,
  CONTACTS: `${API_BASE.CHAT}/contacts`,
  HISTORY: (contactId: number | string) => `${API_BASE.CHAT}/history/${contactId}`,
  READ: (contactId: number | string) => `${API_BASE.CHAT}/read/${contactId}`,
  READ_ALL: `${API_BASE.CHAT}/read/all`,
  BLOCK: (targetUserId: number | string) => `${API_BASE.CHAT}/block/${targetUserId}`,
  BLOCKS: `${API_BASE.CHAT}/blocks`,
  BLOCK_RELATION: (targetUserId: number | string) => `${API_BASE.CHAT}/block/relation/${targetUserId}`,
} as const;

// 纠纷相关
export const DISPUTE_API = {
  LIST: `${API_BASE.DISPUTE}/list`,
  DETAIL: (disputeId: number | string) => `${API_BASE.DISPUTE}/${disputeId}`,
  CREATE: API_BASE.DISPUTE,
  WITHDRAW: (disputeId: number | string) => `${API_BASE.DISPUTE}/${disputeId}/withdraw`,
  EVIDENCE: `${API_BASE.DISPUTE}/evidence`,
} as const;

// 评价相关
export const REVIEW_API = {
  CREATE: API_BASE.REVIEW,
  RECEIVED: (userId: number | string) => `${API_BASE.REVIEW}/received/${userId}`,
  SENT: `${API_BASE.REVIEW}/sent`,
  STATS: (userId: number | string) => `${API_BASE.REVIEW}/stats/${userId}`,
  CAN_REVIEW: `${API_BASE.REVIEW}/can-review`,
  HAS_REVIEWED: `${API_BASE.REVIEW}/has-reviewed`,
} as const;

// 通知相关
export const NOTICE_API = {
  LIST: `${API_BASE.NOTICE}/list`,
  READ: (noticeId: number | string) => `${API_BASE.NOTICE}/read/${noticeId}`,
  READ_ALL: `${API_BASE.NOTICE}/read/all`,
  UNREAD_COUNT: `${API_BASE.NOTICE}/unread/count`,
} as const;

// 搜索相关
export const SEARCH_API = {
  GOODS: `${API_BASE.SEARCH}/goods`,
  ERRAND: `${API_BASE.SEARCH}/errand`,
  HOT_WORDS: `${API_BASE.SEARCH}/hot`,
  HISTORY: `${API_BASE.SEARCH}/history`,
  SUGGESTIONS: `${API_BASE.SEARCH}/suggest`,
  CLEAR_HISTORY: `${API_BASE.SEARCH}/history`,
  SYNC_FULL: `${API_BASE.SEARCH}/sync/full`,
  INDEX_CREATE: `${API_BASE.SEARCH}/index/create`,
  ERRAND_SYNC_FULL: `${API_BASE.SEARCH}/errand/sync/full`,
  ERRAND_INDEX_CREATE: `${API_BASE.SEARCH}/errand/index/create`,
} as const;

// 推荐相关
export const RECOMMEND_API = {
  HOME: `${API_BASE.RECOMMEND}/home`,
  HOT: `${API_BASE.RECOMMEND}/hot`,
  SIMILAR: (productId: number | string) => `${API_BASE.RECOMMEND}/similar/${productId}`,
  FOLLOWING: `${API_BASE.RECOMMEND}/following`,
  VIEW_BEHAVIOR: `${API_BASE.RECOMMEND}/behavior/view`,
} as const;

// 文件相关
export const FILE_API = {
  UPLOAD: `${API_BASE.FILE}/upload`,
  UPLOAD_BATCH: `${API_BASE.FILE}/upload/batch`,
  DELETE: `${API_BASE.FILE}/delete`,
} as const;

// 管理后台相关
export const ADMIN_API = {
  STATS: `${API_BASE.ADMIN}/dashboard/stats`,
  GOODS_PENDING: `${API_BASE.ADMIN}/goods/pending`,
  GOODS_LIST: `${API_BASE.ADMIN}/goods/list`,
  GOODS_AUDIT: `${API_BASE.ADMIN}/goods/audit`,
  GOODS_OFFLINE: `${API_BASE.ADMIN}/goods/offline`,
  USER_LIST: `${API_BASE.ADMIN}/user/list`,
  USER_STATUS: `${API_BASE.ADMIN}/user/status`,
  AUTH_PENDING: `${API_BASE.ADMIN}/auth/pending`,
  AUTH_AUDIT: `${API_BASE.ADMIN}/auth/audit`,
  RUNNER_PENDING: `${API_BASE.ADMIN}/runner/pending`,
  RUNNER_AUDIT: `${API_BASE.ADMIN}/runner/audit`,
  ORDER_LIST: `${API_BASE.ADMIN}/order/list`,
  ERRAND_LIST: `${API_BASE.ADMIN}/errand/list`,
  ERRAND_AUDIT: `${API_BASE.ADMIN}/errand/audit`,
  DISPUTE_LIST: `${API_BASE.ADMIN}/dispute/list`,
  DISPUTE_HANDLE: `${API_BASE.ADMIN}/dispute/handle`,
  RISK_EVENTS: `${API_BASE.ADMIN}/risk/events`,
  RISK_CASES: `${API_BASE.ADMIN}/risk/cases`,
  RISK_CASE_HANDLE: `${API_BASE.ADMIN}/risk/case/handle`,
  RISK_RULES: `${API_BASE.ADMIN}/risk/rules`,
  RISK_RULE: `${API_BASE.ADMIN}/risk/rule`,
  RISK_RULE_STATUS: (ruleId: number | string) => `${API_BASE.ADMIN}/risk/rule/${ruleId}/status`,
  RISK_BEHAVIOR_CONTROLS: (userId: number | string) => `${API_BASE.ADMIN}/risk/behavior-control/${userId}`,
  RISK_BEHAVIOR_CONTROL: `${API_BASE.ADMIN}/risk/behavior-control`,
  IAM_ROLES: `${API_BASE.ADMIN}/iam/roles`,
  IAM_USER_ROLES: (userId: number | string) => `${API_BASE.ADMIN}/iam/user-roles/${userId}`,
  IAM_GRANT_USER_ROLE: `${API_BASE.ADMIN}/iam/user-role/grant`,
  IAM_REVOKE_USER_ROLE: (bindingId: number | string) => `${API_BASE.ADMIN}/iam/user-role/${bindingId}`,
  IAM_SCOPES: (userId: number | string) => `${API_BASE.ADMIN}/iam/scopes/${userId}`,
  IAM_SCOPE: `${API_BASE.ADMIN}/iam/scope`,
  IAM_DISABLE_SCOPE: (bindingId: number | string) => `${API_BASE.ADMIN}/iam/scope/${bindingId}`,
  AUDIT_OPERATIONS: `${API_BASE.ADMIN}/audit/operations`,
  AUDIT_PERMISSION_CHANGES: `${API_BASE.ADMIN}/audit/permission-changes`,
  AUDIT_LOGIN_TRACES: `${API_BASE.ADMIN}/audit/login-traces`,
  AUDIT_OVERVIEW: `${API_BASE.ADMIN}/audit/overview`,
  USER_DETAIL: (userId: number | string) => `${API_BASE.ADMIN}/user/${userId}`,
  USER_CREDIT: `${API_BASE.ADMIN}/user/credit`,
  NOTICE_BROADCAST: `${API_BASE.ADMIN}/notice/broadcast`,
} as const;

// AI相关
export const AI_API = {
  CHAT: `${API_BASE.AI}/chat`,
  HISTORY: `${API_BASE.AI}/history`,
  PRICE_ESTIMATE: `${API_BASE.AI}/price-estimate`,
} as const;

// 导出所有API常量
export const API_PATHS = {
  ...AUTH_API,
  ...USER_API,
  ...GOODS_API,
  ...CATEGORY_API,
  ...ORDER_API,
  ...ERRAND_API,
  ...CHAT_API,
  ...DISPUTE_API,
  ...REVIEW_API,
  ...NOTICE_API,
  ...SEARCH_API,
  ...RECOMMEND_API,
  ...FILE_API,
  ...ADMIN_API,
  ...AI_API,
} as const;

export default API_PATHS;
