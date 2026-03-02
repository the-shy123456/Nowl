<script setup lang="ts">
import { computed } from 'vue'
import { Bike, CheckCircle, XCircle } from 'lucide-vue-next'
import { ElOption, ElPagination, ElSelect } from 'element-plus'
import type { SchoolInfo, UserInfo } from '@/types'

const props = defineProps<{
  schoolList: SchoolInfo[]
  campusList: SchoolInfo[]
  filterSchoolCode: string
  filterCampusCode: string
  canSelectSchool: boolean
  canSelectCampus: boolean
  isCampusAdmin: boolean
  effectiveSchoolCode: string
  pendingRunnerUsers: UserInfo[]
  isAuditPending: (userId: number) => boolean
  pageNum: number
  pageSize: number
  total: number
}>()

const emit = defineEmits<{
  (e: 'update:filterSchoolCode', value: string): void
  (e: 'update:filterCampusCode', value: string): void
  (e: 'school-change'): void
  (e: 'campus-change'): void
  (e: 'page-change', page: number): void
  (e: 'size-change', size: number): void
  (e: 'audit-runner', userId: number, status: number): void
}>()

const schoolCodeModel = computed({
  get: () => props.filterSchoolCode,
  set: (value: string) => emit('update:filterSchoolCode', value),
})

const campusCodeModel = computed({
  get: () => props.filterCampusCode,
  set: (value: string) => emit('update:filterCampusCode', value),
})
</script>

<template>
  <div class="admin-panel">
    <!-- 筛选栏 -->
    <div class="flex gap-4 mb-6 p-5 bg-gradient-to-r from-slate-50 to-emerald-50/50 rounded-2xl border border-warm-100">
      <el-select
        v-model="schoolCodeModel"
        @change="emit('school-change')"
        placeholder="选择学校"
        class="w-52"
        :clearable="canSelectSchool"
        :disabled="!canSelectSchool"
      >
        <el-option
          v-for="s in schoolList"
          :key="s.schoolCode"
          :label="s.schoolName"
          :value="s.schoolCode"
        />
      </el-select>

      <el-select
        v-model="campusCodeModel"
        @change="emit('campus-change')"
        placeholder="选择校区"
        class="w-52"
        :clearable="canSelectCampus && !isCampusAdmin"
        :disabled="!effectiveSchoolCode || !canSelectCampus || isCampusAdmin"
      >
        <el-option
          v-for="c in campusList"
          :key="c.campusCode"
          :label="c.campusName"
          :value="c.campusCode"
        />
      </el-select>
    </div>

    <div v-if="pendingRunnerUsers.length === 0" class="flex flex-col items-center justify-center py-24">
      <div class="w-24 h-24 bg-warm-100 rounded-full flex items-center justify-center mb-4">
        <Bike :size="40" class="text-warm-300" />
      </div>
      <p class="text-slate-400 text-lg">暂无待审核跑腿员</p>
      <p class="text-warm-300 text-sm mt-1">所有跑腿员申请都已处理完毕</p>
    </div>
    <div v-else class="overflow-x-auto rounded-2xl border border-warm-100">
      <table class="w-full">
        <thead>
          <tr class="bg-gradient-to-r from-slate-50 to-warm-50/60">
            <th class="text-left py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">用户</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">手机号</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">学校/校区</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">信用分</th>
            <th class="text-right py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-warm-100">
          <tr v-for="user in pendingRunnerUsers" :key="user.userId" class="hover:bg-warm-50/60 transition-colors">
            <td class="py-4 px-6">
              <div class="flex items-center gap-3">
                <img :src="user.imageUrl || '/avatar-placeholder.svg'" class="w-10 h-10 rounded-full ring-2 ring-warm-100" />
                <div>
                  <p class="font-bold text-slate-800">{{ user.nickName }}</p>
                  <p class="text-xs text-slate-400">ID: {{ user.userId }}</p>
                </div>
              </div>
            </td>
            <td class="py-4 px-4 text-sm text-slate-600">{{ user.phone }}</td>
            <td class="py-4 px-4">
              <p class="text-sm font-medium text-slate-700">{{ user.schoolName }}</p>
              <p class="text-xs text-slate-400 mt-0.5">{{ user.campusName }}</p>
            </td>
            <td class="py-4 px-4">
              <div class="flex items-center gap-2">
                <span
                  class="text-lg font-bold"
                  :class="{
                    'text-emerald-500': user.creditScore >= 80,
                    'text-amber-500': user.creditScore >= 60 && user.creditScore < 80,
                    'text-red-500': user.creditScore < 60,
                  }"
                >
                  {{ user.creditScore }}
                </span>
              </div>
            </td>
            <td class="py-4 px-6">
              <div class="flex justify-end gap-2">
                <button
                  @click="emit('audit-runner', user.userId, 1)"
                  :disabled="isAuditPending(user.userId)"
                  class="flex items-center gap-1.5 px-4 py-2 bg-emerald-500 text-white rounded-xl text-sm font-medium hover:bg-emerald-600 transition-colors shadow-sm shadow-emerald-200 disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:bg-emerald-500"
                >
                  <CheckCircle :size="16" />
                  通过
                </button>
                <button
                  @click="emit('audit-runner', user.userId, 0)"
                  :disabled="isAuditPending(user.userId)"
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
      />
    </div>
  </div>
</template>
