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
