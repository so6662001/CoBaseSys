<template>
  <CrudTable title="Webhook 配置" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    @create="openDialog()" @edit="openDialog($event)" @delete="handleDelete($event)"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <el-table-column prop="id" label="ID" width="70" />
    <el-table-column prop="webhookName" label="名称" width="150" />
    <el-table-column prop="url" label="推送地址" show-overflow-tooltip />
    <el-table-column prop="events" label="订阅事件" show-overflow-tooltip />
    <el-table-column prop="status" label="状态" width="80">
      <template #default="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
      </template>
    </el-table-column>
  </CrudTable>

  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑Webhook' : '新增Webhook'" width="600px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="名称"><el-input v-model="form.webhookName" /></el-form-item>
      <el-form-item label="推送地址"><el-input v-model="form.url" placeholder="https://..." /></el-form-item>
      <el-form-item label="密钥"><el-input v-model="form.secret" placeholder="用于签名验证（留空自动生成）" /></el-form-item>
      <el-form-item label="关联系统ID"><el-input-number v-model="form.systemId" :min="0" /></el-form-item>
      <el-form-item label="订阅事件">
        <el-checkbox-group v-model="selectedEvents">
          <el-checkbox value="point.earned">积分增加</el-checkbox>
          <el-checkbox value="point.deducted">积分扣减</el-checkbox>
          <el-checkbox value="wallet.recharged">充值成功</el-checkbox>
          <el-checkbox value="wallet.consumed">消费扣费</el-checkbox>
          <el-checkbox value="member.upgraded">会员升级</el-checkbox>
        </el-checkbox-group>
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
import { webhookApi } from '@/api'
import { ElMessage } from 'element-plus'
import CrudTable from '@/components/CrudTable.vue'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const dialogVisible = ref(false), isEdit = ref(false), submitting = ref(false), editId = ref(null)
const selectedEvents = ref([])
const form = reactive({ webhookName: '', url: '', secret: '', systemId: null, events: '', status: 1 })

async function fetchData() {
  loading.value = true
  try {
    const res = await webhookApi.list({ page: page.value, pageSize: pageSize.value })
    list.value = res.data.items; total.value = res.data.total
  } finally { loading.value = false }
}

function openDialog(row) {
  isEdit.value = !!row; editId.value = row?.id || null
  if (row) {
    Object.assign(form, row)
    selectedEvents.value = row.events ? row.events.split(',') : []
  } else {
    Object.assign(form, { webhookName: '', url: '', secret: '', systemId: null, events: '', status: 1 })
    selectedEvents.value = []
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  form.events = selectedEvents.value.join(',')
  try {
    if (isEdit.value) await webhookApi.update(editId.value, form)
    else await webhookApi.create(form)
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false; fetchData()
  } finally { submitting.value = false }
}

async function handleDelete(row) { await webhookApi.delete(row.id); ElMessage.success('删除成功'); fetchData() }
onMounted(fetchData)
</script>
