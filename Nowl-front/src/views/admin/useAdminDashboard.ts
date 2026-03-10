import { ref, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  AlertTriangle,
  Bell,
  Bike,
  ChevronRight,
  ClipboardList,
  Home,
  LogOut,
  Package,
  RefreshCw,
  Search,
  ShieldCheck,
  ShoppingBag,
  TrendingUp,
  Users,
  XCircle,
} from 'lucide-vue-next'
import logoSvg from '@/assets/logo.svg'
import {
  getPendingAuditGoods,
  auditGoods,
  getAdminGoodsList,
  getUserList,
  updateUserStatus,
  handleDispute,
  forceOfflineGoods,
  getDisputeList,
  getDashboardStats,
  getPendingAuthUsers,
  auditUserAuth,
  getPendingRunnerUsers,
  auditRunner,
  getAdminOrderList,
  getAdminErrandList,
  auditErrand,
  getRiskMode,
  updateRiskMode,
  getBlacklist,
  upsertBlacklist,
  updateBlacklistStatus,
  getWhitelist,
  upsertWhitelist,
  updateWhitelistStatus,
  getRiskEvents,
  getRiskCases,
  handleRiskCase,
  getRiskRules,
  upsertRiskRule,
  updateRiskRuleStatus,
  getUserBehaviorControls,
  upsertBehaviorControl,
  disableBehaviorControl,
  getIamRoles,
  getUserRoleBindings,
  grantUserRole,
  revokeUserRole,
  getAdminScopeBindings,
  upsertAdminScope,
  disableAdminScope,
  getAdminOperationAudits,
  getPermissionChanges,
  getLoginTraces,
  getAuditOverview,
  syncGoodsSearchFull,
  createGoodsSearchIndex,
  syncErrandSearchFull,
  createErrandSearchIndex,
  getUserDetail,
  adjustUserCredit,
  broadcastNotice,
  type DashboardStats,
  type AdminOrderItem,
  type AdminDisputeItem,
  type AdminErrandItem,
  type PendingAuthUserItem,
  type RiskModeItem,
  type RiskSubjectListItem,
  type RiskEventItem,
  type RiskCaseItem,
  type RiskRuleItem,
  type UserBehaviorControlItem,
  type IamRoleItem,
  type UserRoleBindingItem,
  type AdminScopeBindingItem,
  type AdminOperationAuditItem,
  type PermissionChangeItem,
  type LoginTraceItem,
  type AuditOverview,
  type UserDetailVO,
} from '@/api/modules/admin'
import { getSchoolList, getCampusList } from '@/api/modules/school'
import { ElMessage, ElMessageBox } from '@/utils/feedback'
import { hasPermission, hasAnyPermission } from '@/utils/authz'
import { useUserStore } from '@/stores/user'
import {
  createQueryBinding,
  parseOptionalPositiveIntQuery,
  parsePositiveIntQuery,
  serializeOptionalPositiveIntQuery,
  serializePageQuery,
  useListQuerySync,
} from '@/composables/useListQuerySync'
import type { GoodsVO, UserInfo, SchoolInfo } from '@/types'
import AdminRiskCenterPanel from './components/AdminRiskCenterPanel.vue'
import AdminIamCenterPanel from './components/AdminIamCenterPanel.vue'
import AdminAuditCenterPanel from './components/AdminAuditCenterPanel.vue'
import AdminGoodsAuditPanel from './components/AdminGoodsAuditPanel.vue'
import AdminAuthAuditPanel from './components/AdminAuthAuditPanel.vue'
import AdminRunnerAuditPanel from './components/AdminRunnerAuditPanel.vue'
import AdminGoodsManagePanel from './components/AdminGoodsManagePanel.vue'
import AdminUserManagePanel from './components/AdminUserManagePanel.vue'
import AdminOrderManagePanel from './components/AdminOrderManagePanel.vue'
import AdminErrandManagePanel from './components/AdminErrandManagePanel.vue'
import AdminDisputeManagePanel from './components/AdminDisputeManagePanel.vue'
import AdminSearchManagePanel from './components/AdminSearchManagePanel.vue'
import AdminDashboardOverviewPanel from './components/AdminDashboardOverviewPanel.vue'
import { getDisputeDetail as getDisputeDetailForAdmin, type DisputeDetail } from '@/api/modules/dispute'


export const useAdminDashboard = () => {
  const route = useRoute()
  const router = useRouter()
  const userStore = useUserStore()
  
  type AdminMenuId =
    | 'dashboard'
    | 'goods-audit'
    | 'auth-audit'
    | 'runner-audit'
    | 'goods-manage'
    | 'user-manage'
    | 'order-manage'
    | 'errand-manage'
    | 'dispute-manage'
    | 'risk-center'
    | 'iam-center'
    | 'audit-center'
    | 'search-manage'
  
  // 状态定义
  const activeMenu = ref<AdminMenuId>('dashboard')
  const loading = ref(false)
  const sidebarCollapsed = ref(false)
  const manualRefreshSpinning = ref(false)
  
  // 仪表盘统计数据
  const dashboardStats = ref<DashboardStats | null>(null)
  
  // 动画数字
  const animatedStats = ref({
    totalUsers: 0,
    todayNewUsers: 0,
    verifiedUsers: 0,
    totalGoods: 0,
    onSaleGoods: 0,
    todayNewGoods: 0,
    pendingGoods: 0,
    totalOrders: 0,
    todayOrders: 0,
    completedOrders: 0,
    totalAmount: 0,
    todayAmount: 0,
    pendingAuth: 0,
    pendingRunners: 0,
    pendingDisputes: 0,
    totalErrands: 0,
    activeErrands: 0,
  })
  
  // 数字动画函数
  const animateNumber = (key: keyof typeof animatedStats.value, target: number, duration = 1000) => {
    const start = animatedStats.value[key]
    const startTime = performance.now()
  
    const animate = (currentTime: number) => {
      const elapsed = currentTime - startTime
      const progress = Math.min(elapsed / duration, 1)
  
      // 使用 easeOutQuart 缓动函数
      const easeOut = 1 - Math.pow(1 - progress, 4)
      animatedStats.value[key] = Math.floor(start + (target - start) * easeOut)
  
      if (progress < 1) {
        requestAnimationFrame(animate)
      }
    }
  
    requestAnimationFrame(animate)
  }
  
  // 监听数据变化，触发动画
  watch(dashboardStats, (newStats) => {
    if (newStats) {
      Object.keys(animatedStats.value).forEach((key) => {
        const k = key as keyof typeof animatedStats.value
        const value = newStats[k as keyof DashboardStats]
        const target = typeof value === 'number' ? value : 0
        animateNumber(k, target)
      })
    }
  }, { immediate: true })
  
  // 分页状态
  const pagination = ref({
    pageNum: 1,
    pageSize: 10,
    total: 0,
  })
  
  // 搜索关键词
  const searchKeyword = ref('')
  
  // 筛选状态
  const filterSchoolCode = ref('')
  const filterCampusCode = ref('')
  // '' 表示不传 reviewStatus，让后端按“待审核 + 待人工复核”默认筛选
  const goodsAuditReviewStatus = ref<number | '' | undefined>('')
  const goodsTradeStatus = ref<number | '' | undefined>('')
  const goodsReviewStatus = ref<number | '' | undefined>('')
  const userAccountStatus = ref<number | '' | undefined>('')
  const userAuthStatus = ref<number | '' | undefined>('')
  const orderStatusFilter = ref<number | '' | undefined>('')
  const errandStatusFilter = ref<number | '' | undefined>('')
  const errandReviewStatusFilter = ref<number | '' | undefined>('')
  const disputeStatusFilter = ref<number | '' | undefined>('')
  const schoolList = ref<SchoolInfo[]>([])
  const campusList = ref<SchoolInfo[]>([])
  
  // 顶部浮层交互
  const showNoticePanel = ref(false)
  const showAvatarPanel = ref(false)
  const noticePanelRef = ref<HTMLElement | null>(null)
  const avatarPanelRef = ref<HTMLElement | null>(null)
  
  // 数据列表
  const pendingGoods = ref<GoodsVO[]>([])
  const pendingAuthUsers = ref<PendingAuthUserItem[]>([])
  const pendingRunnerUsers = ref<UserInfo[]>([])
  const allGoods = ref<GoodsVO[]>([])
  const allUsers = ref<UserInfo[]>([])
  const allOrders = ref<AdminOrderItem[]>([])
  const disputes = ref<AdminDisputeItem[]>([])
  const allErrands = ref<AdminErrandItem[]>([])
  const riskEvents = ref<RiskEventItem[]>([])
  const riskCases = ref<RiskCaseItem[]>([])
  const riskRules = ref<RiskRuleItem[]>([])
  const behaviorControls = ref<UserBehaviorControlItem[]>([])
  const iamRoles = ref<IamRoleItem[]>([])
  const userRoleBindings = ref<UserRoleBindingItem[]>([])
  const adminScopeBindings = ref<AdminScopeBindingItem[]>([])
  const operationAudits = ref<AdminOperationAuditItem[]>([])
  const permissionChanges = ref<PermissionChangeItem[]>([])
  const loginTraces = ref<LoginTraceItem[]>([])
  const auditOverview = ref<AuditOverview | null>(null)
  
  // 搜索管理状态
  const searchOpLoading = ref(false)
  const adminActionPendingKeys = ref<Set<string>>(new Set())

  const beginAdminAction = (actionKey: string) => {
    if (adminActionPendingKeys.value.has(actionKey)) {
      ElMessage.warning('操作处理中，请勿重复提交')
      return false
    }
    const next = new Set(adminActionPendingKeys.value)
    next.add(actionKey)
    adminActionPendingKeys.value = next
    return true
  }

  const endAdminAction = (actionKey: string) => {
    if (!adminActionPendingKeys.value.has(actionKey)) {
      return
    }
    const next = new Set(adminActionPendingKeys.value)
    next.delete(actionKey)
    adminActionPendingKeys.value = next
  }

  const isAdminActionPending = (actionKey: string) => adminActionPendingKeys.value.has(actionKey)
  const isGoodsAuditPending = (goodsId: number) => isAdminActionPending(`goods-audit:${goodsId}`)
  const isAuthAuditPending = (userId: number) => isAdminActionPending(`auth-audit:${userId}`)
  const isRunnerAuditPending = (userId: number) => isAdminActionPending(`runner-audit:${userId}`)
  const isErrandAuditPending = (taskId: number) => isAdminActionPending(`errand-audit:${taskId}`)
  const isDisputeHandleOpeningPending = (disputeId: number) => isAdminActionPending(`dispute-open:${disputeId}`)
  
  // 用户详情对话框状态
  const showUserDetailDialog = ref(false)
  const userDetailData = ref<UserDetailVO | null>(null)
  const creditChangeForm = ref({ change: 0, reason: '' })
  const showBroadcastDialog = ref(false)
  const broadcastForm = ref({ title: '', content: '' })

  // 纠纷处理对话框状态
  const showDisputeHandleDialog = ref(false)
  const disputeHandleDialogLoading = ref(false)
  const disputeHandleSubmitting = ref(false)
  const disputeHandleDetail = ref<DisputeDetail | null>(null)
  const disputeHandleForm = ref({
    disputeId: 0,
    handleStatus: 2 as 2 | 3,
    result: '',
    // Vue 在 `<input type="number">` 上可能会自动将 v-model 值转为 number，需要兼容 string/number。
    deductCreditScoreText: '' as string | number,
    refundAmountText: '' as string | number,
  })

  const disputeHandleMaxRefund = computed(() => {
    const detail = disputeHandleDetail.value
    if (!detail || detail.targetType !== 0) {
      return null
    }
    const orderAmount = Number(detail.orderAmount ?? NaN)
    const claimAmount = Number(detail.claimRefundAmount ?? NaN)

    let max = Number.isFinite(orderAmount) && orderAmount > 0 ? orderAmount : NaN
    if (Number.isFinite(claimAmount) && claimAmount > 0) {
      max = Number.isFinite(max) ? Math.min(max, claimAmount) : claimAmount
    }
    return Number.isFinite(max) ? max : null
  })

  const canDisputeDeductCredit = computed(() => {
    if (disputeHandleForm.value.handleStatus !== 2) return false
    return Number(disputeHandleDetail.value?.claimSellerCreditPenalty) === 1
  })

  const canDisputeRefund = computed(() => {
    if (disputeHandleForm.value.handleStatus !== 2) return false
    const detail = disputeHandleDetail.value
    return detail?.targetType === 0 && Number(detail?.claimRefund) === 1
  })

  const disputeHandleClaimSummary = computed(() => {
    const detail = disputeHandleDetail.value
    if (!detail) return '无法读取诉求信息'

    const parts: string[] = []
    parts.push(`申请扣分：${Number(detail.claimSellerCreditPenalty) === 1 ? '是' : '否'}`)
    if (detail.targetType === 0) {
      if (Number(detail.claimRefund) === 1) {
        const claimAmount = Number(detail.claimRefundAmount ?? NaN)
        parts.push(`申请退款：是${Number.isFinite(claimAmount) && claimAmount > 0 ? `（¥${claimAmount}）` : ''}`)
      } else {
        parts.push('申请退款：否')
      }
    }
    return parts.join('，')
  })
  
  const riskTab = ref<'mode' | 'events' | 'cases' | 'rules' | 'behavior' | 'blacklist' | 'whitelist'>('events')
  const auditTab = ref<'operations' | 'permission' | 'login'>('operations')
  const riskTabOptions = ['mode', 'events', 'cases', 'rules', 'behavior', 'blacklist', 'whitelist'] as const
  const auditTabOptions = ['operations', 'permission', 'login'] as const
  const riskMode = ref('FULL')
  const riskEventTypeFilter = ref('')
  const riskDecisionActionFilter = ref('')
  const riskLevelFilter = ref('')
  const riskCaseStatusFilter = ref('')
  const riskStartTimeFilter = ref('')
  const riskEndTimeFilter = ref('')
  const riskSubjectTypeFilter = ref('')
  const riskSubjectIdFilter = ref('')
  const behaviorUserIdInput = ref('')
  const behaviorTargetUserId = ref<number | null>(null)
  const blacklistItems = ref<RiskSubjectListItem[]>([])
  const whitelistItems = ref<RiskSubjectListItem[]>([])
  const iamUserIdInput = ref('')
  const iamTargetUserId = ref<number | null>(null)
  const auditUserIdInput = ref('')
  const auditTargetUserId = ref<number | null>(null)
  
  // 菜单配置
  const menus: Array<{ id: AdminMenuId; label: string; icon: unknown; badge: keyof DashboardStats | null }> = [
    { id: 'dashboard', label: '数据概览', icon: TrendingUp, badge: null },
    { id: 'goods-audit', label: '商品审核', icon: Package, badge: 'pendingGoods' },
    { id: 'auth-audit', label: '认证审核', icon: ShieldCheck, badge: 'pendingAuth' },
    { id: 'runner-audit', label: '跑腿员审核', icon: Bike, badge: 'pendingRunners' },
    { id: 'goods-manage', label: '商品管理', icon: ShoppingBag, badge: null },
    { id: 'user-manage', label: '用户管理', icon: Users, badge: null },
    { id: 'order-manage', label: '订单管理', icon: ClipboardList, badge: null },
    { id: 'errand-manage', label: '跑腿管理', icon: Bike, badge: null },
    { id: 'dispute-manage', label: '纠纷管理', icon: AlertTriangle, badge: 'pendingDisputes' },
    { id: 'risk-center', label: '风控中心', icon: ShieldCheck, badge: null },
    { id: 'iam-center', label: 'IAM权限', icon: Users, badge: null },
    { id: 'audit-center', label: '审计中心', icon: ClipboardList, badge: null },
    { id: 'search-manage', label: '搜索管理', icon: Search, badge: null },
  ]
  
  const adminMenuRouteMap: Record<AdminMenuId, string> = {
    dashboard: '/admin/dashboard',
    'goods-audit': '/admin/goods-audit',
    'auth-audit': '/admin/auth-audit',
    'runner-audit': '/admin/runner-audit',
    'goods-manage': '/admin/goods-manage',
    'user-manage': '/admin/user-manage',
    'order-manage': '/admin/order-manage',
    'errand-manage': '/admin/errand-manage',
    'dispute-manage': '/admin/dispute-manage',
    'risk-center': '/admin/risk',
    'iam-center': '/admin/iam',
    'audit-center': '/admin/audit',
    'search-manage': '/admin/search-manage',
  }
  
  const menuPerms: Record<AdminMenuId, string[]> = {
    dashboard: ['admin:dashboard:view'],
    'goods-audit': ['admin:goods:pending:view'],
    'auth-audit': ['admin:auth:pending:view'],
    'runner-audit': ['admin:runner:pending:view'],
    'goods-manage': ['admin:goods:list:view'],
    'user-manage': ['admin:user:list:view'],
    'order-manage': ['admin:order:list:view'],
    'errand-manage': ['admin:errand:list:view'],
    'dispute-manage': ['admin:dispute:list:view'],
    'risk-center': ['risk:mode:view', 'risk:list:view', 'risk:event:view', 'risk:case:handle', 'risk:rule:manage', 'admin:risk:behavior:view'],
    'iam-center': ['admin:iam:role:view', 'admin:iam:user-role:view', 'admin:iam:scope:view'],
    'audit-center': ['admin:audit:operation:view', 'admin:audit:permission:view', 'admin:audit:login:view'],
    'search-manage': ['admin:search:manage'],
  }
  
  const visibleMenus = computed(() => menus.filter((menu) => hasAnyPermission(userStore.userInfo, menuPerms[menu.id])))
  
  const roleCodes = computed(() => userStore.userInfo?.roleCodes || [])
  const isSuperAdmin = computed(() => roleCodes.value.includes('SUPER_ADMIN'))
  const isSchoolAdmin = computed(() => !isSuperAdmin.value && roleCodes.value.includes('SCHOOL_ADMIN'))
  const isCampusAdmin = computed(() => !isSuperAdmin.value && roleCodes.value.includes('CAMPUS_ADMIN'))
  
  const roleLabel = computed(() => {
    if (isSuperAdmin.value) return '平台超级管理员'
    if (isSchoolAdmin.value) return '学校管理员'
    if (isCampusAdmin.value) return '校区管理员'
    return '后台管理员'
  })
  
  const canSelectSchool = computed(() => isSuperAdmin.value)
  const canSelectCampus = computed(() => isSuperAdmin.value || isSchoolAdmin.value)
  
  const effectiveSchoolCode = computed(() => {
    if (!canSelectSchool.value) {
      return userStore.userInfo?.schoolCode || ''
    }
    return filterSchoolCode.value
  })
  
  const querySchoolCode = computed(() => {
    return isSuperAdmin.value ? filterSchoolCode.value : ''
  })
  
  const queryCampusCode = computed(() => {
    if (isSuperAdmin.value || isSchoolAdmin.value) {
      return filterCampusCode.value
    }
    return ''
  })
  
  const pendingQuickActions = computed(() => {
    const stats = dashboardStats.value
    if (!stats) return []
    return [
      { menu: 'goods-audit' as AdminMenuId, label: '商品待审', count: stats.pendingGoods },
      { menu: 'auth-audit' as AdminMenuId, label: '认证待审', count: stats.pendingAuth },
      { menu: 'runner-audit' as AdminMenuId, label: '跑腿员待审', count: stats.pendingRunners },
      { menu: 'dispute-manage' as AdminMenuId, label: '纠纷待处理', count: stats.pendingDisputes },
    ].filter((item) => item.count > 0)
  })
  
  const pendingNoticeCount = computed(() => {
    return pendingQuickActions.value.reduce((sum, item) => sum + item.count, 0)
  })
  
  const formatSchoolCampus = (schoolName?: string, schoolCode?: string, campusName?: string, campusCode?: string) => {
    const school = schoolName || schoolCode || '未知学校'
    const campus = campusName || campusCode || '未知校区'
    return `${school} · ${campus}`
  }
  
  const isAdminMenuId = (value: string): value is AdminMenuId => {
    return Object.prototype.hasOwnProperty.call(menuPerms, value)
  }
  
  const routeMenuId = computed<AdminMenuId>(() => {
    const raw = route.meta.adminMenu
    if (typeof raw === 'string' && isAdminMenuId(raw)) {
      return raw
    }
    return 'dashboard'
  })
  
  const navigateToMenu = (menuId: AdminMenuId, replace = false) => {
    const targetPath = adminMenuRouteMap[menuId]
    if (!targetPath || route.path === targetPath) {
      return
    }
    if (replace) {
      router.replace(targetPath)
    } else {
      router.push(targetPath)
    }
  }
  
  // 获取菜单徽章数量
  const getMenuBadge = (badgeKey: keyof DashboardStats | null) => {
    if (!badgeKey || !dashboardStats.value) return 0
    const badge = dashboardStats.value[badgeKey]
    return typeof badge === 'number' ? badge : 0
  }
  
  const toOptionalString = (value?: string) => (value ? value : undefined)
  const toOptionalNumber = (value: number | '' | undefined) => (value === '' || value === undefined ? undefined : value)
  
  const normalizeSchoolOptions = (list: SchoolInfo[]) => {
    const map = new Map<string, SchoolInfo>()
    list.forEach((item) => {
      if (!map.has(item.schoolCode)) {
        map.set(item.schoolCode, item)
      }
    })
    return Array.from(map.values())
  }
  
  const normalizeCampusOptions = (list: SchoolInfo[]) => {
    const map = new Map<string, SchoolInfo>()
    list.forEach((item) => {
      if (!map.has(item.campusCode)) {
        map.set(item.campusCode, item)
      }
    })
    return Array.from(map.values())
  }
  
  const loadCampuses = async (schoolCode: string, shouldReload = false) => {
    if (!schoolCode) {
      campusList.value = []
      filterCampusCode.value = ''
      if (shouldReload) {
        loadData()
      }
      return
    }
    try {
      const res = await getCampusList(schoolCode)
      campusList.value = normalizeCampusOptions(res)
      if (isCampusAdmin.value) {
        filterCampusCode.value = userStore.userInfo?.campusCode || ''
      } else if (
        filterCampusCode.value &&
        !campusList.value.some((campus) => campus.campusCode === filterCampusCode.value)
      ) {
        filterCampusCode.value = ''
      }
      if (shouldReload) {
        loadData()
      }
    } catch (error) {
      console.error(error)
    }
  }
  
  const applyScopeDefaults = () => {
    if (!canSelectSchool.value) {
      filterSchoolCode.value = userStore.userInfo?.schoolCode || ''
    }
    if (isCampusAdmin.value) {
      filterCampusCode.value = userStore.userInfo?.campusCode || ''
    }
  }
  
  // 获取学校列表
  const fetchSchools = async () => {
    applyScopeDefaults()
    try {
      if (canSelectSchool.value) {
        const res = await getSchoolList()
        schoolList.value = normalizeSchoolOptions(res)
      } else if (userStore.userInfo?.schoolCode) {
        schoolList.value = [{
          schoolCode: userStore.userInfo.schoolCode,
          schoolName: userStore.userInfo.schoolName || userStore.userInfo.schoolCode,
          campusCode: userStore.userInfo.campusCode || '',
          campusName: userStore.userInfo.campusName || '',
          status: 1,
        }]
      } else {
        schoolList.value = []
      }
  
      if (effectiveSchoolCode.value) {
        await loadCampuses(effectiveSchoolCode.value, false)
      }
    } catch (e) {
      console.error(e)
    }
  }
  
  // 监听学校选择变化
  const handleSchoolChange = async () => {
    if (!canSelectSchool.value) return
    filterCampusCode.value = ''
    pagination.value.pageNum = 1
    await loadCampuses(filterSchoolCode.value, true)
  }
  
  const handleCampusChange = () => {
    if (isCampusAdmin.value) return
    pagination.value.pageNum = 1
    loadData()
  }
  
  const parsePositiveInt = (raw: string): number | null => {
    const value = Number(raw)
    if (!Number.isInteger(value) || value <= 0) {
      return null
    }
    return value
  }
  
  const parseEnumQueryValue = <T extends string>(
    raw: unknown,
    allowedValues: readonly T[],
    defaultValue: T,
  ): T => {
    const value = Array.isArray(raw) ? raw[0] : raw
    if (typeof value === 'string' && allowedValues.includes(value as T)) {
      return value as T
    }
    return defaultValue
  }
  
  const currentPage = computed({
    get: () => pagination.value.pageNum,
    set: (value: number) => {
      pagination.value.pageNum = value
    },
  })
  
  const currentPageSize = computed({
    get: () => pagination.value.pageSize,
    set: (value: number) => {
      pagination.value.pageSize = value
    },
  })
  
  useListQuerySync([
    createQueryBinding({
      key: 'page',
      state: currentPage,
      defaultValue: 1,
      parse: raw => parsePositiveIntQuery(raw, 1),
      serialize: value => serializePageQuery(value, 1),
    }),
    createQueryBinding({
      key: 'size',
      state: currentPageSize,
      defaultValue: 10,
      parse: raw => parsePositiveIntQuery(raw, 10),
    }),
    createQueryBinding({
      key: 'q',
      state: searchKeyword,
      defaultValue: '',
    }),
    createQueryBinding({
      key: 'schoolCode',
      state: filterSchoolCode,
      defaultValue: '',
    }),
    createQueryBinding({
      key: 'campusCode',
      state: filterCampusCode,
      defaultValue: '',
    }),
    createQueryBinding({
      key: 'riskTab',
      state: riskTab,
      defaultValue: 'events',
      parse: raw => parseEnumQueryValue(raw, riskTabOptions, 'events'),
      serialize: value => (value === 'events' ? undefined : value),
    }),
    createQueryBinding({
      key: 'auditTab',
      state: auditTab,
      defaultValue: 'operations',
      parse: raw => parseEnumQueryValue(raw, auditTabOptions, 'operations'),
      serialize: value => (value === 'operations' ? undefined : value),
    }),
    createQueryBinding({
      key: 'riskEventType',
      state: riskEventTypeFilter,
      defaultValue: '',
    }),
    createQueryBinding({
      key: 'riskDecision',
      state: riskDecisionActionFilter,
      defaultValue: '',
    }),
    createQueryBinding({
      key: 'riskLevel',
      state: riskLevelFilter,
      defaultValue: '',
    }),
    createQueryBinding({
      key: 'riskCaseStatus',
      state: riskCaseStatusFilter,
      defaultValue: '',
    }),
    createQueryBinding({
      key: 'riskStart',
      state: riskStartTimeFilter,
      defaultValue: '',
    }),
    createQueryBinding({
      key: 'riskEnd',
      state: riskEndTimeFilter,
      defaultValue: '',
    }),
    createQueryBinding({
      key: 'riskUserId',
      state: behaviorTargetUserId,
      defaultValue: null,
      parse: raw => parseOptionalPositiveIntQuery(raw) ?? null,
      serialize: value => serializeOptionalPositiveIntQuery(value),
    }),
    createQueryBinding({
      key: 'iamUserId',
      state: iamTargetUserId,
      defaultValue: null,
      parse: raw => parseOptionalPositiveIntQuery(raw) ?? null,
      serialize: value => serializeOptionalPositiveIntQuery(value),
    }),
    createQueryBinding({
      key: 'auditUserId',
      state: auditTargetUserId,
      defaultValue: null,
      parse: raw => parseOptionalPositiveIntQuery(raw) ?? null,
      serialize: value => serializeOptionalPositiveIntQuery(value),
    }),
  ], {
    onQueryApplied: () => {
      if (routeMenuId.value !== activeMenu.value) {
        return
      }
      loadData()
    },
  })
  
  watch(behaviorTargetUserId, (value) => {
    behaviorUserIdInput.value = value ? String(value) : ''
  }, { immediate: true })
  
  watch(iamTargetUserId, (value) => {
    iamUserIdInput.value = value ? String(value) : ''
  }, { immediate: true })
  const canViewRiskMode = computed(() => hasPermission(userStore.userInfo, 'risk:mode:view'))
  const canManageRiskMode = computed(() => hasPermission(userStore.userInfo, 'risk:mode:manage'))
  const canViewRiskList = computed(() => hasPermission(userStore.userInfo, 'risk:list:view'))
  const canManageRiskList = computed(() => hasPermission(userStore.userInfo, 'risk:list:manage'))
  const canViewRiskEvents = computed(() => hasPermission(userStore.userInfo, 'risk:event:view'))
  const canHandleRiskCase = computed(() => hasPermission(userStore.userInfo, 'risk:case:handle'))
  const canManageRiskRule = computed(() => hasPermission(userStore.userInfo, 'risk:rule:manage'))
  const canViewBehaviorControl = computed(() => hasPermission(userStore.userInfo, 'admin:risk:behavior:view'))
  const canManageBehaviorControl = computed(() => hasPermission(userStore.userInfo, 'admin:risk:behavior:manage'))
  
  const canViewIamRole = computed(() => hasPermission(userStore.userInfo, 'admin:iam:role:view'))
  const canViewIamUserRole = computed(() => hasPermission(userStore.userInfo, 'admin:iam:user-role:view'))
  const canManageIamUserRole = computed(() => hasPermission(userStore.userInfo, 'admin:iam:user-role:manage'))
  const canViewIamScope = computed(() => hasPermission(userStore.userInfo, 'admin:iam:scope:view'))
  const canManageIamScope = computed(() => hasPermission(userStore.userInfo, 'admin:iam:scope:manage'))
  
  const canViewOperationAudit = computed(() => hasPermission(userStore.userInfo, 'admin:audit:operation:view'))
  const canViewPermissionAudit = computed(() => hasPermission(userStore.userInfo, 'admin:audit:permission:view'))
  const canViewLoginAudit = computed(() => hasPermission(userStore.userInfo, 'admin:audit:login:view'))
  const canAuditErrand = computed(() => hasPermission(userStore.userInfo, 'admin:errand:audit'))
  
  const hasRiskTabAccess = (tab: 'mode' | 'events' | 'cases' | 'rules' | 'behavior' | 'blacklist' | 'whitelist') => {
    if (tab === 'mode') return canViewRiskMode.value
    if (tab === 'events') return canViewRiskEvents.value
    if (tab === 'cases') return canHandleRiskCase.value
    if (tab === 'rules') return canManageRiskRule.value
    if (tab === 'behavior') return canViewBehaviorControl.value
    return canViewRiskList.value
  }
  
  const hasAuditTabAccess = (tab: 'operations' | 'permission' | 'login') => {
    if (tab === 'operations') return canViewOperationAudit.value
    if (tab === 'permission') return canViewPermissionAudit.value
    return canViewLoginAudit.value
  }
  
  const normalizeRiskDateTimeParam = (value: string): string | undefined => {
    const trimmed = value.trim()
    if (!trimmed) return undefined
    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(trimmed)) {
      return `${trimmed}:00`
    }
    return trimmed
  }
  
  const buildRiskTimeRangeParams = (showWarning = false): { startTime?: string; endTime?: string } | null => {
    const startTime = normalizeRiskDateTimeParam(riskStartTimeFilter.value)
    const endTime = normalizeRiskDateTimeParam(riskEndTimeFilter.value)
  
    if (!startTime && !endTime) {
      return {}
    }
  
    const startTs = startTime ? Date.parse(startTime) : NaN
    const endTs = endTime ? Date.parse(endTime) : NaN
  
    if (startTime && Number.isNaN(startTs)) {
      if (showWarning) ElMessage.warning('开始时间格式不正确')
      return null
    }
    if (endTime && Number.isNaN(endTs)) {
      if (showWarning) ElMessage.warning('结束时间格式不正确')
      return null
    }
    if (startTime && endTime && startTs > endTs) {
      if (showWarning) ElMessage.warning('开始时间不能晚于结束时间')
      return null
    }
  
    return { startTime, endTime }
  }
  
  const ensureRiskTab = () => {
    const orderedTabs: Array<'mode' | 'events' | 'cases' | 'rules' | 'behavior' | 'blacklist' | 'whitelist'> = ['mode', 'events', 'cases', 'rules', 'behavior', 'blacklist', 'whitelist']
    const firstAllowed = orderedTabs.find((tab) => hasRiskTabAccess(tab))
    if (!firstAllowed) {
      return
    }
    if (!hasRiskTabAccess(riskTab.value)) {
      riskTab.value = firstAllowed
    }
  }
  
  const ensureAuditTab = () => {
    const orderedTabs: Array<'operations' | 'permission' | 'login'> = ['operations', 'permission', 'login']
    const firstAllowed = orderedTabs.find((tab) => hasAuditTabAccess(tab))
    if (!firstAllowed) {
      return
    }
    if (!hasAuditTabAccess(auditTab.value)) {
      auditTab.value = firstAllowed
    }
  }
  
  const hasMenuAccess = (menuId: string) => {
    const perms = menuPerms[menuId as AdminMenuId]
    if (!perms) {
      return false
    }
    return hasAnyPermission(userStore.userInfo, perms)
  }
  
  const ensureActiveMenu = () => {
    if (visibleMenus.value.length === 0) {
      return
    }
  
    const routeMenu = routeMenuId.value
    if (hasMenuAccess(routeMenu)) {
      activeMenu.value = routeMenu
    }
  
    if (!visibleMenus.value.some((menu) => menu.id === activeMenu.value)) {
      const firstMenu = visibleMenus.value[0]
      if (firstMenu) {
        activeMenu.value = firstMenu.id
        navigateToMenu(firstMenu.id, true)
      }
    }
  }
  
  const ensurePermission = (permissionCode: string): boolean => {
    if (hasPermission(userStore.userInfo, permissionCode)) {
      return true
    }
    ElMessage.warning('当前账号无此操作权限')
    return false
  }
  
  // 数据加载
  const loadData = async () => {
    ensureActiveMenu()
    if (visibleMenus.value.length === 0) {
      loading.value = false
      return
    }
  
    if (activeMenu.value === 'risk-center') {
      ensureRiskTab()
    }
    if (activeMenu.value === 'audit-center') {
      ensureAuditTab()
    }
  
    loading.value = true
    const { pageNum, pageSize } = pagination.value
    try {
      if (activeMenu.value === 'dashboard') {
        const res = await getDashboardStats()
        dashboardStats.value = res
      } else if (activeMenu.value === 'goods-audit') {
        const res = await getPendingAuditGoods({
          pageNum,
          pageSize,
          schoolCode: toOptionalString(querySchoolCode.value),
          campusCode: toOptionalString(queryCampusCode.value),
          reviewStatus: toOptionalNumber(goodsAuditReviewStatus.value),
        })
        pendingGoods.value = res?.records || []
        pagination.value.total = res?.total || 0
      } else if (activeMenu.value === 'auth-audit') {
        const res = await getPendingAuthUsers({
          pageNum,
          pageSize,
          schoolCode: toOptionalString(querySchoolCode.value),
          campusCode: toOptionalString(queryCampusCode.value)
        })
        pendingAuthUsers.value = res?.records || []
        pagination.value.total = res?.total || 0
      } else if (activeMenu.value === 'runner-audit') {
        const res = await getPendingRunnerUsers({
          pageNum,
          pageSize,
          schoolCode: toOptionalString(querySchoolCode.value),
          campusCode: toOptionalString(queryCampusCode.value)
        })
        pendingRunnerUsers.value = res?.records || []
        pagination.value.total = res?.total || 0
      } else if (activeMenu.value === 'goods-manage') {
        const res = await getAdminGoodsList({
          pageNum,
          pageSize,
          keyword: searchKeyword.value || undefined,
          schoolCode: toOptionalString(querySchoolCode.value),
          campusCode: toOptionalString(queryCampusCode.value),
          tradeStatus: toOptionalNumber(goodsTradeStatus.value),
          reviewStatus: toOptionalNumber(goodsReviewStatus.value),
        })
        allGoods.value = res?.records || []
        pagination.value.total = res?.total || 0
      } else if (activeMenu.value === 'user-manage') {
        const res = await getUserList({
          pageNum,
          pageSize,
          keyword: searchKeyword.value || undefined,
          schoolCode: toOptionalString(querySchoolCode.value),
          campusCode: toOptionalString(queryCampusCode.value),
          accountStatus: toOptionalNumber(userAccountStatus.value),
          authStatus: toOptionalNumber(userAuthStatus.value),
        })
        allUsers.value = res?.records || []
        pagination.value.total = res?.total || 0
      } else if (activeMenu.value === 'order-manage') {
        const res = await getAdminOrderList({
          pageNum,
          pageSize,
          keyword: searchKeyword.value || undefined,
          schoolCode: toOptionalString(querySchoolCode.value),
          campusCode: toOptionalString(queryCampusCode.value),
          orderStatus: toOptionalNumber(orderStatusFilter.value),
        })
        allOrders.value = res?.records || []
        pagination.value.total = res?.total || 0
      } else if (activeMenu.value === 'dispute-manage') {
        const res = await getDisputeList({
          pageNum,
          pageSize,
          status: toOptionalNumber(disputeStatusFilter.value),
          schoolCode: toOptionalString(querySchoolCode.value),
          campusCode: toOptionalString(queryCampusCode.value),
        })
        disputes.value = res?.records || []
        pagination.value.total = res?.total || 0
      } else if (activeMenu.value === 'errand-manage') {
        const res = await getAdminErrandList({
          pageNum,
          pageSize,
          keyword: searchKeyword.value || undefined,
          status: toOptionalNumber(errandStatusFilter.value),
          reviewStatus: toOptionalNumber(errandReviewStatusFilter.value),
          schoolCode: toOptionalString(querySchoolCode.value),
          campusCode: toOptionalString(queryCampusCode.value),
        })
        allErrands.value = res?.records || []
        pagination.value.total = res?.total || 0
      } else if (activeMenu.value === 'risk-center') {
        if (riskTab.value === 'events') {
          if (!canViewRiskEvents.value) {
            riskEvents.value = []
            pagination.value.total = 0
          } else {
            const timeRange = buildRiskTimeRangeParams()
            if (!timeRange) {
              riskEvents.value = []
              pagination.value.total = 0
              return
            }
            const res = await getRiskEvents({
              pageNum,
              pageSize,
              eventType: riskEventTypeFilter.value || undefined,
              decisionAction: riskDecisionActionFilter.value || undefined,
              riskLevel: riskLevelFilter.value || undefined,
              ...timeRange,
            })
            riskEvents.value = res?.records || []
            pagination.value.total = res?.total || 0
          }
        } else if (riskTab.value === 'cases') {
          if (!canHandleRiskCase.value) {
            riskCases.value = []
            pagination.value.total = 0
          } else {
            const timeRange = buildRiskTimeRangeParams()
            if (!timeRange) {
              riskCases.value = []
              pagination.value.total = 0
              return
            }
            const res = await getRiskCases({
              pageNum,
              pageSize,
              caseStatus: riskCaseStatusFilter.value || undefined,
              ...timeRange,
            })
            riskCases.value = res?.records || []
            pagination.value.total = res?.total || 0
          }
        } else if (riskTab.value === 'rules') {
          if (!canManageRiskRule.value) {
            riskRules.value = []
            pagination.value.total = 0
          } else {
            const res = await getRiskRules({ pageNum, pageSize, eventType: riskEventTypeFilter.value || undefined })
            riskRules.value = res?.records || []
            pagination.value.total = res?.total || 0
          }
        } else {
          if (!canViewBehaviorControl.value) {
            behaviorControls.value = []
          } else if (behaviorTargetUserId.value) {
            behaviorControls.value = await getUserBehaviorControls(behaviorTargetUserId.value)
          } else {
            behaviorControls.value = []
          }
          pagination.value.total = 0
        }
      } else if (activeMenu.value === 'iam-center') {
        if (canViewIamRole.value) {
          iamRoles.value = await getIamRoles()
        } else {
          iamRoles.value = []
        }
        if (iamTargetUserId.value && canViewIamUserRole.value && canViewIamScope.value) {
          const [roles, scopes] = await Promise.all([getUserRoleBindings(iamTargetUserId.value), getAdminScopeBindings(iamTargetUserId.value)])
          userRoleBindings.value = roles || []
          adminScopeBindings.value = scopes || []
        } else if (iamTargetUserId.value && canViewIamUserRole.value) {
          userRoleBindings.value = (await getUserRoleBindings(iamTargetUserId.value)) || []
          adminScopeBindings.value = []
        } else if (iamTargetUserId.value && canViewIamScope.value) {
          userRoleBindings.value = []
          adminScopeBindings.value = (await getAdminScopeBindings(iamTargetUserId.value)) || []
        } else {
          userRoleBindings.value = []
          adminScopeBindings.value = []
        }
        pagination.value.total = 0
      } else if (activeMenu.value === 'audit-center') {
        if (canViewOperationAudit.value || canViewPermissionAudit.value || canViewLoginAudit.value) {
          auditOverview.value = await getAuditOverview({ days: 7 })
        } else {
          auditOverview.value = null
        }
  
        if (auditTab.value === 'operations') {
          if (!canViewOperationAudit.value) {
            operationAudits.value = []
            pagination.value.total = 0
          } else {
            const res = await getAdminOperationAudits({
              pageNum,
              pageSize,
              operatorId: auditTargetUserId.value || undefined,
            })
            operationAudits.value = res?.records || []
            pagination.value.total = res?.total || 0
          }
        } else if (auditTab.value === 'permission') {
          if (!canViewPermissionAudit.value) {
            permissionChanges.value = []
            pagination.value.total = 0
          } else {
            const res = await getPermissionChanges({
              pageNum,
              pageSize,
              targetUserId: auditTargetUserId.value || undefined,
            })
            permissionChanges.value = res?.records || []
            pagination.value.total = res?.total || 0
          }
        } else {
          if (!canViewLoginAudit.value) {
            loginTraces.value = []
            pagination.value.total = 0
          } else {
            const res = await getLoginTraces({
              pageNum,
              pageSize,
              userId: auditTargetUserId.value || undefined,
            })
            loginTraces.value = res?.records || []
            pagination.value.total = res?.total || 0
          }
        }
      }
    } catch (error) {
      console.error(error)
    } finally {
      loading.value = false
    }
  }
  
  // 分页变化
  const handlePageChange = (page: number) => {
    pagination.value.pageNum = page
    loadData()
  }
  
  const handleSizeChange = (size: number) => {
    pagination.value.pageSize = size
    pagination.value.pageNum = 1
    loadData()
  }
  
  // 搜索
  const handleSearch = () => {
    pagination.value.pageNum = 1
    loadData()
  }
  
  const switchRiskTab = (tab: 'mode' | 'events' | 'cases' | 'rules' | 'behavior' | 'blacklist' | 'whitelist') => {
    if (!hasRiskTabAccess(tab)) {
      ElMessage.warning('当前账号无此模块权限')
      return
    }
    riskTab.value = tab
    pagination.value.pageNum = 1
    loadData()
  }
  
  const handleRiskSearch = () => {
    if (!hasRiskTabAccess(riskTab.value)) {
      ElMessage.warning('当前账号无此风控模块权限')
      return
    }
    if (!buildRiskTimeRangeParams(true)) {
      return
    }
    pagination.value.pageNum = 1
    loadData()
  }
  
  const handleUpdateRiskMode = async () => {
    if (!ensurePermission('risk:mode:manage')) {
      return
    }
    try {
      const { value: mode } = await ElMessageBox.prompt(
        '输入风控模式（OFF / BASIC / FULL）',
        '更新风控模式',
        {
          confirmButtonText: '提交',
          cancelButtonText: '取消',
          inputValue: riskMode.value,
          inputPlaceholder: '请输入 OFF / BASIC / FULL',
          inputValidator: (val) => !!val || '风控模式不能为空',
        },
      )
      await updateRiskMode({ mode })
      ElMessage.success('风控模式已更新')
      loadData()
    } catch (action) {
      if (action !== 'cancel') {
        ElMessage.error('更新风控模式失败')
      }
    }
  }

  const handleAddBlacklist = async () => {
    if (!ensurePermission('risk:list:manage')) {
      return
    }
    try {
      const { value: subjectType } = await ElMessageBox.prompt('输入主体类型（USER / IP / DEVICE / CONTENT）', '新增黑名单', {
        confirmButtonText: '下一步',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入主体类型',
        inputValidator: (val) => !!val || '主体类型不能为空',
      })
      const { value: subjectId } = await ElMessageBox.prompt('输入主体标识', '新增黑名单', {
        confirmButtonText: '下一步',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入用户ID / IP / 设备标识',
        inputValidator: (val) => !!val || '主体标识不能为空',
      })
      const { value: reason } = await ElMessageBox.prompt('可选：输入加入原因', '新增黑名单', {
        confirmButtonText: '提交',
        cancelButtonText: '跳过',
        inputPlaceholder: '例如：恶意刷消息',
      })
      await upsertBlacklist({
        subjectType,
        subjectId,
        reason: reason || undefined,
      })
      ElMessage.success('黑名单已保存')
      loadData()
    } catch (action) {
      if (action !== 'cancel') {
        ElMessage.error('保存黑名单失败')
      }
    }
  }

  const handleToggleBlacklist = async (item: RiskSubjectListItem) => {
    if (!ensurePermission('risk:list:manage')) {
      return
    }
    try {
      const nextStatus = item.status === 1 ? 0 : 1
      await updateBlacklistStatus(item.id, nextStatus)
      ElMessage.success(nextStatus === 1 ? '黑名单已启用' : '黑名单已禁用')
      loadData()
    } catch {
      ElMessage.error('切换黑名单状态失败')
    }
  }

  const handleAddWhitelist = async () => {
    if (!ensurePermission('risk:list:manage')) {
      return
    }
    try {
      const { value: subjectType } = await ElMessageBox.prompt('输入主体类型（USER / IP / DEVICE / CONTENT）', '新增白名单', {
        confirmButtonText: '下一步',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入主体类型',
        inputValidator: (val) => !!val || '主体类型不能为空',
      })
      const { value: subjectId } = await ElMessageBox.prompt('输入主体标识', '新增白名单', {
        confirmButtonText: '下一步',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入用户ID / IP / 设备标识',
        inputValidator: (val) => !!val || '主体标识不能为空',
      })
      const { value: reason } = await ElMessageBox.prompt('可选：输入加入原因', '新增白名单', {
        confirmButtonText: '提交',
        cancelButtonText: '跳过',
        inputPlaceholder: '例如：测试账号放行',
      })
      await upsertWhitelist({
        subjectType,
        subjectId,
        reason: reason || undefined,
      })
      ElMessage.success('白名单已保存')
      loadData()
    } catch (action) {
      if (action !== 'cancel') {
        ElMessage.error('保存白名单失败')
      }
    }
  }

  const handleToggleWhitelist = async (item: RiskSubjectListItem) => {
    if (!ensurePermission('risk:list:manage')) {
      return
    }
    try {
      const nextStatus = item.status === 1 ? 0 : 1
      await updateWhitelistStatus(item.id, nextStatus)
      ElMessage.success(nextStatus === 1 ? '白名单已启用' : '白名单已禁用')
      loadData()
    } catch {
      ElMessage.error('切换白名单状态失败')
    }
  }

  const handleBehaviorUserQuery = () => {
    if (!ensurePermission('admin:risk:behavior:view')) {
      return
    }
    if (!behaviorUserIdInput.value) {
      behaviorTargetUserId.value = null
      behaviorControls.value = []
      return
    }
    const userId = parsePositiveInt(behaviorUserIdInput.value)
    if (!userId) {
      ElMessage.warning('请输入合法的用户ID')
      return
    }
    behaviorTargetUserId.value = userId
    loadData()
  }
  
  const handleAddBehaviorControl = async () => {
    if (!ensurePermission('admin:risk:behavior:manage')) {
      return
    }
    if (!behaviorTargetUserId.value) {
      ElMessage.warning('请先查询目标用户ID')
      return
    }
    try {
      const { value: eventType } = await ElMessageBox.prompt(
        '输入行为类型（如 LOGIN / GOODS_PUBLISH / ERRAND_ACCEPT / CHAT_SEND / AI_CHAT_SEND / FOLLOW_USER / ALL）',
        '新增行为管控',
        {
          confirmButtonText: '下一步',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入行为类型',
          inputValidator: (val) => !!val || '行为类型不能为空',
        },
      )
      const { value: controlAction } = await ElMessageBox.prompt(
        '输入管控动作（ALLOW / REJECT / REVIEW / LIMIT / CHALLENGE）',
        '新增行为管控',
        {
          confirmButtonText: '继续',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入管控动作',
          inputValidator: (val) => !!val || '管控动作不能为空',
        },
      )
      const { value: reason } = await ElMessageBox.prompt(
        '可选：输入原因备注',
        '新增行为管控',
        {
          confirmButtonText: '提交',
          cancelButtonText: '跳过',
          inputPlaceholder: '例如：批量刷消息，先限制聊天',
        },
      )
      await upsertBehaviorControl({
        userId: behaviorTargetUserId.value,
        eventType,
        controlAction,
        reason: reason || undefined,
      })
      ElMessage.success('行为管控已保存')
      loadData()
    } catch (action) {
      if (action !== 'cancel') {
        ElMessage.error('保存行为管控失败')
      }
    }
  }
  
  const handleDisableBehavior = async (controlId: number) => {
    if (!ensurePermission('admin:risk:behavior:manage')) {
      return
    }
    try {
      await ElMessageBox.confirm('确认关闭该行为管控？', '提示', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
      })
      await disableBehaviorControl(controlId)
      ElMessage.success('已关闭行为管控')
      loadData()
    } catch (action) {
      if (action !== 'cancel') {
        ElMessage.error('操作失败')
      }
    }
  }
  
  const handleRiskCaseProcess = async (caseId: number) => {
    if (!ensurePermission('risk:case:handle')) {
      return
    }
    try {
      const { value: caseStatus } = await ElMessageBox.prompt(
        '输入工单状态（CLOSED / REJECTED / PROCESSING）',
        '处理风控工单',
        {
          confirmButtonText: '下一步',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入工单状态',
          inputValidator: (val) => !!val || '工单状态不能为空',
        },
      )
      const { value: result } = await ElMessageBox.prompt(
        '可选：输入处理结论（PASS / BLOCK / WARN）',
        '处理风控工单',
        {
          confirmButtonText: '继续',
          cancelButtonText: '跳过',
          inputPlaceholder: '如 PASS / BLOCK / WARN',
        },
      )
      const { value: resultReason } = await ElMessageBox.prompt(
        '可选：输入处理说明',
        '处理风控工单',
        {
          confirmButtonText: '提交',
          cancelButtonText: '跳过',
          inputPlaceholder: '请输入处理说明',
        },
      )
      await handleRiskCase({
        caseId,
        caseStatus,
        result: result || undefined,
        resultReason: resultReason || undefined,
      })
      ElMessage.success('风控工单已处理')
      loadData()
    } catch (action) {
      if (action !== 'cancel') {
        ElMessage.error('处理风控工单失败')
      }
    }
  }
  
  const handleToggleRiskRule = async (rule: RiskRuleItem) => {
    if (!ensurePermission('risk:rule:manage')) {
      return
    }
    try {
      const nextStatus = rule.status === 1 ? 0 : 1
      await updateRiskRuleStatus(rule.ruleId, nextStatus)
      ElMessage.success(nextStatus === 1 ? '规则已启用' : '规则已禁用')
      loadData()
    } catch {
      ElMessage.error('切换规则状态失败')
    }
  }
  
  const handleCreateRiskRule = async () => {
    if (!ensurePermission('risk:rule:manage')) {
      return
    }
    try {
      const { value: ruleCode } = await ElMessageBox.prompt('输入规则编码（唯一）', '新增风控规则', {
        confirmButtonText: '下一步',
        cancelButtonText: '取消',
        inputPlaceholder: '例如 RULE_CHAT_SPAM',
        inputValidator: (val) => !!val || '规则编码不能为空',
      })
      const { value: ruleName } = await ElMessageBox.prompt('输入规则名称', '新增风控规则', {
        confirmButtonText: '下一步',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入规则名称',
        inputValidator: (val) => !!val || '规则名称不能为空',
      })
      const { value: eventType } = await ElMessageBox.prompt('输入事件类型（如 CHAT_SEND）', '新增风控规则', {
        confirmButtonText: '下一步',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入事件类型',
        inputValidator: (val) => !!val || '事件类型不能为空',
      })
      const { value: ruleType } = await ElMessageBox.prompt('输入规则类型（THRESHOLD / KEYWORD）', '新增风控规则', {
        confirmButtonText: '下一步',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入规则类型',
        inputValidator: (val) => !!val || '规则类型不能为空',
      })
      const { value: decisionAction } = await ElMessageBox.prompt(
        '输入决策动作（ALLOW / REJECT / CHALLENGE / REVIEW / LIMIT）',
        '新增风控规则',
        {
          confirmButtonText: '下一步',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入决策动作',
          inputValidator: (val) => !!val || '决策动作不能为空',
        },
      )
      const { value: ruleConfig } = await ElMessageBox.prompt(
        '输入规则配置（JSON 字符串）',
        '新增风控规则',
        {
          confirmButtonText: '提交',
          cancelButtonText: '取消',
          inputPlaceholder: '{"maxCount": 5, "windowMinutes": 10}',
          inputValidator: (val) => !!val || '规则配置不能为空',
        },
      )
      await upsertRiskRule({
        ruleCode,
        ruleName,
        eventType,
        ruleType,
        decisionAction,
        ruleConfig,
        priority: 100,
      })
      ElMessage.success('风控规则已创建')
      loadData()
    } catch (action) {
      if (action !== 'cancel') {
        ElMessage.error('创建风控规则失败')
      }
    }
  }
  
  const handleIamUserQuery = () => {
    if (!canViewIamUserRole.value && !canViewIamScope.value) {
      ElMessage.warning('当前账号无IAM查询权限')
      return
    }
    if (!iamUserIdInput.value) {
      iamTargetUserId.value = null
      userRoleBindings.value = []
      adminScopeBindings.value = []
      return
    }
    const userId = parsePositiveInt(iamUserIdInput.value)
    if (!userId) {
      ElMessage.warning('请输入合法的用户ID')
      return
    }
    iamTargetUserId.value = userId
    loadData()
  }
  
  const handleGrantRole = async () => {
    if (!ensurePermission('admin:iam:user-role:manage')) {
      return
    }
    if (!iamTargetUserId.value) {
      ElMessage.warning('请先查询目标用户ID')
      return
    }
    try {
      const { value: roleCode } = await ElMessageBox.prompt('输入角色编码（如 ADMIN / RISK_OPERATOR）', '授予角色', {
        confirmButtonText: '下一步',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入角色编码',
        inputValidator: (val) => !!val || '角色编码不能为空',
      })
      const { value: reason } = await ElMessageBox.prompt('可选：输入授予原因', '授予角色', {
        confirmButtonText: '提交',
        cancelButtonText: '跳过',
        inputPlaceholder: '请输入原因',
      })
      await grantUserRole({
        userId: iamTargetUserId.value,
        roleCode,
        reason: reason || undefined,
      })
      ElMessage.success('角色授予成功')
      loadData()
    } catch (action) {
      if (action !== 'cancel') {
        ElMessage.error('角色授予失败')
      }
    }
  }
  
  const handleRevokeRole = async (bindingId: number) => {
    if (!ensurePermission('admin:iam:user-role:manage')) {
      return
    }
    try {
      await ElMessageBox.confirm('确认撤销该角色绑定？', '提示', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
      })
      await revokeUserRole(bindingId, '管理员手动撤销')
      ElMessage.success('角色绑定已撤销')
      loadData()
    } catch (action) {
      if (action !== 'cancel') {
        ElMessage.error('撤销角色失败')
      }
    }
  }
  
  const handleAddScope = async () => {
    if (!ensurePermission('admin:iam:scope:manage')) {
      return
    }
    if (!iamTargetUserId.value) {
      ElMessage.warning('请先查询目标用户ID')
      return
    }
    try {
      const { value: scopeType } = await ElMessageBox.prompt('输入范围类型（ALL / SCHOOL / CAMPUS）', '新增管理范围', {
        confirmButtonText: '下一步',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入范围类型',
        inputValidator: (val) => !!val || '范围类型不能为空',
      })
      let schoolCode: string | undefined
      let campusCode: string | undefined
      if (scopeType === 'SCHOOL' || scopeType === 'CAMPUS') {
        const school = await ElMessageBox.prompt('输入学校编码', '新增管理范围', {
          confirmButtonText: '下一步',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入学校编码',
          inputValidator: (val) => !!val || '学校编码不能为空',
        })
        schoolCode = school.value || undefined
      }
      if (scopeType === 'CAMPUS') {
        const campus = await ElMessageBox.prompt('输入校区编码', '新增管理范围', {
          confirmButtonText: '提交',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入校区编码',
          inputValidator: (val) => !!val || '校区编码不能为空',
        })
        campusCode = campus.value || undefined
      }
      await upsertAdminScope({
        userId: iamTargetUserId.value,
        scopeType,
        schoolCode,
        campusCode,
        reason: '管理员配置',
      })
      ElMessage.success('管理范围已保存')
      loadData()
    } catch (action) {
      if (action !== 'cancel') {
        ElMessage.error('保存管理范围失败')
      }
    }
  }
  
  const handleDisableScopeBinding = async (bindingId: number) => {
    if (!ensurePermission('admin:iam:scope:manage')) {
      return
    }
    try {
      await ElMessageBox.confirm('确认关闭该管理范围？', '提示', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
      })
      await disableAdminScope(bindingId, '管理员手动关闭')
      ElMessage.success('管理范围已关闭')
      loadData()
    } catch (action) {
      if (action !== 'cancel') {
        ElMessage.error('关闭管理范围失败')
      }
    }
  }
  
  const switchAuditTab = (tab: 'operations' | 'permission' | 'login') => {
    if (!hasAuditTabAccess(tab)) {
      ElMessage.warning('当前账号无此模块权限')
      return
    }
    auditTab.value = tab
    pagination.value.pageNum = 1
    loadData()
  }
  
  const handleAuditSearch = () => {
    if (!hasAuditTabAccess(auditTab.value)) {
      ElMessage.warning('当前账号无此审计模块权限')
      return
    }
    if (!auditUserIdInput.value) {
      auditTargetUserId.value = null
    } else {
      const userId = parsePositiveInt(auditUserIdInput.value)
      if (!userId) {
        ElMessage.warning('请输入合法的用户ID')
        return
      }
      auditTargetUserId.value = userId
    }
    pagination.value.pageNum = 1
    loadData()
  }
  
  // 切换菜单
  const switchMenu = (id: AdminMenuId) => {
    if (!hasMenuAccess(id)) {
      ElMessage.warning('当前账号无此模块权限')
      return
    }
  
    const targetPath = adminMenuRouteMap[id]
    if (route.path !== targetPath) {
      navigateToMenu(id)
      return
    }
  
    activeMenu.value = id
    pagination.value.pageNum = 1
    pagination.value.total = 0
    searchKeyword.value = ''
    loadData()
  }
  
  // 退出登录
  const handleLogout = () => {
    showNoticePanel.value = false
    showAvatarPanel.value = false
    userStore.logout()
    router.push('/login')
  }
  
  // 返回首页
  const handleGoHome = () => {
    showNoticePanel.value = false
    showAvatarPanel.value = false
    router.push('/')
  }

  const refreshDashboardStats = async () => {
    try {
      dashboardStats.value = await getDashboardStats()
    } catch (error) {
      console.error('获取仪表盘统计失败', error)
    }
  }
  
  // 刷新数据
  const handleRefresh = async () => {
    if (manualRefreshSpinning.value) {
      return
    }
    manualRefreshSpinning.value = true
    const startedAt = Date.now()
    try {
      await Promise.all([
        loadData(),
        activeMenu.value === 'dashboard' ? Promise.resolve() : refreshDashboardStats(),
      ])
    } finally {
      const elapsed = Date.now() - startedAt
      const remain = Math.max(0, 400 - elapsed)
      window.setTimeout(() => {
        manualRefreshSpinning.value = false
      }, remain)
    }
  }
  
  const openProductDetail = (productId: number) => {
    const target = router.resolve(`/product/${productId}`)
    window.open(target.href, '_blank', 'noopener')
  }
  
  const openErrandDetail = (taskId: number) => {
    const target = router.resolve(`/errand/${taskId}`)
    window.open(target.href, '_blank', 'noopener')
  }
  
  const openDisputeDetail = (recordId: number) => {
    const target = router.resolve(`/dispute/${recordId}`)
    window.open(target.href, '_blank', 'noopener')
  }
  
  const toggleNoticePanel = async () => {
    showAvatarPanel.value = false
    showNoticePanel.value = !showNoticePanel.value
    if (showNoticePanel.value) {
      await refreshDashboardStats()
    }
  }
  
  const toggleAvatarPanel = () => {
    showNoticePanel.value = false
    showAvatarPanel.value = !showAvatarPanel.value
  }
  
  const jumpByNotice = (menuId: AdminMenuId) => {
    showNoticePanel.value = false
    switchMenu(menuId)
  }
  
  const handleQuickGotoMessage = () => {
    showNoticePanel.value = false
    router.push('/message')
  }
  
  const handleQuickGotoProfile = () => {
    showAvatarPanel.value = false
    router.push('/profile')
  }
  
  const handleQuickLogout = () => {
    showAvatarPanel.value = false
    handleLogout()
  }
  
  const handleClickOutsidePanels = (event: MouseEvent) => {
    const target = event.target as Node | null
    if (!target) return
  
    if (showNoticePanel.value && noticePanelRef.value && !noticePanelRef.value.contains(target)) {
      showNoticePanel.value = false
    }
    if (showAvatarPanel.value && avatarPanelRef.value && !avatarPanelRef.value.contains(target)) {
      showAvatarPanel.value = false
    }
  }
  
  // --- 操作逻辑 ---
  
  // 商品审核
  const handleAuditGoods = async (goodsId: number, status: number) => {
    if (!ensurePermission('admin:goods:audit')) {
      return
    }
    const actionKey = `goods-audit:${goodsId}`
    if (!beginAdminAction(actionKey)) {
      return
    }
    try {
      await auditGoods({ goodsId, status })
      ElMessage.success('操作成功')
      loadData()
      void refreshDashboardStats()
    } catch {
      ElMessage.error('操作失败')
    } finally {
      endAdminAction(actionKey)
    }
  }
  
  // 认证审核
  const handleAuditAuth = async (userId: number, status: number) => {
    if (!ensurePermission('admin:auth:audit')) {
      return
    }
    const actionKey = `auth-audit:${userId}`
    if (!beginAdminAction(actionKey)) {
      return
    }
    try {
      await auditUserAuth({ userId, status })
      ElMessage.success('操作成功')
      loadData()
      void refreshDashboardStats()
    } catch {
      ElMessage.error('操作失败')
    } finally {
      endAdminAction(actionKey)
    }
  }
  
  // 跑腿员审核
  const handleAuditRunner = async (userId: number, status: number) => {
    if (!ensurePermission('admin:runner:audit')) {
      return
    }
    const actionKey = `runner-audit:${userId}`
    if (!beginAdminAction(actionKey)) {
      return
    }
    try {
      if (status === 0) {
        const { value: reason } = await ElMessageBox.prompt('请输入驳回原因', '驳回跑腿员申请', {
          confirmButtonText: '确认驳回',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入驳回原因（选填）',
        })
        await auditRunner({ userId, status, reason: reason || undefined })
      } else {
        await auditRunner({ userId, status })
      }
      ElMessage.success('操作成功')
      loadData()
      void refreshDashboardStats()
    } catch (action) {
      if (action !== 'cancel') {
        ElMessage.error('操作失败')
      }
    } finally {
      endAdminAction(actionKey)
    }
  }
  
  // 跑腿任务复核
  const handleAuditErrand = async (taskId: number, status: number) => {
    if (!ensurePermission('admin:errand:audit')) {
      return
    }
    const actionKey = `errand-audit:${taskId}`
    if (!beginAdminAction(actionKey)) {
      return
    }
    try {
      if (status === 2) {
        const { value: reason } = await ElMessageBox.prompt('请输入驳回原因', '驳回跑腿任务', {
          confirmButtonText: '确认驳回',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入驳回原因（必填）',
          inputValidator: (value) => {
            if (!value || !value.trim()) return '驳回原因不能为空'
            return true
          },
        })
        await auditErrand({ taskId, status, reason: (reason || '').trim() })
      } else {
        await auditErrand({ taskId, status })
      }
      ElMessage.success('复核操作已提交')
      loadData()
      void refreshDashboardStats()
    } catch (action) {
      if (action !== 'cancel') {
        ElMessage.error('复核操作失败')
      }
    } finally {
      endAdminAction(actionKey)
    }
  }
  
  // 用户封禁
  const handleToggleUser = async (user: UserInfo) => {
    if (!ensurePermission('admin:user:status:update')) {
      return
    }
    const newStatus = user.accountStatus === 0 ? 1 : 0
    try {
      await updateUserStatus({ userId: user.userId, status: newStatus })
      ElMessage.success('操作成功')
      loadData()
    } catch {
      ElMessage.error('操作失败')
    }
  }
  
  // 查看用户详情
  const handleViewUserDetail = async (userId: number) => {
    try {
      const res = await getUserDetail(userId)
      userDetailData.value = res
      creditChangeForm.value = { change: 0, reason: '' }
      showUserDetailDialog.value = true
    } catch {
      ElMessage.error('获取用户详情失败')
    }
  }
  
  // 调整信用分
  const handleAdjustCredit = async () => {
    if (!userDetailData.value) return
    if (!creditChangeForm.value.reason) {
      ElMessage.warning('请输入调整原因')
      return
    }
    if (creditChangeForm.value.change === 0) {
      ElMessage.warning('调整分值不能为0')
      return
    }
    try {
      await adjustUserCredit({
        userId: userDetailData.value.userId,
        change: creditChangeForm.value.change,
        reason: creditChangeForm.value.reason,
      })
      ElMessage.success('信用分调整成功')
      // 刷新详情
      handleViewUserDetail(userDetailData.value.userId)
      loadData()
    } catch {
      ElMessage.error('信用分调整失败')
    }
  }
  
  // 系统通知广播
  const handleBroadcastNotice = async () => {
    if (!broadcastForm.value.title || !broadcastForm.value.content) {
      ElMessage.warning('请填写通知标题和内容')
      return
    }
    try {
      await broadcastNotice(broadcastForm.value)
      ElMessage.success('系统通知已广播')
      showBroadcastDialog.value = false
      broadcastForm.value = { title: '', content: '' }
    } catch {
      ElMessage.error('广播通知失败')
    }
  }
  
  // 搜索管理操作
  const handleSyncGoodsFull = async () => {
    searchOpLoading.value = true
    try {
      await syncGoodsSearchFull()
      ElMessage.success('商品全量同步已完成')
    } catch {
      ElMessage.error('商品全量同步失败')
    } finally {
      searchOpLoading.value = false
    }
  }
  
  const handleCreateGoodsIndex = async () => {
    searchOpLoading.value = true
    try {
      await createGoodsSearchIndex()
      ElMessage.success('商品索引重建已完成')
    } catch {
      ElMessage.error('商品索引重建失败')
    } finally {
      searchOpLoading.value = false
    }
  }
  
  const handleSyncErrandFull = async () => {
    searchOpLoading.value = true
    try {
      await syncErrandSearchFull()
      ElMessage.success('跑腿全量同步已完成')
    } catch {
      ElMessage.error('跑腿全量同步失败')
    } finally {
      searchOpLoading.value = false
    }
  }
  
  const handleCreateErrandIndex = async () => {
    searchOpLoading.value = true
    try {
      await createErrandSearchIndex()
      ElMessage.success('跑腿索引重建已完成')
    } catch {
      ElMessage.error('跑腿索引重建失败')
    } finally {
      searchOpLoading.value = false
    }
  }
  
  // 强制下架商品
  const handleForceOffline = async (goodsId: number) => {
    if (!ensurePermission('admin:goods:offline')) {
      return
    }
    try {
      const { value: reason } = await ElMessageBox.prompt('请输入下架原因', '强制下架', {
        confirmButtonText: '确认下架',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入下架原因（选填）',
      })
      await forceOfflineGoods({ goodsId, reason: reason || '管理员操作' })
      ElMessage.success('下架成功')
      loadData()
    } catch (action) {
      if (action !== 'cancel') {
        ElMessage.error('操作失败')
      }
    }
  }
  
  const closeDisputeHandleDialog = () => {
    showDisputeHandleDialog.value = false
    disputeHandleDialogLoading.value = false
    disputeHandleSubmitting.value = false
    disputeHandleDetail.value = null
    disputeHandleForm.value = {
      disputeId: 0,
      handleStatus: 2,
      result: '',
      deductCreditScoreText: '',
      refundAmountText: '',
    }
  }

  // 处理纠纷（打开合并表单弹窗）
  const handleDisputeAction = async (disputeId: number) => {
    if (!ensurePermission('admin:dispute:handle')) {
      return
    }
    const actionKey = `dispute-open:${disputeId}`
    if (!beginAdminAction(actionKey)) {
      return
    }

    disputeHandleDetail.value = null
    disputeHandleForm.value = {
      disputeId,
      handleStatus: 2,
      result: '',
      deductCreditScoreText: '',
      refundAmountText: '',
    }

    showDisputeHandleDialog.value = true
    disputeHandleDialogLoading.value = true
    try {
      disputeHandleDetail.value = await getDisputeDetailForAdmin(disputeId)
    } catch (error) {
      console.warn('获取纠纷详情失败（将使用简化处理流程）', error)
    } finally {
      disputeHandleDialogLoading.value = false
      endAdminAction(actionKey)
    }
  }

  const submitDisputeHandle = async () => {
    if (disputeHandleSubmitting.value) {
      return
    }

    const disputeId = Number(disputeHandleForm.value.disputeId)
    if (!Number.isFinite(disputeId) || disputeId <= 0) {
      ElMessage.warning('纠纷编号无效')
      return
    }

    const handleStatus = disputeHandleForm.value.handleStatus === 3 ? 3 : 2
    const result = String(disputeHandleForm.value.result || '').trim()
    if (!result) {
      ElMessage.warning('处理结果不能为空')
      return
    }

    let deductCreditScore: number | undefined
    if (canDisputeDeductCredit.value) {
      const trimmed = String(disputeHandleForm.value.deductCreditScoreText ?? '').trim()
      if (trimmed) {
        const parsed = Number(trimmed)
        if (!Number.isFinite(parsed) || parsed <= 0) {
          ElMessage.warning('扣分请输入正整数，留空表示不扣分')
          return
        }
        deductCreditScore = Math.floor(parsed)
      }
    }

    let refundAmount: number | undefined
    if (canDisputeRefund.value) {
      const trimmed = String(disputeHandleForm.value.refundAmountText ?? '').trim()
      if (trimmed) {
        const parsed = Number(trimmed)
        if (!Number.isFinite(parsed) || parsed <= 0) {
          ElMessage.warning('退款金额请输入合法金额，留空表示不退款')
          return
        }
        const maxRefund = disputeHandleMaxRefund.value
        if (maxRefund != null && parsed > maxRefund) {
          ElMessage.warning(`退款金额不能超过 ¥${maxRefund}`)
          return
        }
        refundAmount = parsed
      }
    }

    disputeHandleSubmitting.value = true
    try {
      await handleDispute({ disputeId, result, handleStatus, deductCreditScore, refundAmount })
      ElMessage.success('处理成功')
      closeDisputeHandleDialog()
      loadData()
      void refreshDashboardStats()
    } catch (error) {
      console.error(error)
      ElMessage.error('操作失败')
    } finally {
      disputeHandleSubmitting.value = false
    }
  }
  
  // 当前时间
  const currentTime = ref(new Date())
  const clockTimer = window.setInterval(() => {
    currentTime.value = new Date()
  }, 1000)
  
  const formattedTime = computed(() => {
    return currentTime.value.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    })
  })
  
  onMounted(() => {
    ensureActiveMenu()
    ensureRiskTab()
    ensureAuditTab()
    document.addEventListener('click', handleClickOutsidePanels)
    fetchSchools()
  
    const expectedPath = adminMenuRouteMap[activeMenu.value]
    if (route.path !== expectedPath) {
      navigateToMenu(activeMenu.value, true)
      return
    }
  
    loadData()
  })
  
  watch(routeMenuId, (nextMenu, prevMenu) => {
    showNoticePanel.value = false
    showAvatarPanel.value = false
    if (nextMenu === prevMenu) {
      return
    }
  
    if (!hasMenuAccess(nextMenu)) {
      ensureActiveMenu()
      return
    }
  
    activeMenu.value = nextMenu
    if (Object.keys(route.query).length === 0) {
      pagination.value.pageNum = 1
      searchKeyword.value = ''
    }
    pagination.value.total = 0
    loadData()
  })
  
  watch(visibleMenus, (nextMenus, prevMenus) => {
    ensureActiveMenu()
  
    const expectedPath = adminMenuRouteMap[activeMenu.value]
    if (nextMenus.length > 0 && route.path !== expectedPath) {
      navigateToMenu(activeMenu.value, true)
      return
    }
  
    if (prevMenus.length === 0 && nextMenus.length > 0 && route.path === expectedPath) {
      loadData()
    }
  })
  
  onBeforeUnmount(() => {
    document.removeEventListener('click', handleClickOutsidePanels)
    window.clearInterval(clockTimer)
  })

  return {
    // 资源与组件（供 SFC template 直接使用）
    logoSvg,
    Bell,
    ChevronRight,
    Home,
    LogOut,
    RefreshCw,
    XCircle,
    AdminDashboardOverviewPanel,
    AdminGoodsAuditPanel,
    AdminAuthAuditPanel,
    AdminRunnerAuditPanel,
    AdminGoodsManagePanel,
    AdminUserManagePanel,
    AdminOrderManagePanel,
    AdminErrandManagePanel,
    AdminDisputeManagePanel,
    AdminRiskCenterPanel,
    AdminIamCenterPanel,
    AdminSearchManagePanel,
    AdminAuditCenterPanel,

    userStore,
    activeMenu,
    loading,
    sidebarCollapsed,
    manualRefreshSpinning,

    dashboardStats,
    animatedStats,

    pagination,
    searchKeyword,
    filterSchoolCode,
    filterCampusCode,
    goodsAuditReviewStatus,
    goodsTradeStatus,
    goodsReviewStatus,
    userAccountStatus,
    userAuthStatus,
    orderStatusFilter,
    errandStatusFilter,
    errandReviewStatusFilter,
    disputeStatusFilter,
    schoolList,
    campusList,

    showNoticePanel,
    showAvatarPanel,
    noticePanelRef,
    avatarPanelRef,

    pendingGoods,
    pendingAuthUsers,
    pendingRunnerUsers,
    allGoods,
    allUsers,
    allOrders,
    disputes,
    allErrands,
    riskMode,
    riskEvents,
    riskCases,
    riskRules,
    behaviorControls,
    blacklistItems,
    whitelistItems,
    iamRoles,
    userRoleBindings,
    adminScopeBindings,
    operationAudits,
    permissionChanges,
    loginTraces,
    auditOverview,

    searchOpLoading,
    isGoodsAuditPending,
    isAuthAuditPending,
    isRunnerAuditPending,
    isErrandAuditPending,
    isDisputeHandleOpeningPending,

    showUserDetailDialog,
    userDetailData,
    creditChangeForm,
    showBroadcastDialog,
    broadcastForm,
    showDisputeHandleDialog,
    disputeHandleDialogLoading,
    disputeHandleSubmitting,
    disputeHandleDetail,
    disputeHandleForm,
    disputeHandleMaxRefund,
    canDisputeDeductCredit,
    canDisputeRefund,
    disputeHandleClaimSummary,

    riskTab,
    auditTab,
    riskTabOptions,
    riskSubjectTypeFilter,
    riskSubjectIdFilter,
    riskEventTypeFilter,
    riskDecisionActionFilter,
    riskLevelFilter,
    riskCaseStatusFilter,
    riskStartTimeFilter,
    riskEndTimeFilter,
    behaviorUserIdInput,
    iamUserIdInput,
    iamTargetUserId,
    auditUserIdInput,

    visibleMenus,
    roleLabel,
    formattedTime,
    pendingQuickActions,
    pendingNoticeCount,

    canSelectSchool,
    canSelectCampus,
    isCampusAdmin,
    effectiveSchoolCode,
    canViewRiskMode,
    canManageRiskMode,
    canViewRiskList,
    canManageRiskList,
    canViewRiskEvents,
    canHandleRiskCase,
    canManageRiskRule,
    canViewBehaviorControl,
    canManageBehaviorControl,
    canViewIamRole,
    canViewIamUserRole,
    canManageIamUserRole,
    canViewIamScope,
    canManageIamScope,
    canViewOperationAudit,
    canViewPermissionAudit,
    canViewLoginAudit,
    canAuditErrand,

    formatSchoolCampus,
    getMenuBadge,
    switchMenu,
    switchRiskTab,
    switchAuditTab,
    toggleNoticePanel,
    toggleAvatarPanel,
    jumpByNotice,

    handleSchoolChange,
    handleCampusChange,
    handlePageChange,
    handleSizeChange,
    handleSearch,
    handleRefresh,
    handleRiskSearch,
    handleUpdateRiskMode,
    handleAddBlacklist,
    handleToggleBlacklist,
    handleAddWhitelist,
    handleToggleWhitelist,
    handleBehaviorUserQuery,
    handleAddBehaviorControl,
    handleDisableBehavior,
    handleRiskCaseProcess,
    handleToggleRiskRule,
    handleCreateRiskRule,
    handleIamUserQuery,
    handleGrantRole,
    handleRevokeRole,
    handleAddScope,
    handleDisableScopeBinding,
    handleAuditSearch,
    handleLogout,
    handleGoHome,
    handleQuickGotoMessage,
    handleQuickGotoProfile,
    handleQuickLogout,

    openProductDetail,
    openErrandDetail,
    openDisputeDetail,

    handleAuditGoods,
    handleAuditAuth,
    handleAuditRunner,
    handleAuditErrand,
    handleForceOffline,
    handleDisputeAction,
    handleToggleUser,
    handleViewUserDetail,
    handleAdjustCredit,
    handleBroadcastNotice,
    closeDisputeHandleDialog,
    submitDisputeHandle,
    handleSyncGoodsFull,
    handleCreateGoodsIndex,
    handleSyncErrandFull,
    handleCreateErrandIndex,
  }
}












