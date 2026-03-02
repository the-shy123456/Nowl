<script setup lang="ts">
import { computed } from 'vue'
import { Ban, Bell, Search, Unlock, Users } from 'lucide-vue-next'
import { ElInput, ElOption, ElPagination, ElSelect } from 'element-plus'
import type { SchoolInfo, UserInfo } from '@/types'

const props = defineProps<{
  searchKeyword: string
  schoolList: SchoolInfo[]
  campusList: SchoolInfo[]
  filterSchoolCode: string
  filterCampusCode: string
  userAccountStatus: number | '' | undefined
  userAuthStatus: number | '' | undefined
  canSelectSchool: boolean
  canSelectCampus: boolean
  isCampusAdmin: boolean
  effectiveSchoolCode: string
  allUsers: UserInfo[]
  pageNum: number
  pageSize: number
  total: number
}>()

const emit = defineEmits<{
  (e: 'update:searchKeyword', value: string): void
  (e: 'update:filterSchoolCode', value: string): void
  (e: 'update:filterCampusCode', value: string): void
  (e: 'update:userAccountStatus', value: number | '' | undefined): void
  (e: 'update:userAuthStatus', value: number | '' | undefined): void
  (e: 'school-change'): void
  (e: 'campus-change'): void
  (e: 'search'): void
  (e: 'open-broadcast'): void
  (e: 'view-user-detail', userId: number): void
  (e: 'toggle-user', user: UserInfo): void
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

const userAccountStatusModel = computed({
  get: () => props.userAccountStatus,
  set: (value: number | '' | undefined) => emit('update:userAccountStatus', value),
})

const userAuthStatusModel = computed({
  get: () => props.userAuthStatus,
  set: (value: number | '' | undefined) => emit('update:userAuthStatus', value),
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
    <div class="flex flex-wrap gap-4 mb-6 p-5 bg-gradient-to-r from-slate-50 to-orange-50/60 rounded-2xl border border-warm-100">
      <el-input
        v-model="keywordModel"
        placeholder="搜索用户昵称/手机号..."
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
      <el-select v-model="userAccountStatusModel" @change="emit('search')" placeholder="账号状态" class="w-40" clearable>
        <el-option :value="0" label="正常" />
        <el-option :value="1" label="封禁" />
      </el-select>
      <el-select v-model="userAuthStatusModel" @change="emit('search')" placeholder="认证状态" class="w-44" clearable>
        <el-option :value="0" label="未认证" />
        <el-option :value="1" label="待审核" />
        <el-option :value="2" label="已通过" />
        <el-option :value="3" label="已拒绝" />
      </el-select>
      <button @click="emit('search')" class="px-6 py-2.5 bg-gradient-to-r from-warm-500 to-warm-600 text-white rounded-xl text-sm font-bold hover:shadow-lg hover:shadow-warm-200 transition-all">
        筛选
      </button>
      <button @click="emit('open-broadcast')" class="px-6 py-2.5 bg-gradient-to-r from-amber-500 to-orange-500 text-white rounded-xl text-sm font-bold hover:shadow-lg hover:shadow-amber-200 transition-all ml-auto flex items-center gap-1.5">
        <Bell :size="16" />
        系统通知
      </button>
    </div>

    <div v-if="allUsers.length === 0" class="flex flex-col items-center justify-center py-24">
      <div class="w-24 h-24 bg-warm-100 rounded-full flex items-center justify-center mb-4">
        <Users :size="40" class="text-warm-300" />
      </div>
      <p class="text-slate-400 text-lg">暂无用户数据</p>
    </div>
    <div v-else class="overflow-x-auto rounded-2xl border border-warm-100">
      <table class="w-full">
        <thead>
          <tr class="bg-gradient-to-r from-slate-50 to-warm-50/60">
            <th class="text-left py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">用户</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">学校/校区</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">信用分</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">认证状态</th>
            <th class="text-left py-4 px-4 text-xs font-bold text-slate-500 uppercase tracking-wider">账号状态</th>
            <th class="text-right py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-warm-100">
          <tr v-for="user in allUsers" :key="user.userId" class="hover:bg-warm-50/60 transition-colors">
            <td class="py-4 px-6">
              <div class="flex gap-3 items-center">
                <img :src="user.imageUrl || '/avatar-placeholder.svg'" class="w-10 h-10 rounded-full ring-2 ring-warm-100" />
                <div>
                  <p class="font-bold text-slate-700">{{ user.nickName }}</p>
                  <p class="text-xs text-slate-400">ID: {{ user.userId }}</p>
                </div>
              </div>
            </td>
            <td class="py-4 px-4">
              <span class="text-sm text-slate-600">{{ formatSchoolCampus(user.schoolName, user.schoolCode, user.campusName, user.campusCode) }}</span>
            </td>
            <td class="py-4 px-4">
              <div class="flex items-center gap-2">
                <span class="text-lg font-bold text-warm-500">{{ user.creditScore }}</span>
                <div class="w-20 h-1.5 bg-warm-100 rounded-full overflow-hidden">
                  <div
                    class="h-full rounded-full transition-all"
                    :class="{
                      'bg-emerald-500': user.creditScore >= 80,
                      'bg-amber-500': user.creditScore >= 60 && user.creditScore < 80,
                      'bg-red-500': user.creditScore < 60,
                    }"
                    :style="{ width: `${user.creditScore}%` }"
                  ></div>
                </div>
              </div>
            </td>
            <td class="py-4 px-4">
              <span
                class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold"
                :class="{
                  'bg-slate-100 text-slate-500': user.authStatus === 0,
                  'bg-amber-50 text-amber-600': user.authStatus === 1,
                  'bg-emerald-50 text-emerald-600': user.authStatus === 2,
                  'bg-red-50 text-red-600': user.authStatus === 3,
                }"
              >
                {{ user.authStatus === 0 ? '未认证' : user.authStatus === 1 ? '待审核' : user.authStatus === 2 ? '已通过' : '已拒绝' }}
              </span>
            </td>
            <td class="py-4 px-4">
              <span
                class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold"
                :class="user.accountStatus === 0 ? 'bg-emerald-50 text-emerald-600' : 'bg-red-50 text-red-600'"
              >
                <span class="w-1.5 h-1.5 rounded-full" :class="user.accountStatus === 0 ? 'bg-emerald-500' : 'bg-red-500'"></span>
                {{ user.accountStatus === 0 ? '正常' : '封禁' }}
              </span>
            </td>
            <td class="py-4 px-6 text-right">
              <div class="flex items-center gap-2 justify-end">
                <button
                  @click="emit('view-user-detail', user.userId)"
                  class="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-medium bg-white text-warm-500 border border-warm-200 hover:bg-warm-50 transition-all"
                >
                  详情
                </button>
                <button
                  @click="emit('toggle-user', user)"
                  class="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-medium transition-all"
                  :class="user.accountStatus === 0
                    ? 'bg-white text-red-500 border border-red-200 hover:bg-red-50'
                    : 'bg-emerald-500 text-white hover:bg-emerald-600 shadow-sm shadow-emerald-200'"
                >
                  <component :is="user.accountStatus === 0 ? Ban : Unlock" :size="16" />
                  {{ user.accountStatus === 0 ? '封禁' : '解封' }}
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

