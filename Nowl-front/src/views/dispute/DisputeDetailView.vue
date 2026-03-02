<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  ChevronRight,
  AlertTriangle,
  Clock,
  CheckCircle,
  XCircle,
  Undo2,
  Image as ImageIcon,
  Package,
  Bike,
  User,
  MessageCircle,
  Send,
  Upload,
  X,
  ChevronLeft,
} from 'lucide-vue-next'
import { getDisputeDetail, withdrawDispute, addDisputeEvidence, type DisputeDetail } from '@/api/modules/dispute'
import { uploadFile } from '@/api/modules/file'
import { ElMessage, ElMessageBox } from '@/utils/feedback'
import { DisputeStatusMap } from '@/constants/statusMaps'
import SubPageShell from '@/components/SubPageShell.vue'

const router = useRouter()
const route = useRoute()

const recordId = computed(() => Number(route.params.id))
const dispute = ref<DisputeDetail | null>(null)
const loading = ref(true)
const replying = ref(false)
const replyForm = ref({
  additionalContent: '',
  additionalEvidence: [] as string[],
})
const uploadCount = ref(0)

const showPreview = ref(false)
const previewImages = ref<string[]>([])
const previewIndex = ref(0)
let previewClosedAt = 0

const currentPreviewImage = computed(() => previewImages.value[previewIndex.value] ?? '')
const canShowReplyForm = computed(() => Boolean(dispute.value?.canReply))

const formatTime = (time?: string | Date) => {
  if (!time) return '--'
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const getStatusIcon = (status: number) => {
  switch (status) {
    case 0:
      return Clock
    case 1:
      return AlertTriangle
    case 2:
      return CheckCircle
    case 3:
      return XCircle
    case 4:
      return Undo2
    default:
      return Clock
  }
}

const goToContent = () => {
  if (!dispute.value) return
  if (dispute.value.targetType === 0) {
    router.push('/profile/my-orders')
  } else {
    router.push(`/errand/${dispute.value.contentId}`)
  }
}

const openPreview = (images: string[], index = 0) => {
  if (Date.now() - previewClosedAt < 220) return
  if (!images.length) return
  previewImages.value = images
  previewIndex.value = index
  showPreview.value = true
}

const closePreview = () => {
  showPreview.value = false
  previewClosedAt = Date.now()
}

const movePreview = (direction: -1 | 1) => {
  if (!previewImages.value.length) return
  const total = previewImages.value.length
  previewIndex.value = (previewIndex.value + direction + total) % total
}

const handlePreviewKeydown = (event: KeyboardEvent) => {
  if (!showPreview.value) return
  if (event.key === 'Escape') {
    closePreview()
    return
  }
  if (event.key === 'ArrowLeft') {
    event.preventDefault()
    movePreview(-1)
    return
  }
  if (event.key === 'ArrowRight') {
    event.preventDefault()
    movePreview(1)
  }
}

const claimItems = computed(() => {
  if (!dispute.value) return []
  return [
    {
      label: '申请扣分',
      value: dispute.value.claimSellerCreditPenalty === 1 ? '是' : '否',
    },
    {
      label: '申请退款',
      value: dispute.value.claimRefund === 1 ? `是（¥${dispute.value.claimRefundAmount || 0}）` : '否',
    },
    {
      label: '补充次数',
      value: `发起方 ${dispute.value.initiatorReplyCount || 0}/3 · 被投诉方 ${dispute.value.relatedReplyCount || 0}/3`,
    },
  ]
})

const fetchDetail = async () => {
  if (!Number.isFinite(recordId.value) || recordId.value <= 0) {
    ElMessage.error('纠纷编号无效')
    router.push('/dispute/list')
    return
  }

  loading.value = true
  try {
    const res = await getDisputeDetail(recordId.value)
    dispute.value = res
  } catch {
    ElMessage.error('获取纠纷详情失败')
  } finally {
    loading.value = false
  }
}

const handleWithdraw = async () => {
  if (!dispute.value) return
  if (!dispute.value.canWithdraw) {
    ElMessage.warning('当前状态不可撤回纠纷')
    return
  }

  try {
    await ElMessageBox.confirm('确认撤回该纠纷？撤回后无法恢复。', '提示', {
      confirmButtonText: '确认撤回',
      cancelButtonText: '取消',
    })

    await withdrawDispute(recordId.value)
    ElMessage.success('纠纷已撤回')
    await fetchDetail()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('撤回纠纷失败:', error)
      ElMessage.error('撤回失败，请稍后重试')
    }
  }
}

const handleReplyUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const files = input.files
  if (!files || files.length === 0) return

  if (replyForm.value.additionalEvidence.length + files.length > 9) {
    ElMessage.warning('最多上传9张图片')
    input.value = ''
    return
  }

  for (const file of files) {
    if (!file.type.startsWith('image/')) {
      ElMessage.warning('只能上传图片文件')
      continue
    }
    if (file.size > 5 * 1024 * 1024) {
      ElMessage.warning('单张图片不能超过5MB')
      continue
    }

    uploadCount.value++
    try {
      const url = await uploadFile(file)
      if (url) {
        replyForm.value.additionalEvidence.push(url)
      }
    } catch {
      ElMessage.error('上传失败，请重试')
    } finally {
      uploadCount.value--
    }
  }

  input.value = ''
}

const removeReplyEvidence = (index: number) => {
  replyForm.value.additionalEvidence.splice(index, 1)
}

const handleReply = async () => {
  if (!dispute.value) return
  if (!dispute.value.canReply) {
    ElMessage.warning('当前无法继续补充')
    return
  }

  if (!replyForm.value.additionalContent.trim() && replyForm.value.additionalEvidence.length === 0) {
    ElMessage.warning('请至少填写说明或上传证据')
    return
  }

  replying.value = true
  try {
    await addDisputeEvidence({
      recordId: dispute.value.recordId,
      additionalContent: replyForm.value.additionalContent.trim() || undefined,
      additionalEvidence:
        replyForm.value.additionalEvidence.length > 0
          ? JSON.stringify(replyForm.value.additionalEvidence)
          : undefined,
    })
    ElMessage.success('补充成功')
    replyForm.value.additionalContent = ''
    replyForm.value.additionalEvidence = []
    await fetchDetail()
  } catch {
    ElMessage.error('补充失败，请稍后重试')
  } finally {
    replying.value = false
  }
}

onMounted(() => {
  void fetchDetail()
  window.addEventListener('keydown', handlePreviewKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handlePreviewKeydown)
})
</script>

<template>
  <SubPageShell title="纠纷详情" subtitle="查看处理进度与双方补充记录" back-to="/dispute/list" max-width="lg" :use-card="false">
    <template #icon>
      <MessageCircle class="text-white w-8 h-8" stroke-width="2.5" />
    </template>

    <div v-if="loading" class="flex justify-center py-20">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-warm-500"></div>
    </div>

    <template v-else-if="dispute">
      <div class="space-y-4">
        <section class="um-card p-5 bg-gradient-to-r from-white to-slate-50 border-slate-200">
          <div class="flex items-start justify-between gap-3">
            <div>
              <div class="px-3 py-1 rounded-full text-xs font-semibold inline-flex items-center gap-1 w-fit" :class="DisputeStatusMap[dispute.handleStatus]?.color || 'bg-warm-100 text-warm-700'">
                <component :is="getStatusIcon(dispute.handleStatus)" class="w-3 h-3" />
                {{ dispute.statusDesc }}
              </div>
              <h2 class="text-lg font-semibold text-slate-900 mt-2">{{ dispute.targetTypeDesc }}</h2>
              <p class="text-sm text-slate-500 mt-1">
                {{ dispute.isInitiator ? '您发起的投诉，正在等待平台处理' : '您收到一条投诉，请及时补充说明' }}
              </p>
            </div>
            <span class="text-xs text-slate-400">{{ formatTime(dispute.createTime) }}</span>
          </div>
        </section>

        <section class="um-card p-4">
          <h3 class="font-medium text-slate-700 mb-3 flex items-center gap-2">
            <component :is="dispute.targetType === 0 ? Package : Bike" class="w-4 h-4" />
            关联内容
          </h3>
          <button
            @click="goToContent"
            class="w-full text-left flex gap-3 rounded-xl p-2 -m-2 hover:bg-warm-50 transition-colors"
          >
            <div class="w-16 h-16 rounded-lg overflow-hidden bg-warm-50 border border-warm-100 flex-shrink-0">
              <img
                v-if="dispute.productImage || dispute.errandImage"
                :src="dispute.productImage || dispute.errandImage"
                class="w-full h-full object-cover"
                alt="关联内容"
              />
              <div v-else class="w-full h-full flex items-center justify-center">
                <component :is="dispute.targetType === 0 ? Package : Bike" class="w-6 h-6 text-slate-300" />
              </div>
            </div>
            <div class="flex-1 min-w-0">
              <h4 class="font-medium text-slate-800 line-clamp-1">{{ dispute.productTitle || dispute.errandTitle || '未知内容' }}</h4>
              <p v-if="dispute.targetType === 0" class="text-sm text-slate-500 mt-1">
                订单号：{{ dispute.orderNo || '--' }}
              </p>
              <p class="text-sm text-emerald-600 font-medium mt-1">
                ¥{{ dispute.orderAmount || dispute.errandReward || 0 }}
              </p>
            </div>
            <ChevronRight class="w-5 h-5 text-slate-300 self-center" />
          </button>
        </section>

        <section class="um-card p-4">
          <h3 class="font-medium text-slate-700 mb-3 flex items-center gap-2">
            <User class="w-4 h-4" />
            双方信息
          </h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <article class="rounded-xl border border-slate-100 bg-slate-50 p-3">
              <div class="flex items-center gap-3">
                <img
                  v-if="dispute.initiatorAvatar"
                  :src="dispute.initiatorAvatar"
                  class="w-10 h-10 rounded-full object-cover"
                  alt="发起人头像"
                />
                <div v-else class="w-10 h-10 rounded-full bg-warm-100 flex items-center justify-center">
                  <User class="w-5 h-5 text-slate-400" />
                </div>
                <div>
                  <p class="font-medium text-slate-800">{{ dispute.initiatorName || '未知用户' }}</p>
                  <p class="text-xs text-slate-500">发起人</p>
                </div>
              </div>
            </article>
            <article class="rounded-xl border border-slate-100 bg-slate-50 p-3">
              <div class="flex items-center gap-3">
                <img
                  v-if="dispute.relatedAvatar"
                  :src="dispute.relatedAvatar"
                  class="w-10 h-10 rounded-full object-cover"
                  alt="被投诉方头像"
                />
                <div v-else class="w-10 h-10 rounded-full bg-warm-100 flex items-center justify-center">
                  <User class="w-5 h-5 text-slate-400" />
                </div>
                <div>
                  <p class="font-medium text-slate-800">{{ dispute.relatedName || '未知用户' }}</p>
                  <p class="text-xs text-slate-500">被投诉方</p>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section class="um-card p-4">
          <h3 class="font-medium text-slate-700 mb-3 flex items-center gap-2">
            <AlertTriangle class="w-4 h-4" />
            争议内容
          </h3>
          <p class="text-slate-600 whitespace-pre-wrap leading-7">{{ dispute.content || '暂无描述' }}</p>

          <div v-if="dispute.evidenceUrlList && dispute.evidenceUrlList.length > 0" class="mt-4">
            <p class="text-sm text-slate-500 mb-2 flex items-center gap-1">
              <ImageIcon class="w-4 h-4" />
              首次证据图片
            </p>
            <div class="grid grid-cols-3 sm:grid-cols-4 gap-2">
              <button
                v-for="(url, index) in dispute.evidenceUrlList"
                :key="`${url}-${index}`"
                class="aspect-square rounded-lg overflow-hidden border border-slate-200"
                @click="openPreview(dispute.evidenceUrlList || [], index)"
              >
                <img :src="url" class="w-full h-full object-cover" alt="证据图片" />
              </button>
            </div>
          </div>
        </section>

        <section class="um-card p-4">
          <h3 class="font-medium text-slate-700 mb-3 flex items-center gap-2">
            <MessageCircle class="w-4 h-4" />
            诉求项
          </h3>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-2 text-sm">
            <div v-for="item in claimItems" :key="item.label" class="rounded-lg bg-slate-50 border border-slate-100 px-3 py-2">
              <p class="text-xs text-slate-500">{{ item.label }}</p>
              <p class="text-slate-700 mt-1 font-medium">{{ item.value }}</p>
            </div>
          </div>
        </section>

        <section class="um-card p-4">
          <h3 class="font-medium text-slate-700 mb-3 flex items-center gap-2">
            <MessageCircle class="w-4 h-4" />
            双方补充记录
          </h3>
          <div v-if="!dispute.conversations || dispute.conversations.length === 0" class="text-sm text-slate-400">
            暂无补充记录
          </div>
          <div v-else class="space-y-3">
            <article
              v-for="(item, index) in dispute.conversations"
              :key="`${item.createTime}-${index}`"
              class="rounded-lg border border-warm-100 p-3 bg-warm-50/40"
            >
              <div class="flex items-center justify-between gap-2">
                <div class="text-sm font-medium text-slate-700">
                  {{ item.userName || (item.initiator ? '发起方' : '被投诉方') }}
                </div>
                <div class="text-xs text-slate-400">{{ formatTime(item.createTime) }}</div>
              </div>
              <p v-if="item.content" class="text-sm text-slate-600 whitespace-pre-wrap mt-2 leading-6">{{ item.content }}</p>
              <div v-if="item.evidenceUrls && item.evidenceUrls.length > 0" class="grid grid-cols-4 gap-2 mt-2">
                <button
                  v-for="(url, evidenceIndex) in item.evidenceUrls"
                  :key="`${url}-${evidenceIndex}`"
                  class="aspect-square rounded-md overflow-hidden border border-slate-200"
                  @click="openPreview(item.evidenceUrls || [], evidenceIndex)"
                >
                  <img :src="url" class="w-full h-full object-cover" alt="补充证据" />
                </button>
              </div>
            </article>
          </div>
        </section>

        <section v-if="canShowReplyForm" class="um-card p-4">
          <h3 class="font-medium text-slate-700 mb-3">补充说明（最多3次）</h3>
          <textarea
            v-model="replyForm.additionalContent"
            maxlength="500"
            rows="4"
            placeholder="补充你的观点或说明"
            class="w-full p-3 border border-warm-200 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-warm-500"
          />
          <p class="text-xs text-slate-400 mt-1 text-right">{{ replyForm.additionalContent.length }}/500</p>

          <div class="mt-3 flex items-center gap-2 flex-wrap">
            <div
              v-for="(url, index) in replyForm.additionalEvidence"
              :key="`${url}-${index}`"
              class="relative"
            >
              <img :src="url" class="w-16 h-16 rounded-md object-cover border border-slate-200" alt="补充证据" />
              <button
                class="absolute -top-2 -right-2 w-5 h-5 rounded-full bg-red-500 text-white flex items-center justify-center"
                @click="removeReplyEvidence(index)"
              >
                <X class="w-3 h-3" />
              </button>
            </div>
            <label
              v-if="replyForm.additionalEvidence.length < 9"
              class="w-16 h-16 rounded-md border border-dashed border-warm-200 bg-warm-50/40 flex items-center justify-center cursor-pointer hover:bg-warm-50 transition-colors"
            >
              <Upload class="w-4 h-4 text-slate-400" />
              <input type="file" accept="image/*" multiple class="hidden" @change="handleReplyUpload" />
            </label>
          </div>

          <button
            class="mt-3 w-full py-2.5 rounded-lg bg-warm-500 text-white disabled:opacity-60"
            :disabled="replying || uploadCount > 0"
            @click="handleReply"
          >
            <span class="inline-flex items-center gap-1">
              <Send class="w-4 h-4" />
              {{ replying ? '提交中...' : '提交补充' }}
            </span>
          </button>
        </section>

        <section v-if="dispute.handleResult" class="um-card p-4">
          <h3 class="font-medium text-slate-700 mb-2 flex items-center gap-2">
            <CheckCircle class="w-4 h-4" />
            处理结果
          </h3>
          <p class="text-slate-600 whitespace-pre-wrap leading-7">{{ dispute.handleResult }}</p>
          <p class="text-xs text-slate-400 mt-2">处理时间：{{ formatTime(dispute.handleTime) }}</p>
        </section>

        <section class="um-card p-4 text-sm text-slate-500 space-y-1">
          <p>发起时间：{{ formatTime(dispute.createTime) }}</p>
          <p>更新时间：{{ formatTime(dispute.updateTime) }}</p>
        </section>

        <section v-if="dispute.canWithdraw" class="um-card p-3">
          <button
            @click="handleWithdraw"
            class="w-full py-3 bg-warm-50 border border-warm-200 text-warm-700 rounded-xl font-medium hover:bg-warm-100 transition-colors"
          >
            撤回纠纷
          </button>
        </section>
      </div>
    </template>

    <div v-else class="py-20 text-center text-slate-500">
      纠纷记录不存在或已被删除
    </div>

    <div
      v-if="showPreview && currentPreviewImage"
      class="fixed inset-0 z-[70] bg-black/90 backdrop-blur-sm p-4 md:p-8"
      @click.self="closePreview"
    >
      <button
        class="absolute top-4 right-4 z-10 inline-flex items-center gap-1.5 px-3 py-2 rounded-full bg-black/55 text-white hover:bg-black/70 border border-white/25 transition-colors"
        @click.stop.prevent="closePreview"
      >
        <X :size="20" />
        <span class="text-xs font-medium">关闭</span>
      </button>

      <div class="h-full w-full flex items-center justify-center">
        <button
          v-if="previewImages.length > 1"
          class="absolute left-3 md:left-6 p-2 md:p-3 rounded-full bg-white/10 text-white hover:bg-white/20 transition-colors"
          @click.stop.prevent="movePreview(-1)"
        >
          <ChevronLeft :size="22" />
        </button>

        <img :src="currentPreviewImage" class="max-w-full max-h-full object-contain rounded-2xl" alt="图片预览" @click.stop.prevent="closePreview" />

        <button
          v-if="previewImages.length > 1"
          class="absolute right-3 md:right-6 p-2 md:p-3 rounded-full bg-white/10 text-white hover:bg-white/20 transition-colors"
          @click.stop.prevent="movePreview(1)"
        >
          <ChevronRight :size="22" />
        </button>
      </div>

      <button
        class="absolute bottom-4 right-4 z-10 px-3 py-2 rounded-full bg-black/55 text-white text-xs font-medium border border-white/25 hover:bg-black/70 transition-colors"
        @click.stop.prevent="closePreview"
      >
        关闭预览
      </button>
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
</style>
