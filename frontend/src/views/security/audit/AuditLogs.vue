<template>
  <CrudTable title="审计日志" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    :showCreate="false" :showActions="false"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <template #filter>
      <div class="filter-bar">
        <el-select v-model="filterModule" placeholder="模块" style="width:120px" clearable>
          <el-option value="system" label="系统" /><el-option value="points" label="积分" />
          <el-option value="wallet" label="钱包" /><el-option value="billing" label="计费" />
          <el-option value="member" label="会员" /><el-option value="security" label="安全" />
        </el-select>
        <el-input v-model="filterOperatorId" placeholder="操作人ID" style="width:120px" clearable />
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
    </template>
    <el-table-column prop="id" label="ID" width="70" />
    <el-table-column prop="module" label="模块" width="70">
      <template #default="{row}"><el-tag size="small">{{ row.module }}</el-tag></template>
    </el-table-column>
    <el-table-column prop="action" label="操作" width="130" />
    <el-table-column prop="operatorName" label="操作人" width="90" />
    <el-table-column prop="operatorIp" label="IP" width="130" />
    <el-table-column prop="description" label="描述" show-overflow-tooltip />
    <el-table-column prop="targetType" label="目标类型" width="100" />
    <el-table-column prop="targetId" label="目标ID" width="80" />
    <el-table-column prop="status" label="结果" width="60">
      <template #default="{row}"><el-tag :type="row.status==='SUCCESS'?'success':'danger'" size="small">{{ row.status==='SUCCESS'?'成功':'失败' }}</el-tag></template>
    </el-table-column>
    <el-table-column prop="createdAt" label="时间" width="170" />
    <el-table-column label="操作" width="60" fixed="right">
      <template #default="{row}"><el-button link type="primary" @click="showDetail(row)">详情</el-button></template>
    </el-table-column>
  </CrudTable>

  <el-dialog v-model="detailVisible" title="审计详情" width="700px">
    <el-descriptions :column="2" border v-if="detailRow">
      <el-descriptions-item label="操作人">{{ detailRow.operatorName }} ({{ detailRow.operatorId }})</el-descriptions-item>
      <el-descriptions-item label="IP">{{ detailRow.operatorIp }}</el-descriptions-item>
      <el-descriptions-item label="模块">{{ detailRow.module }}</el-descriptions-item>
      <el-descriptions-item label="操作">{{ detailRow.action }}</el-descriptions-item>
      <el-descriptions-item label="目标">{{ detailRow.targetType }} #{{ detailRow.targetId }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ detailRow.status }}</el-descriptions-item>
      <el-descriptions-item label="追踪ID" :span="2">{{ detailRow.traceId }}</el-descriptions-item>
      <el-descriptions-item label="描述" :span="2">{{ detailRow.description }}</el-descriptions-item>
    </el-descriptions>
    <div style="margin-top:16px" v-if="detailRow?.beforeData">
      <div style="font-weight:600;margin-bottom:4px">操作前数据:</div>
      <el-input type="textarea" :model-value="formatJson(detailRow.beforeData)" :rows="5" readonly />
    </div>
    <div style="margin-top:12px" v-if="detailRow?.afterData">
      <div style="font-weight:600;margin-bottom:4px">操作后数据:</div>
      <el-input type="textarea" :model-value="formatJson(detailRow.afterData)" :rows="5" readonly />
    </div>
    <div style="margin-top:12px" v-if="detailRow?.errorMessage">
      <div style="font-weight:600;margin-bottom:4px;color:#f56c6c">错误信息:</div>
      <el-input type="textarea" :model-value="detailRow.errorMessage" :rows="2" readonly />
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { auditApi } from '@/api'
import CrudTable from '@/components/CrudTable.vue'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const filterModule = ref(''), filterOperatorId = ref('')
const detailVisible = ref(false), detailRow = ref(null)

async function fetchData() {
  loading.value = true
  try {
    const params = { page:page.value, pageSize:pageSize.value }
    if (filterModule.value) params.module = filterModule.value
    if (filterOperatorId.value) params.operatorId = filterOperatorId.value
    const r = await auditApi.listAuditLogs(params)
    list.value = r.data.items; total.value = r.data.total
  } finally { loading.value = false }
}

function showDetail(row) { detailRow.value = row; detailVisible.value = true }
function formatJson(str) { try { return JSON.stringify(JSON.parse(str),null,2) } catch { return str||'' } }
onMounted(fetchData)
</script>
