import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { hasAdminAccess } from '@/utils/authz'
import { showError } from '@/utils/modal'
import { recordRouteSource } from '@/utils/navigation'

const adminDashboardView = () => import('../views/admin/AdminDashboardView.vue')

const ADMIN_ROUTE_META = {
  requiresAuth: true,
  requiresAdmin: true,
  hideLayout: true,
} as const

const adminRouteConfigs = [
  { path: '/admin/dashboard', name: 'admin-dashboard', title: '管理后台', adminMenu: 'dashboard' },
  { path: '/admin/goods-audit', name: 'admin-goods-audit', title: '商品审核', adminMenu: 'goods-audit' },
  { path: '/admin/auth-audit', name: 'admin-auth-audit', title: '认证审核', adminMenu: 'auth-audit' },
  { path: '/admin/runner-audit', name: 'admin-runner-audit', title: '跑腿员审核', adminMenu: 'runner-audit' },
  { path: '/admin/goods-manage', name: 'admin-goods-manage', title: '商品管理', adminMenu: 'goods-manage' },
  { path: '/admin/user-manage', name: 'admin-user-manage', title: '用户管理', adminMenu: 'user-manage' },
  { path: '/admin/order-manage', name: 'admin-order-manage', title: '订单管理', adminMenu: 'order-manage' },
  { path: '/admin/errand-manage', name: 'admin-errand-manage', title: '跑腿管理', adminMenu: 'errand-manage' },
  { path: '/admin/dispute-manage', name: 'admin-dispute-manage', title: '纠纷管理', adminMenu: 'dispute-manage' },
  { path: '/admin/risk', name: 'admin-risk-center', title: '风控中心', adminMenu: 'risk-center' },
  { path: '/admin/iam', name: 'admin-iam-center', title: 'IAM权限中心', adminMenu: 'iam-center' },
  { path: '/admin/audit', name: 'admin-audit-center', title: '审计中心', adminMenu: 'audit-center' },
  { path: '/admin/search-manage', name: 'admin-search-manage', title: '搜索管理', adminMenu: 'search-manage' },
] as const

const adminRoutes: RouteRecordRaw[] = adminRouteConfigs.map((route) => ({
  path: route.path,
  name: route.name,
  component: adminDashboardView,
  meta: {
    title: route.title,
    ...ADMIN_ROUTE_META,
    adminMenu: route.adminMenu,
  },
}))


const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      alias: '/home',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
      meta: { title: '首页', fullWidth: true }, // 原型首页，全宽展示
    },
    {
      path: '/market',
      name: 'market',
      component: () => import('../views/market/MarketView.vue'),
      meta: { title: '集市' }, // 公开页面，无需登录
    },
    {
      path: '/errands',
      name: 'errands',
      component: () => import('../views/errand/ErrandListView.vue'),
      meta: { title: '跑腿' }, // 公开页面，无需登录
    },

    {
      path: '/errand/publish',
      name: 'errand-publish',
      component: () => import('../views/errand/ErrandPublishView.vue'),
      meta: { title: '发布跑腿', requiresAuth: true },
    },
    {
      path: '/errand/edit/:taskId',
      name: 'errand-edit',
      component: () => import('../views/errand/ErrandPublishView.vue'),
      meta: { title: '编辑跑腿', requiresAuth: true },
    },
    {
      path: '/errand/:taskId',
      name: 'errand-detail',
      component: () => import('../views/errand/ErrandDetailView.vue'),
      meta: { title: '跑腿详情' }, // 公开页面，无需登录
    },
    {
      path: '/profile/my-errands',
      name: 'my-errands',
      component: () => import('../views/profile/MyErrandsView.vue'),
      meta: { title: '我的跑腿', requiresAuth: true },
    },
    {
      path: '/publish',
      name: 'publish',
      component: () => import('../views/PublishView.vue'),
      meta: { title: '发布', requiresAuth: true },
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('../views/ChatView.vue'),
      meta: { title: 'Nowl AI', requiresAuth: true },
    },
    {
      path: '/chat/user/:id',
      name: 'user-chat',
      component: () => import('../views/UserChatView.vue'),
      meta: { title: '聊天', requiresAuth: true },
    },
    {
      path: '/message',
      name: 'message',
      component: () => import('../views/MessageCenterView.vue'),
      meta: { title: '消息中心', requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/profile/ProfileView.vue'),
      meta: { title: '我的', requiresAuth: true },
    },
    {
      path: '/profile/edit',
      name: 'edit-profile',
      component: () => import('../views/profile/EditProfileView.vue'),
      meta: { title: '编辑资料', requiresAuth: true },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/auth/LoginView.vue'),
      meta: { title: '登录', hideLayout: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/auth/RegisterView.vue'),
      meta: { title: '注册', hideLayout: true },
    },
    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('../views/auth/ForgotPasswordView.vue'),
      meta: { title: '忘记密码', hideLayout: true },
    },
    {
      path: '/product/:id',
      name: 'product-detail',
      component: () => import('../views/market/ProductDetailView.vue'),
      meta: { title: '商品详情' }, // 公开页面，无需登录
    },
    {
      path: '/user/:id',
      name: 'personal-profile',
      component: () => import('../views/profile/PersonalProfileView.vue'),
      meta: { title: '个人主页' }, // 公开页面，无需登录
    },
    {
      path: '/user/:userId/following',
      name: 'user-following',
      component: () => import('../views/profile/FollowListView.vue'),
      meta: { title: '关注列表' }, // 公开页面，无需登录
    },
    {
      path: '/user/:userId/followers',
      name: 'user-followers',
      component: () => import('../views/profile/FollowListView.vue'),
      meta: { title: '粉丝列表' }, // 公开页面，无需登录
    },
    {
      path: '/profile/my-orders',
      name: 'my-orders',
      component: () => import('../views/profile/MyOrdersView.vue'),
      meta: { title: '我的订单', requiresAuth: true },
    },
    {
      path: '/profile/my-goods',
      name: 'my-goods',
      component: () => import('../views/profile/MyGoodsView.vue'),
      meta: { title: '我的商品', requiresAuth: true },
    },
    {
      path: '/profile/my-collections',
      name: 'my-collections',
      component: () => import('../views/profile/MyCollectionsView.vue'),
      meta: { title: '我的收藏', requiresAuth: true },
    },
    {
      path: '/profile/auth',
      name: 'auth',
      component: () => import('../views/profile/AuthView.vue'),
      meta: { title: '实名认证', requiresAuth: true },
    },
    {
      path: '/profile/runner-apply',
      name: 'runner-apply',
      component: () => import('../views/profile/RunnerApplyView.vue'),
      meta: { title: '跑腿员申请', requiresAuth: true },
    },
    {
      path: '/profile/settings',
      name: 'settings',
      component: () => import('../views/profile/SettingsView.vue'),
      meta: { title: '设置', requiresAuth: true },
    },
    {
      path: '/profile/help',
      name: 'help',
      component: () => import('../views/profile/HelpView.vue'),
      meta: { title: '帮助中心' }, // 公开页面，无需登录
    },
    {
      path: '/profile/blacklist',
      name: 'blacklist',
      component: () => import('../views/profile/BlackListView.vue'),
      meta: { title: '我的拉黑', requiresAuth: true },
    },
    {
      path: '/admin',
      redirect: '/admin/dashboard',
    },
    ...adminRoutes,
    // 纠纷相关路由
    {
      path: '/dispute/list',
      name: 'dispute-list',
      component: () => import('../views/dispute/DisputeListView.vue'),
      meta: { title: '我的纠纷', requiresAuth: true },
    },
    {
      path: '/dispute/create',
      name: 'dispute-create',
      component: () => import('../views/dispute/DisputeCreateView.vue'),
      meta: { title: '发起纠纷', requiresAuth: true },
    },
    {
      path: '/dispute/:id',
      name: 'dispute-detail',
      component: () => import('../views/dispute/DisputeDetailView.vue'),
      meta: { title: '纠纷详情', requiresAuth: true },
    },
    // 评价相关路由
    {
      path: '/review/create',
      name: 'review-create',
      component: () => import('../views/review/ReviewCreateView.vue'),
      meta: { title: '发表评价', requiresAuth: true },
    },
    {
      path: '/review/user/:userId',
      name: 'review-list',
      component: () => import('../views/review/ReviewListView.vue'),
      meta: { title: '用户评价' }, // 公开页面，无需登录
    },
  ],
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  // 固定站点标题，避免按路由动态变化
  document.title = 'Nowl'

  // 检查是否需要登录
  if (to.meta.requiresAuth) {
    const userStore = useUserStore()

    // 如果本地状态显示已登录，但 userInfo 不存在，尝试从后端验证
    if (userStore.isLoggedIn && !userStore.userInfo) {
      try {
        // 验证 Cookie 中的 token 是否有效
        await userStore.fetchUserInfo()
      } catch {
        // Token 无效或过期，跳转到登录页
        next({
          path: '/login',
          query: { redirect: to.fullPath },
        })
        return
      }
    }

    // 检查登录状态
    if (!userStore.isLoggedIn) {
      next({
        path: '/login',
        query: { redirect: to.fullPath },
      })
      return
    }

    // 检查是否需要管理员权限
    if (to.meta.requiresAdmin && !hasAdminAccess(userStore.userInfo)) {
      showError('需要管理员权限', '访问受限')
      next('/')
      return
    }
  }

  next()
})

router.afterEach((to, from, failure) => {
  if (failure) {
    return
  }
  if (!from.matched.length) {
    return
  }
  if (to.path === from.path) {
    return
  }
  recordRouteSource(to.fullPath, from.fullPath)
})

export default router
