import { ref, watch, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { showInfo } from '@/utils/modal'

export function useNotificationWs() {
  const userStore = useUserStore()
  const messageStore = useMessageStore()
  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  const connected = ref(false)
  let lastNoticeToastAt = 0

  const shouldToastNotice = (title: string) => /审核|复核|纠纷|退款/.test(title)

  const connect = () => {
    if (!userStore.isLoggedIn) return

    disconnect()

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsPath = '/api/ws/chat'
    const url = `${protocol}//${window.location.host}${wsPath}`

    try {
      ws = new WebSocket(url)

      ws.onopen = () => {
        connected.value = true
      }

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          if (data.type === 'NOTICE') {
            messageStore.increaseUnreadCount(1)
            const title = typeof data.title === 'string' ? data.title.trim() : ''
            if (title && shouldToastNotice(title)) {
              const now = Date.now()
              if (now - lastNoticeToastAt >= 1200) {
                lastNoticeToastAt = now
                void showInfo(title, '新通知')
              }
            }
          }
        } catch (error) {
          console.warn('通知WS消息解析失败', error)
        }
      }

      ws.onclose = () => {
        connected.value = false
        scheduleReconnect()
      }

      ws.onerror = () => {
        connected.value = false
      }
    } catch (error) {
      console.warn('通知WS连接创建失败', error)
    }
  }

  const disconnect = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (ws) {
      ws.onclose = null
      ws.onerror = null
      ws.onmessage = null
      ws.close()
      ws = null
    }
    connected.value = false
  }

  const scheduleReconnect = () => {
    if (reconnectTimer) return
    if (!userStore.isLoggedIn) return
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      if (userStore.isLoggedIn) {
        connect()
      }
    }, 5000)
  }

  watch(
    () => userStore.isLoggedIn,
    (loggedIn) => {
      if (loggedIn) {
        connect()
      } else {
        disconnect()
      }
    },
    { immediate: true },
  )

  onUnmounted(() => {
    disconnect()
  })

  return { connected, connect, disconnect }
}
