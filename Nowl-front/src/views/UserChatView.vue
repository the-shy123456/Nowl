<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Send, ShieldAlert, User } from 'lucide-vue-next'
import {
  blockUser,
  getBlockRelation,
  getChatHistory,
  markChatAsRead,
  sendMessage,
  type ChatMessage,
  unblockUser,
} from '@/api/modules/chat'
import { getUserInfo } from '@/api/modules/user'
import { ChatWebSocket } from '@/utils/websocket'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { showConfirm, showError, showInfo, showSuccess, showWarning } from '@/utils/modal'
import SubPageShell from '@/components/SubPageShell.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()

const contactId = computed(() => Number(route.params.id))
const hasValidContactId = computed(() => Number.isFinite(contactId.value) && contactId.value > 0)
const contactName = computed(() => {
  const name = route.query.name
  return typeof name === 'string' && name.trim() ? name : '联系人'
})
const contactAvatar = ref('')
const blockRelation = ref(0)
const blockActionLoading = ref(false)

const messages = ref<ChatMessage[]>([])
const chatInput = ref('')
const chatContainer = ref<HTMLElement | null>(null)
const wsAvailable = ref(false)

let ws: ChatWebSocket | null = null
let pollingTimer: ReturnType<typeof setInterval> | null = null

const scrollToBottom = () => {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

const isBlockedByOther = computed(() => blockRelation.value === 2 || blockRelation.value === 3)
const isBlockingOther = computed(() => blockRelation.value === 1 || blockRelation.value === 3)
const sendDisabled = computed(
  () => isBlockedByOther.value || isBlockingOther.value || blockActionLoading.value,
)
const blockStatusText = computed(() => {
  if (isBlockedByOther.value) {
    return '你已被对方拉黑，暂时无法发送消息'
  }
  if (isBlockingOther.value) {
    return '你已拉黑对方，解除拉黑后可继续沟通'
  }
  return ''
})

const ensureValidContactId = () => {
  if (hasValidContactId.value) return true
  showWarning('会话对象无效，已返回消息中心')
  router.replace('/message')
  return false
}

const fetchContactProfile = async () => {
  if (!hasValidContactId.value) {
    contactAvatar.value = ''
    return
  }
  try {
    const profile = await getUserInfo(contactId.value)
    contactAvatar.value = profile.imageUrl || ''
  } catch {
    contactAvatar.value = ''
  }
}

const fetchBlockRelation = async () => {
  if (!hasValidContactId.value) {
    blockRelation.value = 0
    return
  }
  try {
    blockRelation.value = await getBlockRelation(contactId.value)
  } catch {
    blockRelation.value = 0
  }
}

const openContactProfile = () => {
  if (!hasValidContactId.value) {
    return
  }
  router.push(`/user/${contactId.value}`)
}

const fetchHistory = async () => {
  if (!hasValidContactId.value) return
  const res = await getChatHistory(contactId.value)
  messages.value = res || []
  await nextTick()
  scrollToBottom()
}

const markCurrentChatAsRead = async () => {
  if (!hasValidContactId.value) return
  await markChatAsRead(contactId.value)
  await messageStore.fetchUnreadMessageCount().catch((error) => {
    console.warn('刷新未读消息数失败', error)
  })
}

const startPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
  }
  pollingTimer = setInterval(async () => {
    if (!hasValidContactId.value) return
    if (document.hidden) return
    await fetchHistory()
  }, wsAvailable.value ? 15000 : 5000)
}

const connectWebSocket = () => {
  if (!userStore.isLoggedIn) {
    wsAvailable.value = false
    return
  }

  ws = new ChatWebSocket(data => {
    if (data.senderId !== contactId.value) {
      messageStore.increaseUnreadCount(1)
      return
    }

    if (isBlockedByOther.value || isBlockingOther.value) {
      return
    }

    messages.value.push(data)
    nextTick(scrollToBottom)
    markCurrentChatAsRead().catch((error) => {
      console.warn('标记消息已读失败', error)
    })
  })
  ws.connect()
  wsAvailable.value = true
}

const sendChatMessage = async () => {
  const content = chatInput.value.trim()
  if (!content || !hasValidContactId.value) return
  if (sendDisabled.value) {
    showWarning(blockStatusText.value || '当前无法发送消息')
    return
  }

  try {
    await sendMessage({
      receiverId: contactId.value,
      content,
      type: 0,
    })

    messages.value.push({
      senderId: userStore.userInfo?.userId || 0,
      receiverId: contactId.value,
      content,
      createTime: new Date().toISOString(),
    })

    chatInput.value = ''
    await nextTick()
    scrollToBottom()
    await messageStore.fetchUnreadMessageCount().catch((error) => {
      console.warn('刷新未读消息数失败', error)
    })
  } catch (error) {
    console.error('发送私信失败', error)
    showError('发送失败，请稍后重试')
  }
}

const handleBlockToggle = async () => {
  if (!hasValidContactId.value) {
    return
  }
  if (isBlockedByOther.value) {
    showInfo('对方已将你拉黑，暂不支持反向操作')
    return
  }

  if (isBlockingOther.value) {
    const confirmed = await showConfirm('确定解除拉黑？解除后可继续互发消息', '解除拉黑', {
      confirmText: '解除',
      cancelText: '取消',
    })
    if (!confirmed) {
      return
    }
    blockActionLoading.value = true
    try {
      await unblockUser(contactId.value)
      showSuccess('已解除拉黑')
      await fetchBlockRelation()
    } finally {
      blockActionLoading.value = false
    }
    return
  }

  const confirmed = await showConfirm('拉黑后对方将无法继续给你发消息，确定拉黑？', '拉黑用户', {
    confirmText: '拉黑',
    cancelText: '取消',
  })
  if (!confirmed) {
    return
  }

  blockActionLoading.value = true
  try {
    await blockUser(contactId.value)
    showSuccess('已加入拉黑列表')
    await fetchBlockRelation()
  } finally {
    blockActionLoading.value = false
  }
}

const initializeChat = async () => {
  if (!ensureValidContactId()) return
  if (ws) {
    ws.close()
    ws = null
  }
  await fetchHistory()
  await markCurrentChatAsRead()
  await fetchContactProfile()
  await fetchBlockRelation()
  connectWebSocket()
  startPolling()
}

onMounted(() => {
  initializeChat().catch(error => {
    console.error('初始化聊天失败', error)
    showError('加载聊天记录失败')
  })
})

watch(contactId, (newValue, oldValue) => {
  if (!Number.isFinite(newValue) || newValue <= 0) {
    ensureValidContactId()
    return
  }
  if (newValue === oldValue) {
    return
  }
  initializeChat().catch(error => {
    console.error('切换联系人失败', error)
    showError('加载聊天记录失败')
  })
})

onUnmounted(() => {
  if (ws) {
    ws.close()
    ws = null
  }
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
})
</script>

<template>
  <SubPageShell :title="contactName" subtitle="沟通交易细节，保障双方权益" back-to="/message" max-width="lg" :use-card="false">
    <div class="um-card h-[calc(100vh-220px)] min-h-[520px] flex flex-col overflow-hidden">
      <div class="p-5 border-b border-warm-100 flex items-center justify-between gap-3 bg-white">
        <div class="flex items-center gap-3 min-w-0">
          <button
            @click="openContactProfile"
            class="w-11 h-11 rounded-full overflow-hidden border border-warm-100 hover:border-warm-300 transition-colors bg-warm-50 flex items-center justify-center"
            title="查看对方主页"
          >
            <img
              v-if="contactAvatar"
              :src="contactAvatar"
              :alt="`${contactName}头像`"
              class="w-full h-full object-cover"
            />
            <User v-else :size="18" class="text-warm-500" />
          </button>
          <div class="min-w-0">
            <h3 class="font-bold text-slate-800 truncate">{{ contactName }}</h3>
            <p class="text-xs text-slate-400">{{ wsAvailable ? '实时消息已开启' : '网络同步模式' }}</p>
          </div>
        </div>
        <button
          @click="handleBlockToggle"
          :disabled="blockActionLoading"
          class="shrink-0 px-3 py-1.5 rounded-full text-xs font-semibold border transition-colors"
          :class="isBlockingOther ? 'border-amber-200 text-amber-600 bg-amber-50 hover:bg-amber-100' : 'border-red-200 text-red-500 bg-red-50 hover:bg-red-100'"
        >
          {{ isBlockingOther ? '解除拉黑' : '拉黑' }}
        </button>
      </div>

      <div v-if="blockStatusText" class="px-5 py-3 border-b border-amber-100 bg-amber-50 text-amber-700 text-xs flex items-center gap-2">
        <ShieldAlert :size="14" />
        <span>{{ blockStatusText }}</span>
      </div>

      <div ref="chatContainer" class="flex-1 p-5 overflow-y-auto space-y-4 bg-[#f8f4f2]">
        <div
          v-for="(msg, index) in messages"
          :key="`${msg.senderId}-${msg.receiverId}-${msg.createTime}-${index}`"
          class="flex"
          :class="msg.senderId === userStore.userInfo?.userId ? 'justify-end' : 'justify-start'"
        >
          <div
            class="max-w-[85%] p-4 rounded-2xl text-sm leading-relaxed shadow-sm"
            :class="
              msg.senderId === userStore.userInfo?.userId
                ? 'bg-warm-500 text-white rounded-tr-none'
                : 'bg-white text-slate-700 rounded-tl-none border border-warm-100'
            "
          >
            {{ msg.content }}
          </div>
        </div>
      </div>

      <div class="p-5 bg-white border-t border-warm-100">
        <div class="relative flex items-center">
          <input
            v-model="chatInput"
            @keydown.enter="sendChatMessage"
            :disabled="sendDisabled"
            class="w-full pl-5 pr-16 py-3.5 bg-white border border-warm-100 rounded-full focus:ring-2 focus:ring-warm-200 text-sm outline-none disabled:opacity-60 disabled:cursor-not-allowed"
            :placeholder="sendDisabled ? (blockStatusText || '当前无法发送消息') : '输入消息...'"
            aria-label="聊天输入框"
          />
          <button
            @click="sendChatMessage"
            :disabled="sendDisabled"
            class="absolute right-2 p-2.5 bg-warm-500 text-white rounded-full shadow-sm hover:bg-warm-600 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
            aria-label="发送消息"
          >
            <Send :size="18" />
          </button>
        </div>
      </div>
    </div>
  </SubPageShell>
</template>
