<script setup lang="ts">
import { computed } from 'vue'
import { Bike, Search } from 'lucide-vue-next'
import { ElInput, ElOption, ElPagination, ElSelect } from 'element-plus'
import type { AdminErrandItem } from '@/api/modules/admin'
import type { SchoolInfo } from '@/types'

const props = defineProps<{
  searchKeyword: string
  schoolList: SchoolInfo[]
  campusList: SchoolInfo[]
  filterSchoolCode: string
  filterCampusCode: string
  errandStatusFilter: number | '' | undefined
  errandReviewStatusFilter: number | '' | undefined
  canSelectSchool: boolean
  canSelectCampus: boolean
  isCampusAdmin: boolean
  effectiveSchoolCode: string
  canAuditErrand: boolean
  allErrands: AdminErrandItem[]
  isAuditPending: (taskId: number) => boolean
  pageNum: number
  pageSize: number
  total: number
}>()

const emit = defineEmits<{
  (e: 'update:searchKeyword', value: string): void
  (e: 'update:filterSchoolCode', value: string): void
  (e: 'update:filterCampusCode', value: string): void
  (e: 'update:errandStatusFilter', value: number | '' | undefined): void
  (e: 'update:errandReviewStatusFilter', value: number | '' | undefined): void
  (e: 'school-change'): void
  (e: 'campus-change'): void
  (e: 'search'): void
  (e: 'audit-errand', taskId: number, status: number): void
  (e: 'open-errand-detail', taskId: number): void
  (e: 'page-change', page: number): void
  (e: 'size-change', size: number): void
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

const errandStatusModel = computed({
  get: () => props.errandStatusFilter,
  set: (value: number | '' | undefined) => emit('update:errandStatusFilter', value),
})

const errandReviewStatusModel = computed({
  get: () => props.errandReviewStatusFilter,
  set: (value: number | '' | undefined) => emit('update:errandReviewStatusFilter', value),
})

const formatSchoolCampus = (schoolName?: string, schoolCode?: string, campusName?: string, campusCode?: string) => {
  const school = schoolName || schoolCode || '未知学校'
  const campus = campusName || campusCode || '未知校区'
  return `${school} · ${campus}`
}
</script>

<template>
  <div class="admin-panel">
    <div class="flex flex-wrap gap-4 mb-6 p-5 bg-gradient-to-r from-slate-50 to-emerald-50/50 rounded-2xl border border-warm-100">
      <el-input
        v-model="keywordModel"
        placeholder="搜索任务标题..."
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
      <el-select v-model="errandStatusModel" @change="emit('search')" placeholder="任务状态" class="w-44" clearable>
        <el-option :value="0" label="待接单" />
        <el-option :value="1" label="进行中" />
        <el-option :value="2" label="待确认" />
        <el-option :value="3" label="已完成" />
        <el-option :value="4" label="已取消" />
      </el-select>
      <el-select v-model="errandReviewStatusModel" @change="emit('search')" placeholder="审核状态" class="w-44" clearable>
        <el-option :value="0" label="待审核" />
        <el-option :value="1" label="AI通过" />
        <el-option :value="2" label="人工通过" />
        <el-option :value="4" label="待人工复核" />
        <el-option :value="3" label="已驳回" />
      </el-select>
      <button @click="emit('search')" class="px-6 py-2.5 bg-gradient-to-r from-emerald-500 to-emerald-600 text-white rounded-xl text-sm font-bold hover:shadow-lg hover:shadow-emerald-200 transition-all">
        筛选
      </button>
    </div>
    <div v-if="allErrands.length === 0" class="flex flex-col items-center justify-center py-24">
      <div class="w-24 h-24 bg-warm-100 rounded-full flex items-center justify-center mb-4">
        <Bike :size="40" class="text-warm-300" />
      </div>
      <p class="text-slate-400 text-lg">暂无跑腿任务</p>
    </div>
    <div v-else class="overflow-x-auto rounded-2xl border border-warm-100">
      <table class="w-full">
        <thead>
          <tr class="bg-gradient-to-r from-slate-50 to-warm-50/60">
            <th class="text-left py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">任务ID</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">任务信息</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">发布者</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">接单者</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">学校/校区</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">报酬</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">状态</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">审核</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">时间</th>
            <th class="text-right py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-warm-100">
          <tr v-for="item in allErrands" :key="item.taskId" class="hover:bg-warm-50/60 transition-colors">
            <td class="py-4 px-6 text-slate-500 font-mono text-sm">#{{ item.taskId }}</td>
            <td class="py-4 px-4">
              <div class="max-w-[260px]">
                <div class="font-medium text-slate-700 line-clamp-1">{{ item.title || '无标题任务' }}</div>
                <div class="text-xs text-slate-400 line-clamp-1 mt-1">
                  {{ item.taskContent || item.description || '暂无任务详情' }}
                </div>
              </div>
            </td>
            <td class="py-4 px-4">
              <div class="flex gap-2 items-center">
                <img :src="item.publisherAvatar || '/avatar-placeholder.svg'" class="w-8 h-8 rounded-full ring-2 ring-warm-100" />
                <span class="font-medium text-slate-700">{{ item.publisherName || `用户#${item.publisherId || '-'}` }}</span>
              </div>
            </td>
            <td class="py-4 px-4">
              <div v-if="item.acceptorId" class="flex gap-2 items-center">
                <img :src="item.acceptorAvatar || '/avatar-placeholder.svg'" class="w-8 h-8 rounded-full ring-2 ring-warm-100" />
                <span class="text-slate-600">{{ item.acceptorName || `用户#${item.acceptorId}` }}</span>
              </div>
              <span v-else class="text-warm-300">-</span>
            </td>
            <td class="py-4 px-4 text-sm text-slate-600">
              {{ formatSchoolCampus(item.schoolName, item.schoolCode, item.campusName, item.campusCode) }}
            </td>
            <td class="py-4 px-4">
              <span class="text-lg font-bold text-warm-500">¥{{ item.reward }}</span>
            </td>
            <td class="py-4 px-4">
              <span
                class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-bold"
                :class="{
                  'bg-amber-50 text-amber-600': item.taskStatus === 0,
                  'bg-warm-50 text-warm-600': item.taskStatus === 1,
                  'bg-orange-50 text-orange-600': item.taskStatus === 2,
                  'bg-emerald-50 text-emerald-600': item.taskStatus === 3,
                  'bg-warm-100 text-warm-700': item.taskStatus === 4,
                }"
              >
                {{ item.statusText || ['待接单', '进行中', '待确认', '已完成', '已取消'][item.taskStatus] || '未知' }}
              </span>
            </td>
            <td class="py-4 px-4">
              <div class="flex flex-col items-start gap-1">
                <span
                  class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-bold"
                  :class="{
                    'bg-amber-50 text-amber-600': item.reviewStatus === 0,
                    'bg-blue-50 text-blue-600': item.reviewStatus === 1,
                    'bg-emerald-50 text-emerald-600': item.reviewStatus === 2,
                    'bg-orange-50 text-orange-600': item.reviewStatus === 4,
                    'bg-red-50 text-red-600': item.reviewStatus === 3,
                  }"
                >
                  {{ item.reviewStatusText || (item.reviewStatus === 0 ? '待审核' : item.reviewStatus === 1 ? 'AI通过' : item.reviewStatus === 2 ? '人工通过' : item.reviewStatus === 4 ? '待人工复核' : item.reviewStatus === 3 ? '已驳回' : '未知') }}
                </span>
                <span
                  v-if="item.auditReason && (item.reviewStatus === 3 || item.reviewStatus === 4)"
                  class="text-xs text-slate-500 line-clamp-1 max-w-[220px]"
                  :title="item.auditReason"
                >
                  {{ item.auditReason }}
                </span>
              </div>
            </td>
            <td class="py-4 px-4 text-xs text-slate-400">{{ item.createTime?.slice(0, 16) }}</td>
            <td class="py-4 px-6 text-right">
              <div class="flex items-center justify-end gap-2">
                <button
                  v-if="canAuditErrand && (item.reviewStatus === 0 || item.reviewStatus === 4)"
                  @click="emit('audit-errand', item.taskId, 1)"
                  :disabled="isAuditPending(item.taskId)"
                  class="px-3 py-2 rounded-xl text-sm font-medium bg-emerald-500 text-white hover:bg-emerald-600 transition-colors disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:bg-emerald-500"
                >
                  通过
                </button>
                <button
                  v-if="canAuditErrand && (item.reviewStatus === 0 || item.reviewStatus === 4)"
                  @click="emit('audit-errand', item.taskId, 2)"
                  :disabled="isAuditPending(item.taskId)"
                  class="px-3 py-2 rounded-xl text-sm font-medium bg-red-500 text-white hover:bg-red-600 transition-colors disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:bg-red-500"
                >
                  驳回
                </button>
                <button
                  @click="emit('open-errand-detail', item.taskId)"
                  class="px-4 py-2 rounded-xl text-sm font-medium bg-white text-emerald-600 border border-emerald-200 hover:bg-emerald-50 transition-colors"
                >
                  详情
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
