import { createApp, h, ref } from 'vue'
import UniModal from '@/components/UniModal.vue'
import UniPromptModal from '@/components/UniPromptModal.vue'

export interface ModalOptions {
  type?: 'success' | 'error' | 'warning' | 'info' | 'ai'
  title?: string
  message: string
  confirmText?: string
  cancelText?: string
  showCancel?: boolean
  closable?: boolean
}

export interface PromptOptions {
  title?: string
  message?: string
  placeholder?: string
  confirmText?: string
  cancelText?: string
  inputValue?: string
  inputType?: 'text' | 'textarea'
  validator?: (value: string) => string | null
}

type ToastType = Exclude<NonNullable<ModalOptions['type']>, undefined>

interface ToastTheme {
  border: string
  background: string
  titleColor: string
  messageColor: string
}

const TOAST_DURATION = 2200
const TOAST_CLOSE_ANIMATION = 180
let toastHost: HTMLDivElement | null = null

const toastThemes: Record<ToastType, ToastTheme> = {
  success: {
    border: 'rgba(34, 197, 94, 0.25)',
    background: 'linear-gradient(135deg, rgba(240, 253, 244, 0.98), rgba(220, 252, 231, 0.98))',
    titleColor: '#166534',
    messageColor: '#166534',
  },
  error: {
    border: 'rgba(239, 68, 68, 0.25)',
    background: 'linear-gradient(135deg, rgba(254, 242, 242, 0.98), rgba(254, 226, 226, 0.98))',
    titleColor: '#991b1b',
    messageColor: '#991b1b',
  },
  warning: {
    border: 'rgba(245, 158, 11, 0.25)',
    background: 'linear-gradient(135deg, rgba(255, 251, 235, 0.98), rgba(254, 243, 199, 0.98))',
    titleColor: '#92400e',
    messageColor: '#92400e',
  },
  info: {
    border: 'rgba(59, 130, 246, 0.22)',
    background: 'linear-gradient(135deg, rgba(239, 246, 255, 0.98), rgba(219, 234, 254, 0.98))',
    titleColor: '#1d4ed8',
    messageColor: '#1d4ed8',
  },
  ai: {
    border: 'rgba(141, 110, 99, 0.25)',
    background: 'linear-gradient(135deg, rgba(239, 235, 233, 0.98), rgba(255, 243, 224, 0.98))',
    titleColor: '#6D4C41',
    messageColor: '#E65100',
  },
}

const ensureToastHost = () => {
  if (toastHost && document.body.contains(toastHost)) {
    return toastHost
  }

  const host = document.createElement('div')
  host.style.position = 'fixed'
  host.style.top = '20px'
  host.style.right = '20px'
  host.style.display = 'flex'
  host.style.flexDirection = 'column'
  host.style.gap = '10px'
  host.style.zIndex = '4000'
  host.style.pointerEvents = 'none'
  host.style.maxWidth = 'min(360px, calc(100vw - 28px))'
  document.body.appendChild(host)
  toastHost = host
  return host
}

const showToast = (type: ToastType, message: string, title: string) => {
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    return
  }

  const host = ensureToastHost()
  const theme = toastThemes[type]
  const toast = document.createElement('div')
  toast.style.pointerEvents = 'none'
  toast.style.border = `1px solid ${theme.border}`
  toast.style.borderRadius = '14px'
  toast.style.padding = '12px 14px'
  toast.style.background = theme.background
  toast.style.boxShadow = '0 12px 28px rgba(15, 23, 42, 0.12)'
  toast.style.backdropFilter = 'blur(4px)'
  toast.style.opacity = '0'
  toast.style.transform = 'translateY(-6px)'
  toast.style.transition = 'opacity 0.18s ease, transform 0.18s ease'

  const titleNode = document.createElement('div')
  titleNode.textContent = title
  titleNode.style.fontSize = '13px'
  titleNode.style.fontWeight = '700'
  titleNode.style.color = theme.titleColor

  const messageNode = document.createElement('div')
  messageNode.textContent = message
  messageNode.style.marginTop = '4px'
  messageNode.style.fontSize = '13px'
  messageNode.style.lineHeight = '1.45'
  messageNode.style.color = theme.messageColor

  toast.appendChild(titleNode)
  toast.appendChild(messageNode)
  host.appendChild(toast)

  requestAnimationFrame(() => {
    toast.style.opacity = '1'
    toast.style.transform = 'translateY(0)'
  })

  const closeToast = () => {
    toast.style.opacity = '0'
    toast.style.transform = 'translateY(-6px)'
    window.setTimeout(() => {
      toast.remove()
      if (host.childElementCount === 0) {
        host.remove()
        toastHost = null
      }
    }, TOAST_CLOSE_ANIMATION)
  }

  window.setTimeout(closeToast, TOAST_DURATION)
}

/**
 * 创建一个Promise形式的弹窗
 */
function createModal(options: ModalOptions): Promise<boolean> {
  return new Promise((resolve) => {
    const container = document.createElement('div')
    document.body.appendChild(container)

    const visible = ref(true)

    const app = createApp({
      setup() {
        const handleConfirm = () => {
          visible.value = false
          setTimeout(() => {
            app.unmount()
            container.remove()
            resolve(true)
          }, 300)
        }

        const handleCancel = () => {
          visible.value = false
          setTimeout(() => {
            app.unmount()
            container.remove()
            resolve(false)
          }, 300)
        }

        return () =>
          h(UniModal, {
            modelValue: visible.value,
            'onUpdate:modelValue': (val: boolean) => {
              visible.value = val
              if (!val) handleCancel()
            },
            type: options.type || 'info',
            title: options.title || '提示',
            message: options.message,
            confirmText: options.confirmText || '确定',
            cancelText: options.cancelText || '取消',
            showCancel: options.showCancel || false,
            closable: options.closable !== false,
            onConfirm: handleConfirm,
            onCancel: handleCancel,
          })
      },
    })

    app.mount(container)
  })
}

function createPrompt(options: PromptOptions): Promise<{ confirmed: boolean; value: string }> {
  return new Promise((resolve) => {
    const container = document.createElement('div')
    document.body.appendChild(container)

    const visible = ref(true)

    const closeWith = (payload: { confirmed: boolean; value: string }) => {
      visible.value = false
      setTimeout(() => {
        app.unmount()
        container.remove()
        resolve(payload)
      }, 300)
    }

    const validationError = ref('')

    const app = createApp({
      setup() {
        const handleConfirm = (value: string) => {
          if (options.validator) {
            const error = options.validator(value)
            if (error) {
              validationError.value = error
              return
            }
          }
          validationError.value = ''
          closeWith({ confirmed: true, value })
        }

        const handleCancel = () => {
          validationError.value = ''
          closeWith({ confirmed: false, value: options.inputValue || '' })
        }

        return () =>
          h(UniPromptModal, {
            modelValue: visible.value,
            'onUpdate:modelValue': (val: boolean) => {
              visible.value = val
              if (!val) {
                handleCancel()
              }
            },
            title: options.title || '请输入',
            message: options.message || '',
            placeholder: options.placeholder || '',
            confirmText: options.confirmText || '确定',
            cancelText: options.cancelText || '取消',
            inputValue: options.inputValue || '',
            inputType: options.inputType || 'text',
            errorText: validationError.value,
            onConfirm: handleConfirm,
            onCancel: handleCancel,
          })
      },
    })

    app.mount(container)
  })
}

/**
 * 成功提示弹窗
 */
export function showSuccess(message: string, title = '成功'): Promise<boolean> {
  showToast('success', message, title)
  return Promise.resolve(true)
}

/**
 * 错误提示弹窗
 */
export function showError(message: string, title = '出错了'): Promise<boolean> {
  showToast('error', message, title)
  return Promise.resolve(true)
}

/**
 * 警告提示弹窗
 */
export function showWarning(message: string, title = '注意'): Promise<boolean> {
  showToast('warning', message, title)
  return Promise.resolve(true)
}

/**
 * 信息提示弹窗
 */
export function showInfo(message: string, title = '提示'): Promise<boolean> {
  showToast('info', message, title)
  return Promise.resolve(true)
}

/**
 * AI相关提示弹窗（带渐变效果）
 */
export function showAiMessage(message: string, title = 'Nowl AI'): Promise<boolean> {
  showToast('ai', message, title)
  return Promise.resolve(true)
}

/**
 * 确认弹窗（带取消按钮）
 */
export function showConfirm(
  message: string,
  title = '确认',
  options?: Partial<ModalOptions>
): Promise<boolean> {
  return createModal({
    type: 'warning',
    title,
    message,
    showCancel: true,
    ...options,
  })
}

export function showPrompt(options: PromptOptions): Promise<{ confirmed: boolean; value: string }> {
  return createPrompt(options)
}

/**
 * 权限不足提示
 */
export function showUnauthorized(message = '请先登录后再进行此操作'): Promise<boolean> {
  showToast('warning', message, '需要登录')
  return Promise.resolve(true)
}

/**
 * 需要认证提示
 */
export function showNeedAuth(message = '为保障交易安全，请先完成实名认证'): Promise<boolean> {
  showToast('warning', message, '需要认证')
  return Promise.resolve(true)
}

// 默认导出所有方法
export default {
  success: showSuccess,
  error: showError,
  warning: showWarning,
  info: showInfo,
  ai: showAiMessage,
  confirm: showConfirm,
  prompt: showPrompt,
  unauthorized: showUnauthorized,
  needAuth: showNeedAuth,
}
