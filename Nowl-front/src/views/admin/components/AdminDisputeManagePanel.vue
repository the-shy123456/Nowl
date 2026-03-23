<script setup lang="ts">
import { computed } from 'vue'
import { AlertTriangle, CheckCircle } from 'lucide-vue-next'
import { ElOption, ElPagination, ElSelect } from 'element-plus'
import type { AdminDisputeItem } from '@/api/modules/admin'
import type { SchoolInfo } from '@/types'

const props = defineProps<{
  schoolList: SchoolInfo[]
  campusList: SchoolInfo[]
  filterSchoolCode: string
  filterCampusCode: string
  disputeStatusFilter: number | '' | undefined
  disputeTargetTypeFilter: number | '' | undefined
  canSelectSchool: boolean
  canSelectCampus: boolean
  isCampusAdmin: boolean
  effectiveSchoolCode: string
  disputes: AdminDisputeItem[]
  isHandlePending: (disputeId: number) => boolean
  pageNum: number
  pageSize: number
  total: number
}>()

const emit = defineEmits<{
  (e: 'update:filterSchoolCode', value: string): void
  (e: 'update:filterCampusCode', value: string): void
  (e: 'update:disputeStatusFilter', value: number | '' | undefined): void
  (e: 'update:disputeTargetTypeFilter', value: number | '' | undefined): void
  (e: 'school-change'): void
  (e: 'campus-change'): void
  (e: 'search'): void
  (e: 'page-change', page: number): void
  (e: 'size-change', size: number): void
  (e: 'open-dispute-detail', recordId: number): void
  (e: 'handle-dispute', recordId: number): void
}>()

const schoolCodeModel = computed({
  get: () => props.filterSchoolCode,
  set: (value: string) => emit('update:filterSchoolCode', value),
})

const campusCodeModel = computed({
  get: () => props.filterCampusCode,
  set: (value: string) => emit('update:filterCampusCode', value),
})

const disputeStatusModel = computed({
  get: () => props.disputeStatusFilter,
  set: (value: number | '' | undefined) => emit('update:disputeStatusFilter', value),
})

const disputeTargetTypeModel = computed({
  get: () => props.disputeTargetTypeFilter,
  set: (value: number | '' | undefined) => emit('update:disputeTargetTypeFilter', value),
})

const disputeStatusMap: Record<number, { text: string; color: string; bg: string }> = {
  0: { text: '待处理', color: 'text-amber-600', bg: 'bg-amber-50' },
  1: { text: '处理中', color: 'text-blue-600', bg: 'bg-blue-50' },
  2: { text: '已解决', color: 'text-emerald-600', bg: 'bg-emerald-50' },
  3: { text: '已驳回', color: 'text-red-600', bg: 'bg-red-50' },
  4: { text: '已撤回', color: 'text-slate-500', bg: 'bg-slate-100' },
}

const disputeTargetTypeMap: Record<number, { text: string; color: string; bg: string }> = {
  0: { text: '商品纠纷', color: 'text-blue-600', bg: 'bg-blue-50' },
  1: { text: '跑腿纠纷', color: 'text-orange-700', bg: 'bg-orange-50' },
}

const canHandleDispute = (status?: number) => status === 0 || status === 1

const formatSchoolCampus = (schoolName?: string, schoolCode?: string, campusName?: string, campusCode?: string) => {
  const school = schoolName || schoolCode || '未知学校'
  const campus = campusName || campusCode || '未知校区'
  return `${school} · ${campus}`
}
</script>

<template>
  <div class="admin-panel">
    <div class="flex flex-wrap gap-4 mb-6 p-5 bg-gradient-to-r from-slate-50 to-warm-50/60 rounded-2xl border border-warm-100">
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
      <el-select v-model="disputeStatusModel" @change="emit('search')" placeholder="处理状态" class="w-44" clearable>
        <el-option :value="0" label="待处理" />
        <el-option :value="1" label="处理中" />
        <el-option :value="2" label="已解决" />
        <el-option :value="3" label="已驳回" />
      </el-select>
      <el-select v-model="disputeTargetTypeModel" @change="emit('search')" placeholder="纠纷类型" class="w-44" clearable>
        <el-option :value="0" label="商品纠纷" />
        <el-option :value="1" label="跑腿纠纷" />
      </el-select>
      <button @click="emit('search')" class="px-6 py-2.5 bg-gradient-to-r from-warm-500 to-warm-600 text-white rounded-xl text-sm font-bold hover:shadow-lg hover:shadow-warm-200 transition-all">
        筛选
      </button>
    </div>
    <div v-if="disputes.length === 0" class="flex flex-col items-center justify-center py-24">
      <div class="w-24 h-24 bg-warm-100 rounded-full flex items-center justify-center mb-4">
        <AlertTriangle :size="40" class="text-warm-300" />
      </div>
      <p class="text-slate-400 text-lg">暂无纠纷记录</p>
      <p class="text-warm-300 text-sm mt-1">平台运行良好，无待处理纠纷</p>
    </div>
    <div v-else class="overflow-x-auto rounded-2xl border border-warm-100">
      <table class="w-full">
        <thead>
          <tr class="bg-gradient-to-r from-slate-50 to-warm-50/60">
            <th class="text-left py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">纠纷ID</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">发起人</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">被投诉方</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">类型</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">关联对象</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">学校/校区</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">纠纷内容</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">状态</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">时间</th>
            <th class="text-right py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-warm-100">
          <tr v-for="item in disputes" :key="item.recordId" class="hover:bg-warm-50/60 transition-colors">
            <td class="py-4 px-6 text-slate-500 font-mono text-sm">#{{ item.recordId }}</td>
            <td class="py-4 px-4">
              <div class="flex gap-2 items-center">
                <img :src="item.initiatorAvatar || '/avatar-placeholder.svg'" class="w-8 h-8 rounded-full ring-2 ring-warm-100" />
                <span class="font-medium text-slate-700">{{ item.initiatorName }}</span>
              </div>
            </td>
            <td class="py-4 px-4">
              <div class="flex gap-2 items-center">
                <img :src="item.relatedAvatar || '/avatar-placeholder.svg'" class="w-8 h-8 rounded-full ring-2 ring-warm-100" />
                <span class="text-slate-600">{{ item.relatedName }}</span>
              </div>
            </td>
            <td class="py-4 px-4">
              <span
                class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-bold"
                :class="[disputeTargetTypeMap[item.targetType]?.bg, disputeTargetTypeMap[item.targetType]?.color]"
              >
                {{ disputeTargetTypeMap[item.targetType]?.text || '未知类型' }}
              </span>
            </td>
            <td class="py-4 px-4">
              <code v-if="item.orderNo" class="text-xs text-warm-700 bg-warm-100 px-2 py-1 rounded">{{ item.orderNo }}</code>
              <code v-else-if="item.targetType === 1" class="text-xs text-orange-700 bg-orange-100 px-2 py-1 rounded">跑腿#{{ item.contentId }}</code>
              <span v-else class="text-warm-300">-</span>
            </td>
            <td class="py-4 px-4 text-sm text-slate-600">
              {{ formatSchoolCampus(item.schoolName, item.schoolCode, item.campusName, item.campusCode) }}
            </td>
            <td class="py-4 px-4">
              <p class="text-sm text-slate-600 line-clamp-2 max-w-[200px]">{{ item.content }}</p>
            </td>
            <td class="py-4 px-4">
              <span
                class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-bold"
                :class="[disputeStatusMap[item.handleStatus]?.bg, disputeStatusMap[item.handleStatus]?.color]"
              >
                {{ disputeStatusMap[item.handleStatus]?.text || '未知' }}
              </span>
            </td>
            <td class="py-4 px-4 text-xs text-slate-400">{{ item.createTime?.slice(0, 16) }}</td>
            <td class="py-4 px-6 text-right">
              <div class="flex items-center justify-end gap-2">
                <button
                  v-if="canHandleDispute(item.handleStatus)"
                  @click="emit('handle-dispute', item.recordId)"
                  :disabled="isHandlePending(item.recordId)"
                  class="flex items-center gap-1.5 px-4 py-2 bg-gradient-to-r from-warm-500 to-warm-600 text-white rounded-xl text-sm font-medium hover:shadow-lg hover:shadow-warm-200 transition-all disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  <CheckCircle :size="16" />
                  {{ item.handleStatus === 1 ? '继续处理' : '处理' }}
                </button>
                <button
                  @click="emit('open-dispute-detail', item.recordId)"
                  class="px-4 py-2 rounded-xl text-sm font-medium bg-white text-warm-600 border border-warm-200 hover:bg-warm-50 transition-colors"
                >
                  详情
                </button>
              </div>
              <p v-if="item.handleResult && item.handleStatus !== 0" class="text-xs text-slate-400 mt-2 text-right line-clamp-1">
                {{ item.handleResult }}
              </p>
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
