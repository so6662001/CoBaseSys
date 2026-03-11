<template>
  <CrudTable title="充值促销" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    :showEdit="false" @create="openDialog()" @delete="handleDelete($event)"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <el-table-column prop="id" label="ID" width="70" />
    <el-table-column prop="name" label="活动名称" />
    <el-table-column prop="minAmount" label="最低充值(分)" width="120" />
    <el-table-column prop="giftType" label="赠送类型" width="100">
      <template #default="{ row }">
        <el-tag size="small">{{ row.giftType === 'fixed' ? '固定金额' : '按比例' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="giftValue" label="赠送值" width="100" />
    <el-table-column prop="effectiveFrom" label="开始时间" width="170" />
    <el-table-column prop="effectiveTo" label="结束时间" width="170" />
    <el-table-column prop="status" label="状态" width="70">
      <template #default="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
      </template>
    </el-table-column>
  </CrudTable>

  <el-dialog v-model="dialogVisible" title="新增充值促销" width="500px">
    <el-form :model="form" label-width="110px">
      <el-form-item label="活动名称"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="最低充值(分)"><el-input-number v-model="form.minAmount" :min="1" /></el-form-item>
      <el-form-item label="赠送类型">
        <el-select v-model="form.giftType">
          <el-option value="fixed" label="固定金额(分)" /><el-option value="rate" label="按比例" />
        </el-select>
      </el-form-item>
      <el-form-item label="赠送值"><el-input-number v-model="form.giftValue" :precision="2" /></el-form-item>
      <el-form-item label="开始时间"><el-date-picker v-model="form.effectiveFrom" type="datetime" style="width:100%" /></el-form-item>
      <el-form-item label="结束时间"><el-date-picker v-model="form.effectiveTo" type="datetime" style="width:100%" /></el-form-item>
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
const dialogVisible = ref(false), submitting = ref(false)
const form = reactive({ name: '', minAmount: 10000, giftType: 'fixed', giftValue: 0, effectiveFrom: null, effectiveTo: null })

async function fetchData() {
  loading.value = true
  try {
    const res = await walletApi.listPromotions({ page: page.value, pageSize: pageSize.value })
    list.value = res.data.items; total.value = res.data.total
  } finally { loading.value = false }
}

function openDialog() {
  Object.assign(form, { name: '', minAmount: 10000, giftType: 'fixed', giftValue: 0, effectiveFrom: null, effectiveTo: null })
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    await walletApi.createPromotion(form)
    ElMessage.success('创建成功'); dialogVisible.value = false; fetchData()
  } finally { submitting.value = false }
}

async function handleDelete(row) { await walletApi.deletePromotion(row.id); ElMessage.success('删除成功'); fetchData() }
onMounted(fetchData)
</script>
