<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { Check, ChevronDown, LogOut, MapPin, Plus, User } from 'lucide-vue-next'
import logoSvg from '@/assets/logo.svg'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { useNotificationWs } from '@/composables/useNotificationWs'
import { getCampusList } from '@/api/modules/school'
import type { SchoolInfo } from '@/types'
import { AuthStatus } from '@/constants'
import { normalizeMediaUrl } from '@/utils/media'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()

// 全局通知 WebSocket
useNotificationWs()

const tabRouteMap = {
  home: '/',
  market: '/market',
  errands: '/errands',
  chat: '/chat',
  message: '/message',
} as const

type NavTab = keyof typeof tabRouteMap

const showCampusSelector = ref(false)
const campusList = ref<SchoolInfo[]>([])
const loadingCampus = ref(false)

const showPublishPopover = ref(false)
let publishPopoverCloseTimer: ReturnType<typeof setTimeout> | null = null

const showProfilePopover = ref(false)
let profilePopoverCloseTimer: ReturnType<typeof setTimeout> | null = null
let routeRefreshInFlight = false
let lastRouteRefreshAt = 0

const avatarLoadFailed = ref(false)
const currentUserAvatar = computed(() => normalizeMediaUrl(userStore.userInfo?.imageUrl) || '')
const userCreditScore = computed(() => userStore.userInfo?.creditScore ?? 0)
const userBalanceText = computed(() => {
  const amount = Number(userStore.userInfo?.money ?? 0)
  return Number.isFinite(amount) ? amount.toFixed(2) : '0.00'
})

const clearPublishPopoverCloseTimer = () => {
  if (publishPopoverCloseTimer) {
    clearTimeout(publishPopoverCloseTimer)
    publishPopoverCloseTimer = null
  }
}

const clearProfilePopoverCloseTimer = () => {
  if (profilePopoverCloseTimer) {
    clearTimeout(profilePopoverCloseTimer)
    profilePopoverCloseTimer = null
  }
}

const handleAvatarImageError = () => {
  avatarLoadFailed.value = true
}

const handlePublishMouseEnter = () => {
  clearPublishPopoverCloseTimer()
  showProfilePopover.value = false
  showPublishPopover.value = true
}

const handlePublishMouseLeave = () => {
  clearPublishPopoverCloseTimer()
  publishPopoverCloseTimer = setTimeout(() => {
    showPublishPopover.value = false
    publishPopoverCloseTimer = null
  }, 120)
}

const togglePublishPopover = () => {
  showProfilePopover.value = false
  showPublishPopover.value = !showPublishPopover.value
}

const handleAvatarMouseEnter = () => {
  clearProfilePopoverCloseTimer()
  showPublishPopover.value = false
  showProfilePopover.value = true
}

const handleAvatarMouseLeave = () => {
  clearProfilePopoverCloseTimer()
  profilePopoverCloseTimer = setTimeout(() => {
    showProfilePopover.value = false
    profilePopoverCloseTimer = null
  }, 120)
}

const handleLogout = async () => {
  await userStore.logout()
  messageStore.stopUnreadPolling()
  messageStore.clearUnreadCount()
  showPublishPopover.value = false
  showProfilePopover.value = false
  router.push('/login')
}

const activeTab = computed(() => (route.name as string) || 'market')
const showLayout = computed(() => !route.meta.hideLayout)
const showTopNav = computed(() =>
  showLayout.value && ['home', 'market', 'errands', 'chat', 'message', 'profile', 'user-chat'].includes(activeTab.value),
)

const mainContentClass = computed(() => {
  if (!showLayout.value) {
    return 'relative z-10'
  }
  if (route.meta.fullWidth) {
    return 'relative z-10'
  }
  return 'relative z-10 um-shell pt-6 pb-16'
})

const campusName = computed(() => {
  if (!userStore.isLoggedIn) return '未登录'
  if (userStore.userInfo?.authStatus === AuthStatus.APPROVED) {
    if (userStore.currentCampus) return userStore.currentCampus.name
    return userStore.userInfo?.campusName || '全部校区'
  }
  return '游客'
})

const isNavActive = (tab: NavTab) => {
  if (tab === 'message') {
    return ['message', 'user-chat'].includes(activeTab.value)
  }
  return activeTab.value === tab
}

const navigateTo = (tab: NavTab) => {
  const targetPath = tabRouteMap[tab]
  if (route.path === targetPath) return
  router.push(targetPath)
}

const navigateWithAuth = (path: string) => {
  showPublishPopover.value = false
  showProfilePopover.value = false
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  router.push(path)
}

const navigateToMyReviews = () => {
  const userId = userStore.userInfo?.userId
  navigateWithAuth(userId ? `/review/user/${userId}` : '/profile')
}

const handleAvatarClick = () => {
  clearProfilePopoverCloseTimer()
  showPublishPopover.value = false
  showProfilePopover.value = !showProfilePopover.value
}

const refreshUserContextOnRouteChange = async () => {
  if (!userStore.isLoggedIn || routeRefreshInFlight) {
    return
  }
  const now = Date.now()
  if (now - lastRouteRefreshAt < 400) {
    return
  }
  routeRefreshInFlight = true
  lastRouteRefreshAt = now
  try {
    await userStore.fetchUserInfo()
  } catch (error) {
    console.warn('路由切换后刷新用户信息失败', error)
  } finally {
    routeRefreshInFlight = false
  }
}

const handleCampusClick = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }

  if (userStore.userInfo?.authStatus !== AuthStatus.APPROVED) {
    return
  }

  if (showCampusSelector.value) {
    showCampusSelector.value = false
    return
  }

  if (userStore.userInfo?.schoolCode) {
    loadingCampus.value = true
    try {
      const res = await getCampusList(userStore.userInfo.schoolCode)
      campusList.value = res
    } catch (err) {
      console.error('获取校区列表失败', err)
    } finally {
      loadingCampus.value = false
    }
  }

  showCampusSelector.value = true
}

const selectCampus = (campus: SchoolInfo | null) => {
  if (campus === null) {
    userStore.switchCampus(userStore.userInfo!.schoolCode, '', '全部校区')
  } else {
    userStore.switchCampus(campus.schoolCode, campus.campusCode, campus.campusName)
  }
  showCampusSelector.value = false
}

watch(currentUserAvatar, () => {
  avatarLoadFailed.value = false
})

watch(
  () => userStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      messageStore.fetchUnreadMessageCount().catch((error) => {
        console.warn('登录后拉取未读消息数失败', error)
      })
      messageStore.startUnreadPolling()
      return
    }
    messageStore.stopUnreadPolling()
    messageStore.clearUnreadCount()
  },
)

watch(
  () => route.fullPath,
  () => {
    showCampusSelector.value = false
    showPublishPopover.value = false
    showProfilePopover.value = false
    void refreshUserContextOnRouteChange()
  },
)

onMounted(() => {
  if (!userStore.isLoggedIn) {
    messageStore.clearUnreadCount()
    return
  }

  userStore
    .fetchUserInfo()
    .catch((error) => {
      console.warn('启动阶段获取用户信息失败', error)
    })
    .finally(() => {
      if (userStore.isLoggedIn) {
        messageStore.fetchUnreadMessageCount().catch((error) => {
          console.warn('启动阶段拉取未读消息数失败', error)
        })
        messageStore.startUnreadPolling()
      } else {
        messageStore.clearUnreadCount()
      }
    })
})

onUnmounted(() => {
  clearPublishPopoverCloseTimer()
  clearProfilePopoverCloseTimer()
  messageStore.stopUnreadPolling()
})
</script>

<template>
  <div class="um-theme antialiased">
    <header v-if="showTopNav" class="proto-header">
      <div class="proto-header-inner">
        <button type="button" class="logo-area" @click="navigateTo('home')" aria-label="返回首页">
          <div class="nav-logo">
            <img :src="logoSvg" alt="Nowl" />
          </div>
          <span class="logo-title">Nowl</span>
        </button>

        <nav class="nav-links" aria-label="主导航">
          <button type="button" class="nav-item" :class="{ active: isNavActive('home') }" @click="navigateTo('home')">
            首页
          </button>
          <button type="button" class="nav-item" :class="{ active: isNavActive('market') }" @click="navigateTo('market')">
            集市
          </button>
          <button type="button" class="nav-item" :class="{ active: isNavActive('errands') }" @click="navigateTo('errands')">
            跑腿
          </button>
          <button type="button" class="nav-item ai-helper" :class="{ active: isNavActive('chat') }" @click="navigateTo('chat')">
            Nowl AI
          </button>
          <button type="button" class="nav-item message-item" :class="{ active: isNavActive('message') }" @click="navigateTo('message')">
            消息
            <span v-if="messageStore.unreadMessageCount > 0" class="message-badge">
              {{ messageStore.unreadMessageBadge }}
            </span>
          </button>
          <div class="publish-shell" @mouseenter="handlePublishMouseEnter" @mouseleave="handlePublishMouseLeave">
            <button type="button" class="publish-button" @click="togglePublishPopover">
              <Plus :size="14" />
              发布
              <ChevronDown :size="14" class="publish-arrow" :class="{ rotated: showPublishPopover }" />
            </button>
            <Transition name="dropdown-fade">
              <div v-if="showPublishPopover" class="publish-dropdown">
                <button type="button" class="publish-item" @click="navigateWithAuth('/publish')">
                  发布商品
                </button>
                <button type="button" class="publish-item" @click="navigateWithAuth('/errand/publish')">
                  发布跑腿
                </button>
              </div>
            </Transition>
          </div>
        </nav>

        <div class="user-actions">
          <div class="campus-shell">
            <button
              type="button"
              class="campus-selector"
              :class="{ disabled: userStore.userInfo?.authStatus !== AuthStatus.APPROVED }"
              @click="handleCampusClick"
            >
              <MapPin :size="14" />
              <span class="campus-text">{{ campusName }}</span>
              <ChevronDown
                v-if="userStore.userInfo?.authStatus === AuthStatus.APPROVED"
                :size="14"
                class="campus-arrow"
                :class="{ rotated: showCampusSelector }"
              />
            </button>

            <Transition name="dropdown-fade">
              <div v-if="showCampusSelector" class="campus-dropdown">
                <button type="button" class="campus-item" @click="selectCampus(null)">
                  <span>全部校区</span>
                  <Check v-if="!userStore.currentCampus?.campusCode" :size="16" class="text-warm-500" />
                </button>
                <div class="campus-divider"></div>
                <div v-if="loadingCampus" class="campus-loading">加载中...</div>
                <template v-else>
                  <button
                    v-for="campus in campusList"
                    :key="campus.campusCode"
                    type="button"
                    class="campus-item"
                    @click="selectCampus(campus)"
                  >
                    <span>{{ campus.campusName }}</span>
                    <Check
                      v-if="userStore.currentCampus?.campusCode === campus.campusCode"
                      :size="16"
                      class="text-warm-500"
                    />
                  </button>
                </template>
              </div>
            </Transition>
          </div>

          <div class="avatar-container" @mouseenter="handleAvatarMouseEnter" @mouseleave="handleAvatarMouseLeave">
            <button type="button" class="avatar-button" @click="handleAvatarClick">
              <img
                v-if="userStore.isLoggedIn && currentUserAvatar && !avatarLoadFailed"
                :src="currentUserAvatar"
                class="avatar-image"
                @error="handleAvatarImageError"
              />
              <div v-else class="avatar-fallback">
                <User :size="18" />
              </div>
            </button>

            <Transition name="popover-fade">
              <div v-if="showProfilePopover" class="profile-dropdown">
                <div class="profile-summary">
                  <p class="profile-name">{{ userStore.userInfo?.nickName || '游客' }}</p>
                  <p class="profile-campus">{{ campusName }}</p>
                  <div class="profile-metrics">
                    <div class="profile-metric">
                      <span class="profile-metric-label">信用分</span>
                      <strong class="profile-metric-value">{{ userStore.isLoggedIn ? userCreditScore : '--' }}</strong>
                    </div>
                    <div class="profile-metric">
                      <span class="profile-metric-label">金额</span>
                      <strong class="profile-metric-value">{{ userStore.isLoggedIn ? `¥${userBalanceText}` : '--' }}</strong>
                    </div>
                  </div>
                </div>
                <div class="profile-divider"></div>
                <div class="profile-quick-grid">
                  <button type="button" class="profile-quick-item" @click="navigateWithAuth('/profile')">我的主页</button>
                  <button type="button" class="profile-quick-item" @click="navigateWithAuth('/profile/my-orders')">我的订单</button>
                  <button type="button" class="profile-quick-item" @click="navigateWithAuth('/profile/my-goods')">我的商品</button>
                  <button type="button" class="profile-quick-item" @click="navigateWithAuth('/profile/my-errands')">我的跑腿</button>
                  <button type="button" class="profile-quick-item" @click="navigateWithAuth('/profile/my-collections')">我的收藏</button>
                  <button type="button" class="profile-quick-item" @click="navigateToMyReviews">我的评价</button>
                </div>
                <div class="profile-quick-row">
                  <button type="button" class="profile-quick-item profile-quick-item-full" @click="navigateWithAuth('/profile/settings')">设置</button>
                </div>
                <div class="profile-divider"></div>
                <button v-if="userStore.isLoggedIn" type="button" class="profile-item logout" @click="handleLogout">
                  <LogOut :size="16" />
                  退出登录
                </button>
                <button v-else type="button" class="profile-item login-entry" @click="router.push('/login')">
                  <User :size="16" />
                  去登录
                </button>
              </div>
            </Transition>
          </div>
        </div>
      </div>
    </header>

    <main :class="mainContentClass">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.proto-header {
  background: var(--um-header-bg);
  position: sticky;
  top: 0;
  z-index: 40;
  box-shadow: var(--um-header-shadow);
  border-bottom: 1px solid var(--um-header-border);
  backdrop-filter: blur(8px);
}

.proto-header-inner {
  max-width: 1280px;
  height: 70px;
  margin: 0 auto;
  padding: 0 5%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.logo-area {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 1.2rem;
  border: none;
  background: transparent;
  color: var(--um-text);
  cursor: pointer;
  padding: 0;
}

.nav-logo img {
  width: 50px;
  height: 50px;
  display: block;
}

.logo-title {
  white-space: nowrap;
}

.nav-links {
  display: flex;
  gap: 30px;
  font-weight: 500;
  align-items: center;
  flex: 1;
  justify-content: center;
  min-width: 0;
}

.nav-item {
  position: relative;
  border: none;
  background: transparent;
  color: var(--um-text);
  cursor: pointer;
  transition: color var(--um-duration-fast) var(--um-ease-standard);
  font-size: 0.95rem;
  padding: 0;
  white-space: nowrap;
}

.nav-item:hover,
.nav-item.active {
  color: var(--um-accent);
}

.ai-helper {
  background: linear-gradient(135deg, var(--um-primary), var(--um-accent));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-weight: 700;
}

.logo-area:focus-visible,
.nav-item:focus-visible,
.publish-button:focus-visible,
.campus-selector:focus-visible,
.avatar-button:focus-visible,
.publish-item:focus-visible,
.campus-item:focus-visible,
.profile-quick-item:focus-visible,
.profile-guest-btn:focus-visible,
.profile-item:focus-visible {
  outline: 2px solid var(--um-focus-ring);
  outline-offset: 2px;
  border-radius: 12px;
}

.message-item {
  padding-right: 2px;
}

.publish-shell {
  position: relative;
}

.publish-button {
  border: 1px solid rgba(255, 112, 67, 0.24);
  background: linear-gradient(135deg, rgba(255, 112, 67, 0.14), rgba(255, 112, 67, 0.22));
  color: var(--um-accent);
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 0.86rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: transform var(--um-duration-fast), box-shadow var(--um-duration-fast);
}

.publish-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 14px rgba(255, 112, 67, 0.2);
}

.publish-arrow {
  transition: transform 0.2s ease;
}

.publish-arrow.rotated {
  transform: rotate(180deg);
}

.publish-dropdown {
  position: absolute;
  top: 44px;
  right: 0;
  width: 156px;
  background: var(--um-surface);
  border: 1px solid var(--um-border);
  border-radius: 12px;
  box-shadow: var(--um-shadow-soft);
  padding: 6px;
  z-index: 72;
}

.publish-item {
  width: 100%;
  border: none;
  background: transparent;
  color: var(--um-text);
  border-radius: 8px;
  padding: 9px 10px;
  text-align: left;
  font-size: 0.84rem;
  cursor: pointer;
  transition: background-color var(--um-duration-fast), color var(--um-duration-fast);
}

.publish-item:hover {
  background: var(--um-bg-soft);
  color: var(--um-accent);
}

.message-badge {
  position: absolute;
  top: -8px;
  right: -14px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  background: #ef4444;
  color: #fff;
  font-size: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.campus-shell {
  position: relative;
}

.campus-selector {
  background: var(--um-bg-soft);
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 0.9rem;
  cursor: pointer;
  border: 1px solid var(--um-border);
  color: var(--um-text);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: border-color var(--um-duration-fast), background-color var(--um-duration-fast);
}

.campus-selector:hover {
  background: var(--um-hover-surface);
  border-color: var(--um-primary);
}

.campus-selector.disabled {
  color: var(--um-muted);
}

.campus-selector.disabled:hover {
  background: var(--um-bg-soft);
  border-color: var(--um-border);
}

.campus-text {
  max-width: 128px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.campus-arrow {
  transition: transform 0.2s ease;
}

.campus-arrow.rotated {
  transform: rotate(180deg);
}

.campus-dropdown {
  position: absolute;
  top: 50px;
  right: 0;
  width: 210px;
  background: var(--um-surface);
  border-radius: var(--um-radius);
  box-shadow: var(--um-shadow);
  border: 1px solid var(--um-border);
  padding: 8px 0;
  z-index: 70;
}

.campus-item {
  width: 100%;
  border: none;
  background: transparent;
  padding: 10px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.88rem;
  color: var(--um-text);
  cursor: pointer;
}

.campus-item:hover {
  background: var(--um-bg-soft);
}

.campus-divider {
  height: 1px;
  background: var(--um-primary-100);
  margin: 4px 0;
}

.campus-loading {
  text-align: center;
  color: var(--um-muted);
  font-size: 0.86rem;
  padding: 8px 0;
}

.avatar-container {
  position: relative;
  cursor: pointer;
}

.avatar-button {
  width: 40px;
  height: 40px;
  background: var(--um-primary-100);
  border-radius: 50%;
  border: 2px solid var(--um-primary);
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  display: flex;
  justify-content: center;
  align-items: center;
  color: var(--um-text);
}

.profile-dropdown {
  position: absolute;
  top: 50px;
  right: 0;
  width: 296px;
  background: var(--um-surface);
  border-radius: var(--um-radius);
  box-shadow: var(--um-shadow);
  border: 1px solid var(--um-border);
  padding: 12px;
  z-index: 70;
}

.profile-summary {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.profile-name {
  font-size: 0.96rem;
  font-weight: 700;
  color: var(--um-text);
  margin: 0;
}

.profile-campus {
  margin: 0;
  font-size: 0.78rem;
  color: var(--um-muted);
}

.profile-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.profile-metric {
  background: var(--um-bg-soft);
  border: 1px solid var(--um-border);
  border-radius: 10px;
  padding: 8px 10px;
}

.profile-metric-label {
  display: block;
  font-size: 0.72rem;
  color: var(--um-muted);
}

.profile-metric-value {
  display: block;
  margin-top: 4px;
  color: var(--um-text);
  font-size: 0.98rem;
}

.profile-divider {
  height: 1px;
  background: var(--um-border);
  margin: 10px 0;
}

.profile-quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.profile-quick-row {
  margin-top: 8px;
}

.profile-quick-item {
  border: 1px solid var(--um-border);
  background: #fff;
  border-radius: 10px;
  padding: 9px 10px;
  color: var(--um-text);
  font-size: 0.82rem;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition: border-color var(--um-duration-fast), background-color var(--um-duration-fast);
}

.profile-quick-item:hover {
  background: var(--um-bg-soft);
  border-color: rgba(141, 110, 99, 0.4);
}

.profile-quick-item-full {
  width: 100%;
}

.profile-item {
  width: 100%;
  border: none;
  background: transparent;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.9rem;
  color: var(--um-text);
  cursor: pointer;
  text-align: left;
}

.profile-item:hover {
  background: var(--um-bg-soft);
}

.profile-item.logout {
  color: var(--um-accent);
  border-radius: 10px;
}

.profile-item.login-entry {
  color: var(--um-text);
  border-radius: 10px;
}

@media (max-width: 1080px) {
  .proto-header-inner {
    height: auto;
    min-height: 70px;
    padding: 10px 16px;
    gap: 12px;
    flex-wrap: wrap;
  }

  .logo-area {
    order: 1;
  }

  .user-actions {
    order: 2;
    margin-left: auto;
    gap: 12px;
  }

  .nav-links {
    order: 3;
    width: 100%;
    justify-content: flex-start;
    overflow-x: auto;
    gap: 18px;
    padding-bottom: 2px;
  }

  .nav-links::-webkit-scrollbar {
    display: none;
  }
}

@media (max-width: 720px) {
  .logo-title {
    display: none;
  }

  .nav-logo img {
    width: 42px;
    height: 42px;
  }

  .campus-text {
    max-width: 84px;
  }

  .profile-dropdown {
    right: -10px;
    width: min(296px, calc(100vw - 24px));
  }
}

.popover-fade-enter-active,
.popover-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.popover-fade-enter-from,
.popover-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

.dropdown-fade-enter-active,
.dropdown-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.dropdown-fade-enter-from,
.dropdown-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>

