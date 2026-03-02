import { request } from '../request'
import type { OrderInfo, PageQuery, PageResult } from '@/types'
import { ORDER_API } from '@/config/apiPaths'

/**
 * 订单相关API
 */

// 创建订单
export const createOrder = (data: { productId: number; remark?: string }) => {
  return request.post<void>(ORDER_API.CREATE, data)
}

// 查询订单详情
export const getOrderDetail = (orderId: number) => {
  return request.get<OrderInfo>(ORDER_API.DETAIL(orderId))
}

// 查询我的订单
export const getMyOrders = (params: PageQuery & { orderStatus?: number; orderType?: 'buy' | 'sell' }) => {
  return request.get<PageResult<OrderInfo>>(ORDER_API.LIST, { params })
}

// 支付订单
export const payOrder = (orderId: number) => {
  return request.put(ORDER_API.PAY(orderId))
}

// 发货
export const deliverOrder = (orderId: number) => {
  return request.put(ORDER_API.DELIVER(orderId))
}

// 确认收货
export const confirmOrder = (orderId: number) => {
  return request.put(ORDER_API.CONFIRM(orderId))
}

// 取消订单
export const cancelOrder = (orderId: number) => {
  return request.put(ORDER_API.CANCEL(orderId))
}

// 申请退款
export const applyRefund = (orderId: number, data: { reason: string; amount: number }) => {
  return request.post(ORDER_API.REFUND_APPLY(orderId), data)
}

// 卖家处理退款
export const processRefund = (orderId: number, data: { action: 'approve' | 'reject'; remark?: string }) => {
  return request.put(ORDER_API.REFUND_PROCESS(orderId), data)
}

// 发起纠纷
export const applyDispute = (data: { orderId: number; reason: string; evidenceImages?: string }) => {
  return request.post(ORDER_API.DISPUTE, data)
}
