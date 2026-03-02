import { request } from '../request'
import type { ErrandTask, PageQuery, PageResult } from '@/types'
import { ERRAND_API } from '@/config/apiPaths'

/**
 * 跑腿相关API
 */

// 查询跑腿任务列表
export const getErrandList = (params: PageQuery & { taskStatus?: number; keyword?: string }) => {
  return request.get<PageResult<ErrandTask>>(ERRAND_API.LIST, { params })
}

// 查询跑腿任务详情
export const getErrandDetail = (taskId: number) => {
  return request.get<ErrandTask>(ERRAND_API.DETAIL(taskId))
}

// 发布跑腿任务
export const publishErrand = (data: Partial<ErrandTask>) => {
  return request.post(ERRAND_API.PUBLISH, data)
}

// 接单
export const acceptErrand = (taskId: number) => {
  return request.post(ERRAND_API.ACCEPT(taskId))
}

// 送达任务（接单人上传凭证）
export const deliverErrand = (taskId: number, evidenceImage: string) => {
  return request.put(`${ERRAND_API.DETAIL(taskId)}/deliver`, null, {
    params: { evidenceImage }
  })
}

// 确认完成（发布者确认）
export const confirmErrand = (taskId: number) => {
  return request.put(ERRAND_API.CONFIRM(taskId))
}

// 取消任务
export const cancelErrand = (taskId: number, reason?: string) => {
  return request.delete(ERRAND_API.DELETE(taskId), {
    params: { reason }
  })
}

// 查询我发布的任务
export const getMyPublishedErrands = (params: PageQuery) => {
  return request.get<PageResult<ErrandTask>>(ERRAND_API.MY_PUBLISHED, { params })
}

// 查询我接的任务
export const getMyAcceptedErrands = (params: PageQuery) => {
  return request.get<PageResult<ErrandTask>>(ERRAND_API.MY_ACCEPTED, { params })
}

// 更新跑腿任务
export const updateErrand = (taskId: number, data: Partial<ErrandTask>) => {
  return request.put(ERRAND_API.UPDATE(taskId), data)
}
