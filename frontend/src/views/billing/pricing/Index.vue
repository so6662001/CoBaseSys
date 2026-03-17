<template>
  <CrudTable title="定价方案" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    @create="openDialog()" @edit="openDialog($event)" @delete="handleDelete($event)"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <template #filter>
      <div class="filter-bar">
        <el-select v-model="filterTargetType" placeholder="目标类型" style="width:120px" clearable>
          <el-option value="PRODUCT" label="产品" /><el-option value="PACKAGE" label="套餐" />
        </el-select>
        <el-input v-model="filterTargetId" placeholder="目标ID" style="width:120px" clearable />
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
    </template>
    <el-table-column prop="id" label="ID" width="60" />
    <el-table-column prop="planName" label="方案名称" width="150" />
    <el-table-column prop="targetType" label="目标" width="70" />
    <el-table-column prop="targetId" label="目标ID" width="70" />
    <el-table-column prop="pricingModel" label="收费模式" width="130" />
    <el-table-column label="价格信息" min-width="200">
      <template #default="{row}">
        <span v-if="row.pricingModel==='ONE_TIME_ANNUAL'">软件款{{ (row.softwareFee/100).toFixed(2) }}元 + 年服务费{{ (row.annualServiceFee/100).toFixed(2) }}元</span>
        <span v-else-if="row.pricingModel==='SUBSCRIPTION'">{{ row.periodType }} {{ (row.periodPrice/100).toFixed(2) }}元/周期</span>
        <span v-else-if="row.pricingModel==='USAGE_BASED'||row.pricingModel==='TIERED_PROGRESSIVE'">{{ (row.unitPrice/100).toFixed(2) }}元/{{ row.unitName }}</span>
        <span v-else-if="row.pricingModel==='CLOUD_RENTAL'">{{ (row.rentalPrice/100).toFixed(2) }}元/{{ row.rentalPeriodType }}</span>
        <span v-else-if="row.pricingModel==='SPACE_RENTAL'">{{ (row.spaceUnitPrice/100).toFixed(2) }}元/{{ row.spaceUnit }}</span>
        <span v-else-if="row.pricingModel==='ONE_TIME'">{{ (row.oneTimePrice/100).toFixed(2) }}元</span>
      </template>
    </el-table-column>
    <el-table-column prop="priority" label="优先级" width="70" />
    <el-table-column prop="status" label="状态" width="60">
      <template #default="{row}"><el-tag :type="row.status===1?'success':'info'" size="small">{{ row.status===1?'启用':'禁用' }}</el-tag></template>
    </el-table-column>
  </CrudTable>

  <el-dialog v-model="dialogVisible" :title="isEdit?'编辑定价':'新增定价'" width="700px" top="3vh">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item label="目标类型" prop="targetType"><el-select v-model="form.targetType" style="width:100%"><el-option value="PRODUCT" label="产品" /><el-option value="PACKAGE" label="套餐" /></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="目标ID" prop="targetId"><el-input-number v-model="form.targetId" :min="1" style="width:100%" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item label="方案名称" prop="planName"><el-input v-model="form.planName" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="收费模式" prop="pricingModel"><el-select v-model="form.pricingModel" style="width:100%">
          <el-option value="ONE_TIME_ANNUAL" label="一次性+年服务费" /><el-option value="SUBSCRIPTION" label="订阅+增量" />
          <el-option value="USAGE_BASED" label="按量计费" /><el-option value="TIERED_PROGRESSIVE" label="阶梯累进" />
          <el-option value="CLOUD_RENTAL" label="云端租用" /><el-option value="SPACE_RENTAL" label="空间租用" />
          <el-option value="ONE_TIME" label="一次性买断" />
          <el-option value="QUOTA_PLAN" label="月费配额套餐" />
        </el-select></el-form-item></el-col>
      </el-row>
      <el-divider content-position="left">价格配置（按收费模式填写）</el-divider>
      <template v-if="form.pricingModel==='ONE_TIME_ANNUAL'">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="软件款(分)"><el-input-number v-model="form.softwareFee" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="年服务费(分)"><el-input-number v-model="form.annualServiceFee" :min="0" style="width:100%" /></el-form-item></el-col>
        </el-row>
      </template>
      <template v-if="form.pricingModel==='SUBSCRIPTION'">
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="周期"><el-select v-model="form.periodType"><el-option value="YEAR" label="年" /><el-option value="QUARTER" label="季" /><el-option value="MONTH" label="月" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="周期价(分)"><el-input-number v-model="form.periodPrice" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="含基础量"><el-input-number v-model="form.includedQuantity" :min="0" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="增量单位"><el-input v-model="form.overageUnitName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="增量单价(分)"><el-input-number v-model="form.overageUnitPrice" :min="0" style="width:100%" /></el-form-item></el-col>
        </el-row>
      </template>
      <template v-if="form.pricingModel==='USAGE_BASED'||form.pricingModel==='TIERED_PROGRESSIVE'">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="计量单位"><el-input v-model="form.unitName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="单价(分)"><el-input-number v-model="form.unitPrice" :min="0" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="阶梯配置" v-if="form.pricingModel==='TIERED_PROGRESSIVE'"><el-input v-model="form.tieredPricing" type="textarea" :rows="3" placeholder='{"tiers":[{"min":0,"max":100,"unit_price":100}]}' /></el-form-item>
      </template>
      <template v-if="form.pricingModel==='CLOUD_RENTAL'">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="租用周期"><el-select v-model="form.rentalPeriodType"><el-option value="YEAR" label="年" /><el-option value="QUARTER" label="季" /><el-option value="MONTH" label="月" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="租用价(分)"><el-input-number v-model="form.rentalPrice" :min="0" style="width:100%" /></el-form-item></el-col>
        </el-row>
      </template>
      <template v-if="form.pricingModel==='SPACE_RENTAL'">
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="空间单位"><el-select v-model="form.spaceUnit"><el-option value="GB" label="GB" /><el-option value="TB" label="TB" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="单位价(分)"><el-input-number v-model="form.spaceUnitPrice" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="租用周期"><el-select v-model="form.rentalPeriodType"><el-option value="YEAR" label="年" /><el-option value="MONTH" label="月" /></el-select></el-form-item></el-col>
        </el-row>
      </template>
      <template v-if="form.pricingModel==='ONE_TIME'">
        <el-form-item label="买断价(分)"><el-input-number v-model="form.oneTimePrice" :min="0" style="width:100%" /></el-form-item>
      </template>
      <template v-if="form.pricingModel==='QUOTA_PLAN'">
        <el-form-item label="月费(分)"><el-input-number v-model="form.periodPrice" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="配额配置">
          <el-input v-model="form.quotaConfig" type="textarea" :rows="4"
            placeholder='{"quotas":[{"dimension":"customer_count","label":"客户数","limit":50},{"dimension":"delivery_count","label":"提货次数/月","limit":200}]}' />
        </el-form-item>
      </template>
      <el-divider content-position="left">有效期</el-divider>
      <el-row :gutter="16">
        <el-col :span="8"><el-form-item label="有效天数"><el-input-number v-model="form.validityDays" :min="0" style="width:100%" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="优先级"><el-input-number v-model="form.priority" style="width:100%" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item label="生效开始"><el-date-picker v-model="form.effectiveFrom" type="datetime" style="width:100%" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="生效结束"><el-date-picker v-model="form.effectiveTo" type="datetime" style="width:100%" /></el-form-item></el-col>
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
import { billingPricingApi } from '@/api'
import { ElMessage } from 'element-plus'
import CrudTable from '@/components/CrudTable.vue'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const dialogVisible = ref(false), isEdit = ref(false), submitting = ref(false), editId = ref(null), formRef = ref(null)
const filterTargetType = ref(''), filterTargetId = ref('')
const form = reactive({ targetType:'PRODUCT', targetId:null, planName:'', pricingModel:'SUBSCRIPTION',
  softwareFee:0, annualServiceFee:0, periodType:'YEAR', periodPrice:0, includedQuantity:0,
  overageUnitName:'', overageUnitPrice:0, unitName:'', unitPrice:0, tieredPricing:'',
  rentalPeriodType:'MONTH', rentalPrice:0, spaceUnit:'GB', spaceUnitPrice:0, oneTimePrice:0,
  validityDays:365, priority:0, effectiveFrom:null, effectiveTo:null, quotaConfig:'' })
const rules = { targetType:[{required:true}], targetId:[{required:true}], planName:[{required:true,message:'必填'}], pricingModel:[{required:true}] }

async function fetchData() {
  loading.value = true
  try {
    const params = { page:page.value, pageSize:pageSize.value }
    if (filterTargetType.value) params.targetType = filterTargetType.value
    if (filterTargetId.value) params.targetId = filterTargetId.value
    const r = await billingPricingApi.list(params); list.value = r.data.items; total.value = r.data.total
  } finally { loading.value = false }
}
function openDialog(row) {
  isEdit.value = !!row; editId.value = row?.id
  if (row) Object.assign(form, row)
  else Object.assign(form, { targetType:'PRODUCT', targetId:null, planName:'', pricingModel:'SUBSCRIPTION', softwareFee:0, annualServiceFee:0, periodType:'YEAR', periodPrice:0, includedQuantity:0, overageUnitName:'', overageUnitPrice:0, unitName:'', unitPrice:0, tieredPricing:'', rentalPeriodType:'MONTH', rentalPrice:0, spaceUnit:'GB', spaceUnitPrice:0, oneTimePrice:0, validityDays:365, priority:0, effectiveFrom:null, effectiveTo:null, quotaConfig:'' })
  dialogVisible.value = true
}
async function handleSubmit() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try { if(isEdit.value) await billingPricingApi.update(editId.value,form); else await billingPricingApi.create(form); ElMessage.success('操作成功'); dialogVisible.value=false; fetchData() } finally { submitting.value=false }
}
async function handleDelete(row) { await billingPricingApi.delete(row.id); ElMessage.success('删除成功'); fetchData() }
onMounted(fetchData)
</script>
