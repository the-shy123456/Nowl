<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { AlertTriangle, Clock, CheckCircle, XCircle, Undo2, ShieldAlert, ChevronRight } from 'lucide-vue-next'
import { getDisputeList, type DisputeListItem } from '@/api/modules/dispute'
import { ElMessage } from '@/utils/feedback'
import { DisputeStatusMap } from '@/constants/statusMaps'
import SubPageShell from '@/components/SubPageShell.vue'
import InfiniteListFooter from '@/components/InfiniteListFooter.vue'
import { PAGE_CONSTANTS } from '@/constants'
import { usePaginatedList } from '@/composables/usePaginatedList'
import {
  createQueryBinding,
  parseOptionalEnumQueryNumber,
  parsePositiveIntQuery,
  serializePageQuery,
  useListQuerySync,
} from '@/composables/useListQuerySync'

const router = useRouter()

const activeStatus = ref<number | undefined>(undefined)
const DISPUTE_STATUS_VALUES = [0, 1, 2, 3, 4]

const statusOptions: Array<{ value: number | undefined; label: string }> = [
  { value: undefined, label: '全部' },
  { value: 0, label: '待处理' },
  { value: 1, label: '处理中' },
  { value: 2, label: '已解决' },
  { value: 3, label: '已驳回' },
  { value: 4, label: '已撤回' },
]

const {
  list: disputes,
  pageNum,
  hasMore,
  showInitialLoading,
  isLoadingMore,
  loadMoreTrigger,
  refresh,
} = usePaginatedList<DisputeListItem>({
  pageSize: PAGE_CONSTANTS.LARGE_PAGE_SIZE,
  fetchPage: ({ pageNum, pageSize }) =>
    getDisputeList({
      pageNum,
      pageSize,
      handleStatus: activeStatus.value,
    }),
  onError: () => {
    ElMessage.error('获取纠纷列表失败')
  },
})

useListQuerySync([
  createQueryBinding({
    key: 'status',
    state: activeStatus,
    defaultValue: undefined,
    parse: raw => parseOptionalEnumQueryNumber(raw, DISPUTE_STATUS_VALUES),
    serialize: value => (value === undefined ? undefined : String(value)),
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
    void refresh(pageNum.value)
  },
})

const setLoadMoreTrigger = (element: HTMLElement | null) => {
  loadMoreTrigger.value = element
}

const switchStatus = (status: number | undefined) => {
  if (activeStatus.value === status) return
  activeStatus.value = status
  pageNum.value = 1
}

const viewDetail = (recordId: number) => {
  router.push(`/dispute/${recordId}`)
}

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

const getStatusIcon = (status: number) => {
  switch (status) {
    case 0:
      return Clock
    case 1:
      return AlertTriangle
    case 2:
      return CheckCircle
    case 3:
      return XCircle
    case 4:
      return Undo2
    default:
      return Clock
  }
}

const stats = computed(() => {
  const list = disputes.value
  return {
    total: list.length,
    pending: list.filter(item => item.handleStatus === 0).length,
    processing: list.filter(item => item.handleStatus === 1).length,
    done: list.filter(item => item.handleStatus === 2).length,
  }
})

const typeLabel = (item: DisputeListItem) => item.targetTypeDesc || (item.targetType === 0 ? '商品交易' : '跑腿服务')

onMounted(() => {
  void refresh(pageNum.value)
})
</script>

<template>
  <SubPageShell title="我的纠纷" subtitle="记录、处理进度与结果一览" back-to="/profile" max-width="lg" :use-card="false">
    <template #icon>
      <AlertTriangle class="text-white w-8 h-8" stroke-width="2.5" />
    </template>

    <div class="space-y-4">
      <section class="um-card p-4 md:p-5">
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
          <div class="rounded-xl bg-warm-50 border border-warm-100 px-3 py-2 text-warm-700">
            <div class="text-[11px] text-warm-500">当前列表</div>
            <div class="text-sm font-bold">{{ stats.total }}</div>
          </div>
          <div class="rounded-xl bg-yellow-50 border border-yellow-100 px-3 py-2 text-yellow-700">
            <div class="text-[11px] text-yellow-500">待处理</div>
            <div class="text-sm font-bold">{{ stats.pending }}</div>
          </div>
          <div class="rounded-xl bg-orange-50 border border-orange-100 px-3 py-2 text-orange-700">
            <div class="text-[11px] text-orange-500">处理中</div>
            <div class="text-sm font-bold">{{ stats.processing }}</div>
          </div>
          <div class="rounded-xl bg-emerald-50 border border-emerald-100 px-3 py-2 text-emerald-700">
            <div class="text-[11px] text-emerald-500">已解决</div>
            <div class="text-sm font-bold">{{ stats.done }}</div>
          </div>
        </div>

        <div class="mt-3 flex overflow-x-auto gap-2 scrollbar-hide">
          <button
            v-for="option in statusOptions"
            :key="option.value ?? 'all'"
            @click="switchStatus(option.value)"
            class="px-4 py-1.5 rounded-full text-sm whitespace-nowrap transition-colors"
            :class="activeStatus === option.value
              ? 'bg-warm-500 text-white'
              : 'bg-white border border-warm-200 text-warm-700 hover:bg-warm-50'"
          >
            {{ option.label }}
          </button>
        </div>
      </section>

      <div v-if="showInitialLoading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-warm-500"></div>
      </div>

      <div v-else-if="disputes.length === 0" class="um-card py-16 text-center text-slate-400">
        <ShieldAlert class="w-14 h-14 mx-auto text-slate-300 mb-3" />
        <p>暂无纠纷记录</p>
      </div>

      <div v-else class="space-y-3">
        <article
          v-for="dispute in disputes"
          :key="dispute.recordId"
          @click="viewDetail(dispute.recordId)"
          class="um-card p-4 cursor-pointer hover:border-warm-200 transition-colors"
        >
          <header class="flex items-center justify-between gap-2 mb-3">
            <span
              class="px-2.5 py-1 rounded-full text-xs font-medium inline-flex items-center gap-1"
              :class="DisputeStatusMap[dispute.handleStatus]?.color || 'bg-warm-100 text-warm-700'"
            >
              <component :is="getStatusIcon(dispute.handleStatus)" class="w-3 h-3" />
              {{ dispute.statusDesc }}
            </span>
            <span class="text-xs text-slate-400">{{ typeLabel(dispute) }}</span>
          </header>

          <div class="flex gap-3">
            <div class="w-16 h-16 rounded-xl overflow-hidden bg-warm-50 border border-warm-100 flex-shrink-0">
              <img
                v-if="dispute.contentImage"
                :src="dispute.contentImage"
                :alt="dispute.contentTitle"
                class="w-full h-full object-cover"
              />
              <div v-else class="w-full h-full flex items-center justify-center">
                <AlertTriangle class="w-5 h-5 text-slate-300" />
              </div>
            </div>

            <div class="flex-1 min-w-0">
              <h3 class="font-semibold text-slate-800 line-clamp-1">{{ dispute.contentTitle || '未知内容' }}</h3>
              <p class="text-sm text-slate-500 line-clamp-2 mt-1 leading-6">{{ dispute.contentSummary }}</p>
            </div>
          </div>

          <footer class="flex items-center justify-between mt-3 pt-3 border-t border-warm-100">
            <div class="flex items-center gap-2 min-w-0">
              <img
                v-if="dispute.otherUserAvatar"
                :src="dispute.otherUserAvatar"
                class="w-5 h-5 rounded-full object-cover"
                alt="对方头像"
              />
              <span class="text-xs text-slate-500 truncate">
                {{ dispute.isInitiator ? '投诉对象' : '投诉方' }}：{{ dispute.otherUserName || '未知用户' }}
              </span>
            </div>
            <div class="inline-flex items-center gap-2 shrink-0">
              <span class="text-xs text-slate-400">{{ formatTime(dispute.createTime) }}</span>
              <ChevronRight :size="14" class="text-slate-300" />
            </div>
          </footer>
        </article>

        <InfiniteListFooter
          :is-loading-more="isLoadingMore"
          :has-more="hasMore"
          :set-trigger="setLoadMoreTrigger"
          loading-text="正在加载更多纠纷..."
        />
      </div>
    </div>
  </SubPageShell>
</template>

<style scoped>
.scrollbar-hide::-webkit-scrollbar {
  display: none;
}

.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

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
