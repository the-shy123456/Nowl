<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { MapPin, DollarSign, Clock, Send, Truck, Loader2 } from 'lucide-vue-next'
import { publishErrand, updateErrand, getErrandDetail } from '@/api/modules/errand'
import { getCampusList } from '@/api/modules/school'
import { useUserStore } from '@/stores/user'
import { ElMessage } from '@/utils/feedback'
import type { SchoolInfo } from '@/types'
import SubPageShell from '@/components/SubPageShell.vue'
import ImageUpload from '@/components/ImageUpload.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const initialLoading = ref(false)
const campuses = ref<SchoolInfo[]>([])

const hasEditTaskParam = computed(() => route.params.taskId !== undefined)
const editTaskId = computed(() => {
  if (!hasEditTaskParam.value) return null
  const parsed = Number(route.params.taskId)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
})
const isEditMode = computed(() => hasEditTaskParam.value)

const form = ref({
  title: '',
  description: '',
  images: [] as string[],
  pickupAddress: '',
  deliveryAddress: '',
  reward: '',
  deadline: '',
  remark: '',
  schoolCode: '',
  campusCode: '',
})

const parseImageList = (raw?: string): string[] => {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) {
      return parsed.filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
    }
  } catch {
    return raw
      .split(',')
      .map(item => item.trim())
      .filter(Boolean)
  }
  return []
}

const schoolDisplayName = computed(() => {
  return userStore.userInfo?.schoolName || form.value.schoolCode || '未设置学校'
})

const loadCampuses = async (schoolCode: string) => {
  if (!schoolCode) {
    campuses.value = []
    return
  }
  try {
    campuses.value = await getCampusList(schoolCode)
  } catch {
    campuses.value = []
  }
}

const loadErrandDetail = async (taskId: number) => {
  initialLoading.value = true
  try {
    const res = await getErrandDetail(taskId)
    form.value = {
      title: res.title || '',
      description: res.description || res.taskContent || '',
      images: parseImageList(res.imageList),
      pickupAddress: res.pickupAddress || '',
      deliveryAddress: res.deliveryAddress || '',
      reward: res.reward ? String(res.reward) : '',
      deadline: res.deadline ? res.deadline.replace(' ', 'T').slice(0, 16) : '',
      remark: res.remark || '',
      schoolCode: res.schoolCode || userStore.userInfo?.schoolCode || '',
      campusCode: res.campusCode || userStore.userInfo?.campusCode || '',
    }
    await loadCampuses(form.value.schoolCode)
    if (
      form.value.campusCode
      && !campuses.value.some(item => item.campusCode === form.value.campusCode)
    ) {
      form.value.campusCode = ''
    }
  } catch {
    ElMessage.error('获取任务详情失败')
    router.push('/profile/my-errands')
  } finally {
    initialLoading.value = false
  }
}

const handlePublish = async () => {
  if (isEditMode.value && !editTaskId.value) {
    ElMessage.error('任务编号无效')
    router.push('/profile/my-errands')
    return
  }
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再发布跑腿任务')
    router.push('/login')
    return
  }
  if (userStore.userInfo?.authStatus !== 2) {
    ElMessage.warning('请先完成实名认证后再发布跑腿任务')
    router.push('/profile/auth')
    return
  }
  if (
    !form.value.title ||
    !form.value.pickupAddress ||
    !form.value.deliveryAddress ||
    !form.value.reward ||
    !form.value.schoolCode ||
    !form.value.campusCode
  ) {
    ElMessage.warning('请填写完整必填信息')
    return
  }

  loading.value = true
  try {
    const formattedDeadline = form.value.deadline
      ? form.value.deadline.replace('T', ' ') + ':00'
      : ''
    const { images, ...formData } = form.value

    const payload = {
      ...formData,
      taskContent: form.value.description || form.value.title,
      reward: Number(form.value.reward),
      deadline: formattedDeadline,
      imageList: JSON.stringify(images),
    }

    if (isEditMode.value) {
      await updateErrand(editTaskId.value!, payload)
      ElMessage.success('修改成功，已重新提交审核')
    } else {
      await publishErrand(payload)
      ElMessage.success('发布成功，已提交审核，结果会通过系统通知告知')
    }
    router.push(isEditMode.value ? '/profile/my-errands' : '/errands')
  } catch {
    ElMessage.error(isEditMode.value ? '修改失败' : '发布失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (isEditMode.value && !editTaskId.value) {
    ElMessage.error('任务编号无效')
    router.replace('/profile/my-errands')
    return
  }
  if (userStore.isLoggedIn && !userStore.userInfo?.schoolCode) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      // ignore
    }
  }
  form.value.schoolCode = userStore.userInfo?.schoolCode || ''
  form.value.campusCode = userStore.currentCampus?.campusCode || userStore.userInfo?.campusCode || ''
  await loadCampuses(form.value.schoolCode)
  if (editTaskId.value) {
    void loadErrandDetail(editTaskId.value)
  }
})
</script>

<template>
  <SubPageShell
    :title="isEditMode ? '编辑跑腿任务' : '发布跑腿任务'"
    :subtitle="isEditMode ? '修改任务信息后保存' : '清晰描述需求，让同学更好帮助你'"
    :back-to="isEditMode ? '/profile/my-errands' : '/errands'"
    max-width="lg"
  >
    <template #icon>
      <Truck class="text-white w-8 h-8" stroke-width="2.5" />
    </template>

    <div v-if="initialLoading" class="flex items-center justify-center py-20">
      <Loader2 class="animate-spin text-warm-500" :size="32" />
    </div>

    <div v-else class="space-y-6">
      <div class="space-y-4">
        <div>
          <label class="text-xs font-bold text-slate-400 uppercase ml-2">任务标题</label>
          <input
            type="text"
            class="w-full mt-1"
            placeholder="例如：帮取快递、代买奶茶..."
            v-model="form.title"
          />
        </div>

        <div>
          <label class="text-xs font-bold text-slate-400 uppercase ml-2">详细描述</label>
          <textarea
            rows="4"
            class="w-full mt-1"
            placeholder="请详细说明任务内容、要求等..."
            v-model="form.description"
          ></textarea>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="text-xs font-bold text-slate-400 uppercase ml-2">学校</label>
            <input
              type="text"
              class="w-full mt-1 bg-slate-100 text-slate-500 cursor-not-allowed"
              :value="schoolDisplayName"
              disabled
            />
          </div>
          <div>
            <label class="text-xs font-bold text-slate-400 uppercase ml-2">校区</label>
            <select v-model="form.campusCode" class="w-full mt-1" :disabled="!form.schoolCode">
              <option value="" disabled>请选择校区</option>
              <option v-for="item in campuses" :key="item.campusCode" :value="item.campusCode">
                {{ item.campusName }}
              </option>
            </select>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="text-xs font-bold text-slate-400 uppercase ml-2">取件/起点地址</label>
            <div class="relative mt-1">
              <MapPin class="absolute left-3 top-3.5 text-warm-400" :size="16" />
              <input
                type="text"
                class="w-full um-input-with-prefix pr-4 py-3 transition-colors"
                placeholder="从哪里取？"
                v-model="form.pickupAddress"
              />
            </div>
          </div>

          <div>
            <label class="text-xs font-bold text-slate-400 uppercase ml-2">送达/终点地址</label>
            <div class="relative mt-1">
              <MapPin class="absolute left-3 top-3.5 text-warm-400" :size="16" />
              <input
                type="text"
                class="w-full um-input-with-prefix pr-4 py-3 transition-colors"
                placeholder="送到哪里？"
                v-model="form.deliveryAddress"
              />
            </div>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="text-xs font-bold text-slate-400 uppercase ml-2">报酬金额 (元)</label>
            <div class="relative mt-1">
              <DollarSign class="absolute left-3 top-3.5 text-slate-400" :size="16" />
              <input
                type="text"
                class="w-full um-input-with-prefix pr-4 py-3 transition-colors"
                placeholder="0.00"
                v-model="form.reward"
              />
            </div>
          </div>

          <div>
            <label class="text-xs font-bold text-slate-400 uppercase ml-2">截止时间</label>
            <div class="relative mt-1">
              <Clock class="absolute left-3 top-3.5 text-slate-400" :size="16" />
              <input
                type="datetime-local"
                class="w-full um-input-with-prefix pr-4 py-3 transition-colors"
                v-model="form.deadline"
              />
            </div>
          </div>
        </div>

        <div>
          <label class="text-xs font-bold text-slate-400 uppercase ml-2">备注信息</label>
          <input
            type="text"
            class="w-full mt-1"
            placeholder="其他需要注意的事项..."
            v-model="form.remark"
          />
        </div>

        <div>
          <label class="text-xs font-bold text-slate-400 uppercase ml-2">任务图片（可选）</label>
          <p class="text-xs text-slate-400 mt-1 ml-2">上传任务相关图片，能帮助跑腿员更快理解需求</p>
          <div class="mt-2">
            <ImageUpload v-model="form.images" :max-count="6" :max-size="5" />
          </div>
        </div>
      </div>

      <div class="pt-4">
        <button
          @click="handlePublish"
          :disabled="loading"
          class="w-full py-4 um-btn um-btn-primary disabled:opacity-50 flex items-center justify-center gap-2"
        >
          <Send v-if="!loading" :size="18" />
          <span v-if="loading">{{ isEditMode ? '保存中...' : '发布中...' }}</span>
          <span v-else>{{ isEditMode ? '保存修改' : '立即发布任务' }}</span>
        </button>
      </div>
    </div>
  </SubPageShell>
</template>
