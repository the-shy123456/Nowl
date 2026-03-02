<script setup lang="ts">
import { computed } from 'vue'
import { CheckCircle, Package, XCircle } from 'lucide-vue-next'
import { ElOption, ElPagination, ElSelect } from 'element-plus'
import type { GoodsVO, SchoolInfo } from '@/types'

const resolveReviewStatusMeta = (status: number | null | undefined) => {
  if (status === 0) {
    return { text: '待审核', cls: 'bg-amber-50 text-amber-600' }
  }
  if (status === 4) {
    return { text: '待人工复核', cls: 'bg-orange-50 text-orange-700' }
  }
  if (status === 1) {
    return { text: 'AI通过', cls: 'bg-blue-50 text-blue-600' }
  }
  if (status === 2) {
    return { text: '人工通过', cls: 'bg-emerald-50 text-emerald-600' }
  }
  if (status === 3) {
    return { text: '违规驳回', cls: 'bg-red-50 text-red-600' }
  }
  return { text: '未知', cls: 'bg-slate-100 text-slate-600' }
}

const props = defineProps<{
  schoolList: SchoolInfo[]
  campusList: SchoolInfo[]
  filterSchoolCode: string
  filterCampusCode: string
  goodsAuditReviewStatus: number | '' | undefined
  canSelectSchool: boolean
  canSelectCampus: boolean
  isCampusAdmin: boolean
  effectiveSchoolCode: string
  pendingGoods: GoodsVO[]
  isAuditPending: (goodsId: number) => boolean
  pageNum: number
  pageSize: number
  total: number
}>()

const emit = defineEmits<{
  (e: 'update:filterSchoolCode', value: string): void
  (e: 'update:filterCampusCode', value: string): void
  (e: 'update:goodsAuditReviewStatus', value: number | '' | undefined): void
  (e: 'school-change'): void
  (e: 'campus-change'): void
  (e: 'search'): void
  (e: 'page-change', page: number): void
  (e: 'size-change', size: number): void
  (e: 'open-product-detail', productId: number): void
  (e: 'audit-goods', goodsId: number, status: number): void
}>()

const schoolCodeModel = computed({
  get: () => props.filterSchoolCode,
  set: (value: string) => emit('update:filterSchoolCode', value),
})

const campusCodeModel = computed({
  get: () => props.filterCampusCode,
  set: (value: string) => emit('update:filterCampusCode', value),
})

const reviewStatusModel = computed({
  get: () => props.goodsAuditReviewStatus,
  set: (value: number | '' | undefined) => emit('update:goodsAuditReviewStatus', value),
})

const formatSchoolCampus = (schoolName?: string, schoolCode?: string, campusName?: string, campusCode?: string) => {
  const school = schoolName || schoolCode || '未知学校'
  const campus = campusName || campusCode || '未知校区'
  return `${school} · ${campus}`
}
</script>

<template>
  <div class="admin-panel">
    <div class="flex gap-4 mb-6 p-5 bg-gradient-to-r from-slate-50 to-warm-50/60 rounded-2xl border border-warm-100">
      <el-select
        v-model="schoolCodeModel"
        @change="emit('school-change')"
        placeholder="学校"
        class="w-52"
        :clearable="canSelectSchool"
        :disabled="!canSelectSchool"
      >
        <el-option v-for="s in schoolList" :key="s.schoolCode" :label="s.schoolName" :value="s.schoolCode" />
      </el-select>
      <el-select
        v-model="campusCodeModel"
        @change="emit('campus-change')"
        placeholder="校区"
        class="w-52"
        :clearable="canSelectCampus && !isCampusAdmin"
        :disabled="!effectiveSchoolCode || !canSelectCampus || isCampusAdmin"
      >
        <el-option v-for="c in campusList" :key="c.campusCode" :label="c.campusName" :value="c.campusCode" />
      </el-select>
      <el-select
        v-model="reviewStatusModel"
        @change="emit('search')"
        placeholder="审核状态"
        class="w-52"
        clearable
      >
        <el-option :value="''" label="全部待审" />
        <el-option :value="0" label="待审核" />
        <el-option :value="4" label="待人工复核" />
      </el-select>
    </div>
    <div v-if="pendingGoods.length === 0" class="flex flex-col items-center justify-center py-24">
      <div class="w-24 h-24 bg-warm-100 rounded-full flex items-center justify-center mb-4">
        <Package :size="40" class="text-warm-300" />
      </div>
      <p class="text-slate-400 text-lg">暂无待审核商品</p>
      <p class="text-warm-300 text-sm mt-1">所有商品都已审核完毕</p>
    </div>
    <div v-else class="overflow-x-auto rounded-2xl border border-warm-100">
      <table class="w-full">
        <thead>
          <tr class="bg-gradient-to-r from-slate-50 to-warm-50/60">
            <th class="text-left py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">商品信息</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">价格</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">卖家</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">学校/校区</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">审核状态</th>
            <th class="text-right py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-warm-100">
          <tr v-for="item in pendingGoods" :key="item.productId" class="hover:bg-warm-50/60 transition-colors">
            <td class="py-4 px-6">
              <div class="flex gap-4 items-center">
                <img :src="item.image" class="w-16 h-16 rounded-xl object-cover ring-2 ring-warm-100" />
                <div>
                  <p class="font-bold text-slate-800">{{ item.title }}</p>
                  <p class="text-xs text-slate-400 line-clamp-1 max-w-[200px] mt-1">
                    {{ item.auditReason || `商品#${item.productId}` }}
                  </p>
                </div>
              </div>
            </td>
            <td class="py-4 px-4">
              <span class="text-lg font-bold text-warm-500">¥{{ item.price }}</span>
            </td>
            <td class="py-4 px-4">
              <div class="text-sm text-slate-600">
                <p>{{ item.sellerName || `用户#${item.sellerId}` }}</p>
                <p class="text-xs text-slate-400 mt-0.5">UID: {{ item.sellerId }}</p>
              </div>
            </td>
            <td class="py-4 px-4 text-sm text-slate-600">
              {{ formatSchoolCampus(item.schoolName, item.schoolCode, item.campusName, item.campusCode) }}
            </td>
            <td class="py-4 px-4">
              <span
                class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-bold"
                :class="resolveReviewStatusMeta(item.reviewStatus).cls"
              >
                {{ resolveReviewStatusMeta(item.reviewStatus).text }}
              </span>
              <span
                v-if="item.auditReason"
                class="ml-2 inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-medium bg-slate-50 text-slate-500 border border-slate-200"
              >
                {{ item.auditReason }}
              </span>
            </td>
            <td class="py-4 px-6">
              <div class="flex justify-end gap-2">
                <button
                  @click="emit('open-product-detail', item.productId)"
                  class="flex items-center gap-1.5 px-4 py-2 bg-white text-warm-600 border border-warm-200 rounded-xl text-sm font-medium hover:bg-warm-50 transition-colors"
                >
                  详情
                </button>
                <button
                  @click="emit('audit-goods', item.productId, 1)"
                  :disabled="isAuditPending(item.productId)"
                  class="flex items-center gap-1.5 px-4 py-2 bg-emerald-500 text-white rounded-xl text-sm font-medium hover:bg-emerald-600 transition-colors shadow-sm shadow-emerald-200 disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:bg-emerald-500"
                >
                  <CheckCircle :size="16" />
                  通过
                </button>
                <button
                  @click="emit('audit-goods', item.productId, 2)"
                  :disabled="isAuditPending(item.productId)"
                  class="flex items-center gap-1.5 px-4 py-2 bg-white text-red-500 border border-red-200 rounded-xl text-sm font-medium hover:bg-red-50 transition-colors disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:bg-white"
                >
                  <XCircle :size="16" />
                  驳回
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
        class="custom-pagination"
      />
    </div>
  </div>
</template>
