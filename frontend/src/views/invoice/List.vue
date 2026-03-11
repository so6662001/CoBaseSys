<template>
  <el-card shadow="never">
    <div class="page-header"><h2>发票记录</h2></div>

    <el-row :gutter="16" style="margin-bottom:20px" v-if="stats">
      <el-col :span="6"><el-card shadow="hover"><div style="text-align:center"><div style="font-size:28px;font-weight:700;color:#67c23a">{{ stats.totalIssued }}</div><div style="color:#909399">已开具</div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div style="text-align:center"><div style="font-size:28px;font-weight:700;color:#e6a23c">{{ stats.totalPending }}</div><div style="color:#909399">待审核</div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div style="text-align:center"><div style="font-size:28px;font-weight:700;color:#f56c6c">{{ stats.totalRejected }}</div><div style="color:#909399">已驳回</div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div style="text-align:center"><div style="font-size:20px;font-weight:700;color:#409eff">{{ stats.totalIssuedAmountDisplay || '0.00元' }}</div><div style="color:#909399">累计开票金额</div></div></el-card></el-col>
    </el-row>

    <el-table :data="list" v-loading="loading" stripe border>
      <el-table-column prop="applicationNo" label="申请编号" width="180" />
      <el-table-column prop="customerName" label="客户" width="120" />
      <el-table-column prop="invoiceTypeText" label="类型" width="120" />
      <el-table-column prop="titleName" label="抬头" width="150" show-overflow-tooltip />
      <el-table-column prop="totalAmountDisplay" label="金额" width="100" />
      <el-table-column prop="statusText" label="状态" width="80">
        <template #default="{row}"><el-tag :type="{PENDING:'warning',ISSUED:'success',REJECTED:'danger',VOIDED:'info'}[row.status]" size="small">{{ row.statusText }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="invoiceNumber" label="发票号" width="120" />
      <el-table-column prop="invoiceDate" label="开票日期" width="100" />
      <el-table-column label="PDF" width="80">
        <template #default="{row}"><el-link v-if="row.pdfUrl" :href="row.pdfUrl" target="_blank" type="primary">下载</el-link></template>
      </el-table-column>
      <el-table-column prop="createdAt" label="申请时间" width="160" />
    </el-table>
    <div style="display:flex;justify-content:flex-end;margin-top:16px" v-if="total>0">
      <el-pagination v-model:current-page="page" :total="total" :page-size="pageSize" layout="total, prev, pager, next" @current-change="fetchData" />
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { invoiceApi } from '@/api'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20), stats = ref(null)

async function fetchData() {
  loading.value = true
  try { const r = await invoiceApi.list({ page:page.value, pageSize:pageSize.value }); list.value = r.data.items; total.value = r.data.total } finally { loading.value = false }
}

async function loadStats() { try { const r = await invoiceApi.statistics(); stats.value = r.data } catch {} }

onMounted(() => { fetchData(); loadStats() })
</script>
