import { expect, test, type Page } from '@playwright/test'

test.setTimeout(60_000)

const apiOk = <T>(data: T) => ({
  code: 200,
  message: 'ok',
  data,
  timestamp: Date.now(),
})

const withLoggedInUser = async (page: Page, userId: number) => {
  await page.addInitScript((id) => {
    localStorage.setItem('user-store', JSON.stringify({
      userInfo: {
        userId: id,
        studentNo: '20260001',
        nickName: 'E2E用户',
        schoolCode: 'SC001',
        campusCode: 'CP001',
        authStatus: 2,
        creditScore: 100,
        accountStatus: 0,
        runnableStatus: 2,
        money: 0,
        followCount: 0,
        fanCount: 0,
      },
      currentCampus: null,
    }))
  }, userId)
}

const buildUserInfo = (userId: number) => ({
  userId,
  studentNo: '20260001',
  nickName: 'E2E用户',
  schoolCode: 'SC001',
  campusCode: 'CP001',
  authStatus: 2,
  creditScore: 100,
  accountStatus: 0,
  runnableStatus: 2,
  money: 0,
  followCount: 0,
  fanCount: 0,
})

const mockApi = async (
  page: Page,
  resolver: (url: string) => unknown | undefined,
) => {
  await page.route('**://*/api/**', async (route) => {
    const url = route.request().url()
    const data = resolver(url) ?? {}
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(apiOk(data)),
    })
  })
}

test('商品详情从订单入口进入时，隐藏立即购买按钮', async ({ page }) => {
  await mockApi(page, (url) => {
    if (url.includes('/api/goods/123')) {
      return {
        productId: 123,
        sellerId: 20,
        categoryId: 1,
        title: '测试商品',
        schoolCode: 'SC001',
        campusCode: 'CP001',
        itemCondition: 1,
        price: 88,
        tradeType: 2,
        tradeStatus: 0,
        reviewStatus: 1,
        collectCount: 0,
        deliveryFee: 0,
        sellerName: '卖家A',
        schoolName: '测试学校',
        campusName: '主校区',
        createTime: '2026-02-27 10:00:00',
      }
    }
    if (url.includes('/api/recommend/similar/123')) {
      return []
    }
    return undefined
  })

  await page.goto('/product/123?fromOrderId=9527')

  await expect(page.getByRole('button', { name: '请在我的订单处理' })).toBeVisible()
  await expect(page.getByRole('button', { name: '立即购买' })).toHaveCount(0)
})

test('评价页传入非法 reviewedId 时回退到订单页', async ({ page }) => {
  await withLoggedInUser(page, 10)
  await mockApi(page, (url) => {
    if (url.includes('/api/user/info')) {
      return buildUserInfo(10)
    }
    if (url.includes('/api/notice/unread/count')) {
      return 0
    }
    if (url.includes('/api/order/1')) {
      return {
        orderId: 1,
        orderNo: 'O20260227001',
        buyerId: 10,
        sellerId: 20,
        productId: 123,
        orderAmount: 88,
        deliveryFee: 0,
        totalAmount: 88,
        orderStatus: 3,
        createTime: '2026-02-27 10:00:00',
        updateTime: '2026-02-27 10:30:00',
        productTitle: '测试商品',
      }
    }
    if (url.includes('/api/order/my')) {
      return {
        total: 0,
        records: [],
        pageNum: 1,
        pageSize: 50,
        pages: 0,
      }
    }
    return undefined
  })

  await page.goto('/review/create?type=0&id=1&reviewedId=999&from=/profile/my-orders')

  await expect(page).toHaveURL(/\/profile\/my-orders/)
})

test('评价资格不满足时禁用提交按钮', async ({ page }) => {
  await withLoggedInUser(page, 10)
  await mockApi(page, (url) => {
    if (url.includes('/api/user/info')) {
      return buildUserInfo(10)
    }
    if (url.includes('/api/notice/unread/count')) {
      return 0
    }
    if (url.includes('/api/order/1')) {
      return {
        orderId: 1,
        orderNo: 'O20260227001',
        buyerId: 10,
        sellerId: 20,
        productId: 123,
        orderAmount: 88,
        deliveryFee: 0,
        totalAmount: 88,
        orderStatus: 3,
        createTime: '2026-02-27 10:00:00',
        updateTime: '2026-02-27 10:30:00',
        productTitle: '测试商品',
      }
    }
    if (url.includes('/api/review/can-review')) {
      return false
    }
    return undefined
  })

  await page.goto('/review/create?type=0&id=1&reviewedId=20')

  await expect(page.getByText('当前评价条件不满足，可能已评价或订单/任务未完成')).toBeVisible()
  await expect(page.getByRole('button', { name: '提交评价' })).toBeDisabled()
})

test('评价页传入非法 type 时回退到订单页', async ({ page }) => {
  await withLoggedInUser(page, 10)
  await mockApi(page, (url) => {
    if (url.includes('/api/user/info')) {
      return buildUserInfo(10)
    }
    if (url.includes('/api/notice/unread/count')) {
      return 0
    }
    if (url.includes('/api/order/my')) {
      return {
        total: 0,
        records: [],
        pageNum: 1,
        pageSize: 50,
        pages: 0,
      }
    }
    return undefined
  })

  await page.goto('/review/create?type=abc&id=1&reviewedId=20')

  await expect(page).toHaveURL(/\/profile\/my-orders/)
})

test('纠纷页传入非法 type 时回退到纠纷列表', async ({ page }) => {
  await withLoggedInUser(page, 10)
  await mockApi(page, (url) => {
    if (url.includes('/api/user/info')) {
      return buildUserInfo(10)
    }
    if (url.includes('/api/notice/unread/count')) {
      return 0
    }
    if (url.includes('/api/dispute/list')) {
      return {
        total: 0,
        records: [],
        pageNum: 1,
        pageSize: 10,
        pages: 0,
      }
    }
    return undefined
  })

  await page.goto('/dispute/create?type=abc&id=1')

  await expect(page).toHaveURL(/\/dispute\/list/)
})

test('发布页传入非法商品 id 时回退到我的商品', async ({ page }) => {
  await withLoggedInUser(page, 10)
  await mockApi(page, (url) => {
    if (url.includes('/api/user/info')) {
      return buildUserInfo(10)
    }
    if (url.includes('/api/notice/unread/count')) {
      return 0
    }
    if (url.includes('/api/category/tree')) {
      return []
    }
    return undefined
  })

  await page.goto('/publish?id=abc')

  await expect(page).toHaveURL(/\/profile\/my-goods/)
})

test('私聊页传入非法 contactId 时回退到消息中心', async ({ page }) => {
  await withLoggedInUser(page, 10)
  await mockApi(page, (url) => {
    if (url.includes('/api/user/info')) {
      return buildUserInfo(10)
    }
    if (url.includes('/api/notice/unread/count')) {
      return 0
    }
    if (url.includes('/api/chat/contacts')) {
      return []
    }
    if (url.includes('/api/notice/list')) {
      return []
    }
    return undefined
  })

  await page.goto('/chat/user/abc')

  await expect(page).toHaveURL(/\/message/)
})

test('待支付订单不显示查看纠纷按钮', async ({ page }) => {
  await withLoggedInUser(page, 10)
  await mockApi(page, (url) => {
    if (url.includes('/api/user/info')) {
      return buildUserInfo(10)
    }
    if (url.includes('/api/notice/unread/count')) {
      return 0
    }
    if (url.includes('/api/order/my')) {
      return {
        total: 1,
        records: [{
          orderId: 11,
          orderNo: 'O20260228001',
          buyerId: 10,
          sellerId: 20,
          productId: 123,
          orderAmount: 88,
          deliveryFee: 0,
          totalAmount: 88,
          orderStatus: 0,
          hasActiveDispute: false,
          activeDisputeId: null,
          activeDisputeStatus: 0,
          createTime: '2026-02-28 10:00:00',
          updateTime: '2026-02-28 10:00:00',
          productTitle: '待支付测试商品',
          sellerName: '卖家A',
          buyerName: '买家A',
        }],
        pageNum: 1,
        pageSize: 50,
        pages: 1,
      }
    }
    return undefined
  })

  await page.goto('/profile/my-orders?type=buy')

  await expect(page.getByRole('button', { name: '立即支付' })).toBeVisible()
  await expect(page.getByRole('button', { name: '取消订单' })).toBeVisible()
  await expect(page.getByRole('button', { name: '查看纠纷' })).toHaveCount(0)
})

test('订单双击支付只提交一次请求', async ({ page }) => {
  await withLoggedInUser(page, 10)
  let payRequestCount = 0
  await mockApi(page, (url) => {
    if (url.includes('/api/user/info')) {
      return buildUserInfo(10)
    }
    if (url.includes('/api/notice/unread/count')) {
      return 0
    }
    if (url.includes('/api/order/my')) {
      return {
        total: 1,
        records: [{
          orderId: 21,
          orderNo: 'O20260301001',
          buyerId: 10,
          sellerId: 20,
          productId: 223,
          orderAmount: 66,
          deliveryFee: 0,
          totalAmount: 66,
          orderStatus: 0,
          refundStatus: 0,
          hasActiveDispute: false,
          activeDisputeId: null,
          activeDisputeStatus: null,
          createTime: '2026-03-01 10:00:00',
          updateTime: '2026-03-01 10:00:00',
          productTitle: '双击支付防重订单',
          sellerName: '卖家A',
          buyerName: '买家A',
        }],
        pageNum: 1,
        pageSize: 50,
        pages: 1,
      }
    }
    if (url.includes('/api/order/21/pay')) {
      payRequestCount += 1
      return {}
    }
    return undefined
  })

  await page.goto('/profile/my-orders?type=buy')

  const payButton = page.getByRole('button', { name: '立即支付' })
  await expect(payButton).toBeVisible()
  await payButton.evaluate((element) => {
    const button = element as HTMLButtonElement
    button.click()
    button.click()
  })

  const confirmButton = page.getByRole('button', { name: '确认' }).last()
  await expect(confirmButton).toBeVisible()
  await confirmButton.click()

  await expect.poll(() => payRequestCount).toBe(1)
})

test('退款被拒绝状态为字符串时买家可发起纠纷', async ({ page }) => {
  await withLoggedInUser(page, 10)
  await mockApi(page, (url) => {
    if (url.includes('/api/user/info')) {
      return buildUserInfo(10)
    }
    if (url.includes('/api/notice/unread/count')) {
      return 0
    }
    if (url.includes('/api/order/my')) {
      return {
        total: 1,
        records: [{
          orderId: 15,
          orderNo: 'O20260228005',
          buyerId: 10,
          sellerId: 20,
          productId: 127,
          orderAmount: 99,
          deliveryFee: 0,
          totalAmount: 99,
          orderStatus: 2,
          refundStatus: '3',
          hasActiveDispute: false,
          activeDisputeId: null,
          activeDisputeStatus: null,
          createTime: '2026-02-28 15:00:00',
          updateTime: '2026-02-28 15:20:00',
          productTitle: '退款拒绝字符串状态订单',
          sellerName: '卖家E',
          buyerName: '买家E',
        }],
        pageNum: 1,
        pageSize: 50,
        pages: 1,
      }
    }
    return undefined
  })

  await page.goto('/profile/my-orders?type=buy')

  await expect(page.getByRole('button', { name: '发起纠纷' })).toBeVisible()
})

test('退款状态为空时买家仍可申请退款', async ({ page }) => {
  await withLoggedInUser(page, 10)
  await mockApi(page, (url) => {
    if (url.includes('/api/user/info')) {
      return buildUserInfo(10)
    }
    if (url.includes('/api/notice/unread/count')) {
      return 0
    }
    if (url.includes('/api/order/my')) {
      return {
        total: 1,
        records: [{
          orderId: 14,
          orderNo: 'O20260228004',
          buyerId: 10,
          sellerId: 20,
          productId: 126,
          orderAmount: 128,
          deliveryFee: 0,
          totalAmount: 128,
          orderStatus: 2,
          refundStatus: null,
          hasActiveDispute: false,
          activeDisputeId: null,
          activeDisputeStatus: null,
          createTime: '2026-02-28 14:00:00',
          updateTime: '2026-02-28 14:30:00',
          productTitle: '退款状态为空订单',
          sellerName: '卖家D',
          buyerName: '买家D',
        }],
        pageNum: 1,
        pageSize: 50,
        pages: 1,
      }
    }
    return undefined
  })

  await page.goto('/profile/my-orders?type=buy')

  await expect(page.getByRole('button', { name: '申请退款' })).toBeVisible()
  await expect(page.getByRole('button', { name: '查看纠纷' })).toHaveCount(0)
})

test('订单缺少有效纠纷ID时不显示查看纠纷入口', async ({ page }) => {
  await withLoggedInUser(page, 10)
  await mockApi(page, (url) => {
    if (url.includes('/api/user/info')) {
      return buildUserInfo(10)
    }
    if (url.includes('/api/notice/unread/count')) {
      return 0
    }
    if (url.includes('/api/order/my')) {
      return {
        total: 1,
        records: [{
          orderId: 12,
          orderNo: 'O20260228002',
          buyerId: 10,
          sellerId: 20,
          productId: 124,
          orderAmount: 66,
          deliveryFee: 0,
          totalAmount: 66,
          orderStatus: 0,
          hasActiveDispute: true,
          activeDisputeId: null,
          activeDisputeStatus: 1,
          createTime: '2026-02-28 11:00:00',
          updateTime: '2026-02-28 11:00:00',
          productTitle: '纠纷字段异常商品',
          sellerName: '卖家B',
          buyerName: '买家B',
        }],
        pageNum: 1,
        pageSize: 50,
        pages: 1,
      }
    }
    return undefined
  })

  await page.goto('/profile/my-orders?type=buy')

  await expect(page.getByRole('button', { name: '立即支付' })).toBeVisible()
  await expect(page.getByRole('button', { name: '查看纠纷' })).toHaveCount(0)
  await expect(page.getByText('纠纷处理中')).toHaveCount(0)
})

test('订单评价对象无效时不显示去评价按钮', async ({ page }) => {
  await withLoggedInUser(page, 10)
  await mockApi(page, (url) => {
    if (url.includes('/api/user/info')) {
      return buildUserInfo(10)
    }
    if (url.includes('/api/notice/unread/count')) {
      return 0
    }
    if (url.includes('/api/order/my')) {
      return {
        total: 1,
        records: [{
          orderId: 13,
          orderNo: 'O20260228003',
          buyerId: 10,
          sellerId: 0,
          productId: 125,
          orderAmount: 66,
          deliveryFee: 0,
          totalAmount: 66,
          orderStatus: 3,
          hasActiveDispute: false,
          activeDisputeId: null,
          activeDisputeStatus: null,
          createTime: '2026-02-28 12:00:00',
          updateTime: '2026-02-28 12:30:00',
          productTitle: '评价对象异常订单',
          sellerName: '未知卖家',
          buyerName: '买家C',
        }],
        pageNum: 1,
        pageSize: 50,
        pages: 1,
      }
    }
    if (url.includes('/api/review/has-reviewed')) {
      return false
    }
    return undefined
  })

  await page.goto('/profile/my-orders?type=buy')

  await expect(page.getByRole('button', { name: '去评价' })).toHaveCount(0)
})

test('跑腿列表中自己发布的任务不显示立即接单', async ({ page }) => {
  await withLoggedInUser(page, 10)
  await mockApi(page, (url) => {
    if (url.includes('/api/user/info')) {
      return buildUserInfo(10)
    }
    if (url.includes('/api/notice/unread/count')) {
      return 0
    }
    if (url.includes('/api/search/errand')) {
      return {
        total: 1,
        records: [{
          taskId: 101,
          title: '代取快递',
          reward: 12,
          taskStatus: 0,
          publisherId: 10,
          pickupAddress: '菜鸟驿站',
          deliveryAddress: '宿舍A栋',
        }],
        pageNum: 1,
        pageSize: 50,
        pages: 1,
      }
    }
    return undefined
  })

  await page.goto('/errands')

  await expect(page.getByText('我发布的任务')).toBeVisible()
  await expect(page.getByRole('button', { name: '立即接单' })).toHaveCount(0)
})

test('我的跑腿评价对象无效时不显示去评价按钮', async ({ page }) => {
  await withLoggedInUser(page, 10)
  await mockApi(page, (url) => {
    if (url.includes('/api/user/info')) {
      return buildUserInfo(10)
    }
    if (url.includes('/api/notice/unread/count')) {
      return 0
    }
    if (url.includes('/api/errand/my/published')) {
      return {
        total: 1,
        records: [{
          taskId: 201,
          publisherId: 10,
          acceptorId: null,
          title: '已完成但缺少接单方',
          reward: 15,
          taskStatus: 3,
          pickupAddress: '教学楼',
          deliveryAddress: '宿舍C栋',
          createTime: '2026-02-28 12:00:00',
        }],
        pageNum: 1,
        pageSize: 50,
        pages: 1,
      }
    }
    if (url.includes('/api/errand/my/accepted')) {
      return {
        total: 0,
        records: [],
        pageNum: 1,
        pageSize: 50,
        pages: 0,
      }
    }
    if (url.includes('/api/review/has-reviewed')) {
      return false
    }
    return undefined
  })

  await page.goto('/profile/my-errands?tab=published')

  await expect(page.getByRole('button', { name: '去评价' })).toHaveCount(0)
})

test('跑腿编辑页传入非法 taskId 时回退到我的跑腿', async ({ page }) => {
  await withLoggedInUser(page, 10)
  await mockApi(page, (url) => {
    if (url.includes('/api/user/info')) {
      return buildUserInfo(10)
    }
    if (url.includes('/api/notice/unread/count')) {
      return 0
    }
    if (url.includes('/api/errand/my/published') || url.includes('/api/errand/my/accepted')) {
      return {
        total: 0,
        records: [],
        pageNum: 1,
        pageSize: 50,
        pages: 0,
      }
    }
    return undefined
  })

  await page.goto('/errand/edit/not-a-number')

  await expect(page).toHaveURL(/\/profile\/my-errands/)
})

test('未登录用户在跑腿详情页不显示立即接单', async ({ page }) => {
  await mockApi(page, (url) => {
    if (url.includes('/api/errand/101')) {
      return {
        taskId: 101,
        publisherId: 20,
        title: '代取快递',
        reward: 12,
        taskStatus: 0,
        pickupAddress: '菜鸟驿站',
        deliveryAddress: '宿舍A栋',
        createTime: '2026-02-28 10:00:00',
      }
    }
    return undefined
  })

  await page.goto('/errand/101')

  await expect(page.getByRole('button', { name: '立即接单' })).toHaveCount(0)
  await expect(page.getByText('当前状态暂无可执行操作')).toBeVisible()
})

test('跑腿详情非参与者不显示去评价按钮', async ({ page }) => {
  await withLoggedInUser(page, 10)
  await mockApi(page, (url) => {
    if (url.includes('/api/user/info')) {
      return buildUserInfo(10)
    }
    if (url.includes('/api/notice/unread/count')) {
      return 0
    }
    if (url.includes('/api/errand/102')) {
      return {
        taskId: 102,
        publisherId: 20,
        acceptorId: 30,
        title: '已完成跑腿',
        reward: 18,
        taskStatus: 3,
        pickupAddress: '图书馆',
        deliveryAddress: '宿舍B栋',
        createTime: '2026-02-28 09:00:00',
      }
    }
    if (url.includes('/api/review/has-reviewed')) {
      return false
    }
    return undefined
  })

  await page.goto('/errand/102')

  await expect(page.getByRole('button', { name: '去评价' })).toHaveCount(0)
  await expect(page.getByText('当前状态暂无可执行操作')).toBeVisible()
})

test('跑腿纠纷创建页对非参与者禁用提交', async ({ page }) => {
  await withLoggedInUser(page, 10)
  await mockApi(page, (url) => {
    if (url.includes('/api/user/info')) {
      return buildUserInfo(10)
    }
    if (url.includes('/api/notice/unread/count')) {
      return 0
    }
    if (url.includes('/api/errand/103')) {
      return {
        taskId: 103,
        publisherId: 20,
        acceptorId: 30,
        title: '他人跑腿任务',
        reward: 20,
        taskStatus: 1,
        pickupAddress: '快递站',
        deliveryAddress: '宿舍D栋',
        createTime: '2026-02-28 13:00:00',
      }
    }
    return undefined
  })

  await page.goto('/dispute/create?type=1&id=103')

  await expect(page.getByText('当前跑腿暂不满足纠纷条件（需为任务参与方，且任务已被接单）。')).toBeVisible()
  await expect(page.getByRole('button', { name: '提交纠纷' })).toBeDisabled()
})
