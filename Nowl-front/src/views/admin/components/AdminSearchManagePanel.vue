<script setup lang="ts">
const props = defineProps<{
  searchOpLoading: boolean
}>()

const emit = defineEmits<{
  (e: 'sync-goods-full'): void
  (e: 'create-goods-index'): void
  (e: 'sync-errand-full'): void
  (e: 'create-errand-index'): void
}>()
</script>

<template>
  <div class="admin-panel space-y-6">
    <div class="p-6 bg-gradient-to-r from-slate-50 to-warm-50/60 rounded-2xl border border-warm-100">
      <h3 class="text-lg font-bold text-slate-700 mb-2">搜索索引管理</h3>
      <p class="text-sm text-slate-400 mb-6">管理 Elasticsearch 搜索索引，同步数据或重建索引</p>
      <div class="grid grid-cols-2 gap-4">
        <div class="bg-white rounded-xl p-5 border border-warm-100">
          <h4 class="font-bold text-slate-700 mb-1">商品搜索</h4>
          <p class="text-xs text-slate-400 mb-4">同步商品数据到搜索引擎或重建商品索引</p>
          <div class="flex gap-3">
            <button
              @click="emit('sync-goods-full')"
              :disabled="props.searchOpLoading"
              class="px-4 py-2 bg-gradient-to-r from-warm-500 to-warm-600 text-white rounded-xl text-sm font-bold hover:shadow-lg hover:shadow-warm-200 transition-all disabled:opacity-50"
            >
              全量同步
            </button>
            <button
              @click="emit('create-goods-index')"
              :disabled="props.searchOpLoading"
              class="px-4 py-2 bg-white text-warm-600 border border-warm-200 rounded-xl text-sm font-bold hover:bg-warm-50 transition-all disabled:opacity-50"
            >
              重建索引
            </button>
          </div>
        </div>
        <div class="bg-white rounded-xl p-5 border border-warm-100">
          <h4 class="font-bold text-slate-700 mb-1">跑腿搜索</h4>
          <p class="text-xs text-slate-400 mb-4">同步跑腿数据到搜索引擎或重建跑腿索引</p>
          <div class="flex gap-3">
            <button
              @click="emit('sync-errand-full')"
              :disabled="props.searchOpLoading"
              class="px-4 py-2 bg-gradient-to-r from-emerald-500 to-emerald-600 text-white rounded-xl text-sm font-bold hover:shadow-lg hover:shadow-emerald-200 transition-all disabled:opacity-50"
            >
              全量同步
            </button>
            <button
              @click="emit('create-errand-index')"
              :disabled="props.searchOpLoading"
              class="px-4 py-2 bg-white text-emerald-600 border border-emerald-200 rounded-xl text-sm font-bold hover:bg-emerald-50 transition-all disabled:opacity-50"
            >
              重建索引
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

