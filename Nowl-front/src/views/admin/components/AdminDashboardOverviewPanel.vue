<script setup lang="ts">
import {
  Activity,
  AlertTriangle,
  Bike,
  CheckCircle,
  ChevronRight,
  ClipboardList,
  Clock,
  DollarSign,
  Package,
  PackageCheck,
  ShieldCheck,
  TrendingUp,
  UserCheck,
  Users,
} from 'lucide-vue-next'
import type { DashboardStats } from '@/api/modules/admin'

type DashboardShortcutMenu = 'goods-audit' | 'auth-audit' | 'runner-audit' | 'dispute-manage'

defineProps<{
  animatedStats: DashboardStats
  dashboardStats: DashboardStats | null
}>()

const emit = defineEmits<{
  (e: 'switch-menu', menuId: DashboardShortcutMenu): void
}>()
</script>

<template>
  <div class="space-y-8">
    <!-- 待处理事项 -->
    <div>
      <h3 class="text-lg font-bold text-slate-700 mb-4 flex items-center gap-2">
        <Clock :size="20" class="text-amber-500" />
        待处理事项
      </h3>
      <div class="grid grid-cols-4 gap-6">
        <!-- 待审核商品 -->
        <div
          @click="emit('switch-menu', 'goods-audit')"
          class="group relative overflow-hidden rounded-2xl p-6 cursor-pointer transform hover:scale-[1.02] transition-all duration-300"
        >
          <div class="absolute inset-0 bg-gradient-to-br from-amber-400 via-orange-500 to-orange-600"></div>
          <div class="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgdmlld0JveD0iMCAwIDEwMCAxMDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iNTAiIGN5PSI1MCIgcj0iNDAiIGZpbGw9Im5vbmUiIHN0cm9rZT0id2hpdGUiIHN0cm9rZS1vcGFjaXR5PSIwLjEiIHN0cm9rZS13aWR0aD0iMiIvPjwvc3ZnPg==')] opacity-30"></div>
          <div class="relative">
            <div class="flex items-center justify-between mb-4">
              <div class="w-12 h-12 bg-white/25 rounded-xl flex items-center justify-center">
                <Package :size="24" class="text-white" />
              </div>
              <ChevronRight :size="20" class="text-white/60 group-hover:translate-x-1 transition-transform" />
            </div>
            <p class="text-white/80 text-sm font-medium">待审核商品</p>
            <p class="text-4xl font-bold text-white mt-2">{{ animatedStats.pendingGoods }}</p>
            <div class="mt-4 pt-4 border-t border-white/20">
              <span class="text-white/70 text-xs flex items-center gap-1">
                <Activity :size="12" />
                点击前往处理
              </span>
            </div>
          </div>
        </div>

        <!-- 待认证用户 -->
        <div
          @click="emit('switch-menu', 'auth-audit')"
          class="group relative overflow-hidden rounded-2xl p-6 cursor-pointer transform hover:scale-[1.02] transition-all duration-300"
        >
          <div class="absolute inset-0 bg-gradient-to-br from-warm-500 to-warm-400"></div>
          <div class="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgdmlld0JveD0iMCAwIDEwMCAxMDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iNTAiIGN5PSI1MCIgcj0iNDAiIGZpbGw9Im5vbmUiIHN0cm9rZT0id2hpdGUiIHN0cm9rZS1vcGFjaXR5PSIwLjEiIHN0cm9rZS13aWR0aD0iMiIvPjwvc3ZnPg==')] opacity-30"></div>
          <div class="relative">
            <div class="flex items-center justify-between mb-4">
              <div class="w-12 h-12 bg-white/25 rounded-xl flex items-center justify-center">
                <UserCheck :size="24" class="text-white" />
              </div>
              <ChevronRight :size="20" class="text-white/60 group-hover:translate-x-1 transition-transform" />
            </div>
            <p class="text-white/80 text-sm font-medium">待认证用户</p>
            <p class="text-4xl font-bold text-white mt-2">{{ animatedStats.pendingAuth }}</p>
            <div class="mt-4 pt-4 border-t border-white/20">
              <span class="text-white/70 text-xs flex items-center gap-1">
                <Activity :size="12" />
                点击前往处理
              </span>
            </div>
          </div>
        </div>

        <!-- 待审核跑腿员 -->
        <div
          @click="emit('switch-menu', 'runner-audit')"
          class="group relative overflow-hidden rounded-2xl p-6 cursor-pointer transform hover:scale-[1.02] transition-all duration-300"
        >
          <div class="absolute inset-0 bg-gradient-to-br from-emerald-400 via-emerald-500 to-emerald-600"></div>
          <div class="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgdmlld0JveD0iMCAwIDEwMCAxMDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iNTAiIGN5PSI1MCIgcj0iNDAiIGZpbGw9Im5vbmUiIHN0cm9rZT0id2hpdGUiIHN0cm9rZS1vcGFjaXR5PSIwLjEiIHN0cm9rZS13aWR0aD0iMiIvPjwvc3ZnPg==')] opacity-30"></div>
          <div class="relative">
            <div class="flex items-center justify-between mb-4">
              <div class="w-12 h-12 bg-white/25 rounded-xl flex items-center justify-center">
                <Bike :size="24" class="text-white" />
              </div>
              <ChevronRight :size="20" class="text-white/60 group-hover:translate-x-1 transition-transform" />
            </div>
            <p class="text-white/80 text-sm font-medium">待审核跑腿员</p>
            <p class="text-4xl font-bold text-white mt-2">{{ animatedStats.pendingRunners }}</p>
            <div class="mt-4 pt-4 border-t border-white/20">
              <span class="text-white/70 text-xs flex items-center gap-1">
                <Activity :size="12" />
                点击前往处理
              </span>
            </div>
          </div>
        </div>

        <!-- 待处理纠纷 -->
        <div
          @click="emit('switch-menu', 'dispute-manage')"
          class="group relative overflow-hidden rounded-2xl p-6 cursor-pointer transform hover:scale-[1.02] transition-all duration-300"
        >
          <div class="absolute inset-0 bg-gradient-to-br from-warm-400 to-warm-500"></div>
          <div class="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgdmlld0JveD0iMCAwIDEwMCAxMDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iNTAiIGN5PSI1MCIgcj0iNDAiIGZpbGw9Im5vbmUiIHN0cm9rZT0id2hpdGUiIHN0cm9rZS1vcGFjaXR5PSIwLjEiIHN0cm9rZS13aWR0aD0iMiIvPjwvc3ZnPg==')] opacity-30"></div>
          <div class="relative">
            <div class="flex items-center justify-between mb-4">
              <div class="w-12 h-12 bg-white/25 rounded-xl flex items-center justify-center">
                <AlertTriangle :size="24" class="text-white" />
              </div>
              <ChevronRight :size="20" class="text-white/60 group-hover:translate-x-1 transition-transform" />
            </div>
            <p class="text-white/80 text-sm font-medium">待处理纠纷</p>
            <p class="text-4xl font-bold text-white mt-2">{{ animatedStats.pendingDisputes }}</p>
            <div class="mt-4 pt-4 border-t border-white/20">
              <span class="text-white/70 text-xs flex items-center gap-1">
                <Activity :size="12" />
                点击前往处理
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 用户统计 -->
    <div class="bg-gradient-to-br from-slate-50 to-orange-50/60 rounded-2xl p-6 border border-warm-100">
      <h3 class="font-bold text-slate-700 mb-5 flex items-center gap-2">
        <div class="w-8 h-8 bg-warm-100 rounded-lg flex items-center justify-center">
          <Users :size="16" class="text-warm-600" />
        </div>
        用户数据
      </h3>
      <div class="grid grid-cols-4 gap-4">
        <div class="bg-white rounded-xl p-5 border border-warm-100 hover:shadow-lg hover:shadow-warm-100/60 transition-all duration-300 group">
          <div class="flex items-center justify-between">
            <span class="text-xs text-slate-400 font-medium">总用户数</span>
            <Users :size="14" class="text-slate-300 group-hover:text-warm-400 transition-colors" />
          </div>
          <p class="text-3xl font-bold text-slate-800 mt-3">{{ animatedStats.totalUsers }}</p>
          <div class="mt-3 h-1.5 bg-warm-100 rounded-full overflow-hidden">
            <div class="h-full bg-gradient-to-r from-warm-400 to-warm-600 rounded-full" style="width: 100%"></div>
          </div>
        </div>
        <div class="bg-white rounded-xl p-5 border border-warm-100 hover:shadow-lg hover:shadow-emerald-100/50 transition-all duration-300 group">
          <div class="flex items-center justify-between">
            <span class="text-xs text-slate-400 font-medium">今日新增</span>
            <TrendingUp :size="14" class="text-slate-300 group-hover:text-emerald-400 transition-colors" />
          </div>
          <p class="text-3xl font-bold text-emerald-500 mt-3">+{{ animatedStats.todayNewUsers }}</p>
          <div class="mt-3 flex items-center gap-1">
            <span class="text-xs text-emerald-500 bg-emerald-50 px-2 py-0.5 rounded-full">↑ 活跃</span>
          </div>
        </div>
        <div class="bg-white rounded-xl p-5 border border-warm-100 hover:shadow-lg hover:shadow-warm-100/60 transition-all duration-300 group">
          <div class="flex items-center justify-between">
            <span class="text-xs text-slate-400 font-medium">已认证用户</span>
            <ShieldCheck :size="14" class="text-slate-300 group-hover:text-warm-400 transition-colors" />
          </div>
          <p class="text-3xl font-bold text-warm-500 mt-3">{{ animatedStats.verifiedUsers }}</p>
          <div class="mt-3 h-1.5 bg-warm-100 rounded-full overflow-hidden">
            <div
              class="h-full bg-gradient-to-r from-warm-400 to-warm-600 rounded-full transition-all duration-500"
              :style="{ width: dashboardStats?.totalUsers ? `${(dashboardStats.verifiedUsers || 0) / dashboardStats.totalUsers * 100}%` : '0%' }"
            ></div>
          </div>
        </div>
        <div class="bg-white rounded-xl p-5 border border-warm-100 hover:shadow-lg hover:shadow-orange-100/50 transition-all duration-300 group">
          <div class="flex items-center justify-between">
            <span class="text-xs text-slate-400 font-medium">认证率</span>
            <Activity :size="14" class="text-slate-300 group-hover:text-orange-400 transition-colors" />
          </div>
          <p class="text-3xl font-bold text-orange-500 mt-3">
            {{ dashboardStats?.totalUsers ? Math.round((dashboardStats.verifiedUsers || 0) / dashboardStats.totalUsers * 100) : 0 }}%
          </p>
          <div class="mt-3">
            <span class="text-xs text-orange-500 bg-orange-50 px-2 py-0.5 rounded-full">认证比例</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 商品统计 -->
    <div class="bg-gradient-to-br from-slate-50 to-warm-50/50 rounded-2xl p-6 border border-warm-100">
      <h3 class="font-bold text-slate-700 mb-5 flex items-center gap-2">
        <div class="w-8 h-8 bg-warm-100 rounded-lg flex items-center justify-center">
          <Package :size="16" class="text-warm-600" />
        </div>
        商品数据
      </h3>
      <div class="grid grid-cols-4 gap-4">
        <div class="bg-white rounded-xl p-5 border border-warm-100 hover:shadow-lg hover:shadow-warm-100/50 transition-all duration-300 group">
          <div class="flex items-center justify-between">
            <span class="text-xs text-slate-400 font-medium">总商品数</span>
            <Package :size="14" class="text-slate-300 group-hover:text-warm-400 transition-colors" />
          </div>
          <p class="text-3xl font-bold text-slate-800 mt-3">{{ animatedStats.totalGoods }}</p>
          <div class="mt-3 h-1.5 bg-warm-100 rounded-full overflow-hidden">
            <div class="h-full bg-gradient-to-r from-warm-400 to-warm-500 rounded-full" style="width: 100%"></div>
          </div>
        </div>
        <div class="bg-white rounded-xl p-5 border border-warm-100 hover:shadow-lg hover:shadow-emerald-100/50 transition-all duration-300 group">
          <div class="flex items-center justify-between">
            <span class="text-xs text-slate-400 font-medium">在售商品</span>
            <PackageCheck :size="14" class="text-slate-300 group-hover:text-emerald-400 transition-colors" />
          </div>
          <p class="text-3xl font-bold text-emerald-500 mt-3">{{ animatedStats.onSaleGoods }}</p>
          <div class="mt-3 flex items-center gap-1">
            <span class="text-xs text-emerald-500 bg-emerald-50 px-2 py-0.5 rounded-full">在售中</span>
          </div>
        </div>
        <div class="bg-white rounded-xl p-5 border border-warm-100 hover:shadow-lg hover:shadow-warm-100/60 transition-all duration-300 group">
          <div class="flex items-center justify-between">
            <span class="text-xs text-slate-400 font-medium">今日新增</span>
            <TrendingUp :size="14" class="text-slate-300 group-hover:text-warm-400 transition-colors" />
          </div>
          <p class="text-3xl font-bold text-warm-500 mt-3">+{{ animatedStats.todayNewGoods }}</p>
          <div class="mt-3 flex items-center gap-1">
            <span class="text-xs text-warm-500 bg-warm-50 px-2 py-0.5 rounded-full">↑ 新发布</span>
          </div>
        </div>
        <div class="bg-white rounded-xl p-5 border border-warm-100 hover:shadow-lg hover:shadow-amber-100/50 transition-all duration-300 group">
          <div class="flex items-center justify-between">
            <span class="text-xs text-slate-400 font-medium">待审核</span>
            <Clock :size="14" class="text-slate-300 group-hover:text-amber-400 transition-colors" />
          </div>
          <p class="text-3xl font-bold text-amber-500 mt-3">{{ animatedStats.pendingGoods }}</p>
          <div class="mt-3 flex items-center gap-1">
            <span class="text-xs text-amber-500 bg-amber-50 px-2 py-0.5 rounded-full">待处理</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 订单与交易统计 -->
    <div class="grid grid-cols-5 gap-6">
      <!-- 订单数据 -->
      <div class="col-span-3 bg-gradient-to-br from-slate-50 to-warm-50/60 rounded-2xl p-6 border border-warm-100">
        <h3 class="font-bold text-slate-700 mb-5 flex items-center gap-2">
          <div class="w-8 h-8 bg-warm-100 rounded-lg flex items-center justify-center">
            <ClipboardList :size="16" class="text-warm-600" />
          </div>
          订单数据
        </h3>
        <div class="grid grid-cols-4 gap-4">
          <div class="bg-white rounded-xl p-4 border border-warm-100 hover:shadow-md transition-all">
            <span class="text-xs text-slate-400">总订单数</span>
            <p class="text-2xl font-bold text-slate-800 mt-2">{{ animatedStats.totalOrders }}</p>
          </div>
          <div class="bg-white rounded-xl p-4 border border-warm-100 hover:shadow-md transition-all">
            <span class="text-xs text-slate-400">今日订单</span>
            <p class="text-2xl font-bold text-emerald-500 mt-2">+{{ animatedStats.todayOrders }}</p>
          </div>
          <div class="bg-white rounded-xl p-4 border border-warm-100 hover:shadow-md transition-all">
            <span class="text-xs text-slate-400">已完成</span>
            <p class="text-2xl font-bold text-warm-500 mt-2">{{ animatedStats.completedOrders }}</p>
          </div>
          <div class="bg-white rounded-xl p-4 border border-warm-100 hover:shadow-md transition-all">
            <span class="text-xs text-slate-400">完成率</span>
            <p class="text-2xl font-bold text-orange-500 mt-2">
              {{ dashboardStats?.totalOrders ? Math.round((dashboardStats.completedOrders || 0) / dashboardStats.totalOrders * 100) : 0 }}%
            </p>
          </div>
        </div>
      </div>

      <!-- 交易金额 -->
      <div class="col-span-2 relative overflow-hidden rounded-2xl p-6">
        <div class="absolute inset-0 bg-gradient-to-br from-warm-500 via-orange-500 to-warm-700"></div>
        <div class="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgdmlld0JveD0iMCAwIDIwMCAyMDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJnIiB4MT0iMCUiIHkxPSIwJSIgeDI9IjEwMCUiIHkyPSIxMDAlIj48c3RvcCBvZmZzZXQ9IjAlIiBzdG9wLWNvbG9yPSJ3aGl0ZSIgc3RvcC1vcGFjaXR5PSIwLjEiLz48c3RvcCBvZmZzZXQ9IjEwMCUiIHN0b3AtY29sb3I9IndoaXRlIiBzdG9wLW9wYWNpdHk9IjAiLz48L2xpbmVhckdyYWRpZW50PjwvZGVmcz48Y2lyY2xlIGN4PSIxNTAiIGN5PSI1MCIgcj0iMTAwIiBmaWxsPSJ1cmwoI2cpIi8+PC9zdmc+')] opacity-50"></div>
        <div class="relative">
          <h3 class="font-bold text-white/90 mb-5 flex items-center gap-2">
            <div class="w-8 h-8 bg-white/25 rounded-lg flex items-center justify-center">
              <DollarSign :size="16" class="text-white" />
            </div>
            交易金额
          </h3>
          <div class="space-y-5">
            <div>
              <p class="text-white/70 text-sm">累计交易额</p>
              <p class="text-4xl font-bold text-white mt-1">
                ¥{{ (animatedStats.totalAmount || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}
              </p>
            </div>
            <div class="pt-4 border-t border-white/20">
              <p class="text-white/70 text-sm">今日交易额</p>
              <p class="text-2xl font-bold text-white mt-1">
                ¥{{ (animatedStats.todayAmount || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 跑腿统计 -->
    <div class="bg-gradient-to-br from-slate-50 to-emerald-50/50 rounded-2xl p-6 border border-warm-100">
      <h3 class="font-bold text-slate-700 mb-5 flex items-center gap-2">
        <div class="w-8 h-8 bg-emerald-100 rounded-lg flex items-center justify-center">
          <Bike :size="16" class="text-emerald-600" />
        </div>
        跑腿数据
      </h3>
      <div class="grid grid-cols-3 gap-4">
        <div class="bg-white rounded-xl p-5 border border-warm-100 hover:shadow-lg hover:shadow-emerald-100/50 transition-all duration-300">
          <div class="flex items-center justify-between">
            <span class="text-xs text-slate-400 font-medium">总跑腿任务</span>
            <Bike :size="14" class="text-slate-300" />
          </div>
          <p class="text-3xl font-bold text-slate-800 mt-3">{{ animatedStats.totalErrands }}</p>
        </div>
        <div class="bg-white rounded-xl p-5 border border-warm-100 hover:shadow-lg hover:shadow-warm-100/60 transition-all duration-300">
          <div class="flex items-center justify-between">
            <span class="text-xs text-slate-400 font-medium">进行中</span>
            <Activity :size="14" class="text-warm-400 animate-pulse" />
          </div>
          <p class="text-3xl font-bold text-warm-500 mt-3">{{ animatedStats.activeErrands }}</p>
        </div>
        <div class="bg-white rounded-xl p-5 border border-warm-100 hover:shadow-lg hover:shadow-emerald-100/50 transition-all duration-300">
          <div class="flex items-center justify-between">
            <span class="text-xs text-slate-400 font-medium">已完成</span>
            <CheckCircle :size="14" class="text-emerald-400" />
          </div>
          <p class="text-3xl font-bold text-emerald-500 mt-3">{{ (animatedStats.totalErrands || 0) - (animatedStats.activeErrands || 0) }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

