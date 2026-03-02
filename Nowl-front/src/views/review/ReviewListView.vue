<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bike, Package, Star, User } from 'lucide-vue-next'
import {
  getReceivedReviews,
  getSentReviews,
  getUserReviewStats,
  type ReviewListItem,
  type UserReviewStats,
} from '@/api/modules/review'
import { useUserStore } from '@/stores/user'
import { ElMessage } from '@/utils/feedback'
import { normalizeMediaUrl } from '@/utils/media'
import { CreditLevelMap, RatingMap } from '@/constants/statusMaps'
import SubPageShell from '@/components/SubPageShell.vue'
import InfiniteListFooter from '@/components/InfiniteListFooter.vue'
import { PAGE_CONSTANTS } from '@/constants'
import { usePaginatedList } from '@/composables/usePaginatedList'
import {
  createQueryBinding,
  parsePositiveIntQuery,
  serializePageQuery,
  useListQuerySync,
} from '@/composables/useListQuerySync'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userId = computed(() => Number(route.params.userId))
const isMyself = computed(() => userId.value === userStore.userInfo?.userId)
const stats = ref<UserReviewStats | null>(null)
const loadingStats = ref(false)
const reviewScope = ref<'received' | 'sent'>('received')
const backTo = computed(() =>
  isMyself.value ? '/profile' : `/user/${userId.value}`,
)
const pageTitle = computed(() => (isMyself.value ? '我的评价' : '用户评价'))
const pageSubtitle = computed(() => {
  if (!isMyself.value) {
    return '查看信用分与历史评价详情'
  }
  return reviewScope.value === 'sent'
    ? '查看你发出的买卖与跑腿评价'
    : '查看你收到的买卖与跑腿评价'
})
const scopeSummaryText = computed(() =>
  reviewScope.value === 'sent' ? '我发出的评价' : '我收到的评价',
)

const {
  list: reviews,
  pageNum,
  hasMore,
  showInitialLoading,
  isLoadingMore,
  loadMoreTrigger,
  refresh,
  reset,
} = usePaginatedList<ReviewListItem>({
  pageSize: PAGE_CONSTANTS.LARGE_PAGE_SIZE,
  fetchPage: ({ pageNum, pageSize }) => {
    if (reviewScope.value === 'sent' && isMyself.value) {
      return getSentReviews({ pageNum, pageSize })
    }
    return getReceivedReviews(userId.value, { pageNum, pageSize })
  },
  onError: () => {
    ElMessage.error('获取评价列表失败')
  },
})

useListQuerySync([
  createQueryBinding({
    key: 'page',
    state: pageNum,
    defaultValue: 1,
    parse: raw => parsePositiveIntQuery(raw, 1),
    serialize: value => serializePageQuery(value, 1),
  }),
  createQueryBinding({
    key: 'scope',
    state: reviewScope,
    defaultValue: 'received',
    parse: raw => {
      const value = Array.isArray(raw) ? raw[0] : raw
      if (!isMyself.value) return 'received'
      return value === 'sent' ? 'sent' : 'received'
    },
    serialize: value => {
      if (!isMyself.value || value === 'received') return undefined
      return value
    },
  }),
], {
  onQueryApplied: () => {
    void refresh(pageNum.value)
  },
})

const setLoadMoreTrigger = (element: HTMLElement | null) => {
  loadMoreTrigger.value = element
}

const fetchStats = async () => {
  loadingStats.value = true
  try {
    stats.value = await getUserReviewStats(userId.value)
  } catch {
    stats.value = null
  } finally {
    loadingStats.value = false
  }
}

const renderStars = (rating: number) => Array.from({ length: 5 }, (_, index) => index < rating)

const switchScope = (scope: 'received' | 'sent') => {
  if (!isMyself.value || reviewScope.value === scope) return
  reviewScope.value = scope
  pageNum.value = 1
  void refresh(1)
}

const formatTime = (value: string) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
}

const fromRoleLabel = (review: ReviewListItem) => review.reviewerRole || '参与方'

const canOpenReviewerProfile = (review: ReviewListItem) => {
  if (review.anonymous) return false
  const reviewerId = Number(review.reviewerId)
  return Number.isFinite(reviewerId) && reviewerId > 0
}

const openReviewerProfile = (review: ReviewListItem) => {
  if (!canOpenReviewerProfile(review)) return
  const reviewerId = Number(review.reviewerId)
  if (reviewerId === userStore.userInfo?.userId) {
    void router.push('/profile')
    return
  }
  void router.push(`/user/${reviewerId}`)
}

const hasRelatedContentTarget = (review: ReviewListItem) => {
  if (review.targetType === 0) {
    const productId = Number(review.productId)
    return Number.isFinite(productId) && productId > 0
  }
  if (review.targetType === 1) {
    const taskId = Number(review.taskId)
    return Number.isFinite(taskId) && taskId > 0
  }
  return false
}

const openRelatedContent = (review: ReviewListItem) => {
  if (!hasRelatedContentTarget(review)) return
  if (review.targetType === 0) {
    void router.push(`/product/${review.productId}`)
    return
  }
  if (review.targetType === 1) {
    void router.push(`/errand/${review.taskId}`)
  }
}

watch(
  () => userId.value,
  (next, previous) => {
    if (!Number.isFinite(next) || next <= 0 || next === previous) return
    if (!isMyself.value) {
      reviewScope.value = 'received'
    }
    reset()
    pageNum.value = 1
    void fetchStats()
    void refresh(1)
  },
)

onMounted(() => {
  void fetchStats()
  void refresh(pageNum.value)
})
</script>

<template>
  <SubPageShell :title="pageTitle" :subtitle="pageSubtitle" :back-to="backTo" max-width="lg" :use-card="false">
    <template #icon>
      <Star class="text-white w-8 h-8" stroke-width="2.5" />
    </template>

    <section v-if="stats" class="um-card p-4 md:p-5">
      <div class="grid grid-cols-2 md:grid-cols-4 gap-2 text-xs">
        <div class="rounded-xl bg-warm-50 border border-warm-100 px-3 py-2 text-warm-700">
          <div class="text-[11px] text-warm-500">平均评分</div>
          <div class="text-base font-bold mt-0.5">{{ stats.averageRating }}</div>
        </div>
        <div class="rounded-xl bg-blue-50 border border-blue-100 px-3 py-2 text-blue-700">
          <div class="text-[11px] text-blue-500">好评率</div>
          <div class="text-base font-bold mt-0.5">{{ stats.goodRate }}%</div>
        </div>
        <div class="rounded-xl bg-slate-100 border border-slate-200 px-3 py-2 text-slate-700">
          <div class="text-[11px] text-slate-500">评价总数</div>
          <div class="text-base font-bold mt-0.5">{{ stats.totalReviews }}</div>
        </div>
        <div class="rounded-xl border px-3 py-2" :class="[CreditLevelMap[stats.creditColor]?.bgColor, CreditLevelMap[stats.creditColor]?.color]">
          <div class="text-[11px] opacity-80">信用分</div>
          <div class="text-base font-bold mt-0.5">{{ stats.creditScore }} · {{ stats.creditLevel }}</div>
        </div>
      </div>
      <p v-if="loadingStats" class="mt-2 text-xs text-slate-400">统计刷新中...</p>
    </section>

    <section v-if="isMyself" class="um-card p-3 md:p-4 mt-4">
      <div class="flex items-center justify-between gap-3">
        <div class="inline-flex rounded-2xl border border-slate-200 bg-slate-50 p-1">
          <button
            @click="switchScope('received')"
            class="px-4 py-2 rounded-xl text-sm font-semibold transition-colors"
            :class="reviewScope === 'received' ? 'bg-white text-warm-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'"
          >
            我收到的
          </button>
          <button
            @click="switchScope('sent')"
            class="px-4 py-2 rounded-xl text-sm font-semibold transition-colors"
            :class="reviewScope === 'sent' ? 'bg-white text-warm-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'"
          >
            我发出的
          </button>
        </div>
        <span class="text-xs text-slate-500">{{ scopeSummaryText }}</span>
      </div>
    </section>

    <div class="space-y-3 mt-4">
      <div v-if="showInitialLoading" class="text-center py-16">
        <div class="inline-block animate-spin rounded-full h-10 w-10 border-4 border-warm-500 border-t-transparent"></div>
        <p class="mt-3 text-sm text-slate-500">正在加载评价...</p>
      </div>

      <div v-else-if="reviews.length === 0" class="um-card py-16 text-center">
        <Star class="w-12 h-12 mx-auto text-slate-300 mb-3" />
        <p class="text-slate-500">
          {{
            isMyself
              ? (reviewScope === 'sent' ? '你还没有发出评价' : '你还没有收到评价')
              : '暂无评价记录'
          }}
        </p>
      </div>

      <div v-else class="space-y-3">
        <article
          v-for="review in reviews"
          :key="review.reviewId"
          class="um-card p-4"
        >
          <header class="flex items-center justify-between gap-3">
            <button
              type="button"
              class="flex items-center gap-2 min-w-0 rounded-xl transition-colors"
              :class="canOpenReviewerProfile(review) ? 'cursor-pointer hover:bg-slate-50 px-1 py-1 -mx-1 -my-1' : 'cursor-default'"
              :disabled="!canOpenReviewerProfile(review)"
              @click="openReviewerProfile(review)"
            >
              <img
                v-if="normalizeMediaUrl(review.reviewerAvatar) && !review.anonymous"
                :src="normalizeMediaUrl(review.reviewerAvatar)"
                class="w-9 h-9 rounded-full object-cover border border-slate-200"
              />
              <div v-else class="w-9 h-9 rounded-full bg-slate-100 flex items-center justify-center border border-slate-200">
                <User class="w-4 h-4 text-slate-400" />
              </div>
              <div class="min-w-0">
                <p class="text-sm font-semibold text-slate-700 truncate">
                  {{ review.anonymous ? '匿名用户' : review.reviewerName }}
                </p>
                <p class="text-[11px] text-slate-400">{{ formatTime(review.createTime) }}</p>
              </div>
            </button>

            <div class="inline-flex items-center gap-1">
              <Star
                v-for="(filled, index) in renderStars(review.rating)"
                :key="index"
                class="w-4 h-4"
                :class="filled ? 'text-yellow-400 fill-yellow-400' : 'text-slate-200'"
              />
            </div>
          </header>

          <div class="mt-2 flex flex-wrap items-center gap-2 text-[11px]">
            <span class="px-2 py-1 rounded-full bg-warm-50 text-warm-600">
              来自{{ fromRoleLabel(review) }}
            </span>
          </div>

          <p class="mt-2 text-sm font-medium" :class="RatingMap[review.rating]?.color || 'text-slate-500'">
            {{ RatingMap[review.rating]?.text || '暂无评价等级' }}
          </p>

          <p v-if="review.content" class="mt-2 text-sm text-slate-600 leading-6 whitespace-pre-wrap">
            {{ review.content }}
          </p>

          <button
            v-if="review.contentTitle"
            type="button"
            class="w-full mt-3 rounded-xl border border-slate-200 bg-slate-50 p-2.5 flex items-center gap-2 text-left"
            :class="hasRelatedContentTarget(review) ? 'cursor-pointer hover:border-warm-200 hover:bg-warm-50/80 transition-colors' : 'cursor-default'"
            :disabled="!hasRelatedContentTarget(review)"
            @click="openRelatedContent(review)"
          >
            <div class="w-11 h-11 rounded-lg overflow-hidden bg-slate-100 border border-slate-200 shrink-0">
              <img v-if="normalizeMediaUrl(review.contentImage)" :src="normalizeMediaUrl(review.contentImage)" class="w-full h-full object-cover" />
              <div v-else class="w-full h-full flex items-center justify-center text-slate-400">
                <component :is="review.targetType === 0 ? Package : Bike" :size="16" />
              </div>
            </div>
            <div class="min-w-0">
              <p class="text-xs text-slate-700 truncate">{{ review.contentTitle }}</p>
              <p class="text-[11px] text-slate-400">{{ review.targetTypeDesc }}</p>
            </div>
          </button>
        </article>

        <InfiniteListFooter
          :is-loading-more="isLoadingMore"
          :has-more="hasMore"
          :set-trigger="setLoadMoreTrigger"
          loading-text="正在加载更多评价..."
        />
      </div>
    </div>
  </SubPageShell>
</template>
