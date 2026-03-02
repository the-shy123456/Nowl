import { onUnmounted, ref, watch, type Ref } from 'vue'

interface UseInfiniteScrollOptions {
  rootMargin?: string
  threshold?: number
}

export const useInfiniteScroll = (
  canLoadMore: Ref<boolean>,
  isLoading: Ref<boolean>,
  onLoadMore: () => void,
  options: UseInfiniteScrollOptions = {}
) => {
  const loadMoreTrigger = ref<HTMLElement | null>(null)
  let observer: IntersectionObserver | null = null

  const reconnectInfiniteObserver = () => {
    if (observer) {
      observer.disconnect()
      observer = null
    }

    if (!loadMoreTrigger.value) return

    observer = new IntersectionObserver(
      entries => {
        if (entries.some(entry => entry.isIntersecting) && canLoadMore.value && !isLoading.value) {
          onLoadMore()
        }
      },
      {
        root: null,
        rootMargin: options.rootMargin ?? '240px 0px',
        threshold: options.threshold ?? 0.1,
      }
    )

    observer.observe(loadMoreTrigger.value)
  }

  watch(loadMoreTrigger, () => {
    reconnectInfiniteObserver()
  })

  onUnmounted(() => {
    if (observer) {
      observer.disconnect()
      observer = null
    }
  })

  return {
    loadMoreTrigger,
    reconnectInfiniteObserver,
  }
}

