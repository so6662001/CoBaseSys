<template>
  <el-card shadow="never">
    <div class="page-header"><h2>申请开票</h2></div>

    <el-steps :active="step" align-center style="margin-bottom:24px">
      <el-step title="选择订单" /><el-step title="填写开票信息" /><el-step title="提交完成" />
    </el-steps>

    <!-- Step 1: 选择订单 -->
    <div v-if="step===0">
      <el-alert type="info" :closable="false" style="margin-bottom:16px">请选择已付款且未申请开票的订单，支持合并多个订单开票</el-alert>
      <div class="filter-bar"><el-input v-model="customerId" placeholder="客户ID" style="width:160px" /><el-button type="primary" @click="loadOrders">查询</el-button></div>
      <el-table :data="orders" border @selection-change="onSelect" v-loading="ordersLoading">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="orderNo" label="订单号" width="190" />
        <el-table-column prop="customerName" label="客户" width="130" />
        <el-table-column prop="actualAmountDisplay" label="实付金额" width="110" />
        <el-table-column prop="createdAt" label="下单时间" width="160" />
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <span style="margin-right:16px">已选 {{ selectedOrders.length }} 个订单，合计 <b style="color:#f56c6c">{{ (selectedTotal/100).toFixed(2) }}元</b></span>
        <el-button type="primary" :disabled="!selectedOrders.length" @click="step=1">下一步</el-button>
      </div>
    </div>

    <!-- Step 2: 填写开票信息 -->
    <div v-if="step===1" style="max-width:600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="发票类型" prop="invoiceType">
          <el-select v-model="form.invoiceType" style="width:100%">
            <el-option value="NORMAL" label="增值税普通发票" /><el-option value="SPECIAL" label="增值税专用发票" />
          </el-select>
        </el-form-item>
        <el-form-item label="发票抬头" prop="titleName"><el-input v-model="form.titleName" /></el-form-item>
        <el-form-item label="纳税人识别号" prop="taxNo"><el-input v-model="form.taxNo" /></el-form-item>
        <template v-if="form.invoiceType==='SPECIAL'">
          <el-form-item label="开户银行"><el-input v-model="form.bankName" /></el-form-item>
          <el-form-item label="银行账号"><el-input v-model="form.bankAccount" /></el-form-item>
          <el-form-item label="公司地址"><el-input v-model="form.companyAddress" /></el-form-item>
          <el-form-item label="公司电话"><el-input v-model="form.companyPhone" /></el-form-item>
        </template>
        <el-form-item label="接收邮箱" prop="receiverEmail"><el-input v-model="form.receiverEmail" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <div style="text-align:right">
        <el-button @click="step=0">上一步</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">提交申请</el-button>
      </div>
    </div>

    <!-- Step 3: 完成 -->
    <el-result v-if="step===2" icon="success" title="开票申请已提交" sub-title="财务审核通过后将自动开具发票并发送至您的邮箱">
      <template #extra>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请编号">{{ result?.applicationNo }}</el-descriptions-item>
          <el-descriptions-item label="金额">{{ result?.totalAmountDisplay }}</el-descriptions-item>
          <el-descriptions-item label="发票类型">{{ result?.invoiceTypeText }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag type="warning">{{ result?.statusText }}</el-tag></el-descriptions-item>
        </el-descriptions>
        <el-button type="primary" style="margin-top:16px" @click="step=0;selectedOrders=[]">继续申请</el-button>
      </template>
    </el-result>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { invoiceApi } from '@/api'
import { useAppStore } from '@/stores/app'
import { ElMessage } from 'element-plus'

const app = useAppStore()
const step = ref(0), customerId = ref(''), ordersLoading = ref(false), submitting = ref(false)
const orders = ref([]), selectedOrders = ref([]), result = ref(null), formRef = ref(null)
const selectedTotal = computed(() => selectedOrders.value.reduce((s, o) => s + (o.actualAmount || 0), 0))

const form = reactive({ invoiceType:'NORMAL', titleName:'', taxNo:'', bankName:'', bankAccount:'', companyAddress:'', companyPhone:'', receiverEmail:'', remark:'' })
const rules = {
  invoiceType:[{required:true}], titleName:[{required:true,message:'请输入发票抬头'}],
  taxNo:[{required:true,message:'请输入纳税人识别号'}], receiverEmail:[{required:true,type:'email',message:'请输入正确的邮箱'}]
}

async function loadOrders() {
  if (!customerId.value) return
  ordersLoading.value = true
  try {
    const r = await invoiceApi.availableOrders({ customerId: customerId.value, page:1, pageSize:100 })
    orders.value = (r.data?.items || []).filter(o => !o.invoiceStatus || o.invoiceStatus === 0)
  } finally { ordersLoading.value = false }
}

function onSelect(rows) { selectedOrders.value = rows }

async function handleSubmit() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    const r = await invoiceApi.apply({
      ...form, customerId: customerId.value, customerName: selectedOrders.value[0]?.customerName || '',
      orderIds: selectedOrders.value.map(o => o.id)
    })
    result.value = r.data; step.value = 2; ElMessage.success('开票申请已提交')
  } finally { submitting.value = false }
}
</script>
