<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import logoSvg from '@/assets/logo.svg'
import { getHomeRecommend } from '@/api/modules/recommend'
import { useUserStore } from '@/stores/user'
import type { RecommendItemVO } from '@/types'
import {
  ArrowRight,
  Bike,
  LoaderCircle,
  MapPin,
  Package,
  ShieldCheck,
} from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()

const RECOMMEND_LIMIT = 10
const RECOMMEND_ROW_SIZE = 5

const recommendGoods = ref<RecommendItemVO[]>([])
const recommendLoading = ref(false)
const usingMockRecommend = ref(false)

interface HomeErrandTask {
  taskId: number
  title: string
  reward: number
  taskContent?: string
  description?: string
  pickupAddress?: string
  deliveryAddress?: string
}

const errands = ref<HomeErrandTask[]>([
  {
    taskId: -1,
    title: '代取菜鸟驿站快递',
    reward: 3.5,
    taskContent: '今晚 7 点前帮取一个小件，顺路送到三号宿舍楼下。',
    pickupAddress: '菜鸟驿站',
    deliveryAddress: '三号宿舍楼下',
  },
  {
    taskId: -2,
    title: '食堂带饭到图书馆',
    reward: 4,
    taskContent: '二食堂窗口取餐，送到图书馆东门。',
    pickupAddress: '二食堂取餐口',
    deliveryAddress: '图书馆东门',
  },
  {
    taskId: -3,
    title: '打印资料送教学楼',
    reward: 5,
    taskContent: '打印店取 20 页材料，送到教学楼 C 区 302。',
    pickupAddress: '文印中心',
    deliveryAddress: '教学楼 C 区 302',
  },
])

const mockRecommendGoods: RecommendItemVO[] = [
  {
    productId: -101,
    title: '罗技机械键盘 K98',
    image: '/mock/recommend/keyboard.jpg',
    price: 129,
    categoryName: '数码产品',
    sellerId: 0,
    sellerName: '同学A',
    recommendType: 'hot',
  },
  {
    productId: -102,
    title: '雅思备考全套教材',
    image: '/mock/recommend/books.jpg',
    price: 48,
    categoryName: '图书资料',
    sellerId: 0,
    sellerName: '同学B',
    recommendType: 'cf',
  },
  {
    productId: -103,
    title: '九成新羽毛球拍双拍',
    image: '/mock/recommend/badminton.jpg',
    price: 76,
    categoryName: '运动器材',
    sellerId: 0,
    sellerName: '同学C',
    recommendType: 'hybrid',
  },
  {
    productId: -104,
    title: '小米电水壶 1.5L',
    image: '/mock/recommend/kettle.jpg',
    price: 35,
    categoryName: '宿舍好物',
    sellerId: 0,
    sellerName: '同学D',
    recommendType: 'content',
  },
  {
    productId: -105,
    title: '便携投影仪 mini 版',
    image: '/mock/recommend/projector.jpg',
    price: 188,
    categoryName: '影音设备',
    sellerId: 0,
    sellerName: '同学E',
    recommendType: 'hot',
  },
  {
    productId: -106,
    title: 'Switch 健身环套装',
    image: '/mock/recommend/console.jpg',
    price: 219,
    categoryName: '游戏娱乐',
    sellerId: 0,
    sellerName: '同学F',
    recommendType: 'cf',
  },
  {
    productId: -107,
    title: 'ins 拍立得 mini11',
    image: '/mock/recommend/camera.jpg',
    price: 169,
    categoryName: '摄影器材',
    sellerId: 0,
    sellerName: '同学G',
    recommendType: 'content',
  },
  {
    productId: -108,
    title: '宿舍收纳抽屉柜',
    image: '/mock/recommend/storage.jpg',
    price: 26,
    categoryName: '宿舍日用',
    sellerId: 0,
    sellerName: '同学H',
    recommendType: 'hybrid',
  },
  {
    productId: -109,
    title: '晨跑蓝牙耳机',
    image: '/mock/recommend/headphones.jpg',
    price: 58,
    categoryName: '耳机音频',
    sellerId: 0,
    sellerName: '同学I',
    recommendType: 'hot',
  },
  {
    productId: -110,
    title: '高数真题冲刺卷',
    image: '/mock/recommend/study.jpg',
    price: 18,
    categoryName: '学习资料',
    sellerId: 0,
    sellerName: '同学J',
    recommendType: 'cf',
  },
]

const reasons = [
  {
    title: '拾光集市',
    desc: '校内面对面交易，告别快递等待，拒绝货不对板，让闲置就近流转。',
    icon: Package,
  },
  {
    title: '校园跑腿',
    desc: '代取快递、食堂带饭、打印资料，Nowl同学随时待命，响应更快。',
    icon: Bike,
  },
  {
    title: '校园隔离',
    desc: '按学校和校区精准匹配，交易发生在步行可达范围，沟通履约更安心。',
    icon: ShieldCheck,
  },
]

const campusName = computed(() => userStore.currentCampus?.name || '未登录')

const topRecommendGoods = computed(() => recommendGoods.value.slice(0, RECOMMEND_ROW_SIZE))
const bottomRecommendGoods = computed(() => recommendGoods.value.slice(RECOMMEND_ROW_SIZE, RECOMMEND_LIMIT))

const formatPrice = (price: number | undefined) => {
  if (typeof price !== 'number') return '--'
  return Number.isInteger(price) ? String(price) : price.toFixed(2)
}

const getRecommendBadge = (type?: string) => {
  if (type === 'hot') return '热门'
  if (type === 'cf') return '猜你喜欢'
  if (type === 'content') return '相似推荐'
  if (type === 'hybrid') return '智能推荐'
  return '精选'
}

const fetchRecommendGoods = async () => {
  recommendLoading.value = true
  usingMockRecommend.value = false

  try {
    const schoolCode = userStore.currentCampus?.schoolCode
    const campusCode = userStore.currentCampus?.campusCode
    const result = await getHomeRecommend(1, RECOMMEND_LIMIT, schoolCode, campusCode)
    const records = (result.records || []).slice(0, RECOMMEND_LIMIT)

    if (records.length < RECOMMEND_LIMIT) {
      recommendGoods.value = mockRecommendGoods
      usingMockRecommend.value = true
      return
    }

    recommendGoods.value = records
  } catch (error) {
    console.error('获取推荐好物失败', error)
    recommendGoods.value = mockRecommendGoods
    usingMockRecommend.value = true
  } finally {
    recommendLoading.value = false
  }
}

const goToMarket = () => {
  router.push('/market')
}

const goToErrands = () => {
  router.push('/errands')
}

const goToProduct = (productId: number) => {
  router.push(`/product/${productId}`)
}

const handleRecommendClick = (item: RecommendItemVO) => {
  if (item.productId > 0) {
    goToProduct(item.productId)
    return
  }
  goToMarket()
}

const handleErrandCardClick = () => {
  goToErrands()
}

const openAssistant = () => {
  router.push('/chat')
}

watch(
  () => userStore.currentCampus,
  () => {
    void fetchRecommendGoods()
  },
  { deep: true, immediate: true },
)
</script>

<template>
  <div class="home-page">
    <section class="hero">
      <div class="hero-text">
        <div class="campus-tag" >
          <MapPin :size="14" />
          当前定位：{{ campusName }}（已开启校区隔离）
        </div>
        <h1>
          Night owls<br />
          <span>你需要的，都在这里</span>
        </h1>
        <p>
          “Nowl”是专为校园打造的交易社区，以校园隔离为保障。物品交易与校园跑腿兼具，欢迎加入我们，一起营造美好。
        </p>
        <div class="hero-actions">
          <button type="button" class="hero-btn primary" @click="goToMarket">
            开启拾光之旅
            <ArrowRight :size="16" />
          </button>
          <button type="button" class="hero-btn secondary" @click="goToErrands">
            了解校园跑腿
          </button>
        </div>
      </div>

      <div class="hero-visual" aria-hidden="true">
        <div class="owl-frame">
          <img :src="logoSvg" alt="" />
        </div>
      </div>
    </section>

    <section class="recommend-section">
      <div class="recommend-head">
        <h2 class="section-title">校园集市 · 好物推荐</h2>
        <p class="recommend-subtitle">精选 10 件好物推荐</p>
        <p v-if="usingMockRecommend" class="recommend-fallback-tip">
          推荐接口暂不可用，当前展示示例好物。
        </p>
      </div>

      <div v-if="recommendLoading" class="recommend-feedback">
        <LoaderCircle :size="18" class="spin" />
        正在加载校园好物...
      </div>

      <div v-else class="recommend-marquee">
        <div class="recommend-row">
          <div class="recommend-track move-right">
            <div class="recommend-group">
              <article
                v-for="item in topRecommendGoods"
                :key="`top-a-${item.productId}`"
                class="recommend-card"
                @click="handleRecommendClick(item)"
              >
                <div class="recommend-cover">
                  <img v-if="item.image" :src="item.image" :alt="item.title" loading="lazy" />
                  <div v-else class="recommend-placeholder">
                    <img :src="logoSvg" alt="" />
                  </div>
                  <span class="recommend-type">{{ getRecommendBadge(item.recommendType) }}</span>
                </div>
                <div class="recommend-body">
                  <h3>{{ item.title }}</h3>
                  <p class="recommend-category">{{ item.categoryName || '校园精选好物' }}</p>
                  <div class="recommend-meta">
                    <span class="price">¥{{ formatPrice(item.price) }}</span>
                    <span class="seller">{{ item.sellerName || 'Nowl同学' }}</span>
                  </div>
                </div>
              </article>
            </div>

            <div class="recommend-group" aria-hidden="true">
              <article
                v-for="item in topRecommendGoods"
                :key="`top-b-${item.productId}`"
                class="recommend-card"
              >
                <div class="recommend-cover">
                  <img v-if="item.image" :src="item.image" :alt="item.title" loading="lazy" />
                  <div v-else class="recommend-placeholder">
                    <img :src="logoSvg" alt="" />
                  </div>
                  <span class="recommend-type">{{ getRecommendBadge(item.recommendType) }}</span>
                </div>
                <div class="recommend-body">
                  <h3>{{ item.title }}</h3>
                  <p class="recommend-category">{{ item.categoryName || '校园精选好物' }}</p>
                  <div class="recommend-meta">
                    <span class="price">¥{{ formatPrice(item.price) }}</span>
                    <span class="seller">{{ item.sellerName || 'Nowl同学' }}</span>
                  </div>
                </div>
              </article>
            </div>
          </div>
        </div>

        <div class="recommend-row">
          <div class="recommend-track move-left">
            <div class="recommend-group">
              <article
                v-for="item in bottomRecommendGoods"
                :key="`bottom-a-${item.productId}`"
                class="recommend-card"
                @click="handleRecommendClick(item)"
              >
                <div class="recommend-cover">
                  <img v-if="item.image" :src="item.image" :alt="item.title" loading="lazy" />
                  <div v-else class="recommend-placeholder">
                    <img :src="logoSvg" alt="" />
                  </div>
                  <span class="recommend-type">{{ getRecommendBadge(item.recommendType) }}</span>
                </div>
                <div class="recommend-body">
                  <h3>{{ item.title }}</h3>
                  <p class="recommend-category">{{ item.categoryName || '校园精选好物' }}</p>
                  <div class="recommend-meta">
                    <span class="price">¥{{ formatPrice(item.price) }}</span>
                    <span class="seller">{{ item.sellerName || 'Nowl同学' }}</span>
                  </div>
                </div>
              </article>
            </div>

            <div class="recommend-group" aria-hidden="true">
              <article
                v-for="item in bottomRecommendGoods"
                :key="`bottom-b-${item.productId}`"
                class="recommend-card"
              >
                <div class="recommend-cover">
                  <img v-if="item.image" :src="item.image" :alt="item.title" loading="lazy" />
                  <div v-else class="recommend-placeholder">
                    <img :src="logoSvg" alt="" />
                  </div>
                  <span class="recommend-type">{{ getRecommendBadge(item.recommendType) }}</span>
                </div>
                <div class="recommend-body">
                  <h3>{{ item.title }}</h3>
                  <p class="recommend-category">{{ item.categoryName || '校园精选好物' }}</p>
                  <div class="recommend-meta">
                    <span class="price">¥{{ formatPrice(item.price) }}</span>
                    <span class="seller">{{ item.sellerName || 'Nowl同学' }}</span>
                  </div>
                </div>
              </article>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="runner-section">
      <div class="runner-head">
        <h2 class="section-title">校园跑腿 · 实时必达</h2>
        <p>从起点到终点，一路陪伴</p>
      </div>

      <div class="runner-layout">
        <div class="task-list">
          <article
            v-for="task in errands"
            :key="task.taskId"
            class="task-card"
            @click="handleErrandCardClick"
          >
            <div class="task-header">
              <span class="task-title">
                <Package :size="16" />
                {{ task.title }}
              </span>
              <span class="bounty">¥{{ formatPrice(task.reward) }}</span>
            </div>
            <p>{{ task.taskContent || task.description || '跑腿需求已发布，等待同学接单中。' }}</p>
            <div class="task-route">
              <MapPin :size="14" />
              <span>{{ task.pickupAddress || '校内取货点' }} → {{ task.deliveryAddress || '指定送达点' }}</span>
            </div>
          </article>
        </div>

        <div class="map-viz" aria-hidden="true">
          <div class="map-grid"></div>
          <svg class="route-svg" viewBox="0 0 100 100" preserveAspectRatio="none">
            <path d="M 8 78 C 22 24, 38 62, 52 38 C 64 18, 76 62, 92 24" />
          </svg>
          <div class="map-tag origin">取货点</div>
          <div class="map-tag waypoint">骑手配送中</div>
          <div class="map-tag destination">送达点</div>
          <div class="runner-courier">
            <Bike :size="17" />
          </div>
        </div>
      </div>
    </section>

    <section class="why-section">
      <h2 class="why-title">为什么选择Nowl？</h2>
      <div class="why-grid">
        <article v-for="item in reasons" :key="item.title" class="why-card">
          <div class="why-icon">
            <component :is="item.icon" :size="28" />
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
        </article>
      </div>
    </section>

    <button type="button" class="assistant-float" @click="openAssistant" aria-label="打开Nowl AI">
      <span class="assistant-pulse"></span>
      <span class="assistant-text"><strong>Nowl AI：</strong>同学，有什么可以帮你的？</span>
    </button>
  </div>
</template>

<style scoped src="./HomeView.css"></style>
