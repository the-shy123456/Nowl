<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  Package,
  Clock,
  CheckCircle,
  XCircle,
  ChevronRight,
  Timer,
  Star,
  CreditCard,
  Truck,
  CircleDollarSign,
  ShieldAlert,
} from 'lucide-vue-next'
import {
  getMyOrders,
  payOrder,
  confirmOrder,
  cancelOrder,
  deliverOrder,
  applyRefund,
  processRefund,
} from '@/api/modules/order'
import { hasReviewed } from '@/api/modules/review'
import { getGoodsDetail } from '@/api/modules/goods'
import { ElMessage, ElMessageBox } from '@/utils/feedback'
import { normalizeMediaUrl } from '@/utils/media'
import type { OrderInfo } from '@/types'
import { DisputeStatus, ORDER_STATUS_MAP, OrderStatus, PAGE_CONSTANTS } from '@/constants'
import { useUserStore } from '@/stores/user'
import SubPageShell from '@/components/SubPageShell.vue'
import { usePaginatedList } from '@/composables/usePaginatedList'
import {
  createQueryBinding,
  parsePositiveIntQuery,
  serializePageQuery,
  useListQuerySync,
} from '@/composables/useListQuerySync'
import { useAutoRefreshOnVisible } from '@/composables/useAutoRefreshOnVisible'
import InfiniteListFooter from '@/components/InfiniteListFooter.vue'
import { RefundStatusMap } from '@/constants/statusMaps'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref<'buy' | 'sell'>('buy')
const currentUserId = computed(() => userStore.userInfo?.userId ?? null)
const ordersCache = ref<Record<'buy' | 'sell', OrderInfo[]>>({
  buy: [],
  sell: [],
})
const cacheLoaded = ref<Record<'buy' | 'sell', boolean>>({
  buy: false,
  sell: false,
})

const isBuyerPerspective = (order: OrderInfo) => {
  if (currentUserId.value == null) return activeTab.value === 'buy'
  return order.buyerId === currentUserId.value
}

const isSellerPerspective = (order: OrderInfo) => {
  if (currentUserId.value == null) return activeTab.value === 'sell'
  return order.sellerId === currentUserId.value
}

const counterpartyLabel = (order: OrderInfo) => (isBuyerPerspective(order) ? '卖家' : '买家')
const counterpartyName = (order: OrderInfo) =>
  (isBuyerPerspective(order) ? order.sellerName : order.buyerName) || '--'

// 存储订单评价状态
const reviewedMap = ref<Record<number, boolean>>({})
const orderActionPending = ref<Set<number>>(new Set())

const isOrderActionPending = (orderId: number) => orderActionPending.value.has(orderId)

const markOrderActionPending = (orderId: number) => {
  const next = new Set(orderActionPending.value)
  next.add(orderId)
  orderActionPending.value = next
}

const clearOrderActionPending = (orderId: number) => {
  if (!orderActionPending.value.has(orderId)) return
  const next = new Set(orderActionPending.value)
  next.delete(orderId)
  orderActionPending.value = next
}

const orderStatusMap: Record<number, { text: string; color: string; icon: typeof Clock }> = {
  [OrderStatus.PENDING_PAY]: {
    ...ORDER_STATUS_MAP[OrderStatus.PENDING_PAY],
    icon: Clock,
  },
  [OrderStatus.PENDING_DELIVERY]: {
    ...ORDER_STATUS_MAP[OrderStatus.PENDING_DELIVERY],
    icon: Package,
  },
  [OrderStatus.PENDING_RECEIVE]: {
    ...ORDER_STATUS_MAP[OrderStatus.PENDING_RECEIVE],
    icon: Truck,
  },
  [OrderStatus.COMPLETED]: {
    ...ORDER_STATUS_MAP[OrderStatus.COMPLETED],
    icon: CheckCircle,
  },
  [OrderStatus.CANCELLED]: {
    ...ORDER_STATUS_MAP[OrderStatus.CANCELLED],
    icon: XCircle,
  },
  [OrderStatus.ENDED]: {
    ...ORDER_STATUS_MAP[OrderStatus.ENDED],
    icon: XCircle,
  },
}

const getStatusMeta = (status: number) =>
  orderStatusMap[status] || { text: '未知状态', color: 'bg-slate-100 text-slate-500', icon: Clock }

const getRefundMeta = (status?: number) => {
  if (status === undefined || status === null) return null
  const normalized = Number(status)
  if (!Number.isFinite(normalized)) return null
  return RefundStatusMap[normalized] || { text: '未知', color: 'bg-slate-100 text-slate-500' }
}

const getRefundStatus = (order: OrderInfo): number | null => {
  if (order.refundStatus === undefined || order.refundStatus === null) return null
  const status = Number(order.refundStatus)
  return Number.isFinite(status) ? status : null
}

const formatAmountText = (value?: number | null) => {
  const amount = Number(value)
  if (!Number.isFinite(amount) || amount <= 0) return ''
  return amount.toString()
}

const extractHandleRemark = (text?: string) => {
  const content = String(text || '').trim()
  if (!content) return ''
  const marker = '处理说明：'
  const markerIndex = content.indexOf(marker)
  if (markerIndex === -1) return ''
  return content.slice(markerIndex + marker.length).trim()
}

const isRefundPending = (order: OrderInfo) => getRefundStatus(order) === 1

const formatRefundBadgeText = (order: OrderInfo) => {
  const meta = getRefundMeta(order.refundStatus)
  if (!meta) return ''
  const status = getRefundStatus(order)
  if (status === 2) {
    const amount = Number(order.refundAmount)
    if (Number.isFinite(amount) && amount > 0) {
      return `${meta.text}（¥${amount}）`
    }
  }
  return meta.text
}

// 检查订单评价状态
const checkReviewStatus = async (currentOrders: OrderInfo[]) => {
  const completedOrders = currentOrders.filter(o => o.orderStatus === OrderStatus.COMPLETED)
  const results = await Promise.allSettled(
    completedOrders.map(order =>
      hasReviewed({ targetType: 0, orderId: order.orderId })
        .then(res => ({ orderId: order.orderId, reviewed: res === true })),
    ),
  )
  for (const result of results) {
    if (result.status === 'fulfilled') {
      reviewedMap.value[result.value.orderId] = result.value.reviewed
    }
  }
}

const {
  list: orders,
  pageNum,
  hasMore,
  showInitialLoading,
  isLoadingMore,
  loadMoreTrigger,
  refresh,
} = usePaginatedList<OrderInfo>({
  pageSize: PAGE_CONSTANTS.LARGE_PAGE_SIZE,
  fetchPage: ({ pageNum, pageSize }) =>
    getMyOrders({
      pageNum,
      pageSize,
      orderType: activeTab.value,
    }),
  onSuccess: async list => {
    ordersCache.value[activeTab.value] = [...list]
    cacheLoaded.value[activeTab.value] = true
    const currentTabOrders = list.filter(order => activeTab.value === 'buy'
      ? isBuyerPerspective(order)
      : isSellerPerspective(order))
    await checkReviewStatus(currentTabOrders)
  },
  onError: () => {
    ElMessage.error('获取订单列表失败')
  },
})

useListQuerySync([
  createQueryBinding({
    key: 'type',
    state: activeTab,
    defaultValue: 'buy',
    parse: raw => {
      const value = Array.isArray(raw) ? raw[0] : raw
      return value === 'sell' ? 'sell' : 'buy'
    },
    serialize: value => (value === 'buy' ? undefined : value),
  }),
  createQueryBinding({
    key: 'page',
    state: pageNum,
    defaultValue: 1,
    parse: raw => parsePositiveIntQuery(raw, 1),
    serialize: value => serializePageQuery(value, 1),
  }),
], {
  onQueryApplied: () => {
    refresh(pageNum.value)
  },
})

const setLoadMoreTrigger = (element: HTMLElement | null) => {
  loadMoreTrigger.value = element
}

const visibleOrders = computed(() => {
  if (activeTab.value === 'buy') {
    return orders.value.filter(isBuyerPerspective)
  }
  return orders.value.filter(isSellerPerspective)
})

const orderStats = computed(() => {
  const list = visibleOrders.value
  const pending = list.filter(item => item.orderStatus === OrderStatus.PENDING_PAY).length
  const shipping = list.filter(item => item.orderStatus === OrderStatus.PENDING_DELIVERY || item.orderStatus === OrderStatus.PENDING_RECEIVE).length
  const completed = list.filter(item => item.orderStatus === OrderStatus.COMPLETED).length
  const refunding = list.filter(isRefundPending).length
  return { total: list.length, pending, shipping, completed, refunding }
})

// 切换标签
const switchTab = (tab: 'buy' | 'sell') => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  pageNum.value = 1
  if (cacheLoaded.value[tab]) {
    orders.value = [...ordersCache.value[tab]]
  }
  void refresh(1)
}

useAutoRefreshOnVisible({
  refresh: () => refresh(pageNum.value),
})

// 通用订单操作
const handleOrderAction = async (
  orderId: number,
  confirmMsg: string,
  action: (id: number) => Promise<unknown>,
  successMsg: string,
) => {
  if (isOrderActionPending(orderId)) {
    ElMessage.warning('当前订单正在处理中，请勿重复提交')
    return
  }
  markOrderActionPending(orderId)
  try {
    await ElMessageBox.confirm(confirmMsg, '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    })
    await action(orderId)
    ElMessage.success(successMsg)
    void refresh()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败，请稍后重试')
    }
  } finally {
    clearOrderActionPending(orderId)
  }
}

const canPayOrder = (order: OrderInfo) =>
  isBuyerPerspective(order) && order.orderStatus === OrderStatus.PENDING_PAY

const canDeliverOrder = (order: OrderInfo) =>
  isSellerPerspective(order) && order.orderStatus === OrderStatus.PENDING_DELIVERY

const canConfirmOrder = (order: OrderInfo) =>
  isBuyerPerspective(order)
  && order.orderStatus === OrderStatus.PENDING_RECEIVE
  && !isRefundPending(order)
  && !hasActiveDispute(order)

const canCancelOrder = (order: OrderInfo) =>
  isBuyerPerspective(order) && order.orderStatus === OrderStatus.PENDING_PAY

const canProcessRefund = (order: OrderInfo) =>
  isSellerPerspective(order) && isRefundPending(order)

const handlePay = (order: OrderInfo) => {
  if (!canPayOrder(order)) {
    ElMessage.warning('当前订单状态不可支付')
    return
  }
  void handleOrderAction(order.orderId, '确认支付该订单？', payOrder, '支付成功')
}

const handleDeliver = (order: OrderInfo) => {
  if (!canDeliverOrder(order)) {
    ElMessage.warning('当前订单状态不可发货')
    return
  }
  void handleOrderAction(order.orderId, '确认已发货？', deliverOrder, '发货成功')
}

const handleConfirm = (order: OrderInfo) => {
  if (isRefundPending(order)) {
    ElMessage.warning('订单退款处理中，暂不可确认收货')
    return
  }
  if (hasActiveDispute(order)) {
    ElMessage.warning('订单存在进行中纠纷，暂不可确认收货')
    return
  }
  if (!canConfirmOrder(order)) {
    ElMessage.warning('当前订单状态不可确认收货')
    return
  }
  void handleOrderAction(order.orderId, '确认收货？', confirmOrder, '确认收货成功')
}

const handleCancel = (order: OrderInfo) => {
  if (!canCancelOrder(order)) {
    ElMessage.warning('当前订单状态不可取消')
    return
  }
  void handleOrderAction(order.orderId, '确认取消该订单？', cancelOrder, '订单已取消')
}

const handleApplyRefund = async (order: OrderInfo) => {
  if (isOrderActionPending(order.orderId)) {
    ElMessage.warning('当前订单正在处理中，请勿重复提交')
    return
  }
  if (hasActiveDispute(order)) {
    ElMessage.warning('订单存在进行中纠纷，暂不可申请退款')
    return
  }
  if (!canApplyRefund(order)) {
    ElMessage.warning('当前订单状态不可申请退款')
    return
  }
  markOrderActionPending(order.orderId)
  try {
    const { value: reason } = await ElMessageBox.prompt(
      '请填写退款原因',
      '申请退款',
      {
        confirmButtonText: '提交申请',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：卖家长期未发货 / 商品问题等',
        inputValidator: val => !!val || '退款原因不能为空',
      },
    )

    await applyRefund(order.orderId, {
      reason,
      amount: Number(order.totalAmount || 0),
    })
    ElMessage.success('退款申请已提交')
    void refresh()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('退款申请失败，请稍后重试')
    }
  } finally {
    clearOrderActionPending(order.orderId)
  }
}

const handleProcessRefund = async (order: OrderInfo, action: 'approve' | 'reject') => {
  if (isOrderActionPending(order.orderId)) {
    ElMessage.warning('当前订单正在处理中，请勿重复提交')
    return
  }
  if (!canProcessRefund(order)) {
    ElMessage.warning('当前订单状态不可处理退款')
    return
  }
  const confirmText = action === 'approve' ? '确认同意退款？' : '确认拒绝退款？'
  const successText = action === 'approve' ? '已同意退款' : '已拒绝退款'

  markOrderActionPending(order.orderId)
  try {
    const { value: remark } = await ElMessageBox.prompt(
      action === 'approve' ? '可填写处理备注（选填）' : '请填写拒绝原因（选填）',
      action === 'approve' ? '同意退款' : '拒绝退款',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        inputPlaceholder: action === 'approve' ? '例如：已核实' : '例如：已发货且证据充足',
      },
    )

    await ElMessageBox.confirm(confirmText, '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    })

    await processRefund(order.orderId, { action, remark: remark || undefined })
    ElMessage.success(successText)
    void refresh()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('退款处理失败，请稍后重试')
    }
  } finally {
    clearOrderActionPending(order.orderId)
  }
}

const goToDispute = (order: OrderInfo) => {
  if (isRefundPending(order)) {
    ElMessage.warning('订单退款处理中，暂不可发起纠纷')
    return
  }
  if (!canRaiseDispute(order)) {
    ElMessage.warning('仅支持待确认收货订单发起纠纷')
    return
  }
  router.push(`/dispute/create?type=0&id=${order.orderId}`)
}

const formatRefundCountdown = (deadline?: string) => {
  if (!deadline) return ''
  const now = Date.now()
  const end = new Date(deadline).getTime()
  const diff = end - now
  if (diff <= 0) return '即将自动退款'

  const hours = Math.floor(diff / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  return `${hours}小时${minutes}分钟后自动退款`
}

// 查看订单关联商品
const viewDetail = async (order: OrderInfo) => {
  if (!order.productId) {
    ElMessage.info('关联商品不存在，当前仅保留订单记录')
    return
  }

  try {
    await getGoodsDetail(order.productId)
    await router.push({
      path: `/product/${order.productId}`,
      query: { fromOrderId: String(order.orderId) },
    })
  } catch {
    ElMessage.info('商品可能已删除或下架，当前仅保留订单记录')
  }
}

const getOrderReviewTargetId = (order: OrderInfo) =>
  isBuyerPerspective(order) ? order.sellerId : order.buyerId

const hasValidOrderReviewTarget = (order: OrderInfo) => {
  const reviewedId = getOrderReviewTargetId(order)
  return Number.isFinite(reviewedId) && reviewedId > 0
}

// 去评价
const goToReview = (order: OrderInfo) => {
  if (!canCreateReview(order)) {
    ElMessage.warning('当前订单暂不可评价')
    return
  }
  const reviewedId = getOrderReviewTargetId(order)
  if (!hasValidOrderReviewTarget(order)) {
    ElMessage.warning('评价对象无效')
    return
  }
  router.push(`/review/create?type=0&id=${order.orderId}&reviewedId=${reviewedId}`)
}

// 格式化时间
const formatTime = (time: string | undefined) => {
  if (!time) return '--'
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// 计算自动确认收货倒计时（7天）
const getAutoConfirmCountdown = (deliveryTime: string | undefined): string => {
  if (!deliveryTime) return ''

  const deliveryDate = new Date(deliveryTime)
  const autoConfirmDate = new Date(deliveryDate.getTime() + 7 * 24 * 60 * 60 * 1000)
  const now = new Date()

  const diffMs = autoConfirmDate.getTime() - now.getTime()
  if (diffMs <= 0) return '即将自动确认'

  const diffDays = Math.floor(diffMs / (24 * 60 * 60 * 1000))
  const diffHours = Math.floor((diffMs % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000))

  if (diffDays > 0) {
    return `${diffDays}天${diffHours}小时后自动确认`
  }
  return `${diffHours}小时后自动确认`
}

const canApplyRefund = (order: OrderInfo) =>
  isBuyerPerspective(order)
  && (order.orderStatus === OrderStatus.PENDING_DELIVERY || order.orderStatus === OrderStatus.PENDING_RECEIVE)
  && (() => {
    const status = getRefundStatus(order)
    return status === null || status === 0 || status === 3
  })()
  && !hasActiveDispute(order)

const getActiveDisputeId = (order: OrderInfo) => {
  const disputeId = Number(order.activeDisputeId)
  return Number.isFinite(disputeId) && disputeId > 0 ? disputeId : null
}

const getLatestClosedDisputeId = (order: OrderInfo) => {
  const disputeId = Number(order.latestClosedDisputeId)
  return Number.isFinite(disputeId) && disputeId > 0 ? disputeId : null
}

const hasActiveDispute = (order: OrderInfo) => {
  if (getActiveDisputeId(order) === null) return false
  const status = Number(order.activeDisputeStatus)
  if (!Number.isFinite(status)) return false
  return status === DisputeStatus.PENDING || status === DisputeStatus.PROCESSING
}

const hasLatestClosedDispute = (order: OrderInfo) => {
  if (getLatestClosedDisputeId(order) === null) return false
  const status = Number(order.latestClosedDisputeStatus)
  if (!Number.isFinite(status)) return false
  return status === DisputeStatus.RESOLVED || status === DisputeStatus.REJECTED
}

const latestClosedDisputeText = (order: OrderInfo) => {
  const parts: string[] = []
  const refundText = formatAmountText(order.latestClosedDisputeRefundAmount)
  if (refundText) {
    parts.push(`退款¥${refundText}`)
  }
  const creditPenalty = Number(order.latestClosedDisputeCreditPenalty)
  if (Number.isFinite(creditPenalty) && creditPenalty > 0) {
    parts.push(`扣除对方信用分${creditPenalty}分`)
  }
  const remark = extractHandleRemark(order.latestClosedDisputeResult)
  if (remark) {
    parts.push(`处理说明：${remark}`)
  }
  if (parts.length > 0) {
    return parts.join('，')
  }
  return String(order.latestClosedDisputeResult || '').trim()
}

const getDisplayDisputeId = (order: OrderInfo) => {
  return getActiveDisputeId(order) ?? getLatestClosedDisputeId(order)
}

const canRaiseDispute = (order: OrderInfo) =>
  isBuyerPerspective(order)
  && order.orderStatus === OrderStatus.PENDING_RECEIVE
  && !isRefundPending(order)
  && !hasActiveDispute(order)

const canCreateReview = (order: OrderInfo) =>
  order.orderStatus === OrderStatus.COMPLETED
  && hasValidOrderReviewTarget(order)
  && reviewedMap.value[order.orderId] === false
  && !hasActiveDispute(order)

const openDisputeDetail = (order: OrderInfo) => {
  const disputeId = getDisplayDisputeId(order)
  if (disputeId === null) {
    router.push('/dispute/list')
    return
  }
  router.push(`/dispute/${disputeId}`)
}

const productImage = (order: OrderInfo) =>
  normalizeMediaUrl(order.productImage) || 'https://via.placeholder.com/320x240?text=No+Image'

onMounted(() => {
  void refresh(pageNum.value)
  const preloadType: 'buy' | 'sell' = activeTab.value === 'buy' ? 'sell' : 'buy'
  void getMyOrders({
    pageNum: 1,
    pageSize: PAGE_CONSTANTS.LARGE_PAGE_SIZE,
    orderType: preloadType,
  }).then(res => {
    ordersCache.value[preloadType] = [...(res.records || [])]
    cacheLoaded.value[preloadType] = true
  }).catch((error) => {
    console.warn('预加载另一侧订单失败', error)
  })
})
</script>

<template>
  <SubPageShell title="我的订单" subtitle="查看买入与卖出订单" back-to="/profile" max-width="lg" :use-card="false">
    <div class="space-y-4">
      <section class="um-card p-4 md:p-5">
        <div class="flex items-center justify-between gap-3 flex-wrap">
          <div class="inline-flex rounded-2xl border border-slate-200 bg-slate-50 p-1">
            <button
              @click="switchTab('buy')"
              class="px-5 py-2 rounded-xl text-sm font-semibold transition-colors"
              :class="activeTab === 'buy' ? 'bg-white text-warm-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'"
            >
              我买到的
            </button>
            <button
              @click="switchTab('sell')"
              class="px-5 py-2 rounded-xl text-sm font-semibold transition-colors"
              :class="activeTab === 'sell' ? 'bg-white text-warm-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'"
            >
              我卖出的
            </button>
          </div>

          <div class="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
            <div class="rounded-xl bg-warm-50 border border-warm-100 px-3 py-2 text-warm-700">
              <div class="text-[11px] text-warm-500">总订单</div>
              <div class="text-sm font-bold">{{ orderStats.total }}</div>
            </div>
            <div class="rounded-xl bg-yellow-50 border border-yellow-100 px-3 py-2 text-yellow-700">
              <div class="text-[11px] text-yellow-500">待处理</div>
              <div class="text-sm font-bold">{{ orderStats.pending }}</div>
            </div>
            <div class="rounded-xl bg-blue-50 border border-blue-100 px-3 py-2 text-blue-700">
              <div class="text-[11px] text-blue-500">运输中</div>
              <div class="text-sm font-bold">{{ orderStats.shipping }}</div>
            </div>
            <div class="rounded-xl bg-emerald-50 border border-emerald-100 px-3 py-2 text-emerald-700">
              <div class="text-[11px] text-emerald-500">已完成</div>
              <div class="text-sm font-bold">{{ orderStats.completed }}</div>
            </div>
          </div>
        </div>

        <p v-if="orderStats.refunding > 0" class="mt-3 text-xs text-orange-600 inline-flex items-center gap-1">
          <ShieldAlert :size="14" />
          当前有 {{ orderStats.refunding }} 笔退款处理中
        </p>
      </section>

      <div v-if="showInitialLoading" class="text-center py-12">
        <div class="inline-block animate-spin rounded-full h-10 w-10 border-4 border-warm-500 border-t-transparent"></div>
        <p class="mt-3 text-slate-500 text-sm">正在加载订单...</p>
      </div>

      <div v-else-if="visibleOrders.length === 0" class="um-card py-16 text-center">
        <Package :size="48" class="mx-auto text-slate-300 mb-3" />
        <p class="text-slate-500">{{ activeTab === 'buy' ? '还没有买入订单' : '还没有卖出订单' }}</p>
      </div>

      <div v-else class="space-y-4">
        <article
          v-for="order in visibleOrders"
          :key="order.orderId"
          class="um-card p-4 md:p-5"
        >
          <header class="flex items-start justify-between gap-3 pb-3 border-b border-slate-100">
            <div class="min-w-0">
              <p class="text-xs text-slate-400">订单号 {{ order.orderId }}</p>
              <p class="text-xs text-slate-400 mt-1">{{ formatTime(order.createTime) }}</p>
            </div>
            <div class="px-3 py-1 rounded-full text-xs font-semibold inline-flex items-center gap-1" :class="getStatusMeta(order.orderStatus).color">
              <component :is="getStatusMeta(order.orderStatus).icon" :size="13" />
              {{ getStatusMeta(order.orderStatus).text }}
            </div>
          </header>

          <div class="pt-4 flex flex-col md:flex-row gap-4">
            <button
              class="w-full md:w-28 h-28 rounded-2xl overflow-hidden border border-slate-200 bg-slate-50 shrink-0"
              @click="viewDetail(order)"
            >
              <img :src="productImage(order)" :alt="order.productTitle || '商品'" class="w-full h-full object-cover" />
            </button>

            <div class="flex-1 min-w-0">
              <h3 class="font-semibold text-slate-900 line-clamp-2 leading-6">{{ order.productTitle || '商品已下架' }}</h3>

              <div class="mt-2 space-y-1.5 text-sm">
                <p class="text-slate-600">
                  {{ counterpartyLabel(order) }}：
                  <span class="font-medium text-slate-700">{{ counterpartyName(order) }}</span>
                </p>
                <p class="text-slate-600 inline-flex items-center gap-1">
                  <CircleDollarSign :size="14" class="text-slate-400" />
                  交易金额：
                  <span class="font-bold text-warm-600">¥{{ order.totalAmount }}</span>
                </p>
                <p v-if="order.deliveryTime" class="text-xs text-slate-500">
                  发货时间：{{ formatTime(order.deliveryTime) }}
                </p>
                <p v-if="order.remark" class="text-xs text-slate-500">备注：{{ order.remark }}</p>
              </div>

              <div class="mt-2 flex flex-wrap items-center gap-2">
                <span
                  v-if="getRefundMeta(order.refundStatus) && !hasLatestClosedDispute(order)"
                  class="text-[11px] rounded-full px-2.5 py-1"
                  :class="getRefundMeta(order.refundStatus)?.color"
                >
                  退款：{{ formatRefundBadgeText(order) }}
                </span>
                <span
                  v-if="order.orderStatus === OrderStatus.PENDING_RECEIVE && order.deliveryTime"
                  class="text-[11px] rounded-full px-2.5 py-1 bg-orange-50 text-orange-600 inline-flex items-center gap-1"
                >
                  <Timer :size="12" />
                  {{ getAutoConfirmCountdown(order.deliveryTime) }}
                </span>
                <span
                  v-if="isRefundPending(order) && order.refundDeadline"
                  class="text-[11px] rounded-full px-2.5 py-1 bg-red-50 text-red-600 inline-flex items-center gap-1"
                >
                  <Clock :size="12" />
                  {{ formatRefundCountdown(order.refundDeadline) }}
                </span>
                <span
                  v-if="hasActiveDispute(order)"
                  class="text-[11px] rounded-full px-2.5 py-1 bg-orange-50 text-orange-700 inline-flex items-center gap-1"
                >
                  <ShieldAlert :size="12" />
                  纠纷处理中
                </span>
              </div>
              <button
                v-if="!hasActiveDispute(order) && hasLatestClosedDispute(order) && latestClosedDisputeText(order)"
                class="mt-2 text-left text-xs text-red-600 hover:text-red-700 line-clamp-2"
                @click="openDisputeDetail(order)"
              >
                纠纷：{{ latestClosedDisputeText(order) }}
              </button>
              <p v-if="order.refundProcessRemark && !hasLatestClosedDispute(order)" class="mt-2 text-xs text-slate-400">
                处理说明：{{ order.refundProcessRemark }}
              </p>
            </div>
          </div>

          <footer class="mt-4 pt-4 border-t border-slate-100 flex flex-wrap gap-2">
            <button
              v-if="canPayOrder(order)"
              @click="handlePay(order)"
              class="um-btn px-3.5 py-2 text-sm bg-warm-500 text-white hover:bg-warm-600"
            >
              <span class="inline-flex items-center gap-1">
                <CreditCard :size="14" />
                立即支付
              </span>
            </button>

            <button
              v-if="canDeliverOrder(order)"
              @click="handleDeliver(order)"
              class="um-btn px-3.5 py-2 text-sm bg-warm-500 text-white hover:bg-warm-600"
            >
              立即发货
            </button>

            <button
              v-if="canConfirmOrder(order)"
              @click="handleConfirm(order)"
              class="um-btn px-3.5 py-2 text-sm bg-emerald-500 text-white hover:bg-emerald-600"
            >
              确认收货
            </button>

            <button
              v-if="canCancelOrder(order)"
              @click="handleCancel(order)"
              class="um-btn px-3.5 py-2 text-sm bg-slate-100 text-slate-600 hover:bg-slate-200"
            >
              取消订单
            </button>

            <button
              v-if="canApplyRefund(order)"
              @click="handleApplyRefund(order)"
              class="um-btn px-3.5 py-2 text-sm bg-orange-500 text-white hover:bg-orange-600"
            >
              申请退款
            </button>

            <button
              v-if="canProcessRefund(order)"
              @click="handleProcessRefund(order, 'approve')"
              class="um-btn px-3.5 py-2 text-sm bg-emerald-500 text-white hover:bg-emerald-600"
            >
              同意退款
            </button>

            <button
              v-if="canProcessRefund(order)"
              @click="handleProcessRefund(order, 'reject')"
              class="um-btn px-3.5 py-2 text-sm bg-red-500 text-white hover:bg-red-600"
            >
              拒绝退款
            </button>

            <button
              v-if="canRaiseDispute(order)"
              @click="goToDispute(order)"
              class="um-btn px-3.5 py-2 text-sm bg-warm-500 text-white hover:bg-warm-600"
            >
              发起纠纷
            </button>

            <button
              v-if="hasActiveDispute(order) || hasLatestClosedDispute(order)"
              @click="openDisputeDetail(order)"
              class="um-btn px-3.5 py-2 text-sm"
              :class="hasActiveDispute(order)
                ? 'bg-orange-100 text-orange-700 hover:bg-orange-200'
                : 'bg-red-50 text-red-600 hover:bg-red-100'"
            >
              查看纠纷
            </button>

            <button
              v-if="canCreateReview(order)"
              @click="goToReview(order)"
              class="um-btn px-3.5 py-2 text-sm bg-yellow-500 text-white hover:bg-yellow-600"
            >
              <span class="inline-flex items-center gap-1">
                <Star :size="14" />
                去评价
              </span>
            </button>

            <span
              v-if="order.orderStatus === OrderStatus.COMPLETED && reviewedMap[order.orderId] && !hasActiveDispute(order)"
              class="um-btn px-3.5 py-2 text-sm bg-slate-50 text-slate-400 inline-flex items-center gap-1"
            >
              <CheckCircle :size="14" />
              已评价
            </span>

            <button
              @click="viewDetail(order)"
              class="um-btn px-3.5 py-2 text-sm bg-slate-100 text-slate-600 hover:bg-slate-200"
            >
              <span class="inline-flex items-center gap-1">
                查看详情
                <ChevronRight :size="14" />
              </span>
            </button>
          </footer>
        </article>

        <InfiniteListFooter
          :is-loading-more="isLoadingMore"
          :has-more="hasMore"
          :set-trigger="setLoadMoreTrigger"
          loading-text="正在加载更多订单..."
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
