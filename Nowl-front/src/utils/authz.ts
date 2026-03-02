import type { UserInfo } from '@/types'

const ADMIN_ROLE_CODES = new Set([
  'SUPER_ADMIN',
  'SCHOOL_ADMIN',
  'CAMPUS_ADMIN',
  'CONTENT_AUDITOR',
  'RISK_OPERATOR',
  'CUSTOMER_SUPPORT',
  'FINANCE_AUDITOR',
])

export const hasAdminAccess = (user?: UserInfo | null): boolean => {
  if (!user) return false

  const roleCodes = user.roleCodes || []
  if (roleCodes.some((role) => ADMIN_ROLE_CODES.has(role))) {
    return true
  }

  const permissionCodes = user.permissionCodes || []
  return permissionCodes.some((code) => code.startsWith('admin:') || code.startsWith('risk:'))
}

export const hasPermission = (user: UserInfo | null | undefined, permissionCode: string): boolean => {
  if (!user || !permissionCode) return false
  if ((user.roleCodes || []).includes('SUPER_ADMIN')) return true
  return (user.permissionCodes || []).includes(permissionCode)
}

export const hasAnyPermission = (user: UserInfo | null | undefined, permissionCodes: string[]): boolean => {
  if (!permissionCodes.length) return true
  return permissionCodes.some((code) => hasPermission(user, code))
}
