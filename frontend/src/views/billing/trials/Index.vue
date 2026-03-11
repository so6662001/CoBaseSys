<template>
  <CrudTable title="试用管理" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    :showCreate="false" :showActions="false"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <template #filter>
      <div class="filter-bar">
        <el-select v-model="filterStatus" placeholder="状态" style="width:120px" clearable>
          <el-option value="ACTIVE" label="试用中" /><el-option value="EXPIRED" label="已到期" /><el-option value="CONVERTED" label="已转正" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
    </template>
    <el-table-column prop="id" label="ID" width="60" />
    <el-table-column prop="customerId" label="客户ID" width="100" />
    <el-table-column prop="sourceName" label="产品/套餐" width="160" />
    <el-table-column prop="trialDays" label="试用天数" width="80" />
    <el-table-column prop="startDate" label="开始日期" width="100" />
    <el-table-column prop="endDate" label="到期日期" width="100" />
    <el-table-column prop="status" label="状态" width="80">
      <template #default="{row}"><el-tag :type="{ACTIVE:'success',EXPIRED:'danger',CONVERTED:''}[row.status]" size="small">{{ {ACTIVE:'试用中',EXPIRED:'已到期',CONVERTED:'已转正'}[row.status] }}</el-tag></template>
    </el-table-column>
    <el-table-column prop="extendCount" label="延长次数" width="80" />
    <el-table-column prop="totalExtendDays" label="累计延长天" width="90" />
    <el-table-column prop="createdAt" label="创建时间" width="160" />
  </CrudTable>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { billingTrialApi } from '@/api'
import CrudTable from '@/components/CrudTable.vue'
const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20), filterStatus = ref('')
async function fetchData() {
  loading.value = true
  try { const params = { page:page.value, pageSize:pageSize.value }; if(filterStatus.value) params.status=filterStatus.value; const r=await billingTrialApi.listTrials(params); list.value=r.data.items; total.value=r.data.total } finally { loading.value=false }
}
onMounted(fetchData)
</script>
