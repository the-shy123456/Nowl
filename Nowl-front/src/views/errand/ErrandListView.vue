<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, MapPin, Truck, Library, RotateCw, User, Search } from 'lucide-vue-next'
import { acceptErrand } from '@/api/modules/errand'
import { searchErrands, type ErrandSearchResultVO } from '@/api/modules/search'
import { ElMessage, ElMessageBox } from '@/utils/feedback'
import { useUserStore } from '@/stores/user'
import { ErrandStatusMap } from '@/constants/statusMaps'
import { PAGE_CONSTANTS, AuthStatus, RunnableStatus, ErrandStatus } from '@/constants'
import { usePaginatedList } from '@/composables/usePaginatedList'
import {
  createQueryBinding,
  parseEnumQueryNumber,
  parsePositiveIntQuery,
  serializePageQuery,
  useListQuerySync,
} from '@/composables/useListQuerySync'
import { useAutoRefreshOnVisible } from '@/composables/useAutoRefreshOnVisible'
import InfiniteListFooter from '@/components/InfiniteListFooter.vue'

const router = useRouter()
const userStore = useUserStore()
const pageSize = PAGE_CONSTANTS.LARGE_PAGE_SIZE
const searchKeyword = ref('')
const sortType = ref(0) // 0-综合 1-最新 2-赏金最低 3-赏金最高
const listAnchor = ref<HTMLElement | null>(null)
const refreshButtonSpinning = ref(false)

const errandQueryParams = computed(() => ({
  keyword: searchKeyword.value.trim() || undefined,
  // 公开跑腿页仅展示可接单任务，避免已完成/待确认任务在列表中残留
  taskStatus: ErrandStatus.PENDING,
  sortType: sortType.value,
  schoolCode: userStore.currentCampus?.schoolCode,
  campusCode: userStore.currentCampus?.campusCode || undefined,
}))

const sortOptions = [
  { value: 0, label: '综合' },
  { value: 1, label: '最新' },
  { value: 2, label: '赏金最低' },
  { value: 3, label: '赏金最高' },
]

const selectSort = (value: number) => {
  sortType.value = value
  pageNum.value = 1
  refresh()
}

// 搜索：回车或点击搜索按钮触发
const handleSearch = () => {
  pageNum.value = 1
  refresh()
}

const ensureRunnerApproved = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return false
  }

  if (userStore.userInfo?.authStatus !== AuthStatus.APPROVED) {
    ElMessage.warning('请先完成实名认证')
    router.push('/profile/auth')
    return false
  }

  const status = userStore.userInfo?.runnableStatus ?? RunnableStatus.NOT_APPLIED
  if (status === RunnableStatus.APPROVED) {
    return true
  }

  if (status === RunnableStatus.PENDING) {
    ElMessage.warning('跑腿员申请审核中，请耐心等待')
    return false
  }

  if (status === RunnableStatus.REJECTED) {
    ElMessage.warning('申请未通过，可重新提交')
    router.push('/profile/runner-apply')
    return false
  }

  ElMessage.warning('请先申请成为跑腿员')
  router.push('/profile/runner-apply')
  return false
}

const displayCount = computed(() => {
  return total.value
})

const runnerStatus = computed(() => userStore.userInfo?.runnableStatus ?? RunnableStatus.NOT_APPLIED)

const runnerEntryLabel = computed(() => {
  if (!userStore.isLoggedIn) return '跑腿注册'
  if (userStore.userInfo?.authStatus !== AuthStatus.APPROVED) return '先完成认证'
  switch (runnerStatus.value) {
    case RunnableStatus.APPROVED:
      return '已认证跑腿员'
    case RunnableStatus.PENDING:
      return '跑腿审核中'
    case RunnableStatus.REJECTED:
      return '重新申请'
    default:
      return '跑腿注册'
  }
})

const {
  list: errands,
  pageNum,
  total,
  hasMore,
  showInitialLoading,
  isLoadingMore,
  loadMoreTrigger,
  refresh,
} = usePaginatedList<ErrandSearchResultVO>({
  pageSize,
  fetchPage: ({ pageNum, pageSize }) =>
    searchErrands({
      ...errandQueryParams.value,
      pageNum,
      pageSize,
    }),
  onError: error => {
    console.error('获取跑腿任务失败', error)
  },
})

useListQuerySync([
  createQueryBinding({
    key: 'q',
    state: searchKeyword,
    defaultValue: '',
    serialize: value => {
      const normalized = value.trim()
      return normalized || undefined
    },
  }),
  createQueryBinding({
    key: 'sort',
    state: sortType,
    defaultValue: 0,
    parse: raw => parseEnumQueryNumber(raw, [0, 1, 2, 3], 0),
    serialize: value => (value === 0 ? undefined : String(value)),
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

// 安全渲染搜索高亮：仅保留 <em> 标签，避免把 HTML 字面量直接展示给用户
const renderHighlightTitle = (title: string) => {
  if (!title) return ''
  const escaped = title
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  return escaped
    .replace(/&lt;em&gt;/g, '<em class="text-warm-500 not-italic font-semibold">')
    .replace(/&lt;\/em&gt;/g, '</em>')
}

const getPlainTitle = (title: string) => {
  if (!title) return ''
  return title.replace(/<\/?em>/gi, '')
}

useAutoRefreshOnVisible({
  refresh: () => refresh(pageNum.value),
})

watch(
  () => userStore.currentCampus,
  () => {
    pageNum.value = 1
    refresh()
  },
)

// 跳转到详情页
const goToDetail = (taskId: number) => {
  router.push(`/errand/${taskId}`)
}

const isOwnTask = (task: ErrandSearchResultVO) =>
  Number(task.publisherId) > 0 && task.publisherId === userStore.userInfo?.userId

const canAcceptTask = (task: ErrandSearchResultVO) =>
  task.taskStatus === 0 && !isOwnTask(task)

const handleAccept = async (task: ErrandSearchResultVO, event: Event) => {
  event.stopPropagation() // 阻止冒泡，避免跳转详情
  if (isOwnTask(task)) {
    ElMessage.info('这是你发布的任务，不能自己接单')
    return
  }
  if (!canAcceptTask(task)) return

  if (!ensureRunnerApproved()) return

  try {
    await ElMessageBox.confirm('确定要接下这个跑腿任务吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })

    await acceptErrand(task.taskId)
    ElMessage.success('接单成功！请尽快完成任务')
    // 跳转到详情页
    router.push(`/errand/${task.taskId}`)
  } catch (error: unknown) {
    if (error !== 'cancel') {
      const requestError = error as { response?: { data?: { message?: string } } }
      ElMessage.error(requestError.response?.data?.message || '接单失败')
    }
  }
}

// 获取状态信息
const getStatusInfo = (status: number) => {
  return ErrandStatusMap[status] || { text: '未知', color: 'bg-gray-100 text-gray-500' }
}

const handleRunnerEntry = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (userStore.userInfo?.authStatus !== AuthStatus.APPROVED) {
    ElMessage.warning('请先完成实名认证')
    router.push('/profile/auth')
    return
  }
  if (runnerStatus.value === RunnableStatus.APPROVED) {
    ElMessage.info('你已通过跑腿员审核，可直接在任务列表点击“立即接单”')
    router.push('/profile/runner-apply')
    return
  }
  if (runnerStatus.value === RunnableStatus.PENDING) {
    ElMessage.info('跑腿员申请审核中，可在申请页查看状态')
    router.push('/profile/runner-apply')
    return
  }
  if (runnerStatus.value === RunnableStatus.REJECTED) {
    ElMessage.warning('申请未通过，可在申请页重新提交')
    router.push('/profile/runner-apply')
    return
  }
  router.push('/profile/runner-apply')
}

const handleRefresh = () => {
  refreshButtonSpinning.value = true
  const startedAt = Date.now()
  void refresh().finally(() => {
    const elapsed = Date.now() - startedAt
    const remain = Math.max(0, 400 - elapsed)
    window.setTimeout(() => {
      refreshButtonSpinning.value = false
    }, remain)
  })
}

onMounted(() => {
  refresh(pageNum.value)
})
</script>

<template>
  <div class="max-w-6xl mx-auto pb-20 pt-6 px-4 space-y-6 animate-in fade-in duration-500">
    <!-- Unified Header & Search Card -->
    <div class="um-card p-6 space-y-5">
      <!-- Header Section -->
      <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-5">
        <div>
          <div class="flex items-center gap-2 mb-2">
            <span class="um-badge">校园互助</span>
            <span class="um-badge bg-emerald-50 text-emerald-600 border-emerald-100">安全交易</span>
          </div>
          <h1 class="text-2xl font-black text-slate-800 tracking-tight font-display">校园跑腿</h1>
          <p class="text-sm text-um-muted font-medium mt-1">代取、代送、代办更高效，接单赚点零花钱</p>
        </div>
        <div class="flex flex-wrap items-center gap-3">
          <button
            @click="router.push('/profile/my-errands')"
            class="flex items-center gap-2 px-4 py-2 rounded-full text-sm text-slate-600 bg-white border border-warm-100 hover:bg-warm-50 transition-colors"
          >
            <User :size="16" />
            我的跑腿
          </button>
          <button
            @click="router.push('/errand/publish')"
            class="flex items-center gap-2 um-btn um-btn-primary px-5 py-2.5 text-sm active:scale-95"
          >
            <Plus :size="16" />
            发布跑腿
          </button>
          <button
            @click="handleRunnerEntry"
            class="flex items-center gap-2 px-5 py-2.5 rounded-full text-sm font-medium border border-warm-200 text-warm-600 bg-white hover:bg-warm-50 transition-colors"
          >
            <Truck :size="16" />
            {{ runnerEntryLabel }}
          </button>
        </div>
      </div>

      <!-- Divider -->
      <div class="border-t border-warm-100"></div>

      <!-- Search & Filter Section -->
      <div class="space-y-3">
        <div
          class="flex items-center gap-3 bg-white border border-warm-100 rounded-2xl px-4 py-2"
        >
          <Search :size="16" class="text-um-muted" />
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索任务关键词，支持中文和拼音..."
            class="flex-1 bg-transparent outline-none text-sm text-um-text placeholder:text-um-muted"
            @keyup.enter="handleSearch"
          />
          <button
            @click="handleSearch"
            class="px-4 py-1.5 rounded-full text-xs font-semibold bg-gradient-to-r from-warm-500 to-warm-400 text-white hover:shadow-md transition-all"
          >
            搜索
          </button>
        </div>

        <!-- Sort Options -->
        <div class="flex items-center gap-2 pt-2 border-t border-warm-50">
          <span class="text-xs font-bold text-um-muted whitespace-nowrap">排序：</span>
          <div class="flex gap-2 overflow-x-auto py-1 no-scrollbar">
            <button
              v-for="option in sortOptions"
              :key="option.value"
              @click="selectSort(option.value)"
              class="whitespace-nowrap px-3 py-1.5 rounded-full text-xs font-semibold transition-all"
              :class="
                sortType === option.value
                  ? 'bg-warm-100 text-warm-700 shadow-sm'
                  : 'bg-white text-um-muted hover:bg-warm-50 border border-slate-100'
              "
            >
              {{ option.label }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 统计 & 刷新 -->
    <div class="flex items-center justify-between">
      <div class="text-sm text-um-muted">
        当前任务总数 <span class="text-slate-800 font-semibold">{{ displayCount }}</span> 个
      </div>
      <button
        @click="handleRefresh"
        :disabled="refreshButtonSpinning"
        class="flex items-center gap-2 text-sm text-slate-500 hover:text-warm-500 transition-colors disabled:opacity-60"
      >
        <RotateCw :size="14" :class="refreshButtonSpinning ? 'animate-spin' : ''" />
        {{ refreshButtonSpinning ? '刷新中...' : '刷新' }}
      </button>
    </div>

    <!-- 任务列表 -->
    <div v-if="showInitialLoading" class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <div v-for="i in 4" :key="i" class="h-48 bg-slate-100 animate-pulse rounded-2xl"></div>
    </div>

    <div ref="listAnchor" v-else-if="errands.length > 0" class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <div
        v-for="task in errands"
        :key="task.taskId"
        @click="goToDetail(task.taskId)"
        class="um-card p-6 hover:-translate-y-1 transition-all duration-300 flex flex-col justify-between h-full group cursor-pointer"
      >
        <!-- Card Header -->
        <div class="flex justify-between items-start mb-4">
          <div class="flex gap-4">
            <!-- Icon Box -->
            <div class="w-12 h-12 rounded-2xl bg-warm-50 text-warm-500 flex items-center justify-center flex-shrink-0 group-hover:bg-warm-500 group-hover:text-white transition-colors duration-300">
              <Truck v-if="getPlainTitle(task.title).includes('快递')" :size="24" />
              <Library v-else-if="getPlainTitle(task.title).includes('图书馆')" :size="24" />
              <MapPin v-else :size="24" />
            </div>

            <!-- Title & Address -->
            <div>
              <h3 class="font-bold text-slate-800 text-lg leading-tight mb-1" v-html="renderHighlightTitle(task.title)"></h3>
              <div class="flex items-center gap-1 text-xs text-slate-400">
                <MapPin :size="12" />
                <span>{{ task.pickupAddress || '校内' }}</span>
              </div>
            </div>
          </div>

          <!-- Price & Status -->
          <div class="text-right">
            <div class="text-warm-600 font-black text-xl">¥{{ task.reward }}</div>
            <span :class="['px-2 py-0.5 rounded-full text-[10px] font-medium mt-1 inline-block', getStatusInfo(task.taskStatus).color]">
              {{ task.statusText || getStatusInfo(task.taskStatus).text }}
            </span>
          </div>
        </div>

        <!-- Description -->
        <div class="bg-warm-50/50 rounded-xl p-3 mb-4">
          <p class="text-sm text-slate-600 line-clamp-2 leading-relaxed">
            {{ task.description || task.taskContent || '暂无描述' }}
          </p>
        </div>

        <!-- 地址信息 -->
        <div class="text-xs text-slate-500 mb-4 flex items-center gap-2">
          <span class="bg-green-50 text-green-600 px-2 py-1 rounded">取</span>
          <span class="flex-1 truncate">{{ task.pickupAddress || '-' }}</span>
          <span>→</span>
          <span class="bg-red-50 text-red-600 px-2 py-1 rounded">送</span>
          <span class="flex-1 truncate">{{ task.deliveryAddress || '-' }}</span>
        </div>

        <!-- Button -->
        <button
          v-if="canAcceptTask(task)"
          @click="handleAccept(task, $event)"
          class="w-full um-btn um-btn-primary py-3 active:scale-[0.98]"
        >
          立即接单
        </button>
        <div v-else class="w-full text-center text-gray-400 py-3 bg-gray-100 rounded-xl">
          {{ isOwnTask(task) && task.taskStatus === 0 ? '我发布的任务' : getStatusInfo(task.taskStatus).text }}
        </div>
      </div>
    </div>

    <div v-if="errands.length > 0">
      <InfiniteListFooter
        :is-loading-more="isLoadingMore"
        :has-more="hasMore"
        :set-trigger="setLoadMoreTrigger"
        loading-text="正在加载更多任务..."
        loading-text-class="text-um-muted"
        end-text-class="text-um-muted"
      />
    </div>

    <!-- Empty State -->
    <div v-if="!showInitialLoading && errands.length === 0" class="text-center py-20">
      <div class="w-24 h-24 bg-warm-50 rounded-full flex items-center justify-center mx-auto mb-4">
        <Truck class="w-12 h-12 text-warm-200" />
      </div>
      <p class="text-slate-400 mb-2">暂无匹配的任务</p>
      <p class="text-xs text-um-muted">可以换个关键词或筛选条件试试</p>
    </div>
  </div>
</template>
