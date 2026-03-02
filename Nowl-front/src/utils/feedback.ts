import { showConfirm, showError, showInfo, showPrompt, showSuccess, showWarning } from '@/utils/modal'

type MessageType = 'success' | 'error' | 'warning' | 'info'

type PromptValidator = (value: string) => boolean | string

interface ConfirmOptions {
  confirmButtonText?: string
  cancelButtonText?: string
}

interface PromptOptions extends ConfirmOptions {
  inputPlaceholder?: string
  inputValue?: string
  inputValidator?: PromptValidator
}

const openMessage = (type: MessageType, message: string) => {
  if (type === 'success') {
    void showSuccess(message)
    return
  }
  if (type === 'warning') {
    void showWarning(message)
    return
  }
  if (type === 'info') {
    void showInfo(message)
    return
  }
  void showError(message)
}

export const ElMessage = {
  success(message: string) {
    openMessage('success', message)
  },
  warning(message: string) {
    openMessage('warning', message)
  },
  info(message: string) {
    openMessage('info', message)
  },
  error(message: string) {
    openMessage('error', message)
  },
}

const normalizeValidator = (validator?: PromptValidator) => {
  if (!validator) {
    return undefined
  }
  return (value: string): string | null => {
    const result = validator(value)
    if (result === true) {
      return null
    }
    if (result === false) {
      return '输入不合法'
    }
    if (typeof result === 'string' && result.trim()) {
      return result
    }
    return null
  }
}

export const ElMessageBox = {
  async confirm(message: string, title = '提示', options?: ConfirmOptions) {
    const confirmed = await showConfirm(message, title, {
      confirmText: options?.confirmButtonText || '确定',
      cancelText: options?.cancelButtonText || '取消',
    })
    if (!confirmed) {
      throw 'cancel'
    }
  },

  async prompt(message: string, title = '请输入', options?: PromptOptions) {
    const result = await showPrompt({
      title,
      message,
      placeholder: options?.inputPlaceholder || '',
      inputValue: options?.inputValue || '',
      confirmText: options?.confirmButtonText || '确定',
      cancelText: options?.cancelButtonText || '取消',
      validator: normalizeValidator(options?.inputValidator),
    })
    if (!result.confirmed) {
      throw 'cancel'
    }
    return { value: result.value }
  },
}
