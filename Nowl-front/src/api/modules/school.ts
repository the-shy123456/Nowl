import { request } from '../request'
import type { SchoolInfo } from '@/types'

/**
 * 学校相关API
 */

// 查询学校列表
export const getSchoolList = () => {
  return request.get<SchoolInfo[]>('/school/list')
}

// 根据学校编码查询校区列表
export const getCampusList = (schoolCode: string) => {
  return request.get<SchoolInfo[]>(`/school/${schoolCode}/campus`)
}
