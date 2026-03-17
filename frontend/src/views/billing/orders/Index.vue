<template>
  <CrudTable title="订单管理" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    :showCreate="false" :showEdit="false" :showDelete="false" :actionWidth="200"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <template #filter>
      <div class="filter-bar">
        <el-input v-model="filterCustomerId" placeholder="客户ID" style="width:140px" clearable />
        <el-select v-model="filterPaymentStatus" placeholder="支付状态" style="width:120px" clearable>
          <el-option :value="0" label="待支付" /><el-option :value="1" label="已支付" /><el-option :value="2" label="已取消" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
    </template>
    <el-table-column prop="orderNo" label="订单号" width="190" show-overflow-tooltip />
    <el-table-column prop="customerId" label="客户ID" width="100" />
    <el-table-column prop="customerName" label="客户名称" width="130" />
    <el-table-column prop="orderType" label="订单类型" width="90">
      <template #default="{row}"><el-tag size="small">{{ {NEW_PURCHASE:'新购',RENEWAL:'续费',UPGRADE:'升级',TRIAL:'试用'}[row.orderType]||row.orderType }}</el-tag></template>
    </el-table-column>
    <el-table-column prop="orderSource" label="来源" width="70">
      <template #default="{row}"><el-tag :type="row.orderSource==='ADMIN'?'warning':''" size="small">{{ row.orderSource==='ADMIN'?'代客':'自助' }}</el-tag></template>
    </el-table-column>
    <el-table-column prop="actualAmountDisplay" label="实付金额" width="100" />
    <el-table-column prop="paymentStatusText" label="支付状态" width="80">
      <template #default="{row}"><el-tag :type="{0:'warning',1:'success',2:'info',3:'danger'}[row.paymentStatus]" size="small">{{ row.paymentStatusText }}</el-tag></template>
    </el-table-column>
    <el-table-column prop="createdAt" label="下单时间" width="160" />
    <el-table-column label="操作" width="200" fixed="right">
      <template #default="{row}">
        <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
        <el-popconfirm v-if="row.paymentStatus===0" title="确认收款？" @confirm="confirmPayment(row)">
          <template #reference><el-button link type="success">确认付款</el-button></template>
        </el-popconfirm>
        <el-popconfirm v-if="row.paymentStatus===0" title="确定取消？" @confirm="cancelOrder(row)">
          <template #reference><el-button link type="danger">取消</el-button></template>
        </el-popconfirm>
      </template>
    </el-table-column>
  </CrudTable>

  <el-dialog v-model="detailVisible" title="订单详情" width="700px">
    <el-descriptions :column="2" border v-if="detailOrder">
      <el-descriptions-item label="订单号">{{ detailOrder.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="客户">{{ detailOrder.customerName }} ({{ detailOrder.customerId }})</el-descriptions-item>
      <el-descriptions-item label="类型">{{ detailOrder.orderType }}</el-descriptions-item>
      <el-descriptions-item label="来源">{{ detailOrder.orderSource==='ADMIN'?'代客下单 - '+detailOrder.operatorName:'客户自助' }}</el-descriptions-item>
      <el-descriptions-item label="总金额">{{ (detailOrder.totalAmount/100).toFixed(2) }}元</el-descriptions-item>
      <el-descriptions-item label="折扣">-{{ (detailOrder.discountAmount/100).toFixed(2) }}元</el-descriptions-item>
      <el-descriptions-item label="积分抵扣">-{{ (detailOrder.pointsDeductAmount/100).toFixed(2) }}元 ({{ detailOrder.pointsUsed }}积分)</el-descriptions-item>
      <el-descriptions-item label="实付金额"><span style="color:#f56c6c;font-weight:700;font-size:16px">{{ detailOrder.actualAmountDisplay }}</span></el-descriptions-item>
    </el-descriptions>
    <el-table :data="detailOrder?.items||[]" border style="margin-top:16px">
      <el-table-column prop="itemName" label="商品" /><el-table-column prop="quantity" label="数量" width="60" />
      <el-table-column label="单价" width="100"><template #default="{row}">{{ row.unitPrice?(row.unitPrice/100).toFixed(2)+'元':'-' }}</template></el-table-column>
      <el-table-column label="金额" width="100"><template #default="{row}">{{ row.actualAmount?(row.actualAmount/100).toFixed(2)+'元':'-' }}</template></el-table-column>
      <el-table-column prop="periodType" label="周期" width="60" /><el-table-column prop="periodCount" label="周期数" width="60" />
      <el-table-column label="赠品" width="50"><template #default="{row}"><el-tag v-if="row.isGift" type="success" size="small">赠</el-tag></template></el-table-column>
    </el-table>
  </el-dialog>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { billingOrderApi } from '@/api'
import { ElMessage } from 'element-plus'
import CrudTable from '@/components/CrudTable.vue'
const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const filterCustomerId = ref(''), filterPaymentStatus = ref(null)
const detailVisible = ref(false), detailOrder = ref(null)
async function fetchData() {
  loading.value = true
  try {
    const params = { page:page.value, pageSize:pageSize.value }
    if (filterCustomerId.value) params.customerId = filterCustomerId.value
    if (filterPaymentStatus.value !== null && filterPaymentStatus.value !== '') params.paymentStatus = filterPaymentStatus.value
    const r = await billingOrderApi.list(params); list.value = r.data.items; total.value = r.data.total
  } finally { loading.value = false }
}
async function viewDetail(row) { const r = await billingOrderApi.getById(row.orderNo||row.id); detailOrder.value = r.data; detailVisible.value = true }
async function confirmPayment(row) { await billingOrderApi.confirmPayment(row.id, { paymentMethod:'offline' }); ElMessage.success('确认付款成功'); fetchData() }
async function cancelOrder(row) { await billingOrderApi.cancel(row.id); ElMessage.success('订单已取消'); fetchData() }
onMounted(fetchData)
</script>
