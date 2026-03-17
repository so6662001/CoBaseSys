<template>
  <CrudTable title="赠送规则" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    @create="openDialog()" @edit="openDialog($event)" @delete="handleDelete($event)"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <el-table-column prop="id" label="ID" width="60" />
    <el-table-column prop="ruleName" label="规则名称" width="200" />
    <el-table-column prop="conditionType" label="条件类型" width="130">
      <template #default="{row}"><el-tag size="small">{{ {BUY_PRODUCT:'买产品满N',BUY_PACKAGE:'买套餐满N',SPEND_AMOUNT:'消费满额'}[row.conditionType]||row.conditionType }}</el-tag></template>
    </el-table-column>
    <el-table-column prop="conditionQuantity" label="条件数量" width="90" />
    <el-table-column prop="giftType" label="赠送类型" width="90">
      <template #default="{row}"><el-tag :type="{PRODUCT:'success',PACKAGE:'warning',POINTS:''}[row.giftType]" size="small">{{ {PRODUCT:'产品',PACKAGE:'套餐',POINTS:'积分'}[row.giftType] }}</el-tag></template>
    </el-table-column>
    <el-table-column prop="giftQuantity" label="赠送数量" width="80" />
    <el-table-column prop="giftPoints" label="赠送积分" width="80" />
    <el-table-column prop="status" label="状态" width="60">
      <template #default="{row}"><el-tag :type="row.status===1?'success':'info'" size="small">{{ row.status===1?'启用':'禁用' }}</el-tag></template>
    </el-table-column>
  </CrudTable>
  <el-dialog v-model="dialogVisible" :title="isEdit?'编辑赠送':'新增赠送'" width="550px">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
      <el-form-item label="规则名称" prop="ruleName"><el-input v-model="form.ruleName" placeholder="如: 买10个企业版送1个" /></el-form-item>
      <el-form-item label="条件类型" prop="conditionType"><el-select v-model="form.conditionType" style="width:100%">
        <el-option value="BUY_PRODUCT" label="购买产品满N个" /><el-option value="BUY_PACKAGE" label="购买套餐满N个" /><el-option value="SPEND_AMOUNT" label="消费满额" />
      </el-select></el-form-item>
      <el-form-item label="条件目标ID" v-if="form.conditionType!=='SPEND_AMOUNT'"><el-input-number v-model="form.conditionTargetId" :min="1" /></el-form-item>
      <el-form-item label="条件数量"><el-input-number v-model="form.conditionQuantity" :min="1" /></el-form-item>
      <el-divider content-position="left">赠送内容</el-divider>
      <el-form-item label="赠送类型" prop="giftType"><el-select v-model="form.giftType" style="width:100%">
        <el-option value="PRODUCT" label="赠送产品" /><el-option value="PACKAGE" label="赠送套餐" /><el-option value="POINTS" label="赠送积分" />
      </el-select></el-form-item>
      <el-form-item label="赠品ID" v-if="form.giftType!=='POINTS'"><el-input-number v-model="form.giftTargetId" :min="1" /></el-form-item>
      <el-form-item label="赠送数量" v-if="form.giftType!=='POINTS'"><el-input-number v-model="form.giftQuantity" :min="1" /></el-form-item>
      <el-form-item label="赠送积分" v-if="form.giftType==='POINTS'"><el-input-number v-model="form.giftPoints" :min="0" /></el-form-item>
      <el-form-item label="赠品有效天数"><el-input-number v-model="form.giftValidityDays" :min="0" /></el-form-item>
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
const form = reactive({ ruleName:'', conditionType:'BUY_PRODUCT', conditionTargetType:'PRODUCT', conditionTargetId:null, conditionQuantity:10, giftType:'PRODUCT', giftTargetId:null, giftQuantity:1, giftPoints:0, giftValidityDays:365, effectiveFrom:null, effectiveTo:null })
const rules = { ruleName:[{required:true,message:'必填'}], conditionType:[{required:true}], giftType:[{required:true}] }
async function fetchData() { loading.value=true; try { const r=await billingRuleApi.listGifts({page:page.value,pageSize:pageSize.value}); list.value=r.data.items; total.value=r.data.total } finally { loading.value=false } }
function openDialog(row) { isEdit.value=!!row; editId.value=row?.id; if(row) Object.assign(form,row); else Object.assign(form,{ruleName:'',conditionType:'BUY_PRODUCT',conditionTargetType:'PRODUCT',conditionTargetId:null,conditionQuantity:10,giftType:'PRODUCT',giftTargetId:null,giftQuantity:1,giftPoints:0,giftValidityDays:365,effectiveFrom:null,effectiveTo:null}); dialogVisible.value=true }
async function handleSubmit() { try{await formRef.value.validate()}catch{return} submitting.value=true; try{if(isEdit.value)await billingRuleApi.updateGift(editId.value,form);else await billingRuleApi.createGift(form);ElMessage.success('操作成功');dialogVisible.value=false;fetchData()}finally{submitting.value=false} }
async function handleDelete(row) { await billingRuleApi.deleteGift(row.id); ElMessage.success('删除成功'); fetchData() }
onMounted(fetchData)
</script>
