<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import logoSvg from '@/assets/logo.svg'
import { ArrowRight, Lock, RefreshCw, ShieldCheck, User } from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'
import * as userApi from '@/api/modules/user'
import { ElMessage } from '@/utils/feedback'
import { hasAdminAccess } from '@/utils/authz'
import SubPageShell from '@/components/SubPageShell.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const phone = ref('')
const password = ref('')
const captcha = ref('')
const captchaUuid = ref('')
const captchaImage = ref('')
const loading = ref(false)

type LoginError = {
  isBusinessError?: boolean
  message?: string
}

const normalizeCaptcha = (value: string) => {
  if (!value) return ''
  if (value.startsWith('data:image') || value.startsWith('http')) return value
  return `data:image/png;base64,${value}`
}

// 获取验证码
const fetchCaptcha = async () => {
  try {
    const uuid = crypto.randomUUID()
    captchaUuid.value = uuid
    const res = await userApi.getCaptcha(uuid)
    captchaImage.value = normalizeCaptcha(res)
  } catch {
    ElMessage.error('获取验证码失败')
  }
}


const handleLogin = async () => {
  if (!phone.value || !password.value) {
    ElMessage.warning('请输入手机号和密码')
    return
  }

  // 简单的验证码校验
  if (!captcha.value) {
    ElMessage.warning('请输入图形码')
    return
  }


  loading.value = true
  try {
    // 登录前先清除旧状态，避免管理员/用户切换时状态残留
    await userStore.logout({ notifyServer: false })

    await userStore.login({
        phone: phone.value, 
        password: password.value,
        code: captcha.value, 
        uuid: captchaUuid.value
    })
    ElMessage.success('欢迎回来')

    const redirectQuery = Array.isArray(route.query.redirect)
      ? route.query.redirect[0]
      : route.query.redirect
    const redirectPath = typeof redirectQuery === 'string' && redirectQuery.startsWith('/')
      ? redirectQuery
      : ''
    const hasRedirectRoute = redirectPath ? router.resolve(redirectPath).matched.length > 0 : false

    // 判断是否为管理员
    if (hasAdminAccess(userStore.userInfo)) {
      router.push(hasRedirectRoute ? redirectPath : '/admin')
    } else {
      router.push(hasRedirectRoute && !redirectPath.startsWith('/admin') ? redirectPath : '/')
    }

  } catch (err) {
    const loginError = err as LoginError
    ElMessage.error(loginError.isBusinessError ? (loginError.message || '登录失败，请检查账号密码') : '登录失败，请检查账号密码')
    fetchCaptcha() // 失败刷新
  } finally {
    loading.value = false
  }
}

const navigateToRegister = () => {
  router.push('/register')
}

const navigateToForgotPassword = () => {
  router.push('/forgot-password')
}

onMounted(() => {
  fetchCaptcha()
})
</script>

<template>
  <SubPageShell title="欢迎回来" subtitle="使用手机号、密码和图形码登录" back-to="/" max-width="sm">
    <template #icon>
      <img :src="logoSvg" alt="Nowl" class="w-12 h-12" />
    </template>

    <form @submit.prevent="handleLogin" class="space-y-4">
      <div class="space-y-2">
        <label class="text-xs font-bold text-slate-500 ml-1">手机号</label>
        <div class="relative">
          <User class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" :size="18" />
          <input
            type="text"
            v-model="phone"
            placeholder="请输入手机号"
            class="w-full um-input-with-prefix"
            autocomplete="tel"
          />
        </div>
      </div>

      <div class="space-y-2">
        <label class="text-xs font-bold text-slate-500 ml-1">登录密码</label>
        <div class="relative">
          <Lock class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" :size="18" />
          <input
            type="password"
            v-model="password"
            placeholder="请输入登录密码"
            class="w-full um-input-with-prefix"
            autocomplete="current-password"
          />
        </div>
      </div>

      <div class="space-y-2">
        <label class="text-xs font-bold text-slate-500 ml-1">图形验证码</label>
        <div class="flex items-center gap-3">
          <div class="relative flex-1">
            <ShieldCheck class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" :size="18" />
            <input
              type="text"
              v-model="captcha"
              placeholder="输入图形码"
              class="w-full um-input-with-prefix"
            />
          </div>
          <button
            type="button"
            class="w-32 h-11 bg-white rounded-xl flex items-center justify-center hover:bg-warm-50 transition-colors select-none overflow-hidden border border-warm-100"
            @click="fetchCaptcha"
            title="点击刷新验证码"
            aria-label="刷新图形验证码"
          >
            <img v-if="captchaImage" :src="captchaImage" alt="captcha" class="w-full h-full object-cover" />
            <RefreshCw v-else class="w-5 h-5 text-warm-400 transition-transform duration-500" />
          </button>
        </div>
      </div>

      <button
        type="submit"
        class="w-full mt-4 um-btn um-btn-primary py-3.5 flex items-center justify-center gap-2 group"
        :disabled="loading"
      >
        <span>{{ loading ? '登录中...' : '即刻登录' }}</span>
        <ArrowRight v-if="!loading" class="w-4 h-4 group-hover:translate-x-1 transition-transform" />
      </button>

      <div class="flex items-center justify-between text-xs font-medium text-slate-400">
        <button type="button" @click="navigateToForgotPassword" class="hover:text-warm-600 transition-colors">忘记密码?</button>
        <button type="button" @click="navigateToRegister" class="hover:text-warm-600 transition-colors">新用户注册</button>
      </div>
    </form>
  </SubPageShell>
</template>
