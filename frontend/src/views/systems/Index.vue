<template>
  <CrudTable title="外部系统管理" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    @create="openDialog()" @edit="openDialog($event)" @delete="handleDelete($event)"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()" :actionWidth="220">
    <el-table-column prop="id" label="ID" width="70" />
    <el-table-column prop="systemCode" label="系统编码" width="120" />
    <el-table-column prop="systemName" label="系统名称" width="140" />
    <el-table-column prop="appKey" label="AppKey" width="200" />
    <el-table-column prop="rateLimit" label="限流/分" width="90" />
    <el-table-column prop="status" label="状态" width="80">
      <template #default="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
      </template>
    </el-table-column>
    <template #extra-actions="{ row }">
      <el-popconfirm title="确定重置密钥？" @confirm="handleResetSecret(row)">
        <template #reference><el-button link type="warning">重置密钥</el-button></template>
      </el-popconfirm>
    </template>
  </CrudTable>

  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑系统' : '注册系统'" width="550px">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
      <el-form-item label="系统编码" prop="systemCode" v-if="!isEdit"><el-input v-model="form.systemCode" /></el-form-item>
      <el-form-item label="系统名称" prop="systemName"><el-input v-model="form.systemName" /></el-form-item>
      <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
      <el-form-item label="回调地址"><el-input v-model="form.callbackUrl" /></el-form-item>
      <el-form-item label="IP白名单"><el-input v-model="form.ipWhitelist" placeholder="多个IP用逗号分隔" /></el-form-item>
      <el-form-item label="限流(次/分)"><el-input-number v-model="form.rateLimit" :min="1" /></el-form-item>
      <el-form-item label="状态" v-if="isEdit">
        <el-select v-model="form.status"><el-option :value="1" label="启用" /><el-option :value="0" label="禁用" /></el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible=false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="secretVisible" title="系统密钥" width="500px">
    <el-alert type="warning" :closable="false" style="margin-bottom:16px">请妥善保存密钥信息，密钥仅在此处显示一次</el-alert>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="AppKey">{{ secretInfo.appKey }}</el-descriptions-item>
      <el-descriptions-item label="AppSecret">{{ secretInfo.appSecret }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { systemApi } from '@/api'
import { ElMessage } from 'element-plus'
import CrudTable from '@/components/CrudTable.vue'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const dialogVisible = ref(false), isEdit = ref(false), submitting = ref(false), editId = ref(null)
const secretVisible = ref(false), secretInfo = reactive({ appKey: '', appSecret: '' })
const formRef = ref(null)
const form = reactive({ systemCode: '', systemName: '', description: '', callbackUrl: '', ipWhitelist: '', rateLimit: 1000, status: 1 })
const rules = {
  systemCode: [{ required: true, message: '请输入系统编码', trigger: 'blur' }],
  systemName: [{ required: true, message: '请输入系统名称', trigger: 'blur' }],
}

async function fetchData() {
  loading.value = true
  try {
    const res = await systemApi.list({ page: page.value, pageSize: pageSize.value })
    list.value = res.data.items; total.value = res.data.total
  } finally { loading.value = false }
}

function openDialog(row) {
  isEdit.value = !!row; editId.value = row?.id || null
  Object.assign(form, row || { systemCode: '', systemName: '', description: '', callbackUrl: '', ipWhitelist: '', rateLimit: 1000, status: 1 })
  dialogVisible.value = true
}

async function handleSubmit() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    if (isEdit.value) {
      await systemApi.update(editId.value, form)
      ElMessage.success('更新成功')
    } else {
      const res = await systemApi.create(form)
      secretInfo.appKey = res.data.appKey; secretInfo.appSecret = res.data.appSecret
      secretVisible.value = true
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false; fetchData()
  } finally { submitting.value = false }
}

async function handleDelete(row) { await systemApi.delete(row.id); ElMessage.success('删除成功'); fetchData() }

async function handleResetSecret(row) {
  const res = await systemApi.resetSecret(row.id)
  secretInfo.appKey = res.data.appKey; secretInfo.appSecret = res.data.appSecret
  secretVisible.value = true; ElMessage.success('密钥已重置')
}

onMounted(fetchData)
</script>
