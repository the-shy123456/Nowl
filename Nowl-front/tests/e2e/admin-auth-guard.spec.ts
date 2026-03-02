import { expect, test } from '@playwright/test'

test('unauthenticated user is redirected to login from admin route', async ({ page }) => {
  await page.goto('/admin/risk')
  await expect(page).toHaveURL(/\/login\?redirect=%2Fadmin%2Frisk/)
  await expect(page.getByText('欢迎回来')).toBeVisible()
})

test('admin root route redirects to dashboard then login', async ({ page }) => {
  await page.goto('/admin')
  await expect(page).toHaveURL(/\/login\?redirect=%2Fadmin%2Fdashboard/)
})
