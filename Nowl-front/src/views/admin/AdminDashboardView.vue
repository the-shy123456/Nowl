<script setup lang="ts">
import { useAdminDashboard } from './useAdminDashboard'

const {
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
} = useAdminDashboard()
</script>
<template>
  <div class="admin-owl min-h-screen flex">
    <!-- Sidebar -->
    <aside
      class="admin-sidebar fixed h-full z-30 transition-all duration-300 ease-in-out"
      :class="sidebarCollapsed ? 'w-20' : 'w-72'"
    >
      <!-- 背景装饰 -->
      <div class="absolute inset-0 bg-gradient-to-b from-[#4e342e] via-[#5d4037] to-[#3e2723]"></div>
      <div class="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHZpZXdCb3g9IjAgMCA2MCA2MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZyBmaWxsPSJub25lIiBmaWxsLXJ1bGU9ImV2ZW5vZGQiPjxnIGZpbGw9IiNmZmYiIGZpbGwtb3BhY2l0eT0iMC4wMyI+PHBhdGggZD0iTTM2IDM0djItSDI0di0yaDEyek0zNiAyNHYySDI0di0yaDEyeiIvPjwvZz48L2c+PC9zdmc+')] opacity-50"></div>

      <div class="relative flex flex-col h-full text-white">
        <!-- Logo -->
        <div class="p-6 flex items-center gap-4 border-b border-white/10">
          <div class="relative">
            <div class="w-12 h-12 bg-gradient-to-br from-amber-400 via-orange-500 to-amber-600 rounded-2xl flex items-center justify-center shadow-lg shadow-amber-500/35 transform hover:scale-105 transition-transform p-2">
              <img :src="logoSvg" alt="Nowl" class="w-full h-full object-contain drop-shadow-sm" />
            </div>
            <div class="absolute -bottom-1 -right-1 w-4 h-4 bg-emerald-500 rounded-full border-2 border-slate-900 animate-pulse"></div>
          </div>
          <div v-if="!sidebarCollapsed" class="flex-1">
            <h1 class="font-bold text-xl tracking-wide bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
              Nowl Admin
            </h1>
            <p class="text-xs text-amber-100/80 mt-0.5">猫头鹰管理控制台</p>
          </div>
        </div>

        <!-- 菜单 -->
        <nav class="flex-1 p-4 space-y-1.5 overflow-y-auto scrollbar-thin">
          <button
            v-for="menu in visibleMenus"
            :key="menu.id"
            @click="switchMenu(menu.id)"
            class="group w-full flex items-center gap-3 px-4 py-3.5 rounded-2xl transition-all duration-200 relative overflow-hidden"
            :class="activeMenu === menu.id
              ? 'bg-gradient-to-r from-warm-500 to-warm-400 text-white shadow-lg shadow-warm-500/25'
              : 'text-slate-400 hover:bg-white/5 hover:text-white'"
          >
            <!-- 活跃状态装饰 -->
            <div
              v-if="activeMenu === menu.id"
              class="absolute inset-0 bg-gradient-to-r from-white/10 to-transparent"
            ></div>

            <component
              :is="menu.icon"
              :size="20"
              class="relative z-10 transition-transform group-hover:scale-110"
              :class="activeMenu === menu.id ? 'text-white' : ''"
            />
            <span
              v-if="!sidebarCollapsed"
              class="relative z-10 text-sm font-medium flex-1 text-left"
            >
              {{ menu.label }}
            </span>

            <!-- 徽章 -->
            <span
              v-if="!sidebarCollapsed && menu.badge && getMenuBadge(menu.badge) > 0"
              class="relative z-10 min-w-[22px] h-[22px] flex items-center justify-center text-xs font-bold rounded-full"
              :class="activeMenu === menu.id
                ? 'bg-white/25 text-white'
                : 'bg-warm-500/20 text-warm-400'"
            >
              {{ getMenuBadge(menu.badge) }}
            </span>

            <ChevronRight
              v-if="!sidebarCollapsed"
              :size="16"
              class="relative z-10 opacity-0 -translate-x-2 group-hover:opacity-100 group-hover:translate-x-0 transition-all"
              :class="activeMenu === menu.id ? 'opacity-100 translate-x-0' : ''"
            />
          </button>
        </nav>

        <!-- 底部操作 -->
        <div class="p-4 border-t border-white/10 space-y-2">
          <button
            @click="handleGoHome"
            class="w-full flex items-center gap-3 px-4 py-3 text-slate-400 hover:text-white hover:bg-white/5 rounded-xl transition-all group"
          >
            <Home :size="18" class="group-hover:scale-110 transition-transform" />
            <span v-if="!sidebarCollapsed" class="text-sm">返回前台</span>
          </button>
          <button
            @click="handleLogout"
            class="w-full flex items-center gap-3 px-4 py-3 text-slate-400 hover:text-red-400 hover:bg-red-500/10 rounded-xl transition-all group"
          >
            <LogOut :size="18" class="group-hover:scale-110 transition-transform" />
            <span v-if="!sidebarCollapsed" class="text-sm">退出登录</span>
          </button>
        </div>
      </div>
    </aside>

    <!-- Main Content -->
    <main
      class="flex-1 transition-all duration-300"
      :class="sidebarCollapsed ? 'ml-20' : 'ml-72'"
    >
      <!-- Topbar -->
      <header class="admin-topbar sticky top-0 z-20">
        <div class="flex justify-between items-center px-8 py-4">
          <div class="flex items-center gap-4">
            <button
              @click="sidebarCollapsed = !sidebarCollapsed"
              class="p-2 hover:bg-warm-100/80 rounded-xl transition-colors"
            >
              <svg class="w-5 h-5 text-slate-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            </button>
            <div>
              <h2 class="text-xl font-bold text-slate-800">
                {{ visibleMenus.find(m => m.id === activeMenu)?.label || '管理后台' }}
              </h2>
              <p class="text-xs text-slate-400 mt-0.5">{{ formattedTime }}</p>
            </div>
          </div>

          <div class="flex items-center gap-3">
            <!-- 刷新按钮 -->
            <button
              @click="handleRefresh"
              :disabled="manualRefreshSpinning || loading"
              class="p-2.5 hover:bg-warm-100/80 rounded-xl transition-colors group disabled:opacity-60"
            >
              <RefreshCw
                :size="18"
                :class="manualRefreshSpinning || loading ? 'animate-spin text-warm-700' : 'text-slate-500 group-hover:text-warm-700'"
              />
            </button>

            <!-- 通知 -->
            <div ref="noticePanelRef" class="relative">
              <button
                @click.stop="toggleNoticePanel"
                class="relative p-2.5 hover:bg-warm-100/80 rounded-xl transition-colors"
              >
                <Bell :size="18" class="text-slate-500" />
                <span
                  v-if="pendingNoticeCount > 0"
                  class="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full bg-warm-500 text-white text-[10px] flex items-center justify-center font-bold"
                >
                  {{ pendingNoticeCount > 99 ? '99+' : pendingNoticeCount }}
                </span>
                <span
                  v-else
                  class="absolute top-1.5 right-1.5 w-2 h-2 bg-emerald-400 rounded-full"
                ></span>
              </button>

              <div
                v-if="showNoticePanel"
                class="absolute right-0 mt-3 w-80 bg-white rounded-2xl border border-warm-100 shadow-xl shadow-warm-100/50 p-4 z-50"
                @click.stop
              >
                <div class="flex items-center justify-between mb-3">
                  <p class="text-sm font-bold text-slate-700">待处理消息</p>
                  <button
                    @click="handleQuickGotoMessage"
                    class="text-xs text-warm-600 hover:text-warm-700 font-medium"
                  >
                    消息中心
                  </button>
                </div>
                <div v-if="pendingQuickActions.length === 0" class="py-5 text-center text-sm text-slate-400">
                  当前没有待处理事项
                </div>
                <div v-else class="space-y-2">
                  <button
                    v-for="item in pendingQuickActions"
                    :key="item.menu"
                    @click="jumpByNotice(item.menu)"
                    class="w-full flex items-center justify-between px-3 py-2.5 rounded-xl bg-warm-50 hover:bg-warm-100 transition-colors"
                  >
                    <span class="text-sm text-slate-700">{{ item.label }}</span>
                    <span class="text-xs font-bold text-warm-600 bg-white border border-warm-200 px-2 py-0.5 rounded-lg">
                      {{ item.count }}
                    </span>
                  </button>
                </div>
              </div>
            </div>

            <!-- 分隔线 -->
            <div class="w-px h-8 bg-warm-200"></div>

            <!-- 用户信息 -->
            <div ref="avatarPanelRef" class="relative pl-2">
              <button @click.stop="toggleAvatarPanel" class="flex items-center gap-3 rounded-xl px-2 py-1.5 hover:bg-warm-100/70 transition-colors">
                <div class="text-right">
                  <p class="text-sm font-semibold text-slate-700">{{ userStore.userInfo?.nickName }}</p>
                  <p class="text-xs text-slate-400">{{ roleLabel }}</p>
                </div>
                <div class="relative">
                  <img
                    :src="userStore.userInfo?.imageUrl || '/avatar-placeholder.svg'"
                    class="w-10 h-10 rounded-xl object-cover ring-2 ring-warm-100"
                  />
                  <div class="absolute -bottom-0.5 -right-0.5 w-3.5 h-3.5 bg-emerald-500 rounded-full border-2 border-white"></div>
                </div>
              </button>

              <div
                v-if="showAvatarPanel"
                class="absolute right-0 mt-3 w-80 bg-white rounded-2xl border border-warm-100 shadow-xl shadow-warm-100/50 p-4 z-50"
                @click.stop
              >
                <div class="flex items-center gap-3 pb-4 border-b border-warm-100">
                  <img :src="userStore.userInfo?.imageUrl || '/avatar-placeholder.svg'" class="w-12 h-12 rounded-xl object-cover ring-2 ring-warm-100" />
                  <div class="min-w-0">
                    <p class="text-sm font-bold text-slate-700 truncate">{{ userStore.userInfo?.nickName }}</p>
                    <p class="text-xs text-slate-400 mt-0.5">{{ roleLabel }}</p>
                    <p class="text-xs text-slate-400 mt-1 truncate">
                      {{ formatSchoolCampus(userStore.userInfo?.schoolName, userStore.userInfo?.schoolCode, userStore.userInfo?.campusName, userStore.userInfo?.campusCode) }}
                    </p>
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-2 mt-4">
                  <div class="rounded-xl bg-warm-50 border border-warm-100 px-3 py-2">
                    <p class="text-[11px] text-slate-400">信用分</p>
                    <p class="text-sm font-bold text-warm-600">{{ userStore.userInfo?.creditScore ?? '-' }}</p>
                  </div>
                  <div class="rounded-xl bg-emerald-50 border border-emerald-100 px-3 py-2">
                    <p class="text-[11px] text-slate-400">账户余额</p>
                    <p class="text-sm font-bold text-emerald-600">¥{{ userStore.userInfo?.money ?? 0 }}</p>
                  </div>
                </div>

                <div class="mt-4 space-y-2">
                  <button @click="handleQuickGotoProfile" class="w-full text-left px-3 py-2.5 rounded-xl bg-warm-50 hover:bg-warm-100 text-sm text-slate-700 transition-colors">
                    个人资料
                  </button>
                  <button @click="handleGoHome" class="w-full text-left px-3 py-2.5 rounded-xl bg-warm-50 hover:bg-warm-100 text-sm text-slate-700 transition-colors">
                    返回前台
                  </button>
                  <button @click="handleQuickLogout" class="w-full text-left px-3 py-2.5 rounded-xl bg-red-50 hover:bg-red-100 text-sm text-red-600 transition-colors">
                    退出登录
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </header>

      <!-- Content Area -->
      <div class="p-8">
        <div class="admin-content-card p-8 min-h-[calc(100vh-180px)]">

          <!-- Loading -->
          <div v-if="loading" class="flex flex-col items-center justify-center py-32">
            <div class="relative">
              <div class="w-16 h-16 border-4 border-warm-100 rounded-full"></div>
              <div class="absolute inset-0 w-16 h-16 border-4 border-transparent border-t-warm-500 rounded-full animate-spin"></div>
            </div>
            <p class="mt-4 text-slate-400 text-sm">加载中...</p>
          </div>

          <div v-else-if="visibleMenus.length === 0" class="py-28 text-center">
            <p class="text-lg font-semibold text-slate-700">当前账号暂未开通后台模块权限</p>
            <p class="mt-2 text-sm text-slate-400">请联系超级管理员分配角色或权限点</p>
          </div>

          <!-- 0. 数据概览仪表盘 -->
          <AdminDashboardOverviewPanel
            v-else-if="activeMenu === 'dashboard'"
            :animated-stats="animatedStats"
            :dashboard-stats="dashboardStats"
            @switch-menu="switchMenu"
          />

          <AdminGoodsAuditPanel
            v-else-if="activeMenu === 'goods-audit'"
            v-model:filter-school-code="filterSchoolCode"
            v-model:filter-campus-code="filterCampusCode"
            v-model:goods-audit-review-status="goodsAuditReviewStatus"
            :school-list="schoolList"
            :campus-list="campusList"
            :can-select-school="canSelectSchool"
            :can-select-campus="canSelectCampus"
            :is-campus-admin="isCampusAdmin"
            :effective-school-code="effectiveSchoolCode"
            :pending-goods="pendingGoods"
            :is-audit-pending="isGoodsAuditPending"
            :page-num="pagination.pageNum"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            @school-change="handleSchoolChange"
            @campus-change="handleCampusChange"
            @search="handleSearch"
            @page-change="handlePageChange"
            @size-change="handleSizeChange"
            @open-product-detail="openProductDetail"
            @audit-goods="handleAuditGoods"
          />

          <AdminAuthAuditPanel
            v-else-if="activeMenu === 'auth-audit'"
            v-model:filter-school-code="filterSchoolCode"
            v-model:filter-campus-code="filterCampusCode"
            :school-list="schoolList"
            :campus-list="campusList"
            :can-select-school="canSelectSchool"
            :can-select-campus="canSelectCampus"
            :is-campus-admin="isCampusAdmin"
            :effective-school-code="effectiveSchoolCode"
            :pending-auth-users="pendingAuthUsers"
            :is-audit-pending="isAuthAuditPending"
            :page-num="pagination.pageNum"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            @school-change="handleSchoolChange"
            @campus-change="handleCampusChange"
            @audit-auth="handleAuditAuth"
            @page-change="handlePageChange"
            @size-change="handleSizeChange"
          />

          <AdminRunnerAuditPanel
            v-else-if="activeMenu === 'runner-audit'"
            v-model:filter-school-code="filterSchoolCode"
            v-model:filter-campus-code="filterCampusCode"
            :school-list="schoolList"
            :campus-list="campusList"
            :can-select-school="canSelectSchool"
            :can-select-campus="canSelectCampus"
            :is-campus-admin="isCampusAdmin"
            :effective-school-code="effectiveSchoolCode"
            :pending-runner-users="pendingRunnerUsers"
            :is-audit-pending="isRunnerAuditPending"
            :page-num="pagination.pageNum"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            @school-change="handleSchoolChange"
            @campus-change="handleCampusChange"
            @audit-runner="handleAuditRunner"
            @page-change="handlePageChange"
            @size-change="handleSizeChange"
          />

          <AdminGoodsManagePanel
            v-else-if="activeMenu === 'goods-manage'"
            v-model:search-keyword="searchKeyword"
            v-model:filter-school-code="filterSchoolCode"
            v-model:filter-campus-code="filterCampusCode"
            v-model:goods-trade-status="goodsTradeStatus"
            v-model:goods-review-status="goodsReviewStatus"
            :school-list="schoolList"
            :campus-list="campusList"
            :can-select-school="canSelectSchool"
            :can-select-campus="canSelectCampus"
            :is-campus-admin="isCampusAdmin"
            :effective-school-code="effectiveSchoolCode"
            :all-goods="allGoods"
            :page-num="pagination.pageNum"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            @school-change="handleSchoolChange"
            @campus-change="handleCampusChange"
            @search="handleSearch"
            @page-change="handlePageChange"
            @size-change="handleSizeChange"
            @open-product-detail="openProductDetail"
            @force-offline="handleForceOffline"
          />

          <AdminUserManagePanel
            v-else-if="activeMenu === 'user-manage'"
            v-model:search-keyword="searchKeyword"
            v-model:filter-school-code="filterSchoolCode"
            v-model:filter-campus-code="filterCampusCode"
            v-model:user-account-status="userAccountStatus"
            v-model:user-auth-status="userAuthStatus"
            :school-list="schoolList"
            :campus-list="campusList"
            :can-select-school="canSelectSchool"
            :can-select-campus="canSelectCampus"
            :is-campus-admin="isCampusAdmin"
            :effective-school-code="effectiveSchoolCode"
            :all-users="allUsers"
            :page-num="pagination.pageNum"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            @school-change="handleSchoolChange"
            @campus-change="handleCampusChange"
            @search="handleSearch"
            @open-broadcast="showBroadcastDialog = true"
            @view-user-detail="handleViewUserDetail"
            @toggle-user="handleToggleUser"
            @page-change="handlePageChange"
            @size-change="handleSizeChange"
          />

          <AdminOrderManagePanel
            v-else-if="activeMenu === 'order-manage'"
            v-model:search-keyword="searchKeyword"
            v-model:filter-school-code="filterSchoolCode"
            v-model:filter-campus-code="filterCampusCode"
            v-model:order-status-filter="orderStatusFilter"
            :school-list="schoolList"
            :campus-list="campusList"
            :can-select-school="canSelectSchool"
            :can-select-campus="canSelectCampus"
            :is-campus-admin="isCampusAdmin"
            :effective-school-code="effectiveSchoolCode"
            :all-orders="allOrders"
            :page-num="pagination.pageNum"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            @school-change="handleSchoolChange"
            @campus-change="handleCampusChange"
            @search="handleSearch"
            @page-change="handlePageChange"
            @size-change="handleSizeChange"
            @open-product-detail="openProductDetail"
          />

          <AdminErrandManagePanel
            v-else-if="activeMenu === 'errand-manage'"
            v-model:search-keyword="searchKeyword"
            v-model:filter-school-code="filterSchoolCode"
            v-model:filter-campus-code="filterCampusCode"
            v-model:errand-status-filter="errandStatusFilter"
            v-model:errand-review-status-filter="errandReviewStatusFilter"
            :school-list="schoolList"
            :campus-list="campusList"
            :can-select-school="canSelectSchool"
            :can-select-campus="canSelectCampus"
            :is-campus-admin="isCampusAdmin"
            :effective-school-code="effectiveSchoolCode"
            :can-audit-errand="canAuditErrand"
            :all-errands="allErrands"
            :is-audit-pending="isErrandAuditPending"
            :page-num="pagination.pageNum"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            @school-change="handleSchoolChange"
            @campus-change="handleCampusChange"
            @search="handleSearch"
            @audit-errand="handleAuditErrand"
            @open-errand-detail="openErrandDetail"
            @page-change="handlePageChange"
            @size-change="handleSizeChange"
          />

          <AdminDisputeManagePanel
            v-else-if="activeMenu === 'dispute-manage'"
            v-model:filter-school-code="filterSchoolCode"
            v-model:filter-campus-code="filterCampusCode"
            v-model:dispute-status-filter="disputeStatusFilter"
            :school-list="schoolList"
            :campus-list="campusList"
            :can-select-school="canSelectSchool"
            :can-select-campus="canSelectCampus"
            :is-campus-admin="isCampusAdmin"
            :effective-school-code="effectiveSchoolCode"
            :disputes="disputes"
            :is-handle-pending="isDisputeHandleOpeningPending"
            :page-num="pagination.pageNum"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            @school-change="handleSchoolChange"
            @campus-change="handleCampusChange"
            @search="handleSearch"
            @handle-dispute="handleDisputeAction"
            @open-dispute-detail="openDisputeDetail"
            @page-change="handlePageChange"
            @size-change="handleSizeChange"
          />

          <AdminRiskCenterPanel
            v-else-if="activeMenu === 'risk-center'"
            :can-view-risk-mode="canViewRiskMode"
            :can-manage-risk-mode="canManageRiskMode"
            :can-view-risk-list="canViewRiskList"
            :can-manage-risk-list="canManageRiskList"
            :can-view-risk-events="canViewRiskEvents"
            :can-handle-risk-case="canHandleRiskCase"
            :can-manage-risk-rule="canManageRiskRule"
            :can-view-behavior-control="canViewBehaviorControl"
            :can-manage-behavior-control="canManageBehaviorControl"
            :risk-tab="riskTab"
            :risk-mode="riskMode"
            :risk-event-type-filter="riskEventTypeFilter"
            :risk-decision-action-filter="riskDecisionActionFilter"
            :risk-level-filter="riskLevelFilter"
            :risk-case-status-filter="riskCaseStatusFilter"
            :risk-start-time-filter="riskStartTimeFilter"
            :risk-end-time-filter="riskEndTimeFilter"
            :risk-subject-type-filter="riskSubjectTypeFilter"
            :risk-subject-id-filter="riskSubjectIdFilter"
            :behavior-user-id-input="behaviorUserIdInput"
            :risk-events="riskEvents"
            :risk-cases="riskCases"
            :risk-rules="riskRules"
            :behavior-controls="behaviorControls"
            :blacklist-items="blacklistItems"
            :whitelist-items="whitelistItems"
            :page-num="pagination.pageNum"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            @switch-risk-tab="switchRiskTab"
            @risk-search="handleRiskSearch"
            @risk-mode-update="handleUpdateRiskMode"
            @behavior-user-query="handleBehaviorUserQuery"
            @update:risk-event-type-filter="riskEventTypeFilter = $event"
            @update:risk-decision-action-filter="riskDecisionActionFilter = $event"
            @update:risk-level-filter="riskLevelFilter = $event"
            @update:risk-case-status-filter="riskCaseStatusFilter = $event"
            @update:risk-start-time-filter="riskStartTimeFilter = $event"
            @update:risk-end-time-filter="riskEndTimeFilter = $event"
            @update:risk-subject-type-filter="riskSubjectTypeFilter = $event"
            @update:risk-subject-id-filter="riskSubjectIdFilter = $event"
            @update:behavior-user-id-input="behaviorUserIdInput = $event"
            @risk-case-process="handleRiskCaseProcess"
            @risk-rule-toggle="handleToggleRiskRule"
            @risk-rule-create="handleCreateRiskRule"
            @behavior-control-add="handleAddBehaviorControl"
            @behavior-control-disable="handleDisableBehavior"
            @blacklist-add="handleAddBlacklist"
            @blacklist-toggle="handleToggleBlacklist"
            @whitelist-add="handleAddWhitelist"
            @whitelist-toggle="handleToggleWhitelist"
            @page-change="handlePageChange"
            @size-change="handleSizeChange"
          />

          <AdminIamCenterPanel
            v-else-if="activeMenu === 'iam-center'"
            :can-view-iam-role="canViewIamRole"
            :can-view-iam-user-role="canViewIamUserRole"
            :can-manage-iam-user-role="canManageIamUserRole"
            :can-view-iam-scope="canViewIamScope"
            :can-manage-iam-scope="canManageIamScope"
            :iam-roles="iamRoles"
            :iam-user-id-input="iamUserIdInput"
            :iam-target-user-id="iamTargetUserId"
            :user-role-bindings="userRoleBindings"
            :admin-scope-bindings="adminScopeBindings"
            @update:iam-user-id-input="iamUserIdInput = $event"
            @query-user="handleIamUserQuery"
            @grant-role="handleGrantRole"
            @revoke-role="handleRevokeRole"
            @add-scope="handleAddScope"
            @disable-scope="handleDisableScopeBinding"
          />

          <!-- 搜索管理面板 -->
          <AdminSearchManagePanel
            v-else-if="activeMenu === 'search-manage'"
            :search-op-loading="searchOpLoading"
            @sync-goods-full="handleSyncGoodsFull"
            @create-goods-index="handleCreateGoodsIndex"
            @sync-errand-full="handleSyncErrandFull"
            @create-errand-index="handleCreateErrandIndex"
          />

          <AdminAuditCenterPanel
            v-else-if="activeMenu === 'audit-center'"
            :can-view-operation-audit="canViewOperationAudit"
            :can-view-permission-audit="canViewPermissionAudit"
            :can-view-login-audit="canViewLoginAudit"
            :audit-tab="auditTab"
            :audit-user-id-input="auditUserIdInput"
            :operation-audits="operationAudits"
            :permission-changes="permissionChanges"
            :login-traces="loginTraces"
            :audit-overview="auditOverview"
            :page-num="pagination.pageNum"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            @switch-tab="switchAuditTab"
            @update:audit-user-id-input="auditUserIdInput = $event"
            @search="handleAuditSearch"
            @page-change="handlePageChange"
            @size-change="handleSizeChange"
          />

        </div>
      </div>
    </main>

    <!-- 用户详情对话框 -->
    <Teleport to="body">
      <div v-if="showUserDetailDialog" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="absolute inset-0 bg-black/35" @click="showUserDetailDialog = false"></div>
        <div class="relative bg-white rounded-2xl shadow-um w-[480px] max-h-[80vh] overflow-y-auto p-6 border border-warm-100">
          <h3 class="text-lg font-bold text-slate-700 mb-4">用户详情</h3>
          <div v-if="userDetailData" class="space-y-4">
            <div class="flex items-center gap-4">
              <img :src="userDetailData.imageUrl || '/avatar-placeholder.svg'" class="w-16 h-16 rounded-full ring-2 ring-warm-100" />
              <div>
                <p class="font-bold text-slate-700 text-lg">{{ userDetailData.nickName }}</p>
                <p class="text-sm text-slate-400">ID: {{ userDetailData.userId }}</p>
              </div>
            </div>
            <div class="grid grid-cols-2 gap-3 text-sm">
              <div class="bg-warm-50 rounded-xl p-3 border border-warm-100">
                <p class="text-slate-400 text-xs">手机号</p>
                <p class="font-medium text-slate-700">{{ userDetailData.phone || '-' }}</p>
              </div>
              <div class="bg-warm-50 rounded-xl p-3 border border-warm-100">
                <p class="text-slate-400 text-xs">账号状态</p>
                <p class="font-medium" :class="userDetailData.accountStatus === 0 ? 'text-emerald-600' : 'text-red-600'">
                  {{ userDetailData.accountStatus === 0 ? '正常' : '封禁' }}
                </p>
              </div>
              <div class="bg-warm-50 rounded-xl p-3 border border-warm-100">
                <p class="text-slate-400 text-xs">认证状态</p>
                <p class="font-medium text-slate-700">{{ userDetailData.authStatus === 2 ? '已认证' : '未认证' }}</p>
              </div>
              <div class="bg-warm-50 rounded-xl p-3">
                <p class="text-warm-500 text-xs">信用分</p>
                <p class="font-bold text-warm-600 text-lg">{{ userDetailData.creditScore }}</p>
              </div>
            </div>

            <!-- 信用分调整 -->
            <div class="border-t border-warm-100 pt-4">
              <h4 class="text-sm font-bold text-slate-600 mb-3">调整信用分</h4>
              <div class="flex gap-3">
                <input
                  v-model.number="creditChangeForm.change"
                  type="number"
                  class="w-24 px-3 py-2 border border-warm-200 rounded-xl text-sm"
                  placeholder="分值"
                />
                <input
                  v-model="creditChangeForm.reason"
                  type="text"
                  class="flex-1 px-3 py-2 border border-warm-200 rounded-xl text-sm"
                  placeholder="调整原因"
                />
                <button
                  @click="handleAdjustCredit"
                  class="px-4 py-2 bg-gradient-to-r from-warm-500 to-warm-400 text-white rounded-xl text-sm font-bold hover:shadow-lg transition-all"
                >
                  调整
                </button>
              </div>
              <p class="text-xs text-slate-400 mt-2">正数加分，负数扣分</p>
            </div>
          </div>
          <button @click="showUserDetailDialog = false" class="absolute top-4 right-4 p-1.5 hover:bg-warm-50 rounded-lg transition-colors">
            <XCircle :size="20" class="text-slate-400" />
          </button>
        </div>
      </div>
    </Teleport>

    <!-- 纠纷处理对话框 -->
    <Teleport to="body">
      <div v-if="showDisputeHandleDialog" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="absolute inset-0 bg-black/35" @click="closeDisputeHandleDialog"></div>
        <div class="relative bg-white rounded-2xl shadow-um w-[520px] max-h-[85vh] overflow-y-auto p-6 border border-warm-100">
          <h3 class="text-lg font-bold text-slate-700 mb-1">处理纠纷</h3>
          <p class="text-xs text-slate-400 mb-4">
            纠纷 #{{ disputeHandleForm.disputeId }}
            <span v-if="disputeHandleDetail?.targetTypeDesc">· {{ disputeHandleDetail.targetTypeDesc }}</span>
          </p>

          <div v-if="disputeHandleDialogLoading" class="py-10 text-center text-sm text-slate-400">
            正在加载纠纷信息...
          </div>

          <div v-else class="space-y-4">
            <div class="grid grid-cols-2 gap-3 text-sm">
              <div class="bg-warm-50 rounded-xl p-3 border border-warm-100">
                <p class="text-slate-400 text-xs">发起者</p>
                <p class="font-medium text-slate-700">{{ disputeHandleDetail?.initiatorName || '-' }}</p>
              </div>
              <div class="bg-warm-50 rounded-xl p-3 border border-warm-100">
                <p class="text-slate-400 text-xs">被投诉方</p>
                <p class="font-medium text-slate-700">{{ disputeHandleDetail?.relatedName || '-' }}</p>
              </div>
            </div>

            <div class="rounded-xl border border-slate-100 bg-slate-50 p-3 text-xs text-slate-600">
              <p class="font-medium text-slate-700 mb-1">诉求摘要</p>
              <p>{{ disputeHandleClaimSummary }}</p>
              <p v-if="canDisputeRefund && disputeHandleMaxRefund != null" class="mt-1 text-slate-500">
                最高可裁定退款：¥{{ disputeHandleMaxRefund }}
              </p>
            </div>

            <div class="space-y-2">
              <label class="text-xs font-bold text-slate-400 uppercase">处理状态</label>
              <select
                v-model.number="disputeHandleForm.handleStatus"
                class="w-full px-3 py-2.5 border border-warm-200 rounded-xl text-sm bg-white"
              >
                <option :value="2">已解决</option>
                <option :value="3">已驳回</option>
              </select>
            </div>

            <div class="space-y-2">
              <label class="text-xs font-bold text-slate-400 uppercase">处理结果</label>
              <textarea
                v-model="disputeHandleForm.result"
                rows="3"
                class="w-full px-3 py-2.5 border border-warm-200 rounded-xl text-sm"
                placeholder="请输入处理结果或驳回原因"
              ></textarea>
            </div>

            <div class="grid grid-cols-2 gap-3">
              <div class="space-y-2">
                <label class="text-xs font-bold text-slate-400 uppercase">扣除信用分</label>
                <input
                  v-model="disputeHandleForm.deductCreditScoreText"
                  type="number"
                  min="0"
                  step="1"
                  :disabled="!canDisputeDeductCredit"
                  class="w-full px-3 py-2.5 border border-warm-200 rounded-xl text-sm disabled:opacity-60 disabled:bg-slate-50"
                  :placeholder="canDisputeDeductCredit ? '例如 5（留空表示不扣分）' : '未申请扣分/不可用'"
                />
              </div>
              <div class="space-y-2">
                <label class="text-xs font-bold text-slate-400 uppercase">退款金额</label>
                <input
                  v-model="disputeHandleForm.refundAmountText"
                  type="number"
                  min="0"
                  step="0.01"
                  :disabled="!canDisputeRefund"
                  class="w-full px-3 py-2.5 border border-warm-200 rounded-xl text-sm disabled:opacity-60 disabled:bg-slate-50"
                  :placeholder="canDisputeRefund ? (disputeHandleMaxRefund != null ? `最高 ¥${disputeHandleMaxRefund}（留空表示不退款）` : '例如 20.5（留空表示不退款）') : '仅订单纠纷可裁定退款'"
                />
              </div>
            </div>

            <div class="flex justify-end gap-3 pt-2">
              <button
                @click="closeDisputeHandleDialog"
                :disabled="disputeHandleSubmitting"
                class="px-5 py-2.5 bg-warm-50 border border-warm-200 text-warm-700 rounded-xl text-sm font-bold hover:bg-warm-100 transition-all disabled:opacity-60"
              >
                取消
              </button>
              <button
                @click="submitDisputeHandle"
                :disabled="disputeHandleSubmitting"
                class="px-5 py-2.5 bg-gradient-to-r from-amber-500 to-orange-500 text-white rounded-xl text-sm font-bold hover:shadow-lg hover:shadow-amber-200 transition-all disabled:opacity-60"
              >
                {{ disputeHandleSubmitting ? '提交中...' : '确认提交' }}
              </button>
            </div>
          </div>

          <button @click="closeDisputeHandleDialog" class="absolute top-4 right-4 p-1.5 hover:bg-warm-50 rounded-lg transition-colors">
            <XCircle :size="20" class="text-slate-400" />
          </button>
        </div>
      </div>
    </Teleport>

    <!-- 系统通知广播对话框 -->
    <Teleport to="body">
      <div v-if="showBroadcastDialog" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="absolute inset-0 bg-black/35" @click="showBroadcastDialog = false"></div>
        <div class="relative bg-white rounded-2xl shadow-um w-[480px] p-6 border border-warm-100">
          <h3 class="text-lg font-bold text-slate-700 mb-4">发送系统通知</h3>
          <div class="space-y-4">
            <div>
              <label class="text-xs font-bold text-slate-400 uppercase">通知标题</label>
              <input
                v-model="broadcastForm.title"
                type="text"
                class="w-full mt-1 px-3 py-2.5 border border-warm-200 rounded-xl text-sm"
                placeholder="请输入通知标题"
              />
            </div>
            <div>
              <label class="text-xs font-bold text-slate-400 uppercase">通知内容</label>
              <textarea
                v-model="broadcastForm.content"
                rows="4"
                class="w-full mt-1 px-3 py-2.5 border border-warm-200 rounded-xl text-sm"
                placeholder="请输入通知内容"
              ></textarea>
            </div>
            <div class="flex justify-end gap-3 pt-2">
              <button @click="showBroadcastDialog = false" class="px-5 py-2.5 bg-warm-50 border border-warm-200 text-warm-700 rounded-xl text-sm font-bold hover:bg-warm-100 transition-all">
                取消
              </button>
              <button @click="handleBroadcastNotice" class="px-5 py-2.5 bg-gradient-to-r from-amber-500 to-orange-500 text-white rounded-xl text-sm font-bold hover:shadow-lg hover:shadow-amber-200 transition-all">
                发送广播
              </button>
            </div>
          </div>
          <button @click="showBroadcastDialog = false" class="absolute top-4 right-4 p-1.5 hover:bg-warm-50 rounded-lg transition-colors">
            <XCircle :size="20" class="text-slate-400" />
          </button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.admin-owl {
  background:
    radial-gradient(circle at 10% 10%, rgba(255, 183, 77, 0.18), transparent 35%),
    radial-gradient(circle at 85% 20%, rgba(141, 110, 99, 0.16), transparent 32%),
    linear-gradient(160deg, #f7f2ef 0%, #efe6df 100%);
}

.admin-sidebar {
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.06), 0 16px 36px rgba(32, 18, 12, 0.35);
}

.admin-topbar {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(141, 110, 99, 0.18);
}

.admin-content-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 22px;
  border: 1px solid rgba(141, 110, 99, 0.2);
  box-shadow: 0 12px 34px rgba(80, 55, 42, 0.12);
}

/* 自定义滚动条 */
.scrollbar-thin::-webkit-scrollbar {
  width: 4px;
}

.scrollbar-thin::-webkit-scrollbar-track {
  background: transparent;
}

.scrollbar-thin::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

.scrollbar-thin::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.2);
}

/* Element Plus 分页组件美化 */
:deep(.el-pagination) {
  --el-pagination-button-bg-color: white;
  --el-pagination-hover-color: var(--um-primary);
}

:deep(.el-pagination .el-pager li) {
  border-radius: 8px;
  margin: 0 2px;
}

:deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, var(--um-primary), #a1887f);
  color: white;
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next) {
  border-radius: 8px;
}

:deep(.el-select .el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px var(--um-border);
}

:deep(.el-select .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(141, 110, 99, 0.45);
}

:deep(.el-select .el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 2px rgba(141, 110, 99, 0.2);
}

:deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px var(--um-border);
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(141, 110, 99, 0.45);
}

:deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 2px rgba(141, 110, 99, 0.2);
}
</style>








