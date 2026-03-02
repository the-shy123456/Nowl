<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Ban, MessageCircle, Search, ShieldAlert, User } from 'lucide-vue-next'
import { getBlockList, type ChatBlockItem, unblockUser } from '@/api/modules/chat'
import { ElMessage } from '@/utils/feedback'
import { normalizeMediaUrl } from '@/utils/media'
import { showConfirm, showSuccess } from '@/utils/modal'
import SubPageShell from '@/components/SubPageShell.vue'

const router = useRouter()

const loading = ref(false)
const keyword = ref('')
const list = ref<ChatBlockItem[]>([])
const actionLoadingMap = ref<Record<number, boolean>>({})

const filteredList = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  if (!key) {
    return list.value
  }
  return list.value.filter((item) => {
    const nickName = (item.nickName || '').toLowerCase()
    return nickName.includes(key) || String(item.userId).includes(key)
  })
})

const blockedStats = computed(() => ({
  total: list.value.length,
  showing: filteredList.value.length,
}))

const formatBlockTime = (value?: string) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

const fetchList = async () => {
  loading.value = true
  try {
    list.value = await getBlockList()
  } catch {
    ElMessage.error('获取拉黑列表失败')
  } finally {
    loading.value = false
  }
}

const handleUnblock = async (item: ChatBlockItem) => {
  if (!item.userId || actionLoadingMap.value[item.userId]) return

  const confirmed = await showConfirm(
    `确定将“${item.nickName || `用户${item.userId}`}”移出拉黑列表？`,
    '解除拉黑',
    {
      confirmText: '解除',
      cancelText: '取消',
    },
  )
  if (!confirmed) return

  actionLoadingMap.value[item.userId] = true
  try {
    await unblockUser(item.userId)
    list.value = list.value.filter(entry => entry.userId !== item.userId)
    showSuccess('已解除拉黑')
  } catch {
    ElMessage.error('解除失败，请稍后重试')
  } finally {
    actionLoadingMap.value[item.userId] = false
  }
}

const openProfile = (userId?: number) => {
  if (!userId) return
  router.push(`/user/${userId}`)
}

const openChat = (item: ChatBlockItem) => {
  if (!item.userId) return
  router.push({
    path: `/chat/user/${item.userId}`,
    query: { name: item.nickName || `用户${item.userId}` },
  })
}

onMounted(() => {
  void fetchList()
})
</script>

<template>
  <SubPageShell title="我的拉黑" subtitle="管理已屏蔽用户" back-to="/profile" max-width="lg" :use-card="false">
    <div class="space-y-4">
      <section class="um-card p-4 md:p-5 space-y-3">
        <div class="flex items-center justify-between gap-3 flex-wrap">
          <div class="inline-flex items-center gap-2 text-slate-700 font-semibold">
            <ShieldAlert :size="16" class="text-amber-500" />
            拉黑列表
          </div>

          <div class="grid grid-cols-2 gap-2 text-xs">
            <div class="rounded-xl bg-warm-50 border border-warm-100 px-3 py-2 text-warm-700 min-w-[88px]">
              <div class="text-[11px] text-warm-500">总人数</div>
              <div class="text-sm font-bold">{{ blockedStats.total }}</div>
            </div>
            <div class="rounded-xl bg-slate-100 border border-slate-200 px-3 py-2 text-slate-700 min-w-[88px]">
              <div class="text-[11px] text-slate-500">筛选结果</div>
              <div class="text-sm font-bold">{{ blockedStats.showing }}</div>
            </div>
          </div>
        </div>

        <label class="flex items-center gap-2 rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2.5">
          <Search :size="15" class="text-slate-400" />
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索昵称或用户ID"
            class="w-full bg-transparent outline-none text-sm text-slate-700 placeholder:text-slate-400"
          />
        </label>
      </section>

      <div v-if="loading" class="text-center py-12">
        <div class="inline-block animate-spin rounded-full h-10 w-10 border-4 border-warm-500 border-t-transparent"></div>
        <p class="mt-3 text-slate-500 text-sm">正在加载拉黑列表...</p>
      </div>

      <div v-else-if="filteredList.length === 0" class="um-card text-center py-16 text-slate-400">
        <Ban :size="44" class="mx-auto mb-3 text-slate-300" />
        <p class="text-sm">{{ keyword ? '暂无匹配用户' : '暂无拉黑用户' }}</p>
      </div>

      <div v-else class="space-y-3">
        <article
          v-for="item in filteredList"
          :key="item.userId"
          class="um-card p-4 flex items-center gap-3"
        >
          <button
            @click="openProfile(item.userId)"
            class="w-11 h-11 rounded-full overflow-hidden border border-slate-200 bg-slate-50 flex items-center justify-center shrink-0"
          >
            <img
              v-if="normalizeMediaUrl(item.avatar)"
              :src="normalizeMediaUrl(item.avatar)"
              :alt="`${item.nickName || item.userId}头像`"
              class="w-full h-full object-cover"
            />
            <User v-else :size="16" class="text-slate-400" />
          </button>

          <div class="flex-1 min-w-0">
            <div class="font-semibold text-slate-800 truncate">{{ item.nickName || `用户${item.userId}` }}</div>
            <div class="mt-0.5 text-xs text-slate-500">ID: {{ item.userId }}</div>
            <div class="mt-0.5 text-[11px] text-slate-400">拉黑时间：{{ formatBlockTime(item.blockTime) }}</div>
          </div>

          <button
            @click="openChat(item)"
            class="um-btn px-3 py-2 text-xs bg-slate-100 text-slate-600 hover:bg-slate-200 inline-flex items-center gap-1"
          >
            <MessageCircle :size="13" />
            会话
          </button>
          <button
            @click="handleUnblock(item)"
            :disabled="actionLoadingMap[item.userId]"
            class="um-btn px-3 py-2 text-xs bg-emerald-50 text-emerald-600 hover:bg-emerald-100 disabled:opacity-60"
          >
            {{ actionLoadingMap[item.userId] ? '处理中...' : '解除拉黑' }}
          </button>
        </article>
      </div>
    </div>
  </SubPageShell>
</template>
