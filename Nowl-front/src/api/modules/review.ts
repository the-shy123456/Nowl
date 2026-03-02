import { request } from '../request'
import type { PageResult } from '@/types'
import { REVIEW_API } from '@/config/apiPaths'

export interface ReviewListItem {
  reviewId: number
  targetType: number
  targetTypeDesc: string
  orderId?: number
  productId?: number
  taskId?: number
  reviewerId: number
  reviewerName: string
  reviewerAvatar?: string
  reviewerRole?: string
  reviewedRole?: string
  rating: number
  content?: string
  anonymous: boolean
  contentTitle?: string
  contentImage?: string
  createTime: string
}

export interface UserReviewStats {
  userId: number
  averageRating: number
  totalReviews: number
  goodReviews: number
  goodRate: number
  creditScore: number
  creditLevel: string
  creditColor: string
}

export interface ReviewCreateParams {
  orderId?: number
  taskId?: number
  targetType: number // 0-商品交易，1-跑腿任务
  reviewedId: number
  rating: number // 1-5星
  content?: string
  anonymous?: number // 0-否，1-是
}

// 创建评价
export const createReview = (data: ReviewCreateParams) => {
  return request.post(REVIEW_API.CREATE, data)
}

// 获取用户收到的评价列表
export const getReceivedReviews = (userId: number, params: { pageNum?: number; pageSize?: number }) => {
  return request.get<PageResult<ReviewListItem>>(REVIEW_API.RECEIVED(userId), { params })
}

// 获取我发出的评价列表
export const getSentReviews = (params: { pageNum?: number; pageSize?: number }) => {
  return request.get<PageResult<ReviewListItem>>(REVIEW_API.SENT, { params })
}

// 获取用户评价统计信息
export const getUserReviewStats = (userId: number) => {
  return request.get<UserReviewStats>(REVIEW_API.STATS(userId))
}

// 检查是否可以评价
export const canReview = (params: { targetType: number; contentId: number; reviewedId: number }) => {
  return request.get<boolean>(REVIEW_API.CAN_REVIEW, { params })
}

// 检查是否已评价
export const hasReviewed = (params: { targetType: number; orderId?: number; taskId?: number }) => {
  return request.get<boolean>(REVIEW_API.HAS_REVIEWED, { params })
}
