<template>
  <CrudTable title="订阅管理" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    :showCreate="false" :showEdit="false" :showDelete="false" :actionWidth="240"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <template #filter>
      <div class="filter-bar">
        <el-input v-model="filterCustomerId" placeholder="客户ID" style="width:140px" clearable />
        <el-select v-model="filterStatus" placeholder="状态" style="width:120px" clearable>
          <el-option value="ACTIVE" label="正常" /><el-option value="TRIAL" label="试用" />
          <el-option value="EXPIRING" label="即将到期" /><el-option value="EXPIRED" label="已过期" />
          <el-option value="SUSPENDED" label="已暂停" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
    </template>
    <el-table-column prop="subscriptionNo" label="订阅编号" width="180" show-overflow-tooltip />
    <el-table-column prop="customerId" label="客户ID" width="100" />
    <el-table-column prop="sourceName" label="产品/套餐" width="150" />
    <el-table-column prop="pricingModel" label="收费模式" width="100" />
    <el-table-column prop="quantity" label="数量" width="60" />
    <el-table-column prop="status" label="状态" width="80">
      <template #default="{row}"><el-tag :type="{ACTIVE:'success',TRIAL:'warning',EXPIRING:'danger',EXPIRED:'info',SUSPENDED:'info'}[row.status]" size="small">{{ {ACTIVE:'正常',TRIAL:'试用',EXPIRING:'即将到期',EXPIRED:'已过期',SUSPENDED:'已暂停'}[row.status]||row.status }}</el-tag></template>
    </el-table-column>
    <el-table-column label="时间/用量" min-width="180">
      <template #default="{row}">
        <div v-if="row.totalDays>0">时间: {{ row.daysUsed }}天/{{ row.totalDays }}天, 剩余{{ row.daysRemaining }}天</div>
        <div v-if="row.usageQuota>0">用量: {{ row.usageUsed }}/{{ row.usageQuota }} {{ row.usageUnit }}, 剩余{{ row.usageRemaining }}</div>
        <div v-if="row.spaceTotal>0">空间: {{ (row.spaceUsed/1073741824).toFixed(1) }}GB/{{ (row.spaceTotal/1073741824).toFixed(1) }}GB ({{ row.spaceUsageRate }})</div>
      </template>
    </el-table-column>
    <el-table-column prop="endDate" label="到期日" width="100" />
    <el-table-column label="操作" width="240" fixed="right">
      <template #default="{row}">
        <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
        <el-button v-if="row.status==='ACTIVE'||row.status==='EXPIRING'" link type="success" @click="openExtend(row)">延期</el-button>
        <el-button v-if="row.status==='ACTIVE'" link type="warning" @click="suspend(row)">暂停</el-button>
        <el-button v-if="row.status==='SUSPENDED'" link type="success" @click="resume(row)">恢复</el-button>
        <el-button v-if="row.usageQuota>0" link type="primary" @click="viewLedger(row)">流水</el-button>
      </template>
    </el-table-column>
  </CrudTable>

  <el-dialog v-model="detailVisible" title="订阅详情" width="600px">
    <el-descriptions :column="2" border v-if="detailSub">
      <el-descriptions-item label="订阅编号">{{ detailSub.subscriptionNo }}</el-descriptions-item>
      <el-descriptions-item label="产品/套餐">{{ detailSub.sourceName }}</el-descriptions-item>
      <el-descriptions-item label="收费模式">{{ detailSub.pricingModel }}</el-descriptions-item>
      <el-descriptions-item label="数量">{{ detailSub.quantity }}</el-descriptions-item>
      <el-descriptions-item label="开始日期">{{ detailSub.startDate }}</el-descriptions-item>
      <el-descriptions-item label="到期日期">{{ detailSub.endDate }}</el-descriptions-item>
      <el-descriptions-item label="总天数">{{ detailSub.totalDays }}</el-descriptions-item>
      <el-descriptions-item label="已用天数">{{ detailSub.daysUsed }}</el-descriptions-item>
      <el-descriptions-item label="剩余天数"><span :style="{color:detailSub.daysRemaining<=15?'#f56c6c':'#67c23a',fontWeight:700}">{{ detailSub.daysRemaining }}天</span></el-descriptions-item>
      <el-descriptions-item label="续费价格">{{ detailSub.renewalPriceDisplay }}</el-descriptions-item>
      <el-descriptions-item v-if="detailSub.usageQuota>0" label="用量">{{ detailSub.usageUsed }}/{{ detailSub.usageQuota }} {{ detailSub.usageUnit }}</el-descriptions-item>
      <el-descriptions-item v-if="detailSub.spaceTotal>0" label="空间">{{ (detailSub.spaceUsed/1073741824).toFixed(1) }}GB / {{ (detailSub.spaceTotal/1073741824).toFixed(1) }}GB</el-descriptions-item>
    </el-descriptions>
  </el-dialog>

  <el-dialog v-model="extendVisible" title="手动延期" width="400px">
    <el-form label-width="80px">
      <el-form-item label="延长天数"><el-input-number v-model="extendDays" :min="1" :max="3650" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="extendVisible=false">取消</el-button><el-button type="primary" @click="doExtend">确定延期</el-button></template>
  </el-dialog>

  <el-dialog v-model="ledgerVisible" title="用量流水" width="700px">
    <el-table :data="ledgerList" border>
      <el-table-column prop="action" label="动作" width="70" /><el-table-column prop="quantity" label="数量" width="80" />
      <el-table-column prop="unit" label="单位" width="60" /><el-table-column prop="balanceBefore" label="变动前" width="80" />
      <el-table-column prop="balanceAfter" label="变动后" width="80" /><el-table-column prop="bizDescription" label="说明" />
      <el-table-column prop="createdAt" label="时间" width="160" />
    </el-table>
  </el-dialog>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { billingSubApi } from '@/api'
import { ElMessage } from 'element-plus'
import CrudTable from '@/components/CrudTable.vue'
const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const filterCustomerId = ref(''), filterStatus = ref('')
const detailVisible = ref(false), detailSub = ref(null)
const extendVisible = ref(false), extendDays = ref(30), extendSubId = ref(null)
const ledgerVisible = ref(false), ledgerList = ref([])

async function fetchData() {
  loading.value = true
  try {
    const params = { page:page.value, pageSize:pageSize.value }
    if (filterCustomerId.value) params.customerId = filterCustomerId.value
    if (filterStatus.value) params.status = filterStatus.value
    const r = await billingSubApi.list(params); list.value = r.data.items; total.value = r.data.total
  } finally { loading.value = false }
}
async function viewDetail(row) { const r = await billingSubApi.getById(row.id); detailSub.value = r.data; detailVisible.value = true }
function openExtend(row) { extendSubId.value = row.id; extendDays.value = 30; extendVisible.value = true }
async function doExtend() { await billingSubApi.extend(extendSubId.value, extendDays.value); ElMessage.success('延期成功'); extendVisible.value = false; fetchData() }
async function suspend(row) { await billingSubApi.suspend(row.id); ElMessage.success('已暂停'); fetchData() }
async function resume(row) { await billingSubApi.resume(row.id); ElMessage.success('已恢复'); fetchData() }
async function viewLedger(row) { const r = await billingSubApi.usageLedger(row.id, { page:1, pageSize:50 }); ledgerList.value = r.data.items; ledgerVisible.value = true }
onMounted(fetchData)
</script>
