/**
 * 通用类型定义
 */

// API响应结构
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 分页查询参数
export interface PageQuery {
  pageNum: number
  pageSize: number
  orderBy?: string
  sortOrder?: 'asc' | 'desc'
  keyword?: string
}

// 分页结果
export interface PageResult<T> {
  total: number
  records: T[]
  pageNum?: number
  pageSize?: number
  pages?: number
}

// 用户信息
export interface UserInfo {
  userId: number
  studentNo: string
  nickName: string
  imageUrl?: string
  userName?: string
  phone?: string
  schoolCode: string
  campusCode: string
  schoolName?: string
  campusName?: string
  authStatus: number
  creditScore: number
  accountStatus: number
  runnableStatus: number
  money: number
  followCount: number
  fanCount: number
  roleCodes?: string[]
  permissionCodes?: string[]
  gender?: number
  grade?: string
  certImage?: string
  selfImage?: string
}

// 登录表单
export interface LoginForm {
  phone: string
  password: string
  code: string
  uuid: string
}

// 注册表单
export interface RegisterForm {
  phone: string
  password: string
  nickName: string
  code: string
}

// 商品信息
export interface GoodsInfo {
  productId: number
  sellerId: number
  categoryId: number
  title: string
  schoolCode: string
  campusCode: string
  tradeStatus: number
  description?: string
  imageList?: string
  image?: string
  itemCondition: number
  tradeType: number
  deliveryFee: number
  reviewStatus: number
  auditReason?: string
  collectCount: number
  price: number
  originalPrice?: number
  createTime: string
  updateTime: string
  // 扩展字段
  sellerName?: string
  sellerAvatar?: string
  sellerAuthStatus?: number
  sellerCreditScore?: number
  categoryName?: string
  isCollected?: boolean
  schoolName?: string
  campusName?: string
  aiValuation?: number
}

// 商品VO（用于管理员审核）
export interface GoodsVO {
  productId: number
  sellerId: number
  categoryId: number
  title: string
  schoolCode: string
  campusCode: string
  tradeStatus: number
  description?: string
  imageList?: string
  image?: string
  itemCondition: number
  tradeType: number
  deliveryFee: number
  reviewStatus: number
  auditReason?: string
  collectCount: number
  price: number
  createTime: string
  updateTime: string
  // 扩展字段
  sellerName?: string
  sellerAvatar?: string
  sellerAuthStatus?: number
  categoryName?: string
  isCollected?: boolean
  schoolName?: string
  campusName?: string
}

// 商品分类（ItemCategory别名）
export interface ItemCategory {
  categoryId: number
  categoryName: string
  parentId: number
  sort: number
  status: number
  children?: ItemCategory[]
}

// 商品查询参数
export interface GoodsQuery extends PageQuery {
  categoryId?: number
  parentCategoryId?: number
  schoolCode?: string
  campusCode?: string
  keyword?: string
  minPrice?: number
  maxPrice?: number
  tradeStatus?: number
  sellerId?: number
  sortType?: number
}

// 订单信息
export interface OrderInfo {
  orderId: number
  orderNo: string
  buyerId: number
  sellerId: number
  productId: number
  orderAmount: number
  deliveryFee: number
  totalAmount: number
  orderStatus: number
  payTime?: string
  deliveryTime?: string
  receiveTime?: string
  cancelTime?: string
  remark?: string
  refundStatus?: number
  refundReason?: string
  refundAmount?: number
  refundApplyTime?: string
  refundDeadline?: string
  refundProcessTime?: string
  refundProcessRemark?: string
  refundFastTrack?: number
  hasActiveDispute?: boolean
  activeDisputeId?: number
  activeDisputeStatus?: number
  latestClosedDisputeId?: number
  latestClosedDisputeStatus?: number
  latestClosedDisputeResult?: string
  latestClosedDisputeRefundAmount?: number
  latestClosedDisputeCreditPenalty?: number
  createTime: string
  updateTime: string
  // 扩展字段
  productImage?: string
  productTitle?: string
  sellerName?: string
  sellerAvatar?: string
  buyerName?: string
  buyerAvatar?: string
}

// 学校信息
export interface SchoolInfo {
  schoolCode: string
  schoolName: string
  campusCode: string
  campusName: string
  status: number
}

// 商品分类
export interface CategoryInfo {
  categoryId: number
  categoryName: string
  parentId: number
  sort: number
  status: number
  children?: CategoryInfo[]
}

// 跑腿任务
export interface ErrandTask {
  taskId: number
  publisherId: number
  acceptorId?: number
  title: string
  description?: string
  taskContent?: string
  imageList?: string
  pickupAddress?: string
  deliveryAddress?: string
  reward: number
  deadline?: string
  remark?: string
  taskStatus: number
  reviewStatus?: number
  reviewStatusText?: string
  auditReason?: string
  evidenceImage?: string
  schoolCode: string
  campusCode: string
  acceptTime?: string
  deliverTime?: string
  confirmTime?: string
  cancelTime?: string
  cancelReason?: string
  pickupLatitude?: number
  pickupLongitude?: number
  deliveryLatitude?: number
  deliveryLongitude?: number
  createTime: string
  updateTime?: string
  // 扩展字段
  publisherName?: string
  publisherAvatar?: string
  acceptorName?: string
  acceptorAvatar?: string
  statusText?: string
  currentLatitude?: number
  currentLongitude?: number
  hasActiveDispute?: boolean
  activeDisputeId?: number
  activeDisputeStatus?: number
  latestClosedDisputeId?: number
  latestClosedDisputeStatus?: number
  latestClosedDisputeResult?: string
  latestClosedDisputeRefundAmount?: number
  latestClosedDisputeCreditPenalty?: number
}

// 搜索结果VO
export interface SearchResultVO {
  productId: number
  title: string
  description?: string
  categoryId: number
  categoryName?: string
  price: number
  originalPrice?: number
  sellerId: number
  sellerName?: string
  sellerAvatar?: string
  sellerAuthStatus?: number
  schoolName?: string
  campusName?: string
  schoolCode?: string
  campusCode?: string
  tradeStatus: number
  image?: string
  collectCount?: number
  viewCount?: number
  hotScore?: number
  score?: number
  createTime?: string
  isCollected?: boolean
}

// 搜索请求参数
export interface SearchRequestDTO {
  keyword?: string
  categoryId?: number
  parentCategoryId?: number
  minPrice?: number
  maxPrice?: number
  schoolCode?: string
  campusCode?: string
  tradeStatus?: number
  sellerId?: number
  sortType?: number // 0-综合 1-最新 2-价格升序 3-价格降序 4-热度
  pageNum?: number
  pageSize?: number
}

// 推荐商品VO
export interface RecommendItemVO {
  productId: number
  title: string
  image?: string
  price: number
  originalPrice?: number
  categoryId?: number
  categoryName?: string
  sellerId: number
  sellerName?: string
  sellerAvatar?: string
  sellerAuthStatus?: number
  collectCount?: number
  score?: number
  isCollected?: boolean
  recommendType?: string // cf(协同过滤), content(内容推荐), hot(热门), hybrid(混合), fallback(兜底)
}
