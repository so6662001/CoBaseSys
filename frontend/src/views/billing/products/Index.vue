<template>
  <CrudTable title="产品管理" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    @create="openDialog()" @edit="openDialog($event)" @delete="handleDelete($event)"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <el-table-column prop="id" label="ID" width="60" />
    <el-table-column prop="productCode" label="产品编码" width="130" />
    <el-table-column prop="productName" label="产品名称" width="160" />
    <el-table-column prop="category" label="分类" width="90" />
    <el-table-column prop="pricingModel" label="收费模式" width="130">
      <template #default="{row}"><el-tag size="small">{{ modelLabel(row.pricingModel) }}</el-tag></template>
    </el-table-column>
    <el-table-column prop="trialEnabled" label="试用" width="60">
      <template #default="{row}"><el-tag :type="row.trialEnabled?'success':'info'" size="small">{{ row.trialEnabled ? row.trialDays+'天' : '否' }}</el-tag></template>
    </el-table-column>
    <el-table-column prop="pointsPayable" label="积分支付" width="80">
      <template #default="{row}">{{ row.pointsPayable ? '支持('+row.maxPointsRatio*100+'%)' : '不支持' }}</template>
    </el-table-column>
    <el-table-column prop="status" label="状态" width="90">
      <template #default="{row}">
        <el-switch :model-value="row.status===1" @change="toggleStatus(row)" active-text="上架" inactive-text="下架" inline-prompt size="small" />
      </template>
    </el-table-column>
  </CrudTable>

  <el-dialog v-model="dialogVisible" :title="isEdit?'编辑产品':'新增产品'" width="650px" top="5vh">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12"><el-form-item label="产品编码" prop="productCode"><el-input v-model="form.productCode" :disabled="isEdit" /></el-form-item></el-col>
        <el-col :xs="24" :sm="12"><el-form-item label="产品名称" prop="productName"><el-input v-model="form.productName" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12"><el-form-item label="分类"><el-select v-model="form.category" placeholder="选择分类">
          <el-option value="software" label="软件" /><el-option value="service" label="服务" />
          <el-option value="resource" label="资源" /><el-option value="storage" label="存储" />
        </el-select></el-form-item></el-col>
        <el-col :xs="24" :sm="12"><el-form-item label="收费模式" prop="pricingModel"><el-select v-model="form.pricingModel">
          <el-option v-for="m in pricingModels" :key="m.value" :value="m.value" :label="m.label" />
        </el-select></el-form-item></el-col>
      </el-row>
      <el-form-item label="产品描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
      <el-divider content-position="left">试用设置</el-divider>
      <el-row :gutter="16">
        <el-col :span="6"><el-form-item label="支持试用"><el-switch v-model="form.trialEnabled" :active-value="1" :inactive-value="0" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="试用天数"><el-input-number v-model="form.trialDays" :min="0" size="small" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="可延长"><el-switch v-model="form.trialExtendEnabled" :active-value="1" :inactive-value="0" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="最大延长天"><el-input-number v-model="form.trialMaxExtendDays" :min="0" size="small" /></el-form-item></el-col>
      </el-row>
      <el-divider content-position="left">积分支付</el-divider>
      <el-row :gutter="16">
        <el-col :span="8"><el-form-item label="积分支付"><el-switch v-model="form.pointsPayable" :active-value="1" :inactive-value="0" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="最大抵扣比例"><el-input-number v-model="form.maxPointsRatio" :min="0" :max="1" :step="0.05" :precision="2" size="small" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="积分汇率"><el-input-number v-model="form.pointsExchangeRate" :min="0" :precision="2" size="small" /></el-form-item></el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible=false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { billingProductApi } from '@/api'
import { ElMessage } from 'element-plus'
import CrudTable from '@/components/CrudTable.vue'

const pricingModels = [
  { value: 'ONE_TIME_ANNUAL', label: '一次性+年服务费' },
  { value: 'SUBSCRIPTION', label: '订阅+增量' },
  { value: 'USAGE_BASED', label: '按量计费' },
  { value: 'TIERED_PROGRESSIVE', label: '阶梯累进' },
  { value: 'CLOUD_RENTAL', label: '云端租用' },
  { value: 'SPACE_RENTAL', label: '空间租用' },
  { value: 'ONE_TIME', label: '一次性买断' },
  { value: 'QUOTA_PLAN', label: '月费配额套餐' },
]
const modelLabel = (v) => pricingModels.find(m => m.value === v)?.label || v

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const dialogVisible = ref(false), isEdit = ref(false), submitting = ref(false), editId = ref(null), formRef = ref(null)
const form = reactive({ productCode:'', productName:'', category:'software', description:'', pricingModel:'SUBSCRIPTION',
  trialEnabled:0, trialDays:0, trialExtendEnabled:0, trialMaxExtendDays:0,
  pointsPayable:0, maxPointsRatio:0, pointsExchangeRate:0, sortOrder:0 })
const rules = { productCode: [{ required:true, message:'必填' }], productName: [{ required:true, message:'必填' }], pricingModel: [{ required:true, message:'必填' }] }

async function fetchData() {
  loading.value = true
  try { const r = await billingProductApi.list({ page: page.value, pageSize: pageSize.value }); list.value = r.data.items; total.value = r.data.total } finally { loading.value = false }
}
function openDialog(row) {
  isEdit.value = !!row; editId.value = row?.id
  const defaults = { productCode:'', productName:'', category:'software', description:'', pricingModel:'SUBSCRIPTION', trialEnabled:0, trialDays:0, trialExtendEnabled:0, trialMaxExtendDays:0, pointsPayable:0, maxPointsRatio:0, pointsExchangeRate:0, sortOrder:0 }
  Object.assign(form, row || defaults)
  dialogVisible.value = true
}
async function handleSubmit() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    if (isEdit.value) await billingProductApi.update(editId.value, form)
    else await billingProductApi.create(form)
    ElMessage.success(isEdit.value?'更新成功':'创建成功'); dialogVisible.value = false; fetchData()
  } finally { submitting.value = false }
}
async function handleDelete(row) { await billingProductApi.delete(row.id); ElMessage.success('删除成功'); fetchData() }
async function toggleStatus(row) { await billingProductApi.update(row.id, { ...row, status: row.status===1?0:1 }); ElMessage.success('状态已切换'); fetchData() }
onMounted(fetchData)
</script>
