import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores/app'

const request = axios.create({
  baseURL: '',
  timeout: 15000
})

request.interceptors.request.use(config => {
  const app = useAppStore()
  if (app.token) {
    config.headers['Authorization'] = `Bearer ${app.token}`
  }
  if (app.tenantId) {
    config.headers['X-Tenant-Id'] = app.tenantId
  }
  return config
})

request.interceptors.response.use(
  response => {
    const data = response.data
    if (data.code !== undefined && data.code !== 0) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  error => {
    if (error.response?.status === 401) {
      ElMessage.error('认证失败，请重新登录')
      const app = useAppStore()
      app.logout()
      window.location.href = '/login'
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
