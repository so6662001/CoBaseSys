import request from './request'

// Tenants
export const tenantApi = {
  list: (params) => request.get('/admin/tenants', { params }),
  create: (data) => request.post('/admin/tenants', data),
  update: (id, data) => request.put(`/admin/tenants/${id}`, data),
  delete: (id) => request.delete(`/admin/tenants/${id}`),
}

// External Systems
export const systemApi = {
  list: (params) => request.get('/admin/systems', { params }),
  create: (data) => request.post('/admin/systems', data),
  update: (id, data) => request.put(`/admin/systems/${id}`, data),
  delete: (id) => request.delete(`/admin/systems/${id}`),
  resetSecret: (id) => request.post(`/admin/systems/${id}/reset-secret`),
}

// Points
export const pointApi = {
  listActions: (params) => request.get('/admin/points/actions', { params }),
  createAction: (data) => request.post('/admin/points/actions', data),
  updateAction: (id, data) => request.put(`/admin/points/actions/${id}`, data),
  deleteAction: (id) => request.delete(`/admin/points/actions/${id}`),
  listRules: (params) => request.get('/admin/points/rules', { params }),
  createRule: (data) => request.post('/admin/points/rules', data),
  updateRule: (id, data) => request.put(`/admin/points/rules/${id}`, data),
  deleteRule: (id) => request.delete(`/admin/points/rules/${id}`),
  listAccounts: (params) => request.get('/admin/points/accounts', { params }),
  listTransactions: (params) => request.get('/admin/points/transactions', { params }),
  giftApply: (data) => request.post('/admin/points/gift/apply', data),
  giftPending: (params) => request.get('/admin/points/gift/pending', { params }),
  giftApprove: (id, params) => request.post(`/admin/points/gift/${id}/approve`, null, { params }),
  giftReject: (id, params) => request.post(`/admin/points/gift/${id}/reject`, null, { params }),
  giftList: (params) => request.get('/admin/points/gift/list', { params }),
  giftSummary: () => request.get('/admin/points/gift/summary'),
  giftTransactions: (params) => request.get('/admin/points/gift/transactions', { params }),
}

// Wallet
export const walletApi = {
  listActions: (params) => request.get('/admin/wallet/actions', { params }),
  createAction: (data) => request.post('/admin/wallet/actions', data),
  updateAction: (id, data) => request.put(`/admin/wallet/actions/${id}`, data),
  deleteAction: (id) => request.delete(`/admin/wallet/actions/${id}`),
  listRules: (params) => request.get('/admin/wallet/rules', { params }),
  createRule: (data) => request.post('/admin/wallet/rules', data),
  updateRule: (id, data) => request.put(`/admin/wallet/rules/${id}`, data),
  deleteRule: (id) => request.delete(`/admin/wallet/rules/${id}`),
  listPromotions: (params) => request.get('/admin/wallet/promotions', { params }),
  createPromotion: (data) => request.post('/admin/wallet/promotions', data),
  deletePromotion: (id) => request.delete(`/admin/wallet/promotions/${id}`),
  listAccounts: (params) => request.get('/admin/wallet/accounts', { params }),
  listTransactions: (params) => request.get('/admin/wallet/transactions', { params }),
  adjust: (params) => request.post('/admin/wallet/adjust', null, { params }),
}

// Members
export const memberApi = {
  listLevels: (params) => request.get('/admin/members/levels', { params }),
  createLevel: (data) => request.post('/admin/members/levels', data),
  updateLevel: (id, data) => request.put(`/admin/members/levels/${id}`, data),
  deleteLevel: (id) => request.delete(`/admin/members/levels/${id}`),
  listUsers: (params) => request.get('/admin/members/users', { params }),
  getUser: (userId) => request.get(`/admin/members/users/${userId}`),
}

// Notifications
export const notificationApi = {
  listTemplates: (params) => request.get('/admin/notifications/templates', { params }),
  createTemplate: (data) => request.post('/admin/notifications/templates', data),
  updateTemplate: (id, data) => request.put(`/admin/notifications/templates/${id}`, data),
  deleteTemplate: (id) => request.delete(`/admin/notifications/templates/${id}`),
  listRules: (params) => request.get('/admin/notifications/rules', { params }),
  createRule: (data) => request.post('/admin/notifications/rules', data),
  updateRule: (id, data) => request.put(`/admin/notifications/rules/${id}`, data),
  deleteRule: (id) => request.delete(`/admin/notifications/rules/${id}`),
  listRecords: (params) => request.get('/admin/notifications/records', { params }),
}

// Webhooks
export const webhookApi = {
  list: (params) => request.get('/admin/webhooks', { params }),
  create: (data) => request.post('/admin/webhooks', data),
  update: (id, data) => request.put(`/admin/webhooks/${id}`, data),
  delete: (id) => request.delete(`/admin/webhooks/${id}`),
  listLogs: (params) => request.get('/admin/webhooks/logs', { params }),
}

// Auth
export const authApi = {
  login: (data) => request.post('/admin/auth/login', data),
  refresh: (refreshToken) => request.post('/admin/auth/refresh', null, { params: { refreshToken } }),
  logout: (userId) => request.post('/admin/auth/logout', null, { params: { userId } }),
  sendMfaCode: (userId) => request.post('/admin/auth/mfa/send', null, { params: { userId } }),
  verifyMfaCode: (userId, code) => request.post('/admin/auth/mfa/verify', null, { params: { userId, code } }),
  changePassword: (userId, oldPassword, newPassword) => request.post('/admin/auth/change-password', null, { params: { userId, oldPassword, newPassword } }),
}

// Security - Admin Users
export const securityUserApi = {
  list: (params) => request.get('/admin/security/users', { params }),
  create: (data) => request.post('/admin/security/users', data),
  resetPassword: (id, newPassword) => request.post(`/admin/security/users/${id}/reset-password`, null, { params: { newPassword } }),
  delete: (id) => request.delete(`/admin/security/users/${id}`),
  listRoles: () => request.get('/admin/security/roles'),
  listPermissions: () => request.get('/admin/security/permissions'),
  loginLogs: (params) => request.get('/admin/security/login-logs', { params }),
}

// Security - Audit & Reconciliation
export const auditApi = {
  listAuditLogs: (params) => request.get('/admin/security/audit-logs', { params }),
  listReconciliationReports: (params) => request.get('/admin/security/reconciliation-reports', { params }),
}

// Invoice
export const invoiceApi = {
  list: (params) => request.get('/admin/invoice/applications', { params }),
  getById: (id) => request.get(`/admin/invoice/applications/${id}`),
  approve: (id, data) => request.post(`/admin/invoice/applications/${id}/approve`, data),
  reject: (id, data) => request.post(`/admin/invoice/applications/${id}/reject`, data),
  voidInvoice: (id, data) => request.post(`/admin/invoice/applications/${id}/void`, data),
  resendEmail: (id) => request.post(`/admin/invoice/applications/${id}/resend-email`),
  statistics: () => request.get('/admin/invoice/applications/statistics'),
  apply: (data) => request.post('/admin/invoice/customer/apply', data),
  availableOrders: (params) => request.get('/admin/invoice/customer/available-orders', { params }),
  myApplications: (params) => request.get('/admin/invoice/customer/my', { params }),
}

// Billing - Products
export const billingProductApi = {
  list: (params) => request.get('/admin/billing/products', { params }),
  getById: (id) => request.get(`/admin/billing/products/${id}`),
  create: (data) => request.post('/admin/billing/products', data),
  update: (id, data) => request.put(`/admin/billing/products/${id}`, data),
  delete: (id) => request.delete(`/admin/billing/products/${id}`),
}

// Billing - Packages
export const billingPackageApi = {
  list: (params) => request.get('/admin/billing/packages', { params }),
  getById: (id) => request.get(`/admin/billing/packages/${id}`),
  create: (data) => request.post('/admin/billing/packages', data),
  update: (id, data) => request.put(`/admin/billing/packages/${id}`, data),
  delete: (id) => request.delete(`/admin/billing/packages/${id}`),
  addItem: (id, data) => request.post(`/admin/billing/packages/${id}/items`, data),
  removeItem: (id, itemId) => request.delete(`/admin/billing/packages/${id}/items/${itemId}`),
}

// Billing - Pricing Plans
export const billingPricingApi = {
  list: (params) => request.get('/admin/billing/pricing-plans', { params }),
  create: (data) => request.post('/admin/billing/pricing-plans', data),
  update: (id, data) => request.put(`/admin/billing/pricing-plans/${id}`, data),
  delete: (id) => request.delete(`/admin/billing/pricing-plans/${id}`),
}

// Billing - Discount & Gift Rules
export const billingRuleApi = {
  listDiscounts: (params) => request.get('/admin/billing/discount-rules', { params }),
  createDiscount: (data) => request.post('/admin/billing/discount-rules', data),
  updateDiscount: (id, data) => request.put(`/admin/billing/discount-rules/${id}`, data),
  deleteDiscount: (id) => request.delete(`/admin/billing/discount-rules/${id}`),
  listGifts: (params) => request.get('/admin/billing/gift-rules', { params }),
  createGift: (data) => request.post('/admin/billing/gift-rules', data),
  updateGift: (id, data) => request.put(`/admin/billing/gift-rules/${id}`, data),
  deleteGift: (id) => request.delete(`/admin/billing/gift-rules/${id}`),
}

// Billing - Orders
export const billingOrderApi = {
  list: (params) => request.get('/admin/billing/orders', { params }),
  getById: (idOrNo) => request.get(`/admin/billing/orders/${idOrNo}`),
  proxyOrder: (data, params) => request.post('/admin/billing/orders/proxy', data, { params }),
  confirmPayment: (id, params) => request.post(`/admin/billing/orders/${id}/confirm-payment`, null, { params }),
  cancel: (id) => request.post(`/admin/billing/orders/${id}/cancel`),
}

// Billing - Subscriptions
export const billingSubApi = {
  list: (params) => request.get('/admin/billing/subscriptions', { params }),
  getById: (id) => request.get(`/admin/billing/subscriptions/${id}`),
  extend: (id, days) => request.post(`/admin/billing/subscriptions/${id}/extend`, null, { params: { days } }),
  suspend: (id) => request.post(`/admin/billing/subscriptions/${id}/suspend`),
  resume: (id) => request.post(`/admin/billing/subscriptions/${id}/resume`),
  usageLedger: (id, params) => request.get(`/admin/billing/subscriptions/${id}/usage-ledger`, { params }),
}

// Billing - Trials & Approvals
export const billingTrialApi = {
  listTrials: (params) => request.get('/admin/billing/trials', { params }),
  submitExtend: (trialId, data) => request.post('/admin/billing/trial-extend/apply', data, { params: { trialId } }),
  listPending: (params) => request.get('/admin/billing/trial-extend/pending', { params }),
  approve: (id, params) => request.post(`/admin/billing/trial-extend/${id}/approve`, null, { params }),
  reject: (id, params) => request.post(`/admin/billing/trial-extend/${id}/reject`, null, { params }),
  listApprovals: (params) => request.get('/admin/billing/trial-extend', { params }),
}

// Billing - Reports
export const billingReportApi = {
  expiringSubscriptions: () => request.get('/admin/billing/reports/expiring-subscriptions'),
  expiredSubscriptions: () => request.get('/admin/billing/reports/expired-subscriptions'),
  expiringTrials: (params) => request.get('/admin/billing/reports/expiring-trials', { params }),
  expiredTrials: (params) => request.get('/admin/billing/reports/expired-trials', { params }),
  subscriptionDetail: (id) => request.get(`/admin/billing/reports/subscription-detail/${id}`),
  usageLedger: (params) => request.get('/admin/billing/reports/usage-ledger', { params }),
  customerAssets: (customerId, params) => request.get(`/admin/billing/reports/customer-assets/${customerId}`, { params }),
}
