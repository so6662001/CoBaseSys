<template>
  <CrudTable title="折扣规则" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    @create="openDialog()" @edit="openDialog($event)" @delete="handleDelete($event)"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <el-table-column prop="id" label="ID" width="60" />
    <el-table-column prop="ruleName" label="规则名称" width="160" />
    <el-table-column prop="discountType" label="折扣类型" width="130">
      <template #default="{row}"><el-tag size="small">{{ {PRODUCT_DISCOUNT:'产品折扣',PACKAGE_DISCOUNT:'套餐折扣',AMOUNT_DISCOUNT:'满额折扣',AMOUNT_POINTS_BONUS:'满额送积分'}[row.discountType]||row.discountType }}</el-tag></template>
    </el-table-column>
    <el-table-column prop="discountRate" label="折扣率" width="80"><template #default="{row}">{{ row.discountRate ? (row.discountRate*10).toFixed(1)+'折' : '-' }}</template></el-table-column>
    <el-table-column prop="thresholdAmount" label="满额门槛(分)" width="110" />
    <el-table-column prop="bonusPoints" label="赠送积分" width="90" />
    <el-table-column prop="minQuantity" label="最低数量" width="80" />
    <el-table-column prop="priority" label="优先级" width="70" />
    <el-table-column prop="status" label="状态" width="60">
      <template #default="{row}"><el-tag :type="row.status===1?'success':'info'" size="small">{{ row.status===1?'启用':'禁用' }}</el-tag></template>
    </el-table-column>
  </CrudTable>
  <el-dialog v-model="dialogVisible" :title="isEdit?'编辑折扣':'新增折扣'" width="550px">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
      <el-form-item label="规则名称" prop="ruleName"><el-input v-model="form.ruleName" /></el-form-item>
      <el-form-item label="折扣类型" prop="discountType"><el-select v-model="form.discountType" style="width:100%">
        <el-option value="PRODUCT_DISCOUNT" label="产品折扣" /><el-option value="PACKAGE_DISCOUNT" label="套餐折扣" />
        <el-option value="AMOUNT_DISCOUNT" label="满额折扣" /><el-option value="AMOUNT_POINTS_BONUS" label="满额送积分" />
      </el-select></el-form-item>
      <el-form-item label="目标ID" v-if="form.discountType==='PRODUCT_DISCOUNT'||form.discountType==='PACKAGE_DISCOUNT'"><el-input-number v-model="form.targetId" :min="1" /></el-form-item>
      <el-form-item label="折扣率"><el-input-number v-model="form.discountRate" :min="0" :max="1" :step="0.05" :precision="2" /></el-form-item>
      <el-form-item label="满额门槛(分)"><el-input-number v-model="form.thresholdAmount" :min="0" /></el-form-item>
      <el-form-item label="赠送积分"><el-input-number v-model="form.bonusPoints" :min="0" /></el-form-item>
      <el-form-item label="最低数量"><el-input-number v-model="form.minQuantity" :min="1" /></el-form-item>
      <el-form-item label="优先级"><el-input-number v-model="form.priority" /></el-form-item>
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item label="生效开始"><el-date-picker v-model="form.effectiveFrom" type="datetime" style="width:100%" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="生效结束"><el-date-picker v-model="form.effectiveTo" type="datetime" style="width:100%" /></el-form-item></el-col>
      </el-row>
    </el-form>
    <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button></template>
  </el-dialog>
</template>
<script setup>
import { ref, onMounted, reactive } from 'vue'
import { billingRuleApi } from '@/api'
import { ElMessage } from 'element-plus'
import CrudTable from '@/components/CrudTable.vue'
const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const dialogVisible = ref(false), isEdit = ref(false), submitting = ref(false), editId = ref(null), formRef = ref(null)
const form = reactive({ ruleName:'', discountType:'PRODUCT_DISCOUNT', targetType:'PRODUCT', targetId:null, discountRate:0.9, thresholdAmount:0, bonusPoints:0, minQuantity:1, priority:0, effectiveFrom:null, effectiveTo:null })
const rules = { ruleName:[{required:true,message:'必填'}], discountType:[{required:true}] }
async function fetchData() { loading.value=true; try { const r=await billingRuleApi.listDiscounts({page:page.value,pageSize:pageSize.value}); list.value=r.data.items; total.value=r.data.total } finally { loading.value=false } }
function openDialog(row) { isEdit.value=!!row; editId.value=row?.id; if(row) Object.assign(form,row); else Object.assign(form,{ruleName:'',discountType:'PRODUCT_DISCOUNT',targetType:'PRODUCT',targetId:null,discountRate:0.9,thresholdAmount:0,bonusPoints:0,minQuantity:1,priority:0,effectiveFrom:null,effectiveTo:null}); dialogVisible.value=true }
async function handleSubmit() { try{await formRef.value.validate()}catch{return} submitting.value=true; try{if(isEdit.value)await billingRuleApi.updateDiscount(editId.value,form);else await billingRuleApi.createDiscount(form);ElMessage.success('操作成功');dialogVisible.value=false;fetchData()}finally{submitting.value=false} }
async function handleDelete(row) { await billingRuleApi.deleteDiscount(row.id); ElMessage.success('删除成功'); fetchData() }
onMounted(fetchData)
</script>
