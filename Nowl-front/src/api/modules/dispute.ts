import { request } from '../request'
import type { PageResult } from '@/types'
import { DISPUTE_API } from '@/config/apiPaths'

/**
 * 纠纷相关API
 */

// 纠纷列表项
export interface DisputeListItem {
  recordId: number
  targetType: number
  targetTypeDesc: string
  contentId: number
  contentTitle: string
  contentImage?: string
  contentSummary: string
  handleStatus: number
  statusDesc: string
  otherUserId: number
  otherUserName: string
  otherUserAvatar?: string
  isInitiator: boolean
  createTime: string
  updateTime: string
}

// 纠纷详情
export interface DisputeDetail {
  recordId: number
  targetType: number
  targetTypeDesc: string
  contentId: number
  content: string
  evidenceUrlList: string[]
  handleStatus: number
  statusDesc: string
  handleResult?: string
  claimSellerCreditPenalty?: number
  claimRefund?: number
  claimRefundAmount?: number
  initiatorReplyCount?: number
  relatedReplyCount?: number
  canReply?: boolean
  conversations?: Array<{
    userId: number
    userName: string
    userAvatar?: string
    initiator: boolean
    content?: string
    evidenceUrls?: string[]
    createTime: string
  }>
  handleTime?: string
  isInitiator: boolean
  canWithdraw: boolean
  // 发起人信息
  initiatorId: number
  initiatorName: string
  initiatorAvatar?: string
  // 被投诉方信息
  relatedId: number
  relatedName: string
  relatedAvatar?: string
  // 订单信息（targetType=0时）
  orderNo?: string
  productTitle?: string
  productImage?: string
  orderAmount?: number
  orderStatus?: number
  // 跑腿信息（targetType=1时）
  errandTitle?: string
  errandImage?: string
  errandReward?: number
  errandStatus?: number
  createTime: string
  updateTime: string
}

// 创建纠纷参数
export interface DisputeCreateParams {
  contentId: number
  targetType: number // 0-商品交易，1-跑腿劳务
  content: string
  evidenceUrls?: string
  claimSellerCreditPenalty: number
  claimRefund: number
  claimRefundAmount?: number
}

// 补充证据参数
export interface DisputeEvidenceParams {
  recordId: number
  additionalContent?: string
  additionalEvidence?: string
}

// 发起纠纷
export const createDispute = (data: DisputeCreateParams) => {
  return request.post(DISPUTE_API.CREATE, data)
}

// 获取我的纠纷列表
export const getDisputeList = (params: {
  pageNum?: number
  pageSize?: number
  handleStatus?: number
}) => {
  return request.get<PageResult<DisputeListItem>>(DISPUTE_API.LIST, { params })
}

// 获取纠纷详情
export const getDisputeDetail = (recordId: number) => {
  return request.get<DisputeDetail>(DISPUTE_API.DETAIL(recordId))
}

// 撤回纠纷
export const withdrawDispute = (recordId: number) => {
  return request.put(DISPUTE_API.WITHDRAW(recordId))
}

// 补充证据
export const addDisputeEvidence = (data: DisputeEvidenceParams) => {
  return request.post(DISPUTE_API.EVIDENCE, data)
}
