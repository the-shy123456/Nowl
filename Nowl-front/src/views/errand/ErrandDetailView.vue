<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from '@/utils/feedback'
import {
  Truck,
  MapPin,
  Clock,
  User,
  Camera,
  CheckCircle,
  XCircle,
  Star,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Package,
  X,
} from 'lucide-vue-next'
import { getErrandDetail, acceptErrand, deliverErrand, confirmErrand, cancelErrand } from '@/api/modules/errand'
import { hasReviewed } from '@/api/modules/review'
import { uploadFile } from '@/api/modules/file'
import { useUserStore } from '@/stores/user'
import { normalizeMediaUrl } from '@/utils/media'
import type { ErrandTask } from '@/types'
import { ErrandStatusMap } from '@/constants/statusMaps'
import { AuthStatus, ErrandStatus, ReviewStatus, RunnableStatus } from '@/constants'
import dayjs from 'dayjs'
import SubPageShell from '@/components/SubPageShell.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const taskId = computed(() => Number(route.params.taskId))
const loading = ref(true)
const task = ref<ErrandTask | null>(null)
const actionLoading = ref(false)

// 上传凭证相关
const uploadLoading = ref(false)
const evidenceImageUrl = ref('')

// 评价相关
const hasReviewedTask = ref(false)

// 预览相关
const showPreview = ref(false)
const previewImages = ref<string[]>([])
const previewIndex = ref(0)
let previewClosedAt = 0

// 当前用户角色
const isPublisher = computed(() => task.value?.publisherId === userStore.userInfo?.userId)
const isAcceptor = computed(() => task.value?.acceptorId === userStore.userInfo?.userId)
const isParticipant = computed(() => isPublisher.value || isAcceptor.value)

const getReviewStatus = (currentTask?: ErrandTask | null): number | null => {
  if (!currentTask) return null
  const status = Number(currentTask.reviewStatus)
  return Number.isFinite(status) ? status : null
}

const isReviewBlocked = (currentTask?: ErrandTask | null) => {
  const reviewStatus = getReviewStatus(currentTask)
  return reviewStatus === ReviewStatus.PENDING
    || reviewStatus === ReviewStatus.WAIT_MANUAL
    || reviewStatus === ReviewStatus.REJECTED
}

const getTaskStatusInfo = (currentTask?: ErrandTask | null) => {
  if (!currentTask) return { text: '未知', color: 'bg-gray-100 text-gray-500' }
  if (currentTask.taskStatus === ErrandStatus.CANCELLED || currentTask.taskStatus === ErrandStatus.COMPLETED) {
    return ErrandStatusMap[currentTask.taskStatus] || { text: '未知', color: 'bg-gray-100 text-gray-500' }
  }
  const reviewStatus = getReviewStatus(currentTask)
  if (reviewStatus === ReviewStatus.PENDING) {
    return { text: '待审核', color: 'bg-yellow-100 text-yellow-700' }
  }
  if (reviewStatus === ReviewStatus.WAIT_MANUAL) {
    return { text: '待人工复核', color: 'bg-orange-100 text-orange-700' }
  }
  if (reviewStatus === ReviewStatus.REJECTED) {
    return { text: '审核未通过', color: 'bg-red-100 text-red-700' }
  }
  return ErrandStatusMap[currentTask.taskStatus] || { text: '未知', color: 'bg-gray-100 text-gray-500' }
}

// 状态信息
const statusInfo = computed(() => {
  return getTaskStatusInfo(task.value)
})

const actionHint = computed(() => {
  if (!task.value) return ''
  if (task.value.taskStatus === ErrandStatus.CANCELLED) {
    return `任务已取消${task.value.cancelReason ? `：${task.value.cancelReason}` : ''}`
  }

  const reviewStatus = getReviewStatus(task.value)
  if (reviewStatus === ReviewStatus.PENDING) {
    return '任务已提交审核，审核通过后才会开放接单'
  }
  if (reviewStatus === ReviewStatus.WAIT_MANUAL) {
    return `任务待人工复核${task.value.auditReason ? `：${task.value.auditReason}` : ''}`
  }
  if (reviewStatus === ReviewStatus.REJECTED) {
    return `任务审核未通过${task.value.auditReason ? `：${task.value.auditReason}` : ''}，悬赏金额已退回余额，无需再次取消`
  }

  if (task.value.taskStatus === ErrandStatus.PENDING) {
    return isPublisher.value ? '等待跑腿员接单' : '你可以直接接单并开始配送'
  }
  if (task.value.taskStatus === ErrandStatus.IN_PROGRESS) {
    return isAcceptor.value ? '上传送达凭证后可提交完成' : '任务进行中，请留意配送进度'
  }
  if (task.value.taskStatus === ErrandStatus.PENDING_CONFIRM) {
    return isPublisher.value ? '确认收货后任务将完成' : '等待发布者确认完成'
  }
  if (task.value.taskStatus === ErrandStatus.COMPLETED) {
    return '任务已完成，可进行评价'
  }
  return '任务已取消'
})

const parseImageList = (raw?: string): string[] => {
  if (!raw) return []
  const parsedImages: string[] = []

  try {
    const list = JSON.parse(raw)
    if (Array.isArray(list)) {
      list.forEach((item) => {
        if (typeof item === 'string' && item.trim()) {
          parsedImages.push(item.trim())
        }
      })
    }
  } catch {
    raw
      .split(',')
      .map(item => item.trim())
      .filter(Boolean)
      .forEach(item => parsedImages.push(item))
  }

  return Array.from(
    new Set(
      parsedImages
        .map(item => normalizeMediaUrl(item))
        .filter((item): item is string => Boolean(item)),
    ),
  )
}

const taskImages = computed(() => parseImageList(task.value?.imageList))

const evidenceImages = computed(() => {
  const urls: string[] = []
  const normalizedTaskEvidence = normalizeMediaUrl(task.value?.evidenceImage)
  const normalizedUploadingEvidence = normalizeMediaUrl(evidenceImageUrl.value)

  if (normalizedTaskEvidence) urls.push(normalizedTaskEvidence)
  if (normalizedUploadingEvidence) urls.push(normalizedUploadingEvidence)
  return Array.from(new Set(urls))
})

const currentPreviewImage = computed(() => previewImages.value[previewIndex.value] ?? '')

const isRunnerApproved = computed(() =>
  userStore.isLoggedIn
  && userStore.userInfo?.authStatus === AuthStatus.APPROVED
  && userStore.userInfo?.runnableStatus === RunnableStatus.APPROVED,
)

const ensureRunnerApproved = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return false
  }
  if (userStore.userInfo?.authStatus !== AuthStatus.APPROVED) {
    ElMessage.warning('请先完成实名认证')
    router.push('/profile/auth')
    return false
  }

  const status = userStore.userInfo?.runnableStatus ?? RunnableStatus.NOT_APPLIED
  if (status === RunnableStatus.APPROVED) {
    return true
  }
  if (status === RunnableStatus.PENDING) {
    ElMessage.warning('跑腿员申请审核中，请耐心等待')
    return false
  }
  if (status === RunnableStatus.REJECTED) {
    ElMessage.warning('申请未通过，可重新提交')
    router.push('/profile/runner-apply')
    return false
  }

  ElMessage.warning('请先申请成为跑腿员')
  router.push('/profile/runner-apply')
  return false
}

const canAccept = computed(() =>
  task.value?.taskStatus === ErrandStatus.PENDING
  && !isPublisher.value
  && isRunnerApproved.value
  && !isReviewBlocked(task.value),
)
const canDeliver = computed(() => task.value?.taskStatus === ErrandStatus.IN_PROGRESS && isAcceptor.value)
const canConfirm = computed(() => task.value?.taskStatus === ErrandStatus.PENDING_CONFIRM && isPublisher.value)
const canReview = computed(() =>
  task.value?.taskStatus === ErrandStatus.COMPLETED
  && isParticipant.value
  && !hasReviewedTask.value
  && !isReviewBlocked(task.value),
)
const canCancel = computed(() => {
  if (!task.value) return false
  const reviewStatus = getReviewStatus(task.value)
  if (reviewStatus === ReviewStatus.REJECTED) return false
  return (
    (task.value.taskStatus === ErrandStatus.PENDING && isPublisher.value) ||
    (task.value.taskStatus === ErrandStatus.IN_PROGRESS && (isPublisher.value || isAcceptor.value))
  )
})

const timelineItems = computed(() => {
  if (!task.value) return []
  return [
    {
      title: '任务发布',
      time: task.value.createTime,
      detail: '发布者创建任务，等待跑腿员接单',
      done: true,
    },
    {
      title: '跑腿接单',
      time: task.value.acceptTime,
      detail: task.value.acceptTime
        ? `${task.value.acceptorName || '跑腿员'} 已接单`
        : '暂未接单',
      done: Boolean(task.value.acceptTime),
    },
    {
      title: '配送送达',
      time: task.value.deliverTime,
      detail: task.value.deliverTime ? '已提交送达凭证' : '待送达并上传凭证',
      done: Boolean(task.value.deliverTime),
    },
    {
      title: '任务确认',
      time: task.value.confirmTime || task.value.cancelTime,
      detail:
        task.value.taskStatus === ErrandStatus.CANCELLED
          ? `任务已取消${task.value.cancelReason ? `：${task.value.cancelReason}` : ''}`
          : task.value.confirmTime
            ? '发布者已确认任务完成'
            : '待发布者确认',
      done: task.value.taskStatus === ErrandStatus.CANCELLED || Boolean(task.value.confirmTime),
    },
  ]
})

// 24小时倒计时
const autoConfirmCountdown = computed(() => {
  if (!task.value?.deliverTime || task.value.taskStatus !== ErrandStatus.PENDING_CONFIRM) return null
  const autoConfirmTime = dayjs(task.value.deliverTime).add(24, 'hour')
  const diffMinutes = autoConfirmTime.diff(dayjs(), 'minute')
  if (diffMinutes <= 0) return '即将自动确认'
  const hours = Math.floor(diffMinutes / 60)
  const minutes = diffMinutes % 60
  if (hours <= 0) return `${minutes}分钟后自动确认`
  return `${hours}小时${minutes}分钟后自动确认`
})

const formatTime = (time?: string | Date) => {
  if (!time) return '—'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
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

// 加载任务详情
const loadTaskDetail = async () => {
  if (!Number.isFinite(taskId.value) || taskId.value <= 0) {
    ElMessage.error('任务编号无效')
    router.push('/errands')
    return
  }

  try {
    loading.value = true
    const res = await getErrandDetail(taskId.value)
    task.value = res
    if (res?.evidenceImage) {
      evidenceImageUrl.value = res.evidenceImage
    }

    // 如果任务已完成，检查评价状态
    if (task.value?.taskStatus === ErrandStatus.COMPLETED) {
      await checkReviewStatus()
    } else {
      hasReviewedTask.value = false
    }
  } catch (error) {
    console.error('加载跑腿详情失败:', error)
    ElMessage.error('加载跑腿详情失败')
  } finally {
    loading.value = false
  }
}

// 检查评价状态
const checkReviewStatus = async () => {
  try {
    const res = await hasReviewed({ targetType: 1, taskId: taskId.value })
    hasReviewedTask.value = res === true
  } catch {
    hasReviewedTask.value = false
  }
}

// 去评价
const goToReview = () => {
  if (!canReview.value || !task.value) {
    ElMessage.warning('当前任务暂不可评价')
    return
  }
  const reviewedId = isPublisher.value ? task.value.acceptorId : task.value.publisherId
  if (!Number.isFinite(reviewedId) || Number(reviewedId) <= 0) {
    ElMessage.warning('评价对象无效')
    return
  }
  router.push(`/review/create?type=1&id=${task.value.taskId}&reviewedId=${reviewedId}`)
}

const openUserProfile = (userId?: number) => {
  if (!userId) return
  if (userId === userStore.userInfo?.userId) {
    router.push('/profile')
    return
  }
  router.push(`/user/${userId}`)
}

// 接单
const handleAccept = async () => {
  if (!canAccept.value) {
    ElMessage.warning('当前任务状态不可接单')
    return
  }
  if (!ensureRunnerApproved()) return
  try {
    await ElMessageBox.confirm('确定要接单吗？', '确认接单')
    actionLoading.value = true
    await acceptErrand(taskId.value)
    ElMessage.success('接单成功')
    await loadTaskDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('接单失败')
    }
  } finally {
    actionLoading.value = false
  }
}

// 上传凭证
const handleUploadEvidence = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    input.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过5MB')
    input.value = ''
    return
  }

  uploadLoading.value = true
  try {
    const url = await uploadFile(file)
    evidenceImageUrl.value = url
    ElMessage.success('上传成功')
  } catch {
    ElMessage.error('上传失败')
  } finally {
    uploadLoading.value = false
    input.value = ''
  }
}

// 送达
const handleDeliver = async () => {
  if (!canDeliver.value) {
    ElMessage.warning('当前任务状态不可送达')
    return
  }
  if (!evidenceImageUrl.value) {
    ElMessage.warning('请先上传送达凭证')
    return
  }

  try {
    await ElMessageBox.confirm('确认已送达？', '确认送达')
    actionLoading.value = true
    await deliverErrand(taskId.value, evidenceImageUrl.value)
    ElMessage.success('送达成功，等待发布者确认')
    await loadTaskDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  } finally {
    actionLoading.value = false
  }
}

// 确认完成
const handleConfirm = async () => {
  if (!canConfirm.value) {
    ElMessage.warning('当前任务状态不可确认完成')
    return
  }
  try {
    await ElMessageBox.confirm('确认任务已完成？', '确认完成')
    actionLoading.value = true
    await confirmErrand(taskId.value)
    ElMessage.success('任务已完成')
    await loadTaskDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  } finally {
    actionLoading.value = false
  }
}

// 取消任务
const handleCancel = async () => {
  if (!canCancel.value) {
    ElMessage.warning('当前任务状态不可取消')
    return
  }
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入取消原因', '取消任务', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入取消原因',
      inputValidator: (value) => {
        if (!value || value.trim() === '') return '请输入取消原因'
        return true
      },
    })

    actionLoading.value = true
    await cancelErrand(taskId.value, reason)
    ElMessage.success('任务已取消')
    await loadTaskDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  } finally {
    actionLoading.value = false
  }
}

watch(
  () => taskId.value,
  (newId, oldId) => {
    if (newId === oldId || !Number.isFinite(newId) || newId <= 0) return
    void loadTaskDetail()
  },
)

onMounted(() => {
  void loadTaskDetail()
  window.addEventListener('keydown', handlePreviewKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handlePreviewKeydown)
})
</script>

<template>
  <SubPageShell
    :title="task?.title || '跑腿详情'"
    subtitle="查看任务详情与进度，按当前状态完成后续操作"
    back-to="/errands"
    max-width="lg"
    :use-card="false"
  >
    <template #icon>
      <Truck class="text-white w-8 h-8" stroke-width="2.5" />
    </template>

    <div v-if="loading" class="flex justify-center py-20">
      <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-warm-500"></div>
    </div>

    <div v-else-if="task" class="space-y-4">
      <section class="um-card p-5 md:p-6 bg-gradient-to-r from-white to-slate-50 border-slate-200">
        <div class="flex flex-wrap items-start justify-between gap-4">
          <div class="min-w-0 space-y-2">
            <div class="flex items-center gap-2 flex-wrap">
              <span class="px-4 py-1.5 rounded-full text-sm font-bold border border-white/70" :class="statusInfo.color">
                {{ statusInfo.text }}
              </span>
              <span
                v-if="autoConfirmCountdown"
                class="text-xs text-amber-700 bg-amber-50 border border-amber-100 px-3 py-1 rounded-full inline-flex items-center gap-1"
              >
                <Clock :size="12" />
                {{ autoConfirmCountdown }}
              </span>
            </div>
            <h2 class="text-xl md:text-2xl font-bold text-slate-900 leading-tight">{{ task.title }}</h2>
            <p class="text-sm text-slate-500">{{ actionHint }}</p>
          </div>
          <div class="text-right">
            <div class="text-xs text-slate-400">跑腿赏金</div>
            <div class="text-3xl font-black text-warm-600 tracking-tight">¥{{ task.reward }}</div>
          </div>
        </div>
      </section>

      <section
        v-if="task.auditReason && isReviewBlocked(task)"
        class="um-card p-4 border-orange-100 bg-orange-50/60"
      >
        <p class="text-xs font-semibold text-orange-700">审核说明</p>
        <p class="text-sm text-orange-700 mt-1 leading-6">{{ task.auditReason }}</p>
        <p
          v-if="getReviewStatus(task) === ReviewStatus.REJECTED"
          class="text-xs text-orange-600 mt-2"
        >
          审核未通过的任务已自动退回悬赏金额，无需再次取消。
        </p>
      </section>

      <section class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <article class="um-card p-4 border-green-100 bg-green-50/45">
          <div class="flex items-start gap-3">
            <MapPin :size="18" class="text-green-600 mt-0.5" />
            <div class="flex-1 min-w-0">
              <div class="text-xs font-bold text-green-700">取件地址</div>
              <div class="text-sm text-slate-700 mt-1 break-words">{{ task.pickupAddress || '未填写' }}</div>
            </div>
          </div>
        </article>

        <article class="um-card p-4 border-warm-100 bg-warm-50/45">
          <div class="flex items-start gap-3">
            <MapPin :size="18" class="text-warm-600 mt-0.5" />
            <div class="flex-1 min-w-0">
              <div class="text-xs font-bold text-warm-700">送达地址</div>
              <div class="text-sm text-slate-700 mt-1 break-words">{{ task.deliveryAddress || '未填写' }}</div>
            </div>
          </div>
        </article>

        <article class="um-card p-4">
          <div class="flex items-start gap-3">
            <CalendarDays :size="18" class="text-slate-500 mt-0.5" />
            <div class="flex-1">
              <div class="text-xs text-slate-500">发布时间</div>
              <div class="text-sm text-slate-700 mt-1">{{ formatTime(task.createTime) }}</div>
            </div>
          </div>
        </article>

        <article class="um-card p-4">
          <div class="flex items-start gap-3">
            <Clock :size="18" class="text-slate-500 mt-0.5" />
            <div class="flex-1">
              <div class="text-xs text-slate-500">截止时间</div>
              <div class="text-sm text-slate-700 mt-1">{{ formatTime(task.deadline) }}</div>
            </div>
          </div>
        </article>
      </section>

      <section class="um-card p-5">
        <h3 class="text-sm font-bold text-slate-500 uppercase tracking-[0.08em] mb-3">任务描述</h3>
        <p class="text-slate-700 leading-7 whitespace-pre-wrap">
          {{ task.description || task.taskContent || '发布者暂未补充描述' }}
        </p>
      </section>

      <section v-if="taskImages.length > 0" class="um-card p-5">
        <h3 class="text-sm font-bold text-slate-500 uppercase tracking-[0.08em] mb-3">任务图片</h3>
        <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
          <button
            v-for="(img, idx) in taskImages"
            :key="`${img}-${idx}`"
            class="group relative aspect-square rounded-2xl overflow-hidden border border-slate-200 bg-slate-50"
            @click="openPreview(taskImages, idx)"
          >
            <img :src="img" class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105" />
          </button>
        </div>
      </section>

      <section class="um-card p-5">
        <h3 class="text-sm font-bold text-slate-500 uppercase tracking-[0.08em] mb-4">任务进度</h3>
        <div class="space-y-1">
          <div
            v-for="(item, index) in timelineItems"
            :key="item.title"
            class="relative pl-7 pb-5 last:pb-0"
          >
            <span
              class="absolute left-0 top-1.5 w-3 h-3 rounded-full border-2"
              :class="item.done ? 'bg-warm-500 border-warm-500' : 'bg-white border-slate-300'"
            ></span>
            <span
              v-if="index < timelineItems.length - 1"
              class="absolute left-[5px] top-5 bottom-0 w-px bg-slate-200"
            ></span>
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <div class="text-sm font-semibold text-slate-800">{{ item.title }}</div>
                <div class="text-xs text-slate-500 mt-1 break-words">{{ item.detail }}</div>
              </div>
              <div class="text-xs text-slate-400 whitespace-nowrap">{{ formatTime(item.time) }}</div>
            </div>
          </div>
        </div>
      </section>

      <section class="um-card p-5">
        <h3 class="text-sm font-bold text-slate-500 uppercase tracking-[0.08em] mb-3">参与者</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
          <article class="rounded-2xl border border-slate-100 bg-slate-50 p-4">
            <button
              type="button"
              class="w-full flex items-center gap-3 text-left rounded-xl transition-colors"
              :class="task.publisherId ? 'cursor-pointer hover:bg-white/70 px-1 py-1 -m-1' : ''"
              :disabled="!task.publisherId"
              @click="openUserProfile(task.publisherId)"
            >
              <img
                v-if="task.publisherAvatar"
                :src="task.publisherAvatar"
                class="w-11 h-11 rounded-full border border-slate-100 object-cover"
                alt="发布者头像"
              />
              <div v-else class="w-11 h-11 rounded-full bg-warm-100 flex items-center justify-center">
                <User :size="18" class="text-warm-700" />
              </div>
              <div class="min-w-0">
                <div class="text-xs text-slate-500">发布者</div>
                <div class="text-sm font-semibold text-slate-800 truncate">{{ task.publisherName || '未知用户' }}</div>
              </div>
            </button>
          </article>

          <article class="rounded-2xl border border-slate-100 bg-slate-50 p-4">
            <button
              type="button"
              class="w-full flex items-center gap-3 text-left rounded-xl transition-colors"
              :class="task.acceptorId ? 'cursor-pointer hover:bg-white/70 px-1 py-1 -m-1' : ''"
              :disabled="!task.acceptorId"
              @click="openUserProfile(task.acceptorId)"
            >
              <img
                v-if="task.acceptorAvatar"
                :src="task.acceptorAvatar"
                class="w-11 h-11 rounded-full border border-slate-100 object-cover"
                alt="跑腿员头像"
              />
              <div v-else class="w-11 h-11 rounded-full bg-amber-100 flex items-center justify-center">
                <Truck :size="18" class="text-amber-700" />
              </div>
              <div class="min-w-0">
                <div class="text-xs text-slate-500">跑腿员</div>
                <div class="text-sm font-semibold text-slate-800 truncate">{{ task.acceptorName || '暂无' }}</div>
              </div>
            </button>
          </article>
        </div>
      </section>

      <section v-if="evidenceImages.length > 0" class="um-card p-5">
        <h3 class="text-sm font-bold text-slate-500 uppercase tracking-[0.08em] mb-3">送达凭证</h3>
        <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
          <button
            v-for="(img, idx) in evidenceImages"
            :key="`${img}-${idx}`"
            class="group relative aspect-square rounded-2xl overflow-hidden border border-slate-200 bg-slate-50"
            @click="openPreview(evidenceImages, idx)"
          >
            <img :src="img" alt="送达凭证" class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105" />
          </button>
        </div>
      </section>

      <section v-if="task.remark" class="um-card p-5">
        <h3 class="text-sm font-bold text-slate-500 uppercase tracking-[0.08em] mb-3">备注</h3>
        <p class="text-sm text-slate-600 bg-slate-50 rounded-xl p-3">{{ task.remark }}</p>
      </section>

      <section class="um-card p-4 space-y-3">
        <button
          v-if="canAccept"
          @click="handleAccept"
          :disabled="actionLoading"
          class="w-full py-3.5 um-btn um-btn-primary disabled:opacity-50 flex items-center justify-center gap-2"
        >
          <CheckCircle v-if="!actionLoading" :size="18" />
          <span>{{ actionLoading ? '处理中...' : '立即接单' }}</span>
        </button>

        <div v-if="canDeliver" class="space-y-3">
          <div class="rounded-2xl border border-warm-100 bg-warm-50/60 p-4">
            <label class="text-sm font-semibold text-warm-700 mb-2 block">上传送达凭证</label>
            <input
              id="evidence-upload"
              type="file"
              accept="image/*"
              @change="handleUploadEvidence"
              class="hidden"
            />
            <label
              for="evidence-upload"
              class="w-full rounded-xl border-2 border-dashed border-warm-200 bg-white py-3 px-4 text-sm text-warm-700 font-medium flex items-center justify-center gap-2 cursor-pointer hover:bg-warm-50 transition-colors"
            >
              <Camera :size="18" />
              {{ uploadLoading ? '上传中...' : (evidenceImageUrl ? '重新上传凭证' : '选择图片上传') }}
            </label>
            <button
              v-if="evidenceImageUrl"
              class="mt-3 w-full relative rounded-xl overflow-hidden border border-slate-200"
              @click="openPreview([evidenceImageUrl], 0)"
            >
              <img :src="evidenceImageUrl" alt="凭证预览" class="w-full max-h-56 object-cover" />
            </button>
          </div>

          <button
            @click="handleDeliver"
            :disabled="actionLoading || !evidenceImageUrl"
            class="w-full py-3.5 um-btn um-btn-primary disabled:opacity-50 flex items-center justify-center gap-2"
          >
            <Package v-if="!actionLoading" :size="18" />
            <span>{{ actionLoading ? '提交中...' : '确认送达' }}</span>
          </button>
        </div>

        <button
          v-if="canConfirm"
          @click="handleConfirm"
          :disabled="actionLoading"
          class="w-full py-3.5 um-btn um-btn-primary disabled:opacity-50 flex items-center justify-center gap-2"
        >
          <CheckCircle v-if="!actionLoading" :size="18" />
          <span>{{ actionLoading ? '处理中...' : '确认完成' }}</span>
        </button>

        <button
          v-if="canReview"
          @click="goToReview"
          class="w-full py-3.5 rounded-2xl bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold hover:from-amber-600 hover:to-orange-600 transition-colors flex items-center justify-center gap-2"
        >
          <Star :size="18" />
          去评价
        </button>

        <button
          v-if="canCancel"
          @click="handleCancel"
          :disabled="actionLoading"
          class="w-full py-3.5 bg-white border border-red-200 text-red-600 font-semibold rounded-2xl hover:bg-red-50 transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
        >
          <XCircle v-if="!actionLoading" :size="18" />
          <span>{{ actionLoading ? '处理中...' : '取消任务' }}</span>
        </button>

        <p
          v-if="!canAccept && !canDeliver && !canConfirm && !canReview && !canCancel"
          class="text-sm text-slate-400 text-center py-1"
        >
          当前状态暂无可执行操作
        </p>
      </section>
    </div>

    <div v-else class="text-center py-20">
      <p class="text-slate-500">加载失败</p>
    </div>

    <div
      v-if="showPreview && currentPreviewImage"
      class="fixed inset-0 z-[70] bg-black/90 backdrop-blur-sm p-4 md:p-8"
      @click.self="closePreview"
    >
      <button
        class="absolute top-4 right-4 z-10 inline-flex items-center gap-1.5 px-3 py-2 rounded-full bg-black/55 text-white hover:bg-black/70 border border-white/25 transition-colors"
        @click.stop.prevent="closePreview"
        aria-label="关闭预览"
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


