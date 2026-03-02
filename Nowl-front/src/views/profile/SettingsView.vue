<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Bell,
  CircleDollarSign,
  Database,
  Lock,
  MessageCircle,
  Moon,
  Scale,
  Save,
  Settings,
  Sun,
  Truck,
  Undo2,
} from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from '@/utils/feedback'
import SubPageShell from '@/components/SubPageShell.vue'

const STORAGE_KEY = 'um:profile-settings'

type ThemeMode = 'light' | 'dark'
type NotifySettingKey = 'notifyMessage' | 'notifyOrder' | 'notifyRefund' | 'notifyDispute'

interface ProfileSettings {
  notifyMessage: boolean
  notifyOrder: boolean
  notifyRefund: boolean
  notifyDispute: boolean
  themeMode: ThemeMode
}

const defaultSettings: ProfileSettings = {
  notifyMessage: true,
  notifyOrder: true,
  notifyRefund: true,
  notifyDispute: true,
  themeMode: 'light',
}

const notificationItems: Array<{
  key: NotifySettingKey
  title: string
  subtitle: string
  icon: typeof MessageCircle
}> = [
  {
    key: 'notifyMessage',
    title: '私信与系统消息',
    subtitle: '聊天消息、系统公告与提醒',
    icon: MessageCircle,
  },
  {
    key: 'notifyOrder',
    title: '订单状态更新',
    subtitle: '发货、收货、取消等关键状态',
    icon: Truck,
  },
  {
    key: 'notifyRefund',
    title: '退款进度通知',
    subtitle: '申请提交、处理结果与到账提醒',
    icon: CircleDollarSign,
  },
  {
    key: 'notifyDispute',
    title: '纠纷处理通知',
    subtitle: '申诉进度、补充举证与裁定结果',
    icon: Scale,
  },
]

const sharedThemeVariables = {
  '--um-primary': '#8D6E63',
  '--um-primary-600': '#6D4C41',
} as const

const themeVariableMap: Record<ThemeMode, Record<string, string>> = {
  light: {
    ...sharedThemeVariables,
    '--um-primary-100': '#EFEBE9',
    '--um-accent': '#FF7043',
    '--um-cta': '#22c55e',
    '--um-bg': '#FFFBF8',
    '--um-bg-soft': '#FFF5EE',
    '--um-surface': '#ffffff',
    '--um-surface-2': '#FFFCFA',
    '--um-border': 'rgba(141, 110, 99, 0.18)',
    '--um-text': '#3E2723',
    '--um-muted': '#8D7B75',
    '--um-shadow': '0 16px 40px rgba(141, 110, 99, 0.18)',
    '--um-shadow-soft': '0 8px 24px rgba(141, 110, 99, 0.12)',
    '--um-gradient': 'linear-gradient(135deg, #EFEBE9 0%, #FFF8F5 45%, #FFF3E0 100%)',
  },
  dark: {
    ...sharedThemeVariables,
    '--um-primary-100': 'rgba(141, 110, 99, 0.18)',
    '--um-accent': '#FF8A65',
    '--um-cta': '#34d399',
    '--um-bg': '#1A1210',
    '--um-bg-soft': '#211A17',
    '--um-surface': '#2A211E',
    '--um-surface-2': '#332924',
    '--um-border': 'rgba(141, 110, 99, 0.24)',
    '--um-text': '#F5EDE8',
    '--um-muted': '#C4B5AE',
    '--um-shadow': '0 16px 40px rgba(0, 0, 0, 0.35)',
    '--um-shadow-soft': '0 10px 30px rgba(0, 0, 0, 0.28)',
    '--um-gradient': 'linear-gradient(135deg, #2A211E 0%, #1A1210 40%, #211A17 100%)',
  },
}

const router = useRouter()
const settings = ref<ProfileSettings>({ ...defaultSettings })
const cacheSizeMB = ref('0.00')

const isDarkMode = computed(() => settings.value.themeMode === 'dark')
const themeText = computed(() => (isDarkMode.value ? '夜晚模式' : '白天模式'))

const applyThemeMode = (mode: ThemeMode) => {
  const html = document.documentElement
  const variableMap = themeVariableMap[mode]
  Object.entries(variableMap).forEach(([key, value]) => {
    html.style.setProperty(key, value)
  })
}

const estimateCacheSize = () => {
  let totalChars = 0
  for (let index = 0; index < localStorage.length; index += 1) {
    const key = localStorage.key(index)
    if (!key) continue
    const value = localStorage.getItem(key) || ''
    totalChars += key.length + value.length
  }
  const bytes = totalChars * 2
  cacheSizeMB.value = (bytes / 1024 / 1024).toFixed(2)
}

const loadSettings = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<ProfileSettings>
      settings.value = {
        ...defaultSettings,
        ...parsed,
        themeMode: parsed.themeMode === 'dark' ? 'dark' : 'light',
      }
    } else {
      settings.value = { ...defaultSettings }
    }
  } catch {
    settings.value = { ...defaultSettings }
  }
  applyThemeMode(settings.value.themeMode)
}

const saveSettings = () => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(settings.value))
  applyThemeMode(settings.value.themeMode)
  ElMessage.success('设置已保存')
}

const resetSettings = () => {
  settings.value = { ...defaultSettings }
  saveSettings()
}

const clearCache = async () => {
  try {
    await ElMessageBox.confirm('将清除本地缓存并保留当前偏好设置，是否继续？', '清理缓存', {
      confirmButtonText: '确认清理',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  const preservedSettings = JSON.stringify(settings.value)
  const keysToRemove: string[] = []
  for (let i = 0; i < localStorage.length; i += 1) {
    const key = localStorage.key(i)
    if (!key) continue
    if (
      key === STORAGE_KEY ||
      key.endsWith('-store') ||
      key.startsWith('um-') ||
      key.startsWith('um:')
    ) {
      keysToRemove.push(key)
    }
  }
  keysToRemove.forEach((key) => localStorage.removeItem(key))
  localStorage.setItem(STORAGE_KEY, preservedSettings)
  applyThemeMode(settings.value.themeMode)
  estimateCacheSize()
  ElMessage.success('缓存已清理')
}

const switchThemeMode = (mode: ThemeMode) => {
  settings.value.themeMode = mode
  applyThemeMode(mode)
}

const openChangePassword = () => {
  router.push('/forgot-password')
}

onMounted(() => {
  loadSettings()
  estimateCacheSize()
})
</script>

<template>
  <SubPageShell title="通知与偏好设置" subtitle="统一管理通知、外观和安全偏好" back-to="/profile" max-width="lg" :use-card="false">
    <template #icon>
      <Settings class="text-white w-8 h-8" stroke-width="2.5" />
    </template>

    <div class="space-y-4 pb-4">
      <section class="um-card p-4 md:p-5">
        <div class="flex items-center justify-between gap-3 flex-wrap">
          <div class="inline-flex items-center gap-2 text-slate-700 font-semibold">
            <Bell :size="16" class="text-warm-500" />
            通知设置
          </div>
          <div class="rounded-xl bg-slate-100 border border-slate-200 px-3 py-2 text-xs text-slate-600">
            当前主题：<span class="font-semibold">{{ themeText }}</span>
          </div>
        </div>

        <div class="mt-3 space-y-2">
          <label
            v-for="item in notificationItems"
            :key="item.key"
            class="flex items-center justify-between gap-3 rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3 hover:border-warm-200 hover:bg-warm-50/60 transition-colors"
          >
            <div class="flex items-center gap-3 min-w-0">
              <component :is="item.icon" :size="18" class="text-warm-500 shrink-0" />
              <div class="min-w-0">
                <p class="text-sm font-semibold text-slate-700 truncate">{{ item.title }}</p>
                <p class="text-xs text-slate-500 truncate">{{ item.subtitle }}</p>
              </div>
            </div>
            <input
              v-model="settings[item.key]"
              type="checkbox"
              class="h-4 w-4 accent-warm-500 shrink-0"
            />
          </label>
        </div>
      </section>

      <section class="um-card p-4 md:p-5 space-y-3">
        <div class="inline-flex items-center gap-2 text-slate-700 font-semibold">
          <Sun :size="16" class="text-amber-500" />
          外观偏好
        </div>

        <div class="grid grid-cols-2 gap-2 rounded-2xl bg-slate-50 p-1 border border-slate-200">
          <button
            @click="switchThemeMode('light')"
            class="h-10 rounded-xl text-sm font-semibold transition-all inline-flex items-center justify-center gap-2"
            :class="settings.themeMode === 'light'
              ? 'bg-white text-amber-600 shadow-sm'
              : 'text-slate-500 hover:text-slate-700'"
          >
            <Sun :size="15" />
            白天
          </button>
          <button
            @click="switchThemeMode('dark')"
            class="h-10 rounded-xl text-sm font-semibold transition-all inline-flex items-center justify-center gap-2"
            :class="settings.themeMode === 'dark'
              ? 'bg-slate-900 text-white shadow-sm'
              : 'text-slate-500 hover:text-slate-700'"
          >
            <Moon :size="15" />
            夜晚
          </button>
        </div>
      </section>

      <section class="um-card p-4 md:p-5 space-y-2">
        <div class="inline-flex items-center gap-2 text-slate-700 font-semibold">
          <Lock :size="16" class="text-warm-500" />
          安全与本地数据
        </div>

        <button
          @click="openChangePassword"
          class="w-full rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3 hover:border-warm-200 hover:bg-warm-50/60 transition-colors flex items-center justify-between"
        >
          <div class="inline-flex items-center gap-2 text-sm text-slate-700">
            <Lock :size="16" class="text-warm-500" />
            修改登录密码
          </div>
          <span class="text-xs text-slate-400">通过忘记密码重置</span>
        </button>

        <button
          @click="clearCache"
          class="w-full rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3 hover:border-warm-200 hover:bg-warm-50/60 transition-colors flex items-center justify-between"
        >
          <div class="inline-flex items-center gap-2 text-sm text-slate-700">
            <Database :size="16" class="text-warm-500" />
            清理缓存
          </div>
          <span class="text-xs text-slate-400">{{ cacheSizeMB }} MB</span>
        </button>
      </section>

      <section class="flex gap-2.5">
        <button
          @click="saveSettings"
          class="flex-1 um-btn px-4 py-3 bg-warm-500 text-white hover:bg-warm-600 inline-flex items-center justify-center gap-2 text-sm font-semibold"
        >
          <Save :size="15" />
          保存设置
        </button>
        <button
          @click="resetSettings"
          class="um-btn px-4 py-3 bg-slate-100 text-slate-600 hover:bg-slate-200 inline-flex items-center justify-center gap-1.5 text-sm font-semibold"
        >
          <Undo2 :size="15" />
          重置
        </button>
      </section>

      <div class="text-center text-xs text-um-muted py-2">Nowl AI v1.0.0</div>
    </div>
  </SubPageShell>
</template>
