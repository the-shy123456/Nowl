<script setup lang="ts">
import { ref, watch } from 'vue'
import { AlertTriangle, X } from 'lucide-vue-next'

const props = withDefaults(defineProps<{
  modelValue: boolean
  title?: string
  message?: string
  placeholder?: string
  confirmText?: string
  cancelText?: string
  inputValue?: string
  inputType?: 'text' | 'textarea'
  closable?: boolean
  errorText?: string
}>(), {
  modelValue: false,
  title: '请输入',
  message: '',
  placeholder: '',
  confirmText: '确定',
  cancelText: '取消',
  inputValue: '',
  inputType: 'text',
  closable: true,
  errorText: '',
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm', value: string): void
  (e: 'cancel'): void
}>()

const visible = ref(props.modelValue)
const input = ref(props.inputValue)

watch(() => props.modelValue, (value) => {
  visible.value = value
  if (value) {
    input.value = props.inputValue
  }
})

watch(visible, (value) => {
  emit('update:modelValue', value)
})

const close = () => {
  if (!props.closable) {
    return
  }
  visible.value = false
  emit('cancel')
}

const cancel = () => {
  visible.value = false
  emit('cancel')
}

const confirm = () => {
  emit('confirm', input.value)
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="visible"
        class="fixed inset-0 z-[9999] flex items-center justify-center p-4"
        @click.self="close"
      >
        <div class="absolute inset-0 bg-black/35"></div>

        <div class="relative w-full max-w-md bg-white rounded-[18px] shadow-um border border-warm-100 overflow-hidden animate-in zoom-in-95 duration-300">
          <button
            v-if="closable"
            type="button"
            @click="close"
            class="absolute top-4 right-4 p-2 rounded-full hover:bg-warm-50 transition-colors z-10"
          >
            <X :size="20" class="text-warm-400" />
          </button>

          <div class="p-6 pt-8">
            <div class="w-14 h-14 mx-auto rounded-full bg-warm-100 text-warm-500 flex items-center justify-center mb-4">
              <AlertTriangle :size="30" />
            </div>

            <h3 class="text-lg font-bold text-slate-800 text-center mb-2">{{ title }}</h3>
            <p v-if="message" class="text-slate-500 text-sm leading-relaxed text-center mb-5">{{ message }}</p>

            <textarea
              v-if="inputType === 'textarea'"
              v-model="input"
              :placeholder="placeholder"
              class="w-full min-h-[120px] p-3 rounded-xl border border-warm-100 bg-white text-sm outline-none focus:ring-2 focus:ring-warm-100 focus:border-warm-200 resize-y"
            />
            <input
              v-else
              v-model="input"
              :placeholder="placeholder"
              class="w-full p-3 rounded-xl border border-warm-100 bg-white text-sm outline-none focus:ring-2 focus:ring-warm-100 focus:border-warm-200"
            />
            <p v-if="errorText" class="mt-2 text-xs text-red-500 pl-1">{{ errorText }}</p>

            <div class="mt-6 flex gap-3">
              <button
                type="button"
                @click="cancel"
                class="flex-1 py-3 px-6 bg-warm-100 text-warm-700 border border-warm-200 rounded-xl font-bold hover:bg-warm-200 transition-colors"
              >
                {{ cancelText }}
              </button>
              <button
                type="button"
                @click="confirm"
                class="flex-1 py-3 px-6 text-white rounded-xl font-bold bg-warm-500 hover:bg-warm-600 transition-colors"
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
