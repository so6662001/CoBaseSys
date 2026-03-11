<template>
  <CrudTable title="消费规则" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    @create="openDialog()" @edit="openDialog($event)" @delete="handleDelete($event)"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <el-table-column prop="id" label="ID" width="70" />
    <el-table-column prop="actionId" label="动作ID" width="80" />
    <el-table-column prop="ruleName" label="规则名称" width="150" />
    <el-table-column prop="calcType" label="计算类型" width="100">
      <template #default="{ row }">
        <el-tag size="small">{{ { fixed:'固定', unit_price:'按量', tiered:'阶梯', custom:'自定义' }[row.calcType] || row.calcType }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="calcValue" label="单价/金额" width="100" />
    <el-table-column prop="unitName" label="计量单位" width="90" />
    <el-table-column prop="freeQuota" label="免费额度" width="90" />
    <el-table-column prop="priority" label="优先级" width="70" />
    <el-table-column prop="status" label="状态" width="70">
      <template #default="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
      </template>
    </el-table-column>
  </CrudTable>

  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑规则' : '新增规则'" width="600px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="动作ID"><el-input-number v-model="form.actionId" :min="1" /></el-form-item>
      <el-form-item label="规则名称"><el-input v-model="form.ruleName" /></el-form-item>
      <el-form-item label="计算类型">
        <el-select v-model="form.calcType">
          <el-option value="fixed" label="固定费用" /><el-option value="unit_price" label="按量计费" />
          <el-option value="tiered" label="阶梯定价" /><el-option value="custom" label="自定义" />
        </el-select>
      </el-form-item>
      <el-form-item label="单价/金额(元)"><el-input-number v-model="form.calcValue" :precision="4" /></el-form-item>
      <el-form-item label="计量单位"><el-input v-model="form.unitName" placeholder="如: 次、GB、千Token" /></el-form-item>
      <el-form-item label="计算表达式" v-if="form.calcType==='tiered'||form.calcType==='custom'">
        <el-input v-model="form.calcExpression" type="textarea" :rows="3" />
      </el-form-item>
      <el-row :gutter="12">
        <el-col :span="8"><el-form-item label="免费额度"><el-input-number v-model="form.freeQuota" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="最低收费(分)"><el-input-number v-model="form.minCharge" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="最高收费(分)"><el-input-number v-model="form.maxCharge" /></el-form-item></el-col>
      </el-row>
      <el-form-item label="优先级"><el-input-number v-model="form.priority" /></el-form-item>
      <el-form-item label="生效时间"><el-date-picker v-model="form.effectiveFrom" type="datetime" style="width:100%" /></el-form-item>
      <el-form-item label="失效时间"><el-date-picker v-model="form.effectiveTo" type="datetime" style="width:100%" /></el-form-item>
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
import { walletApi } from '@/api'
import { ElMessage } from 'element-plus'
import CrudTable from '@/components/CrudTable.vue'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const dialogVisible = ref(false), isEdit = ref(false), submitting = ref(false), editId = ref(null)
const form = reactive({ actionId: null, ruleName: '', calcType: 'fixed', calcValue: 0, calcExpression: '',
  unitName: '', freeQuota: 0, minCharge: null, maxCharge: null, priority: 0,
  effectiveFrom: null, effectiveTo: null, status: 1 })

async function fetchData() {
  loading.value = true
  try {
    const res = await walletApi.listRules({ page: page.value, pageSize: pageSize.value })
    list.value = res.data.items; total.value = res.data.total
  } finally { loading.value = false }
}

function openDialog(row) {
  isEdit.value = !!row; editId.value = row?.id || null
  if (row) Object.assign(form, row)
  else Object.assign(form, { actionId: null, ruleName: '', calcType: 'fixed', calcValue: 0, calcExpression: '',
    unitName: '', freeQuota: 0, minCharge: null, maxCharge: null, priority: 0,
    effectiveFrom: null, effectiveTo: null, status: 1 })
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (isEdit.value) await walletApi.updateRule(editId.value, form)
    else await walletApi.createRule(form)
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false; fetchData()
  } finally { submitting.value = false }
}

async function handleDelete(row) { await walletApi.deleteRule(row.id); ElMessage.success('删除成功'); fetchData() }
onMounted(fetchData)
</script>
