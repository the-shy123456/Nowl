import { ref, watch, type Ref } from 'vue'
import {
  useRoute,
  useRouter,
  type LocationQueryRaw,
  type LocationQueryValue,
} from 'vue-router'

type QueryRawValue = LocationQueryValue | LocationQueryValue[] | undefined

interface QueryBinding<T = unknown> {
  key: string
  state: Ref<T>
  defaultValue: T
  parse?: (raw: QueryRawValue) => T
  serialize?: (value: T, defaultValue: T) => string | undefined
}

interface UseListQuerySyncOptions {
  onQueryApplied?: (changedKeys: string[]) => void
}

export const createQueryBinding = <T>(
  binding: QueryBinding<T>
): QueryBinding<unknown> => binding as QueryBinding<unknown>

const getFirstQueryValue = (raw: QueryRawValue): string | undefined => {
  if (Array.isArray(raw)) return raw[0] ?? undefined
  return raw ?? undefined
}

export const parseEnumQueryNumber = (
  raw: QueryRawValue,
  allowedValues: number[],
  defaultValue: number
): number => {
  const value = Math.trunc(parseNumber(raw, defaultValue))
  return allowedValues.includes(value) ? value : defaultValue
}

export const parseOptionalEnumQueryNumber = (
  raw: QueryRawValue,
  allowedValues: number[]
): number | undefined => {
  const firstValue = getFirstQueryValue(raw)
  if (firstValue === undefined || firstValue === '') return undefined

  const numericValue = Math.trunc(Number(firstValue))
  if (!Number.isFinite(numericValue)) return undefined
  return allowedValues.includes(numericValue) ? numericValue : undefined
}

export const parsePositiveIntQuery = (
  raw: QueryRawValue,
  defaultValue = 1
): number => {
  const value = Math.trunc(parseNumber(raw, defaultValue))
  return value > 0 ? value : defaultValue
}

export const parseOptionalPositiveIntQuery = (
  raw: QueryRawValue
): number | undefined => {
  const firstValue = getFirstQueryValue(raw)
  if (firstValue === undefined || firstValue === '') return undefined

  const numericValue = Math.trunc(Number(firstValue))
  if (!Number.isFinite(numericValue)) return undefined
  return numericValue > 0 ? numericValue : undefined
}

export const serializeOptionalPositiveIntQuery = (
  value: number | undefined | null
): string | undefined => {
  if (value === undefined || value === null) return undefined
  const normalizedValue = Math.trunc(Number(value))
  if (!Number.isFinite(normalizedValue) || normalizedValue <= 0) return undefined
  return String(normalizedValue)
}

export const serializePageQuery = (
  value: number,
  defaultPage = 1
): string | undefined => {
  const normalizedPage = Math.trunc(Number(value))
  if (!Number.isFinite(normalizedPage) || normalizedPage <= defaultPage) return undefined
  return String(normalizedPage)
}

const parseNumber = (raw: QueryRawValue, defaultValue: number): number => {
  const firstValue = getFirstQueryValue(raw)
  if (firstValue === undefined || firstValue === '') return defaultValue
  const numericValue = Number(firstValue)
  return Number.isFinite(numericValue) ? numericValue : defaultValue
}

const parseBoolean = (raw: QueryRawValue, defaultValue: boolean): boolean => {
  const firstValue = getFirstQueryValue(raw)
  if (firstValue === undefined) return defaultValue
  return firstValue === '1' || firstValue === 'true'
}

const parseByDefaultType = <T>(raw: QueryRawValue, defaultValue: T): T => {
  if (typeof defaultValue === 'number') {
    return parseNumber(raw, defaultValue) as T
  }
  if (typeof defaultValue === 'boolean') {
    return parseBoolean(raw, defaultValue) as T
  }
  const firstValue = getFirstQueryValue(raw)
  return (firstValue ?? defaultValue) as T
}

const serializeByDefaultType = <T>(value: T, defaultValue: T): string | undefined => {
  if (value === defaultValue) return undefined

  if (typeof value === 'boolean') {
    return value ? '1' : '0'
  }

  const normalized = String(value).trim()
  return normalized ? normalized : undefined
}

export const useListQuerySync = (
  bindings: Array<QueryBinding<unknown>>,
  options: UseListQuerySyncOptions = {}
) => {
  const route = useRoute()
  const router = useRouter()
  const applyingRouteState = ref(false)

  const applyRouteQueryToState = (emitChange = true) => {
    applyingRouteState.value = true
    const changedKeys: string[] = []

    for (const binding of bindings) {
      const rawValue = route.query[binding.key]
      const parsedValue = binding.parse
        ? binding.parse(rawValue)
        : parseByDefaultType(rawValue, binding.defaultValue)

      if (binding.state.value !== parsedValue) {
        binding.state.value = parsedValue
        changedKeys.push(binding.key)
      }
    }

    applyingRouteState.value = false
    if (emitChange && changedKeys.length > 0) {
      options.onQueryApplied?.(changedKeys)
    }
  }

  const syncStateToRouteQuery = async () => {
    if (applyingRouteState.value) return

    const nextQuery: LocationQueryRaw = { ...route.query }
    let changed = false

    for (const binding of bindings) {
      const serializedValue = binding.serialize
        ? binding.serialize(binding.state.value, binding.defaultValue)
        : serializeByDefaultType(binding.state.value, binding.defaultValue)
      const currentValue = getFirstQueryValue(route.query[binding.key])

      if (serializedValue === undefined) {
        if (currentValue !== undefined) {
          delete nextQuery[binding.key]
          changed = true
        }
        continue
      }

      if (currentValue !== serializedValue) {
        nextQuery[binding.key] = serializedValue
        changed = true
      }
    }

    if (!changed) return

    try {
      await router.replace({ query: nextQuery })
    } catch {
      // ignore duplicated navigation
    }
  }

  applyRouteQueryToState(false)

  watch(
    () => route.query,
    () => {
      applyRouteQueryToState()
    },
    { deep: true }
  )

  watch(bindings.map(binding => binding.state), () => {
    syncStateToRouteQuery()
  })

  return {
    applyRouteQueryToState,
    syncStateToRouteQuery,
  }
}
