const LOCAL_BACKEND_HOSTS = new Set(['localhost', '127.0.0.1'])
const LOCAL_BACKEND_PORT = '8080'
const INVALID_MEDIA_TEXT = new Set(['null', 'undefined', 'n/a', 'none'])

const shouldProxyLocalBackend = (url: URL): boolean => {
  if (!LOCAL_BACKEND_HOSTS.has(url.hostname)) {
    return false
  }
  const port = url.port || (url.protocol === 'https:' ? '443' : '80')
  return port === LOCAL_BACKEND_PORT
}

const normalizeAbsoluteMediaUrl = (raw: string): string => {
  let parsed: URL
  try {
    parsed = new URL(raw)
  } catch {
    return raw
  }

  if (typeof window === 'undefined') {
    return raw
  }

  const currentOrigin = window.location.origin
  if (!shouldProxyLocalBackend(parsed) || parsed.origin === currentOrigin) {
    return raw
  }

  const pathWithQuery = `${parsed.pathname}${parsed.search}${parsed.hash}`
  if (parsed.pathname.startsWith('/uploads')) {
    return pathWithQuery
  }
  return `/api${pathWithQuery}`
}

export const normalizeMediaUrl = (value?: string | null): string | undefined => {
  if (value == null) {
    return undefined
  }

  const raw = value.trim()
  if (!raw) {
    return undefined
  }

  const lower = raw.toLowerCase()
  if (INVALID_MEDIA_TEXT.has(lower)) {
    return undefined
  }

  if (raw.startsWith('data:') || raw.startsWith('blob:')) {
    return raw
  }

  if (raw.startsWith('//')) {
    if (typeof window === 'undefined') {
      return `https:${raw}`
    }
    return `${window.location.protocol}${raw}`
  }

  if (/^https?:\/\//i.test(raw)) {
    return normalizeAbsoluteMediaUrl(raw)
  }

  if (raw.startsWith('/')) {
    return raw
  }

  if (raw.startsWith('uploads/')) {
    return `/${raw}`
  }

  return raw
}

const MEDIA_KEY_PATTERN = /(avatar|image|img|photo|picture|cover|thumb)/i

export const normalizeMediaData = <T>(data: T): T => {
  if (Array.isArray(data)) {
    return data.map(item => normalizeMediaData(item)) as T
  }

  if (!data || typeof data !== 'object') {
    return data
  }

  const source = data as Record<string, unknown>
  const output: Record<string, unknown> = {}

  for (const [key, value] of Object.entries(source)) {
    if (typeof value === 'string') {
      const shouldNormalize =
        MEDIA_KEY_PATTERN.test(key) ||
        value.startsWith('http://') ||
        value.startsWith('https://') ||
        value.startsWith('uploads/') ||
        value.startsWith('/uploads/') ||
        value.startsWith('/file/')

      output[key] = shouldNormalize ? normalizeMediaUrl(value) ?? '' : value
      continue
    }

    if (Array.isArray(value) || (value && typeof value === 'object')) {
      output[key] = normalizeMediaData(value)
      continue
    }

    output[key] = value
  }

  return output as T
}
