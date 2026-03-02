<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  BadgePlus,
  Edit,
  Eye,
  Package,
  PackageX,
  RotateCcw,
  Trash2,
} from 'lucide-vue-next'
import { deleteGoods, getMyGoods, offshelfGoods } from '@/api/modules/goods'
import { ElMessage, ElMessageBox } from '@/utils/feedback'
import { normalizeMediaUrl } from '@/utils/media'
import type { GoodsInfo } from '@/types'
import {
  ITEM_CONDITION_MAP,
  PAGE_CONSTANTS,
  REVIEW_STATUS_MAP,
  ReviewStatus,
  TRADE_STATUS_MAP,
  TradeStatus,
} from '@/constants'
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

const {
  list: goods,
  pageNum,
  hasMore,
  showInitialLoading,
  isLoadingMore,
  loadMoreTrigger,
  refresh,
} = usePaginatedList<GoodsInfo>({
  pageSize: PAGE_CONSTANTS.LARGE_PAGE_SIZE,
  fetchPage: ({ pageNum, pageSize }) => getMyGoods({ pageNum, pageSize }),
  onError: () => {
    ElMessage.error('获取商品列表失败')
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

const goodsStats = computed(() => {
  const list = goods.value
  return {
    total: list.length,
    onSale: list.filter(item => item.tradeStatus === TradeStatus.ON_SALE).length,
    sold: list.filter(item => item.tradeStatus === TradeStatus.SOLD).length,
    offShelf: list.filter(item => item.tradeStatus === TradeStatus.OFF_SHELF).length,
  }
})

const getGoodsStatusMeta = (item: GoodsInfo) => {
  if (
    item.reviewStatus === ReviewStatus.PENDING
    || item.reviewStatus === ReviewStatus.WAIT_MANUAL
    || item.reviewStatus === ReviewStatus.REJECTED
  ) {
    return REVIEW_STATUS_MAP[item.reviewStatus]
      || { text: '未知状态', color: 'bg-slate-100 text-slate-500' }
  }

  return TRADE_STATUS_MAP[item.tradeStatus as TradeStatus]
    || { text: '未知状态', color: 'bg-slate-100 text-slate-500' }
}

const goodsImage = (item: GoodsInfo) => normalizeMediaUrl(item.image)

const conditionText = (item: GoodsInfo) =>
  ITEM_CONDITION_MAP[item.itemCondition as keyof typeof ITEM_CONDITION_MAP]?.text
  ?? `${item.itemCondition}成新`

const viewDetail = (productId: number) => {
  router.push(`/product/${productId}`)
}

const editGoods = (productId: number, rejected = false) => {
  router.push({
    path: '/publish',
    query: rejected
      ? { id: String(productId), rejected: '1' }
      : { id: String(productId) },
  })
}

const canEdit = (item: GoodsInfo) =>
  item.tradeStatus === TradeStatus.ON_SALE
  && item.reviewStatus !== ReviewStatus.PENDING
  && item.reviewStatus !== ReviewStatus.REJECTED

const canOffshelf = (item: GoodsInfo) =>
  item.tradeStatus === TradeStatus.ON_SALE
  && (item.reviewStatus === ReviewStatus.APPROVED || item.reviewStatus === ReviewStatus.MANUAL_PASSED)

const canReeditRejected = (item: GoodsInfo) =>
  item.reviewStatus === ReviewStatus.REJECTED && item.tradeStatus === TradeStatus.OFF_SHELF

const canDelete = (item: GoodsInfo) =>
  item.tradeStatus === TradeStatus.OFF_SHELF || item.reviewStatus === ReviewStatus.REJECTED

const handleOffshelf = async (productId: number) => {
  try {
    await ElMessageBox.confirm('确认下架该商品？下架后商品将不再展示在集市中', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    })
    await offshelfGoods(productId)
    ElMessage.success('下架成功')
    void refresh()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('下架失败，请稍后重试')
    }
  }
}

const handleDelete = async (productId: number) => {
  try {
    await ElMessageBox.confirm('确认删除该商品？删除后无法恢复', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteGoods(productId)
    ElMessage.success('删除成功')
    void refresh()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败，请稍后重试')
    }
  }
}

onMounted(() => {
  void refresh(pageNum.value)
})
</script>

<template>
  <SubPageShell title="我的商品" subtitle="管理你发布的商品状态与操作" back-to="/profile" max-width="lg" :use-card="false">
    <div class="space-y-4">
      <section class="um-card p-4 md:p-5">
        <div class="flex items-center justify-between gap-3 flex-wrap">
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
            <div class="rounded-xl bg-warm-50 border border-warm-100 px-3 py-2 text-warm-700">
              <div class="text-[11px] text-warm-500">全部商品</div>
              <div class="text-sm font-bold">{{ goodsStats.total }}</div>
            </div>
            <div class="rounded-xl bg-emerald-50 border border-emerald-100 px-3 py-2 text-emerald-700">
              <div class="text-[11px] text-emerald-500">在售中</div>
              <div class="text-sm font-bold">{{ goodsStats.onSale }}</div>
            </div>
            <div class="rounded-xl bg-blue-50 border border-blue-100 px-3 py-2 text-blue-700">
              <div class="text-[11px] text-blue-500">已售出</div>
              <div class="text-sm font-bold">{{ goodsStats.sold }}</div>
            </div>
            <div class="rounded-xl bg-slate-100 border border-slate-200 px-3 py-2 text-slate-700">
              <div class="text-[11px] text-slate-500">已下架</div>
              <div class="text-sm font-bold">{{ goodsStats.offShelf }}</div>
            </div>
          </div>

          <button
            class="um-btn px-4 py-2.5 bg-warm-500 text-white hover:bg-warm-600 text-sm inline-flex items-center gap-1.5"
            @click="router.push('/publish')"
          >
            <BadgePlus :size="15" />
            发布新商品
          </button>
        </div>
      </section>

      <div v-if="showInitialLoading" class="text-center py-12">
        <div class="inline-block animate-spin rounded-full h-10 w-10 border-4 border-warm-500 border-t-transparent"></div>
        <p class="mt-3 text-slate-500 text-sm">正在加载商品...</p>
      </div>

      <div v-else-if="goods.length === 0" class="um-card py-16 text-center">
        <Package :size="50" class="mx-auto text-slate-300 mb-3" />
        <p class="text-slate-500">还没有发布商品</p>
        <button
          class="mt-4 um-btn px-4 py-2 bg-warm-500 text-white hover:bg-warm-600 text-sm"
          @click="router.push('/publish')"
        >
          去发布
        </button>
      </div>

      <div v-else class="space-y-4">
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <article
            v-for="item in goods"
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
                :alt="item.title"
                class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                loading="lazy"
                decoding="async"
              />
              <div v-else class="w-full h-full flex flex-col items-center justify-center gap-1 text-slate-400">
                <Package :size="24" />
                <span class="text-xs">暂无图片</span>
              </div>

              <span
                class="absolute top-2 right-2 px-2.5 py-1 rounded-full text-[11px] font-semibold border"
                :class="getGoodsStatusMeta(item).color"
              >
                {{ getGoodsStatusMeta(item).text }}
              </span>
            </button>

            <div class="p-4">
              <h3 class="font-semibold text-slate-800 text-sm line-clamp-2 leading-6">{{ item.title }}</h3>
              <div class="mt-2 flex items-baseline gap-2">
                <span class="text-warm-600 font-bold text-lg">¥{{ item.price }}</span>
                <span class="text-xs text-slate-400">{{ conditionText(item) }}</span>
              </div>

              <div class="mt-4 grid grid-cols-2 gap-2">
                <button
                  @click="viewDetail(item.productId)"
                  class="um-btn px-3 py-2 text-sm bg-slate-100 text-slate-600 hover:bg-slate-200 inline-flex items-center justify-center gap-1"
                >
                  <Eye :size="14" />
                  查看
                </button>

                <button
                  v-if="canEdit(item)"
                  @click="editGoods(item.productId)"
                  class="um-btn px-3 py-2 text-sm bg-warm-50 text-warm-600 hover:bg-warm-100 inline-flex items-center justify-center gap-1"
                >
                  <Edit :size="14" />
                  编辑
                </button>

                <button
                  v-if="canOffshelf(item)"
                  @click="handleOffshelf(item.productId)"
                  class="um-btn px-3 py-2 text-sm bg-orange-50 text-orange-600 hover:bg-orange-100 inline-flex items-center justify-center gap-1"
                >
                  <PackageX :size="14" />
                  下架
                </button>

                <button
                  v-if="canReeditRejected(item)"
                  @click="editGoods(item.productId, true)"
                  class="um-btn px-3 py-2 text-sm bg-warm-50 text-warm-600 hover:bg-warm-100 inline-flex items-center justify-center gap-1"
                >
                  <RotateCcw :size="14" />
                  重编
                </button>

                <button
                  v-if="canDelete(item)"
                  @click="handleDelete(item.productId)"
                  class="um-btn px-3 py-2 text-sm bg-red-50 text-red-600 hover:bg-red-100 inline-flex items-center justify-center gap-1"
                >
                  <Trash2 :size="14" />
                  删除
                </button>
              </div>
            </div>
          </article>
        </div>

        <InfiniteListFooter
          :is-loading-more="isLoadingMore"
          :has-more="hasMore"
          :set-trigger="setLoadMoreTrigger"
          loading-text="正在加载更多商品..."
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
