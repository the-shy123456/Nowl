import type { LocationQuery, RouteLocationNormalizedLoaded, Router } from 'vue-router'

const ROUTE_HISTORY_KEY = 'um:route-history'
const MAX_ROUTE_HISTORY = 80

const isSafeInternalPath = (value: unknown): value is string => {
  return typeof value === 'string' && value.startsWith('/') && !value.startsWith('//')
}

const getQueryValue = (query: LocationQuery, key: string): string | null => {
  const raw = query[key]
  const value = Array.isArray(raw) ? raw[0] : raw
  if (!isSafeInternalPath(value)) {
    return null
  }
  return value
}

const readRouteHistory = (): string[] => {
  if (typeof window === 'undefined') {
    return []
  }
  try {
    const raw = window.sessionStorage.getItem(ROUTE_HISTORY_KEY)
    if (!raw) {
      return []
    }
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return []
    }
    return parsed.filter((item): item is string => isSafeInternalPath(item))
  } catch {
    return []
  }
}

const writeRouteHistory = (history: string[]) => {
  if (typeof window === 'undefined') {
    return
  }
  try {
    const normalized = history
      .filter(path => isSafeInternalPath(path))
      .slice(-MAX_ROUTE_HISTORY)
    window.sessionStorage.setItem(ROUTE_HISTORY_KEY, JSON.stringify(normalized))
  } catch {
    // sessionStorage 不可用时静默降级
  }
}

const popHistoryTarget = (currentFullPath: string): string | null => {
  let history = readRouteHistory()
  if (!history.length) {
    return null
  }

  const last = history[history.length - 1]
  if (last === currentFullPath) {
    history.pop()
  } else {
    const currentIndex = history.lastIndexOf(currentFullPath)
    if (currentIndex > 0) {
      history = history.slice(0, currentIndex)
    }
  }

  const target = history[history.length - 1]
  writeRouteHistory(history)
  if (isSafeInternalPath(target) && target !== currentFullPath) {
    return target
  }
  return null
}

export const recordRouteSource = (toFullPath: string, fromFullPath: string) => {
  if (typeof window === 'undefined') {
    return
  }
  if (!isSafeInternalPath(toFullPath) || !isSafeInternalPath(fromFullPath)) {
    return
  }
  if (toFullPath === fromFullPath) {
    return
  }

  let history = readRouteHistory()
  if (!history.length) {
    writeRouteHistory([fromFullPath, toFullPath])
    return
  }

  const last = history[history.length - 1]
  const prev = history.length > 1 ? history[history.length - 2] : null

  // 已记录为最新目标，避免在返回后再次写入导致死循环
  if (last === toFullPath) {
    return
  }

  if (last === fromFullPath) {
    // 标准前进导航；若是回到上一步则弹栈
    if (prev === toFullPath) {
      history.pop()
    } else {
      history.push(toFullPath)
    }
    writeRouteHistory(history)
    return
  }

  // 栈与当前导航不同步（例如刷新后继续跳转），尝试对齐后再写入
  const fromIndex = history.lastIndexOf(fromFullPath)
  if (fromIndex >= 0) {
    history = history.slice(0, fromIndex + 1)
  } else {
    history.push(fromFullPath)
  }
  if (history[history.length - 1] !== toFullPath) {
    history.push(toFullPath)
  }
  writeRouteHistory(history)
}

export const resolveBackTarget = (
  route: RouteLocationNormalizedLoaded,
  explicitFallback?: string,
): string | null => {
  const queryTarget = getQueryValue(route.query, 'from') || getQueryValue(route.query, 'back')
  if (queryTarget && queryTarget !== route.fullPath) {
    return queryTarget
  }

  const historyTarget = popHistoryTarget(route.fullPath)
  if (historyTarget && historyTarget !== route.fullPath) {
    return historyTarget
  }

  if (isSafeInternalPath(explicitFallback) && explicitFallback !== route.fullPath) {
    return explicitFallback
  }

  return null
}

export const navigateBack = (
  router: Router,
  route: RouteLocationNormalizedLoaded,
  explicitFallback = '/',
) => {
  const target = resolveBackTarget(route, explicitFallback)
  if (target) {
    router.push(target)
    return
  }

  if (typeof window !== 'undefined' && window.history.length > 1) {
    router.back()
    return
  }

  router.push('/')
}
