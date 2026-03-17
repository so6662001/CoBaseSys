<template>
  <CrudTable title="通知模板" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    @create="openDialog()" @edit="openDialog($event)" @delete="handleDelete($event)"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <el-table-column prop="id" label="ID" width="70" />
    <el-table-column prop="templateCode" label="模板编码" width="140" />
    <el-table-column prop="templateName" label="模板名称" width="150" />
    <el-table-column prop="channel" label="渠道" width="100">
      <template #default="{ row }">
        <el-tag size="small">{{ { email:'邮件', sms:'短信', in_app:'应用内', webhook:'Webhook' }[row.channel] || row.channel }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="subject" label="标题" show-overflow-tooltip />
    <el-table-column prop="status" label="状态" width="70">
      <template #default="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
      </template>
    </el-table-column>
  </CrudTable>

  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑模板' : '新增模板'" width="600px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="模板编码" v-if="!isEdit"><el-input v-model="form.templateCode" /></el-form-item>
      <el-form-item label="模板名称"><el-input v-model="form.templateName" /></el-form-item>
      <el-form-item label="渠道" v-if="!isEdit">
        <el-select v-model="form.channel">
          <el-option value="email" label="邮件" /><el-option value="sms" label="短信" />
          <el-option value="in_app" label="应用内" /><el-option value="webhook" label="Webhook" />
        </el-select>
      </el-form-item>
      <el-form-item label="标题"><el-input v-model="form.subject" /></el-form-item>
      <el-form-item label="内容">
        <el-input v-model="form.content" type="textarea" :rows="5" placeholder="支持 ${variable} 占位符" />
      </el-form-item>
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
import { notificationApi } from '@/api'
import { ElMessage } from 'element-plus'
import CrudTable from '@/components/CrudTable.vue'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const dialogVisible = ref(false), isEdit = ref(false), submitting = ref(false), editId = ref(null)
const form = reactive({ templateCode: '', templateName: '', channel: 'email', subject: '', content: '', status: 1 })

async function fetchData() {
  loading.value = true
  try {
    const res = await notificationApi.listTemplates({ page: page.value, pageSize: pageSize.value })
    list.value = res.data.items; total.value = res.data.total
  } finally { loading.value = false }
}

function openDialog(row) {
  isEdit.value = !!row; editId.value = row?.id || null
  Object.assign(form, row || { templateCode: '', templateName: '', channel: 'email', subject: '', content: '', status: 1 })
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (isEdit.value) await notificationApi.updateTemplate(editId.value, form)
    else await notificationApi.createTemplate(form)
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false; fetchData()
  } finally { submitting.value = false }
}

async function handleDelete(row) { await notificationApi.deleteTemplate(row.id); ElMessage.success('删除成功'); fetchData() }
onMounted(fetchData)
</script>
