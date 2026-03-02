import { request } from '../request'
import { AI_API } from '@/config/apiPaths'

/**
 * AI相关API
 */

// AI聊天消息类型
export interface AiChatMessageVO {
  id: number
  userId: number
  role: 'user' | 'model'
  content: string
  imageUrl?: string
  cards?: AiGoodsCardVO[]
  createTime: string
}

// AI返回的商品卡片
export interface AiGoodsCardVO {
  productId: number
  title: string
  price: number
  image?: string
  sellerName?: string
  schoolCode?: string
  campusCode?: string
}

// AI聊天结构化响应
export interface AiChatResponseVO {
  replyText: string
  cards?: AiGoodsCardVO[]
  intent?: string
  keyword?: string
  queryLimit?: number
  queryPage?: number
  maxPrice?: number
  total?: number
  hasMore?: boolean
}

export interface AiChatQueryContext {
  intent?: string
  keyword?: string
  limit?: number
  maxPrice?: number
  page?: number
  switchBatch?: boolean
}


// 商品估价请求参数
export interface GoodsPriceEstimateDTO {
  title: string
  description: string
  categoryId: number
  imageUrl?: string
  itemCondition: number
  referenceData?: string
}

// 商品估价结果
export interface GoodsPriceEstimateVO {
  suggestedPrice: number
  reason: string
  referenceCount: number
}

// 与 Nowl AI 对话
export const chatWithAi = (message: string, imageUrl?: string, queryContext?: AiChatQueryContext) => {
  return request.post<AiChatResponseVO>(AI_API.CHAT, { message, imageUrl, queryContext })
}

// 获取AI聊天历史记录
export const getAiChatHistory = () => {
  return request.get<AiChatMessageVO[]>(AI_API.HISTORY)
}

// 清除AI聊天历史记录
export const clearAiChatHistory = () => {
  return request.delete(AI_API.HISTORY)
}

// AI商品估价（基于数据库同类商品查询）
export const estimatePriceWithAi = (data: GoodsPriceEstimateDTO) => {
  return request.post<GoodsPriceEstimateVO>(AI_API.PRICE_ESTIMATE, data)
}
