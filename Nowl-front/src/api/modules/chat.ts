import { request } from '../request'
import { CHAT_API } from '@/config/apiPaths'

export interface ChatMessage {
  senderId: number
  receiverId: number
  content: string
  createTime: string
}

export interface Contact {
  userId: number
  nickName: string
  avatar?: string
  lastMessage?: string
  lastTime?: string
  unreadCount?: number
}

export interface ChatBlockItem {
  userId: number
  nickName: string
  avatar?: string
  blockTime?: string
}

export const getChatHistory = (contactId: number) => {
  return request.get<ChatMessage[]>(CHAT_API.HISTORY(contactId))
}

export const getContactList = () => {
  return request.get<Contact[]>(CHAT_API.CONTACTS)
}

export const markChatAsRead = (contactId: number) => {
  return request.post(CHAT_API.READ(contactId))
}

export const markAllChatsAsRead = () => {
  return request.post(CHAT_API.READ_ALL)
}

export const sendMessage = (data: { receiverId: number; content: string; type?: number }) => {
  return request.post(CHAT_API.SEND, data)
}

export const blockUser = (targetUserId: number) => {
  return request.post(CHAT_API.BLOCK(targetUserId))
}

export const unblockUser = (targetUserId: number) => {
  return request.delete(CHAT_API.BLOCK(targetUserId))
}

export const getBlockList = (keyword?: string) => {
  return request.get<ChatBlockItem[]>(CHAT_API.BLOCKS, {
    params: keyword?.trim() ? { keyword: keyword.trim() } : undefined,
  })
}

export const getBlockRelation = (targetUserId: number) => {
  return request.get<number>(CHAT_API.BLOCK_RELATION(targetUserId))
}
