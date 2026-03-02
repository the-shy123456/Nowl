import { request } from '../request'
import type { PageResult } from '@/types'
import { RECOMMEND_API } from '@/config/apiPaths'

/**
 * 推荐商品VO
 */
export interface RecommendItemVO {
  productId: number
  title: string
  image?: string
  price: number
  categoryId?: number
  categoryName?: string
  sellerId: number
  sellerName?: string
  collectCount?: number
  score?: number
  recommendType?: string // cf(协同过滤), content(内容推荐), hot(热门), hybrid(混合), fallback(兜底)
}

/**
 * 推荐相关API
 */

// 首页推荐（猜你喜欢）
export const getHomeRecommend = (
  pageNum: number = 1,
  pageSize: number = 20,
  schoolCode?: string,
  campusCode?: string,
) => {
  return request.get<PageResult<RecommendItemVO>>(RECOMMEND_API.HOME, {
    params: { pageNum, pageSize, schoolCode, campusCode },
  })
}

// 热门推荐
export const getHotRecommend = (
  categoryId?: number,
  size: number = 10,
  schoolCode?: string,
  campusCode?: string,
) => {
  return request.get<RecommendItemVO[]>(RECOMMEND_API.HOT, {
    params: { categoryId, size, schoolCode, campusCode },
  })
}

// 相似商品推荐（商品详情页）
export const getSimilarRecommend = (productId: number, size: number = 6) => {
  return request.get<RecommendItemVO[]>(RECOMMEND_API.SIMILAR(productId), {
    params: { size },
  })
}

// 关注的人在卖
export const getFollowingRecommend = (pageNum: number = 1, pageSize: number = 10) => {
  return request.get<PageResult<RecommendItemVO>>(RECOMMEND_API.FOLLOWING, {
    params: { pageNum, pageSize },
  })
}

// 记录商品浏览行为
export const recordViewBehavior = (
  productId: number,
  categoryId?: number,
  duration?: number
) => {
  return request.post<void>(RECOMMEND_API.VIEW_BEHAVIOR, null, {
    params: { productId, categoryId, duration },
  })
}
