<script setup lang="ts">
import { computed } from 'vue'
import { ElInput, ElPagination } from 'element-plus'
import type {
  RiskCaseItem,
  RiskEventItem,
  RiskRuleItem,
  RiskSubjectListItem,
  UserBehaviorControlItem,
} from '@/api/modules/admin'

type RiskTab = 'mode' | 'events' | 'cases' | 'rules' | 'behavior' | 'blacklist' | 'whitelist'

const props = defineProps<{
  canViewRiskMode: boolean
  canManageRiskMode: boolean
  canViewRiskEvents: boolean
  canHandleRiskCase: boolean
  canManageRiskRule: boolean
  canViewBehaviorControl: boolean
  canManageBehaviorControl: boolean
  canViewRiskList: boolean
  canManageRiskList: boolean
  riskTab: RiskTab
  riskMode: string
  riskEventTypeFilter: string
  riskDecisionActionFilter: string
  riskLevelFilter: string
  riskCaseStatusFilter: string
  riskStartTimeFilter: string
  riskEndTimeFilter: string
  riskSubjectTypeFilter: string
  riskSubjectIdFilter: string
  behaviorUserIdInput: string
  riskEvents: RiskEventItem[]
  riskCases: RiskCaseItem[]
  riskRules: RiskRuleItem[]
  behaviorControls: UserBehaviorControlItem[]
  blacklistItems: RiskSubjectListItem[]
  whitelistItems: RiskSubjectListItem[]
  pageNum: number
  pageSize: number
  total: number
}>()

const emit = defineEmits<{
  (e: 'switch-risk-tab', tab: RiskTab): void
  (e: 'risk-search'): void
  (e: 'risk-mode-update'): void
  (e: 'behavior-user-query'): void
  (e: 'update:riskEventTypeFilter', value: string): void
  (e: 'update:riskDecisionActionFilter', value: string): void
  (e: 'update:riskLevelFilter', value: string): void
  (e: 'update:riskCaseStatusFilter', value: string): void
  (e: 'update:riskStartTimeFilter', value: string): void
  (e: 'update:riskEndTimeFilter', value: string): void
  (e: 'update:riskSubjectTypeFilter', value: string): void
  (e: 'update:riskSubjectIdFilter', value: string): void
  (e: 'update:behaviorUserIdInput', value: string): void
  (e: 'risk-case-process', caseId: number): void
  (e: 'risk-rule-toggle', rule: RiskRuleItem): void
  (e: 'risk-rule-create'): void
  (e: 'behavior-control-add'): void
  (e: 'behavior-control-disable', controlId: number): void
  (e: 'blacklist-add'): void
  (e: 'blacklist-toggle', item: RiskSubjectListItem): void
  (e: 'whitelist-add'): void
  (e: 'whitelist-toggle', item: RiskSubjectListItem): void
  (e: 'page-change', page: number): void
  (e: 'size-change', size: number): void
}>()

const riskEventTypeModel = computed({
  get: () => props.riskEventTypeFilter,
  set: (value: string) => emit('update:riskEventTypeFilter', value),
})

const riskDecisionActionModel = computed({
  get: () => props.riskDecisionActionFilter,
  set: (value: string) => emit('update:riskDecisionActionFilter', value),
})

const riskLevelModel = computed({
  get: () => props.riskLevelFilter,
  set: (value: string) => emit('update:riskLevelFilter', value),
})

const riskCaseStatusModel = computed({
  get: () => props.riskCaseStatusFilter,
  set: (value: string) => emit('update:riskCaseStatusFilter', value),
})

const riskStartTimeModel = computed({
  get: () => props.riskStartTimeFilter,
  set: (value: string) => emit('update:riskStartTimeFilter', value),
})

const riskEndTimeModel = computed({
  get: () => props.riskEndTimeFilter,
  set: (value: string) => emit('update:riskEndTimeFilter', value),
})

const riskSubjectTypeModel = computed({
  get: () => props.riskSubjectTypeFilter,
  set: (value: string) => emit('update:riskSubjectTypeFilter', value),
})

const riskSubjectIdModel = computed({
  get: () => props.riskSubjectIdFilter,
  set: (value: string) => emit('update:riskSubjectIdFilter', value),
})

const behaviorUserIdModel = computed({
  get: () => props.behaviorUserIdInput,
  set: (value: string) => emit('update:behaviorUserIdInput', value),
})

const modeLabelMap: Record<string, string> = {
  OFF: '关闭风控',
  BASIC: '基础风控',
  FULL: '完整风控',
}
</script>

<template>
  <div class="admin-panel space-y-6">
    <div class="flex flex-wrap items-center gap-3">
      <button
        v-if="canViewRiskMode"
        @click="emit('switch-risk-tab', 'mode')"
        class="px-4 py-2 rounded-xl text-sm font-semibold transition-colors"
        :class="riskTab === 'mode' ? 'bg-gradient-to-r from-warm-500 to-warm-400 text-white shadow-lg shadow-warm-200/70' : 'bg-white text-slate-600 border border-warm-100 hover:bg-warm-50'"
      >
        风控模式
      </button>
      <button
        v-if="canViewRiskList"
        @click="emit('switch-risk-tab', 'blacklist')"
        class="px-4 py-2 rounded-xl text-sm font-semibold transition-colors"
        :class="riskTab === 'blacklist' ? 'bg-gradient-to-r from-warm-500 to-warm-400 text-white shadow-lg shadow-warm-200/70' : 'bg-white text-slate-600 border border-warm-100 hover:bg-warm-50'"
      >
        黑名单
      </button>
      <button
        v-if="canViewRiskList"
        @click="emit('switch-risk-tab', 'whitelist')"
        class="px-4 py-2 rounded-xl text-sm font-semibold transition-colors"
        :class="riskTab === 'whitelist' ? 'bg-gradient-to-r from-warm-500 to-warm-400 text-white shadow-lg shadow-warm-200/70' : 'bg-white text-slate-600 border border-warm-100 hover:bg-warm-50'"
      >
        白名单
      </button>
      <button
        v-if="canViewRiskEvents"
        @click="emit('switch-risk-tab', 'events')"
        class="px-4 py-2 rounded-xl text-sm font-semibold transition-colors"
        :class="riskTab === 'events' ? 'bg-gradient-to-r from-warm-500 to-warm-400 text-white shadow-lg shadow-warm-200/70' : 'bg-white text-slate-600 border border-warm-100 hover:bg-warm-50'"
      >
        风险事件
      </button>
      <button
        v-if="canHandleRiskCase"
        @click="emit('switch-risk-tab', 'cases')"
        class="px-4 py-2 rounded-xl text-sm font-semibold transition-colors"
        :class="riskTab === 'cases' ? 'bg-gradient-to-r from-warm-500 to-warm-400 text-white shadow-lg shadow-warm-200/70' : 'bg-white text-slate-600 border border-warm-100 hover:bg-warm-50'"
      >
        风险工单
      </button>
      <button
        v-if="canManageRiskRule"
        @click="emit('switch-risk-tab', 'rules')"
        class="px-4 py-2 rounded-xl text-sm font-semibold transition-colors"
        :class="riskTab === 'rules' ? 'bg-gradient-to-r from-warm-500 to-warm-400 text-white shadow-lg shadow-warm-200/70' : 'bg-white text-slate-600 border border-warm-100 hover:bg-warm-50'"
      >
        风险规则
      </button>
      <button
        v-if="canViewBehaviorControl"
        @click="emit('switch-risk-tab', 'behavior')"
        class="px-4 py-2 rounded-xl text-sm font-semibold transition-colors"
        :class="riskTab === 'behavior' ? 'bg-gradient-to-r from-warm-500 to-warm-400 text-white shadow-lg shadow-warm-200/70' : 'bg-white text-slate-600 border border-warm-100 hover:bg-warm-50'"
      >
        行为管控
      </button>
    </div>

    <div
      v-if="!canViewRiskMode && !canViewRiskList && !canViewRiskEvents && !canHandleRiskCase && !canManageRiskRule && !canViewBehaviorControl"
      class="py-16 text-center text-slate-400"
    >
      当前账号暂无风控中心权限
    </div>

    <div v-else-if="riskTab === 'mode' && canViewRiskMode" class="space-y-4">
      <div class="rounded-2xl border border-warm-100 bg-gradient-to-r from-slate-50 to-warm-50/60 p-6 flex items-center justify-between gap-4">
        <div>
          <p class="text-xs font-semibold tracking-wide text-slate-400 uppercase">当前风控模式</p>
          <p class="mt-2 text-2xl font-bold text-slate-700">{{ modeLabelMap[riskMode] || riskMode }}</p>
          <p class="mt-1 text-sm text-slate-500">OFF 直接放行，BASIC 仅人工管控与黑白名单，FULL 启用完整实时风控。</p>
        </div>
        <button
          v-if="canManageRiskMode"
          @click="emit('risk-mode-update')"
          class="px-5 py-2.5 bg-gradient-to-r from-warm-500 to-warm-400 text-white rounded-xl text-sm font-semibold hover:shadow-lg hover:shadow-warm-200 transition-all"
        >
          更新模式
        </button>
      </div>
    </div>

    <div v-else-if="riskTab === 'blacklist' && canViewRiskList" class="space-y-4">
      <div class="flex flex-wrap gap-3 items-center p-4 bg-gradient-to-r from-slate-50 to-warm-50/60 rounded-2xl border border-warm-100">
        <el-input v-model="riskSubjectTypeModel" placeholder="按主体类型筛选（USER / IP / DEVICE）" class="w-72" clearable />
        <el-input v-model="riskSubjectIdModel" placeholder="按主体标识筛选" class="w-72" clearable />
        <button @click="emit('risk-search')" class="px-5 py-2.5 bg-gradient-to-r from-warm-500 to-warm-400 text-white rounded-xl text-sm font-semibold hover:shadow-lg hover:shadow-warm-200 transition-all">
          查询
        </button>
        <button v-if="canManageRiskList" @click="emit('blacklist-add')" class="px-5 py-2.5 bg-emerald-500 text-white rounded-xl text-sm font-semibold hover:bg-emerald-600">
          新增黑名单
        </button>
      </div>
      <div v-if="blacklistItems.length === 0" class="text-center py-16 text-slate-400">暂无黑名单记录</div>
      <div v-else class="overflow-hidden rounded-2xl border border-warm-100">
        <table class="w-full">
          <thead>
            <tr class="bg-slate-50">
              <th class="text-left py-3 px-4 text-xs text-slate-500">主体类型</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">主体标识</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">原因</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">状态</th>
              <th class="text-right py-3 px-4 text-xs text-slate-500">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-warm-100">
            <tr v-for="item in blacklistItems" :key="item.id">
              <td class="py-3 px-4 text-slate-700">{{ item.subjectType }}</td>
              <td class="py-3 px-4 text-slate-600 font-mono text-sm">{{ item.subjectId }}</td>
              <td class="py-3 px-4 text-slate-500">{{ item.reason || '-' }}</td>
              <td class="py-3 px-4">
                <span class="px-2 py-1 rounded-lg text-xs font-semibold" :class="item.status === 1 ? 'bg-red-50 text-red-600' : 'bg-slate-100 text-slate-500'">
                  {{ item.status === 1 ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="py-3 px-4 text-right">
                <button
                  v-if="canManageRiskList"
                  @click="emit('blacklist-toggle', item)"
                  class="px-4 py-2 rounded-lg text-sm font-medium"
                  :class="item.status === 1 ? 'bg-white border border-red-200 text-red-500 hover:bg-red-50' : 'bg-emerald-500 text-white hover:bg-emerald-600'"
                >
                  {{ item.status === 1 ? '禁用' : '启用' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else-if="riskTab === 'whitelist' && canViewRiskList" class="space-y-4">
      <div class="flex flex-wrap gap-3 items-center p-4 bg-gradient-to-r from-slate-50 to-warm-50/60 rounded-2xl border border-warm-100">
        <el-input v-model="riskSubjectTypeModel" placeholder="按主体类型筛选（USER / IP / DEVICE）" class="w-72" clearable />
        <el-input v-model="riskSubjectIdModel" placeholder="按主体标识筛选" class="w-72" clearable />
        <button @click="emit('risk-search')" class="px-5 py-2.5 bg-gradient-to-r from-warm-500 to-warm-400 text-white rounded-xl text-sm font-semibold hover:shadow-lg hover:shadow-warm-200 transition-all">
          查询
        </button>
        <button v-if="canManageRiskList" @click="emit('whitelist-add')" class="px-5 py-2.5 bg-emerald-500 text-white rounded-xl text-sm font-semibold hover:bg-emerald-600">
          新增白名单
        </button>
      </div>
      <div v-if="whitelistItems.length === 0" class="text-center py-16 text-slate-400">暂无白名单记录</div>
      <div v-else class="overflow-hidden rounded-2xl border border-warm-100">
        <table class="w-full">
          <thead>
            <tr class="bg-slate-50">
              <th class="text-left py-3 px-4 text-xs text-slate-500">主体类型</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">主体标识</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">原因</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">状态</th>
              <th class="text-right py-3 px-4 text-xs text-slate-500">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-warm-100">
            <tr v-for="item in whitelistItems" :key="item.id">
              <td class="py-3 px-4 text-slate-700">{{ item.subjectType }}</td>
              <td class="py-3 px-4 text-slate-600 font-mono text-sm">{{ item.subjectId }}</td>
              <td class="py-3 px-4 text-slate-500">{{ item.reason || '-' }}</td>
              <td class="py-3 px-4">
                <span class="px-2 py-1 rounded-lg text-xs font-semibold" :class="item.status === 1 ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-500'">
                  {{ item.status === 1 ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="py-3 px-4 text-right">
                <button
                  v-if="canManageRiskList"
                  @click="emit('whitelist-toggle', item)"
                  class="px-4 py-2 rounded-lg text-sm font-medium"
                  :class="item.status === 1 ? 'bg-white border border-red-200 text-red-500 hover:bg-red-50' : 'bg-emerald-500 text-white hover:bg-emerald-600'"
                >
                  {{ item.status === 1 ? '禁用' : '启用' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else-if="riskTab === 'events' && canViewRiskEvents" class="space-y-4">
      <div class="flex flex-wrap gap-3 items-center p-4 bg-gradient-to-r from-slate-50 to-warm-50/60 rounded-2xl border border-warm-100">
        <el-input v-model="riskEventTypeModel" placeholder="按事件类型筛选（如 LOGIN、CHAT_SEND）" class="w-72" clearable />
        <el-input v-model="riskDecisionActionModel" placeholder="按决策动作筛选（ALLOW/REJECT/REVIEW）" class="w-72" clearable />
        <el-input v-model="riskLevelModel" placeholder="按风险等级筛选（LOW/MEDIUM/HIGH）" class="w-72" clearable />
        <input v-model="riskStartTimeModel" type="datetime-local" class="h-10 px-3 text-sm border border-warm-200 rounded-xl bg-white text-slate-700 outline-none focus:ring-2 focus:ring-warm-100 focus:border-warm-300" title="开始时间" />
        <input v-model="riskEndTimeModel" type="datetime-local" class="h-10 px-3 text-sm border border-warm-200 rounded-xl bg-white text-slate-700 outline-none focus:ring-2 focus:ring-warm-100 focus:border-warm-300" title="结束时间" />
        <button @click="emit('risk-search')" class="px-5 py-2.5 bg-gradient-to-r from-warm-500 to-warm-400 text-white rounded-xl text-sm font-semibold hover:shadow-lg hover:shadow-warm-200 transition-all">查询</button>
      </div>
      <div v-if="riskEvents.length === 0" class="text-center py-16 text-slate-400">暂无风险事件</div>
      <div v-else class="overflow-hidden rounded-2xl border border-warm-100">
        <table class="w-full">
          <thead>
            <tr class="bg-slate-50">
              <th class="text-left py-3 px-4 text-xs text-slate-500">事件ID</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">事件类型</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">主体</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">决策</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">风险等级</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">学校/校区</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">时间</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-warm-100">
            <tr v-for="event in riskEvents" :key="event.eventId">
              <td class="py-3 px-4 text-slate-600 font-mono text-sm">#{{ event.eventId }}</td>
              <td class="py-3 px-4 text-slate-700">{{ event.eventType }}</td>
              <td class="py-3 px-4 text-slate-600">{{ event.subjectType }} / {{ event.subjectId }}</td>
              <td class="py-3 px-4 text-slate-600">{{ event.decisionAction || '-' }}</td>
              <td class="py-3 px-4"><span class="px-2 py-1 rounded-lg text-xs font-semibold bg-amber-50 text-amber-600">{{ event.riskLevel || '-' }}</span></td>
              <td class="py-3 px-4 text-slate-500 text-sm">{{ event.schoolCode || '-' }} / {{ event.campusCode || '-' }}</td>
              <td class="py-3 px-4 text-slate-400 text-sm">{{ event.eventTime?.slice(0, 19) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else-if="riskTab === 'cases' && canHandleRiskCase" class="space-y-4">
      <div class="flex flex-wrap gap-3 items-center p-4 bg-gradient-to-r from-slate-50 to-warm-50/60 rounded-2xl border border-warm-100">
        <el-input v-model="riskCaseStatusModel" placeholder="按工单状态筛选（CLOSED/REJECTED/PROCESSING）" class="w-80" clearable />
        <input v-model="riskStartTimeModel" type="datetime-local" class="h-10 px-3 text-sm border border-warm-200 rounded-xl bg-white text-slate-700 outline-none focus:ring-2 focus:ring-warm-100 focus:border-warm-300" title="开始时间" />
        <input v-model="riskEndTimeModel" type="datetime-local" class="h-10 px-3 text-sm border border-warm-200 rounded-xl bg-white text-slate-700 outline-none focus:ring-2 focus:ring-warm-100 focus:border-warm-300" title="结束时间" />
        <button @click="emit('risk-search')" class="px-5 py-2.5 bg-gradient-to-r from-warm-500 to-warm-400 text-white rounded-xl text-sm font-semibold hover:shadow-lg hover:shadow-warm-200 transition-all">查询</button>
      </div>
      <div v-if="riskCases.length === 0" class="text-center py-16 text-slate-400">暂无风险工单</div>
      <div v-else class="overflow-hidden rounded-2xl border border-warm-100">
        <table class="w-full">
          <thead>
            <tr class="bg-slate-50">
              <th class="text-left py-3 px-4 text-xs text-slate-500">工单ID</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">事件类型</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">主体</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">状态</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">结论</th>
              <th class="text-right py-3 px-4 text-xs text-slate-500">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-warm-100">
            <tr v-for="item in riskCases" :key="item.caseId">
              <td class="py-3 px-4 text-slate-600 font-mono text-sm">#{{ item.caseId }}</td>
              <td class="py-3 px-4 text-slate-700">{{ item.eventType || '-' }}</td>
              <td class="py-3 px-4 text-slate-600">{{ item.subjectType }} / {{ item.subjectId }}</td>
              <td class="py-3 px-4 text-slate-600">{{ item.caseStatus }}</td>
              <td class="py-3 px-4 text-slate-600">{{ item.result || '-' }}</td>
              <td class="py-3 px-4 text-right">
                <button @click="emit('risk-case-process', item.caseId)" class="px-4 py-2 bg-warm-500 text-white rounded-lg text-sm font-medium hover:bg-warm-600">处理</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else-if="riskTab === 'rules' && canManageRiskRule" class="space-y-4">
      <div class="flex gap-3 items-center p-4 bg-gradient-to-r from-slate-50 to-warm-50/60 rounded-2xl border border-warm-100">
        <el-input v-model="riskEventTypeModel" placeholder="按事件类型筛选规则（可空）" class="w-80" clearable />
        <button @click="emit('risk-search')" class="px-5 py-2.5 bg-gradient-to-r from-warm-500 to-warm-400 text-white rounded-xl text-sm font-semibold hover:shadow-lg hover:shadow-warm-200 transition-all">查询</button>
        <button @click="emit('risk-rule-create')" class="px-5 py-2.5 bg-emerald-500 text-white rounded-xl text-sm font-semibold hover:bg-emerald-600">新增规则</button>
      </div>
      <div v-if="riskRules.length === 0" class="text-center py-16 text-slate-400">暂无风险规则</div>
      <div v-else class="overflow-hidden rounded-2xl border border-warm-100">
        <table class="w-full">
          <thead>
            <tr class="bg-slate-50">
              <th class="text-left py-3 px-4 text-xs text-slate-500">规则编码</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">规则名称</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">事件类型</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">决策</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">状态</th>
              <th class="text-right py-3 px-4 text-xs text-slate-500">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-warm-100">
            <tr v-for="rule in riskRules" :key="rule.ruleId">
              <td class="py-3 px-4 text-slate-700 font-mono text-sm">{{ rule.ruleCode }}</td>
              <td class="py-3 px-4 text-slate-700">{{ rule.ruleName }}</td>
              <td class="py-3 px-4 text-slate-600">{{ rule.eventType }}</td>
              <td class="py-3 px-4 text-slate-600">{{ rule.decisionAction }}</td>
              <td class="py-3 px-4">
                <span class="px-2 py-1 rounded-lg text-xs font-semibold" :class="rule.status === 1 ? 'bg-emerald-50 text-emerald-600' : 'bg-warm-100 text-warm-700'">
                  {{ rule.status === 1 ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="py-3 px-4 text-right">
                <button @click="emit('risk-rule-toggle', rule)" class="px-4 py-2 rounded-lg text-sm font-medium" :class="rule.status === 1 ? 'bg-white border border-red-200 text-red-500 hover:bg-red-50' : 'bg-emerald-500 text-white hover:bg-emerald-600'">
                  {{ rule.status === 1 ? '禁用' : '启用' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else-if="riskTab === 'behavior' && canViewBehaviorControl" class="space-y-4">
      <div class="flex gap-3 items-center p-4 bg-gradient-to-r from-slate-50 to-warm-50/60 rounded-2xl border border-warm-100">
        <el-input v-model="behaviorUserIdModel" placeholder="输入目标用户ID后查询行为管控" class="w-72" clearable />
        <button @click="emit('behavior-user-query')" class="px-5 py-2.5 bg-gradient-to-r from-warm-500 to-warm-400 text-white rounded-xl text-sm font-semibold hover:shadow-lg hover:shadow-warm-200 transition-all">查询</button>
        <button v-if="canManageBehaviorControl" @click="emit('behavior-control-add')" class="px-5 py-2.5 bg-emerald-500 text-white rounded-xl text-sm font-semibold hover:bg-emerald-600">新增管控</button>
      </div>
      <div v-if="behaviorControls.length === 0" class="text-center py-16 text-slate-400">暂无行为管控记录</div>
      <div v-else class="overflow-hidden rounded-2xl border border-warm-100">
        <table class="w-full">
          <thead>
            <tr class="bg-slate-50">
              <th class="text-left py-3 px-4 text-xs text-slate-500">用户ID</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">行为类型</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">管控动作</th>
              <th class="text-left py-3 px-4 text-xs text-slate-500">原因</th>
              <th class="text-right py-3 px-4 text-xs text-slate-500">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-warm-100">
            <tr v-for="control in behaviorControls" :key="control.id">
              <td class="py-3 px-4 text-slate-600">#{{ control.userId }}</td>
              <td class="py-3 px-4 text-slate-700">{{ control.eventType }}</td>
              <td class="py-3 px-4 text-slate-700">{{ control.controlAction }}</td>
              <td class="py-3 px-4 text-slate-500">{{ control.reason || '-' }}</td>
              <td class="py-3 px-4 text-right">
                <button v-if="canManageBehaviorControl" @click="emit('behavior-control-disable', control.id)" class="px-4 py-2 bg-white border border-red-200 text-red-500 rounded-lg text-sm font-medium hover:bg-red-50">关闭</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else class="py-16 text-center text-slate-400">当前账号无此风控模块权限</div>

    <div v-if="riskTab !== 'mode' && riskTab !== 'behavior' && total > 0" class="mt-2 flex justify-end">
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
