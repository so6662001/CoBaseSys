<template>
  <CrudTable title="消费动作" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    @create="openDialog()" @edit="openDialog($event)" @delete="handleDelete($event)"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <template #filter>
      <div class="filter-bar">
        <el-input v-model="filterSystemId" placeholder="系统ID筛选" style="width:160px" clearable @clear="fetchData" />
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
    </template>
    <el-table-column prop="id" label="ID" width="70" />
    <el-table-column prop="systemId" label="系统ID" width="80" />
    <el-table-column prop="actionCode" label="动作编码" width="140" />
    <el-table-column prop="actionName" label="动作名称" />
    <el-table-column prop="description" label="描述" show-overflow-tooltip />
    <el-table-column prop="status" label="状态" width="80">
      <template #default="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
      </template>
    </el-table-column>
  </CrudTable>

  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑动作' : '新增动作'" width="500px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="系统ID"><el-input-number v-model="form.systemId" :min="1" /></el-form-item>
      <el-form-item label="动作编码" v-if="!isEdit"><el-input v-model="form.actionCode" /></el-form-item>
      <el-form-item label="动作名称"><el-input v-model="form.actionName" /></el-form-item>
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
import { walletApi } from '@/api'
import { ElMessage } from 'element-plus'
import CrudTable from '@/components/CrudTable.vue'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const dialogVisible = ref(false), isEdit = ref(false), submitting = ref(false), editId = ref(null)
const filterSystemId = ref('')
const form = reactive({ systemId: 1, actionCode: '', actionName: '', description: '', status: 1 })

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (filterSystemId.value) params.systemId = filterSystemId.value
    const res = await walletApi.listActions(params)
    list.value = res.data.items; total.value = res.data.total
  } finally { loading.value = false }
}

function openDialog(row) {
  isEdit.value = !!row; editId.value = row?.id || null
  Object.assign(form, row || { systemId: 1, actionCode: '', actionName: '', description: '', status: 1 })
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (isEdit.value) await walletApi.updateAction(editId.value, form)
    else await walletApi.createAction(form)
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false; fetchData()
  } finally { submitting.value = false }
}

async function handleDelete(row) { await walletApi.deleteAction(row.id); ElMessage.success('删除成功'); fetchData() }
onMounted(fetchData)
</script>
