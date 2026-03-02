import { request } from '../request'
import type { LoginForm, RegisterForm, UserInfo, PageResult } from '@/types'
import { AUTH_API, USER_API } from '@/config/apiPaths'

/**
 * 用户相关API
 */

// 关注用户VO类型
export interface FollowUserVO {
  userId: number
  nickName: string
  imageUrl?: string
  authStatus: number
  creditScore: number
  schoolName?: string
  campusName?: string
  followTime: string
  isMutual: boolean
  isFollowed: boolean
}

// 用户注册
export const register = (data: RegisterForm) => {
  return request.post(AUTH_API.REGISTER, data)
}

// 用户登录
export const login = (data: LoginForm) => {
  return request.post<{ userInfo: UserInfo }>(AUTH_API.LOGIN, data)
}

// 获取图形验证码
export const getCaptcha = (uuid: string) => {
  return request.get<string>(AUTH_API.CAPTCHA, { params: { uuid } })
}

// 退出登录
export const logout = () => {
  return request.post(AUTH_API.LOGOUT)
}

// 获取当前用户信息
export const getCurrentUserInfo = () => {
  return request.get<UserInfo>(USER_API.INFO)
}

// 根据用户ID获取用户信息
export const getUserInfo = (userId: number) => {
  return request.get<UserInfo>(USER_API.GET_USER(userId))
}

// 更新用户信息
export interface UpdateUserInfoDTO {
  nickName?: string
  avatar?: string
  certImage?: string
  selfImage?: string
  phone?: string
  email?: string
  userName?: string
  studentNo?: string
  schoolCode?: string
  campusCode?: string
  gender?: number
  grade?: string
  authStatus?: number
}

export const updateUserInfo = (data: UpdateUserInfoDTO) => {
  return request.put(USER_API.UPDATE_INFO, data)
}

// 申请成为跑腿员
export const applyRunner = () => {
  return request.post(USER_API.RUNNER_APPLY)
}

// 关注用户
export const followUser = (userId: number) => {
  return request.post(USER_API.FOLLOW(userId))
}

// 取消关注
export const unfollowUser = (userId: number) => {
  return request.delete(USER_API.UNFOLLOW(userId))
}

// 是否关注了该用户
export const checkFollow = (userId: number) => {
  return request.get<boolean>(USER_API.CHECK_FOLLOW(userId))
}

// 获取某用户的关注列表
export const getFollowingList = (userId: number, params: { pageNum?: number; pageSize?: number } = {}) => {
  return request.get<PageResult<FollowUserVO>>(USER_API.FOLLOWING(userId), { params })
}

// 获取某用户的粉丝列表
export const getFollowerList = (userId: number, params: { pageNum?: number; pageSize?: number } = {}) => {
  return request.get<PageResult<FollowUserVO>>(USER_API.FOLLOWERS(userId), { params })
}

// 发送短信验证码
export const sendSms = (phone: string) => {
  return request.post(AUTH_API.SEND_SMS, { phone })
}

// 重置密码
export const resetPassword = (data: { phone: string; code: string; newPassword: string }) => {
  return request.post(AUTH_API.RESET_PASSWORD, data)
}

// 刷新Token
export const refreshToken = () => {
  return request.post<void>(AUTH_API.REFRESH)
}
