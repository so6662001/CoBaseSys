<template>
  <CrudTable title="租户管理" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    @create="openDialog()" @edit="openDialog($event)" @delete="handleDelete($event)"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <el-table-column prop="id" label="ID" width="80" />
    <el-table-column prop="tenantCode" label="租户编码" width="140" />
    <el-table-column prop="tenantName" label="租户名称" />
    <el-table-column prop="contactName" label="联系人" width="120" />
    <el-table-column prop="contactPhone" label="联系电话" width="140" />
    <el-table-column prop="contactEmail" label="邮箱" />
    <el-table-column prop="status" label="状态" width="80">
      <template #default="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
      </template>
    </el-table-column>
  </CrudTable>

  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑租户' : '新增租户'" width="500px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="租户编码" v-if="!isEdit"><el-input v-model="form.tenantCode" /></el-form-item>
      <el-form-item label="租户名称"><el-input v-model="form.tenantName" /></el-form-item>
      <el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item>
      <el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item>
      <el-form-item label="邮箱"><el-input v-model="form.contactEmail" /></el-form-item>
      <el-form-item label="状态" v-if="isEdit">
        <el-select v-model="form.status"><el-option :value="1" label="启用" /><el-option :value="0" label="禁用" /></el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible=false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { tenantApi } from '@/api'
import { ElMessage } from 'element-plus'
import CrudTable from '@/components/CrudTable.vue'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const dialogVisible = ref(false), isEdit = ref(false), submitting = ref(false), editId = ref(null)
const form = reactive({ tenantCode: '', tenantName: '', contactName: '', contactPhone: '', contactEmail: '', status: 1 })

async function fetchData() {
  loading.value = true
  try {
    const res = await tenantApi.list({ page: page.value, pageSize: pageSize.value })
    list.value = res.data.items; total.value = res.data.total
  } finally { loading.value = false }
}

function openDialog(row) {
  isEdit.value = !!row; editId.value = row?.id || null
  Object.assign(form, row || { tenantCode: '', tenantName: '', contactName: '', contactPhone: '', contactEmail: '', status: 1 })
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (isEdit.value) await tenantApi.update(editId.value, form)
    else await tenantApi.create(form)
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false; fetchData()
  } finally { submitting.value = false }
}

async function handleDelete(row) {
  await tenantApi.delete(row.id); ElMessage.success('删除成功'); fetchData()
}

onMounted(fetchData)
</script>
