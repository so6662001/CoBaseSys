<template>
  <el-card shadow="never">
    <div class="page-header"><h2>客户资产详情: {{ $route.params.customerId }}</h2></div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="subscriptionNo" label="订阅编号" width="180" />
      <el-table-column prop="sourceName" label="产品/套餐" width="160" />
      <el-table-column prop="pricingModel" label="收费模式" width="100" />
      <el-table-column prop="quantity" label="数量" width="60" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{row}"><el-tag :type="{ACTIVE:'success',TRIAL:'warning',EXPIRED:'danger'}[row.status]" size="small">{{ row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column label="使用情况" min-width="200">
        <template #default="{row}">
          <div v-if="row.totalDays>0">{{ row.daysUsed }}天 / {{ row.totalDays }}天 (剩余{{ row.daysRemaining }}天)</div>
          <div v-if="row.usageQuota>0">{{ row.usageUsed }} / {{ row.usageQuota }} {{ row.usageUnit }}</div>
          <div v-if="row.spaceTotal>0">{{ (row.spaceUsed/1073741824).toFixed(1) }}GB / {{ (row.spaceTotal/1073741824).toFixed(1) }}GB</div>
        </template>
      </el-table-column>
      <el-table-column prop="startDate" label="开始日期" width="100" />
      <el-table-column prop="endDate" label="到期日期" width="100" />
      <el-table-column prop="renewalPriceDisplay" label="续费价格" width="100" />
    </el-table>
  </el-card>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { billingReportApi } from '@/api'
const route = useRoute()
const list = ref([]), loading = ref(false)
async function fetchData() {
  loading.value = true
  try { const r = await billingReportApi.customerAssets(route.params.customerId, { page:1, pageSize:100 }); list.value = r.data?.items || [] } finally { loading.value = false }
}
onMounted(fetchData)
</script>
