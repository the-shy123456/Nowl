import { request } from '../request'
import type { PageResult, GoodsVO, UserInfo, PageQuery } from '@/types'
import { ADMIN_API, SEARCH_API } from '@/config/apiPaths'

/**
 * 仪表盘统计数据
 */
export interface DashboardStats {
  totalUsers: number
  todayNewUsers: number
  verifiedUsers: number
  totalGoods: number
  onSaleGoods: number
  todayNewGoods: number
  totalOrders: number
  todayOrders: number
  completedOrders: number
  totalAmount: number
  todayAmount: number
  pendingGoods: number
  pendingAuth: number
  pendingRunners: number
  pendingDisputes: number
  totalErrands: number
  activeErrands: number
}

export interface AdminOrderItem {
  orderId: number
  orderNo: string
  productId?: number
  schoolCode?: string
  campusCode?: string
  schoolName?: string
  campusName?: string
  productImage?: string
  productTitle?: string
  buyerName?: string
  sellerName?: string
  totalAmount: number
  orderStatus: number
  createTime?: string
}

export interface AdminErrandItem {
  taskId: number
  title?: string
  description?: string
  taskContent?: string
  publisherId?: number
  publisherAvatar?: string
  publisherName?: string
  acceptorId?: number
  acceptorAvatar?: string
  acceptorName?: string
  reward?: number
  taskStatus: number
  statusText?: string
  reviewStatus?: number
  reviewStatusText?: string
  auditReason?: string
  schoolCode?: string
  campusCode?: string
  schoolName?: string
  campusName?: string
  createTime?: string
}

export interface AdminDisputeItem {
  recordId: number
  initiatorAvatar?: string
  initiatorName?: string
  relatedAvatar?: string
  relatedName?: string
  orderNo?: string
  schoolCode?: string
  campusCode?: string
  schoolName?: string
  campusName?: string
  content?: string
  handleStatus: number
  handleResult?: string
  createTime?: string
}

export interface PendingAuthUserItem extends UserInfo {
  certImage?: string
  selfImage?: string
}

/**
 * 管理后台相关API
 */

// 获取仪表盘统计数据
export const getDashboardStats = () => {
  return request.get<DashboardStats>(ADMIN_API.STATS)
}

// 审核商品
export const auditGoods = (params: { goodsId: number; status: number; reason?: string }) => {
  return request.post(ADMIN_API.GOODS_AUDIT, null, { params })
}

// 获取待审核商品列表
export const getPendingAuditGoods = (
  params: PageQuery & { schoolCode?: string; campusCode?: string; reviewStatus?: number },
) => {
  return request.get<PageResult<GoodsVO>>(ADMIN_API.GOODS_PENDING, { params })
}

// 获取全部商品列表（管理员）
export const getAdminGoodsList = (
  params: PageQuery & {
    keyword?: string
    schoolCode?: string
    campusCode?: string
    tradeStatus?: number
    reviewStatus?: number
  },
) => {
  return request.get<PageResult<GoodsVO>>(ADMIN_API.GOODS_LIST, { params })
}

// 更新用户状态
export const updateUserStatus = (params: { userId: number; status: number }) => {
  return request.put(ADMIN_API.USER_STATUS, null, { params })
}

// 获取用户列表
export const getUserList = (
  params: PageQuery & {
    keyword?: string
    schoolCode?: string
    campusCode?: string
    accountStatus?: number
    authStatus?: number
  },
) => {
  return request.get<PageResult<UserInfo>>(ADMIN_API.USER_LIST, { params })
}

// 处理纠纷
export const handleDispute = (params: {
  disputeId: number
  result: string
  handleStatus?: number
  deductCreditScore?: number
  refundAmount?: number
}) => {
  return request.post(ADMIN_API.DISPUTE_HANDLE, null, { params })
}

// 强制下架商品
export const forceOfflineGoods = (params: { goodsId: number; reason?: string }) => {
  return request.post(ADMIN_API.GOODS_OFFLINE, null, { params })
}

// 获取纠纷列表
export const getDisputeList = (params: PageQuery & { status?: number; schoolCode?: string; campusCode?: string }) => {
  return request.get<PageResult<AdminDisputeItem>>(ADMIN_API.DISPUTE_LIST, { params })
}

// 获取待认证用户列表
export const getPendingAuthUsers = (params: PageQuery & { schoolCode?: string; campusCode?: string }) => {
  return request.get<PageResult<PendingAuthUserItem>>(ADMIN_API.AUTH_PENDING, { params })
}

// 审核用户认证
export const auditUserAuth = (params: { userId: number; status: number }) => {
  return request.post(ADMIN_API.AUTH_AUDIT, null, { params })
}

// 获取待审核跑腿员列表
export const getPendingRunnerUsers = (params: PageQuery & { schoolCode?: string; campusCode?: string }) => {
  return request.get<PageResult<UserInfo>>(ADMIN_API.RUNNER_PENDING, { params })
}

// 审核跑腿员
export const auditRunner = (params: { userId: number; status: number; reason?: string }) => {
  return request.post(ADMIN_API.RUNNER_AUDIT, null, { params })
}

// 获取订单列表（管理员）
export const getAdminOrderList = (
  params: PageQuery & { keyword?: string; schoolCode?: string; campusCode?: string; orderStatus?: number },
) => {
  return request.get<PageResult<AdminOrderItem>>(ADMIN_API.ORDER_LIST, { params })
}

// 获取跑腿列表（管理员）
export const getAdminErrandList = (
  params: PageQuery & { keyword?: string; status?: number; reviewStatus?: number; schoolCode?: string; campusCode?: string },
) => {
  return request.get<PageResult<AdminErrandItem>>(ADMIN_API.ERRAND_LIST, { params })
}

// 跑腿任务复核
export const auditErrand = (params: { taskId: number; status: number; reason?: string }) => {
  return request.post(ADMIN_API.ERRAND_AUDIT, null, { params })
}

export interface RiskEventItem {
  eventId: number
  traceId?: string
  eventType: string
  subjectType?: string
  subjectId?: string
  schoolCode?: string
  campusCode?: string
  riskFeatures?: string
  rawPayload?: string
  decisionId?: number
  decisionAction?: string
  riskLevel?: string
  riskScore?: number
  matchedRuleCodes?: string
  decisionReason?: string
  eventTime?: string
}

export interface RiskCaseItem {
  caseId: number
  eventId?: number
  decisionId?: number
  caseStatus: string
  assigneeId?: number
  result?: string
  resultReason?: string
  updateTime?: string
  eventType?: string
  subjectType?: string
  subjectId?: string
  decisionAction?: string
  riskLevel?: string
  schoolCode?: string
  campusCode?: string
  createTime?: string
}

export interface RiskRuleItem {
  ruleId: number
  ruleCode: string
  ruleName: string
  eventType: string
  ruleType: string
  ruleConfig?: string
  decisionAction: string
  priority: number
  status: number
  createTime?: string
}

export interface UserBehaviorControlItem {
  id: number
  userId: number
  userName?: string
  schoolCode?: string
  campusCode?: string
  eventType: string
  controlAction: string
  reason?: string
  expireTime?: string
  status: number
  operatorId?: number
  createTime?: string
}

export interface IamRoleItem {
  roleId: number
  roleCode: string
  roleName: string
  roleLevel: number
  status: number
}

export interface UserRoleBindingItem {
  id: number
  userId: number
  roleId: number
  roleCode: string
  roleName?: string
  status: number
  expiredTime?: string
  createTime?: string
}

export interface AdminScopeBindingItem {
  bindingId: number
  userId: number
  scopeType: string
  schoolCode?: string
  campusCode?: string
  status: number
  createTime?: string
}

export interface AdminOperationAuditItem {
  id: number
  traceId?: string
  operatorId?: number
  operatorIp?: string
  module?: string
  action?: string
  targetType?: string
  targetId?: string
  requestPayload?: string
  resultStatus?: string
  resultMessage?: string
  costMs?: number
  createTime?: string
}

export interface PermissionChangeItem {
  id: number
  traceId?: string
  operatorId?: number
  changeType?: string
  targetUserId?: number
  targetRoleId?: number
  targetPermissionId?: number
  beforeData?: string
  afterData?: string
  reason?: string
  createTime?: string
}

export interface LoginTraceItem {
  id: number
  traceId?: string
  userId?: number
  phone?: string
  ip?: string
  deviceId?: string
  geo?: string
  loginResult?: string
  failReason?: string
  riskLevel?: string
  createTime?: string
}

export interface AuditOverview {
  windowDays: number
  totalOperations: number
  failedOperations: number
  permissionChanges: number
  loginAttempts: number
  loginFailures: number
  highRiskLoginCount: number
  lastOperationTime?: string
}

export const getRiskEvents = (
  params: PageQuery & {
    eventType?: string
    subjectType?: string
    subjectId?: string
    decisionAction?: string
    riskLevel?: string
    schoolCode?: string
    campusCode?: string
    startTime?: string
    endTime?: string
  },
) => {
  return request.get<PageResult<RiskEventItem>>(ADMIN_API.RISK_EVENTS, { params })
}

export const getRiskCases = (
  params: PageQuery & {
    caseStatus?: string
    assigneeId?: number
    schoolCode?: string
    campusCode?: string
    startTime?: string
    endTime?: string
  },
) => {
  return request.get<PageResult<RiskCaseItem>>(ADMIN_API.RISK_CASES, { params })
}

export const handleRiskCase = (data: {
  caseId: number
  caseStatus: string
  result?: string
  resultReason?: string
}) => {
  return request.put(ADMIN_API.RISK_CASE_HANDLE, data)
}

export const getRiskRules = (params: PageQuery & { eventType?: string }) => {
  return request.get<PageResult<RiskRuleItem>>(ADMIN_API.RISK_RULES, { params })
}

export const upsertRiskRule = (data: {
  ruleId?: number
  ruleCode: string
  ruleName: string
  eventType: string
  ruleType: string
  ruleConfig: string
  decisionAction: string
  priority?: number
}) => {
  return request.put(ADMIN_API.RISK_RULE, data)
}

export const updateRiskRuleStatus = (ruleId: number, status: number) => {
  return request.put(ADMIN_API.RISK_RULE_STATUS(ruleId), null, { params: { status } })
}

export const getUserBehaviorControls = (userId: number) => {
  return request.get<UserBehaviorControlItem[]>(ADMIN_API.RISK_BEHAVIOR_CONTROLS(userId))
}

export const upsertBehaviorControl = (data: {
  userId: number
  eventType: string
  controlAction: string
  reason?: string
  expireTime?: string
}) => {
  return request.put(ADMIN_API.RISK_BEHAVIOR_CONTROL, data)
}

export const disableBehaviorControl = (controlId: number) => {
  return request.delete(ADMIN_API.RISK_BEHAVIOR_CONTROL + `/${controlId}`)
}

export const getIamRoles = () => {
  return request.get<IamRoleItem[]>(ADMIN_API.IAM_ROLES)
}

export const getUserRoleBindings = (userId: number) => {
  return request.get<UserRoleBindingItem[]>(ADMIN_API.IAM_USER_ROLES(userId))
}

export const grantUserRole = (data: {
  userId: number
  roleCode: string
  expiredTime?: string
  reason?: string
}) => {
  return request.put(ADMIN_API.IAM_GRANT_USER_ROLE, data)
}

export const revokeUserRole = (bindingId: number, reason?: string) => {
  return request.delete(ADMIN_API.IAM_REVOKE_USER_ROLE(bindingId), {
    params: { reason: reason || undefined },
  })
}

export const getAdminScopeBindings = (userId: number) => {
  return request.get<AdminScopeBindingItem[]>(ADMIN_API.IAM_SCOPES(userId))
}

export const upsertAdminScope = (data: {
  userId: number
  scopeType: string
  schoolCode?: string
  campusCode?: string
  reason?: string
}) => {
  return request.put(ADMIN_API.IAM_SCOPE, data)
}

export const disableAdminScope = (bindingId: number, reason?: string) => {
  return request.delete(ADMIN_API.IAM_DISABLE_SCOPE(bindingId), {
    params: { reason: reason || undefined },
  })
}

export const getAdminOperationAudits = (
  params: PageQuery & {
    operatorId?: number
    module?: string
    action?: string
    resultStatus?: string
    startTime?: string
    endTime?: string
  },
) => {
  return request.get<PageResult<AdminOperationAuditItem>>(ADMIN_API.AUDIT_OPERATIONS, { params })
}

export const getPermissionChanges = (
  params: PageQuery & {
    operatorId?: number
    targetUserId?: number
    changeType?: string
    startTime?: string
    endTime?: string
  },
) => {
  return request.get<PageResult<PermissionChangeItem>>(ADMIN_API.AUDIT_PERMISSION_CHANGES, { params })
}

export const getLoginTraces = (
  params: PageQuery & {
    userId?: number
    phone?: string
    loginResult?: string
    riskLevel?: string
    startTime?: string
    endTime?: string
  },
) => {
  return request.get<PageResult<LoginTraceItem>>(ADMIN_API.AUDIT_LOGIN_TRACES, { params })
}

export const getAuditOverview = (params?: { days?: number }) => {
  return request.get<AuditOverview>(ADMIN_API.AUDIT_OVERVIEW, { params })
}

// --- 搜索管理 API (Item 4) ---

export const syncGoodsSearchFull = () => {
  return request.post(SEARCH_API.SYNC_FULL)
}

export const createGoodsSearchIndex = () => {
  return request.post(SEARCH_API.INDEX_CREATE)
}

export const syncErrandSearchFull = () => {
  return request.post(SEARCH_API.ERRAND_SYNC_FULL)
}

export const createErrandSearchIndex = () => {
  return request.post(SEARCH_API.ERRAND_INDEX_CREATE)
}

// --- 用户管理增强 API (Item 5) ---

export interface UserDetailVO extends UserInfo {
  creditScore: number
}

export const getUserDetail = (userId: number) => {
  return request.get<UserDetailVO>(ADMIN_API.USER_DETAIL(userId))
}

export const adjustUserCredit = (data: { userId: number; change: number; reason: string }) => {
  return request.put(ADMIN_API.USER_CREDIT, null, { params: data })
}

export const broadcastNotice = (data: { title: string; content: string }) => {
  return request.post(ADMIN_API.NOTICE_BROADCAST, data)
}
