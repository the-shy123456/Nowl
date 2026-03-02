<script setup lang="ts">
import { computed } from 'vue'
import { ElInput, ElPagination } from 'element-plus'
import type { AdminOperationAuditItem, AuditOverview, LoginTraceItem, PermissionChangeItem } from '@/api/modules/admin'

type AuditTab = 'operations' | 'permission' | 'login'

const props = defineProps<{
  canViewOperationAudit: boolean
  canViewPermissionAudit: boolean
  canViewLoginAudit: boolean
  auditTab: AuditTab
  auditUserIdInput: string
  operationAudits: AdminOperationAuditItem[]
  permissionChanges: PermissionChangeItem[]
  loginTraces: LoginTraceItem[]
  auditOverview: AuditOverview | null
  pageNum: number
  pageSize: number
  total: number
}>()

const emit = defineEmits<{
  (e: 'switch-tab', tab: AuditTab): void
  (e: 'update:auditUserIdInput', value: string): void
  (e: 'search'): void
  (e: 'page-change', page: number): void
  (e: 'size-change', size: number): void
}>()

const auditUserIdModel = computed({
  get: () => props.auditUserIdInput,
  set: (value: string) => emit('update:auditUserIdInput', value),
})
</script>

<template>
  <div class="admin-panel space-y-6">
    <div v-if="auditOverview" class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="rounded-2xl border border-warm-100 bg-gradient-to-br from-slate-50 to-warm-50/60 p-4">
        <p class="text-xs text-slate-400">{{ auditOverview.windowDays }}天操作总数</p>
        <p class="text-2xl font-semibold text-slate-800 mt-1">{{ auditOverview.totalOperations }}</p>
      </div>
      <div class="rounded-2xl border border-red-100 bg-red-50 p-4">
        <p class="text-xs text-red-400">失败操作</p>
        <p class="text-2xl font-semibold text-red-600 mt-1">{{ auditOverview.failedOperations }}</p>
      </div>
      <div class="rounded-2xl border border-amber-100 bg-amber-50 p-4">
        <p class="text-xs text-amber-400">登录失败</p>
        <p class="text-2xl font-semibold text-amber-600 mt-1">{{ auditOverview.loginFailures }}</p>
      </div>
      <div class="rounded-2xl border border-orange-100 bg-orange-50 p-4">
        <p class="text-xs text-orange-400">高风险登录</p>
        <p class="text-2xl font-semibold text-orange-600 mt-1">{{ auditOverview.highRiskLoginCount }}</p>
      </div>
    </div>

    <div class="flex flex-wrap items-center gap-3">
      <button
        v-if="canViewOperationAudit"
        @click="emit('switch-tab', 'operations')"
        class="px-4 py-2 rounded-xl text-sm font-semibold transition-colors"
        :class="auditTab === 'operations' ? 'bg-gradient-to-r from-warm-500 to-warm-400 text-white shadow-lg shadow-warm-200/70' : 'bg-white text-slate-600 border border-warm-100 hover:bg-warm-50'"
      >
        后台操作审计
      </button>
      <button
        v-if="canViewPermissionAudit"
        @click="emit('switch-tab', 'permission')"
        class="px-4 py-2 rounded-xl text-sm font-semibold transition-colors"
        :class="auditTab === 'permission' ? 'bg-gradient-to-r from-warm-500 to-warm-400 text-white shadow-lg shadow-warm-200/70' : 'bg-white text-slate-600 border border-warm-100 hover:bg-warm-50'"
      >
        权限变更审计
      </button>
      <button
        v-if="canViewLoginAudit"
        @click="emit('switch-tab', 'login')"
        class="px-4 py-2 rounded-xl text-sm font-semibold transition-colors"
        :class="auditTab === 'login' ? 'bg-gradient-to-r from-warm-500 to-warm-400 text-white shadow-lg shadow-warm-200/70' : 'bg-white text-slate-600 border border-warm-100 hover:bg-warm-50'"
      >
        登录轨迹审计
      </button>
    </div>

    <div v-if="canViewOperationAudit || canViewPermissionAudit || canViewLoginAudit" class="flex gap-3 items-center p-4 bg-gradient-to-r from-slate-50 to-warm-50/60 rounded-2xl border border-warm-100">
      <el-input v-model="auditUserIdModel" placeholder="可选：按用户ID筛选" class="w-64" clearable />
      <button @click="emit('search')" class="px-5 py-2.5 bg-gradient-to-r from-warm-500 to-warm-400 text-white rounded-xl text-sm font-semibold hover:shadow-lg hover:shadow-warm-200 transition-all">
        查询
      </button>
    </div>

    <div v-if="!canViewOperationAudit && !canViewPermissionAudit && !canViewLoginAudit" class="py-16 text-center text-slate-400">
      当前账号暂无审计中心访问权限
    </div>

    <div v-else-if="auditTab === 'operations' && canViewOperationAudit" class="space-y-3">
      <div v-if="operationAudits.length === 0" class="text-center py-16 text-slate-400">暂无后台操作审计记录</div>
      <div v-else class="overflow-hidden rounded-2xl border border-warm-100">
        <table class="w-full">
          <thead>
            <tr class="bg-slate-50">
              <th class="text-left py-3 px-4 text-xs text-slate-500">ID</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">操作人</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">模块/动作</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">结果</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">耗时</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">时间</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-warm-100">
            <tr v-for="item in operationAudits" :key="item.id">
              <td class="py-3 px-4 text-slate-600 font-mono text-sm">#{{ item.id }}</td>
              <td class="py-3 px-4 text-slate-600">{{ item.operatorId || '-' }}</td>
              <td class="py-3 px-4 text-slate-700">{{ item.module || '-' }} / {{ item.action || '-' }}</td>
              <td class="py-3 px-4 text-slate-600">{{ item.resultStatus || '-' }}</td>
              <td class="py-3 px-4 text-slate-600">{{ item.costMs || 0 }} ms</td>
              <td class="py-3 px-4 text-slate-400 text-sm">{{ item.createTime?.slice(0, 19) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else-if="auditTab === 'permission' && canViewPermissionAudit" class="space-y-3">
      <div v-if="permissionChanges.length === 0" class="text-center py-16 text-slate-400">暂无权限变更记录</div>
      <div v-else class="overflow-hidden rounded-2xl border border-warm-100">
        <table class="w-full">
          <thead>
            <tr class="bg-slate-50">
              <th class="text-left py-3 px-4 text-xs text-slate-500">ID</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">操作人</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">变更类型</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">目标用户</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">原因</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">时间</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-warm-100">
            <tr v-for="item in permissionChanges" :key="item.id">
              <td class="py-3 px-4 text-slate-600 font-mono text-sm">#{{ item.id }}</td>
              <td class="py-3 px-4 text-slate-600">{{ item.operatorId || '-' }}</td>
              <td class="py-3 px-4 text-slate-700">{{ item.changeType || '-' }}</td>
              <td class="py-3 px-4 text-slate-600">{{ item.targetUserId || '-' }}</td>
              <td class="py-3 px-4 text-slate-500">{{ item.reason || '-' }}</td>
              <td class="py-3 px-4 text-slate-400 text-sm">{{ item.createTime?.slice(0, 19) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else-if="auditTab === 'login' && canViewLoginAudit" class="space-y-3">
      <div v-if="loginTraces.length === 0" class="text-center py-16 text-slate-400">暂无登录轨迹</div>
      <div v-else class="overflow-hidden rounded-2xl border border-warm-100">
        <table class="w-full">
          <thead>
            <tr class="bg-slate-50">
              <th class="text-left py-3 px-4 text-xs text-slate-500">ID</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">用户ID</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">手机号</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">IP</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">登录结果</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">风险等级</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">时间</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-warm-100">
            <tr v-for="item in loginTraces" :key="item.id">
              <td class="py-3 px-4 text-slate-600 font-mono text-sm">#{{ item.id }}</td>
              <td class="py-3 px-4 text-slate-600">{{ item.userId || '-' }}</td>
              <td class="py-3 px-4 text-slate-600">{{ item.phone || '-' }}</td>
              <td class="py-3 px-4 text-slate-600">{{ item.ip || '-' }}</td>
              <td class="py-3 px-4 text-slate-700">{{ item.loginResult || '-' }}</td>
              <td class="py-3 px-4 text-slate-700">{{ item.riskLevel || '-' }}</td>
              <td class="py-3 px-4 text-slate-400 text-sm">{{ item.createTime?.slice(0, 19) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else class="py-16 text-center text-slate-400">当前账号无此审计模块权限</div>

    <div v-if="total > 0" class="mt-2 flex justify-end">
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

<style scoped>
.admin-panel :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px rgba(141, 110, 99, 0.25);
}

.admin-panel :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(141, 110, 99, 0.45);
}

.admin-panel :deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 2px rgba(255, 112, 67, 0.24);
}

.admin-panel :deep(.el-pagination) {
  --el-pagination-hover-color: var(--um-primary);
}

.admin-panel :deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, #ff8a65, #ff7043);
  color: #fff;
}
</style>
