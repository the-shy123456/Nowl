/**
 * 全局常量定义
 * 消除硬编码，所有魔法数字都应使用此处定义的常量
 */

/**
 * 分页相关常量
 */
export const PAGE_CONSTANTS = {
  DEFAULT_PAGE_NUM: 1,
  DEFAULT_PAGE_SIZE: 20,
  MAX_PAGE_SIZE: 100,
  SMALL_PAGE_SIZE: 10,
  LARGE_PAGE_SIZE: 50,
} as const

/**
 * 商品交易状态
 */
export enum TradeStatus {
  ON_SALE = 0,      // 在售
  SOLD = 1,         // 已售出
  OFF_SHELF = 2,    // 已下架
  DELETED = 3,      // 软删除（兼容旧值）
}

/**
 * 商品交易状态映射
 */
export const TRADE_STATUS_MAP: Record<TradeStatus, { text: string; color: string }> = {
  [TradeStatus.ON_SALE]: { text: '在售', color: 'bg-green-50 text-green-600' },
  [TradeStatus.SOLD]: { text: '已售出', color: 'bg-slate-50 text-slate-400' },
  [TradeStatus.OFF_SHELF]: { text: '已下架', color: 'bg-red-50 text-red-600' },
  [TradeStatus.DELETED]: { text: '已删除', color: 'bg-slate-50 text-slate-400' },
}

/**
 * 订单状态
 */
export enum OrderStatus {
  PENDING_PAY = 0,      // 待支付
  PENDING_DELIVERY = 1, // 待发货
  PENDING_RECEIVE = 2,  // 待收货
  COMPLETED = 3,        // 已完成
  CANCELLED = 4,        // 已取消
  ENDED = 5,            // 已结束（纠纷完结等非正常完成）
}

/**
 * 订单状态映射
 */
export const ORDER_STATUS_MAP: Record<OrderStatus, { text: string; color: string }> = {
  [OrderStatus.PENDING_PAY]: { text: '待支付', color: 'text-yellow-600 bg-yellow-50' },
  [OrderStatus.PENDING_DELIVERY]: { text: '待发货', color: 'text-warm-600 bg-warm-50' },
  [OrderStatus.PENDING_RECEIVE]: { text: '待收货', color: 'text-orange-600 bg-orange-50' },
  [OrderStatus.COMPLETED]: { text: '已完成', color: 'text-green-600 bg-green-50' },
  [OrderStatus.CANCELLED]: { text: '已取消', color: 'text-slate-600 bg-slate-50' },
  [OrderStatus.ENDED]: { text: '已结束', color: 'text-slate-600 bg-slate-50' },
}

/**
 * 商品审核状态
 */
export enum ReviewStatus {
  PENDING = 0,          // 待审核
  APPROVED = 1,         // AI审核通过
  MANUAL_PASSED = 2,    // 人工审核通过
  REJECTED = 3,         // 已驳回
  WAIT_MANUAL = 4,      // 待人工复核
}

/**
 * 商品审核状态映射
 */
export const REVIEW_STATUS_MAP: Record<number, { text: string; color: string }> = {
  [ReviewStatus.PENDING]: { text: '待审核', color: 'bg-yellow-100 text-yellow-700' },
  [ReviewStatus.APPROVED]: { text: '已上架', color: 'bg-green-100 text-green-700' },
  [ReviewStatus.MANUAL_PASSED]: { text: '已上架', color: 'bg-green-100 text-green-700' },
  [ReviewStatus.WAIT_MANUAL]: { text: '待人工复核', color: 'bg-orange-100 text-orange-700' },
  [ReviewStatus.REJECTED]: { text: '已驳回', color: 'bg-red-100 text-red-700' },
}

/**
 * 认证状态
 */
export enum AuthStatus {
  NOT_SUBMITTED = 0,    // 未提交
  PENDING = 1,          // 待审核
  APPROVED = 2,         // 已通过
  REJECTED = 3,         // 已拒绝
}

/**
 * 跑腿任务状态
 */
export enum ErrandStatus {
  PENDING = 0,          // 待接单
  IN_PROGRESS = 1,      // 进行中
  PENDING_CONFIRM = 2,  // 待确认
  COMPLETED = 3,        // 已完成
  CANCELLED = 4,        // 已取消
}

/**
 * 纠纷状态
 */
export enum DisputeStatus {
  PENDING = 0,          // 待处理
  PROCESSING = 1,       // 处理中
  RESOLVED = 2,         // 已解决
  REJECTED = 3,         // 已驳回
  WITHDRAWN = 4,        // 已撤回
}

/**
 * 纠纷类型
 */
export enum DisputeType {
  GOODS_TRADE = 0,      // 商品交易
  ERRAND_SERVICE = 1,   // 跑腿劳务
}

/**
 * 评价目标类型
 */
export enum ReviewTargetType {
  ORDER = 0,            // 订单评价
  ERRAND = 1,           // 跑腿评价
}

/**
 * 物品成色
 */
export enum ItemCondition {
  BRAND_NEW = 0,        // 全新
  LIKE_NEW = 1,         // 几乎全新
  EXCELLENT = 2,        // 轻微使用痕迹
  GOOD = 3,             // 使用痕迹明显
  FAIR = 4,             // 功能正常外观老旧
  POOR = 5,             // 需要维修
}

/**
 * 物品成色映射
 */
export const ITEM_CONDITION_MAP: Record<ItemCondition, { text: string; value: number }> = {
  [ItemCondition.BRAND_NEW]: { text: '全新', value: 0 },
  [ItemCondition.LIKE_NEW]: { text: '几乎全新', value: 1 },
  [ItemCondition.EXCELLENT]: { text: '轻微使用痕迹', value: 2 },
  [ItemCondition.GOOD]: { text: '使用痕迹明显', value: 3 },
  [ItemCondition.FAIR]: { text: '功能正常外观老旧', value: 4 },
  [ItemCondition.POOR]: { text: '需要维修', value: 5 },
}

/**
 * 性别
 */
export enum Gender {
  UNKNOWN = 0,
  MALE = 1,
  FEMALE = 2,
}

/**
 * 账号状态
 */
export enum AccountStatus {
  NORMAL = 0,
  FROZEN = 1,
}

/**
 * 跑腿员状态
 */
export enum RunnableStatus {
  NOT_APPLIED = 0,      // 未申请
  PENDING = 1,          // 审核中
  APPROVED = 2,         // 已通过
  REJECTED = 3,         // 已拒绝
}

/**
 * 搜索排序类型
 */
export enum SortType {
  DEFAULT = 0,          // 综合
  LATEST = 1,           // 最新
  PRICE_ASC = 2,        // 价格升序
  PRICE_DESC = 3,       // 价格降序
  HOT = 4,              // 热度
}

/**
 * 是否标志
 */
export enum YesNo {
  NO = 0,
  YES = 1,
}

/**
 * API响应状态码
 */
export const RESULT_CODE = {
  SUCCESS: 200,
  VALIDATION_ERROR: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  SERVER_ERROR: 500,
} as const

/**
 * 消息提示时间（毫秒）
 */
export const MESSAGE_DURATION = {
  SHORT: 2000,
  MEDIUM: 3000,
  LONG: 5000,
} as const

/**
 * 路由名称
 */
export const ROUTE_NAME = {
  HOME: 'home',
  LOGIN: 'login',
  REGISTER: 'register',
  GOODS_LIST: 'goods-list',
  GOODS_DETAIL: 'goods-detail',
  PUBLISH: 'publish',
  CART: 'cart',
  PROFILE: 'profile',
  SETTINGS: 'settings',
  ORDERS: 'orders',
  MY_GOODS: 'my-goods',
  MY_ORDERS: 'my-orders',
  COLLECTIONS: 'collections',
  ERRAND_LIST: 'errand-list',
  ERRAND_DETAIL: 'errand-detail',
  DISPUTE_LIST: 'dispute-list',
  DISPUTE_DETAIL: 'dispute-detail',
} as const

export {
  ERRAND_STATUS_MAP,
  DISPUTE_STATUS_MAP,
  REFUND_STATUS_MAP,
  RATING_MAP,
  CREDIT_LEVEL_MAP,
  ErrandStatusMap,
  DisputeStatusMap,
  RefundStatusMap,
  RatingMap,
  CreditLevelMap,
} from './statusMaps'
