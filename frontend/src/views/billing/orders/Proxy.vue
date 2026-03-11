<template>
  <el-card shadow="never">
    <div class="page-header"><h2>代客下单</h2></div>
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" style="max-width:700px">
      <el-divider content-position="left">客户信息</el-divider>
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item label="客户ID" prop="customerId"><el-input v-model="form.customerId" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="客户名称" prop="customerName"><el-input v-model="form.customerName" /></el-form-item></el-col>
      </el-row>
      <el-divider content-position="left">商品明细</el-divider>
      <div v-for="(item, idx) in form.items" :key="idx" style="display:flex;gap:8px;margin-bottom:12px;align-items:center">
        <el-select v-model="item.itemType" style="width:100px"><el-option value="PRODUCT" label="产品" /><el-option value="PACKAGE" label="套餐" /></el-select>
        <el-input-number v-model="item.itemId" placeholder="ID" :min="1" style="width:100px" />
        <el-input-number v-model="item.quantity" placeholder="数量" :min="1" style="width:90px" />
        <el-select v-model="item.periodType" style="width:80px"><el-option value="YEAR" label="年" /><el-option value="QUARTER" label="季" /><el-option value="MONTH" label="月" /></el-select>
        <el-input-number v-model="item.periodCount" :min="1" style="width:90px" />
        <el-button type="danger" text @click="form.items.splice(idx,1)" :disabled="form.items.length<=1">删除</el-button>
      </div>
      <el-button @click="form.items.push({itemType:'PRODUCT',itemId:null,quantity:1,periodType:'YEAR',periodCount:1})">+ 添加商品</el-button>
      <el-divider content-position="left">其他选项</el-divider>
      <el-row :gutter="16">
        <el-col :span="8"><el-form-item label="使用积分"><el-switch v-model="form.usePoints" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="直接确认收款"><el-switch v-model="form.autoConfirmPayment" /></el-form-item></el-col>
      </el-row>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      <el-form-item>
        <el-popconfirm title="确定提交代客下单？" @confirm="handleSubmit">
          <template #reference><el-button type="primary" :loading="submitting" size="large">提交订单</el-button></template>
        </el-popconfirm>
      </el-form-item>
    </el-form>
    <el-card v-if="result" shadow="never" style="margin-top:20px;background:#f0f9eb">
      <h3 style="color:#67c23a">下单成功</h3>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ result.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="实付金额">{{ result.actualAmountDisplay }}</el-descriptions-item>
        <el-descriptions-item label="支付状态">{{ result.paymentStatusText }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ result.customerName }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </el-card>
</template>
<script setup>
import { ref, reactive } from 'vue'
import { billingOrderApi } from '@/api'
import { ElMessage } from 'element-plus'
const formRef = ref(null), submitting = ref(false), result = ref(null)
const form = reactive({ customerId:'', customerName:'', items:[{itemType:'PRODUCT',itemId:null,quantity:1,periodType:'YEAR',periodCount:1}], usePoints:false, autoConfirmPayment:false, remark:'' })
const rules = { customerId:[{required:true,message:'必填'}], customerName:[{required:true,message:'必填'}] }
async function handleSubmit() {
  try { await formRef.value.validate() } catch { return }
  const invalidItem = form.items.find(i => !i.itemId || !i.quantity)
  if (invalidItem) { ElMessage.warning('请完善商品明细：ID和数量不能为空'); return }
  submitting.value = true
  try {
    const r = await billingOrderApi.proxyOrder(form, { operatorId:'admin', operatorName:'管理员' })
    result.value = r.data; ElMessage.success('下单成功')
  } finally { submitting.value = false }
}
</script>
