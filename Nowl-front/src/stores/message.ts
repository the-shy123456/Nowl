import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getContactList } from '@/api/modules/chat'
import { getNoticeList } from '@/api/modules/notice'

const MESSAGE_COUNT_CHANGED_EVENT = 'unimarket:message-count-changed'

export const useMessageStore = defineStore('message', () => {
  const unreadMessageCount = ref(0)
  const loadingUnread = ref(false)
  let unreadRefreshTimer: ReturnType<typeof setInterval> | null = null

  const unreadMessageBadge = computed(() =>
    unreadMessageCount.value > 99 ? '99+' : unreadMessageCount.value.toString(),
  )

  const emitChanged = () => {
    window.dispatchEvent(
      new CustomEvent(MESSAGE_COUNT_CHANGED_EVENT, {
        detail: { unreadCount: unreadMessageCount.value },
      }),
    )
  }

  const setUnreadCount = (value: number) => {
    const normalized = Number.isFinite(value) ? Math.max(0, Math.floor(value)) : 0
    if (normalized !== unreadMessageCount.value) {
      unreadMessageCount.value = normalized
      emitChanged()
      return
    }
    unreadMessageCount.value = normalized
  }

  const increaseUnreadCount = (delta = 1) => {
    setUnreadCount(unreadMessageCount.value + delta)
  }

  const decreaseUnreadCount = (delta = 1) => {
    setUnreadCount(unreadMessageCount.value - delta)
  }

  const fetchUnreadMessageCount = async () => {
    loadingUnread.value = true
    try {
      const [contacts, notices] = await Promise.all([getContactList(), getNoticeList()])
      const chatUnread = (contacts || []).reduce(
        (sum, contact) => sum + Number(contact.unreadCount || 0),
        0,
      )
      const noticeUnread = (notices || []).filter((item) => Number(item.isRead) === 0).length
      setUnreadCount(chatUnread + noticeUnread)
      return unreadMessageCount.value
    } finally {
      loadingUnread.value = false
    }
  }

  const clearUnreadCount = () => {
    setUnreadCount(0)
  }

  const startUnreadPolling = () => {
    stopUnreadPolling()
    unreadRefreshTimer = setInterval(() => {
      if (document.hidden) {
        return
      }
      fetchUnreadMessageCount().catch((error) => {
        console.warn('轮询未读消息数失败', error)
      })
    }, 15000)
  }

  const stopUnreadPolling = () => {
    if (unreadRefreshTimer) {
      clearInterval(unreadRefreshTimer)
      unreadRefreshTimer = null
    }
  }

  return {
    unreadMessageCount,
    unreadMessageBadge,
    loadingUnread,
    fetchUnreadMessageCount,
    setUnreadCount,
    increaseUnreadCount,
    decreaseUnreadCount,
    clearUnreadCount,
    startUnreadPolling,
    stopUnreadPolling,
    MESSAGE_COUNT_CHANGED_EVENT,
  }
})
