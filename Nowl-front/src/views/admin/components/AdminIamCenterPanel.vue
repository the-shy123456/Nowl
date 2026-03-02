<script setup lang="ts">
import { computed } from 'vue'
import { ElInput } from 'element-plus'
import type { AdminScopeBindingItem, IamRoleItem, UserRoleBindingItem } from '@/api/modules/admin'

const props = defineProps<{
  canViewIamRole: boolean
  canViewIamUserRole: boolean
  canManageIamUserRole: boolean
  canViewIamScope: boolean
  canManageIamScope: boolean
  iamRoles: IamRoleItem[]
  iamUserIdInput: string
  iamTargetUserId: number | null
  userRoleBindings: UserRoleBindingItem[]
  adminScopeBindings: AdminScopeBindingItem[]
}>()

const emit = defineEmits<{
  (e: 'update:iamUserIdInput', value: string): void
  (e: 'query-user'): void
  (e: 'grant-role'): void
  (e: 'revoke-role', bindingId: number): void
  (e: 'add-scope'): void
  (e: 'disable-scope', bindingId: number): void
}>()

const iamUserIdModel = computed({
  get: () => props.iamUserIdInput,
  set: (value: string) => emit('update:iamUserIdInput', value),
})
</script>

<template>
  <div class="admin-panel space-y-6">
    <div v-if="!canViewIamRole && !canViewIamUserRole && !canViewIamScope" class="py-16 text-center text-slate-400">
      当前账号暂无IAM权限中心访问权限
    </div>

    <template v-else>
      <div class="grid grid-cols-2 gap-6">
        <div v-if="canViewIamRole" class="rounded-2xl border border-warm-100 p-5 bg-gradient-to-br from-slate-50 to-warm-50/60">
          <h3 class="text-sm font-bold text-slate-700 mb-3">系统角色</h3>
          <div class="space-y-2 max-h-56 overflow-auto">
            <div
              v-for="role in iamRoles"
              :key="role.roleId"
              class="flex items-center justify-between px-3 py-2 rounded-xl bg-white border border-warm-100"
            >
              <div>
                <p class="text-sm font-semibold text-slate-700">{{ role.roleName || role.roleCode }}</p>
                <p class="text-xs text-slate-400">{{ role.roleCode }}</p>
              </div>
              <span class="text-xs text-slate-500">L{{ role.roleLevel }}</span>
            </div>
            <div v-if="iamRoles.length === 0" class="text-sm text-slate-400 py-8 text-center">暂无角色数据</div>
          </div>
        </div>

        <div class="rounded-2xl border border-warm-100 p-5 bg-gradient-to-br from-slate-50 to-warm-50/60" :class="canViewIamRole ? '' : 'col-span-2'">
          <h3 class="text-sm font-bold text-slate-700 mb-3">查询目标管理员</h3>
          <div class="flex gap-3 items-center">
            <el-input v-model="iamUserIdModel" placeholder="输入用户ID" class="w-64" clearable />
            <button @click="emit('query-user')" class="px-4 py-2 bg-gradient-to-r from-warm-500 to-warm-400 text-white rounded-xl text-sm font-semibold hover:shadow-lg hover:shadow-warm-200 transition-all">
              查询
            </button>
          </div>
          <p class="text-xs text-slate-400 mt-3">当前用户：{{ iamTargetUserId || '-' }}</p>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-6">
        <div v-if="canViewIamUserRole" class="rounded-2xl border border-warm-100 p-5 bg-white">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-bold text-slate-700">用户角色绑定</h3>
            <button v-if="canManageIamUserRole" @click="emit('grant-role')" class="px-3 py-1.5 bg-gradient-to-r from-emerald-500 to-emerald-600 text-white rounded-lg text-xs font-semibold hover:shadow-lg hover:shadow-emerald-200 transition-all">
              授予角色
            </button>
          </div>
          <div v-if="userRoleBindings.length === 0" class="text-sm text-slate-400 py-10 text-center">暂无角色绑定</div>
          <div v-else class="space-y-2 max-h-72 overflow-auto">
            <div
              v-for="binding in userRoleBindings"
              :key="binding.id"
              class="flex items-center justify-between p-3 rounded-xl bg-slate-50/80 border border-warm-100"
            >
              <div>
                <p class="text-sm font-semibold text-slate-700">{{ binding.roleName || binding.roleCode }}</p>
                <p class="text-xs text-slate-400">绑定ID: {{ binding.id }}</p>
              </div>
              <button
                v-if="canManageIamUserRole"
                @click="emit('revoke-role', binding.id)"
                class="px-3 py-1.5 bg-white border border-red-200 text-red-500 rounded-lg text-xs font-medium hover:bg-red-50"
              >
                撤销
              </button>
            </div>
          </div>
        </div>

        <div v-if="canViewIamScope" class="rounded-2xl border border-warm-100 p-5 bg-white">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-bold text-slate-700">管理范围绑定</h3>
            <button v-if="canManageIamScope" @click="emit('add-scope')" class="px-3 py-1.5 bg-gradient-to-r from-warm-500 to-warm-400 text-white rounded-lg text-xs font-semibold hover:shadow-lg hover:shadow-warm-200 transition-all">
              新增范围
            </button>
          </div>
          <div v-if="adminScopeBindings.length === 0" class="text-sm text-slate-400 py-10 text-center">暂无范围绑定</div>
          <div v-else class="space-y-2 max-h-72 overflow-auto">
            <div
              v-for="binding in adminScopeBindings"
              :key="binding.bindingId"
              class="flex items-center justify-between p-3 rounded-xl bg-slate-50/80 border border-warm-100"
            >
              <div>
                <p class="text-sm font-semibold text-slate-700">{{ binding.scopeType }}</p>
                <p class="text-xs text-slate-400">{{ binding.schoolCode || '-' }} / {{ binding.campusCode || '-' }}</p>
              </div>
              <button
                v-if="canManageIamScope"
                @click="emit('disable-scope', binding.bindingId)"
                class="px-3 py-1.5 bg-white border border-red-200 text-red-500 rounded-lg text-xs font-medium hover:bg-red-50"
              >
                关闭
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>
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
</style>
