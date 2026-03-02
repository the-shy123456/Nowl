<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  AlertTriangle,
  Ban,
  BellRing,
  CheckCircle2,
  ChevronRight,
  HelpCircle,
  LogOut,
  MessageSquare,
  Star,
  ShieldCheck,
  Truck,
  UserCog,
} from 'lucide-vue-next'
import { getMyCollections, getMyGoods } from '@/api/modules/goods'
import { getMyOrders } from '@/api/modules/order'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from '@/utils/feedback'
import { normalizeMediaUrl } from '@/utils/media'
import SubPageShell from '@/components/SubPageShell.vue'

const router = useRouter()
const userStore = useUserStore()

interface StatItem {
  key: 'goods' | 'collections' | 'buy' | 'sell'
  label: string
  count: number
  hint: string
  color: string
  route: string
}

interface QuickEntry {
  icon: typeof UserCog
  title: string
  description: string
  route: string
}

const statsLoading = ref(false)
const myGoodsCount = ref(0)
const myBuyOrdersCount = ref(0)
const mySellOrdersCount = ref(0)
const myCollectionsCount = ref(0)
const currentUserId = computed(() => userStore.userInfo?.userId)

const authStatusMeta = computed(() => {
  const status = userStore.userInfo?.authStatus
  if (status === 2) {
    return { text: '已认证', className: 'bg-emerald-50 text-emerald-700 border border-emerald-100' }
  }
  if (status === 1) {
    return { text: '认证中', className: 'bg-amber-50 text-amber-700 border border-amber-100' }
  }
  if (status === 3) {
    return { text: '认证失败', className: 'bg-red-50 text-red-600 border border-red-100' }
  }
  return { text: '待认证', className: 'bg-slate-100 text-slate-600 border border-slate-200' }
})

const profileStats = computed<StatItem[]>(() => [
  {
    key: 'goods',
    label: '我的发布',
    count: myGoodsCount.value,
    hint: '已发布商品',
    color: 'bg-warm-50 text-warm-700 border border-warm-100',
    route: '/profile/my-goods',
  },
  {
    key: 'collections',
    label: '我的收藏',
    count: myCollectionsCount.value,
    hint: '关注的商品',
    color: 'bg-rose-50 text-rose-700 border border-rose-100',
    route: '/profile/my-collections',
  },
  {
    key: 'buy',
    label: '我买到的',
    count: myBuyOrdersCount.value,
    hint: '买入订单',
    color: 'bg-blue-50 text-blue-700 border border-blue-100',
    route: '/profile/my-orders?type=buy',
  },
  {
    key: 'sell',
    label: '我卖出的',
    count: mySellOrdersCount.value,
    hint: '卖出订单',
    color: 'bg-emerald-50 text-emerald-700 border border-emerald-100',
    route: '/profile/my-orders?type=sell',
  },
])

const quickEntries = computed<QuickEntry[]>(() => [
  {
    icon: UserCog,
    title: '编辑资料',
    description: '头像、昵称、学校与校区',
    route: '/profile/edit',
  },
  {
    icon: ShieldCheck,
    title: '校园认证',
    description: `当前状态：${authStatusMeta.value.text}`,
    route: '/profile/auth',
  },
  {
    icon: Truck,
    title: '我的跑腿',
    description: '发布与接单任务管理',
    route: '/profile/my-errands',
  },
  {
    icon: Star,
    title: '我的评价',
    description: '查看收到与发出的评价',
    route: currentUserId.value ? `/review/user/${currentUserId.value}` : '/profile',
  },
  {
    icon: AlertTriangle,
    title: '我的纠纷',
    description: '退款争议与处理进度',
    route: '/dispute/list',
  },
  {
    icon: MessageSquare,
    title: '我的消息',
    description: '系统通知与会话入口',
    route: '/message',
  },
  {
    icon: Ban,
    title: '我的拉黑',
    description: '屏蔽用户管理',
    route: '/profile/blacklist',
  },
  {
    icon: BellRing,
    title: '通知与偏好设置',
    description: '通知、主题、安全',
    route: '/profile/settings',
  },
  {
    icon: HelpCircle,
    title: '校园调解帮助中心',
    description: '退款与纠纷处理指南',
    route: '/profile/help',
  },
])

const userDisplayName = computed(() => userStore.userInfo?.nickName || '用户')
const userAvatar = computed(() => normalizeMediaUrl(userStore.userInfo?.imageUrl))

const fetchStats = async () => {
  statsLoading.value = true
  try {
    const [goodsRes, collectRes, buyOrdersRes, sellOrdersRes] = await Promise.all([
      getMyGoods({ pageNum: 1, pageSize: 1 }),
      getMyCollections({ pageNum: 1, pageSize: 1 }),
      getMyOrders({ pageNum: 1, pageSize: 1, orderType: 'buy' }),
      getMyOrders({ pageNum: 1, pageSize: 1, orderType: 'sell' }),
    ])
    myGoodsCount.value = goodsRes.total
    myCollectionsCount.value = collectRes.total
    myBuyOrdersCount.value = buyOrdersRes.total
    mySellOrdersCount.value = sellOrdersRes.total
  } catch {
    ElMessage.error('获取统计数据失败')
  } finally {
    statsLoading.value = false
  }
}

const navigateTo = (path: string) => {
  router.push(path)
}

const viewMyFollowing = () => {
  if (!currentUserId.value) return
  router.push(`/user/${currentUserId.value}/following`)
}

const viewMyFollowers = () => {
  if (!currentUserId.value) return
  router.push(`/user/${currentUserId.value}/followers`)
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确认退出当前账号？', '退出登录', {
      confirmButtonText: '确认退出',
      cancelButtonText: '取消',
    })
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('退出失败，请重试')
    }
  }
}

onMounted(() => {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: '/profile' } })
    return
  }
  void fetchStats()
})
</script>

<template>
  <SubPageShell title="我的主页" subtitle="管理账号、交易与互动记录" max-width="lg" :use-card="false" :show-back="false">
    <template #icon>
      <UserCog class="text-white w-8 h-8" stroke-width="2.5" />
    </template>

    <div class="space-y-4 pb-8">
      <section class="um-card p-5 md:p-6">
        <div class="flex items-start gap-4">
          <div class="w-20 h-20 rounded-3xl overflow-hidden border border-slate-200 bg-slate-100 shrink-0">
            <img
              v-if="userAvatar"
              :src="userAvatar"
              :alt="userDisplayName"
              class="w-full h-full object-cover"
            />
            <div v-else class="w-full h-full flex items-center justify-center text-slate-400 font-bold text-lg">
              {{ userDisplayName.slice(0, 1) }}
            </div>
          </div>

          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 flex-wrap">
              <h2 class="text-2xl font-extrabold text-slate-800 truncate">
                {{ userDisplayName }}
              </h2>
              <span class="text-[11px] font-semibold px-2 py-1 rounded-full" :class="authStatusMeta.className">
                <CheckCircle2 :size="12" class="inline mr-1" />
                {{ authStatusMeta.text }}
              </span>
            </div>

            <p class="mt-1 text-xs text-slate-500">
              {{ userStore.userInfo?.schoolName || '未绑定学校' }}
              <span v-if="userStore.userInfo?.campusName"> · {{ userStore.userInfo?.campusName }}</span>
              <span v-if="userStore.userInfo?.studentNo"> · 学号 {{ userStore.userInfo?.studentNo }}</span>
            </p>

            <div class="mt-4 grid grid-cols-2 md:grid-cols-4 gap-2 text-xs">
              <div class="rounded-xl bg-warm-50 border border-warm-100 px-3 py-2">
                <div class="text-warm-500">信用分</div>
                <div class="text-lg font-extrabold text-warm-700 mt-0.5">{{ userStore.userInfo?.creditScore || 0 }}</div>
              </div>
              <div class="rounded-xl bg-emerald-50 border border-emerald-100 px-3 py-2">
                <div class="text-emerald-500">余额</div>
                <div class="text-lg font-extrabold text-emerald-700 mt-0.5">¥{{ userStore.userInfo?.money || 0 }}</div>
              </div>
              <button
                class="rounded-xl bg-slate-50 border border-slate-200 px-3 py-2 text-left hover:border-warm-200 hover:bg-warm-50 transition-colors"
                @click="viewMyFollowing"
              >
                <div class="text-slate-500">关注</div>
                <div class="text-lg font-extrabold text-slate-700 mt-0.5">{{ userStore.userInfo?.followCount || 0 }}</div>
              </button>
              <button
                class="rounded-xl bg-slate-50 border border-slate-200 px-3 py-2 text-left hover:border-warm-200 hover:bg-warm-50 transition-colors"
                @click="viewMyFollowers"
              >
                <div class="text-slate-500">粉丝</div>
                <div class="text-lg font-extrabold text-slate-700 mt-0.5">{{ userStore.userInfo?.fanCount || 0 }}</div>
              </button>
            </div>
          </div>
        </div>
      </section>

      <section class="um-card p-4 md:p-5">
        <div class="flex items-center justify-between mb-3">
          <h3 class="text-sm font-bold text-slate-700">交易统计</h3>
          <span v-if="statsLoading" class="text-xs text-slate-400">刷新中...</span>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <button
            v-for="stat in profileStats"
            :key="stat.key"
            class="rounded-2xl p-4 text-left border border-slate-100 hover:border-warm-200 hover:bg-warm-50/40 transition-colors"
            @click="navigateTo(stat.route)"
          >
            <div class="flex items-center justify-between gap-2">
              <div>
                <p class="text-sm font-semibold text-slate-800">{{ stat.label }}</p>
                <p class="text-xs text-slate-400 mt-1">{{ stat.hint }}</p>
              </div>
              <div class="min-w-10 h-10 px-2 rounded-xl inline-flex items-center justify-center text-base font-extrabold" :class="stat.color">
                {{ stat.count }}
              </div>
            </div>
          </button>
        </div>
      </section>

      <section class="um-card overflow-hidden">
        <button
          v-for="item in quickEntries"
          :key="item.title"
          class="w-full flex items-center justify-between gap-3 px-4 py-4 border-b border-slate-100 last:border-b-0 hover:bg-warm-50/60 transition-colors text-left"
          @click="navigateTo(item.route)"
        >
          <div class="flex items-center gap-3 min-w-0">
            <div class="w-9 h-9 rounded-xl border border-slate-200 bg-white inline-flex items-center justify-center text-slate-500">
              <component :is="item.icon" :size="18" />
            </div>
            <div class="min-w-0">
              <p class="text-sm font-semibold text-slate-800 truncate">{{ item.title }}</p>
              <p class="text-xs text-slate-500 truncate">{{ item.description }}</p>
            </div>
          </div>
          <ChevronRight :size="16" class="text-slate-300 shrink-0" />
        </button>
      </section>

      <section class="um-card overflow-hidden">
        <button
          @click="handleLogout"
          class="w-full flex items-center justify-center gap-2 px-4 py-4 text-red-500 hover:bg-red-50 transition-colors font-semibold"
        >
          <LogOut :size="18" />
          退出登录
        </button>
      </section>
    </div>
  </SubPageShell>
</template>
