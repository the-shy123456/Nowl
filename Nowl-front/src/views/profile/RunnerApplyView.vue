<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  AlertCircle,
  ArrowRight,
  BadgeCheck,
  Camera,
  CheckCircle2,
  Clock3,
  CreditCard,
  School,
  Truck,
  User,
} from 'lucide-vue-next'
import { getCampusList, getSchoolList } from '@/api/modules/school'
import { applyRunner, updateUserInfo } from '@/api/modules/user'
import { ElMessage } from '@/utils/feedback'
import { useUserStore } from '@/stores/user'
import type { SchoolInfo } from '@/types'
import { AuthStatus, RunnableStatus } from '@/constants'
import ImageUpload from '@/components/ImageUpload.vue'
import SubPageShell from '@/components/SubPageShell.vue'

const router = useRouter()
const userStore = useUserStore()

const schools = ref<SchoolInfo[]>([])
const campuses = ref<SchoolInfo[]>([])
const loading = ref(false)
const initialLoading = ref(true)

const form = ref({
  userName: '',
  studentNo: '',
  schoolCode: '',
  campusCode: '',
  gender: 1,
  grade: '',
  certImages: [] as string[],
  selfImages: [] as string[],
})

const gradeOptions = ['大一', '大二', '大三', '大四', '研究生', '博士生']
const runnerStatus = computed(() => userStore.userInfo?.runnableStatus ?? RunnableStatus.NOT_APPLIED)
const authStatus = computed(() => userStore.userInfo?.authStatus ?? AuthStatus.NOT_SUBMITTED)
const schoolOptions = computed(() =>
  Array.from(new Map(schools.value.map(item => [item.schoolCode, item])).values()),
)

const runnerStatusMeta = computed(() => {
  if (runnerStatus.value === RunnableStatus.PENDING) {
    return {
      icon: Clock3,
      title: '申请审核中',
      hint: '申请已提交，平台正在审核资料',
      className: 'bg-amber-50 border-amber-200 text-amber-700',
    }
  }
  if (runnerStatus.value === RunnableStatus.APPROVED) {
    return {
      icon: CheckCircle2,
      title: '已成为跑腿员',
      hint: '你已通过审核，可在跑腿页接单',
      className: 'bg-emerald-50 border-emerald-200 text-emerald-700',
    }
  }
  if (runnerStatus.value === RunnableStatus.REJECTED) {
    return {
      icon: AlertCircle,
      title: '申请未通过',
      hint: '请补充或修正资料后重新提交',
      className: 'bg-red-50 border-red-200 text-red-600',
    }
  }
  return {
    icon: AlertCircle,
    title: '未申请',
    hint: '完成申请后可接校园跑腿任务',
    className: 'bg-slate-100 border-slate-200 text-slate-600',
  }
})

const canSubmit = computed(() =>
  authStatus.value === AuthStatus.APPROVED
  && runnerStatus.value !== RunnableStatus.PENDING
  && runnerStatus.value !== RunnableStatus.APPROVED,
)

const fetchSchools = async () => {
  try {
    schools.value = await getSchoolList()
  } catch {
    ElMessage.error('加载学校列表失败')
  }
}

const handleSchoolChange = async () => {
  form.value.campusCode = ''
  campuses.value = []
  if (!form.value.schoolCode) return
  try {
    campuses.value = await getCampusList(form.value.schoolCode)
  } catch {
    ElMessage.error('加载校区列表失败')
  }
}

const hydrateFromUser = async () => {
  const info = userStore.userInfo
  if (!info) {
    initialLoading.value = false
    return
  }
  form.value.userName = info.userName || ''
  form.value.studentNo = info.studentNo || ''
  form.value.schoolCode = info.schoolCode || ''
  form.value.campusCode = info.campusCode || ''
  form.value.gender = info.gender ?? 1
  form.value.grade = info.grade || ''
  form.value.certImages = info.certImage ? [info.certImage] : []
  form.value.selfImages = info.selfImage ? [info.selfImage] : []

  if (form.value.schoolCode) {
    await handleSchoolChange()
    if (!campuses.value.some(item => item.campusCode === form.value.campusCode)) {
      form.value.campusCode = ''
    }
  }

  initialLoading.value = false
}

const validateForm = () => {
  if (!form.value.userName || !form.value.studentNo || !form.value.schoolCode || !form.value.campusCode || !form.value.grade) {
    ElMessage.warning('请填写完整的基本信息')
    return false
  }
  if (form.value.certImages.length === 0) {
    ElMessage.warning('请上传证件照片')
    return false
  }
  return true
}

const handleSubmit = async () => {
  if (authStatus.value !== AuthStatus.APPROVED) {
    ElMessage.warning('请先完成校园认证')
    router.push('/profile/auth')
    return
  }
  if (!canSubmit.value) return
  if (!validateForm()) return

  loading.value = true
  try {
    await updateUserInfo({
      userName: form.value.userName,
      studentNo: form.value.studentNo,
      schoolCode: form.value.schoolCode,
      campusCode: form.value.campusCode,
      gender: form.value.gender,
      grade: form.value.grade,
      certImage: form.value.certImages[0],
      selfImage: form.value.selfImages[0] || undefined,
    })
    await applyRunner()
    await userStore.fetchUserInfo()
    ElMessage.success('跑腿员申请已提交，请等待审核')
    router.push('/errands')
  } catch {
    ElMessage.error('提交失败，请重试')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchSchools()
  await hydrateFromUser()
})
</script>

<template>
  <SubPageShell title="跑腿员申请" subtitle="通过审核后可接单并获得任务收益" back-to="/errands" max-width="lg" :use-card="false">
    <template #icon>
      <Truck class="text-white w-8 h-8" stroke-width="2.5" />
    </template>

    <div v-if="initialLoading" class="flex justify-center py-16">
      <div class="animate-spin rounded-full h-10 w-10 border-4 border-warm-500 border-t-transparent"></div>
    </div>

    <div v-else class="space-y-4 pb-4">
      <section class="um-card p-4 border" :class="runnerStatusMeta.className">
        <div class="flex items-start gap-3">
          <component :is="runnerStatusMeta.icon" :size="20" class="mt-0.5 shrink-0" />
          <div>
            <p class="font-semibold">当前状态：{{ runnerStatusMeta.title }}</p>
            <p class="text-sm mt-1 opacity-90">{{ runnerStatusMeta.hint }}</p>
          </div>
        </div>
      </section>

      <section
        v-if="authStatus !== AuthStatus.APPROVED"
        class="um-card p-4 border border-amber-200 bg-amber-50 text-amber-800 text-sm"
      >
        你还未完成校园认证，需先通过认证后才能申请跑腿员。
        <button
          class="ml-2 text-warm-700 underline"
          @click="router.push('/profile/auth')"
        >
          去认证
        </button>
      </section>

      <section class="um-card p-5 space-y-4">
        <h3 class="text-base font-bold text-slate-800 inline-flex items-center gap-2">
          <User :size="18" class="text-warm-500" />
          基本信息
        </h3>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <label class="space-y-1.5">
            <span class="text-xs text-slate-500">真实姓名</span>
            <input
              v-model="form.userName"
              class="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 outline-none focus:border-warm-300 focus:bg-white"
              placeholder="请输入姓名"
            />
          </label>
          <label class="space-y-1.5">
            <span class="text-xs text-slate-500">学号/工号</span>
            <input
              v-model="form.studentNo"
              class="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 outline-none focus:border-warm-300 focus:bg-white"
              placeholder="请输入学号"
            />
          </label>
        </div>

        <div class="space-y-1.5">
          <span class="text-xs text-slate-500">性别</span>
          <div class="grid grid-cols-2 gap-2">
            <button
              type="button"
              class="rounded-xl border px-3 py-2.5 text-sm font-semibold transition-colors"
              :class="form.gender === 1 ? 'border-warm-300 bg-warm-50 text-warm-700' : 'border-slate-200 bg-slate-50 text-slate-600'"
              @click="form.gender = 1"
            >
              男生
            </button>
            <button
              type="button"
              class="rounded-xl border px-3 py-2.5 text-sm font-semibold transition-colors"
              :class="form.gender === 2 ? 'border-warm-300 bg-warm-50 text-warm-700' : 'border-slate-200 bg-slate-50 text-slate-600'"
              @click="form.gender = 2"
            >
              女生
            </button>
          </div>
        </div>
      </section>

      <section class="um-card p-5 space-y-4">
        <h3 class="text-base font-bold text-slate-800 inline-flex items-center gap-2">
          <School :size="18" class="text-warm-500" />
          学籍信息
        </h3>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <label class="space-y-1.5">
            <span class="text-xs text-slate-500">所属学校</span>
            <select
              v-model="form.schoolCode"
              class="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 outline-none focus:border-warm-300 focus:bg-white"
              @change="handleSchoolChange"
            >
              <option value="">请选择学校</option>
              <option v-for="item in schoolOptions" :key="item.schoolCode" :value="item.schoolCode">
                {{ item.schoolName }}
              </option>
            </select>
          </label>

          <label class="space-y-1.5">
            <span class="text-xs text-slate-500">所属校区</span>
            <select
              v-model="form.campusCode"
              :disabled="!form.schoolCode"
              class="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 outline-none focus:border-warm-300 focus:bg-white disabled:opacity-60"
            >
              <option value="">请选择校区</option>
              <option v-for="item in campuses" :key="item.campusCode" :value="item.campusCode">
                {{ item.campusName }}
              </option>
            </select>
          </label>
        </div>

        <div class="space-y-1.5">
          <span class="text-xs text-slate-500">当前年级</span>
          <div class="grid grid-cols-3 gap-2">
            <button
              v-for="item in gradeOptions"
              :key="item"
              type="button"
              class="rounded-xl border px-2 py-2 text-sm font-medium transition-colors"
              :class="form.grade === item ? 'border-warm-300 bg-warm-50 text-warm-700' : 'border-slate-200 bg-slate-50 text-slate-600'"
              @click="form.grade = item"
            >
              {{ item }}
            </button>
          </div>
        </div>
      </section>

      <section class="um-card p-5 space-y-4">
        <h3 class="text-base font-bold text-slate-800 inline-flex items-center gap-2">
          <BadgeCheck :size="18" class="text-warm-500" />
          证件验证
        </h3>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div class="space-y-1.5">
            <p class="text-xs text-slate-500 inline-flex items-center gap-1">
              <CreditCard :size="13" />
              证件照（必填）
            </p>
            <div class="rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-4">
              <ImageUpload v-model="form.certImages" :max-count="1" :max-size="5" />
            </div>
          </div>
          <div class="space-y-1.5">
            <p class="text-xs text-slate-500 inline-flex items-center gap-1">
              <Camera :size="13" />
              本人照片（选填）
            </p>
            <div class="rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-4">
              <ImageUpload v-model="form.selfImages" :max-count="1" :max-size="5" />
            </div>
          </div>
        </div>
      </section>

      <section class="um-card p-4">
        <button
          @click="handleSubmit"
          :disabled="loading || !canSubmit"
          class="w-full rounded-xl bg-gradient-to-r from-warm-500 to-orange-500 text-white font-semibold py-3.5 hover:from-warm-600 hover:to-orange-600 transition-colors disabled:opacity-60 inline-flex items-center justify-center gap-1.5"
        >
          <span>{{ loading ? '提交中...' : '确认提交申请' }}</span>
          <ArrowRight v-if="!loading" :size="16" />
        </button>
        <p class="mt-3 text-center text-xs text-slate-400">提交即表示你同意平台服务协议与跑腿规范</p>
      </section>
    </div>
  </SubPageShell>
</template>
