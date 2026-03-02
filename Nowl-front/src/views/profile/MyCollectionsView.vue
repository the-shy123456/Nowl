<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Heart, Package, ShoppingBag } from 'lucide-vue-next'
import { getMyCollections, uncollectGoods } from '@/api/modules/goods'
import { ElMessage } from '@/utils/feedback'
import { normalizeMediaUrl } from '@/utils/media'
import type { GoodsInfo } from '@/types'
import { ITEM_CONDITION_MAP, PAGE_CONSTANTS, TRADE_STATUS_MAP, TradeStatus } from '@/constants'
import SubPageShell from '@/components/SubPageShell.vue'
import { usePaginatedList } from '@/composables/usePaginatedList'
import {
  createQueryBinding,
  parsePositiveIntQuery,
  serializePageQuery,
  useListQuerySync,
} from '@/composables/useListQuerySync'
import InfiniteListFooter from '@/components/InfiniteListFooter.vue'

const router = useRouter()
const cancelLoadingMap = ref<Record<number, boolean>>({})

const {
  list: collections,
  pageNum,
  hasMore,
  showInitialLoading,
  isLoadingMore,
  loadMoreTrigger,
  refresh,
} = usePaginatedList<GoodsInfo>({
  pageSize: PAGE_CONSTANTS.LARGE_PAGE_SIZE,
  fetchPage: ({ pageNum, pageSize }) => getMyCollections({ pageNum, pageSize }),
  onError: () => {
    ElMessage.error('获取收藏列表失败')
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
], {
  onQueryApplied: () => {
    void refresh(pageNum.value)
  },
})

const setLoadMoreTrigger = (element: HTMLElement | null) => {
  loadMoreTrigger.value = element
}

const collectionStats = computed(() => {
  const list = collections.value
  return {
    total: list.length,
    onSale: list.filter(item => item.tradeStatus === TradeStatus.ON_SALE).length,
    sold: list.filter(item => item.tradeStatus === TradeStatus.SOLD).length,
    offShelf: list.filter(item => item.tradeStatus === TradeStatus.OFF_SHELF).length,
  }
})

const viewDetail = (productId: number) => {
  router.push(`/product/${productId}`)
}

const handleUncollect = async (productId: number) => {
  if (cancelLoadingMap.value[productId]) return
  cancelLoadingMap.value[productId] = true
  try {
    await uncollectGoods(productId)
    ElMessage.success('取消收藏成功')
    void refresh()
  } catch {
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    cancelLoadingMap.value[productId] = false
  }
}

const getTradeStatusMeta = (status: number) =>
  TRADE_STATUS_MAP[status as TradeStatus] || { text: '未知', color: 'bg-slate-100 text-slate-500' }

const goodsImage = (item: GoodsInfo) => normalizeMediaUrl(item.image)

const conditionText = (item: GoodsInfo) =>
  ITEM_CONDITION_MAP[item.itemCondition as keyof typeof ITEM_CONDITION_MAP]?.text
  ?? `${item.itemCondition}成新`

const goodsCampusText = (item: GoodsInfo) => {
  const school = item.schoolName || item.schoolCode || '未知学校'
  const campus = item.campusName || item.campusCode || '校区未知'
  return `${school} · ${campus}`
}

onMounted(() => {
  void refresh(pageNum.value)
})
</script>

<template>
  <SubPageShell title="我的收藏" subtitle="收藏商品随时可回看" back-to="/profile" max-width="lg" :use-card="false">
    <div class="space-y-4">
      <section class="um-card p-4 md:p-5">
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
          <div class="rounded-xl bg-warm-50 border border-warm-100 px-3 py-2 text-warm-700">
            <div class="text-[11px] text-warm-500">收藏总数</div>
            <div class="text-sm font-bold">{{ collectionStats.total }}</div>
          </div>
          <div class="rounded-xl bg-emerald-50 border border-emerald-100 px-3 py-2 text-emerald-700">
            <div class="text-[11px] text-emerald-500">可购买</div>
            <div class="text-sm font-bold">{{ collectionStats.onSale }}</div>
          </div>
          <div class="rounded-xl bg-blue-50 border border-blue-100 px-3 py-2 text-blue-700">
            <div class="text-[11px] text-blue-500">已售出</div>
            <div class="text-sm font-bold">{{ collectionStats.sold }}</div>
          </div>
          <div class="rounded-xl bg-slate-100 border border-slate-200 px-3 py-2 text-slate-700">
            <div class="text-[11px] text-slate-500">已下架</div>
            <div class="text-sm font-bold">{{ collectionStats.offShelf }}</div>
          </div>
        </div>
      </section>

      <div v-if="showInitialLoading" class="text-center py-12">
        <div class="inline-block animate-spin rounded-full h-10 w-10 border-4 border-warm-500 border-t-transparent"></div>
        <p class="mt-3 text-slate-500 text-sm">正在加载收藏...</p>
      </div>

      <div v-else-if="collections.length === 0" class="um-card py-16 text-center">
        <Heart :size="50" class="mx-auto text-slate-300 mb-3" />
        <p class="text-slate-500">还没有收藏商品</p>
        <button
          @click="router.push('/market')"
          class="mt-4 um-btn px-4 py-2 bg-warm-500 text-white hover:bg-warm-600 text-sm inline-flex items-center gap-1.5"
        >
          <ShoppingBag :size="14" />
          去逛逛
        </button>
      </div>

      <div v-else class="space-y-4">
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <article
            v-for="item in collections"
            :key="item.productId"
            class="um-card overflow-hidden group"
          >
            <button
              class="relative w-full aspect-[4/3] overflow-hidden bg-slate-50"
              @click="viewDetail(item.productId)"
            >
              <img
                v-if="goodsImage(item)"
                :src="goodsImage(item)"
                class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                :alt="item.title"
                loading="lazy"
                decoding="async"
              />
              <div v-else class="w-full h-full flex flex-col items-center justify-center gap-1 text-slate-400">
                <Package :size="24" />
                <span class="text-xs">暂无图片</span>
              </div>

              <div
                class="absolute top-2 right-2 px-2.5 py-1 rounded-full text-[11px] font-semibold border"
                :class="getTradeStatusMeta(item.tradeStatus).color"
              >
                {{ getTradeStatusMeta(item.tradeStatus).text }}
              </div>
              <button
                @click.stop="handleUncollect(item.productId)"
                :disabled="cancelLoadingMap[item.productId]"
                class="absolute bottom-2 right-2 p-2 bg-white/95 border border-warm-100 rounded-full text-warm-500 hover:bg-warm-50 transition-colors disabled:opacity-60"
                aria-label="取消收藏"
              >
                <Heart :size="16" fill="currentColor" />
              </button>
            </button>

            <button class="w-full p-4 text-left" @click="viewDetail(item.productId)">
              <h3 class="font-semibold text-slate-800 text-sm line-clamp-2 leading-6">{{ item.title }}</h3>
              <div class="mt-2 flex items-baseline gap-2">
                <span class="text-warm-600 font-bold text-lg">¥{{ item.price }}</span>
                <span class="text-xs text-slate-400">{{ conditionText(item) }}</span>
              </div>
              <div class="mt-2 text-xs text-slate-500">
                <span>{{ item.sellerName || '未知卖家' }}</span>
                <span class="mx-1.5">·</span>
                <span>{{ goodsCampusText(item) }}</span>
              </div>
            </button>
          </article>
        </div>

        <InfiniteListFooter
          :is-loading-more="isLoadingMore"
          :has-more="hasMore"
          :set-trigger="setLoadMoreTrigger"
          loading-text="正在加载更多收藏..."
        />
      </div>
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
