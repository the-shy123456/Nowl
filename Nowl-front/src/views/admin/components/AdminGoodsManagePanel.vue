<script setup lang="ts">
import { computed } from 'vue'
import { ArrowDownToLine, Search, ShoppingBag } from 'lucide-vue-next'
import { ElInput, ElOption, ElPagination, ElSelect } from 'element-plus'
import type { GoodsVO, SchoolInfo } from '@/types'

const props = defineProps<{
  searchKeyword: string
  schoolList: SchoolInfo[]
  campusList: SchoolInfo[]
  filterSchoolCode: string
  filterCampusCode: string
  goodsTradeStatus: number | '' | undefined
  goodsReviewStatus: number | '' | undefined
  canSelectSchool: boolean
  canSelectCampus: boolean
  isCampusAdmin: boolean
  effectiveSchoolCode: string
  allGoods: GoodsVO[]
  pageNum: number
  pageSize: number
  total: number
}>()

const emit = defineEmits<{
  (e: 'update:searchKeyword', value: string): void
  (e: 'update:filterSchoolCode', value: string): void
  (e: 'update:filterCampusCode', value: string): void
  (e: 'update:goodsTradeStatus', value: number | '' | undefined): void
  (e: 'update:goodsReviewStatus', value: number | '' | undefined): void
  (e: 'school-change'): void
  (e: 'campus-change'): void
  (e: 'search'): void
  (e: 'page-change', page: number): void
  (e: 'size-change', size: number): void
  (e: 'open-product-detail', productId: number): void
  (e: 'force-offline', goodsId: number): void
}>()

const keywordModel = computed({
  get: () => props.searchKeyword,
  set: (value: string) => emit('update:searchKeyword', value),
})

const schoolCodeModel = computed({
  get: () => props.filterSchoolCode,
  set: (value: string) => emit('update:filterSchoolCode', value),
})

const campusCodeModel = computed({
  get: () => props.filterCampusCode,
  set: (value: string) => emit('update:filterCampusCode', value),
})

const tradeStatusModel = computed({
  get: () => props.goodsTradeStatus,
  set: (value: number | '' | undefined) => emit('update:goodsTradeStatus', value),
})

const reviewStatusModel = computed({
  get: () => props.goodsReviewStatus,
  set: (value: number | '' | undefined) => emit('update:goodsReviewStatus', value),
})

const formatSchoolCampus = (schoolName?: string, schoolCode?: string, campusName?: string, campusCode?: string) => {
  const school = schoolName || schoolCode || '未知学校'
  const campus = campusName || campusCode || '未知校区'
  return `${school} · ${campus}`
}
</script>

<template>
  <div class="admin-panel">
    <!-- 搜索栏 -->
    <div class="flex flex-wrap gap-4 mb-6 p-5 bg-gradient-to-r from-slate-50 to-warm-50/50 rounded-2xl border border-warm-100">
      <el-input
        v-model="keywordModel"
        placeholder="搜索商品名称..."
        class="w-72"
        clearable
        @keyup.enter="emit('search')"
        @clear="emit('search')"
      >
        <template #prefix>
          <Search :size="16" class="text-slate-400" />
        </template>
      </el-input>
      <el-select
        v-model="schoolCodeModel"
        @change="emit('school-change')"
        placeholder="学校"
        class="w-44"
        :clearable="canSelectSchool"
        :disabled="!canSelectSchool"
      >
        <el-option v-for="s in schoolList" :key="s.schoolCode" :label="s.schoolName" :value="s.schoolCode" />
      </el-select>
      <el-select
        v-model="campusCodeModel"
        @change="emit('campus-change')"
        placeholder="校区"
        class="w-44"
        :clearable="canSelectCampus && !isCampusAdmin"
        :disabled="!effectiveSchoolCode || !canSelectCampus || isCampusAdmin"
      >
        <el-option v-for="c in campusList" :key="c.campusCode" :label="c.campusName" :value="c.campusCode" />
      </el-select>
      <el-select v-model="tradeStatusModel" @change="emit('search')" placeholder="交易状态" class="w-40" clearable>
        <el-option :value="0" label="在售" />
        <el-option :value="1" label="已售出" />
        <el-option :value="2" label="已下架" />
      </el-select>
      <el-select v-model="reviewStatusModel" @change="emit('search')" placeholder="审核状态" class="w-44" clearable>
        <el-option :value="0" label="待审核" />
        <el-option :value="1" label="AI通过" />
        <el-option :value="2" label="人工通过" />
        <el-option :value="4" label="待人工复核" />
        <el-option :value="3" label="违规驳回" />
      </el-select>
      <button @click="emit('search')" class="px-6 py-2.5 bg-gradient-to-r from-warm-500 to-warm-400 text-white rounded-xl text-sm font-bold hover:shadow-lg hover:shadow-warm-200 transition-all">
        筛选
      </button>
    </div>

    <div v-if="allGoods.length === 0" class="flex flex-col items-center justify-center py-24">
      <div class="w-24 h-24 bg-warm-100 rounded-full flex items-center justify-center mb-4">
        <ShoppingBag :size="40" class="text-warm-300" />
      </div>
      <p class="text-slate-400 text-lg">暂无商品数据</p>
    </div>
    <div v-else class="overflow-x-auto rounded-2xl border border-warm-100">
      <table class="w-full">
        <thead>
          <tr class="bg-gradient-to-r from-slate-50 to-warm-50/60">
            <th class="text-left py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">商品</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">价格</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">卖家</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">学校/校区</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">审核/交易</th>
            <th class="text-right py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-warm-100">
          <tr v-for="item in allGoods" :key="item.productId" class="hover:bg-warm-50/60 transition-colors">
            <td class="py-4 px-6">
              <div class="flex gap-4 items-center">
                <img :src="item.image" class="w-14 h-14 rounded-xl object-cover ring-2 ring-warm-100" />
                <div>
                  <p class="font-bold text-slate-800">{{ item.title }}</p>
                  <p class="text-xs text-slate-400 mt-0.5">#{{ item.productId }}</p>
                </div>
              </div>
            </td>
            <td class="py-4 px-4">
              <span class="text-lg font-bold text-warm-500">¥{{ item.price }}</span>
            </td>
            <td class="py-4 px-4 text-sm text-slate-600">
              <p>{{ item.sellerName || `用户#${item.sellerId}` }}</p>
              <p class="text-xs text-slate-400 mt-0.5">UID: {{ item.sellerId }}</p>
            </td>
            <td class="py-4 px-4">
              <span class="text-sm text-slate-600">{{ formatSchoolCampus(item.schoolName, item.schoolCode, item.campusName, item.campusCode) }}</span>
            </td>
            <td class="py-4 px-4 space-x-2">
              <span
                class="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-bold"
                :class="{
                  'bg-amber-50 text-amber-600': item.reviewStatus === 0,
                  'bg-blue-50 text-blue-600': item.reviewStatus === 1,
                  'bg-emerald-50 text-emerald-600': item.reviewStatus === 2,
                  'bg-orange-50 text-orange-700': item.reviewStatus === 4,
                  'bg-red-50 text-red-600': item.reviewStatus === 3,
                }"
              >
                {{ item.reviewStatus === 0 ? '待审' : item.reviewStatus === 1 ? 'AI通过' : item.reviewStatus === 2 ? '人工通过' : item.reviewStatus === 4 ? '待人工复核' : item.reviewStatus === 3 ? '违规驳回' : '未知' }}
              </span>
              <span
                class="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-bold"
                :class="{
                  'bg-emerald-50 text-emerald-600': item.tradeStatus === 0,
                  'bg-warm-50 text-warm-600': item.tradeStatus === 1,
                  'bg-warm-100 text-warm-700': item.tradeStatus === 2,
                }"
              >
                <span class="w-1.5 h-1.5 rounded-full" :class="{
                  'bg-emerald-500': item.tradeStatus === 0,
                  'bg-warm-500': item.tradeStatus === 1,
                  'bg-slate-400': item.tradeStatus === 2,
                }"></span>
                {{ item.tradeStatus === 0 ? '在售' : item.tradeStatus === 1 ? '已售出' : '已下架' }}
              </span>
            </td>
            <td class="py-4 px-6 text-right">
              <div class="flex justify-end gap-2">
                <button
                  @click="emit('open-product-detail', item.productId)"
                  class="flex items-center gap-1.5 px-4 py-2 bg-white text-warm-600 border border-warm-200 rounded-xl text-sm font-medium hover:bg-warm-50 transition-colors"
                >
                  详情
                </button>
                <button
                  v-if="item.tradeStatus === 0"
                  @click="emit('force-offline', item.productId)"
                  class="flex items-center gap-1.5 px-4 py-2 bg-white text-red-500 border border-red-200 rounded-xl text-sm font-medium hover:bg-red-50 transition-colors"
                >
                  <ArrowDownToLine :size="16" />
                  下架
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <!-- 分页 -->
    <div v-if="total > 0" class="mt-6 flex justify-end">
      <el-pagination
        :current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="emit('page-change', $event)"
        @size-change="emit('size-change', $event)"
      />
    </div>
  </div>
</template>
