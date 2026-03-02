import { computed, ref, unref, type Ref } from 'vue'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'

interface FetchPageParams {
  pageNum: number
  pageSize: number
}

interface PageData<T> {
  records?: T[]
  total?: number
}

interface UsePaginatedListOptions<T> {
  pageSize: number | Ref<number>
  fetchPage: (params: FetchPageParams) => Promise<PageData<T> | null | undefined>
  initialPageNum?: number | Ref<number>
  onError?: (error: unknown) => void
  onSuccess?: (list: T[]) => void | Promise<void>
}

export const usePaginatedList = <T>(options: UsePaginatedListOptions<T>) => {
  const list = ref<T[]>([]) as Ref<T[]>
  const loading = ref(false)
  const pageNum = ref(1)
  const total = ref(0)
  const reachedEndByPageSize = ref(false)
  let latestFetchToken = 0

  const normalizePageNum = (value: number | undefined | null) => {
    const numericValue = Number(value)
    if (!Number.isFinite(numericValue)) return 1
    const normalized = Math.trunc(numericValue)
    return normalized > 0 ? normalized : 1
  }

  const getInitialPageNum = () => normalizePageNum(
    options.initialPageNum === undefined ? 1 : unref(options.initialPageNum)
  )

  const hasMore = computed(() => {
    if (reachedEndByPageSize.value) {
      return false
    }

    if (total.value > 0) {
      return list.value.length < total.value
    }

    return list.value.length > 0
  })

  const showInitialLoading = computed(() => loading.value && pageNum.value === 1 && list.value.length === 0)
  const isLoadingMore = computed(() => loading.value && list.value.length > 0)

  const loadPage = async (targetPage: number, fetchToken = ++latestFetchToken) => {
    if (fetchToken > latestFetchToken) {
      latestFetchToken = fetchToken
    }

    const currentPageSize = unref(options.pageSize)
    loading.value = true

    try {
      const res = await options.fetchPage({
        pageNum: targetPage,
        pageSize: currentPageSize,
      })

      if (fetchToken !== latestFetchToken) return false

      const records = (res?.records || []) as T[]
      list.value = targetPage === 1
        ? [...records]
        : [...(list.value as T[]), ...records]
      total.value = res?.total || 0
      pageNum.value = targetPage
      reachedEndByPageSize.value = records.length < currentPageSize

      if (options.onSuccess) {
        await options.onSuccess(list.value as T[])
      }
      return true
    } catch (error) {
      if (targetPage === 1) {
        list.value = []
        total.value = 0
      }
      options.onError?.(error)
      return false
    } finally {
      if (fetchToken === latestFetchToken) {
        loading.value = false
      }
    }
  }

  const refresh = async (targetPage = getInitialPageNum()) => {
    const normalizedTargetPage = normalizePageNum(targetPage)

    if (normalizedTargetPage <= 1) {
      await loadPage(1)
      return
    }

    const fetchToken = latestFetchToken + 1
    latestFetchToken = fetchToken
    reset()

    for (let page = 1; page <= normalizedTargetPage; page += 1) {
      const loaded = await loadPage(page, fetchToken)
      if (!loaded) return
      if (!hasMore.value && page < normalizedTargetPage) return
    }
  }

  const loadMore = async () => {
    if (loading.value || !hasMore.value) return
    await loadPage(pageNum.value + 1)
  }

  const reset = () => {
    pageNum.value = 1
    total.value = 0
    list.value = []
    reachedEndByPageSize.value = false
  }

  const { loadMoreTrigger, reconnectInfiniteObserver } = useInfiniteScroll(hasMore, loading, loadMore)

  return {
    list,
    loading,
    pageNum,
    total,
    hasMore,
    showInitialLoading,
    isLoadingMore,
    loadMoreTrigger,
    reconnectInfiniteObserver,
    refresh,
    loadMore,
    reset,
  }
}
