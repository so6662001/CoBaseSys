<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1>CoBaseSys</h1>
        <p>公司运营底座系统</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" prefix-icon="User" autocomplete="username" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" placeholder="密码" size="large" prefix-icon="Lock" type="password" show-password autocomplete="current-password" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.tenantId" placeholder="租户ID (默认: 1)" size="large" prefix-icon="OfficeBuilding" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width:100%" @click="handleLogin" :loading="loading">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div v-if="errorMsg" style="color:#f56c6c;text-align:center;margin-top:-8px;font-size:13px">{{ errorMsg }}</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { authApi } from '@/api'

const router = useRouter()
const app = useAppStore()
const loading = ref(false)
const errorMsg = ref('')
const formRef = ref(null)
const form = reactive({ username: '', password: '', tenantId: '1' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  try { await formRef.value.validate() } catch { return }
  errorMsg.value = ''
  loading.value = true
  try {
    const res = await authApi.login({ username: form.username, password: form.password })
    const data = res.data
    app.setAuth(data.accessToken, data.refreshToken, data.userId, data.username, data.realName, data.permissions)
    app.setTenant(form.tenantId || '1', '租户' + (form.tenantId || '1'))
    router.push('/dashboard')
  } catch (e) {
    errorMsg.value = e.response?.data?.message || e.message || '登录失败'
    form.password = ''
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.login-header {
  text-align: center;
  margin-bottom: 32px;
}
.login-header h1 {
  margin: 0;
  font-size: 28px;
  color: #303133;
}
.login-header p {
  margin: 8px 0 0;
  color: #909399;
  font-size: 14px;
}
</style>
