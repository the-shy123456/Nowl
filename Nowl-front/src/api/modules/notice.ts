import { request } from '../request';
import { NOTICE_API } from '@/config/apiPaths';

export interface Notice {
  noticeId: number
  title: string
  content: string
  type: number
  relatedId?: number | null
  bizType?: 'system' | 'product' | 'order' | 'errand' | 'dispute' | 'review'
  isRead?: number
  createTime: string
}

export const getNoticeList = () => {
  return request.get<Notice[]>(NOTICE_API.LIST)
}

export const markNoticeAsRead = (noticeId: number) => {
  return request.post(NOTICE_API.READ(noticeId))
}

export const markAllNoticesAsRead = () => {
  return request.post(NOTICE_API.READ_ALL)
}

export const getUnreadNoticeCount = () => {
  return request.get<number>(NOTICE_API.UNREAD_COUNT)
}
