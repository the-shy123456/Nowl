<script setup lang="ts">
import type { ComponentPublicInstance } from 'vue'

interface Props {
  isLoadingMore: boolean
  hasMore: boolean
  setTrigger: (element: HTMLElement | null) => void
  loadingText?: string
  endText?: string
  loadingTextClass?: string
  endTextClass?: string
}

const props = withDefaults(defineProps<Props>(), {
  loadingText: '正在加载更多...',
  endText: '已经到底啦～',
  loadingTextClass: 'text-slate-500',
  endTextClass: 'text-slate-400',
})

const assignTriggerElement = (element: Element | ComponentPublicInstance | null) => {
  if (!element) {
    props.setTrigger(null)
    return
  }

  const domElement = ('$el' in element ? element.$el : element) as Element | null
  props.setTrigger(domElement instanceof HTMLElement ? domElement : null)
}
</script>

<template>
  <div v-if="isLoadingMore" class="text-center py-6">
    <div class="inline-block animate-spin rounded-full h-6 w-6 border-2 border-warm-500 border-t-transparent"></div>
    <p :class="['mt-2 text-xs', loadingTextClass]">{{ loadingText }}</p>
  </div>
  <div v-else-if="hasMore" :ref="assignTriggerElement" class="h-8"></div>
  <div v-else :class="['text-center py-6 text-xs', endTextClass]">{{ endText }}</div>
</template>
