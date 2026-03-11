import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const token = ref(localStorage.getItem('access_token') || '')
  const refreshToken = ref(localStorage.getItem('refresh_token') || '')
  const userId = ref(localStorage.getItem('user_id') || '')
  const username = ref(localStorage.getItem('username') || '')
  const realName = ref(localStorage.getItem('real_name') || '')
  const permissions = ref(JSON.parse(localStorage.getItem('permissions') || '[]'))
  const tenantId = ref(localStorage.getItem('tenant_id') || '1')
  const tenantName = ref(localStorage.getItem('tenant_name') || '默认租户')
  const sidebarCollapsed = ref(false)

  function setAuth(accessToken, refToken, uid, uname, rname, perms) {
    token.value = accessToken
    refreshToken.value = refToken || ''
    userId.value = String(uid)
    username.value = uname
    realName.value = rname || uname
    permissions.value = perms || []
    localStorage.setItem('access_token', accessToken)
    localStorage.setItem('refresh_token', refToken || '')
    localStorage.setItem('user_id', String(uid))
    localStorage.setItem('username', uname)
    localStorage.setItem('real_name', rname || uname)
    localStorage.setItem('permissions', JSON.stringify(perms || []))
  }

  // backward compat
  function setToken(t) {
    token.value = t
    localStorage.setItem('access_token', t)
  }

  function setTenant(id, name) {
    tenantId.value = id
    tenantName.value = name
    localStorage.setItem('tenant_id', id)
    localStorage.setItem('tenant_name', name)
  }

  function logout() {
    token.value = ''
    refreshToken.value = ''
    userId.value = ''
    username.value = ''
    realName.value = ''
    permissions.value = []
    const keys = ['access_token', 'refresh_token', 'user_id', 'username', 'real_name',
                   'permissions', 'tenant_id', 'tenant_name', 'admin_token']
    keys.forEach(k => localStorage.removeItem(k))
  }

  function hasPermission(perm) {
    return permissions.value.includes(perm)
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return {
    token, refreshToken, userId, username, realName, permissions,
    tenantId, tenantName, sidebarCollapsed,
    setAuth, setToken, setTenant, logout, hasPermission, toggleSidebar
  }
})
