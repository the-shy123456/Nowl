<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import logoSvg from '@/assets/logo.svg'
import { Key, Lock, Phone, UserPlus } from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'
import { ElMessage } from '@/utils/feedback'
import SubPageShell from '@/components/SubPageShell.vue'

import * as userApi from '@/api/modules/user'

const router = useRouter()
const userStore = useUserStore()

const phone = ref('')
const phoneInputRef = ref<HTMLInputElement | null>(null)
const password = ref('')
const confirmPassword = ref('')
const nickName = ref('')
const smsCode = ref('')
const loading = ref(false)
const counting = ref(false)
const count = ref(60)

const sendSmsCode = async () => {
  if (counting.value) return
  if (!phone.value.trim()) {
    phoneInputRef.value?.focus()
    return
  }
  
  try {
    await userApi.sendSms(phone.value)
    counting.value = true
    ElMessage.success('验证码已发送')
    
    const timer = setInterval(() => {
      count.value--
      if (count.value <= 0) {
        clearInterval(timer)
        counting.value = false
        count.value = 60
      }
    }, 1000)
  } catch {
    ElMessage.error('发送失败，请重试')
  }
}

const handleRegister = async () => {
  if (!phone.value || !password.value || !confirmPassword.value || !nickName.value || !smsCode.value) {
    ElMessage.warning('请填写完整信息')
    return
  }

  if (password.value !== confirmPassword.value) {
    ElMessage.warning('两次密码输入不一致')
    return
  }

  loading.value = true
  try {
    await userStore.register({
      phone: phone.value,
      password: password.value,
      nickName: nickName.value,
      code: smsCode.value
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    ElMessage.error('注册失败，请重试')
  } finally {
    loading.value = false
  }
}

const navigateToLogin = () => {
  router.push('/login')
}
</script>

<template>
  <SubPageShell title="创建账号" subtitle="注册后即可发布与接单" back-to="/login" max-width="sm">
    <template #icon>
      <img :src="logoSvg" alt="Nowl" class="w-12 h-12" />
    </template>

    <form @submit.prevent="handleRegister" class="space-y-4">
      <div class="space-y-2">
        <label class="text-xs font-bold text-slate-500 ml-1">手机号</label>
        <div class="relative">
          <Phone class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" :size="18" />
          <input
            type="text"
            v-model="phone"
            ref="phoneInputRef"
            placeholder="请输入手机号"
            class="w-full um-input-with-prefix"
            autocomplete="tel"
          />
        </div>
      </div>

      <div class="space-y-2">
        <label class="text-xs font-bold text-slate-500 ml-1">平台昵称</label>
        <div class="relative">
          <UserPlus class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" :size="18" />
          <input
            type="text"
            v-model="nickName"
            placeholder="设置平台昵称"
            class="w-full um-input-with-prefix"
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
            placeholder="设置登录密码"
            class="w-full um-input-with-prefix"
          />
        </div>
      </div>

      <div class="space-y-2">
        <label class="text-xs font-bold text-slate-500 ml-1">确认密码</label>
        <div class="relative">
          <Lock class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" :size="18" />
          <input
            type="password"
            v-model="confirmPassword"
            placeholder="再次确认密码"
            class="w-full um-input-with-prefix"
          />
        </div>
      </div>

      <div class="space-y-2">
        <label class="text-xs font-bold text-slate-500 ml-1">短信验证码</label>
        <div class="flex items-center gap-3">
          <div class="relative flex-1">
            <Key class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" :size="18" />
            <input
              type="text"
              v-model="smsCode"
              placeholder="输入验证码"
              class="w-full um-input-with-prefix"
            />
          </div>
          <button 
            type="button"
            class="h-11 px-4 bg-white border border-warm-100 rounded-xl flex items-center justify-center cursor-pointer hover:bg-warm-50 transition-colors group select-none text-xs font-bold text-warm-500 min-w-[90px]"
            @click="sendSmsCode"
            :disabled="counting"
          >
            {{ counting ? `${count}s` : '发送验证码' }}
          </button>
        </div>
      </div>

      <button
        type="submit"
        class="w-full mt-4 um-btn um-btn-primary py-3.5 flex items-center justify-center gap-2 group"
        :disabled="loading"
      >
        <span>{{ loading ? '注册中...' : '提交注册信息' }}</span>
      </button>

      <div class="text-center text-xs font-medium text-slate-400">
        <button type="button" @click="navigateToLogin" class="hover:text-slate-600 transition-colors">
          已有账号? 返回登录
        </button>
      </div>
    </form>
  </SubPageShell>
</template>
