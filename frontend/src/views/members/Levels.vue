<template>
  <CrudTable title="会员等级" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    @create="openDialog()" @edit="openDialog($event)" @delete="handleDelete($event)"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <el-table-column prop="id" label="ID" width="70" />
    <el-table-column prop="levelCode" label="等级编码" width="100" />
    <el-table-column prop="levelName" label="等级名称" width="120" />
    <el-table-column prop="levelRank" label="排序" width="70" />
    <el-table-column prop="minPoints" label="最低积分" width="100" />
    <el-table-column prop="minConsumption" label="最低消费(分)" width="120" />
    <el-table-column prop="pointMultiplier" label="积分倍率" width="90" />
    <el-table-column prop="discountRate" label="折扣率" width="80" />
    <el-table-column prop="status" label="状态" width="70">
      <template #default="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
      </template>
    </el-table-column>
  </CrudTable>

  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑等级' : '新增等级'" width="550px">
    <el-form :model="form" label-width="110px">
      <el-form-item label="等级编码" v-if="!isEdit"><el-input v-model="form.levelCode" placeholder="如: silver, gold" /></el-form-item>
      <el-form-item label="等级名称"><el-input v-model="form.levelName" /></el-form-item>
      <el-form-item label="排序值"><el-input-number v-model="form.levelRank" :min="1" /></el-form-item>
      <el-form-item label="最低累计积分"><el-input-number v-model="form.minPoints" :min="0" /></el-form-item>
      <el-form-item label="最低消费(分)"><el-input-number v-model="form.minConsumption" :min="0" /></el-form-item>
      <el-form-item label="积分倍率"><el-input-number v-model="form.pointMultiplier" :min="0.1" :precision="2" :step="0.1" /></el-form-item>
      <el-form-item label="消费折扣"><el-input-number v-model="form.discountRate" :min="0.1" :max="1" :precision="2" :step="0.05" /></el-form-item>
      <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
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
import { memberApi } from '@/api'
import { ElMessage } from 'element-plus'
import CrudTable from '@/components/CrudTable.vue'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const dialogVisible = ref(false), isEdit = ref(false), submitting = ref(false), editId = ref(null)
const form = reactive({ levelCode: '', levelName: '', levelRank: 1, minPoints: 0, minConsumption: 0,
  pointMultiplier: 1, discountRate: 1, description: '', status: 1 })

async function fetchData() {
  loading.value = true
  try {
    const res = await memberApi.listLevels({ page: page.value, pageSize: pageSize.value })
    list.value = res.data.items; total.value = res.data.total
  } finally { loading.value = false }
}

function openDialog(row) {
  isEdit.value = !!row; editId.value = row?.id || null
  Object.assign(form, row || { levelCode: '', levelName: '', levelRank: 1, minPoints: 0, minConsumption: 0,
    pointMultiplier: 1, discountRate: 1, description: '', status: 1 })
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (isEdit.value) await memberApi.updateLevel(editId.value, form)
    else await memberApi.createLevel(form)
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false; fetchData()
  } finally { submitting.value = false }
}

async function handleDelete(row) { await memberApi.deleteLevel(row.id); ElMessage.success('删除成功'); fetchData() }
onMounted(fetchData)
</script>
