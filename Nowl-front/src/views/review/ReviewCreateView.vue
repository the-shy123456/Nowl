<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bike, Package, ShieldCheck, Star, User } from 'lucide-vue-next'
import { canReview, createReview } from '@/api/modules/review'
import { getErrandDetail } from '@/api/modules/errand'
import { getOrderDetail } from '@/api/modules/order'
import { ElMessage } from '@/utils/feedback'
import { normalizeMediaUrl } from '@/utils/media'
import { navigateBack } from '@/utils/navigation'
import type { ErrandTask, OrderInfo } from '@/types'
import { RatingMap } from '@/constants/statusMaps'
import SubPageShell from '@/components/SubPageShell.vue'

const router = useRouter()
const route = useRoute()

const targetType = computed(() => (Number(route.query.type) === 1 ? 1 : 0))
const hasValidTargetType = computed(() => {
  const value = Number(route.query.type)
  return value === 0 || value === 1
})
const contentId = computed(() => {
  const value = Number(route.query.id)
  return Number.isFinite(value) && value > 0 ? value : 0
})
const reviewedId = computed(() => {
  const value = Number(route.query.reviewedId)
  return Number.isFinite(value) && value > 0 ? value : 0
})
const reviewFallback = computed(() => (targetType.value === 1 ? '/profile/my-errands' : '/profile/my-orders'))

const loading = ref(false)
const submitting = ref(false)

const orderInfo = ref<OrderInfo | null>(null)
const errandInfo = ref<ErrandTask | null>(null)
const reviewedUser = ref<{ id: number; name: string; avatar?: string } | null>(null)
const canCreateReview = ref(true)
const reviewBlockReason = ref('')

const form = ref({
  rating: 5,
  content: '',
  anonymous: false,
})

const stars = [1, 2, 3, 4, 5]

const reviewSourceTitle = computed(() =>
  targetType.value === 0
    ? (orderInfo.value?.productTitle || '商品交易')
    : (errandInfo.value?.title || '跑腿任务'),
)

const reviewSourceAmount = computed(() =>
  targetType.value === 0 ? orderInfo.value?.totalAmount : errandInfo.value?.reward,
)

const reviewSourceImage = computed(() =>
  normalizeMediaUrl(orderInfo.value?.productImage),
)

const canSubmit = computed(() => !submitting.value && canCreateReview.value)

const isValidParticipantId = (value?: number) => Number.isFinite(value) && Number(value) > 0

const ensureReviewedIdInParticipants = (participantIds: Array<number | undefined>) => {
  const normalized = participantIds
    .filter(isValidParticipantId)
    .map(id => Number(id))
  return normalized.includes(reviewedId.value)
}

const verifyCanCreateReview = async () => {
  try {
    const allowed = await canReview({
      targetType: targetType.value,
      contentId: contentId.value,
      reviewedId: reviewedId.value,
    })
    canCreateReview.value = allowed === true
    reviewBlockReason.value = canCreateReview.value
      ? ''
      : '当前评价条件不满足，可能已评价或订单/任务未完成'
  } catch {
    canCreateReview.value = false
    reviewBlockReason.value = '当前无法校验评价资格，请稍后重试'
  }
}

const fetchContentDetail = async () => {
  if (!hasValidTargetType.value) {
    ElMessage.error('评价类型无效')
    navigateBack(router, route, reviewFallback.value)
    return
  }
  if (!contentId.value || !reviewedId.value) {
    ElMessage.error('评价参数不完整')
    navigateBack(router, route, reviewFallback.value)
    return
  }

  loading.value = true
  try {
    if (targetType.value === 0) {
      const detail = await getOrderDetail(contentId.value)
      if (!ensureReviewedIdInParticipants([detail.buyerId, detail.sellerId])) {
        ElMessage.error('评价对象无效')
        navigateBack(router, route, reviewFallback.value)
        return
      }
      orderInfo.value = detail
      if (reviewedId.value === detail.sellerId) {
        reviewedUser.value = {
          id: detail.sellerId,
          name: detail.sellerName || '卖家',
          avatar: detail.sellerAvatar,
        }
      } else {
        reviewedUser.value = {
          id: detail.buyerId,
          name: detail.buyerName || '买家',
          avatar: detail.buyerAvatar,
        }
      }
      await verifyCanCreateReview()
      return
    }

    const detail = await getErrandDetail(contentId.value)
    if (!ensureReviewedIdInParticipants([detail.publisherId, detail.acceptorId])) {
      ElMessage.error('评价对象无效')
      navigateBack(router, route, reviewFallback.value)
      return
    }
    errandInfo.value = detail
    if (reviewedId.value === detail.acceptorId) {
      reviewedUser.value = {
        id: detail.acceptorId || reviewedId.value,
        name: detail.acceptorName || '跑腿员',
        avatar: detail.acceptorAvatar,
      }
    } else {
      reviewedUser.value = {
        id: detail.publisherId,
        name: detail.publisherName || '发布者',
        avatar: detail.publisherAvatar,
      }
    }
    await verifyCanCreateReview()
  } catch {
    ElMessage.error('获取评价信息失败')
    navigateBack(router, route, reviewFallback.value)
  } finally {
    loading.value = false
  }
}

const setRating = (value: number) => {
  form.value.rating = value
}

const validateReview = () => {
  if (form.value.rating <= 2 && form.value.content.trim().length < 10) {
    ElMessage.warning('差评需填写至少10字理由')
    return false
  }
  return true
}

const handleSubmit = async () => {
  if (!canCreateReview.value) {
    ElMessage.warning(reviewBlockReason.value || '当前不可评价')
    return
  }
  if (!validateReview()) return

  submitting.value = true
  try {
    await createReview({
      orderId: targetType.value === 0 ? contentId.value : undefined,
      taskId: targetType.value === 1 ? contentId.value : undefined,
      targetType: targetType.value,
      reviewedId: reviewedId.value,
      rating: form.value.rating,
      content: form.value.content.trim() || undefined,
      anonymous: form.value.anonymous ? 1 : 0,
    })
    ElMessage.success('评价成功')
    navigateBack(router, route, reviewFallback.value)
  } catch {
    ElMessage.error('评价失败，请重试')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void fetchContentDetail()
})
</script>

<template>
  <SubPageShell title="发表评价" subtitle="真实反馈有助于建立校园信用体系" :back-to="reviewFallback" max-width="lg" :use-card="false">
    <template #icon>
      <Star class="text-white w-8 h-8" stroke-width="2.5" />
    </template>

    <div v-if="loading" class="flex justify-center py-16">
      <div class="animate-spin rounded-full h-10 w-10 border-4 border-warm-500 border-t-transparent"></div>
    </div>

    <div v-else class="space-y-4 pb-4">
      <section class="um-card p-4">
        <div class="flex items-center gap-3">
          <img
            v-if="normalizeMediaUrl(reviewedUser?.avatar)"
            :src="normalizeMediaUrl(reviewedUser?.avatar)"
            class="w-12 h-12 rounded-full object-cover border border-slate-200"
          />
          <div v-else class="w-12 h-12 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center border border-slate-200">
            <User :size="20" />
          </div>
          <div>
            <h3 class="font-semibold text-slate-800">{{ reviewedUser?.name || '对方用户' }}</h3>
            <p class="text-xs text-slate-500">{{ targetType === 0 ? '商品交易评价' : '跑腿服务评价' }}</p>
          </div>
        </div>
      </section>

      <section class="um-card p-4">
        <div class="flex gap-3">
          <div class="w-16 h-16 rounded-xl overflow-hidden bg-slate-100 shrink-0 border border-slate-200">
            <img v-if="reviewSourceImage" :src="reviewSourceImage" class="w-full h-full object-cover" />
            <div v-else class="w-full h-full flex items-center justify-center text-slate-400">
              <component :is="targetType === 0 ? Package : Bike" :size="20" />
            </div>
          </div>
          <div class="min-w-0 flex-1">
            <p class="text-sm font-semibold text-slate-800 truncate">{{ reviewSourceTitle }}</p>
            <p class="mt-1 text-sm text-warm-600 font-bold">¥{{ reviewSourceAmount ?? '--' }}</p>
          </div>
        </div>
      </section>

      <section
        v-if="!canCreateReview"
        class="um-card p-4 border border-red-100 bg-red-50 text-red-600 text-sm"
      >
        {{ reviewBlockReason || '当前不可评价' }}
      </section>

      <section class="um-card p-5">
        <h3 class="font-semibold text-slate-800 mb-3">评分</h3>
        <div class="flex items-center justify-center gap-2">
          <button
            v-for="star in stars"
            :key="star"
            class="p-1 transition-transform hover:scale-105"
            @click="setRating(star)"
          >
            <Star
              class="w-10 h-10"
              :class="star <= form.rating ? 'text-yellow-400 fill-yellow-400' : 'text-slate-200'"
            />
          </button>
        </div>
        <p class="text-center mt-2 text-sm font-medium" :class="RatingMap[form.rating]?.color || 'text-slate-500'">
          {{ RatingMap[form.rating]?.text || '请选择评分' }}
        </p>
      </section>

      <section class="um-card p-5">
        <h3 class="font-semibold text-slate-800 mb-3">
          评价内容
          <span class="text-xs font-normal text-slate-400 ml-1">
            {{ form.rating <= 2 ? '（差评必填，至少10字）' : '（选填）' }}
          </span>
        </h3>
        <textarea
          v-model="form.content"
          maxlength="500"
          placeholder="请描述你的真实交易体验..."
          class="w-full h-28 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 resize-none outline-none focus:border-warm-300 focus:bg-white"
        ></textarea>
        <p class="text-xs text-slate-400 text-right mt-1">{{ form.content.length }}/500</p>
      </section>

      <section class="um-card p-4">
        <label class="flex items-center justify-between gap-3 cursor-pointer">
          <div>
            <p class="text-sm font-medium text-slate-700">匿名评价</p>
            <p class="text-xs text-slate-400 mt-0.5">开启后对方将看不到你的昵称和头像</p>
          </div>
          <div class="relative">
            <input v-model="form.anonymous" type="checkbox" class="sr-only peer" />
            <div class="w-11 h-6 bg-slate-200 rounded-full peer-checked:bg-warm-500 transition-colors"></div>
            <div class="absolute left-1 top-1 w-4 h-4 bg-white rounded-full peer-checked:translate-x-5 transition-transform"></div>
          </div>
        </label>
      </section>

      <section class="um-card p-4 border border-warm-100 bg-warm-50 text-warm-700 text-sm inline-flex items-start gap-2">
        <ShieldCheck :size="16" class="mt-0.5 shrink-0" />
        评分会影响双方信用分，请保持客观公正的评价。
      </section>

      <section class="um-card p-4">
        <button
          @click="handleSubmit"
          :disabled="!canSubmit"
          class="w-full rounded-xl bg-gradient-to-r from-warm-500 to-orange-500 text-white font-semibold py-3.5 hover:from-warm-600 hover:to-orange-600 transition-colors disabled:opacity-60"
        >
          {{ submitting ? '提交中...' : '提交评价' }}
        </button>
      </section>
    </div>
  </SubPageShell>
</template>
