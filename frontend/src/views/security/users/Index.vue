<template>
  <el-card shadow="never">
    <div class="page-header">
      <h2>管理员管理</h2>
      <el-button type="primary" @click="openDialog()"><el-icon><Plus /></el-icon> 新增管理员</el-button>
    </div>
    <el-table :data="list" v-loading="loading" stripe border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="email" label="邮箱" width="180" />
      <el-table-column prop="mfaEnabled" label="MFA" width="60">
        <template #default="{row}"><el-tag :type="row.mfaEnabled?'success':'info'" size="small">{{ row.mfaEnabled?'开':'关' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="70">
        <template #default="{row}"><el-tag :type="row.status===1?'success':'danger'" size="small">{{ row.status===1?'正常':'禁用' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="lastLoginAt" label="最后登录" width="160" />
      <el-table-column prop="lastLoginIp" label="登录IP" width="130" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{row}">
          <el-button link type="warning" @click="openResetPwd(row)">重置密码</el-button>
          <el-popconfirm v-if="row.id!==1" title="确定删除？" @confirm="handleDelete(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <div style="display:flex;justify-content:flex-end;margin-top:16px" v-if="total>0">
      <el-pagination v-model:current-page="page" :total="total" :page-size="pageSize" layout="total, prev, pager, next" @current-change="fetchData" />
    </div>
  </el-card>

  <el-dialog v-model="dialogVisible" title="新增管理员" width="500px">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
      <el-form-item label="用户名" prop="username"><el-input v-model="form.username" /></el-form-item>
      <el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password /></el-form-item>
      <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
      <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
      <el-form-item label="角色">
        <el-checkbox-group v-model="form.roleIds">
          <el-checkbox v-for="r in roles" :key="r.id" :value="r.id" :label="r.roleName" />
        </el-checkbox-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible=false">取消</el-button>
      <el-button type="primary" @click="handleCreate" :loading="submitting">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="pwdDialogVisible" title="重置密码" width="400px">
    <el-form label-width="80px">
      <el-form-item label="用户名"><el-input :model-value="resetUser?.username" disabled /></el-form-item>
      <el-form-item label="新密码"><el-input v-model="newPassword" type="password" show-password /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdDialogVisible=false">取消</el-button>
      <el-button type="primary" @click="handleResetPwd">确认重置</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { securityUserApi } from '@/api'
import { ElMessage } from 'element-plus'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const dialogVisible = ref(false), submitting = ref(false), formRef = ref(null)
const roles = ref([])
const form = reactive({ username:'', password:'', realName:'', phone:'', email:'', roleIds:[] })
const rules = { username:[{required:true,message:'请输入用户名'}], password:[{required:true,message:'请输入密码',min:6}] }

const pwdDialogVisible = ref(false), resetUser = ref(null), newPassword = ref('')

async function fetchData() {
  loading.value = true
  try {
    const [r1, r2] = await Promise.all([
      securityUserApi.list({ page:page.value, pageSize:pageSize.value }),
      securityUserApi.listRoles()
    ])
    list.value = r1.data.items; total.value = r1.data.total
    roles.value = r2.data || []
  } finally { loading.value = false }
}

function openDialog() {
  Object.assign(form, { username:'', password:'', realName:'', phone:'', email:'', roleIds:[] })
  dialogVisible.value = true
}

async function handleCreate() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try { await securityUserApi.create(form); ElMessage.success('创建成功'); dialogVisible.value=false; fetchData() } finally { submitting.value=false }
}

function openResetPwd(row) { resetUser.value = row; newPassword.value = ''; pwdDialogVisible.value = true }
async function handleResetPwd() {
  if (!newPassword.value || newPassword.value.length < 6) { ElMessage.warning('密码至少6位'); return }
  await securityUserApi.resetPassword(resetUser.value.id, newPassword.value)
  ElMessage.success('密码已重置'); pwdDialogVisible.value = false
}

async function handleDelete(row) { await securityUserApi.delete(row.id); ElMessage.success('已删除'); fetchData() }

onMounted(fetchData)
</script>
