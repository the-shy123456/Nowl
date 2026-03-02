import type { InjectionKey } from 'vue'
import { inject, provide } from 'vue'

export type AdminDashboardContext = Record<string, unknown>

export const adminDashboardKey: InjectionKey<AdminDashboardContext> = Symbol('AdminDashboardContext')

export const provideAdminDashboardContext = (ctx: AdminDashboardContext) => {
  provide(adminDashboardKey, ctx)
  return ctx
}

export const useAdminDashboardContext = <T extends AdminDashboardContext = AdminDashboardContext>() => {
  const ctx = inject(adminDashboardKey, null) as T | null
  if (!ctx) {
    throw new Error('AdminDashboardContext not provided')
  }
  return ctx
}
