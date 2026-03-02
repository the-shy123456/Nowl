<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CheckCircle2,
  MapPin,
  MessageCircle,
  Package,
  ShoppingBag,
  Star,
  UserCheck,
  UserPlus,
  Users,
} from 'lucide-vue-next'
import { checkFollow, followUser, getUserInfo, unfollowUser } from '@/api/modules/user'
import { getUserReviewStats } from '@/api/modules/review'
import { getGoodsList } from '@/api/modules/goods'
import { useUserStore } from '@/stores/user'
import { ElMessage } from '@/utils/feedback'
import { normalizeMediaUrl } from '@/utils/media'
import type { GoodsInfo, UserInfo } from '@/types'
import type { UserReviewStats } from '@/api/modules/review'
import { CreditLevelMap } from '@/constants/statusMaps'
import { TradeStatus } from '@/constants'
import SubPageShell from '@/components/SubPageShell.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const userId = computed(() => Number(route.params.id))
const isMyself = computed(() => userId.value === userStore.userInfo?.userId)

const loading = ref(true)
const loadError = ref(false)
const user = ref<UserInfo | null>(null)
const isFollowing = ref(false)
const followLoading = ref(false)
const products = ref<GoodsInfo[]>([])
const reviewStats = ref<UserReviewStats | null>(null)

const creditInfo = computed(() => {
  if (!reviewStats.value) return null
  return CreditLevelMap[reviewStats.value.creditColor] || null
})

const profileStats = computed(() => {
  const productList = products.value
  return {
    onSale: productList.filter(item => item.tradeStatus === TradeStatus.ON_SALE).length,
    sold: productList.filter(item => item.tradeStatus === TradeStatus.SOLD).length,
    total: productList.length,
  }
})

const formatRating = (value?: number) => {
  if (value === undefined || value === null) return '--'
  return Number(value).toFixed(1)
}

const formatGoodRate = (value?: number) => {
  if (value === undefined || value === null) return '--'
  return `${value}%`
}

const productImage = (product: GoodsInfo) => normalizeMediaUrl(product.image)

const fetchUserInfo = async () => {
  const data = await getUserInfo(userId.value)
  user.value = data
}

const fetchFollowStatus = async () => {
  if (isMyself.value || !userStore.isLoggedIn) {
    isFollowing.value = false
    return
  }
  try {
    const result = await checkFollow(userId.value)
    isFollowing.value = result === true
  } catch {
    isFollowing.value = false
  }
}

const fetchReviewStats = async () => {
  try {
    reviewStats.value = await getUserReviewStats(userId.value)
  } catch {
    reviewStats.value = null
  }
}

const fetchUserProducts = async () => {
  try {
    const res = await getGoodsList({
      sellerId: userId.value,
      pageNum: 1,
      pageSize: 6,
    })
    products.value = res?.records || []
  } catch {
    products.value = []
  }
}

const loadData = async () => {
  if (!Number.isFinite(userId.value) || userId.value <= 0) {
    loadError.value = true
    loading.value = false
    return
  }

  loading.value = true
  loadError.value = false
  try {
    await Promise.all([
      fetchUserInfo(),
      fetchFollowStatus(),
      fetchReviewStats(),
      fetchUserProducts(),
    ])
  } catch {
    loadError.value = true
    ElMessage.error('加载用户主页失败')
  } finally {
    loading.value = false
  }
}

const handleFollow = async () => {
  if (followLoading.value) return
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }

  followLoading.value = true
  try {
    if (isFollowing.value) {
      await unfollowUser(userId.value)
      isFollowing.value = false
      if (user.value) {
        user.value.fanCount = Math.max(0, (user.value.fanCount || 0) - 1)
      }
      ElMessage.success('已取消关注')
    } else {
      await followUser(userId.value)
      isFollowing.value = true
      if (user.value) {
        user.value.fanCount = (user.value.fanCount || 0) + 1
      }
      ElMessage.success('关注成功')
    }
  } catch {
    ElMessage.error('操作失败')
  } finally {
    followLoading.value = false
  }
}

const handleChat = () => {
  if (!user.value) return
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  router.push({
    path: `/chat/user/${userId.value}`,
    query: { name: user.value.nickName || `用户${userId.value}` },
  })
}

const viewProduct = (productId: number) => {
  router.push(`/product/${productId}`)
}

const viewFollowing = () => {
  router.push(`/user/${userId.value}/following`)
}

const viewFollowers = () => {
  router.push(`/user/${userId.value}/followers`)
}

const viewReviews = () => {
  router.push(`/review/user/${userId.value}`)
}

watch(
  () => userId.value,
  (next, previous) => {
    if (!Number.isFinite(next) || next <= 0 || next === previous) return
    void loadData()
  },
  { immediate: true },
)
</script>

<template>
  <SubPageShell
    :title="user ? `${user.nickName}的主页` : '个人主页'"
    subtitle="查看信用、评价与发布商品"
    max-width="lg"
    :use-card="false"
  >
    <template #icon>
      <Users class="text-white w-8 h-8" stroke-width="2.5" />
    </template>

    <div class="space-y-4 pb-8">
      <div v-if="loading" class="text-center py-12">
        <div class="inline-block animate-spin rounded-full h-10 w-10 border-4 border-warm-500 border-t-transparent"></div>
        <p class="mt-3 text-slate-500 text-sm">正在加载主页...</p>
      </div>

      <div v-else-if="loadError || !user" class="um-card py-16 text-center">
        <Users :size="46" class="mx-auto text-slate-300 mb-3" />
        <p class="text-slate-500">用户不存在或已不可访问</p>
      </div>

      <template v-else>
        <section class="um-card p-5 md:p-6">
          <div class="flex items-start gap-4">
            <div class="relative shrink-0">
              <img
                :src="normalizeMediaUrl(user.imageUrl) || '/avatar-placeholder.svg'"
                class="w-20 h-20 rounded-full object-cover border border-slate-200"
                :alt="user.nickName"
              />
              <div
                v-if="user.authStatus === 2"
                class="absolute -right-1 -bottom-1 w-6 h-6 rounded-full bg-emerald-500 text-white border-2 border-white flex items-center justify-center"
              >
                <CheckCircle2 :size="13" />
              </div>
            </div>

            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <h2 class="text-xl font-bold text-slate-800 truncate">{{ user.nickName || `用户${user.userId}` }}</h2>
                <span
                  v-if="user.authStatus === 2"
                  class="text-[11px] px-2 py-0.5 rounded-full border border-emerald-100 bg-emerald-50 text-emerald-700"
                >
                  已认证
                </span>
              </div>

              <p v-if="user.schoolName || user.campusName" class="mt-1 inline-flex items-center gap-1 text-sm text-slate-500">
                <MapPin :size="14" class="text-slate-400" />
                {{ user.schoolName || '未知学校' }}{{ user.campusName ? ` · ${user.campusName}` : '' }}
              </p>

              <div class="mt-3 grid grid-cols-3 gap-2 text-xs">
                <button class="rounded-xl bg-slate-50 border border-slate-200 px-3 py-2 text-center" @click="viewFollowing">
                  <div class="text-slate-500">关注</div>
                  <div class="font-bold text-slate-700 mt-0.5">{{ user.followCount || 0 }}</div>
                </button>
                <button class="rounded-xl bg-slate-50 border border-slate-200 px-3 py-2 text-center" @click="viewFollowers">
                  <div class="text-slate-500">粉丝</div>
                  <div class="font-bold text-slate-700 mt-0.5">{{ user.fanCount || 0 }}</div>
                </button>
                <button class="rounded-xl bg-slate-50 border border-slate-200 px-3 py-2 text-center" @click="viewReviews">
                  <div class="text-slate-500">评价</div>
                  <div class="font-bold text-slate-700 mt-0.5">{{ reviewStats?.totalReviews || 0 }}</div>
                </button>
              </div>
            </div>
          </div>

          <div v-if="!isMyself" class="mt-4 grid grid-cols-2 gap-2">
            <button
              @click="handleFollow"
              :disabled="followLoading"
              class="um-btn px-3 py-2.5 text-sm inline-flex items-center justify-center gap-1.5"
              :class="isFollowing
                ? 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                : 'bg-warm-500 text-white hover:bg-warm-600'"
            >
              <UserCheck v-if="isFollowing" :size="14" />
              <UserPlus v-else :size="14" />
              {{ isFollowing ? '已关注' : '关注' }}
            </button>
            <button
              @click="handleChat"
              class="um-btn px-3 py-2.5 text-sm bg-slate-100 text-slate-600 hover:bg-slate-200 inline-flex items-center justify-center gap-1.5"
            >
              <MessageCircle :size="14" />
              私信
            </button>
          </div>
        </section>

        <section class="um-card p-5 md:p-6">
          <h3 class="font-semibold text-slate-800 mb-3 inline-flex items-center gap-2">
            <Star :size="16" class="text-warm-500" />
            信用与评价概览
          </h3>

          <div class="grid grid-cols-2 md:grid-cols-4 gap-2 text-xs">
            <div class="rounded-xl bg-warm-50 border border-warm-100 px-3 py-2 text-warm-700">
              <div class="text-[11px] text-warm-500">平均评分</div>
              <div class="mt-0.5 text-sm font-bold">{{ formatRating(reviewStats?.averageRating) }}</div>
            </div>
            <div class="rounded-xl bg-blue-50 border border-blue-100 px-3 py-2 text-blue-700">
              <div class="text-[11px] text-blue-500">好评率</div>
              <div class="mt-0.5 text-sm font-bold">{{ formatGoodRate(reviewStats?.goodRate) }}</div>
            </div>
            <div class="rounded-xl border px-3 py-2" :class="[creditInfo?.bgColor || 'bg-slate-100', creditInfo?.color || 'text-slate-700']">
              <div class="text-[11px] opacity-80">信用分</div>
              <div class="mt-0.5 text-sm font-bold">{{ reviewStats?.creditScore ?? '--' }}</div>
            </div>
            <div class="rounded-xl bg-slate-100 border border-slate-200 px-3 py-2 text-slate-700">
              <div class="text-[11px] text-slate-500">信用等级</div>
              <div class="mt-0.5 text-sm font-bold">{{ reviewStats?.creditLevel || '--' }}</div>
            </div>
          </div>
        </section>

        <section class="um-card p-5 md:p-6">
          <div class="flex items-center justify-between gap-3 mb-4">
            <h3 class="font-semibold text-slate-800 inline-flex items-center gap-2">
              <ShoppingBag :size="16" class="text-warm-500" />
              发布商品
            </h3>
            <span class="text-xs text-slate-400">最多展示 6 件</span>
          </div>

          <div class="grid grid-cols-3 gap-2 mb-4 text-xs">
            <div class="rounded-xl bg-emerald-50 border border-emerald-100 px-3 py-2 text-emerald-700">
              <div class="text-[11px] text-emerald-500">在售</div>
              <div class="text-sm font-bold">{{ profileStats.onSale }}</div>
            </div>
            <div class="rounded-xl bg-blue-50 border border-blue-100 px-3 py-2 text-blue-700">
              <div class="text-[11px] text-blue-500">已售</div>
              <div class="text-sm font-bold">{{ profileStats.sold }}</div>
            </div>
            <div class="rounded-xl bg-slate-100 border border-slate-200 px-3 py-2 text-slate-700">
              <div class="text-[11px] text-slate-500">展示中</div>
              <div class="text-sm font-bold">{{ profileStats.total }}</div>
            </div>
          </div>

          <div v-if="products.length > 0" class="grid grid-cols-2 md:grid-cols-3 gap-3">
            <article
              v-for="product in products"
              :key="product.productId"
              class="rounded-2xl border border-slate-200 bg-white overflow-hidden cursor-pointer group"
              @click="viewProduct(product.productId)"
            >
              <div class="relative aspect-square overflow-hidden bg-slate-50">
                <img
                  v-if="productImage(product)"
                  :src="productImage(product)"
                  :alt="product.title"
                  class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                  loading="lazy"
                  decoding="async"
                />
                <div v-else class="w-full h-full flex items-center justify-center text-slate-400">
                  <Package :size="22" />
                </div>
                <div
                  v-if="product.tradeStatus !== TradeStatus.ON_SALE"
                  class="absolute inset-0 bg-black/45 flex items-center justify-center"
                >
                  <span class="text-white text-xs font-semibold">
                    {{ product.tradeStatus === TradeStatus.SOLD ? '已售出' : '已下架' }}
                  </span>
                </div>
              </div>
              <div class="p-3">
                <h4 class="text-sm font-medium text-slate-700 line-clamp-2">{{ product.title }}</h4>
                <p class="mt-1 text-warm-600 font-bold">¥{{ product.price }}</p>
              </div>
            </article>
          </div>

          <div v-else class="text-center py-12 text-slate-500">
            <ShoppingBag :size="40" class="mx-auto text-slate-300 mb-2" />
            暂无发布的商品
          </div>
        </section>
      </template>
    </div>
  </SubPageShell>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
