<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Heart,
  Share2,
  MessageCircle,
  MapPin,
  ShieldCheck,
  ShoppingBag,
  ChevronLeft,
  ChevronRight,
  Package,
  AlertTriangle,
  Sparkles,
  Maximize2,
  X,
  Trash2,
} from 'lucide-vue-next'
import { getGoodsDetail, collectGoods, uncollectGoods, offshelfGoods, deleteGoods } from '@/api/modules/goods'
import { createOrder } from '@/api/modules/order'
import { getSimilarRecommend, recordViewBehavior } from '@/api/modules/recommend'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from '@/utils/feedback'
import { navigateBack } from '@/utils/navigation'
import { normalizeMediaUrl } from '@/utils/media'
import type { GoodsInfo, RecommendItemVO } from '@/types'
import { TradeStatus, ReviewStatus } from '@/constants'
import dayjs from 'dayjs'

interface ProductDetail extends GoodsInfo {
  images?: string[]
}

const CONDITION_LABELS: Record<number, string> = {
  0: '全新',
  1: '几乎全新',
  2: '轻微使用痕迹',
  3: '使用痕迹明显',
  4: '功能正常外观老旧',
  5: '需要维修',
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const productId = computed(() => Number(route.params.id))
const sourceOrderId = computed(() => {
  const raw = Array.isArray(route.query.fromOrderId) ? route.query.fromOrderId[0] : route.query.fromOrderId
  const parsed = Number(raw)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
})
const isOrderReadonlyView = computed(() => sourceOrderId.value !== null)
const product = ref<ProductDetail | null>(null)
const loading = ref(false)
const currentImageIndex = ref(0)
const showImagePreview = ref(false)
const previewImageIndex = ref(0)
const deletingLoading = ref(false)
let previewClosedAt = 0

// 操作 loading 状态（防止重复提交）
const collectingLoading = ref(false)
const buyingLoading = ref(false)

// 相似商品推荐
const similarProducts = ref<RecommendItemVO[]>([])
const loadingSimilar = ref(false)

// 浏览时长记录
let viewStartTime = 0
let viewDuration = 0

const parseImages = (detail: ProductDetail): string[] => {
  const candidates: string[] = []

  if (detail.imageList) {
    try {
      const parsed = JSON.parse(detail.imageList)
      if (Array.isArray(parsed)) {
        parsed.forEach((item) => {
          if (typeof item === 'string' && item.trim()) {
            candidates.push(item.trim())
          }
        })
      }
    } catch {
      // 兼容早期逗号分隔格式
      detail.imageList
        .split(',')
        .map(item => item.trim())
        .filter(Boolean)
        .forEach(item => candidates.push(item))
    }
  }

  if (detail.image) {
    candidates.push(detail.image)
  }

  return Array.from(
    new Set(
      candidates
        .map(item => normalizeMediaUrl(item))
        .filter((item): item is string => Boolean(item)),
    ),
  )
}

const productImages = computed(() => product.value?.images ?? [])
const currentImage = computed(() => productImages.value[currentImageIndex.value] ?? '')
const previewImage = computed(() => productImages.value[previewImageIndex.value] ?? '')
const hasMultiImages = computed(() => productImages.value.length > 1)

const conditionText = computed(() => {
  if (!product.value) return '未知成色'
  return CONDITION_LABELS[product.value.itemCondition] ?? `${product.value.itemCondition}成新`
})

const tradeTypeText = computed(() => {
  const map: Record<number, string> = { 0: '仅支持自提', 1: '仅支持邮寄', 2: '支持自提/邮寄' }
  return product.value ? map[product.value.tradeType] ?? '交易方式待沟通' : ''
})

const tradeStatusText = computed(() => {
  if (!product.value) return ''
  const tradeStatus = product.value.tradeStatus
  const reviewStatus = product.value.reviewStatus

  // 已售出是终态，优先展示
  if (tradeStatus === TradeStatus.SOLD) return '已售出'

  // 审核未通过/审核中/待复核优先展示审核态，避免出现“在售但审核中”的矛盾提示
  if (!isReviewPassed(reviewStatus)) {
    if (reviewStatus === ReviewStatus.REJECTED) return '已驳回'
    if (reviewStatus === ReviewStatus.WAIT_MANUAL) return '待人工复核'
    if (reviewStatus === ReviewStatus.PENDING) return '审核中'
  }

  if (tradeStatus === TradeStatus.OFF_SHELF) return '已下架'
  return '在售'
})

const tradeStatusStyle = computed(() => {
  if (!product.value) return 'bg-slate-100 text-slate-500'
  const tradeStatus = product.value.tradeStatus
  const reviewStatus = product.value.reviewStatus

  if (tradeStatus === TradeStatus.SOLD) {
    return 'bg-slate-100 text-slate-600 border border-slate-200'
  }

  if (!isReviewPassed(reviewStatus)) {
    if (reviewStatus === ReviewStatus.PENDING) {
      return 'bg-yellow-50 text-yellow-700 border border-yellow-100'
    }
    if (reviewStatus === ReviewStatus.WAIT_MANUAL) {
      return 'bg-orange-50 text-orange-700 border border-orange-100'
    }
    if (reviewStatus === ReviewStatus.REJECTED) {
      return 'bg-red-50 text-red-700 border border-red-100'
    }
  }

  if (tradeStatus === TradeStatus.ON_SALE) return 'bg-emerald-50 text-emerald-700 border border-emerald-100'
  if (tradeStatus === TradeStatus.OFF_SHELF) return 'bg-red-50 text-red-600 border border-red-100'
  return 'bg-slate-100 text-slate-500 border border-slate-200'
})

// 判断是否是卖家自己
const isSeller = computed(() => {
  if (!product.value || !userStore.userInfo) return false
  return product.value.sellerId === userStore.userInfo.userId
})

const isReviewPassed = (reviewStatus?: number) =>
  reviewStatus === ReviewStatus.APPROVED || reviewStatus === ReviewStatus.MANUAL_PASSED

const canSellerEdit = computed(() => {
  if (!isSeller.value || !product.value) return false
  const status = product.value.tradeStatus
  const reviewStatus = product.value.reviewStatus
  const rejectedAndOffShelf = reviewStatus === ReviewStatus.REJECTED && status === TradeStatus.OFF_SHELF
  return (status === TradeStatus.ON_SALE && isReviewPassed(reviewStatus)) || rejectedAndOffShelf
})

const canSellerOffShelf = computed(() => {
  if (!isSeller.value || !product.value) return false
  return product.value.tradeStatus === TradeStatus.ON_SALE && isReviewPassed(product.value.reviewStatus)
})

const canSellerRelist = computed(() => {
  if (!isSeller.value || !product.value) return false
  return product.value.tradeStatus === TradeStatus.OFF_SHELF && isReviewPassed(product.value.reviewStatus)
})

const canSellerDelete = computed(() => {
  if (!isSeller.value || !product.value) return false
  return product.value.tradeStatus === TradeStatus.OFF_SHELF || product.value.reviewStatus === ReviewStatus.REJECTED
})

const sellerPrimaryActionText = computed(() => {
  if (!product.value) return '不可操作'

  if (product.value.tradeStatus === TradeStatus.SOLD) {
    return '已售出'
  }
  if (product.value.reviewStatus === ReviewStatus.PENDING) {
    return '审核中'
  }
  if (product.value.reviewStatus === ReviewStatus.WAIT_MANUAL) {
    return '待人工复核'
  }
  if (product.value.reviewStatus === ReviewStatus.REJECTED) {
    return '已驳回'
  }
  if (product.value.tradeStatus === TradeStatus.OFF_SHELF) {
    return '已下架'
  }
  return '不可操作'
})

const formatTime = (time: string) => dayjs(time).format('YYYY-MM-DD')

// 获取商品详情
const fetchProductDetail = async () => {
  if (!Number.isFinite(productId.value) || productId.value <= 0) {
    ElMessage.error('商品编号无效')
    navigateBack(router, route, '/market')
    return
  }

  loading.value = true
  try {
    const res = await getGoodsDetail(productId.value)
    const nextProduct: ProductDetail = { ...res }
    nextProduct.images = parseImages(nextProduct)
    product.value = nextProduct
    currentImageIndex.value = 0
    previewImageIndex.value = 0
    showImagePreview.value = false

    // 获取相似商品推荐
    void fetchSimilarProducts()

    // 开始记录浏览时间
    viewStartTime = Date.now()
  } catch {
    ElMessage.error('获取商品详情失败')
    navigateBack(router, route, '/market')
  } finally {
    loading.value = false
  }
}

// 获取相似商品推荐
const fetchSimilarProducts = async () => {
  loadingSimilar.value = true
  try {
    const res = await getSimilarRecommend(productId.value, 6)
    similarProducts.value = res || []
  } catch (error) {
    console.error('获取相似商品推荐失败:', error)
  } finally {
    loadingSimilar.value = false
  }
}

// 记录浏览行为
const recordView = async (targetProductId = productId.value, targetCategoryId = product.value?.categoryId) => {
  if (!Number.isFinite(targetProductId) || !Number.isFinite(targetCategoryId) || viewStartTime <= 0) return
  viewDuration = Math.floor((Date.now() - viewStartTime) / 1000)
  if (viewDuration <= 0) return

  try {
    await recordViewBehavior(targetProductId, targetCategoryId, viewDuration)
  } catch {
    // 静默失败，不影响用户体验
  }
}

// 跳转到相似商品详情
const goToSimilarProduct = (id: number) => {
  if (!Number.isFinite(id) || id <= 0 || id === productId.value) return
  void recordView()
  router.push(`/product/${id}`)
}

const switchImage = (index: number) => {
  if (index < 0 || index >= productImages.value.length) return
  currentImageIndex.value = index
}

const openImagePreview = (index = currentImageIndex.value) => {
  if (Date.now() - previewClosedAt < 220) return
  if (!productImages.value.length) return
  previewImageIndex.value = index
  showImagePreview.value = true
}

const closeImagePreview = () => {
  showImagePreview.value = false
  previewClosedAt = Date.now()
}

const movePreview = (direction: -1 | 1) => {
  const total = productImages.value.length
  if (!total) return
  const next = (previewImageIndex.value + direction + total) % total
  previewImageIndex.value = next
  currentImageIndex.value = next
}

// 收藏/取消收藏
const handleLike = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!product.value || collectingLoading.value) return

  collectingLoading.value = true
  try {
    if (product.value.isCollected) {
      await uncollectGoods(productId.value)
      product.value.isCollected = false
      product.value.collectCount = Math.max((product.value.collectCount || 0) - 1, 0)
      ElMessage.success('取消收藏成功')
    } else {
      await collectGoods(productId.value)
      product.value.isCollected = true
      product.value.collectCount = (product.value.collectCount || 0) + 1
      ElMessage.success('收藏成功')
    }
  } catch {
    ElMessage.error('操作失败')
  } finally {
    collectingLoading.value = false
  }
}

// 购买商品
const handleBuy = async () => {
  if (isOrderReadonlyView.value) {
    ElMessage.info('该商品已存在订单记录，请在“我的订单”中继续处理')
    return
  }
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (userStore.userInfo?.authStatus !== 2) {
    ElMessage.warning('为保障交易安全，请先完成校园认证')
    return
  }
  if (!product.value || buyingLoading.value) return

  if (product.value.tradeStatus !== TradeStatus.ON_SALE) {
    ElMessage.warning('商品已售出或已下架')
    return
  }

  buyingLoading.value = true
  try {
    await createOrder({ productId: productId.value, remark: '' })
    ElMessage.success('下单成功')
    await router.push('/profile/my-orders?type=buy')
  } catch {
    ElMessage.error('创建订单失败')
  } finally {
    buyingLoading.value = false
  }
}

// 联系卖家
const handleChat = () => {
  if (!product.value) return
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (userStore.userInfo?.authStatus !== 2) {
    ElMessage.warning('请先完成校园认证')
    return
  }

  router.push({
    path: `/chat/user/${product.value.sellerId}`,
    query: { name: product.value.sellerName },
  })
}

const handleShare = async () => {
  const shareUrl = typeof window !== 'undefined' ? window.location.href : route.fullPath
  try {
    if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(shareUrl)
      ElMessage.success('链接已复制到剪贴板')
      return
    }
    ElMessage.warning('当前环境不支持一键复制，请手动复制地址栏链接')
  } catch {
    ElMessage.warning('复制失败，请手动复制地址栏链接')
  }
}

const handleBack = () => {
  navigateBack(router, route, '/market')
}

// 管理商品跳转
const handleEdit = () => {
  if (!product.value) return
  if (!canSellerEdit.value) {
    ElMessage.warning('当前状态下不可编辑商品')
    return
  }
  router.push({
    path: '/publish',
    query: { id: product.value.productId.toString() },
  })
}

// 下架商品
const handleOffShelf = async () => {
  if (!product.value || !canSellerOffShelf.value) {
    ElMessage.warning('当前状态下不可下架商品')
    return
  }
  try {
    await ElMessageBox.confirm('确认下架该商品吗？下架后买家将无法看到此商品。', '下架商品', {
      confirmButtonText: '确认下架',
      cancelButtonText: '取消',
    })

    await offshelfGoods(productId.value)
    ElMessage.success('商品已下架')
    product.value.tradeStatus = TradeStatus.OFF_SHELF
  } catch (error) {
    if (error !== 'cancel') {
      console.error('下架商品失败:', error)
      ElMessage.error('下架失败，请稍后重试')
    }
  }
}

const handleDelete = async () => {
  if (!product.value || deletingLoading.value) return
  if (!canSellerDelete.value) {
    ElMessage.warning('当前状态下不可删除商品')
    return
  }
  try {
    await ElMessageBox.confirm('确认删除该商品吗？删除后不可恢复。', '删除商品', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
    })

    deletingLoading.value = true
    await deleteGoods(productId.value)
    ElMessage.success('商品已删除')
    navigateBack(router, route, '/profile/my-goods')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除商品失败:', error)
      ElMessage.error('删除失败，请稍后重试')
    }
  } finally {
    deletingLoading.value = false
  }
}

// 重新上架商品 - 跳转到发布页面编辑
const handleRelist = () => {
  if (!product.value) return
  router.push({
    path: '/publish',
    query: {
      id: product.value.productId.toString(),
      relist: '1',
    },
  })
}

const handlePreviewKeydown = (event: KeyboardEvent) => {
  if (!showImagePreview.value) return
  if (event.key === 'Escape') {
    closeImagePreview()
    return
  }
  if (event.key === 'ArrowLeft') {
    event.preventDefault()
    movePreview(-1)
    return
  }
  if (event.key === 'ArrowRight') {
    event.preventDefault()
    movePreview(1)
  }
}

watch(currentImageIndex, (nextIndex) => {
  if (showImagePreview.value) {
    previewImageIndex.value = nextIndex
  }
})

watch(
  () => productId.value,
  async (newId, oldId) => {
    if (!Number.isFinite(newId) || newId <= 0 || newId === oldId) return
    const previousCategoryId = product.value?.categoryId
    await recordView(oldId, previousCategoryId)
    await fetchProductDetail()
  },
)

onMounted(() => {
  void fetchProductDetail()
  window.addEventListener('keydown', handlePreviewKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handlePreviewKeydown)
  void recordView()
})
</script>

<template>
  <div class="pb-28 text-slate-800">
    <header class="sticky top-0 z-30 px-4 pt-2">
      <div class="um-card px-3 py-2 flex items-center justify-between backdrop-blur">
        <button @click="handleBack" class="um-icon-btn" aria-label="返回">
          <ChevronLeft :size="20" class="text-um-muted" />
        </button>
        <div class="text-sm font-bold text-um-text tracking-wide">商品详情</div>
        <button @click="handleShare" class="um-icon-btn" aria-label="分享商品">
          <Share2 :size="18" class="text-um-muted" />
        </button>
      </div>
    </header>

    <div v-if="loading" class="flex items-center justify-center py-24">
      <div class="animate-spin rounded-full h-9 w-9 border-2 border-warm-500 border-t-transparent"></div>
    </div>

    <div v-else-if="product" class="max-w-6xl mx-auto px-4 py-6 md:py-10 space-y-6">
      <section
        v-if="isSeller && product.reviewStatus === ReviewStatus.REJECTED"
        class="rounded-2xl border border-red-100 bg-red-50/90 p-4 shadow-sm"
      >
        <div class="flex items-start gap-3">
          <AlertTriangle :size="22" class="text-red-500 mt-0.5 flex-shrink-0" />
          <div class="flex-1 min-w-0">
            <h3 class="font-bold text-red-700">商品审核未通过</h3>
            <p class="text-sm text-red-600 mt-1 leading-relaxed">{{ product.auditReason || '包含违规内容' }}</p>
            <button
              v-if="canSellerEdit"
              @click="handleEdit"
              class="mt-3 text-xs font-bold rounded-xl border border-red-200 bg-white px-3 py-2 text-red-600 hover:bg-red-50 transition-colors"
            >
              立即修改
            </button>
          </div>
        </div>
      </section>

      <section
        v-if="isSeller && product.reviewStatus === ReviewStatus.PENDING"
        class="rounded-2xl border border-yellow-100 bg-yellow-50/90 p-4 shadow-sm"
      >
        <div class="flex items-start gap-3">
          <ShieldCheck :size="22" class="text-yellow-600 mt-0.5 flex-shrink-0" />
          <div>
            <h3 class="font-bold text-yellow-700">商品正在审核中</h3>
            <p class="text-sm text-yellow-700/90 mt-1">系统正在进行智能审核，请耐心等待。</p>
          </div>
        </div>
      </section>

      <section
        v-if="isSeller && product.reviewStatus === ReviewStatus.WAIT_MANUAL"
        class="rounded-2xl border border-orange-100 bg-orange-50/90 p-4 shadow-sm"
      >
        <div class="flex items-start gap-3">
          <ShieldCheck :size="22" class="text-orange-600 mt-0.5 flex-shrink-0" />
          <div>
            <h3 class="font-bold text-orange-700">商品待人工复核</h3>
            <p class="text-sm text-orange-700/90 mt-1">AI 检测到可能风险，已转交人工审核，请耐心等待。</p>
          </div>
        </div>
      </section>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 lg:gap-10 items-start">
        <section class="space-y-3">
          <div class="relative overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
            <button
              type="button"
              class="w-full aspect-[4/3] block bg-slate-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-warm-400"
              @click="openImagePreview()"
            >
              <img
                v-if="currentImage"
                :src="currentImage"
                :alt="product.title"
                class="w-full h-full object-contain"
                loading="eager"
                decoding="async"
              />
              <div v-else class="w-full h-full flex flex-col items-center justify-center gap-2 text-slate-400">
                <Package :size="28" />
                <span class="text-sm">暂无商品图片</span>
              </div>
            </button>

            <div class="pointer-events-none absolute left-3 top-3 flex items-center gap-2">
              <span class="rounded-full px-3 py-1 text-xs font-bold bg-white/95 border border-slate-200 text-slate-700">
                {{ tradeStatusText }}
              </span>
              <span
                v-if="productImages.length > 0"
                class="rounded-full px-3 py-1 text-xs font-medium bg-slate-900/70 text-white"
              >
                {{ currentImageIndex + 1 }}/{{ productImages.length }}
              </span>
            </div>

            <button
              v-if="productImages.length > 0"
              class="absolute right-3 top-3 rounded-full bg-white/95 border border-slate-200 p-2 text-slate-600 hover:text-warm-600 transition-colors"
              @click="openImagePreview()"
              aria-label="查看大图"
            >
              <Maximize2 :size="16" />
            </button>

            <div
              v-if="product.tradeStatus !== TradeStatus.ON_SALE"
              class="absolute inset-0 bg-black/55 backdrop-blur-[1px] flex items-center justify-center"
            >
              <span class="text-white font-bold text-lg md:text-xl px-6 py-2 border border-white/70 rounded-full">
                {{ tradeStatusText }}
              </span>
            </div>
          </div>

          <div
            v-if="hasMultiImages"
            class="flex gap-3 overflow-x-auto no-scrollbar py-1"
          >
            <button
              v-for="(img, idx) in productImages"
              :key="`${img}-${idx}`"
              @click="switchImage(idx)"
              class="relative w-20 h-20 md:w-24 md:h-24 flex-shrink-0 rounded-2xl overflow-hidden border-2 transition-all"
              :class="currentImageIndex === idx
                ? 'border-warm-400 shadow-sm ring-2 ring-warm-100'
                : 'border-transparent opacity-75 hover:opacity-100'"
              :aria-label="`查看第${idx + 1}张图片`"
            >
              <img :src="img" class="w-full h-full object-cover" loading="lazy" decoding="async" />
            </button>
          </div>
        </section>

        <section class="space-y-4">
          <div class="um-card p-5 md:p-6">
            <div class="flex items-start justify-between gap-4">
              <div class="space-y-2 min-w-0">
                <div class="text-3xl md:text-4xl font-black text-warm-600 tracking-tight">¥{{ product.price }}</div>
                <div class="flex flex-wrap items-center gap-2">
                  <span class="rounded-full px-3 py-1 text-xs font-semibold bg-slate-50 border border-slate-200 text-slate-600">
                    {{ conditionText }}
                  </span>
                  <span class="rounded-full px-3 py-1 text-xs font-semibold bg-warm-50 border border-warm-100 text-warm-700">
                    {{ tradeTypeText }}
                  </span>
                  <span class="rounded-full px-3 py-1 text-xs font-semibold" :class="tradeStatusStyle">
                    {{ tradeStatusText }}
                  </span>
                </div>
              </div>

              <button
                v-if="!isSeller"
                @click="handleLike"
                :disabled="collectingLoading"
                class="p-3 rounded-full transition-all active:scale-95 disabled:opacity-60"
                :class="product.isCollected
                  ? 'bg-warm-50 text-warm-500 border border-warm-100'
                  : 'bg-white text-slate-400 hover:bg-warm-50 border border-slate-200'"
                aria-label="收藏商品"
              >
                <Heart :size="22" :fill="product.isCollected ? 'currentColor' : 'none'" />
              </button>
            </div>

            <h1 class="mt-4 text-2xl md:text-[1.9rem] leading-tight font-bold text-slate-900">{{ product.title }}</h1>

            <div class="mt-4 pt-4 border-t border-slate-100 grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm text-slate-600">
              <div class="inline-flex items-center gap-2 min-w-0">
                <MapPin :size="15" class="text-slate-400 shrink-0" />
                <span class="truncate">{{ product.schoolName }} · {{ product.campusName }}</span>
              </div>
              <div class="inline-flex items-center gap-2">
                <span class="text-slate-400">发布于</span>
                <span>{{ formatTime(product.createTime) }}</span>
              </div>
              <div class="inline-flex items-center gap-2 text-slate-500">
                <Heart :size="14" class="text-slate-400" />
                <span>{{ product.collectCount || 0 }} 人收藏</span>
              </div>
            </div>
          </div>

          <div
            class="um-card p-4 flex items-center justify-between gap-3 transition-colors"
            :class="!isSeller ? 'cursor-pointer hover:border-warm-200' : ''"
            @click="!isSeller && handleChat()"
          >
            <div class="flex items-center gap-3 min-w-0">
              <img
                :src="product.sellerAvatar || '/avatar-placeholder.svg'"
                class="w-12 h-12 rounded-full border border-slate-100 object-cover"
                :alt="product.sellerName || '卖家头像'"
                loading="lazy"
                decoding="async"
              />
              <div class="min-w-0">
                <div class="font-bold text-slate-800 flex items-center gap-2">
                  <span class="truncate">{{ isSeller ? '我发布的商品' : product.sellerName }}</span>
                  <span class="rounded-md border border-emerald-100 bg-emerald-50 px-1.5 py-0.5 text-[10px] font-semibold text-emerald-700">
                    已认证
                  </span>
                </div>
                <div class="text-xs text-slate-400 mt-0.5">{{ isSeller ? '你可以在这里管理商品状态' : '点击可直接发起聊天' }}</div>
              </div>
            </div>
            <div v-if="!isSeller" class="rounded-full border border-slate-200 bg-white p-2 text-slate-500">
              <MessageCircle :size="18" />
            </div>
          </div>

          <div class="um-card p-5 md:p-6 space-y-4">
            <h3 class="font-bold text-slate-800 flex items-center gap-2">
              <Package :size="18" class="text-warm-500" />
              商品描述
            </h3>
            <p class="text-slate-600 leading-7 whitespace-pre-wrap text-sm md:text-base">
              {{ product.description || '卖家暂未补充描述信息。' }}
            </p>
          </div>

          <div class="um-card p-5 md:p-6">
            <h3 class="font-bold text-slate-800 mb-4">交易信息</h3>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
              <div class="rounded-xl border border-slate-100 bg-slate-50 p-3">
                <p class="text-slate-400">交易方式</p>
                <p class="mt-1 font-semibold text-slate-700">{{ tradeTypeText }}</p>
              </div>
              <div class="rounded-xl border border-slate-100 bg-slate-50 p-3">
                <p class="text-slate-400">运费</p>
                <p class="mt-1 font-semibold text-slate-700">{{ product.deliveryFee > 0 ? `¥${product.deliveryFee}` : '免邮/协商' }}</p>
              </div>
              <div class="rounded-xl border border-slate-100 bg-slate-50 p-3 sm:col-span-2">
                <p class="text-slate-400">发布地点</p>
                <p class="mt-1 font-semibold text-slate-700">{{ product.schoolName }} · {{ product.campusName }}</p>
              </div>
            </div>
          </div>
        </section>
      </div>

      <section v-if="similarProducts.length > 0" class="um-card p-5 md:p-6">
        <h3 class="font-bold text-slate-800 mb-5 flex items-center gap-2">
          <Sparkles :size="18" class="text-warm-500" />
          相似商品推荐
        </h3>
        <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3 md:gap-4">
          <article
            v-for="item in similarProducts"
            :key="item.productId"
            @click="goToSimilarProduct(item.productId)"
            class="group cursor-pointer"
          >
            <div class="relative aspect-square rounded-2xl overflow-hidden border border-slate-200 bg-white">
              <img
                :src="item.image || ''"
                :alt="item.title"
                class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                loading="lazy"
                decoding="async"
              />
              <div
                v-if="item.recommendType"
                class="absolute top-2 left-2 rounded-md bg-slate-900/70 px-2 py-0.5 text-[10px] text-white"
              >
                {{ item.recommendType === 'content' ? '相似' : item.recommendType === 'cf' ? '猜你喜欢' : '推荐' }}
              </div>
            </div>
            <h4 class="mt-2 text-sm text-slate-700 line-clamp-2 group-hover:text-warm-600 transition-colors">
              {{ item.title }}
            </h4>
            <div class="flex items-baseline gap-2 mt-1">
              <span class="text-warm-600 font-bold">¥{{ item.price }}</span>
              <span v-if="item.collectCount" class="text-slate-400 text-xs">{{ item.collectCount }}收藏</span>
            </div>
          </article>
        </div>
      </section>

      <section v-else-if="loadingSimilar" class="um-card p-6">
        <div class="flex items-center justify-center py-6">
          <div class="animate-spin rounded-full h-6 w-6 border-2 border-warm-500 border-t-transparent"></div>
          <span class="ml-2 text-slate-400 text-sm">加载推荐中...</span>
        </div>
      </section>
    </div>

    <div v-else class="max-w-6xl mx-auto px-4 py-20 text-center text-slate-500">商品不存在或已下架</div>

    <div
      v-if="product"
      class="fixed bottom-0 left-0 right-0 z-40 border-t border-slate-200 bg-white/96 backdrop-blur p-4 md:px-8 md:py-4"
    >
      <div class="max-w-6xl mx-auto flex items-center gap-4">
        <button
          class="flex flex-col items-center gap-0.5 text-xs font-medium text-slate-500 hover:text-warm-600 transition-colors"
          @click="router.push('/market')"
        >
          <ShoppingBag :size="20" />
          <span>集市</span>
        </button>

        <div class="h-8 w-px bg-slate-200 mx-1"></div>

        <template v-if="isSeller">
          <button
            v-if="canSellerEdit"
            @click="handleEdit"
            class="flex-1 bg-white border border-slate-200 text-slate-800 font-bold py-3.5 rounded-2xl transition-colors"
          >
            编辑商品
          </button>
          <button
            v-if="canSellerOffShelf"
            @click="handleOffShelf"
            class="flex-1 bg-red-50 hover:bg-red-100 text-red-600 font-bold py-3.5 rounded-2xl transition-colors"
          >
            下架商品
          </button>
          <button
            v-else-if="canSellerRelist"
            @click="handleRelist"
            class="flex-1 bg-gradient-to-r from-emerald-500 to-green-500 hover:from-emerald-600 hover:to-green-600 text-white font-bold py-3.5 rounded-2xl transition-all shadow-lg shadow-emerald-200"
          >
            重新上架
          </button>
          <button
            v-else
            disabled
            class="flex-1 bg-slate-200 text-slate-400 font-bold py-3.5 rounded-2xl cursor-not-allowed"
          >
            {{ sellerPrimaryActionText }}
          </button>
          <button
            v-if="canSellerDelete"
            @click="handleDelete"
            :disabled="deletingLoading"
            class="flex-1 bg-white border border-red-200 text-red-600 font-bold py-3.5 rounded-2xl transition-colors hover:bg-red-50 disabled:opacity-50 disabled:cursor-not-allowed inline-flex items-center justify-center gap-1.5"
          >
            <Trash2 :size="16" />
            {{ deletingLoading ? '删除中...' : '删除商品' }}
          </button>
        </template>

        <template v-else>
          <button
            @click="handleChat"
            class="flex-1 bg-white border border-slate-200 text-slate-800 font-bold py-3.5 rounded-2xl transition-colors flex items-center justify-center gap-2"
          >
            <MessageCircle :size="18" />
            我想要
          </button>

          <button
            v-if="!isOrderReadonlyView && product.tradeStatus === TradeStatus.ON_SALE && isReviewPassed(product.reviewStatus)"
            @click="handleBuy"
            :disabled="buyingLoading"
            class="flex-[2] bg-gradient-to-r from-warm-500 to-orange-500 hover:from-warm-600 hover:to-orange-600 text-white font-bold py-3.5 rounded-2xl transition-all shadow-lg shadow-warm-200 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ buyingLoading ? '下单中...' : '立即购买' }}
          </button>
          <button
            v-else
            disabled
            class="flex-[2] bg-slate-200 text-slate-400 font-bold py-3.5 rounded-2xl cursor-not-allowed"
          >
            {{ isOrderReadonlyView ? '请在我的订单处理' : tradeStatusText }}
          </button>
        </template>
      </div>
    </div>

    <div
      v-if="showImagePreview && previewImage"
      class="fixed inset-0 z-[60] bg-black/92 backdrop-blur-sm p-4 md:p-8"
      @click.self="closeImagePreview"
    >
      <button
        class="absolute top-4 right-4 z-10 inline-flex items-center gap-1.5 px-3 py-2 rounded-full bg-black/55 text-white hover:bg-black/70 border border-white/25 transition-colors"
        @click.stop.prevent="closeImagePreview"
        aria-label="关闭预览"
      >
        <X :size="22" />
        <span class="text-xs font-medium">关闭</span>
      </button>

      <div class="h-full w-full flex items-center justify-center">
        <button
          v-if="hasMultiImages"
          class="absolute left-3 md:left-6 p-2 md:p-3 rounded-full bg-white/10 text-white hover:bg-white/20 transition-colors"
          @click.stop.prevent="movePreview(-1)"
          aria-label="上一张图片"
        >
          <ChevronLeft :size="22" />
        </button>

        <img
          :src="previewImage"
          :alt="product?.title || '商品图片'"
          class="max-w-full max-h-full object-contain rounded-2xl"
          @click.stop.prevent="closeImagePreview"
        />

        <button
          v-if="hasMultiImages"
          class="absolute right-3 md:right-6 p-2 md:p-3 rounded-full bg-white/10 text-white hover:bg-white/20 transition-colors"
          @click.stop.prevent="movePreview(1)"
          aria-label="下一张图片"
        >
          <ChevronRight :size="22" />
        </button>
      </div>

      <div
        v-if="hasMultiImages"
        class="absolute bottom-4 left-1/2 -translate-x-1/2 flex max-w-[90vw] gap-2 overflow-x-auto no-scrollbar"
      >
        <button
          v-for="(img, idx) in productImages"
          :key="`${img}-preview-${idx}`"
          @click.stop.prevent="previewImageIndex = idx; currentImageIndex = idx"
          class="w-14 h-14 rounded-lg overflow-hidden border-2 transition-all"
          :class="previewImageIndex === idx ? 'border-white' : 'border-transparent opacity-70 hover:opacity-100'"
        >
          <img :src="img" class="w-full h-full object-cover" loading="lazy" decoding="async" />
        </button>
      </div>

      <button
        class="absolute bottom-4 right-4 z-10 px-3 py-2 rounded-full bg-black/55 text-white text-xs font-medium border border-white/25 hover:bg-black/70 transition-colors"
        @click.stop.prevent="closeImagePreview"
      >
        关闭预览
      </button>
    </div>
  </div>
</template>

<style scoped>
.no-scrollbar::-webkit-scrollbar {
  display: none;
}

.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
