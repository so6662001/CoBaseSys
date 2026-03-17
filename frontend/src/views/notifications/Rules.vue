<template>
  <CrudTable title="通知规则" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    @create="openDialog()" @edit="openDialog($event)" @delete="handleDelete($event)"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <el-table-column prop="id" label="ID" width="70" />
    <el-table-column prop="ruleName" label="规则名称" width="150" />
    <el-table-column prop="triggerType" label="触发类型" width="130">
      <template #default="{ row }">
        <el-tag size="small">{{ { low_balance:'余额不足', recharge_success:'充值成功', level_upgrade:'会员升级', point_expiry:'积分到期', custom:'自定义' }[row.triggerType] || row.triggerType }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="thresholdValue" label="阈值" width="100" />
    <el-table-column prop="channel" label="渠道" width="90">
      <template #default="{ row }">
        <el-tag size="small" type="info">{{ { email:'邮件', sms:'短信', in_app:'应用内', webhook:'Webhook' }[row.channel] || row.channel }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="templateId" label="模板ID" width="80" />
    <el-table-column prop="status" label="状态" width="70">
      <template #default="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
      </template>
    </el-table-column>
  </CrudTable>

  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑规则' : '新增规则'" width="500px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="规则名称"><el-input v-model="form.ruleName" /></el-form-item>
      <el-form-item label="触发类型">
        <el-select v-model="form.triggerType">
          <el-option value="low_balance" label="余额不足" /><el-option value="recharge_success" label="充值成功" />
          <el-option value="level_upgrade" label="会员升级" /><el-option value="point_expiry" label="积分到期" />
          <el-option value="custom" label="自定义" />
        </el-select>
      </el-form-item>
      <el-form-item label="阈值"><el-input-number v-model="form.thresholdValue" /></el-form-item>
      <el-form-item label="通知渠道">
        <el-select v-model="form.channel">
          <el-option value="email" label="邮件" /><el-option value="sms" label="短信" />
          <el-option value="in_app" label="应用内" /><el-option value="webhook" label="Webhook" />
        </el-select>
      </el-form-item>
      <el-form-item label="模板ID"><el-input-number v-model="form.templateId" /></el-form-item>
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
const form = reactive({ ruleName: '', triggerType: 'low_balance', thresholdValue: null, channel: 'email', templateId: null, status: 1 })

async function fetchData() {
  loading.value = true
  try {
    const res = await notificationApi.listRules({ page: page.value, pageSize: pageSize.value })
    list.value = res.data.items; total.value = res.data.total
  } finally { loading.value = false }
}

function openDialog(row) {
  isEdit.value = !!row; editId.value = row?.id || null
  Object.assign(form, row || { ruleName: '', triggerType: 'low_balance', thresholdValue: null, channel: 'email', templateId: null, status: 1 })
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (isEdit.value) await notificationApi.updateRule(editId.value, form)
    else await notificationApi.createRule(form)
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false; fetchData()
  } finally { submitting.value = false }
}

async function handleDelete(row) { await notificationApi.deleteRule(row.id); ElMessage.success('删除成功'); fetchData() }
onMounted(fetchData)
</script>
