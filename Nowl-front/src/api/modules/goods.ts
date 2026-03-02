import { request } from '../request'
import type { GoodsInfo, GoodsQuery, PageResult, ItemCategory } from '@/types'
import { GOODS_API, CATEGORY_API } from '@/config/apiPaths'

/**
 * 商品相关API
 */

// 查询商品列表
export const getGoodsList = (params: GoodsQuery) => {
  return request.get<PageResult<GoodsInfo>>(GOODS_API.LIST, { params })
}

// 查询商品详情
export const getGoodsDetail = (productId: number) => {
  return request.get<GoodsInfo>(GOODS_API.DETAIL(productId))
}

// 发布商品
export const publishGoods = (data: Partial<GoodsInfo>) => {
  return request.post(GOODS_API.PUBLISH, data)
}

// 更新商品
export const updateGoods = (productId: number, data: Partial<GoodsInfo>) => {
  return request.put(GOODS_API.UPDATE(productId), data)
}

// 删除商品
export const deleteGoods = (productId: number) => {
  return request.delete(GOODS_API.DELETE(productId))
}

// 下架商品
export const offshelfGoods = (productId: number) => {
  return request.put(GOODS_API.OFFSHELF(productId))
}

// 收藏商品
export const collectGoods = (productId: number) => {
  return request.post(GOODS_API.COLLECT(productId))
}

// 取消收藏
export const uncollectGoods = (productId: number) => {
  return request.delete(GOODS_API.UNCOLLECT(productId))
}

// 查询商品分类树
export const getCategoryTree = () => {
  return request.get<ItemCategory[]>(CATEGORY_API.TREE)
}

// 查询一级分类列表
export const getCategoryList = () => {
  return request.get<ItemCategory[]>(CATEGORY_API.LIST)
}

// 查询我的商品
export const getMyGoods = (params: GoodsQuery) => {
  return request.get<PageResult<GoodsInfo>>(GOODS_API.MY_GOODS, { params })
}

// 查询我的收藏
export const getMyCollections = (params: GoodsQuery) => {
  return request.get<PageResult<GoodsInfo>>(GOODS_API.MY_COLLECTIONS, { params })
}
