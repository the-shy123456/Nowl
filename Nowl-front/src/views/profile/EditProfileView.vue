<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Calendar, Camera, Phone, School, User, Users } from 'lucide-vue-next'
import { getCurrentUserInfo, type UpdateUserInfoDTO, updateUserInfo } from '@/api/modules/user'
import { uploadFile } from '@/api/modules/file'
import { getCampusList, getSchoolList } from '@/api/modules/school'
import { useUserStore } from '@/stores/user'
import { ElMessage } from '@/utils/feedback'
import { normalizeMediaUrl } from '@/utils/media'
import { navigateBack } from '@/utils/navigation'
import type { SchoolInfo } from '@/types'
import SubPageShell from '@/components/SubPageShell.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const uploadLoading = ref(false)

const schoolList = ref<SchoolInfo[]>([])
const campusList = ref<SchoolInfo[]>([])

const form = ref({
  nickName: '',
  imageUrl: '',
  gender: 0,
  grade: '',
  phone: '',
  schoolCode: '',
  campusCode: '',
})

const originalForm = ref({ ...form.value })

const genderOptions = [
  { value: 0, label: '未设置' },
  { value: 1, label: '男生' },
  { value: 2, label: '女生' },
]

const gradeOptions = [
  '大一', '大二', '大三', '大四',
  '研一', '研二', '研三',
  '博一', '博二', '博三', '博四', '其他',
]

const schoolOptions = computed(() =>
  Array.from(new Map(schoolList.value.map(item => [item.schoolCode, item])).values()),
)

const displayAvatar = computed(() => normalizeMediaUrl(form.value.imageUrl))

const hasChanges = computed(() => JSON.stringify(form.value) !== JSON.stringify(originalForm.value))

const schoolChanged = computed(() =>
  form.value.schoolCode !== originalForm.value.schoolCode
  || form.value.campusCode !== originalForm.value.campusCode,
)

const loadSchools = async () => {
  try {
    schoolList.value = await getSchoolList()
  } catch {
    ElMessage.error('加载学校列表失败')
  }
}

const loadCampuses = async (schoolCode: string) => {
  if (!schoolCode) {
    campusList.value = []
    return
  }
  try {
    campusList.value = await getCampusList(schoolCode)
  } catch {
    campusList.value = []
    ElMessage.error('加载校区列表失败')
  }
}

const loadUserInfo = async () => {
  loading.value = true
  try {
    const user = await getCurrentUserInfo()
    form.value = {
      nickName: user.nickName || '',
      imageUrl: user.imageUrl || '',
      gender: user.gender || 0,
      grade: user.grade || '',
      phone: user.phone || '',
      schoolCode: user.schoolCode || '',
      campusCode: user.campusCode || '',
    }
    originalForm.value = { ...form.value }
    if (form.value.schoolCode) {
      await loadCampuses(form.value.schoolCode)
      if (!campusList.value.some(item => item.campusCode === form.value.campusCode)) {
        form.value.campusCode = ''
      }
    }
  } catch {
    ElMessage.error('加载用户信息失败')
  } finally {
    loading.value = false
  }
}

const handleAvatarUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
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
    form.value.imageUrl = url || ''
    ElMessage.success('头像上传成功')
  } catch {
    ElMessage.error('头像上传失败，请重试')
  } finally {
    uploadLoading.value = false
    input.value = ''
  }
}

const handleSchoolChange = async () => {
  form.value.campusCode = ''
  await loadCampuses(form.value.schoolCode)
}

const handleSave = async () => {
  const nickName = form.value.nickName.trim()
  if (!nickName) {
    ElMessage.warning('请输入昵称')
    return
  }
  if (nickName.length > 20) {
    ElMessage.warning('昵称不能超过20个字符')
    return
  }

  if (schoolChanged.value && (!form.value.schoolCode || !form.value.campusCode)) {
    ElMessage.warning('修改学校后请完整选择学校和校区')
    return
  }

  saving.value = true
  try {
    const payload: UpdateUserInfoDTO = {
      nickName,
      avatar: form.value.imageUrl || undefined,
      gender: form.value.gender,
      grade: form.value.grade || undefined,
    }

    if (schoolChanged.value) {
      payload.schoolCode = form.value.schoolCode
      payload.campusCode = form.value.campusCode
      payload.authStatus = 0
    }

    await updateUserInfo(payload)
    await userStore.fetchUserInfo()

    ElMessage.success('保存成功')
    if (schoolChanged.value) {
      ElMessage.info('学校信息已更新，请重新进行校园认证')
    }
    navigateBack(router, route, '/profile')
  } catch {
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadSchools()
  await loadUserInfo()
})
</script>

<template>
  <SubPageShell title="编辑资料" subtitle="完善个人信息，提升交易可信度" back-to="/profile" max-width="lg" :use-card="false">
    <div v-if="loading" class="flex justify-center py-16">
      <div class="animate-spin rounded-full h-10 w-10 border-4 border-warm-500 border-t-transparent"></div>
    </div>

    <div v-else class="space-y-4 pb-4">
      <section class="um-card p-5 flex flex-col items-center">
        <label class="relative cursor-pointer group">
          <input type="file" accept="image/*" class="hidden" @change="handleAvatarUpload" />
          <div class="w-24 h-24 rounded-full overflow-hidden border border-slate-200 bg-slate-100">
            <img v-if="displayAvatar" :src="displayAvatar" class="w-full h-full object-cover" />
            <div v-else class="w-full h-full flex items-center justify-center text-slate-400">
              <User :size="36" />
            </div>
          </div>
          <div class="absolute -right-1 -bottom-1 w-8 h-8 rounded-full bg-warm-500 text-white flex items-center justify-center border-2 border-white group-hover:bg-warm-600 transition-colors">
            <Camera :size="14" />
          </div>
          <div
            v-if="uploadLoading"
            class="absolute inset-0 rounded-full bg-black/45 flex items-center justify-center"
          >
            <div class="animate-spin rounded-full h-6 w-6 border-2 border-white border-t-transparent"></div>
          </div>
        </label>
        <p class="text-xs text-slate-500 mt-3">点击头像更换图片（最大 5MB）</p>
      </section>

      <section class="um-card p-5 space-y-4">
        <h3 class="text-base font-bold text-slate-800 inline-flex items-center gap-2">
          <User :size="18" class="text-warm-500" />
          基础信息
        </h3>

        <label class="space-y-1.5 block">
          <span class="text-xs text-slate-500">昵称</span>
          <input
            v-model="form.nickName"
            maxlength="20"
            class="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 outline-none focus:border-warm-300 focus:bg-white"
            placeholder="请输入昵称"
          />
        </label>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <label class="space-y-1.5">
            <span class="text-xs text-slate-500 inline-flex items-center gap-1">
              <Users :size="13" />
              性别
            </span>
            <div class="grid grid-cols-3 gap-2">
              <button
                v-for="item in genderOptions"
                :key="item.value"
                type="button"
                class="rounded-xl border px-2 py-2 text-sm font-medium transition-colors"
                :class="form.gender === item.value ? 'border-warm-300 bg-warm-50 text-warm-700' : 'border-slate-200 bg-slate-50 text-slate-600'"
                @click="form.gender = item.value"
              >
                {{ item.label }}
              </button>
            </div>
          </label>

          <label class="space-y-1.5">
            <span class="text-xs text-slate-500 inline-flex items-center gap-1">
              <Calendar :size="13" />
              年级
            </span>
            <select
              v-model="form.grade"
              class="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 outline-none focus:border-warm-300 focus:bg-white"
            >
              <option value="">请选择年级</option>
              <option v-for="item in gradeOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
        </div>

        <div class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5">
          <p class="text-xs text-slate-500 inline-flex items-center gap-1">
            <Phone :size="13" />
            手机号
          </p>
          <p class="text-sm text-slate-700 mt-1">
            {{ form.phone ? form.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '未绑定' }}
            <span class="text-xs text-slate-400 ml-2">如需修改请联系平台客服</span>
          </p>
        </div>
      </section>

      <section class="um-card p-5 space-y-4">
        <h3 class="text-base font-bold text-slate-800 inline-flex items-center gap-2">
          <School :size="18" class="text-warm-500" />
          学校信息
        </h3>
        <p class="text-xs text-amber-600 bg-amber-50 border border-amber-100 rounded-lg px-3 py-2">
          修改学校或校区后，校园认证状态会重置为待认证。
        </p>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <label class="space-y-1.5">
            <span class="text-xs text-slate-500">学校</span>
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
            <span class="text-xs text-slate-500">校区</span>
            <select
              v-model="form.campusCode"
              :disabled="!form.schoolCode"
              class="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 outline-none focus:border-warm-300 focus:bg-white disabled:opacity-60"
            >
              <option value="">请选择校区</option>
              <option v-for="item in campusList" :key="item.campusCode" :value="item.campusCode">
                {{ item.campusName }}
              </option>
            </select>
          </label>
        </div>
      </section>

      <section class="um-card p-4">
        <button
          @click="handleSave"
          :disabled="saving || !hasChanges"
          class="w-full rounded-xl bg-gradient-to-r from-warm-500 to-orange-500 text-white font-semibold py-3.5 hover:from-warm-600 hover:to-orange-600 transition-colors disabled:opacity-60"
        >
          {{ saving ? '保存中...' : '保存修改' }}
        </button>
      </section>
    </div>
  </SubPageShell>
</template>
