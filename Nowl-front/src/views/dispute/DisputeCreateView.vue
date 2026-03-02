<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Upload, X, AlertTriangle, Package, Bike, CircleDollarSign, ShieldAlert, Image as ImageIcon } from 'lucide-vue-next'
import { createDispute } from '@/api/modules/dispute'
import { getOrderDetail } from '@/api/modules/order'
import { getErrandDetail } from '@/api/modules/errand'
import { uploadFile } from '@/api/modules/file'
import { ElMessage } from '@/utils/feedback'
import { navigateBack } from '@/utils/navigation'
import type { OrderInfo, ErrandTask } from '@/types'
import { DisputeStatus, OrderStatus } from '@/constants'
import { useUserStore } from '@/stores/user'
import SubPageShell from '@/components/SubPageShell.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const disputeFallback = '/dispute/list'

const targetType = computed(() => {
  const value = Number(route.query.type)
  return value === 1 ? 1 : 0
})
const hasValidTargetType = computed(() => {
  const value = Number(route.query.type)
  return value === 0 || value === 1
})
const contentId = computed(() => Number(route.query.id))

const loading = ref(false)
const submitting = ref(false)

const orderInfo = ref<OrderInfo | null>(null)
const errandInfo = ref<ErrandTask | null>(null)

const form = ref({
  content: '',
  evidenceUrls: [] as string[],
  claimSellerCreditPenalty: 1,
  claimRefund: 1,
  claimRefundAmount: '',
})

const uploadingCount = ref(0)
const previewImage = ref('')
const showPreview = ref(false)

const maxRefundAmount = computed(() => {
  return targetType.value === 0
    ? Number(orderInfo.value?.totalAmount || 0)
    : Number(errandInfo.value?.reward || 0)
})

const hasTargetContent = computed(() => {
  return targetType.value === 0 ? Boolean(orderInfo.value) : Boolean(errandInfo.value)
})

const hasOrderActiveDispute = (order: OrderInfo) => {
  const disputeId = Number(order.activeDisputeId)
  if (!Number.isFinite(disputeId) || disputeId <= 0) return false
  const status = Number(order.activeDisputeStatus)
  if (!Number.isFinite(status)) return false
  return status === DisputeStatus.PENDING || status === DisputeStatus.PROCESSING
}

const isOrderRefundPending = (order: OrderInfo) => {
  const status = Number(order.refundStatus)
  return Number.isFinite(status) && status === 1
}

const isOrderEligibleForDispute = computed(() => {
  if (targetType.value !== 0) return true
  const order = orderInfo.value
  if (!order) return false
  return order.orderStatus === OrderStatus.PENDING_RECEIVE
    && !isOrderRefundPending(order)
    && !hasOrderActiveDispute(order)
})

const isErrandParticipant = computed(() => {
  if (targetType.value !== 1) return true
  if (!errandInfo.value) return false
  const userId = userStore.userInfo?.userId
  if (!Number.isFinite(userId) || Number(userId) <= 0) return false
  return userId === errandInfo.value.publisherId || userId === errandInfo.value.acceptorId
})

const hasErrandAcceptor = computed(() => {
  if (targetType.value !== 1) return true
  const acceptorId = Number(errandInfo.value?.acceptorId)
  return Number.isFinite(acceptorId) && acceptorId > 0
})

const isErrandEligibleForDispute = computed(() => {
  if (targetType.value !== 1) return true
  return Boolean(errandInfo.value) && isErrandParticipant.value && hasErrandAcceptor.value
})

const isDisputeEligible = computed(() =>
  targetType.value === 0 ? isOrderEligibleForDispute.value : isErrandEligibleForDispute.value,
)

const disputeEligibilityHint = computed(() => {
  if (targetType.value === 0) {
    return '当前订单暂不满足纠纷条件（需待确认收货，且不能退款处理中或纠纷处理中）。'
  }
  return '当前跑腿暂不满足纠纷条件（需为任务参与方，且任务已被接单）。'
})

const canSubmit = computed(() => {
  return form.value.content.trim().length >= 10
    && !submitting.value
    && uploadingCount.value === 0
    && hasValidTargetType.value
    && isDisputeEligible.value
    && hasTargetContent.value
})

const handleBack = () => {
  navigateBack(router, route, disputeFallback)
}

const openPreview = (url: string) => {
  previewImage.value = url
  showPreview.value = true
}

const fetchContentDetail = async () => {
  if (!hasValidTargetType.value) {
    ElMessage.error('纠纷类型无效')
    handleBack()
    return
  }
  if (!Number.isFinite(contentId.value) || contentId.value <= 0) {
    ElMessage.error('缺少有效的关联ID')
    handleBack()
    return
  }

  loading.value = true
  try {
    if (targetType.value === 0) {
      const res = await getOrderDetail(contentId.value)
      orderInfo.value = res
      return
    }

    const res = await getErrandDetail(contentId.value)
    errandInfo.value = res
  } catch {
    ElMessage.error('获取关联内容失败')
  } finally {
    loading.value = false
  }
}

const handleUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const files = input.files
  if (!files || files.length === 0) return

  if (form.value.evidenceUrls.length + files.length > 9) {
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

    uploadingCount.value++
    try {
      const res = await uploadFile(file)
      if (res) {
        form.value.evidenceUrls.push(res)
      }
    } catch {
      ElMessage.error('图片上传失败')
    } finally {
      uploadingCount.value--
    }
  }

  input.value = ''
}

const removeImage = (index: number) => {
  form.value.evidenceUrls.splice(index, 1)
}

const handleSubmit = async () => {
  if (!hasTargetContent.value) {
    ElMessage.warning('关联内容不存在，无法发起纠纷')
    return
  }
  if (!isDisputeEligible.value) {
    ElMessage.warning(disputeEligibilityHint.value)
    return
  }

  if (!form.value.content.trim()) {
    ElMessage.warning('请输入争议内容')
    return
  }

  if (form.value.content.trim().length < 10) {
    ElMessage.warning('争议内容至少10个字符')
    return
  }

  const claimRefundAmount = Number(form.value.claimRefundAmount || 0)
  if (form.value.claimRefund === 1) {
    if (!Number.isFinite(claimRefundAmount) || claimRefundAmount <= 0) {
      ElMessage.warning('请输入有效的申请退款金额')
      return
    }
    if (claimRefundAmount > maxRefundAmount.value) {
      ElMessage.warning('申请退款金额不能超过关联金额')
      return
    }
  }

  submitting.value = true
  try {
    await createDispute({
      contentId: contentId.value,
      targetType: targetType.value,
      content: form.value.content.trim(),
      evidenceUrls: form.value.evidenceUrls.length > 0 ? JSON.stringify(form.value.evidenceUrls) : undefined,
      claimSellerCreditPenalty: form.value.claimSellerCreditPenalty,
      claimRefund: form.value.claimRefund,
      claimRefundAmount: form.value.claimRefund === 1 ? claimRefundAmount : undefined,
    })
    ElMessage.success('纠纷已提交，请等待处理')
    router.replace('/dispute/list')
  } catch {
    ElMessage.error('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void fetchContentDetail()
})
</script>

<template>
  <SubPageShell title="发起纠纷" subtitle="提交争议说明与证据，平台将尽快处理" :back-to="disputeFallback" max-width="lg" :use-card="false">
    <template #icon>
      <AlertTriangle class="text-white w-8 h-8" stroke-width="2.5" />
    </template>

    <div v-if="loading" class="flex justify-center py-20">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-warm-500"></div>
    </div>

    <template v-else>
      <div class="space-y-4">
        <section class="um-card p-4">
          <h3 class="font-medium text-slate-700 mb-3 flex items-center gap-2">
            <component :is="targetType === 0 ? Package : Bike" class="w-4 h-4" />
            {{ targetType === 0 ? '关联订单' : '关联跑腿' }}
          </h3>

          <div v-if="targetType === 0 && orderInfo" class="flex gap-3">
            <div class="w-16 h-16 rounded-lg overflow-hidden bg-warm-50 border border-warm-100 flex-shrink-0">
              <img
                v-if="orderInfo.productImage"
                :src="orderInfo.productImage"
                class="w-full h-full object-cover"
                alt="订单商品图片"
              />
              <Package v-else class="w-full h-full p-4 text-slate-300" />
            </div>
            <div class="flex-1 min-w-0">
              <h4 class="font-medium text-slate-800 line-clamp-1">{{ orderInfo.productTitle || '未知商品' }}</h4>
              <p class="text-sm text-slate-500 mt-1">订单号：{{ orderInfo.orderNo || '--' }}</p>
              <p class="text-sm text-warm-600 font-medium mt-1 inline-flex items-center gap-1">
                <CircleDollarSign :size="14" />
                ¥{{ orderInfo.totalAmount || 0 }}
              </p>
            </div>
          </div>

          <div v-else-if="targetType === 1 && errandInfo" class="flex gap-3">
            <div class="w-16 h-16 rounded-lg overflow-hidden bg-warm-50 border border-warm-100 flex-shrink-0 flex items-center justify-center">
              <Bike class="w-7 h-7 text-slate-300" />
            </div>
            <div class="flex-1 min-w-0">
              <h4 class="font-medium text-slate-800 line-clamp-1">{{ errandInfo.title }}</h4>
              <p class="text-sm text-slate-500 mt-1 line-clamp-1">{{ errandInfo.description || errandInfo.taskContent || '暂无说明' }}</p>
              <p class="text-sm text-warm-600 font-medium mt-1 inline-flex items-center gap-1">
                <CircleDollarSign :size="14" />
                ¥{{ errandInfo.reward || 0 }}
              </p>
            </div>
          </div>

          <p
            v-if="targetType === 0 && orderInfo && !isOrderEligibleForDispute"
            class="mt-3 text-xs text-red-600"
          >
            {{ disputeEligibilityHint }}
          </p>
          <p
            v-if="targetType === 1 && errandInfo && !isErrandEligibleForDispute"
            class="mt-3 text-xs text-red-600"
          >
            {{ disputeEligibilityHint }}
          </p>

          <div v-if="!hasTargetContent" class="text-sm text-slate-500">
            关联内容不存在，无法提交纠纷。
          </div>
        </section>

        <section class="um-card p-4">
          <h3 class="font-medium text-slate-700 mb-3 flex items-center gap-2">
            <AlertTriangle class="w-4 h-4" />
            争议内容
          </h3>
          <textarea
            v-model="form.content"
            placeholder="请详细描述你遇到的问题，至少10个字符..."
            class="w-full h-36 p-3 border border-warm-200 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-warm-500 focus:border-transparent"
            maxlength="1000"
          ></textarea>
          <p class="text-xs text-slate-400 text-right mt-1">{{ form.content.length }}/1000</p>
        </section>

        <section class="um-card p-4">
          <h3 class="font-medium text-slate-700 mb-3 flex items-center gap-2">
            <ShieldAlert class="w-4 h-4" />
            诉求项
          </h3>

          <label class="flex items-center justify-between py-2 text-sm text-slate-700">
            <span>申请扣除卖家信用分</span>
            <input v-model="form.claimSellerCreditPenalty" type="checkbox" :true-value="1" :false-value="0" />
          </label>

          <label class="flex items-center justify-between py-2 text-sm text-slate-700">
            <span>申请退还金额</span>
            <input v-model="form.claimRefund" type="checkbox" :true-value="1" :false-value="0" />
          </label>

          <div v-if="form.claimRefund === 1" class="mt-2">
            <input
              v-model="form.claimRefundAmount"
              type="number"
              min="0"
              step="0.01"
              placeholder="请输入退款金额"
              class="w-full p-3 border border-warm-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-warm-500"
            />
            <p class="mt-1 text-xs text-slate-400">
              最大可申请金额：¥{{ maxRefundAmount }}
            </p>
          </div>
        </section>

        <section class="um-card p-4">
          <h3 class="font-medium text-slate-700 mb-3 flex items-center gap-2">
            <ImageIcon class="w-4 h-4" />
            证据图片
            <span class="text-xs text-slate-400 font-normal">(选填，最多9张)</span>
          </h3>

          <div class="flex flex-wrap gap-2">
            <button
              v-for="(url, index) in form.evidenceUrls"
              :key="`${url}-${index}`"
              class="relative w-20 h-20 rounded-lg overflow-hidden border border-slate-200"
              @click="openPreview(url)"
            >
              <img :src="url" class="w-full h-full object-cover" alt="证据图片" />
              <span
                class="absolute -top-2 -right-2 w-5 h-5 bg-red-500 text-white rounded-full flex items-center justify-center"
                @click.stop="removeImage(index)"
              >
                <X class="w-3 h-3" />
              </span>
            </button>

            <div
              v-for="i in uploadingCount"
              :key="'loading-' + i"
              class="w-20 h-20 rounded-lg bg-warm-50 border border-warm-100 flex items-center justify-center"
            >
              <div class="animate-spin rounded-full h-5 w-5 border-b-2 border-warm-500"></div>
            </div>

            <label
              v-if="form.evidenceUrls.length < 9"
              class="w-20 h-20 rounded-lg border-2 border-dashed border-warm-200 bg-warm-50/40 flex flex-col items-center justify-center cursor-pointer hover:border-warm-500 hover:bg-warm-50 transition-colors"
            >
              <Upload class="w-6 h-6 text-slate-400" />
              <span class="text-xs text-slate-400 mt-1">上传</span>
              <input
                type="file"
                accept="image/*"
                multiple
                class="hidden"
                @change="handleUpload"
              />
            </label>
          </div>
        </section>

        <section class="rounded-xl bg-yellow-50 border border-yellow-100 p-3 text-sm text-yellow-700">
          <AlertTriangle class="w-4 h-4 inline mr-1" />
          请如实描述问题并提供相关证据，恶意投诉可能影响账号信用。
        </section>

        <section class="um-card p-3">
          <button
            @click="handleSubmit"
            :disabled="!canSubmit"
            class="w-full py-3 um-btn um-btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ submitting ? '提交中...' : '提交纠纷' }}
          </button>
        </section>
      </div>
    </template>

    <div
      v-if="showPreview && previewImage"
      class="fixed inset-0 bg-black/90 z-[70] flex items-center justify-center p-6"
      @click.self="showPreview = false"
    >
      <img :src="previewImage" class="max-w-full max-h-full object-contain rounded-xl" alt="证据预览" />
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
