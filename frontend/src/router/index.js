import { createRouter, createWebHistory } from 'vue-router'
import { useAppStore } from '@/stores/app'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/',
    component: () => import('@/components/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '控制台' } },
      { path: 'tenants', name: 'Tenants', component: () => import('@/views/tenants/Index.vue'), meta: { title: '租户管理' } },
      { path: 'systems', name: 'Systems', component: () => import('@/views/systems/Index.vue'), meta: { title: '外部系统' } },
      { path: 'points/actions', name: 'PointActions', component: () => import('@/views/points/Actions.vue'), meta: { title: '积分动作' } },
      { path: 'points/rules', name: 'PointRules', component: () => import('@/views/points/Rules.vue'), meta: { title: '积分规则' } },
      { path: 'points/accounts', name: 'PointAccounts', component: () => import('@/views/points/Accounts.vue'), meta: { title: '积分账户' } },
      { path: 'points/transactions', name: 'PointTransactions', component: () => import('@/views/points/Transactions.vue'), meta: { title: '积分流水' } },
      { path: 'points/gift/apply', name: 'PointGiftApply', component: () => import('@/views/points/gift/Apply.vue'), meta: { title: '赠送申请' } },
      { path: 'points/gift/approval', name: 'PointGiftApproval', component: () => import('@/views/points/gift/Approval.vue'), meta: { title: '赠送审批' } },
      { path: 'points/gift/report', name: 'PointGiftReport', component: () => import('@/views/points/gift/Report.vue'), meta: { title: '赠送报表' } },
      { path: 'wallet/actions', name: 'WalletActions', component: () => import('@/views/wallet/Actions.vue'), meta: { title: '消费动作' } },
      { path: 'wallet/rules', name: 'WalletRules', component: () => import('@/views/wallet/Rules.vue'), meta: { title: '消费规则' } },
      { path: 'wallet/promotions', name: 'WalletPromotions', component: () => import('@/views/wallet/Promotions.vue'), meta: { title: '充值促销' } },
      { path: 'wallet/accounts', name: 'WalletAccounts', component: () => import('@/views/wallet/Accounts.vue'), meta: { title: '钱包账户' } },
      { path: 'wallet/transactions', name: 'WalletTransactions', component: () => import('@/views/wallet/Transactions.vue'), meta: { title: '钱包流水' } },
      { path: 'wallet/adjust', name: 'WalletAdjust', component: () => import('@/views/wallet/Adjust.vue'), meta: { title: '手动调账' } },
      { path: 'members/levels', name: 'MemberLevels', component: () => import('@/views/members/Levels.vue'), meta: { title: '会员等级' } },
      { path: 'members/users', name: 'MemberUsers', component: () => import('@/views/members/Users.vue'), meta: { title: '会员用户' } },
      { path: 'notifications/templates', name: 'NotifyTemplates', component: () => import('@/views/notifications/Templates.vue'), meta: { title: '通知模板' } },
      { path: 'notifications/rules', name: 'NotifyRules', component: () => import('@/views/notifications/Rules.vue'), meta: { title: '通知规则' } },
      { path: 'notifications/records', name: 'NotifyRecords', component: () => import('@/views/notifications/Records.vue'), meta: { title: '通知记录' } },
      { path: 'webhooks', name: 'Webhooks', component: () => import('@/views/webhooks/Index.vue'), meta: { title: 'Webhook' } },
      { path: 'webhooks/logs', name: 'WebhookLogs', component: () => import('@/views/webhooks/Logs.vue'), meta: { title: 'Webhook日志' } },
      { path: 'invoice/apply', name: 'InvoiceApply', component: () => import('@/views/invoice/Apply.vue'), meta: { title: '申请开票' } },
      { path: 'invoice/review', name: 'InvoiceReview', component: () => import('@/views/invoice/Review.vue'), meta: { title: '开票审核' } },
      { path: 'invoice/list', name: 'InvoiceList', component: () => import('@/views/invoice/List.vue'), meta: { title: '发票记录' } },
      { path: 'billing/products', name: 'BillingProducts', component: () => import('@/views/billing/products/Index.vue'), meta: { title: '产品管理' } },
      { path: 'billing/packages', name: 'BillingPackages', component: () => import('@/views/billing/packages/Index.vue'), meta: { title: '套餐管理' } },
      { path: 'billing/pricing', name: 'BillingPricing', component: () => import('@/views/billing/pricing/Index.vue'), meta: { title: '定价方案' } },
      { path: 'billing/discounts', name: 'BillingDiscounts', component: () => import('@/views/billing/rules/Discounts.vue'), meta: { title: '折扣规则' } },
      { path: 'billing/gifts', name: 'BillingGifts', component: () => import('@/views/billing/rules/Gifts.vue'), meta: { title: '赠送规则' } },
      { path: 'billing/orders', name: 'BillingOrders', component: () => import('@/views/billing/orders/Index.vue'), meta: { title: '订单管理' } },
      { path: 'billing/orders/proxy', name: 'BillingProxyOrder', component: () => import('@/views/billing/orders/Proxy.vue'), meta: { title: '代客下单' } },
      { path: 'billing/subscriptions', name: 'BillingSubscriptions', component: () => import('@/views/billing/subscriptions/Index.vue'), meta: { title: '订阅管理' } },
      { path: 'billing/trials', name: 'BillingTrials', component: () => import('@/views/billing/trials/Index.vue'), meta: { title: '试用管理' } },
      { path: 'billing/approvals', name: 'BillingApprovals', component: () => import('@/views/billing/trials/Approvals.vue'), meta: { title: '延长审批' } },
      { path: 'billing/reports', name: 'BillingReports', component: () => import('@/views/billing/reports/Index.vue'), meta: { title: '运营报表' } },
      { path: 'billing/reports/customer/:customerId', name: 'BillingCustomerAssets', component: () => import('@/views/billing/reports/CustomerAssets.vue'), meta: { title: '客户资产' } },
      { path: 'security/users', name: 'SecurityUsers', component: () => import('@/views/security/users/Index.vue'), meta: { title: '管理员' } },
      { path: 'security/roles', name: 'SecurityRoles', component: () => import('@/views/security/roles/Index.vue'), meta: { title: '角色权限' } },
      { path: 'security/login-logs', name: 'SecurityLoginLogs', component: () => import('@/views/security/logs/LoginLogs.vue'), meta: { title: '登录日志' } },
      { path: 'security/audit-logs', name: 'SecurityAuditLogs', component: () => import('@/views/security/audit/AuditLogs.vue'), meta: { title: '审计日志' } },
      { path: 'security/reconciliation', name: 'SecurityReconciliation', component: () => import('@/views/security/reconciliation/Index.vue'), meta: { title: '对账报告' } },
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.path !== '/login') {
    const store = useAppStore()
    if (!store.token) {
      next('/login')
      return
    }
  }
  next()
})

export default router
