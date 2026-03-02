<script setup lang="ts">
import { computed } from 'vue'
import { ClipboardList, Search } from 'lucide-vue-next'
import { ElInput, ElOption, ElPagination, ElSelect } from 'element-plus'
import type { AdminOrderItem } from '@/api/modules/admin'
import type { SchoolInfo } from '@/types'

const props = defineProps<{
  searchKeyword: string
  schoolList: SchoolInfo[]
  campusList: SchoolInfo[]
  filterSchoolCode: string
  filterCampusCode: string
  orderStatusFilter: number | '' | undefined
  canSelectSchool: boolean
  canSelectCampus: boolean
  isCampusAdmin: boolean
  effectiveSchoolCode: string
  allOrders: AdminOrderItem[]
  pageNum: number
  pageSize: number
  total: number
}>()

const emit = defineEmits<{
  (e: 'update:searchKeyword', value: string): void
  (e: 'update:filterSchoolCode', value: string): void
  (e: 'update:filterCampusCode', value: string): void
  (e: 'update:orderStatusFilter', value: number | '' | undefined): void
  (e: 'school-change'): void
  (e: 'campus-change'): void
  (e: 'search'): void
  (e: 'page-change', page: number): void
  (e: 'size-change', size: number): void
  (e: 'open-product-detail', productId: number): void
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

const orderStatusModel = computed({
  get: () => props.orderStatusFilter,
  set: (value: number | '' | undefined) => emit('update:orderStatusFilter', value),
})

const orderStatusMap: Record<number, { text: string; color: string; bg: string }> = {
  0: { text: '待支付', color: 'text-amber-600', bg: 'bg-amber-50' },
  1: { text: '待发货', color: 'text-warm-600', bg: 'bg-warm-50' },
  2: { text: '待收货', color: 'text-orange-600', bg: 'bg-orange-50' },
  3: { text: '已完成', color: 'text-emerald-600', bg: 'bg-emerald-50' },
  4: { text: '已取消', color: 'text-warm-700', bg: 'bg-warm-100' },
  5: { text: '已结束', color: 'text-slate-600', bg: 'bg-slate-50' },
}

const formatSchoolCampus = (schoolName?: string, schoolCode?: string, campusName?: string, campusCode?: string) => {
  const school = schoolName || schoolCode || '未知学校'
  const campus = campusName || campusCode || '未知校区'
  return `${school} · ${campus}`
}
</script>

<template>
  <div class="admin-panel">
    <!-- 搜索栏 -->
    <div class="flex flex-wrap gap-4 mb-6 p-5 bg-gradient-to-r from-slate-50 to-warm-50/60 rounded-2xl border border-warm-100">
      <el-input
        v-model="keywordModel"
        placeholder="搜索订单号..."
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
      <el-select v-model="orderStatusModel" @change="emit('search')" placeholder="订单状态" class="w-44" clearable>
        <el-option :value="0" label="待支付" />
        <el-option :value="1" label="待发货" />
        <el-option :value="2" label="待收货" />
        <el-option :value="3" label="已完成" />
        <el-option :value="4" label="已取消" />
        <el-option :value="5" label="已结束" />
      </el-select>
      <button @click="emit('search')" class="px-6 py-2.5 bg-gradient-to-r from-warm-500 to-orange-500 text-white rounded-xl text-sm font-bold hover:shadow-lg hover:shadow-warm-200 transition-all">
        筛选
      </button>
    </div>

    <div v-if="allOrders.length === 0" class="flex flex-col items-center justify-center py-24">
      <div class="w-24 h-24 bg-warm-100 rounded-full flex items-center justify-center mb-4">
        <ClipboardList :size="40" class="text-warm-300" />
      </div>
      <p class="text-slate-400 text-lg">暂无订单数据</p>
    </div>
    <div v-else class="overflow-x-auto rounded-2xl border border-warm-100">
      <table class="w-full">
        <thead>
          <tr class="bg-gradient-to-r from-slate-50 to-warm-50/60">
            <th class="text-left py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">订单号</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">商品</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">买家/卖家</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">学校/校区</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">金额</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">状态</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">时间</th>
            <th class="text-right py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-warm-100">
          <tr v-for="order in allOrders" :key="order.orderId" class="hover:bg-warm-50/60 transition-colors">
            <td class="py-4 px-6">
              <code class="text-xs text-warm-700 bg-warm-100 px-2 py-1 rounded">{{ order.orderNo }}</code>
            </td>
            <td class="py-4 px-4">
              <div class="flex gap-3 items-center">
                <img :src="order.productImage" class="w-12 h-12 rounded-xl object-cover ring-2 ring-warm-100" />
                <span class="font-medium text-slate-800 line-clamp-1 max-w-[120px]">{{ order.productTitle }}</span>
              </div>
            </td>
            <td class="py-4 px-4">
              <div class="text-xs space-y-1">
                <p class="text-slate-600"><span class="text-slate-400">买:</span> {{ order.buyerName }}</p>
                <p class="text-slate-400"><span class="text-slate-400">卖:</span> {{ order.sellerName }}</p>
              </div>
            </td>
            <td class="py-4 px-4 text-sm text-slate-600">
              {{ formatSchoolCampus(order.schoolName, order.schoolCode, order.campusName, order.campusCode) }}
            </td>
            <td class="py-4 px-4">
              <span class="text-lg font-bold text-warm-500">¥{{ order.totalAmount }}</span>
            </td>
            <td class="py-4 px-4">
              <span
                class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-bold"
                :class="[orderStatusMap[order.orderStatus]?.bg, orderStatusMap[order.orderStatus]?.color]"
              >
                {{ orderStatusMap[order.orderStatus]?.text || '未知' }}
              </span>
            </td>
            <td class="py-4 px-4 text-xs text-slate-400">{{ order.createTime?.slice(0, 16) }}</td>
            <td class="py-4 px-6 text-right">
              <button
                v-if="order.productId"
                @click="emit('open-product-detail', order.productId)"
                class="px-4 py-2 rounded-xl text-sm font-medium bg-white text-warm-600 border border-warm-200 hover:bg-warm-50 transition-colors"
              >
                商品详情
              </button>
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
