<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1>CoBaseSys</h1>
        <p>公司运营底座系统</p>
      </div>
      <el-form :model="form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="form.token" placeholder="管理员 Token" size="large" prefix-icon="Lock" />
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
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const app = useAppStore()
const loading = ref(false)
const form = reactive({ token: '', tenantId: '1' })

function handleLogin() {
  if (!form.token) return
  loading.value = true
  app.setToken(form.token)
  app.setTenant(form.tenantId || '1', '租户' + (form.tenantId || '1'))
  setTimeout(() => {
    loading.value = false
    router.push('/dashboard')
  }, 300)
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
