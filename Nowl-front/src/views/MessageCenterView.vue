<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  MessageSquare,
  Bell,
  ChevronRight,
  Search,
  Sparkles,
  UserRound,
  RefreshCw,
} from 'lucide-vue-next'
import { getContactList, markAllChatsAsRead, type Contact } from '@/api/modules/chat'
import { getNoticeList, markAllNoticesAsRead, markNoticeAsRead, type Notice } from '@/api/modules/notice'
import { getGoodsDetail } from '@/api/modules/goods'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { showInfo, showWarning } from '@/utils/modal'
import dayjs from 'dayjs'
import SubPageShell from '@/components/SubPageShell.vue'

const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()

interface ContactItem extends Contact {
  unreadCount: number
}

const activeTab = ref<'chat' | 'notice'>('chat')
const contacts = ref<ContactItem[]>([])
const notices = ref<Notice[]>([])
const loading = ref(false)
const manualRefreshing = ref(false)
const markAllReading = ref(false)
const keyword = ref('')
let refreshTimer: ReturnType<typeof setInterval> | null = null

const syncUnreadMessageCount = () => {
  const chatUnreadCount = contacts.value.reduce((sum, item) => sum + Number(item.unreadCount || 0), 0)
  const noticeUnreadCount = notices.value.filter(item => Number(item.isRead) === 0).length
  messageStore.setUnreadCount(chatUnreadCount + noticeUnreadCount)
}

const fetchContacts = async (silent = false) => {
  if (!userStore.isLoggedIn) return
  if (!silent) loading.value = true
  try {
    const contactList = (await getContactList()) || []
    contacts.value = contactList.map((item) => ({
      ...item,
      unreadCount: item.unreadCount ?? 0,
    }))
  } catch (error) {
    console.error(error)
  } finally {
    syncUnreadMessageCount()
    if (!silent) loading.value = false
  }
}

const fetchNotices = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const noticeList = await getNoticeList()
    notices.value = noticeList || []
    syncUnreadMessageCount()
  } catch (error) {
    console.error(error)
  }
}

const manualRefresh = async () => {
  if (manualRefreshing.value) return
  manualRefreshing.value = true
  const startedAt = Date.now()
  try {
    await Promise.all([
      fetchContacts(true),
      fetchNotices(),
    ])
  } finally {
    const elapsed = Date.now() - startedAt
    const remain = Math.max(0, 400 - elapsed)
    window.setTimeout(() => {
      manualRefreshing.value = false
    }, remain)
  }
}

const handleMarkAllRead = async () => {
  if (markAllReading.value || totalUnread.value <= 0) return
  markAllReading.value = true
  try {
    await Promise.all([
      markAllChatsAsRead(),
      markAllNoticesAsRead(),
    ])
    contacts.value = contacts.value.map(item => ({
      ...item,
      unreadCount: 0,
    }))
    notices.value = notices.value.map(item => ({
      ...item,
      isRead: 1,
    }))
    syncUnreadMessageCount()
    showInfo('全部消息已标记为已读')
  } catch (error) {
    console.error('全部已读失败', error)
    showWarning('全部已读失败，请稍后重试')
  } finally {
    markAllReading.value = false
  }
}

const handleChatClick = (contact: ContactItem) => {
  router.push({
    path: `/chat/user/${contact.userId}`,
    query: { name: contact.nickName || '联系人' },
  })
}

type NoticeTarget = 'product' | 'order' | 'errand' | 'dispute' | 'review' | 'system'

// 与后端 NoticeType 对齐：0-系统，1-交易，2-评价，3-纠纷
const NOTICE_TYPE_SYSTEM = 0
const NOTICE_TYPE_TRADE = 1
const NOTICE_TYPE_REVIEW = 2
const NOTICE_TYPE_DISPUTE = 3

const normalizeNoticeId = (value: unknown): number | null => {
  if (typeof value === 'number' && Number.isFinite(value) && value > 0) {
    return Math.floor(value)
  }
  if (typeof value === 'string') {
    const trimmed = value.trim()
    if (/^\d+$/.test(trimmed)) {
      const parsed = Number(trimmed)
      return parsed > 0 ? parsed : null
    }
  }
  return null
}

const inferNoticeTarget = (notice: Notice): NoticeTarget => {
  const bizType = String(notice.bizType || '').toLowerCase()
  const type = Number(notice.type)

  if (bizType === 'dispute') return 'dispute'
  if (bizType === 'review') return 'review'
  if (bizType === 'errand') return 'errand'
  if (bizType === 'product') return 'product'
  if (bizType === 'order') return 'order'
  if (bizType === 'system') return 'system'

  if (type === NOTICE_TYPE_DISPUTE) return 'dispute'
  if (type === NOTICE_TYPE_REVIEW) return 'review'
  if (type === NOTICE_TYPE_TRADE) return 'order'
  if (type === NOTICE_TYPE_SYSTEM) return notice.relatedId ? 'product' : 'system'
  return 'system'
}

const resolveNoticeRoute = (notice: Notice) => {
  const target = inferNoticeTarget(notice)
  const relatedId = normalizeNoticeId(notice.relatedId)

  if (target === 'errand') {
    return {
      target,
      relatedId,
      path: relatedId ? `/errand/${relatedId}` : '/profile/my-errands',
    }
  }

  if (target === 'dispute') {
    return {
      target,
      relatedId,
      path: relatedId ? `/dispute/${relatedId}` : '/dispute/list',
    }
  }

  if (target === 'order') {
    return {
      target,
      relatedId,
      path: '/profile/my-orders',
    }
  }

  if (target === 'system') {
    return {
      target,
      relatedId,
      path: '',
    }
  }

  if (target === 'review') {
    const currentUserId = userStore.userInfo?.userId
    return {
      target,
      relatedId,
      path: currentUserId ? `/review/user/${currentUserId}` : '/profile',
    }
  }

  return {
    target,
    relatedId,
    path: relatedId ? `/product/${relatedId}` : '/profile/my-goods',
  }
}

const isNoticeNavigable = (notice: Notice) => {
  return Boolean(resolveNoticeRoute(notice).path)
}

const handleNoticeClick = async (notice: Notice) => {
  const wasUnread = Number(notice.isRead) === 0
  if (notice.noticeId && Number(notice.isRead) === 0) {
    try {
      await markNoticeAsRead(notice.noticeId)
      messageStore.decreaseUnreadCount(1)
    } catch (error) {
      console.error('标记通知已读失败', error)
    }
  }

  notice.isRead = 1
  if (wasUnread) {
    syncUnreadMessageCount()
  }

  const { target, relatedId, path } = resolveNoticeRoute(notice)

  if (target === 'product' && relatedId) {
    try {
      await getGoodsDetail(relatedId)
    } catch (error) {
      console.warn('通知关联商品不可访问，降级跳转我的商品', error)
      showInfo('商品可能已删除或下架，已为你跳转到我的商品')
      await router.push('/profile/my-goods')
      return
    }
  }

  if (!relatedId && target === 'errand') {
    showInfo('关联跑腿任务不存在，已为你跳转到我的跑腿')
  }

  if (!relatedId && target === 'dispute') {
    showInfo('关联纠纷不存在，已为你跳转到纠纷列表')
  }

  if (!relatedId && target === 'product') {
    showInfo('关联商品不存在，已为你跳转到我的商品')
  }

  if (target === 'system') {
    return
  }

  try {
    await router.push(path)
  } catch (error) {
    console.error('通知跳转失败：', error)
    showWarning('跳转失败，请稍后重试')
  }
}

const formatTime = (time?: string) => {
  if (!time) return '--'
  return dayjs(time).format('MM-DD HH:mm')
}

const formatNoticeTime = (time?: string) => {
  if (!time) return '--'
  const date = dayjs(time)
  const now = dayjs()
  if (now.diff(date, 'day') < 1) return date.format('HH:mm')
  if (now.diff(date, 'day') < 7) return `${now.diff(date, 'day')}天前`
  return date.format('MM-DD')
}

const chatUnread = computed(() =>
  contacts.value.reduce((sum, item) => sum + (item.unreadCount || 0), 0),
)

const noticeUnread = computed(() =>
  notices.value.filter(item => Number(item.isRead) === 0).length,
)

const totalUnread = computed(() => chatUnread.value + noticeUnread.value)

const filteredContacts = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  if (!key) return contacts.value
  return contacts.value.filter(item => {
    const text = `${item.nickName || ''} ${item.lastMessage || ''}`.toLowerCase()
    return text.includes(key)
  })
})

const filteredNotices = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  if (!key) return notices.value
  return notices.value.filter(item => {
    const text = `${item.title || ''} ${item.content || ''}`.toLowerCase()
    return text.includes(key)
  })
})

const noticeTypeLabel = (type?: number) => {
  if (type === NOTICE_TYPE_TRADE) return '交易'
  if (type === NOTICE_TYPE_REVIEW) return '评价'
  if (type === NOTICE_TYPE_DISPUTE) return '纠纷'
  return '系统'
}

const startAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  refreshTimer = setInterval(() => {
    if (document.hidden) return
    void fetchContacts(true)
    void fetchNotices()
  }, 15000)
}

onMounted(() => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  void fetchContacts()
  void fetchNotices()
  messageStore.fetchUnreadMessageCount().catch((error) => {
    console.warn('初始化未读消息数失败', error)
  })
  startAutoRefresh()
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>

<template>
  <SubPageShell title="消息中心" subtitle="私信会话与系统通知" max-width="lg" :use-card="false">
    <template #icon>
      <MessageSquare class="text-white w-8 h-8" stroke-width="2.5" />
    </template>

    <div class="space-y-4">
      <section class="um-card p-4 md:p-5">
        <div class="flex items-start justify-between gap-3">
          <div>
            <div class="inline-flex items-center gap-2 text-slate-700 font-semibold">
              <Sparkles :size="16" class="text-warm-500" />
              消息总览
            </div>
            <p class="mt-1 text-xs text-slate-500">会话与系统消息每 15 秒自动刷新</p>
          </div>
          <div class="flex flex-wrap items-center justify-end gap-2">
            <button
              class="um-btn px-3 py-2 text-xs bg-slate-100 text-slate-600 hover:bg-slate-200 inline-flex items-center gap-1"
              @click="manualRefresh"
              :disabled="manualRefreshing"
            >
              <RefreshCw :size="12" :class="manualRefreshing ? 'animate-spin' : ''" />
              {{ manualRefreshing ? '刷新中...' : '刷新' }}
            </button>
            <button
              class="um-btn px-3 py-2 text-xs bg-slate-100 text-slate-600 hover:bg-slate-200"
              @click="handleMarkAllRead"
              :disabled="markAllReading || totalUnread <= 0"
            >
              {{ markAllReading ? '处理中...' : `全部已读${totalUnread > 0 ? `(${totalUnread})` : ''}` }}
            </button>
          </div>
        </div>

        <div class="mt-3 grid grid-cols-3 gap-2 text-xs">
          <div class="rounded-xl bg-warm-50 border border-warm-100 px-3 py-2 text-warm-700">
            <div class="text-[11px] text-warm-500">总未读</div>
            <div class="text-sm font-bold">{{ totalUnread }}</div>
          </div>
          <div class="rounded-xl bg-blue-50 border border-blue-100 px-3 py-2 text-blue-700">
            <div class="text-[11px] text-blue-500">私信未读</div>
            <div class="text-sm font-bold">{{ chatUnread }}</div>
          </div>
          <div class="rounded-xl bg-emerald-50 border border-emerald-100 px-3 py-2 text-emerald-700">
            <div class="text-[11px] text-emerald-500">通知未读</div>
            <div class="text-sm font-bold">{{ noticeUnread }}</div>
          </div>
        </div>

        <div class="mt-3 px-4 py-3 rounded-2xl bg-white border border-warm-100 flex items-center gap-3">
          <Search :size="16" class="text-um-muted" />
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索联系人、消息或通知内容"
            class="flex-1 bg-transparent outline-none text-sm text-um-text placeholder:text-um-muted"
            aria-label="搜索联系人或通知"
          />
        </div>
      </section>

      <section class="inline-flex w-full rounded-2xl border border-slate-200 bg-slate-50 p-1">
        <button
          @click="activeTab = 'chat'"
          class="flex-1 py-2 text-sm font-semibold rounded-xl transition-colors"
          :class="activeTab === 'chat' ? 'bg-white text-warm-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'"
        >
          会话消息
        </button>
        <button
          @click="activeTab = 'notice'"
          class="flex-1 py-2 text-sm font-semibold rounded-xl transition-colors"
          :class="activeTab === 'notice' ? 'bg-white text-warm-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'"
        >
          系统通知
        </button>
      </section>

      <section v-if="activeTab === 'chat'" class="space-y-3">
        <div v-if="loading" class="um-card py-10 text-center text-sm text-slate-400">正在加载会话...</div>
        <div v-else-if="filteredContacts.length === 0" class="um-card py-16 text-center text-slate-400">
          <UserRound :size="44" class="mx-auto mb-3 text-slate-300" />
          <p>暂无会话消息</p>
        </div>

        <article
          v-for="contact in filteredContacts"
          v-else
          :key="contact.userId"
          @click="handleChatClick(contact)"
          class="um-card p-4 flex items-center gap-3 cursor-pointer hover:border-warm-200 transition-colors"
        >
          <div class="relative">
            <img
              :src="contact.avatar || '/avatar-placeholder.svg'"
              class="w-12 h-12 rounded-full object-cover border border-warm-100"
              alt="联系人头像"
            />
            <div
              v-if="contact.unreadCount > 0"
              class="absolute -top-1 -right-1 bg-red-500 text-white text-[10px] min-w-[16px] h-4 px-1 rounded-full flex items-center justify-center border-2 border-white"
            >
              {{ contact.unreadCount > 99 ? '99+' : contact.unreadCount }}
            </div>
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center justify-between gap-2">
              <p class="font-semibold text-slate-800 truncate">{{ contact.nickName || '未命名联系人' }}</p>
              <span class="text-xs text-slate-400 shrink-0">{{ formatTime(contact.lastTime) }}</span>
            </div>
            <p class="mt-1 text-sm text-slate-500 truncate">{{ contact.lastMessage || '暂无消息' }}</p>
          </div>
          <ChevronRight :size="16" class="text-slate-300" />
        </article>
      </section>

      <section v-else class="space-y-3">
        <div v-if="filteredNotices.length === 0" class="um-card py-16 text-center text-slate-400">
          <Bell :size="44" class="mx-auto mb-3 text-slate-300" />
          <p>暂无系统通知</p>
        </div>

        <article
          v-for="notice in filteredNotices"
          :key="notice.noticeId"
          @click="handleNoticeClick(notice)"
          class="um-card p-4 md:p-5 flex items-start gap-3 cursor-pointer hover:border-warm-200 transition-colors"
        >
          <div class="mt-0.5 rounded-xl p-2 bg-warm-50 border border-warm-100 text-warm-600">
            <Bell :size="16" />
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-start justify-between gap-2">
              <h4 class="font-semibold text-slate-800 line-clamp-1">{{ notice.title || '系统通知' }}</h4>
              <span class="text-xs text-slate-400 shrink-0">{{ formatNoticeTime(notice.createTime) }}</span>
            </div>
            <p class="text-sm text-slate-600 mt-1 line-clamp-2 leading-6">{{ notice.content || '暂无内容' }}</p>
            <div class="mt-2 flex items-center gap-2 flex-wrap">
              <span class="text-[10px] px-2 py-0.5 rounded-full bg-slate-100 text-slate-500">
                {{ noticeTypeLabel(notice.type) }}
              </span>
              <span
                v-if="Number(notice.isRead) === 0"
                class="text-[10px] px-2 py-0.5 rounded-full bg-red-50 text-red-500"
              >
                未读
              </span>
            </div>
          </div>
          <ChevronRight v-if="isNoticeNavigable(notice)" :size="16" class="text-slate-300 self-center" />
        </article>
      </section>
    </div>
  </SubPageShell>
</template>

<style scoped>
.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
