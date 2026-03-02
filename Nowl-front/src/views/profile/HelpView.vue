<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  BookOpenText,
  ChevronRight,
  CircleCheckBig,
  FileText,
  HelpCircle,
  MessageCircle,
  Phone,
  Scale,
  ShieldAlert,
} from 'lucide-vue-next'
import { ElMessage } from '@/utils/feedback'
import SubPageShell from '@/components/SubPageShell.vue'

const openIndex = ref(0)

const flowSteps = [
  '订单处于待确认收货阶段时，优先与对方协商（必要时可申请退款）',
  '协商无果可发起纠纷并提交证据（资金仍由平台托管）',
  '双方最多各补充 3 次说明与图片',
  '平台结合证据给出最终裁定（可裁定扣分与退款金额）',
  '裁定完成后纠纷完结，订单进入已结束状态',
]

const faqList = [
  {
    title: '如何申请退款？',
    content:
      '订单处于待发货/待收货时，可在“我的订单”发起退款。部分高信用场景支持极速退款，其余由卖家处理或系统超时自动退款。',
  },
  {
    title: '什么情况下应该发起纠纷？',
    content:
      '当订单处于待确认收货（资金托管中）且存在履约争议时，可发起纠纷并上传最多 9 张证据图；跑腿纠纷需为任务参与方且任务已被接单。',
  },
  {
    title: '纠纷处理流程多久有结果？',
    content:
      '纠纷提交后进入审核队列，双方补充材料后由管理员结合证据处理。复杂案例会延长处理时长，请留意通知。',
  },
  {
    title: '平台如何保障交易资金？',
    content:
      '买家支付后资金由平台托管，确认收货后结算给卖家；退款成立时将按裁定直接返还买家账户。',
  },
  {
    title: '如何提高纠纷通过率？',
    content:
      '建议上传清晰、可核验的证据，说明要包含时间、商品/任务信息及争议点，避免只给结论不提供过程。',
  },
]

const fallbackFaq = {
  title: '常见问题',
  content: '内容建设中',
}

const openedFaq = computed(() => faqList[openIndex.value] ?? faqList[0] ?? fallbackFaq)

const openCustomerService = () => {
  ElMessage.info('在线客服接入中，建议先查看调解流程与常见问题')
}

const callService = () => {
  ElMessage.info('服务热线：400-123-4567（工作日 9:00-18:00）')
}

const openDoc = (name: string) => {
  ElMessage.info(`${name}正在整理中，稍后上线`)
}
</script>

<template>
  <SubPageShell title="校园调解帮助中心" subtitle="退款、纠纷与资金保障说明" back-to="/profile" max-width="lg" :use-card="false">
    <template #icon>
      <HelpCircle class="text-white w-8 h-8" stroke-width="2.5" />
    </template>

    <div class="space-y-4 pb-4">
      <section class="um-card p-5 bg-gradient-to-r from-warm-50 via-orange-50 to-amber-50 border border-warm-100">
        <div class="flex items-start gap-3">
          <ShieldAlert class="w-6 h-6 text-warm-500 mt-0.5 shrink-0" />
          <div class="min-w-0">
            <h2 class="font-bold text-slate-800">遇到交易争议？先看这一页</h2>
            <p class="text-sm text-slate-600 mt-1 leading-relaxed">
              出现退款、履约或证据分歧时，建议先在订单/跑腿详情页协商与申请退款，再进入纠纷流程。准备好清晰证据能显著提升处理效率。
            </p>
          </div>
        </div>
      </section>

      <div class="grid md:grid-cols-2 gap-4">
        <section class="um-card p-5">
          <h3 class="font-semibold text-slate-800 mb-3 inline-flex items-center gap-2">
            <Scale :size="16" class="text-warm-500" />
            调解流程
          </h3>
          <ol class="space-y-2 text-sm text-slate-600">
            <li
              v-for="(item, index) in flowSteps"
              :key="item"
              class="flex items-start gap-2.5"
            >
              <CircleCheckBig class="w-4 h-4 text-emerald-500 mt-0.5 shrink-0" />
              <span>{{ index + 1 }}. {{ item }}</span>
            </li>
          </ol>
        </section>

        <section class="um-card p-5">
          <h3 class="font-semibold text-slate-800 mb-3 inline-flex items-center gap-2">
            <BookOpenText :size="16" class="text-warm-500" />
            常用入口
          </h3>
          <div class="space-y-2.5">
            <button
              @click="openCustomerService"
              class="w-full text-left rounded-xl border border-slate-100 bg-slate-50 px-3 py-3 hover:border-warm-200 hover:bg-warm-50 transition-colors"
            >
              <div class="flex items-center justify-between gap-3">
                <div class="flex items-center gap-3 min-w-0">
                  <MessageCircle class="w-5 h-5 text-warm-500 shrink-0" />
                  <div class="min-w-0">
                    <p class="text-sm font-medium text-slate-700">在线客服</p>
                    <p class="text-xs text-slate-400">工作日 9:00-18:00</p>
                  </div>
                </div>
                <ChevronRight :size="15" class="text-slate-400" />
              </div>
            </button>

            <button
              @click="callService"
              class="w-full text-left rounded-xl border border-slate-100 bg-slate-50 px-3 py-3 hover:border-warm-200 hover:bg-warm-50 transition-colors"
            >
              <div class="flex items-center justify-between gap-3">
                <div class="flex items-center gap-3 min-w-0">
                  <Phone class="w-5 h-5 text-warm-500 shrink-0" />
                  <div class="min-w-0">
                    <p class="text-sm font-medium text-slate-700">电话咨询</p>
                    <p class="text-xs text-slate-400">400-123-4567</p>
                  </div>
                </div>
                <ChevronRight :size="15" class="text-slate-400" />
              </div>
            </button>
          </div>
        </section>
      </div>

      <section class="um-card p-5">
        <h3 class="font-semibold text-slate-800 mb-4">常见问题</h3>

        <div class="flex flex-wrap gap-2 mb-4">
          <button
            v-for="(item, index) in faqList"
            :key="item.title"
            @click="openIndex = index"
            class="px-3 py-1.5 rounded-full text-xs transition-colors"
            :class="openIndex === index ? 'bg-warm-500 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'"
          >
            {{ item.title }}
          </button>
        </div>

        <div class="rounded-2xl border border-slate-100 bg-slate-50 p-4">
          <h4 class="font-medium text-slate-700">{{ openedFaq.title }}</h4>
          <p class="text-sm text-slate-600 leading-relaxed mt-2">{{ openedFaq.content }}</p>
        </div>
      </section>

      <section class="um-card p-4">
        <div class="flex flex-wrap items-center justify-center gap-5 text-xs text-slate-500">
          <button class="hover:text-slate-700 inline-flex items-center gap-1" @click="openDoc('用户协议')">
            <FileText :size="13" />
            用户协议
          </button>
          <button class="hover:text-slate-700 inline-flex items-center gap-1" @click="openDoc('隐私政策')">
            <FileText :size="13" />
            隐私政策
          </button>
          <button class="hover:text-slate-700 inline-flex items-center gap-1" @click="openDoc('社区规范')">
            <FileText :size="13" />
            社区规范
          </button>
        </div>
      </section>
    </div>
  </SubPageShell>
</template>
