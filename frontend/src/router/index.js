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
