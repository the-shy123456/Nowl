import { expect, test, type Page } from '@playwright/test'

test.setTimeout(60_000)

const apiOk = <T>(data: T) => ({
  code: 200,
  message: 'ok',
  data,
  timestamp: Date.now(),
})

const buildAdminUserInfo = () => ({
  userId: 1,
  userName: 'admin',
  nickName: '超级管理员',
  schoolCode: 'SC001',
  campusCode: 'CP001',
  schoolName: '测试学校',
  campusName: '主校区',
  authStatus: 2,
  accountStatus: 0,
  roleCodes: ['SUPER_ADMIN'],
  permissionCodes: ['admin:goods:pending:view', 'admin:goods:audit'],
})

const withLoggedInAdmin = async (page: Page) => {
  await page.addInitScript((userInfo) => {
    localStorage.setItem('user-store', JSON.stringify({
      userInfo,
      currentCampus: null,
    }))
  }, buildAdminUserInfo())
}

test('管理员在商品审核页双击通过只发送一次审核请求', async ({ page }) => {
  await withLoggedInAdmin(page)

  let auditRequestCount = 0
  await page.route('**://*/api/**', async (route) => {
    const request = route.request()
    const url = request.url()

    if (url.includes('/api/user/info')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(apiOk(buildAdminUserInfo())),
      })
      return
    }
    if (url.includes('/api/notice/unread/count')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(apiOk(0)),
      })
      return
    }
    if (url.includes('/api/school/list')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(apiOk([])),
      })
      return
    }
    if (url.includes('/api/admin/goods/pending')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(apiOk({
          total: 1,
          records: [{
            productId: 1001,
            title: '并发防重测试商品',
            image: '/goods-placeholder.jpg',
            sellerId: 101,
            sellerName: '卖家A',
            price: 66,
            reviewStatus: 0,
            schoolCode: 'SC001',
            campusCode: 'CP001',
            schoolName: '测试学校',
            campusName: '主校区',
          }],
          pageNum: 1,
          pageSize: 10,
          pages: 1,
        })),
      })
      return
    }
    if (url.includes('/api/admin/goods/audit')) {
      auditRequestCount += 1
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(apiOk({})),
      })
      return
    }
    if (url.includes('/api/admin/dashboard/stats')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(apiOk({
          totalUsers: 0,
          todayNewUsers: 0,
          verifiedUsers: 0,
          totalGoods: 0,
          onSaleGoods: 0,
          todayNewGoods: 0,
          totalOrders: 0,
          todayOrders: 0,
          completedOrders: 0,
          totalAmount: 0,
          todayAmount: 0,
          pendingGoods: 1,
          pendingAuth: 0,
          pendingRunners: 0,
          pendingDisputes: 0,
          totalErrands: 0,
          activeErrands: 0,
        })),
      })
      return
    }

    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(apiOk({})),
    })
  })

  await page.goto('/admin/goods-audit')

  const targetRow = page.locator('tr', { hasText: '并发防重测试商品' })
  const approveButton = targetRow.getByRole('button', { name: '通过' })

  await expect(approveButton).toBeVisible()
  await approveButton.evaluate((node) => {
    const button = node as HTMLButtonElement
    button.click()
    button.click()
  })

  await expect.poll(() => auditRequestCount).toBe(1)
})
