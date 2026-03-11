<template>
  <div>
    <h2 style="margin:0 0 20px">运营报表</h2>
    <el-row :gutter="16" style="margin-bottom:20px">
      <el-col :span="6"><el-card shadow="hover" style="cursor:pointer" @click="activeTab='expiring'">
        <div style="text-align:center"><div style="font-size:32px;font-weight:700;color:#e6a23c">{{ expiringSubs.length }}</div><div style="color:#909399;margin-top:4px">即将到期订阅</div></div>
      </el-card></el-col>
      <el-col :span="6"><el-card shadow="hover" style="cursor:pointer" @click="activeTab='expired'">
        <div style="text-align:center"><div style="font-size:32px;font-weight:700;color:#f56c6c">{{ expiredSubs.length }}</div><div style="color:#909399;margin-top:4px">已到期订阅</div></div>
      </el-card></el-col>
      <el-col :span="3"><el-card shadow="hover" style="cursor:pointer" @click="activeTab='expiringTrials'">
        <div style="text-align:center"><div style="font-size:32px;font-weight:700;color:#409eff">{{ expiringTrials.length }}</div><div style="color:#909399;margin-top:4px">活跃试用</div></div>
      </el-card></el-col>
      <el-col :span="3"><el-card shadow="hover" style="cursor:pointer" @click="activeTab='expiredTrials'">
        <div style="text-align:center"><div style="font-size:32px;font-weight:700;color:#909399">{{ expiredTrialsList.length }}</div><div style="color:#909399;margin-top:4px">已到期试用</div></div>
      </el-card></el-col>
      <el-col :span="6"><el-card shadow="hover" style="cursor:pointer" @click="activeTab='customer'">
        <div style="text-align:center"><el-icon :size="32" color="#67c23a"><Search /></el-icon><div style="color:#909399;margin-top:4px">客户资产查询</div></div>
      </el-card></el-col>
    </el-row>

    <el-card shadow="never" v-if="activeTab==='expiring'">
      <template #header><span style="font-weight:600">即将到期的订阅 (15天内)</span></template>
      <el-table :data="expiringSubs" border stripe>
        <el-table-column prop="subscriptionNo" label="订阅编号" width="180" />
        <el-table-column prop="customerId" label="客户ID" width="100" />
        <el-table-column prop="sourceName" label="产品/套餐" width="160" />
        <el-table-column prop="endDate" label="到期日" width="100" />
        <el-table-column prop="daysRemaining" label="剩余天数" width="90">
          <template #default="{row}"><span :style="{color:row.daysRemaining<=7?'#f56c6c':'#e6a23c',fontWeight:700}">{{ row.daysRemaining }}天</span></template>
        </el-table-column>
        <el-table-column prop="renewalPriceDisplay" label="续费价格" width="100" />
        <el-table-column prop="pricingModel" label="收费模式" width="100" />
      </el-table>
    </el-card>

    <el-card shadow="never" v-if="activeTab==='expired'">
      <template #header><span style="font-weight:600">已到期的订阅</span></template>
      <el-table :data="expiredSubs" border stripe>
        <el-table-column prop="subscriptionNo" label="订阅编号" width="180" />
        <el-table-column prop="customerId" label="客户ID" width="100" />
        <el-table-column prop="sourceName" label="产品/套餐" width="160" />
        <el-table-column prop="endDate" label="到期日" width="100" />
        <el-table-column prop="pricingModel" label="收费模式" width="100" />
        <el-table-column prop="status" label="状态" width="80"><template #default="{row}"><el-tag type="danger" size="small">已过期</el-tag></template></el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" v-if="activeTab==='expiringTrials'">
      <template #header><span style="font-weight:600">活跃试用</span></template>
      <el-table :data="expiringTrials" border stripe>
        <el-table-column prop="customerId" label="客户ID" width="100" />
        <el-table-column prop="sourceName" label="产品/套餐" width="160" />
        <el-table-column prop="trialDays" label="试用天数" width="80" />
        <el-table-column prop="endDate" label="到期日" width="100" />
        <el-table-column prop="extendCount" label="延长次数" width="80" />
        <el-table-column prop="totalExtendDays" label="累计延长" width="80" />
        <el-table-column prop="status" label="状态" width="70"><template #default><el-tag type="warning" size="small">试用中</el-tag></template></el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" v-if="activeTab==='expiredTrials'">
      <template #header><span style="font-weight:600">已到期的试用</span></template>
      <el-table :data="expiredTrialsList" border stripe>
        <el-table-column prop="customerId" label="客户ID" width="100" />
        <el-table-column prop="sourceName" label="产品/套餐" width="160" />
        <el-table-column prop="trialDays" label="试用天数" width="80" />
        <el-table-column prop="endDate" label="到期日" width="100" />
        <el-table-column prop="totalExtendDays" label="累计延长" width="80" />
        <el-table-column prop="status" label="状态" width="70"><template #default><el-tag type="info" size="small">已到期</el-tag></template></el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" v-if="activeTab==='customer'">
      <template #header><span style="font-weight:600">客户资产全景</span></template>
      <div class="filter-bar" style="margin-bottom:16px">
        <el-input v-model="searchCustomerId" placeholder="输入客户ID" style="width:200px" />
        <el-button type="primary" @click="searchCustomer">查询</el-button>
      </div>
      <el-table :data="customerAssets" border stripe v-if="customerAssets.length">
        <el-table-column prop="sourceName" label="产品/套餐" width="160" />
        <el-table-column prop="pricingModel" label="收费模式" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{row}"><el-tag :type="{ACTIVE:'success',TRIAL:'warning',EXPIRED:'danger',SUSPENDED:'info'}[row.status]" size="small">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="关键指标" min-width="200">
          <template #default="{row}">
            <span v-if="row.totalDays>0">时间: {{ row.daysUsed }}/{{ row.totalDays }}天, 剩余{{ row.daysRemaining }}天</span>
            <span v-if="row.usageQuota>0"> | 用量: {{ row.usageUsed }}/{{ row.usageQuota }} {{ row.usageUnit }}</span>
            <span v-if="row.spaceTotal>0"> | 空间: {{ (row.spaceUsed/1073741824).toFixed(1) }}GB/{{ (row.spaceTotal/1073741824).toFixed(1) }}GB</span>
            <span v-if="row.pricingModel==='ONE_TIME'">永久有效</span>
          </template>
        </el-table-column>
        <el-table-column prop="endDate" label="到期日" width="100" />
        <el-table-column prop="renewalPriceDisplay" label="续费价格" width="100" />
      </el-table>
    </el-card>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { billingReportApi } from '@/api'
const activeTab = ref('expiring')
const expiringSubs = ref([]), expiredSubs = ref([]), expiringTrials = ref([]), expiredTrialsList = ref([]), customerAssets = ref([])
const searchCustomerId = ref('')

async function loadReports() {
  try {
    const [r1, r2, r3, r4] = await Promise.all([
      billingReportApi.expiringSubscriptions(),
      billingReportApi.expiredSubscriptions(),
      billingReportApi.expiringTrials({ page:1, pageSize:100 }),
      billingReportApi.expiredTrials({ page:1, pageSize:100 })
    ])
    expiringSubs.value = r1.data || []
    expiredSubs.value = r2.data || []
    expiringTrials.value = r3.data?.items || []
    expiredTrialsList.value = r4.data?.items || []
  } catch {}
}

async function searchCustomer() {
  if (!searchCustomerId.value) return
  try {
    const r = await billingReportApi.customerAssets(searchCustomerId.value, { page:1, pageSize:100 })
    customerAssets.value = r.data?.items || []
  } catch { customerAssets.value = [] }
}
onMounted(loadReports)
</script>
