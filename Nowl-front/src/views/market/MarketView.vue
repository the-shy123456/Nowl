<script setup lang="ts">
import { ref, onMounted, computed, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ShieldCheck, CheckCircle2, Heart, Search, X, Clock, TrendingUp } from 'lucide-vue-next'
import { getCategoryTree, collectGoods, uncollectGoods } from '@/api/modules/goods'
import {
  getHotWords,
  getSearchHistory,
  getSearchSuggestions,
  clearSearchHistory,
  searchGoods,
} from '@/api/modules/search'
import { getHotRecommend } from '@/api/modules/recommend'
import { useUserStore } from '@/stores/user'
import { ElMessage } from '@/utils/feedback'
import type { ItemCategory, SearchResultVO } from '@/types'
import LazyImage from '@/components/LazyImage.vue'
import InfiniteListFooter from '@/components/InfiniteListFooter.vue'
import { SEARCH } from '@/config/constants'
import { PAGE_CONSTANTS } from '@/constants'
import { usePaginatedList } from '@/composables/usePaginatedList'
import {
  createQueryBinding,
  parseEnumQueryNumber,
  parseOptionalPositiveIntQuery,
  parsePositiveIntQuery,
  serializeOptionalPositiveIntQuery,
  serializePageQuery,
  useListQuerySync,
} from '@/composables/useListQuerySync'
import { useAutoRefreshOnVisible } from '@/composables/useAutoRefreshOnVisible'

const router = useRouter()
const userStore = useUserStore()

// 分类数据
const categoryTree = ref<ItemCategory[]>([])
const selectedFirstCategoryId = ref<number | undefined>(undefined)
const selectedSecondCategoryId = ref<number | undefined>(undefined)
const firstLevelCategories = computed(() => categoryTree.value)
const secondLevelCategories = computed(() => {
  const parent = categoryTree.value.find(cat => cat.categoryId === selectedFirstCategoryId.value)
  return parent?.children || []
})

// 商品数据
const pageSize = ref(PAGE_CONSTANTS.LARGE_PAGE_SIZE)

// 搜索相关
const searchKeyword = ref('')
const searchInputFocused = ref(false)
const showSearchPanel = ref(false)
const hotWords = ref<string[]>([])
const searchHistory = ref<string[]>([])
const suggestions = ref<string[]>([])
const isSearchMode = ref(false) // 是否处于搜索模式

// 排序相关
const sortType = ref(0) // 0-综合 1-最新 2-价格升序 3-价格降序 4-热度
const showSortDropdown = ref(false)
const sortOptions = [
  { value: 0, label: '综合排序' },
  { value: 1, label: '最新发布' },
  { value: 2, label: '价格最低' },
  { value: 3, label: '价格最高' },
  { value: 4, label: '最受欢迎' },
]

// 搜索建议防抖
let suggestionTimer: ReturnType<typeof setTimeout> | null = null

const highlightParts = (text: string, keyword: string) => {
  if (!keyword) return [{ text, highlight: false }]
  const escaped = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const parts = text.split(new RegExp(`(${escaped})`, 'gi'))
  return parts
    .filter(Boolean)
    .map(part => ({ text: part, highlight: part.toLowerCase() === keyword.toLowerCase() }))
}

// 安全渲染高亮标题：只保留 <em> 标签，其他 HTML 标签转义
const renderHighlightTitle = (title: string) => {
  if (!title) return ''
  // 先转义所有 HTML，再把 &lt;em&gt; 和 &lt;/em&gt; 还原
  const escaped = title
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  // 还原 <em> 标签
  return escaped
    .replace(/&lt;em&gt;/g, '<em class="text-warm-500 not-italic font-medium">')
    .replace(/&lt;\/em&gt;/g, '</em>')
}

// 查询参数
const goodsQueryParams = computed(() => {
  const params: Record<string, unknown> = {
    keyword: searchKeyword.value || undefined,
    tradeStatus: 0,
    schoolCode: userStore.currentCampus?.schoolCode,
    campusCode: userStore.currentCampus?.campusCode,
    sortType: sortType.value,
  }

  if (selectedSecondCategoryId.value) {
    params.categoryId = selectedSecondCategoryId.value
  } else if (selectedFirstCategoryId.value) {
    params.parentCategoryId = selectedFirstCategoryId.value
  }

  return params
})

const normalizeGoodsCollectionState = (items: SearchResultVO[]) => {
  items.forEach(item => {
    item.isCollected = Boolean(item.isCollected)
    item.collectCount = Math.max(0, Number(item.collectCount || 0))
  })
}

const {
  list: goodsList,
  pageNum,
  total,
  hasMore,
  showInitialLoading,
  isLoadingMore,
  loadMoreTrigger,
  refresh,
} = usePaginatedList<SearchResultVO>({
  pageSize,
  fetchPage: ({ pageNum, pageSize }) => {
    return searchGoods({ ...goodsQueryParams.value, pageNum, pageSize })
  },
  onSuccess: list => {
    normalizeGoodsCollectionState(list)
  },
  onError: error => {
    const requestError = error as { name?: string; code?: string }
    if (requestError.name !== 'CanceledError' && requestError.code !== 'ERR_CANCELED') {
      ElMessage.error('获取商品列表失败')
    }
  },
})

useListQuerySync([
  createQueryBinding({
    key: 'q',
    state: searchKeyword,
    defaultValue: '',
    serialize: value => {
      if (!isSearchMode.value) return undefined
      const normalized = value.trim()
      return normalized || undefined
    },
  }),
  createQueryBinding({
    key: 'sort',
    state: sortType,
    defaultValue: 0,
    parse: raw => parseEnumQueryNumber(raw, [0, 1, 2, 3, 4], 0),
    serialize: value => (value === 0 ? undefined : String(value)),
  }),
  createQueryBinding({
    key: 'c1',
    state: selectedFirstCategoryId,
    defaultValue: undefined,
    parse: raw => parseOptionalPositiveIntQuery(raw),
    serialize: value => serializeOptionalPositiveIntQuery(value),
  }),
  createQueryBinding({
    key: 'c2',
    state: selectedSecondCategoryId,
    defaultValue: undefined,
    parse: raw => {
      if (!selectedFirstCategoryId.value) return undefined
      return parseOptionalPositiveIntQuery(raw)
    },
    serialize: value => serializeOptionalPositiveIntQuery(value),
  }),
  createQueryBinding({
    key: 'page',
    state: pageNum,
    defaultValue: 1,
    parse: raw => parsePositiveIntQuery(raw, 1),
    serialize: value => serializePageQuery(value, 1),
  }),
], {
  onQueryApplied: changedKeys => {
    if (changedKeys.includes('c1') && !selectedFirstCategoryId.value) {
      selectedSecondCategoryId.value = undefined
    }
    isSearchMode.value = !!searchKeyword.value
      || !!selectedFirstCategoryId.value
      || !!selectedSecondCategoryId.value
      || sortType.value !== 0
    refresh(pageNum.value)
  },
})

const collectPendingGoodsIds = ref<Set<number>>(new Set())

const setLoadMoreTrigger = (element: HTMLElement | null) => {
  loadMoreTrigger.value = element
}

useAutoRefreshOnVisible({
  refresh: () => refresh(pageNum.value),
  initialDelayMs: 1200,
})

// 监听校区变化
watch(
  () => userStore.currentCampus,
  () => {
    refresh()
    fetchHotWords()
  }
)

// 监听搜索输入，获取建议
watch(searchKeyword, (val) => {
  if (suggestionTimer) {
    clearTimeout(suggestionTimer)
  }
  if (val && val.length >= SEARCH.SUGGESTION_MIN_LENGTH) {
    suggestionTimer = setTimeout(() => {
      fetchSuggestions(val)
    }, SEARCH.DEBOUNCE_DELAY)
  } else {
    suggestions.value = []
  }
})

// 获取分类列表
const fetchCategories = async () => {
  try {
    const res = await getCategoryTree()
    categoryTree.value = res || []
  } catch {
    console.error('获取分类失败')
  }
}

// 获取热搜词
const extractHotWordsFromRecommend = (titles: Array<{ title?: string }>, maxSize: number) => {
  const result: string[] = []
  const seen = new Set<string>()
  for (const item of titles) {
    const title = item.title?.trim()
    if (!title || seen.has(title)) {
      continue
    }
    seen.add(title)
    result.push(title)
    if (result.length >= maxSize) {
      break
    }
  }
  return result
}

const fetchHotWords = async () => {
  try {
    const size = 10
    const schoolCode = userStore.currentCampus?.schoolCode
    const campusCode = userStore.currentCampus?.campusCode
    const campusHotWords = await getHotWords(schoolCode, size)
    if ((campusHotWords?.length || 0) > 0) {
      hotWords.value = campusHotWords || []
      return
    }

    // 校内热词为空时回退到全站热词
    const globalHotWords = await getHotWords(undefined, size)
    if ((globalHotWords?.length || 0) > 0) {
      hotWords.value = globalHotWords || []
      return
    }

    // 热词统计为空时，回退热门推荐标题，保证面板有可用词
    if (schoolCode || campusCode) {
      const campusRecommend = await getHotRecommend(undefined, size, schoolCode, campusCode)
      const campusRecommendWords = extractHotWordsFromRecommend(campusRecommend || [], size)
      if (campusRecommendWords.length > 0) {
        hotWords.value = campusRecommendWords
        return
      }
    }

    const globalRecommend = await getHotRecommend(undefined, size)
    hotWords.value = extractHotWordsFromRecommend(globalRecommend || [], size)
  } catch {
    console.error('获取热搜词失败')
    hotWords.value = []
  }
}

// 获取搜索历史
const fetchSearchHistory = async () => {
  if (!userStore.isLoggedIn) {
    searchHistory.value = []
    return
  }
  try {
    const res = await getSearchHistory(10)
    searchHistory.value = res || []
  } catch {
    console.error('获取搜索历史失败')
  }
}

// 获取搜索建议
const fetchSuggestions = async (keyword: string) => {
  try {
    const res = await getSearchSuggestions(keyword, 8)
    suggestions.value = res || []
  } catch {
    suggestions.value = []
  }
}

// 清空搜索历史
const handleClearHistory = async () => {
  try {
    await clearSearchHistory()
    searchHistory.value = []
    ElMessage.success('搜索历史已清空')
  } catch {
    ElMessage.error('清空失败')
  }
}

// 执行搜索
const doSearch = (keyword?: string) => {
  if (keyword) {
    searchKeyword.value = keyword
  }
  isSearchMode.value = true
  pageNum.value = 1
  showSearchPanel.value = false
  refresh()
}

// 处理搜索输入框回车
const handleSearchEnter = () => {
  if (searchKeyword.value.trim()) {
    doSearch()
  } else {
    // 搜索词为空时，清空搜索状态
    clearSearch()
  }
}

// 清空搜索
const clearSearch = () => {
  searchKeyword.value = ''
  isSearchMode.value = false
  suggestions.value = []
  pageNum.value = 1
  refresh()
}

// 切换分类
const selectFirstCategory = (categoryId: number | undefined) => {
  selectedFirstCategoryId.value = categoryId || undefined
  selectedSecondCategoryId.value = undefined
  isSearchMode.value = !!searchKeyword.value || !!selectedFirstCategoryId.value || sortType.value !== 0
  pageNum.value = 1
  refresh()
}

const selectSecondCategory = (categoryId: number | undefined) => {
  selectedSecondCategoryId.value = categoryId || undefined
  isSearchMode.value = !!searchKeyword.value || !!selectedFirstCategoryId.value || sortType.value !== 0
  pageNum.value = 1
  refresh()
}

// 切换排序
const selectSort = (type: number) => {
  sortType.value = type
  isSearchMode.value = !!searchKeyword.value || !!selectedFirstCategoryId.value || !!selectedSecondCategoryId.value || type !== 0
  showSortDropdown.value = false
  pageNum.value = 1
  refresh()
}

// 收藏/取消收藏
const toggleCollect = async (goods: SearchResultVO) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  if (collectPendingGoodsIds.value.has(goods.productId)) {
    return
  }

  const willCollect = !Boolean(goods.isCollected)

  collectPendingGoodsIds.value.add(goods.productId)
  try {
    if (willCollect) {
      await collectGoods(goods.productId)
      ElMessage.success('收藏成功')
    } else {
      await uncollectGoods(goods.productId)
      ElMessage.success('取消收藏成功')
    }

    goods.isCollected = willCollect
    const currentCount = Number(goods.collectCount || 0)
    goods.collectCount = Math.max(0, currentCount + (willCollect ? 1 : -1))
  } catch (error) {
    console.error('收藏状态更新失败', error)
    ElMessage.error('操作失败，正在同步收藏状态')
    refresh()
  } finally {
    collectPendingGoodsIds.value.delete(goods.productId)
  }
}

// 跳转到商品详情
const goToDetail = (productId: number) => {
  router.push(`/product/${productId}`)
}

const formatGoodsCampus = (product: SearchResultVO) => {
  const school = product.schoolName || product.schoolCode || '未知学校'
  const campus = product.campusName || product.campusCode || '校区未知'
  return `${school} · ${campus}`
}

// 处理搜索框聚焦
const handleSearchFocus = () => {
  searchInputFocused.value = true
  showSearchPanel.value = true
  fetchHotWords()
  fetchSearchHistory()
}

// 处理搜索框失焦
const handleSearchBlur = () => {
  searchInputFocused.value = false
  // 延迟关闭，允许点击搜索面板中的内容
  setTimeout(() => {
    if (!searchInputFocused.value) {
      showSearchPanel.value = false
    }
  }, 200)
}

// 点击页面其他地方关闭下拉
const handleClickOutside = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (!target.closest('.sort-dropdown-container')) {
    showSortDropdown.value = false
  }
}

onMounted(() => {
  isSearchMode.value = !!searchKeyword.value
    || !!selectedFirstCategoryId.value
    || !!selectedSecondCategoryId.value
    || sortType.value !== 0
  fetchCategories()
  fetchHotWords()
  refresh(pageNum.value)
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  // 清理事件监听器
  document.removeEventListener('click', handleClickOutside)

  // 清理定时器
  if (suggestionTimer) {
    clearTimeout(suggestionTimer)
    suggestionTimer = null
  }

})
</script>

<template>
  <div class="animate-in fade-in duration-500 space-y-6">
    <!-- Unified Search & Category Card -->
    <div class="um-card p-6 space-y-5">
      <!-- Search Bar -->
      <div class="relative">
        <div
          class="flex items-center gap-3 bg-white border border-warm-100 rounded-2xl px-4 py-3 transition-all"
          :class="searchInputFocused ? 'ring-2 ring-warm-200' : ''"
        >
          <Search :size="20" class="text-um-muted" />
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索你想要的宝贝..."
            class="flex-1 bg-transparent outline-none text-sm text-um-text placeholder:text-um-muted"
            @focus="handleSearchFocus"
            @blur="handleSearchBlur"
            @keyup.enter="handleSearchEnter"
          />
          <button
            v-if="searchKeyword"
            @click="clearSearch"
            class="p-1 rounded-full hover:bg-warm-50 transition-colors"
          >
            <X :size="16" class="text-um-muted" />
          </button>
          <button
            @click="handleSearchEnter"
            class="px-4 py-1.5 rounded-full text-xs font-semibold bg-gradient-to-r from-warm-500 to-warm-400 text-white hover:shadow-md transition-all"
          >
            搜索
          </button>
        </div>

        <!-- Search Panel (Hot Words & History) -->
        <div
          v-if="showSearchPanel && !suggestions.length"
          class="absolute top-full left-0 right-0 mt-2 um-card p-4 z-50"
        >
          <!-- Search History -->
          <div v-if="searchHistory.length > 0" class="mb-4">
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs font-bold text-um-muted flex items-center gap-1">
                <Clock :size="12" /> 搜索历史
              </span>
              <button @click="handleClearHistory" class="text-xs text-slate-400 hover:text-warm-500">
                清空
              </button>
            </div>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="word in searchHistory"
                :key="word"
                @click="doSearch(word)"
                class="px-3 py-1.5 bg-um-bg text-um-muted hover:text-warm-600 text-xs rounded-full transition-colors"
              >
                {{ word }}
              </button>
            </div>
          </div>

          <!-- Hot Words -->
          <div v-if="hotWords.length > 0">
            <div class="flex items-center gap-1 mb-2">
              <TrendingUp :size="12" class="text-warm-500" />
              <span class="text-xs font-bold text-um-muted">热门搜索</span>
            </div>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="(word, idx) in hotWords"
                :key="word"
                @click="doSearch(word)"
                class="px-3 py-1.5 text-xs rounded-full transition-colors flex items-center gap-1"
                :class="idx < 3 ? 'bg-warm-50 text-warm-600 hover:bg-warm-100' : 'bg-um-bg text-um-muted hover:bg-warm-50'"
              >
                <span v-if="idx < 3" class="font-bold">{{ idx + 1 }}</span>
                {{ word }}
              </button>
            </div>
          </div>
        </div>

        <!-- Search Suggestions -->
        <div
          v-if="suggestions.length > 0 && searchInputFocused"
          class="absolute top-full left-0 right-0 mt-2 um-card overflow-hidden z-50"
        >
          <button
            v-for="suggestion in suggestions"
            :key="suggestion"
            @click="doSearch(suggestion)"
            class="w-full px-4 py-3 text-left text-sm text-um-text hover:bg-warm-50 transition-colors flex items-center gap-2"
          >
            <Search :size="14" class="text-um-muted" />
            <span class="text-um-text">
              <template v-for="(part, index) in highlightParts(suggestion, searchKeyword)" :key="index">
                <mark v-if="part.highlight" class="bg-transparent text-warm-500 font-bold">{{ part.text }}</mark>
                <span v-else>{{ part.text }}</span>
              </template>
            </span>
          </button>
        </div>
      </div>

      <!-- Divider -->
      <div class="border-t border-warm-100"></div>

      <!-- Category & Sort Section -->
      <div class="space-y-3">
        <!-- First Level Category -->
        <div class="flex items-center gap-3">
          <div class="flex-1 flex gap-2 overflow-x-auto py-1 no-scrollbar">
            <button
              @click="selectFirstCategory(0)"
              class="whitespace-nowrap px-4 py-2 rounded-full text-sm font-semibold transition-all"
              :class="
                !selectedFirstCategoryId
                  ? 'bg-gradient-to-r from-warm-500 to-warm-400 text-white shadow-umSoft'
                  : 'bg-white text-um-muted hover:bg-warm-50'
              "
            >
              全部
            </button>
            <button
              v-for="cat in firstLevelCategories"
              :key="cat.categoryId"
              @click="selectFirstCategory(cat.categoryId)"
              class="whitespace-nowrap px-4 py-2 rounded-full text-sm font-semibold transition-all"
              :class="
                selectedFirstCategoryId === cat.categoryId
                  ? 'bg-gradient-to-r from-warm-500 to-warm-400 text-white shadow-umSoft'
                  : 'bg-white text-um-muted hover:bg-warm-50'
              "
            >
              {{ cat.categoryName }}
            </button>
          </div>
        </div>

        <!-- Second Level Category -->
        <div class="flex items-center gap-3">
          <div class="flex-1">
            <div v-if="selectedFirstCategoryId" class="flex gap-2 overflow-x-auto py-1 no-scrollbar">
              <button
                @click="selectSecondCategory(0)"
                class="whitespace-nowrap px-4 py-2 rounded-full text-sm font-semibold transition-all"
                :class="
                  !selectedSecondCategoryId
                    ? 'bg-warm-100 text-warm-600 shadow-umSoft'
                    : 'bg-white text-um-muted hover:bg-warm-50'
                "
              >
                全部
              </button>
              <button
                v-for="cat in secondLevelCategories"
                :key="cat.categoryId"
                @click="selectSecondCategory(cat.categoryId)"
                class="whitespace-nowrap px-4 py-2 rounded-full text-sm font-semibold transition-all"
                :class="
                  selectedSecondCategoryId === cat.categoryId
                    ? 'bg-warm-100 text-warm-600 shadow-umSoft'
                    : 'bg-white text-um-muted hover:bg-warm-50'
                "
              >
                {{ cat.categoryName }}
              </button>
            </div>
          </div>
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

    <!-- Hero Banner (Only show if not authenticated) -->
    <div
      v-if="userStore.userInfo?.authStatus !== 2 && !isSearchMode"
      class="um-card p-6 mb-8 flex items-center justify-between"
    >
      <div class="max-w-xs">
        <div class="um-badge w-fit mb-2">
          校园认证开启
        </div>
        <h2 class="text-xl font-bold text-um-text leading-tight">
          通过校内身份认证，即可享有安全的交易体验。
        </h2>
        <button
          class="mt-4 um-btn um-btn-primary px-4 py-2 text-sm"
          @click="router.push('/profile/auth')"
        >
          去认证身份
        </button>
      </div>
      <div class="hidden sm:block">
        <ShieldCheck :size="80" class="text-warm-200" />
      </div>
    </div>

    <!-- Search Result Info -->
    <div v-if="isSearchMode && searchKeyword" class="mb-4 flex items-center justify-between">
      <div class="text-sm text-slate-500">
        搜索 "<span class="text-warm-500 font-medium">{{ searchKeyword }}</span>" 共找到
        <span class="font-bold text-slate-700">{{ total }}</span> 件商品
      </div>
    </div>

    <!-- Loading -->
    <div v-if="showInitialLoading" class="text-center py-12">
      <div
        class="inline-block animate-spin rounded-full h-12 w-12 border-4 border-warm-500 border-t-transparent"
      ></div>
      <p class="mt-4 text-um-muted">加载中...</p>
    </div>

    <!-- Empty State -->
    <div v-else-if="goodsList.length === 0" class="text-center py-12">
      <p class="text-slate-500">{{ isSearchMode ? '未找到相关商品，换个关键词试试吧' : '暂无商品' }}</p>
    </div>

    <!-- Grid -->
    <div v-else>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
        <div
          v-for="product in goodsList"
          :key="product.productId"
          class="group um-card overflow-hidden hover:-translate-y-1 transition-all duration-300 cursor-pointer"
          @click="goToDetail(product.productId)"
        >
          <div class="relative aspect-[4/3] overflow-hidden">
            <LazyImage
              :src="product.image || 'https://via.placeholder.com/400x300?text=No+Image'"
              :alt="product.title"
            />
            <div
              class="absolute top-2 left-2 bg-black/55 text-white text-[10px] px-2 py-1 rounded-lg flex items-center gap-1"
            >
              <CheckCircle2 :size="10" class="text-green-400" /> 校内认证
            </div>
            <button
              class="absolute bottom-2 right-2 p-1.5 bg-white border border-warm-100 rounded-full transition-opacity"
              :class="
                product.isCollected
                  ? 'text-warm-500'
                  : 'text-slate-400 opacity-0 group-hover:opacity-100'
              "
              @click.stop="toggleCollect(product)"
            >
              <Heart :size="16" :fill="product.isCollected ? 'currentColor' : 'none'" />
            </button>
          </div>
          <div class="p-4">
            <h3 class="font-bold text-um-text text-sm mb-1 line-clamp-2" v-html="renderHighlightTitle(product.title)"></h3>
            <div class="flex items-baseline gap-2 mb-2">
              <span class="text-warm-500 font-bold text-lg">¥{{ product.price }}</span>
              <span v-if="product.collectCount" class="text-um-muted text-xs">{{ product.collectCount }}收藏</span>
            </div>
            <div class="flex items-center justify-between text-xs text-um-muted">
              <span class="truncate max-w-[48%]">{{ product.sellerName || '未知卖家' }}</span>
              <span>{{ product.categoryName || '未分类' }}</span>
            </div>
            <p class="mt-1 text-xs text-um-muted truncate">{{ formatGoodsCampus(product) }}</p>
          </div>
        </div>
      </div>

      <InfiniteListFooter
        :is-loading-more="isLoadingMore"
        :has-more="hasMore"
        :set-trigger="setLoadMoreTrigger"
        loading-text="正在加载更多商品..."
        loading-text-class="text-um-muted"
        end-text-class="text-um-muted"
      />
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
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
