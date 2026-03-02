/**
 * 状态/展示映射常量
 */

export const ERRAND_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待接单', color: 'bg-warm-100 text-warm-600' },
  1: { text: '进行中', color: 'bg-yellow-100 text-yellow-600' },
  2: { text: '待确认', color: 'bg-orange-100 text-orange-600' },
  3: { text: '已完成', color: 'bg-green-100 text-green-600' },
  4: { text: '已取消', color: 'bg-gray-100 text-gray-500' },
}

export const DISPUTE_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待处理', color: 'bg-yellow-100 text-yellow-600' },
  1: { text: '处理中', color: 'bg-warm-100 text-warm-600' },
  2: { text: '已解决', color: 'bg-green-100 text-green-600' },
  3: { text: '已驳回', color: 'bg-red-100 text-red-600' },
  4: { text: '已撤回', color: 'bg-gray-100 text-gray-500' },
}

export const REFUND_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '无退款', color: 'bg-slate-100 text-slate-600' },
  1: { text: '退款处理中', color: 'bg-yellow-100 text-yellow-700' },
  2: { text: '已退款', color: 'bg-emerald-100 text-emerald-700' },
  3: { text: '退款被拒绝', color: 'bg-red-100 text-red-600' },
}

export const RATING_MAP: Record<number, { text: string; color: string }> = {
  1: { text: '非常差', color: 'text-red-500' },
  2: { text: '不满意', color: 'text-orange-500' },
  3: { text: '一般', color: 'text-yellow-500' },
  4: { text: '满意', color: 'text-green-500' },
  5: { text: '非常满意', color: 'text-emerald-500' },
}

export const CREDIT_LEVEL_MAP: Record<string, { text: string; color: string; bgColor: string }> = {
  gold: { text: '优秀', color: 'text-yellow-600', bgColor: 'bg-yellow-100' },
  green: { text: '良好', color: 'text-green-600', bgColor: 'bg-green-100' },
  blue: { text: '一般', color: 'text-warm-600', bgColor: 'bg-warm-100' },
  orange: { text: '较差', color: 'text-orange-600', bgColor: 'bg-orange-100' },
  red: { text: '危险', color: 'text-red-600', bgColor: 'bg-red-100' },
}

export const ErrandStatusMap = ERRAND_STATUS_MAP
export const DisputeStatusMap = DISPUTE_STATUS_MAP
export const RefundStatusMap = REFUND_STATUS_MAP
export const RatingMap = RATING_MAP
export const CreditLevelMap = CREDIT_LEVEL_MAP
