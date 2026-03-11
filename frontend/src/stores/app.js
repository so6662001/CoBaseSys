import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const tenantId = ref(localStorage.getItem('tenant_id') || '1')
  const tenantName = ref(localStorage.getItem('tenant_name') || '默认租户')
  const sidebarCollapsed = ref(false)

  function setToken(t) {
    token.value = t
    localStorage.setItem('admin_token', t)
  }

  function setTenant(id, name) {
    tenantId.value = id
    tenantName.value = name
    localStorage.setItem('tenant_id', id)
    localStorage.setItem('tenant_name', name)
  }

  function logout() {
    token.value = ''
    tenantId.value = ''
    tenantName.value = ''
    localStorage.removeItem('admin_token')
    localStorage.removeItem('tenant_id')
    localStorage.removeItem('tenant_name')
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return { token, tenantId, tenantName, sidebarCollapsed, setToken, setTenant, logout, toggleSidebar }
})
