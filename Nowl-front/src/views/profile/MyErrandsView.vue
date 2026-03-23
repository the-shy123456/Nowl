<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Package,
  Truck,
  Edit,
  Star,
  AlertTriangle,
  CheckCircle,
  Clock,
  ChevronRight,
  Plus,
} from 'lucide-vue-next'
import { getMyPublishedErrands, getMyAcceptedErrands } from '@/api/modules/errand'
import { hasReviewed } from '@/api/modules/review'
import type { ErrandTask } from '@/types'
import { ErrandStatusMap } from '@/constants/statusMaps'
import { DisputeStatus, PAGE_CONSTANTS, ErrandStatus, ReviewStatus } from '@/constants'
import dayjs from 'dayjs'
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

const router = useRouter()

// 评价状态缓存: taskId -> boolean
const reviewedMap = ref<Record<number, boolean>>({})

// 当前Tab
const activeTab = ref<'published' | 'accepted'>('published')

const pageSize = PAGE_CONSTANTS.LARGE_PAGE_SIZE
const publishedState = usePaginatedList<ErrandTask>({
  pageSize,
  fetchPage: ({ pageNum, pageSize }) => getMyPublishedErrands({ pageNum, pageSize }),
  onError: error => {
    console.error('加载我发布的跑腿失败:', error)
  },
})

const acceptedState = usePaginatedList<ErrandTask>({
  pageSize,
  fetchPage: ({ pageNum, pageSize }) => getMyAcceptedErrands({ pageNum, pageSize }),
  onError: error => {
    console.error('加载我接的跑腿失败:', error)
  },
})

const currentState = computed(() =>
  activeTab.value === 'published' ? publishedState : acceptedState,
)
const currentPage = computed({
  get: () => (activeTab.value === 'published' ? publishedState.pageNum.value : acceptedState.pageNum.value),
  set: value => {
    if (activeTab.value === 'published') {
      publishedState.pageNum.value = value
      return
    }
    acceptedState.pageNum.value = value
  },
})

const currentList = computed(() => currentState.value.list.value)
const showInitialLoading = computed(() => currentState.value.showInitialLoading.value)
const isLoadingMore = computed(() => currentState.value.isLoadingMore.value)
const hasMore = computed(() => currentState.value.hasMore.value)

const currentStats = computed(() => {
  const list = currentList.value
  return {
    total: list.length,
    pending: list.filter(item =>
      item.taskStatus === ErrandStatus.PENDING
      && !isReviewBlocked(item),
    ).length,
    progressing: list.filter(item => item.taskStatus === ErrandStatus.IN_PROGRESS).length,
    done: list.filter(item => item.taskStatus === ErrandStatus.COMPLETED).length,
  }
})

useListQuerySync([
  createQueryBinding({
    key: 'tab',
    state: activeTab,
    defaultValue: 'published',
    parse: raw => {
      const value = Array.isArray(raw) ? raw[0] : raw
      return value === 'accepted' ? 'accepted' : 'published'
    },
    serialize: value => (value === 'published' ? undefined : value),
  }),
  createQueryBinding({
    key: 'page',
    state: currentPage,
    defaultValue: 1,
    parse: raw => parsePositiveIntQuery(raw, 1),
    serialize: value => serializePageQuery(value, 1),
  }),
], {
  onQueryApplied: () => {
    void currentState.value.refresh(currentPage.value)
  },
})

const switchTab = (tab: 'published' | 'accepted') => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  currentPage.value = 1
  void currentState.value.refresh(1)
}

const goToDetail = (taskId: number) => {
  router.push(`/errand/${taskId}`)
}

const formatTime = (time?: string) => {
  if (!time) return '--'
  return dayjs(time).format('MM-DD HH:mm')
}

const getStatusInfo = (status: number) => {
  return ErrandStatusMap[status] || { text: '未知', color: 'bg-gray-100 text-gray-500' }
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

const getReviewStatus = (task: ErrandTask): number | null => {
  const status = Number(task.reviewStatus)
  if (!Number.isFinite(status)) return null
  return status
}

const isReviewBlocked = (task: ErrandTask) => {
  const reviewStatus = getReviewStatus(task)
  return reviewStatus === ReviewStatus.PENDING
    || reviewStatus === ReviewStatus.WAIT_MANUAL
    || reviewStatus === ReviewStatus.REJECTED
}

const isReviewPassed = (task: ErrandTask) => {
  const reviewStatus = getReviewStatus(task)
  if (reviewStatus === null) return true
  return reviewStatus === ReviewStatus.APPROVED || reviewStatus === ReviewStatus.MANUAL_PASSED
}

const getTaskStatusInfo = (task: ErrandTask) => {
  if (task.taskStatus === ErrandStatus.CANCELLED || task.taskStatus === ErrandStatus.COMPLETED) {
    return getStatusInfo(task.taskStatus)
  }
  const reviewStatus = getReviewStatus(task)
  if (reviewStatus === ReviewStatus.PENDING) {
    return { text: '待审核', color: 'bg-yellow-100 text-yellow-700' }
  }
  if (reviewStatus === ReviewStatus.WAIT_MANUAL) {
    return { text: '待人工复核', color: 'bg-orange-100 text-orange-700' }
  }
  if (reviewStatus === ReviewStatus.REJECTED) {
    return { text: '审核未通过', color: 'bg-red-100 text-red-700' }
  }
  return getStatusInfo(task.taskStatus)
}

const getTaskHintClass = (task: ErrandTask) => {
  if (!hasActiveDispute(task) && hasLatestClosedDispute(task)) {
    return 'bg-red-50 text-red-600'
  }
  if (task.taskStatus === ErrandStatus.CANCELLED) {
    return 'bg-red-50 text-red-600'
  }
  const reviewStatus = getReviewStatus(task)
  if (reviewStatus === ReviewStatus.PENDING) {
    return 'bg-yellow-50 text-yellow-700'
  }
  if (reviewStatus === ReviewStatus.WAIT_MANUAL) {
    return 'bg-orange-50 text-orange-700'
  }
  if (reviewStatus === ReviewStatus.REJECTED) {
    return 'bg-red-50 text-red-600'
  }
  if (task.taskStatus === ErrandStatus.PENDING_CONFIRM) {
    return 'bg-orange-50 text-orange-600'
  }
  if (task.taskStatus === ErrandStatus.IN_PROGRESS) {
    return 'bg-yellow-50 text-yellow-700'
  }
  return 'bg-red-50 text-red-600'
}

const reconnectInfiniteObserver = () => {
  currentState.value.loadMoreTrigger.value = null
  currentState.value.reconnectInfiniteObserver()
}

watch(
  activeTab,
  reconnectInfiniteObserver,
  { flush: 'post' },
)

const setLoadMoreTrigger = (element: HTMLElement | null) => {
  currentState.value.loadMoreTrigger.value = element
}

const checkReviewStatus = async (tasks: ErrandTask[]) => {
  const completedTasks = tasks.filter(t => t.taskStatus === ErrandStatus.COMPLETED)
  const unchecked = completedTasks.filter(t => !(t.taskId in reviewedMap.value))
  await Promise.all(
    unchecked.map(async (task) => {
      try {
        const result = await hasReviewed({ targetType: 1, taskId: task.taskId })
        reviewedMap.value[task.taskId] = !!result
      } catch {
        // ignore
      }
    }),
  )
}

const getReviewTargetId = (task: ErrandTask) => {
  if (activeTab.value === 'published') {
    return task.acceptorId || 0
  }
  return task.publisherId || 0
}

const hasValidReviewTarget = (task: ErrandTask) => {
  const reviewedId = getReviewTargetId(task)
  return Number.isFinite(reviewedId) && reviewedId > 0
}

watch(
  () => currentList.value,
  (list) => {
    if (list.length > 0) {
      void checkReviewStatus(list)
    }
  },
)

const quickHint = (task: ErrandTask) => {
  if (!hasActiveDispute(task) && hasLatestClosedDispute(task) && latestClosedDisputeText(task)) {
    return `纠纷：${latestClosedDisputeText(task)}`
  }
  if (task.taskStatus === ErrandStatus.CANCELLED) {
    return `任务已取消${task.cancelReason ? `：${task.cancelReason}` : ''}`
  }

  const reviewStatus = getReviewStatus(task)
  if (reviewStatus === ReviewStatus.PENDING) {
    return '任务已提交，审核通过后才会进入接单列表'
  }
  if (reviewStatus === ReviewStatus.WAIT_MANUAL) {
    return `任务待人工复核${task.auditReason ? `：${task.auditReason}` : ''}`
  }
  if (reviewStatus === ReviewStatus.REJECTED) {
    return `审核未通过${task.auditReason ? `：${task.auditReason}` : ''}`
  }
  if (task.taskStatus === ErrandStatus.PENDING_CONFIRM) {
    return activeTab.value === 'published' ? '跑腿员已送达，请尽快确认完成' : '已送达，等待发布者确认'
  }
  if (task.taskStatus === ErrandStatus.IN_PROGRESS && activeTab.value === 'accepted') {
    return '任务进行中，送达后请上传凭证'
  }
  return ''
}

const canEditTask = (task: ErrandTask) => {
  return activeTab.value === 'published'
    && task.taskStatus === ErrandStatus.PENDING
    && isReviewPassed(task)
}

const canCreateReview = (task: ErrandTask) => {
  return task.taskStatus === ErrandStatus.COMPLETED
    && hasValidReviewTarget(task)
    && reviewedMap.value[task.taskId] === false
    && !isReviewBlocked(task)
}

const getActiveDisputeId = (task: ErrandTask) => {
  const disputeId = Number(task.activeDisputeId)
  return Number.isFinite(disputeId) && disputeId > 0 ? disputeId : null
}

const getLatestClosedDisputeId = (task: ErrandTask) => {
  const disputeId = Number(task.latestClosedDisputeId)
  return Number.isFinite(disputeId) && disputeId > 0 ? disputeId : null
}

const hasActiveDispute = (task: ErrandTask) => {
  if (getActiveDisputeId(task) === null) return false
  const status = Number(task.activeDisputeStatus)
  if (!Number.isFinite(status)) return false
  return status === DisputeStatus.PENDING || status === DisputeStatus.PROCESSING
}

const hasLatestClosedDispute = (task: ErrandTask) => {
  if (getLatestClosedDisputeId(task) === null) return false
  const status = Number(task.latestClosedDisputeStatus)
  if (!Number.isFinite(status)) return false
  return status === DisputeStatus.RESOLVED || status === DisputeStatus.REJECTED
}

const latestClosedDisputeText = (task: ErrandTask) => {
  const parts: string[] = []
  const refundText = formatAmountText(task.latestClosedDisputeRefundAmount)
  if (refundText) {
    parts.push(`退款¥${refundText}`)
  }
  const creditPenalty = Number(task.latestClosedDisputeCreditPenalty)
  if (Number.isFinite(creditPenalty) && creditPenalty > 0) {
    parts.push(`扣除对方信用分${creditPenalty}分`)
  }
  const remark = extractHandleRemark(task.latestClosedDisputeResult)
  if (remark) {
    parts.push(`处理说明：${remark}`)
  }
  if (parts.length > 0) {
    return parts.join('，')
  }
  return String(task.latestClosedDisputeResult || '').trim()
}

const canRaiseDispute = (task: ErrandTask) => {
  return activeTab.value === 'published'
    && (task.taskStatus === ErrandStatus.IN_PROGRESS || task.taskStatus === ErrandStatus.PENDING_CONFIRM)
    && Number(task.acceptorId || 0) > 0
    && !hasActiveDispute(task)
}

const goToReview = (task: ErrandTask) => {
  if (!canCreateReview(task)) return
  const reviewedId = getReviewTargetId(task)
  router.push(`/review/create?type=1&id=${task.taskId}&reviewedId=${reviewedId}`)
}

const goToDispute = (task: ErrandTask) => {
  if (!canRaiseDispute(task)) return
  router.push(`/dispute/create?type=1&id=${task.taskId}`)
}

const openDisputeDetail = (task: ErrandTask) => {
  const disputeId = getActiveDisputeId(task) ?? getLatestClosedDisputeId(task)
  if (disputeId === null) {
    router.push('/dispute/list')
    return
  }
  router.push(`/dispute/${disputeId}`)
}

useAutoRefreshOnVisible({
  refresh: () => currentState.value.refresh(currentPage.value),
})

onMounted(() => {
  void Promise.all([
    publishedState.refresh(publishedState.pageNum.value),
    acceptedState.refresh(acceptedState.pageNum.value),
  ])
})
</script>

<template>
  <SubPageShell title="我的跑腿" subtitle="管理你发布与接单的任务" back-to="/profile" max-width="lg" :use-card="false">
    <div class="space-y-4">
      <section class="um-card p-4 md:p-5">
        <div class="flex items-center justify-between gap-3 flex-wrap">
          <div class="inline-flex rounded-2xl border border-slate-200 bg-slate-50 p-1">
            <button
              @click="switchTab('published')"
              class="px-5 py-2 rounded-xl text-sm font-semibold transition-colors inline-flex items-center gap-1"
              :class="activeTab === 'published' ? 'bg-white text-warm-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'"
            >
              <Package :size="14" />
              我发布的
            </button>
            <button
              @click="switchTab('accepted')"
              class="px-5 py-2 rounded-xl text-sm font-semibold transition-colors inline-flex items-center gap-1"
              :class="activeTab === 'accepted' ? 'bg-white text-warm-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'"
            >
              <Truck :size="14" />
              我接的单
            </button>
          </div>

          <div class="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
            <div class="rounded-xl bg-warm-50 border border-warm-100 px-3 py-2 text-warm-700">
              <div class="text-[11px] text-warm-500">总任务</div>
              <div class="text-sm font-bold">{{ currentStats.total }}</div>
            </div>
            <div class="rounded-xl bg-yellow-50 border border-yellow-100 px-3 py-2 text-yellow-700">
              <div class="text-[11px] text-yellow-500">待接单</div>
              <div class="text-sm font-bold">{{ currentStats.pending }}</div>
            </div>
            <div class="rounded-xl bg-blue-50 border border-blue-100 px-3 py-2 text-blue-700">
              <div class="text-[11px] text-blue-500">进行中</div>
              <div class="text-sm font-bold">{{ currentStats.progressing }}</div>
            </div>
            <div class="rounded-xl bg-emerald-50 border border-emerald-100 px-3 py-2 text-emerald-700">
              <div class="text-[11px] text-emerald-500">已完成</div>
              <div class="text-sm font-bold">{{ currentStats.done }}</div>
            </div>
          </div>
        </div>
      </section>

      <div v-if="showInitialLoading" class="space-y-3">
        <div v-for="i in 3" :key="i" class="animate-pulse um-card p-4">
          <div class="h-5 bg-slate-200 rounded w-3/4 mb-2"></div>
          <div class="h-4 bg-slate-200 rounded w-1/2"></div>
        </div>
      </div>

      <div v-else-if="currentList.length > 0" class="space-y-3">
        <article
          v-for="task in currentList"
          :key="task.taskId"
          class="um-card p-4"
        >
          <button class="w-full text-left" @click="goToDetail(task.taskId)">
            <div class="flex items-start justify-between gap-3">
              <h3 class="font-semibold text-slate-900 flex-1 line-clamp-2 leading-6">{{ task.title }}</h3>
              <span :class="['px-2.5 py-1 rounded-full text-xs font-medium whitespace-nowrap', getTaskStatusInfo(task).color]">
                {{ getTaskStatusInfo(task).text }}
              </span>
            </div>

            <p class="text-sm text-slate-600 mt-2 line-clamp-1">
              {{ task.pickupAddress || '取件地待补充' }} → {{ task.deliveryAddress || '送达地待补充' }}
            </p>

            <div class="mt-3 flex items-center justify-between">
              <span class="text-lg font-bold text-warm-600">¥{{ task.reward }}</span>
              <span class="text-xs text-slate-400 inline-flex items-center gap-1">
                <Clock :size="12" />
                {{ formatTime(task.createTime) }}
              </span>
            </div>

            <p
              v-if="quickHint(task)"
              class="mt-2 text-xs px-2.5 py-1 rounded-lg inline-block"
              :class="getTaskHintClass(task)"
            >
              {{ quickHint(task) }}
            </p>
          </button>

          <div class="mt-3 pt-3 border-t border-slate-100 flex items-center gap-2 flex-wrap">
            <button
              v-if="canEditTask(task)"
              @click="router.push(`/errand/edit/${task.taskId}`)"
              class="um-btn px-3 py-1.5 text-xs bg-warm-50 text-warm-600 hover:bg-warm-100 inline-flex items-center gap-1"
            >
              <Edit :size="12" />
              编辑任务
            </button>

            <button
              v-if="canCreateReview(task)"
              @click="goToReview(task)"
              class="um-btn px-3 py-1.5 text-xs bg-warm-50 text-warm-600 hover:bg-warm-100 inline-flex items-center gap-1"
            >
              <Star :size="12" />
              去评价
            </button>

            <span
              v-if="task.taskStatus === ErrandStatus.COMPLETED && reviewedMap[task.taskId] === true"
              class="um-btn px-3 py-1.5 text-xs bg-green-50 text-green-600 inline-flex items-center gap-1"
            >
              <CheckCircle :size="12" />
              已评价
            </span>

            <button
              v-if="canRaiseDispute(task)"
              @click="goToDispute(task)"
              class="um-btn px-3 py-1.5 text-xs bg-orange-50 text-orange-700 hover:bg-orange-100 inline-flex items-center gap-1"
            >
              <AlertTriangle :size="12" />
              发起纠纷
            </button>

            <button
              v-if="hasActiveDispute(task) || hasLatestClosedDispute(task)"
              @click="openDisputeDetail(task)"
              class="um-btn px-3 py-1.5 text-xs inline-flex items-center gap-1"
              :class="hasActiveDispute(task)
                ? 'bg-orange-100 text-orange-700 hover:bg-orange-200'
                : 'bg-red-50 text-red-600 hover:bg-red-100'"
            >
              <AlertTriangle :size="12" />
              查看纠纷
            </button>

            <button
              class="ml-auto um-btn px-3 py-1.5 text-xs bg-slate-100 text-slate-600 hover:bg-slate-200 inline-flex items-center gap-1"
              @click="goToDetail(task.taskId)"
            >
              查看详情
              <ChevronRight :size="12" />
            </button>
          </div>
        </article>

        <InfiniteListFooter
          :is-loading-more="isLoadingMore"
          :has-more="hasMore"
          :set-trigger="setLoadMoreTrigger"
          loading-text="正在加载更多任务..."
        />
      </div>

      <div v-if="!showInitialLoading && currentList.length === 0" class="um-card py-16 flex flex-col items-center justify-center">
        <div class="w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mb-3">
          <component :is="activeTab === 'published' ? Package : Truck" class="w-10 h-10 text-slate-300" />
        </div>
        <p class="text-slate-500 text-sm">
          {{ activeTab === 'published' ? '暂无发布的跑腿任务' : '暂无接单记录' }}
        </p>
        <button
          v-if="activeTab === 'published'"
          @click="router.push('/errand/publish')"
          class="mt-4 um-btn um-btn-primary px-4 py-2 text-sm inline-flex items-center gap-1"
        >
          <Plus :size="14" />
          去发布任务
        </button>
        <button
          v-else
          @click="router.push('/errands')"
          class="mt-4 um-btn um-btn-primary px-4 py-2 text-sm"
        >
          去接单
        </button>
      </div>
    </div>
  </SubPageShell>
</template>

<style scoped>
.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
