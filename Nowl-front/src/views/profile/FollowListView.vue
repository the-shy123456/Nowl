<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CheckCircle2,
  Search,
  UserCheck,
  UserPlus,
  Users,
} from 'lucide-vue-next'
import {
  followUser,
  getFollowerList,
  getFollowingList,
  type FollowUserVO,
  unfollowUser,
} from '@/api/modules/user'
import { useUserStore } from '@/stores/user'
import { ElMessage } from '@/utils/feedback'
import { normalizeMediaUrl } from '@/utils/media'
import SubPageShell from '@/components/SubPageShell.vue'
import { PAGE_CONSTANTS } from '@/constants'
import { usePaginatedList } from '@/composables/usePaginatedList'
import {
  createQueryBinding,
  parsePositiveIntQuery,
  serializePageQuery,
  useListQuerySync,
} from '@/composables/useListQuerySync'
import InfiniteListFooter from '@/components/InfiniteListFooter.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const keyword = ref('')
const followLoadingMap = ref<Record<number, boolean>>({})
const routeKey = computed(() => `${String(route.name ?? '')}-${String(route.params.userId ?? '')}`)

const userId = computed(() => Number(route.params.userId))
const type = computed<'following' | 'followers'>(() =>
  route.name === 'user-following' ? 'following' : 'followers',
)
const isSelfPage = computed(() => userId.value === userStore.userInfo?.userId)
const backTo = computed(() => (isSelfPage.value ? '/profile' : `/user/${userId.value}`))

const title = computed(() => (type.value === 'following' ? '关注列表' : '粉丝列表'))
const subtitle = computed(() => (type.value === 'following' ? '你关注的用户' : '关注你的用户'))

const {
  list,
  pageNum,
  hasMore,
  showInitialLoading,
  isLoadingMore,
  loadMoreTrigger,
  refresh,
} = usePaginatedList<FollowUserVO>({
  pageSize: PAGE_CONSTANTS.LARGE_PAGE_SIZE,
  fetchPage: ({ pageNum, pageSize }) => {
    const params = { pageNum, pageSize }
    return type.value === 'following'
      ? getFollowingList(userId.value, params)
      : getFollowerList(userId.value, params)
  },
  onError: () => {
    ElMessage.error('获取列表失败')
  },
})

useListQuerySync([
  createQueryBinding({
    key: 'page',
    state: pageNum,
    defaultValue: 1,
    parse: raw => parsePositiveIntQuery(raw, 1),
    serialize: value => serializePageQuery(value, 1),
  }),
], {
  onQueryApplied: () => {
    void refresh(pageNum.value)
  },
})

const setLoadMoreTrigger = (element: HTMLElement | null) => {
  loadMoreTrigger.value = element
}

const filteredList = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query) return list.value
  return list.value.filter((item) => {
    const nickName = (item.nickName || '').toLowerCase()
    const school = (item.schoolName || '').toLowerCase()
    const campus = (item.campusName || '').toLowerCase()
    return nickName.includes(query)
      || school.includes(query)
      || campus.includes(query)
      || String(item.userId).includes(query)
  })
})

const followStats = computed(() => {
  const source = filteredList.value
  return {
    total: source.length,
    mutual: source.filter(item => item.isMutual).length,
    followed: source.filter(item => item.isFollowed).length,
  }
})

const formatFollowTime = (value?: string) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

const handleFollow = async (user: FollowUserVO) => {
  if (followLoadingMap.value[user.userId]) return
  followLoadingMap.value[user.userId] = true
  try {
    if (user.isFollowed) {
      await unfollowUser(user.userId)
      user.isFollowed = false
      ElMessage.success('已取消关注')
    } else {
      await followUser(user.userId)
      user.isFollowed = true
      ElMessage.success('关注成功')
    }
    if (type.value === 'followers') {
      user.isMutual = user.isFollowed
    }
  } catch {
    ElMessage.error('操作失败')
  } finally {
    followLoadingMap.value[user.userId] = false
  }
}

const viewUser = (targetUserId: number) => {
  router.push(`/user/${targetUserId}`)
}

watch(routeKey, (next, previous) => {
  if (next === previous) return
  keyword.value = ''
  if (pageNum.value !== 1) {
    pageNum.value = 1
    return
  }
  void refresh(1)
})

onMounted(() => {
  void refresh(pageNum.value)
})
</script>

<template>
  <SubPageShell :title="title" :subtitle="subtitle" :back-to="backTo" max-width="lg" :use-card="false">
    <div class="space-y-4">
      <section class="um-card p-4 md:p-5 space-y-3">
        <div class="flex items-center justify-between gap-3 flex-wrap">
          <div class="grid grid-cols-3 gap-2 text-xs">
            <div class="rounded-xl bg-warm-50 border border-warm-100 px-3 py-2 text-warm-700 min-w-[88px]">
              <div class="text-[11px] text-warm-500">列表人数</div>
              <div class="text-sm font-bold">{{ followStats.total }}</div>
            </div>
            <div class="rounded-xl bg-emerald-50 border border-emerald-100 px-3 py-2 text-emerald-700 min-w-[88px]">
              <div class="text-[11px] text-emerald-500">互相关注</div>
              <div class="text-sm font-bold">{{ followStats.mutual }}</div>
            </div>
            <div class="rounded-xl bg-blue-50 border border-blue-100 px-3 py-2 text-blue-700 min-w-[88px]">
              <div class="text-[11px] text-blue-500">已关注</div>
              <div class="text-sm font-bold">{{ followStats.followed }}</div>
            </div>
          </div>
        </div>

        <label class="flex items-center gap-2 rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2.5">
          <Search :size="15" class="text-slate-400" />
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索昵称、学校或用户ID"
            class="w-full bg-transparent outline-none text-sm text-slate-700 placeholder:text-slate-400"
          />
        </label>
      </section>

      <div v-if="showInitialLoading" class="text-center py-12">
        <div class="inline-block animate-spin rounded-full h-10 w-10 border-4 border-warm-500 border-t-transparent"></div>
        <p class="mt-3 text-slate-500 text-sm">正在加载列表...</p>
      </div>

      <div v-else-if="filteredList.length === 0" class="um-card py-16 text-center">
        <Users :size="50" class="mx-auto text-slate-300 mb-3" />
        <p class="text-slate-500">{{ keyword ? '没有匹配的用户' : (type === 'following' ? '暂无关注' : '暂无粉丝') }}</p>
      </div>

      <div v-else class="space-y-3">
        <article
          v-for="user in filteredList"
          :key="user.userId"
          class="um-card p-4 flex items-center gap-3 hover:border-warm-200 transition-colors"
        >
          <button
            class="w-12 h-12 rounded-full overflow-hidden border border-slate-200 bg-slate-50 shrink-0"
            @click="viewUser(user.userId)"
          >
            <img
              :src="normalizeMediaUrl(user.imageUrl) || '/avatar-placeholder.svg'"
              :alt="user.nickName"
              class="w-full h-full object-cover"
            />
          </button>

          <button class="flex-1 min-w-0 text-left" @click="viewUser(user.userId)">
            <div class="flex items-center gap-2">
              <span class="font-semibold text-slate-800 truncate">{{ user.nickName || `用户${user.userId}` }}</span>
              <span
                v-if="user.authStatus === 2"
                class="text-[11px] px-1.5 py-0.5 rounded-md bg-emerald-50 text-emerald-700 border border-emerald-100 inline-flex items-center gap-1"
              >
                <CheckCircle2 :size="12" />
                已认证
              </span>
              <span
                v-if="user.isMutual"
                class="text-[11px] px-2 py-0.5 rounded-full bg-warm-50 text-warm-600 border border-warm-100"
              >
                互关
              </span>
            </div>
            <p class="mt-1 text-xs text-slate-500 truncate">
              {{ user.schoolName || '未知学校' }}{{ user.campusName ? ` · ${user.campusName}` : '' }}
            </p>
            <p class="mt-1 text-[11px] text-slate-400">
              关注于 {{ formatFollowTime(user.followTime) }}
            </p>
          </button>

          <button
            v-if="user.userId !== userStore.userInfo?.userId"
            @click.stop="handleFollow(user)"
            :disabled="followLoadingMap[user.userId]"
            class="um-btn px-3.5 py-2 text-sm inline-flex items-center gap-1.5 shrink-0"
            :class="user.isFollowed
              ? 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              : 'bg-warm-500 text-white hover:bg-warm-600'"
          >
            <UserCheck v-if="user.isFollowed" :size="14" />
            <UserPlus v-else :size="14" />
            {{ user.isFollowed ? '已关注' : '关注' }}
          </button>
        </article>

        <InfiniteListFooter
          :is-loading-more="isLoadingMore"
          :has-more="hasMore"
          :set-trigger="setLoadMoreTrigger"
          loading-text="正在加载更多用户..."
        />
      </div>
    </div>
  </SubPageShell>
</template>
