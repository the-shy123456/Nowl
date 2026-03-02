import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, RegisterForm, LoginForm } from '@/types'
import * as userApi from '@/api/modules/user'
import { AuthStatus } from '@/constants'
import { normalizeMediaData } from '@/utils/media'

export const useUserStore = defineStore('user', () => {
  // 状态
  const userInfo = ref<UserInfo | null>(null)
  const currentCampus = ref<{ schoolCode: string; campusCode: string; name: string } | null>(null)

  if (userInfo.value) {
    userInfo.value = normalizeMediaData(userInfo.value)
  }

  // 计算属性：自动根据 userInfo 推导登录状态
  const isLoggedIn = computed(() => !!userInfo.value)

  // 登录
  const login = async (loginData: LoginForm) => {
    try {
      const res = await userApi.login(loginData)
      userInfo.value = normalizeMediaData(res.userInfo)

      // 登录成功后，判断是否应该限制在本校商品
      // 只有 authStatus=2（已认证通过）且有学校信息的用户才默认筛选本校商品
      // 未认证用户（schoolCode/campusCode 为空）、待审核用户都应该看到所有商品
      const hasSchoolInfo = userInfo.value?.schoolCode && userInfo.value?.campusCode
      const isAuthenticated = userInfo.value?.authStatus === AuthStatus.APPROVED

      if (userInfo.value && hasSchoolInfo && isAuthenticated) {
        currentCampus.value = {
          schoolCode: userInfo.value.schoolCode,
          campusCode: userInfo.value.campusCode,
          name: userInfo.value.campusName || '本校',
        }
      } else {
        currentCampus.value = null // 游客、未认证、待审核用户，显示全部商品
      }

      return res
    } catch (error) {
      console.error('登录失败', error)
      throw error
    }
  }

  // 切换当前查看的校区
  const switchCampus = (schoolCode: string, campusCode: string, name: string) => {
    currentCampus.value = { schoolCode, campusCode, name }
  }

  // 注册
  const register = async (data: RegisterForm) => {
    try {
      const res = await userApi.register(data)
      return res
    } catch (error) {
      console.error('注册失败', error)
      throw error
    }
  }

  // 登出
  const clearLocalSession = () => {
    userInfo.value = null
    currentCampus.value = null // 清除校区选择，避免残留影响下次登录
  }

  const logout = async (options?: { notifyServer?: boolean }) => {
    const shouldNotifyServer = options?.notifyServer !== false
    if (shouldNotifyServer) {
      try {
        await userApi.logout()
      } catch (error) {
        console.warn('Backend logout failed, proceeding with local cleanup', error)
      }
    }
    clearLocalSession()
  }

  // 获取用户信息
  const fetchUserInfo = async () => {
    try {
      const res = await userApi.getCurrentUserInfo()
      userInfo.value = normalizeMediaData(res)
      return res
    } catch (error) {
      // 获取失败时清空用户信息（token 可能已过期）
      userInfo.value = null
      console.error('获取用户信息失败', error)
      throw error
    }
  }

  // 更新用户信息
  const updateInfo = async (data: Partial<UserInfo>) => {
    try {
      await userApi.updateUserInfo(data)
      if (userInfo.value) {
        userInfo.value = normalizeMediaData({ ...userInfo.value, ...data })
      }
    } catch (error) {
      console.error('更新用户信息失败', error)
      throw error
    }
  }

  return {
    userInfo,
    isLoggedIn,
    currentCampus,
    login,
    register,
    logout,
    fetchUserInfo,
    updateInfo,
    switchCampus,
  }
}, {
  persist: {
    key: 'user-store',
    storage: localStorage,
    pick: ['userInfo', 'currentCampus'], // 不持久化 isLoggedIn，登录态由 Cookie 真实情况决定
  },
})
