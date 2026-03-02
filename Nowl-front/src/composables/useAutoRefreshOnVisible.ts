import { onMounted, onUnmounted } from 'vue'

interface UseAutoRefreshOnVisibleOptions {
  refresh: () => void | Promise<unknown>
  intervalMs?: number
  minGapMs?: number
  initialDelayMs?: number
  enabled?: () => boolean
}

export const useAutoRefreshOnVisible = (options: UseAutoRefreshOnVisibleOptions) => {
  const intervalMs = options.intervalMs ?? 30000
  const minGapMs = options.minGapMs ?? 5000
  const isEnabled = options.enabled ?? (() => true)

  let timer: ReturnType<typeof setInterval> | null = null
  let initTimer: ReturnType<typeof setTimeout> | null = null
  let lastTriggeredAt = 0

  const triggerAutoRefresh = () => {
    if (!isEnabled()) return

    const now = Date.now()
    if (now - lastTriggeredAt < minGapMs) return

    lastTriggeredAt = now
    void options.refresh()
  }

  const handleWindowFocus = () => {
    triggerAutoRefresh()
  }

  const handleVisibilityChange = () => {
    if (document.visibilityState === 'visible') {
      triggerAutoRefresh()
    }
  }

  onMounted(() => {
    window.addEventListener('focus', handleWindowFocus)
    document.addEventListener('visibilitychange', handleVisibilityChange)
    timer = setInterval(() => {
      if (document.visibilityState === 'visible') {
        triggerAutoRefresh()
      }
    }, intervalMs)

    if (options.initialDelayMs !== undefined && options.initialDelayMs >= 0) {
      initTimer = setTimeout(() => {
        triggerAutoRefresh()
      }, options.initialDelayMs)
    }
  })

  onUnmounted(() => {
    window.removeEventListener('focus', handleWindowFocus)
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    if (initTimer) {
      clearTimeout(initTimer)
      initTimer = null
    }
  })

  return {
    triggerAutoRefresh,
  }
}
