<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Sparkles, Send, Image as ImageIcon, X, Trash2 } from 'lucide-vue-next'
import {
  chatWithAi,
  getAiChatHistory,
  clearAiChatHistory,
  type AiChatMessageVO,
  type AiChatQueryContext,
  type AiChatResponseVO,
  type AiGoodsCardVO,
} from '@/api/modules/ai'
import { uploadFile } from '@/api/modules/file'
import { ElMessage } from '@/utils/feedback'
import SubPageShell from '@/components/SubPageShell.vue'

interface ChatMessage {
  role: AiChatMessageVO['role']
  text: string
  image?: string
  cards?: AiGoodsCardVO[]
  intent?: string
  keyword?: string
  queryLimit?: number
  queryPage?: number
  maxPrice?: number
  total?: number
  hasMore?: boolean
}

const createModelMessage = (text: string, cards: AiGoodsCardVO[] = [], extra: Partial<ChatMessage> = {}): ChatMessage => ({
  role: 'model',
  text,
  cards,
  ...extra,
})

const mapHistoryMessage = (message: AiChatMessageVO): ChatMessage => ({
  role: message.role,
  text: message.content,
  image: message.imageUrl,
  cards: message.cards || [],
})

const buildModelMessageFromResponse = (res: AiChatResponseVO): ChatMessage => {
  const replyText = (res.replyText || '').trim()
  return createModelMessage(
    replyText || (res.cards?.length ? '我帮你找到这些商品了，点卡片就能看详情。' : '我暂时没有更多信息'),
    res.cards || [],
    {
      intent: res.intent,
      keyword: res.keyword,
      queryLimit: res.queryLimit,
      queryPage: res.queryPage,
      maxPrice: res.maxPrice,
      total: res.total,
      hasMore: res.hasMore,
    }
  )
}

const aiAssistantMsgs = ref<ChatMessage[]>([])
const isAiLoading = ref(false)
const isLoadingHistory = ref(true)
const chatInput = ref('')
const chatContainer = ref<HTMLElement | null>(null)
const selectedImage = ref('')
const isUploading = ref(false)
const router = useRouter()
const route = useRoute()

// 加载历史记录
const loadChatHistory = async () => {
  isLoadingHistory.value = true
  try {
    const historyList = await getAiChatHistory()
    if (historyList.length > 0) {
      aiAssistantMsgs.value = historyList.map(mapHistoryMessage)
    } else {
      // 没有历史记录时显示欢迎消息
      aiAssistantMsgs.value = [createModelMessage('你好！我是 Nowl AI。你可以问我商品检索、推荐和平台使用问题~')]
    }
    await nextTick()
    scrollToBottom()
  } catch (e) {
    console.error('加载历史记录失败', e)
    aiAssistantMsgs.value = [createModelMessage('你好！我是 Nowl AI。你可以问我商品检索、推荐和平台使用问题~')]
  } finally {
    isLoadingHistory.value = false
  }
}

// 清除历史记录
const handleClearHistory = async () => {
  try {
    await clearAiChatHistory()
    aiAssistantMsgs.value = [createModelMessage('聊天记录已清除~有什么可以帮你的吗？')]
    ElMessage.success('聊天记录已清除')
  } catch {
    ElMessage.error('清除失败')
  }
}

// 选择图片
const handleSelectImage = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (e: Event) => {
    const file = (e.target as HTMLInputElement).files?.[0]
    if (!file) return

    isUploading.value = true
    try {
      const formData = new FormData()
      formData.append('file', file)
      const res = await uploadFile(formData)
      selectedImage.value = res
    } catch (e) {
      console.error(e)
      ElMessage.error('图片上传失败')
    } finally {
      isUploading.value = false
    }
  }
  input.click()
}

// 粘贴图片处理
const handlePaste = async (event: ClipboardEvent) => {
  const items = event.clipboardData?.items
  if (!items) return

  for (let i = 0; i < items.length; i++) {
    const item = items[i]
    if (item && item.type.indexOf('image') !== -1) {
      const file = item.getAsFile()
      if (!file) continue

      // 阻止默认粘贴行为
      event.preventDefault()

      isUploading.value = true
      try {
        const formData = new FormData()
        formData.append('file', file)
        const res = await uploadFile(formData)
        selectedImage.value = res
        ElMessage.success('图片已粘贴')
      } catch {
        ElMessage.error('图片粘贴失败')
      } finally {
        isUploading.value = false
      }
      return // 只处理第一张图片
    }
  }
}

const handleChat = async () => {
  const text = chatInput.value.trim()
  const image = selectedImage.value

  if (!text && !image) return

  // 1. 添加用户消息到界面
  const newMsgs = [...aiAssistantMsgs.value, {
    role: 'user' as const,
    text: text,
    image: image
  }]
  aiAssistantMsgs.value = newMsgs

  // 2. 清空输入
  chatInput.value = ''
  selectedImage.value = ''
  isAiLoading.value = true

  await nextTick()
  scrollToBottom()

  // 3. 调用 AI
  try {
    const res = await chatWithAi(text || '请看这张图', image)
    aiAssistantMsgs.value = [...newMsgs, buildModelMessageFromResponse(res)]
  } catch (err) {
    console.error(err)
    aiAssistantMsgs.value = [...newMsgs, createModelMessage('哎呀，Nowl AI 有点忙，请稍后再试~')]
  } finally {
    isAiLoading.value = false
    setTimeout(scrollToBottom, 100)
  }
}

const canSwitchBatch = (msg: ChatMessage) => {
  return msg.role === 'model'
    && msg.intent === 'recommend'
    && !!msg.keyword
    && !!msg.queryLimit
    && !!msg.hasMore
}

const buildSwitchQueryContext = (msg: ChatMessage): AiChatQueryContext => {
  const nextPage = Math.max(0, (msg.queryPage ?? 0) + 1)
  return {
    intent: 'recommend',
    keyword: msg.keyword,
    limit: msg.queryLimit,
    maxPrice: msg.maxPrice,
    page: nextPage,
    switchBatch: true,
  }
}

const handleSwitchBatch = async (msg: ChatMessage) => {
  if (!canSwitchBatch(msg) || isAiLoading.value) {
    return
  }

  const userMsg: ChatMessage = { role: 'user', text: '换一批' }
  const newMsgs = [...aiAssistantMsgs.value, userMsg]
  aiAssistantMsgs.value = newMsgs
  isAiLoading.value = true

  await nextTick()
  scrollToBottom()

  try {
    const res = await chatWithAi('换一批', undefined, buildSwitchQueryContext(msg))
    aiAssistantMsgs.value = [...newMsgs, buildModelMessageFromResponse(res)]
  } catch (err) {
    console.error(err)
    aiAssistantMsgs.value = [...newMsgs, createModelMessage('换一批失败了，请稍后重试~')]
  } finally {
    isAiLoading.value = false
    setTimeout(scrollToBottom, 100)
  }
}

const scrollToBottom = () => {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

const formatPrice = (price?: number) => {
  if (price === undefined || price === null || Number.isNaN(price)) {
    return '面议'
  }
  return Number(price).toFixed(2).replace(/\.00$/, '')
}

const formatCardLocation = (card: AiGoodsCardVO) => {
  const schoolCode = card.schoolCode?.trim() || ''
  const campusCode = card.campusCode?.trim() || ''
  if (schoolCode && campusCode) {
    return `${schoolCode} / ${campusCode}`
  }
  return schoolCode || campusCode
}

const handleOpenProduct = (productId: number) => {
  if (!Number.isFinite(productId) || productId <= 0) return
  router.push({
    path: `/product/${productId}`,
    query: { from: route.fullPath },
  })
}

onMounted(() => {
  loadChatHistory()
})
</script>

<template>
  <SubPageShell title="Nowl AI" subtitle="商品问答与平台助手" max-width="lg" :use-card="false" :show-back="false">
    <template #icon>
      <Sparkles class="text-white w-8 h-8" stroke-width="2.5" />
    </template>

    <div class="h-[calc(100vh-240px)] min-h-[560px] flex flex-col um-card overflow-hidden animate-in fade-in duration-500">
      <!-- Header -->
      <div class="p-5 border-b border-warm-100 flex items-center justify-between bg-white">
        <div class="flex items-center gap-3">
          <div class="bg-gradient-to-r from-warm-500 to-warm-400 p-2.5 rounded-2xl text-white">
            <Sparkles :size="20" />
          </div>
          <div>
            <h3 class="font-bold text-slate-800">Nowl AI</h3>
            <p class="text-[10px] text-green-500 font-bold uppercase tracking-wider">多模态 AI • 在线</p>
          </div>
        </div>
        <button
          @click="handleClearHistory"
          class="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-xl transition-colors"
          title="清除聊天记录"
        >
          <Trash2 :size="18" />
        </button>
      </div>

      <!-- Chat Area -->
      <div
        ref="chatContainer"
        class="flex-1 p-6 overflow-y-auto space-y-6 no-scrollbar"
      >
        <!-- Loading History -->
        <div v-if="isLoadingHistory" class="flex justify-center py-10">
          <div class="animate-spin rounded-full h-8 w-8 border-2 border-warm-500 border-t-transparent"></div>
        </div>

        <template v-else>
          <div
            v-for="(msg, i) in aiAssistantMsgs"
            :key="i"
            class="flex"
            :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
          >
            <div class="flex flex-col gap-2 max-w-[85%]">
              <!-- Image -->
              <img
                v-if="msg.image"
                :src="msg.image"
                class="rounded-2xl border border-slate-100 max-w-[200px] object-cover"
              />
              <!-- Text -->
              <div
                v-if="msg.text"
                class="p-4 rounded-2xl text-sm leading-relaxed shadow-sm"
                :class="msg.role === 'user'
                  ? 'bg-warm-500 text-white rounded-tr-none'
                  : 'bg-slate-50 text-slate-700 rounded-tl-none border border-slate-100'"
              >
                {{ msg.text }}
              </div>

              <div
                v-if="msg.role === 'model' && msg.cards && msg.cards.length > 0"
                class="space-y-2"
              >
                <button
                  v-for="card in msg.cards"
                  :key="card.productId"
                  type="button"
                  class="w-full text-left bg-white border border-slate-200 rounded-2xl p-3 hover:border-warm-300 hover:shadow-sm transition-all"
                  @click="handleOpenProduct(card.productId)"
                >
                  <div class="flex items-center gap-3">
                    <img
                      v-if="card.image"
                      :src="card.image"
                      :alt="card.title"
                      class="w-14 h-14 rounded-xl object-cover border border-slate-100 flex-shrink-0"
                    />
                    <div
                      v-else
                      class="w-14 h-14 rounded-xl bg-slate-100 text-slate-400 text-xs flex items-center justify-center flex-shrink-0"
                    >
                      无图
                    </div>
                    <div class="min-w-0 flex-1">
                      <p class="text-sm font-semibold text-slate-800 break-words">{{ card.title }}</p>
                      <p class="mt-1 text-warm-600 font-bold">¥{{ formatPrice(card.price) }}</p>
                      <p class="mt-1 text-xs text-slate-400">
                        {{ card.sellerName || '匿名卖家' }}
                        <span v-if="formatCardLocation(card)"> · {{ formatCardLocation(card) }}</span>
                      </p>
                    </div>
                  </div>
                </button>

                <button
                  v-if="canSwitchBatch(msg)"
                  type="button"
                  :disabled="isAiLoading"
                  class="w-full mt-1 text-sm font-medium rounded-xl py-2.5 border border-warm-200 text-warm-600 bg-warm-50 hover:bg-warm-100 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                  @click="handleSwitchBatch(msg)"
                >
                  换一批
                </button>
              </div>
            </div>
          </div>
        </template>

        <!-- Loading Indicator -->
        <div v-if="isAiLoading" class="flex justify-start">
          <div class="bg-slate-50 p-4 rounded-2xl rounded-tl-none border border-slate-100 flex gap-1">
            <div class="w-1.5 h-1.5 bg-warm-300 rounded-full animate-bounce"></div>
            <div class="w-1.5 h-1.5 bg-warm-300 rounded-full animate-bounce [animation-delay:-0.3s]"></div>
            <div class="w-1.5 h-1.5 bg-warm-300 rounded-full animate-bounce [animation-delay:-0.5s]"></div>
          </div>
        </div>
      </div>

      <!-- Input Area -->
      <div class="p-4 bg-white border-t border-warm-100">
        <!-- Image Preview -->
        <div v-if="selectedImage" class="mb-3 relative inline-block">
          <img :src="selectedImage" class="h-20 rounded-xl border border-warm-100" />
          <button
            @click="selectedImage = ''"
            class="absolute -top-2 -right-2 bg-slate-800 text-white rounded-full p-1 shadow-md hover:bg-slate-900"
          >
            <X :size="12" />
          </button>
        </div>

        <div class="relative flex items-center gap-2">
          <button
            @click="handleSelectImage"
            :disabled="isUploading"
            class="p-3 bg-white border border-warm-100 text-slate-500 rounded-full hover:bg-warm-50 hover:text-warm-500 transition-colors disabled:opacity-50"
          >
            <ImageIcon :size="20" />
          </button>

          <input
            v-model="chatInput"
            @keydown.enter="handleChat"
            @paste="handlePaste"
            class="flex-1 pl-6 pr-16 py-4 bg-slate-100 border-none rounded-full focus:ring-2 focus:ring-warm-200 text-sm outline-none"
            :placeholder="isUploading ? '正在上传图片...' : '发消息或图片...'"
            :disabled="isUploading"
          />

          <button
            @click="handleChat"
            :disabled="!chatInput && !selectedImage"
            class="absolute right-2 p-3 bg-warm-500 text-white rounded-full shadow-lg shadow-warm-100 hover:bg-warm-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Send :size="20" />
          </button>
        </div>
      </div>
    </div>
  </SubPageShell>
</template>
