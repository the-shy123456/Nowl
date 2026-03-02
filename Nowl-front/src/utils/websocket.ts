/**
 * WebSocket工具类
 * 功能：自动重连、心跳检测、消息队列
 */

import type { ChatMessage as ApiChatMessage } from '@/api/modules/chat'

interface ChatMessage extends ApiChatMessage {
  type?: 'message' | 'pong' // 支持心跳响应
}

interface SendMessage {
  receiverId: number
  content: string
  messageType: number
}

interface WebSocketConfig {
  heartbeatInterval?: number // 心跳间隔，默认 30 秒
  reconnectInterval?: number // 初始重连间隔，默认 1 秒
  maxReconnectAttempts?: number // 最大重连次数，默认 5 次
}

export class ChatWebSocket {
  private socket: WebSocket | null = null
  private url: string
  private onMessageCallback: (data: ChatMessage) => void

  // 重连机制
  private reconnectAttempts = 0
  private maxReconnectAttempts: number
  private reconnectInterval: number
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private manualClose = false // 标记是否手动关闭

  // 心跳检测
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null
  private heartbeatInterval: number
  private pongReceived = true // 标记是否收到心跳响应

  // 消息队列（连接断开时缓存消息）
  private messageQueue: SendMessage[] = []

  constructor(onMessage: (data: ChatMessage) => void, config: WebSocketConfig = {}) {
    this.onMessageCallback = onMessage

    // 配置参数
    this.heartbeatInterval = config.heartbeatInterval || 30000 // 30秒
    this.reconnectInterval = config.reconnectInterval || 1000 // 1秒
    this.maxReconnectAttempts = config.maxReconnectAttempts || 5 // 5次

    // 构造 WebSocket URL（统一走前端同源 /api 代理）
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsPath = '/api/ws/chat'
    this.url = `${protocol}//${window.location.host}${wsPath}`
  }

  /**
   * 连接 WebSocket
   */
  connect() {
    if (this.socket?.readyState === WebSocket.OPEN) {
      return
    }

    try {
      this.socket = new WebSocket(this.url)
      this.setupEventHandlers()
    } catch (error) {
      console.warn('聊天WS连接创建失败，准备重连', error)
      // 连接失败，尝试重连
      this.handleReconnect()
    }
  }

  /**
   * 设置事件处理器
   */
  private setupEventHandlers() {
    if (!this.socket) return

    this.socket.onopen = () => {
      this.reconnectAttempts = 0 // 重置重连次数
      this.startHeartbeat() // 启动心跳
      this.flushMessageQueue() // 发送队列中的消息
    }

    this.socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as ChatMessage

        // 处理心跳响应
        if (data.type === 'pong') {
          this.pongReceived = true
          return
        }

        // 处理业务消息
        this.onMessageCallback(data)
      } catch (error) {
        console.warn('聊天WS消息解析失败', error)
      }
    }

    this.socket.onclose = () => {
      this.stopHeartbeat()

      // 如果不是手动关闭，则尝试重连
      if (!this.manualClose) {
        this.handleReconnect()
      }
    }

    this.socket.onerror = () => {
      // 生产环境可以上报到监控系统
    }
  }

  /**
   * 启动心跳检测
   */
  private startHeartbeat() {
    this.stopHeartbeat() // 先清除之前的定时器

    this.heartbeatTimer = setInterval(() => {
      if (this.socket?.readyState === WebSocket.OPEN) {
        // 检查上次心跳是否收到响应
        if (!this.pongReceived) {
          this.socket.close()
          return
        }

        // 发送心跳
        this.pongReceived = false
        this.socket.send(JSON.stringify({ type: 'ping' }))
      }
    }, this.heartbeatInterval)
  }

  /**
   * 停止心跳检测
   */
  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /**
   * 处理重连逻辑
   */
  private handleReconnect() {
    // 清除之前的重连定时器
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      // 达到最大重连次数，可以通知用户或上报监控
      return
    }

    this.reconnectAttempts++

    // 指数退避算法：1s, 2s, 4s, 8s, 16s
    const delay = Math.min(
      this.reconnectInterval * Math.pow(2, this.reconnectAttempts - 1),
      30000 // 最大 30 秒
    )

    this.reconnectTimer = setTimeout(() => {
      this.connect()
    }, delay)
  }

  /**
   * 发送消息
   */
  send(message: SendMessage) {
    if (this.socket?.readyState === WebSocket.OPEN) {
      try {
        this.socket.send(JSON.stringify(message))
      } catch (error) {
        console.warn('聊天WS发送失败，消息已入队', error)
        // 发送失败，加入队列
        this.messageQueue.push(message)
      }
    } else {
      // 连接未打开，加入队列
      this.messageQueue.push(message)

      // 如果未连接，尝试重连
      if (!this.socket || this.socket.readyState === WebSocket.CLOSED) {
        this.connect()
      }
    }
  }

  /**
   * 发送队列中的消息
   */
  private flushMessageQueue() {
    if (this.messageQueue.length === 0) return

    while (this.messageQueue.length > 0) {
      const message = this.messageQueue.shift()
      if (message && this.socket?.readyState === WebSocket.OPEN) {
        try {
          this.socket.send(JSON.stringify(message))
        } catch (error) {
          console.warn('聊天WS补发失败，消息退回队列', error)
          // 如果发送失败，放回队列
          this.messageQueue.unshift(message)
          break
        }
      }
    }
  }

  /**
   * 手动关闭连接
   */
  close() {
    this.manualClose = true
    this.stopHeartbeat()

    // 清除重连定时器
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    if (this.socket) {
      this.socket.close()
      this.socket = null
    }

    // 清空消息队列
    this.messageQueue = []
  }

  /**
   * 获取当前连接状态
   */
  getReadyState(): number {
    return this.socket?.readyState ?? WebSocket.CLOSED
  }

  /**
   * 是否已连接
   */
  isConnected(): boolean {
    return this.socket?.readyState === WebSocket.OPEN
  }
}
