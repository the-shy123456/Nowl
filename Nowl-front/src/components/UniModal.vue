<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { X, CheckCircle, AlertCircle, Info, AlertTriangle, Sparkles } from 'lucide-vue-next'

export interface ModalProps {
  modelValue: boolean
  type?: 'success' | 'error' | 'warning' | 'info' | 'ai'
  title?: string
  message?: string
  confirmText?: string
  cancelText?: string
  showCancel?: boolean
  closable?: boolean
}

const props = withDefaults(defineProps<ModalProps>(), {
  type: 'info',
  title: '提示',
  message: '',
  confirmText: '确定',
  cancelText: '取消',
  showCancel: false,
  closable: true,
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()

const visible = ref(props.modelValue)

watch(() => props.modelValue, (val) => {
  visible.value = val
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const close = () => {
  if (props.closable) {
    visible.value = false
    emit('cancel')
  }
}

const confirm = () => {
  visible.value = false
  emit('confirm')
}

const cancel = () => {
  visible.value = false
  emit('cancel')
}

// 根据类型获取图标和颜色
const iconConfig = computed(() => {
  const configs = {
    success: {
      icon: CheckCircle,
      bgColor: 'bg-emerald-100',
      iconColor: 'text-emerald-500',
      buttonBg: 'bg-emerald-500 hover:bg-emerald-600',
      shadowColor: 'shadow-sm',
    },
    error: {
      icon: AlertCircle,
      bgColor: 'bg-red-100',
      iconColor: 'text-red-500',
      buttonBg: 'bg-red-500 hover:bg-red-600',
      shadowColor: 'shadow-sm',
    },
    warning: {
      icon: AlertTriangle,
      bgColor: 'bg-amber-100',
      iconColor: 'text-amber-500',
      buttonBg: 'bg-amber-500 hover:bg-amber-600',
      shadowColor: 'shadow-sm',
    },
    info: {
      icon: Info,
      bgColor: 'bg-warm-100',
      iconColor: 'text-warm-500',
      buttonBg: 'bg-warm-500 hover:bg-warm-600',
      shadowColor: 'shadow-sm',
    },
    ai: {
      icon: Sparkles,
      bgColor: 'bg-warm-100',
      iconColor: 'text-warm-500',
      buttonBg: 'bg-warm-500 hover:bg-warm-600',
      shadowColor: 'shadow-sm',
    },
  }
  return configs[props.type]
})
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="visible"
        class="fixed inset-0 z-[9999] flex items-center justify-center p-4"
        @click.self="close"
      >
        <!-- 背景遮罩 -->
        <div class="absolute inset-0 bg-black/35"></div>

        <!-- 弹窗主体 -->
        <div
          class="relative w-full max-w-sm bg-white rounded-[18px] shadow-um overflow-hidden animate-in zoom-in-95 duration-300 border border-warm-100"
        >
          <!-- 关闭按钮 -->
          <button
            v-if="closable"
            @click="close"
            class="absolute top-4 right-4 p-2 rounded-full hover:bg-warm-50 transition-colors z-10"
          >
            <X :size="20" class="text-warm-400" />
          </button>

          <!-- 内容区域 -->
          <div class="p-6 pt-8 text-center">
            <!-- 图标 -->
            <div
              class="w-16 h-16 mx-auto rounded-full flex items-center justify-center mb-5"
              :class="iconConfig.bgColor"
            >
              <component :is="iconConfig.icon" :size="32" :class="iconConfig.iconColor" />
            </div>

            <!-- 标题 -->
            <h3 class="text-lg font-bold text-slate-800 mb-2">{{ title }}</h3>

            <!-- 消息内容 -->
            <p class="text-slate-600 text-sm leading-relaxed mb-6">{{ message }}</p>

            <!-- 按钮区域 -->
            <div class="flex gap-3" :class="showCancel ? 'justify-center' : ''">
              <button
                v-if="showCancel"
                @click="cancel"
                class="flex-1 py-3 px-6 bg-warm-100 text-warm-700 border border-warm-200 rounded-xl font-bold hover:bg-warm-200 transition-colors"
              >
                {{ cancelText }}
              </button>
              <button
                @click="confirm"
                class="flex-1 py-3 px-6 text-white rounded-xl font-bold transition-all active:scale-[0.98]"
                :class="[iconConfig.buttonBg, iconConfig.shadowColor]"
              >
                {{ confirmText }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .relative,
.modal-leave-to .relative {
  transform: scale(0.9);
}
</style>
