<template>
  <CrudTable title="套餐管理" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    @create="openDialog()" @edit="openDialog($event)" @delete="handleDelete($event)"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()" :actionWidth="220">
    <el-table-column prop="id" label="ID" width="60" />
    <el-table-column prop="packageCode" label="套餐编码" width="130" />
    <el-table-column prop="packageName" label="套餐名称" width="160" />
    <el-table-column prop="pricingModel" label="收费模式" width="120">
      <template #default="{row}"><el-tag size="small">{{ row.pricingModel }}</el-tag></template>
    </el-table-column>
    <el-table-column label="包含产品" min-width="200">
      <template #default="{row}">
        <el-tag v-for="item in (row.items||[])" :key="item.id" size="small" style="margin:2px">{{ item.productName }} ×{{ item.quantity }}</el-tag>
        <span v-if="!row.items?.length" style="color:#909399">暂无</span>
      </template>
    </el-table-column>
    <el-table-column prop="status" label="状态" width="70">
      <template #default="{row}"><el-tag :type="row.status===1?'success':'info'" size="small">{{ row.status===1?'上架':'下架' }}</el-tag></template>
    </el-table-column>
    <template #extra-actions="{row}">
      <el-button link type="primary" @click="openItemDialog(row)">管理产品</el-button>
    </template>
  </CrudTable>

  <el-dialog v-model="dialogVisible" :title="isEdit?'编辑套餐':'新增套餐'" width="550px">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
      <el-form-item label="套餐编码" prop="packageCode"><el-input v-model="form.packageCode" :disabled="isEdit" /></el-form-item>
      <el-form-item label="套餐名称" prop="packageName"><el-input v-model="form.packageName" /></el-form-item>
      <el-form-item label="收费模式" prop="pricingModel"><el-select v-model="form.pricingModel" style="width:100%">
        <el-option value="ONE_TIME_ANNUAL" label="一次性+年服务费" /><el-option value="SUBSCRIPTION" label="订阅+增量" />
        <el-option value="USAGE_BASED" label="按量计费" /><el-option value="CLOUD_RENTAL" label="云端租用" />
        <el-option value="ONE_TIME" label="一次性买断" />
      </el-select></el-form-item>
      <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible=false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="itemDialogVisible" title="管理套餐产品" width="550px">
    <div style="margin-bottom:16px">
      <el-tag v-for="item in currentItems" :key="item.id" closable @close="removeItem(item)" style="margin:4px" size="large">
        {{ item.productName }} ×{{ item.quantity }}
      </el-tag>
      <div v-if="!currentItems.length" style="color:#909399;padding:20px;text-align:center">暂无产品，请添加</div>
    </div>
    <el-divider />
    <el-form :model="addForm" inline>
      <el-form-item label="产品ID"><el-input-number v-model="addForm.productId" :min="1" /></el-form-item>
      <el-form-item label="数量"><el-input-number v-model="addForm.quantity" :min="1" /></el-form-item>
      <el-form-item><el-button type="primary" @click="addItem">添加</el-button></el-form-item>
    </el-form>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { billingPackageApi } from '@/api'
import { ElMessage } from 'element-plus'
import CrudTable from '@/components/CrudTable.vue'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const dialogVisible = ref(false), isEdit = ref(false), submitting = ref(false), editId = ref(null), formRef = ref(null)
const form = reactive({ packageCode:'', packageName:'', pricingModel:'SUBSCRIPTION', description:'' })
const rules = { packageCode:[{required:true,message:'必填'}], packageName:[{required:true,message:'必填'}], pricingModel:[{required:true,message:'必填'}] }

const itemDialogVisible = ref(false), currentPkgId = ref(null), currentItems = ref([])
const addForm = reactive({ productId: 1, quantity: 1 })

async function fetchData() {
  loading.value = true
  try { const r = await billingPackageApi.list({ page:page.value, pageSize:pageSize.value }); list.value = r.data.items; total.value = r.data.total } finally { loading.value = false }
}
function openDialog(row) { isEdit.value=!!row; editId.value=row?.id; Object.assign(form, row||{packageCode:'',packageName:'',pricingModel:'SUBSCRIPTION',description:''}); dialogVisible.value=true }
async function handleSubmit() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try { if(isEdit.value) await billingPackageApi.update(editId.value,form); else await billingPackageApi.create(form); ElMessage.success('操作成功'); dialogVisible.value=false; fetchData() } finally { submitting.value=false }
}
async function handleDelete(row) { await billingPackageApi.delete(row.id); ElMessage.success('删除成功'); fetchData() }

async function openItemDialog(row) { currentPkgId.value = row.id; currentItems.value = row.items || []; itemDialogVisible.value = true }
async function addItem() {
  await billingPackageApi.addItem(currentPkgId.value, addForm); ElMessage.success('添加成功'); fetchData()
  const r = await billingPackageApi.getById(currentPkgId.value); currentItems.value = r.data.items || []
}
async function removeItem(item) {
  await billingPackageApi.removeItem(currentPkgId.value, item.id); ElMessage.success('移除成功'); fetchData()
  currentItems.value = currentItems.value.filter(i => i.id !== item.id)
}
onMounted(fetchData)
</script>
