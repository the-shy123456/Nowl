import { request } from '../request'
import type { PageResult } from '@/types'
import { SEARCH_API } from '@/config/apiPaths'

/**
 * 搜索结果VO
 */
export interface SearchResultVO {
  productId: number
  title: string
  description?: string
  categoryId: number
  categoryName?: string
  price: number
  sellerId: number
  sellerName?: string
  sellerAvatar?: string
  schoolName?: string
  campusName?: string
  schoolCode?: string
  campusCode?: string
  tradeStatus: number
  image?: string
  collectCount?: number
  viewCount?: number
  hotScore?: number
  score?: number
  createTime?: string
  isCollected?: boolean
}

/**
 * 搜索请求参数
 */
export interface SearchRequestDTO {
  keyword?: string
  categoryId?: number
  parentCategoryId?: number // 一级分类ID
  minPrice?: number
  maxPrice?: number
  schoolCode?: string
  campusCode?: string
  tradeStatus?: number
  sellerId?: number
  sortType?: number // 0-综合 1-最新 2-价格升序 3-价格降序 4-热度
  pageNum?: number
  pageSize?: number
}

/**
 * 搜索相关API
 */

// 商品搜索
export const searchGoods = (params: SearchRequestDTO) => {
  return request.get<PageResult<SearchResultVO>>(SEARCH_API.GOODS, { params })
}

// 搜索建议
export const getSearchSuggestions = (keyword: string, size: number = 10) => {
  return request.get<string[]>(SEARCH_API.SUGGESTIONS, { params: { keyword, size } })
}

// 获取热搜词
export const getHotWords = (schoolCode?: string, size: number = 10) => {
  return request.get<string[]>(SEARCH_API.HOT_WORDS, { params: { schoolCode, size } })
}

// 获取搜索历史
export const getSearchHistory = (size: number = 10) => {
  return request.get<string[]>(SEARCH_API.HISTORY, { params: { size } })
}

// 清空搜索历史
export const clearSearchHistory = () => {
  return request.delete<void>(SEARCH_API.CLEAR_HISTORY)
}

/**
 * 跑腿搜索请求参数
 */
export interface ErrandSearchRequestDTO {
  keyword?: string
  taskStatus?: number
  schoolCode?: string
  campusCode?: string
  minReward?: number
  maxReward?: number
  sortType?: number // 0-综合 1-最新 2-赏金升序 3-赏金降序
  pageNum?: number
  pageSize?: number
}

/**
 * 跑腿搜索结果VO
 */
export interface ErrandSearchResultVO {
  taskId: number
  title: string
  description?: string
  taskContent?: string
  reward: number
  taskStatus: number
  statusText?: string
  publisherId: number
  publisherName?: string
  publisherAvatar?: string
  schoolCode?: string
  campusCode?: string
  pickupAddress?: string
  deliveryAddress?: string
  image?: string
  deadline?: string
  createTime?: string
  score?: number
}

// 跑腿搜索
export const searchErrands = (params: ErrandSearchRequestDTO) => {
  return request.get<PageResult<ErrandSearchResultVO>>(SEARCH_API.ERRAND, { params })
}
