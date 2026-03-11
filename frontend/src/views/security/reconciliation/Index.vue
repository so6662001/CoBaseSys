<template>
  <CrudTable title="对账报告" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    :showCreate="false" :showActions="false"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <template #filter>
      <div class="filter-bar">
        <el-select v-model="filterStatus" placeholder="状态" style="width:120px" clearable>
          <el-option value="PASS" label="通过" /><el-option value="FAIL" label="异常" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
    </template>
    <el-table-column prop="id" label="ID" width="60" />
    <el-table-column prop="reportDate" label="对账日期" width="100" />
    <el-table-column prop="checkType" label="检查类型" width="170">
      <template #default="{row}"><el-tag size="small">{{ checkTypeLabel(row.checkType) }}</el-tag></template>
    </el-table-column>
    <el-table-column prop="targetTable" label="目标表" width="200" show-overflow-tooltip />
    <el-table-column prop="totalRecords" label="总记录" width="80" />
    <el-table-column prop="passCount" label="通过" width="70" />
    <el-table-column prop="failCount" label="失败" width="70">
      <template #default="{row}"><span :style="{color:row.failCount>0?'#f56c6c':'#67c23a',fontWeight:700}">{{ row.failCount }}</span></template>
    </el-table-column>
    <el-table-column prop="status" label="结果" width="70">
      <template #default="{row}"><el-tag :type="row.status==='PASS'?'success':'danger'" size="small">{{ row.status==='PASS'?'通过':'异常' }}</el-tag></template>
    </el-table-column>
    <el-table-column prop="durationMs" label="耗时(ms)" width="90" />
    <el-table-column prop="executedAt" label="执行时间" width="170" />
    <el-table-column label="操作" width="60" fixed="right">
      <template #default="{row}">
        <el-button v-if="row.failCount>0" link type="danger" @click="showFail(row)">详情</el-button>
      </template>
    </el-table-column>
  </CrudTable>

  <el-dialog v-model="failVisible" title="异常详情" width="600px">
    <el-alert type="error" :closable="false" style="margin-bottom:16px">
      对账发现 {{ failRow?.failCount }} 条异常记录
    </el-alert>
    <el-input type="textarea" :model-value="formatJson(failRow?.failDetails)" :rows="10" readonly />
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { auditApi } from '@/api'
import CrudTable from '@/components/CrudTable.vue'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const filterStatus = ref('')
const failVisible = ref(false), failRow = ref(null)

const checkTypeLabel = (t) => ({
  INCREMENTAL_HASH_CHAIN: '增量哈希链校验',
  FULL_HASH_CHAIN: '全量哈希链校验',
  BALANCE_DIGEST: '余额摘要校验',
  BALANCE_SUM: '余额汇总校验',
  ORDER_AMOUNT: '订单金额校验',
})[t] || t

async function fetchData() {
  loading.value = true
  try {
    const params = { page:page.value, pageSize:pageSize.value }
    if (filterStatus.value) params.status = filterStatus.value
    const r = await auditApi.listReconciliationReports(params)
    list.value = r.data.items; total.value = r.data.total
  } finally { loading.value = false }
}

function showFail(row) { failRow.value = row; failVisible.value = true }
function formatJson(str) { try { return JSON.stringify(JSON.parse(str),null,2) } catch { return str||'' } }
onMounted(fetchData)
</script>
