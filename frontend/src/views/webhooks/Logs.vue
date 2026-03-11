<template>
  <CrudTable title="Webhook 推送日志" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    :showCreate="false" :showActions="false"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <template #filter>
      <div class="filter-bar">
        <el-input v-model="filterConfigId" placeholder="Webhook配置ID" style="width:160px" clearable @clear="fetchData" />
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
    </template>
    <el-table-column prop="id" label="ID" width="70" />
    <el-table-column prop="configId" label="配置ID" width="80" />
    <el-table-column prop="eventType" label="事件类型" width="140" />
    <el-table-column prop="statusText" label="状态" width="80">
      <template #default="{ row }">
        <el-tag :type="row.status===1?'success':row.status===2?'danger':'info'" size="small">{{ row.statusText }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="responseStatus" label="HTTP状态" width="90" />
    <el-table-column prop="retryCount" label="重试次数" width="80" />
    <el-table-column prop="errorMessage" label="错误信息" show-overflow-tooltip />
    <el-table-column prop="createdAt" label="时间" width="170" />
    <el-table-column label="操作" width="80" fixed="right">
      <template #default="{ row }">
        <el-button link type="primary" @click="showPayload(row)">详情</el-button>
      </template>
    </el-table-column>
  </CrudTable>

  <el-dialog v-model="detailVisible" title="推送详情" width="600px">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="事件类型">{{ detailRow.eventType }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ detailRow.statusText }}</el-descriptions-item>
      <el-descriptions-item label="HTTP状态码">{{ detailRow.responseStatus }}</el-descriptions-item>
      <el-descriptions-item label="重试次数">{{ detailRow.retryCount }}</el-descriptions-item>
    </el-descriptions>
    <div style="margin-top:16px">
      <div style="font-weight:600;margin-bottom:8px">Payload:</div>
      <el-input type="textarea" :model-value="formatJson(detailRow.payload)" :rows="8" readonly />
    </div>
    <div style="margin-top:12px" v-if="detailRow.errorMessage">
      <div style="font-weight:600;margin-bottom:8px;color:#f56c6c">错误信息:</div>
      <el-input type="textarea" :model-value="detailRow.errorMessage" :rows="3" readonly />
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { webhookApi } from '@/api'
import CrudTable from '@/components/CrudTable.vue'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const filterConfigId = ref('')
const detailVisible = ref(false), detailRow = reactive({})

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (filterConfigId.value) params.configId = filterConfigId.value
    const res = await webhookApi.listLogs(params)
    list.value = res.data.items; total.value = res.data.total
  } finally { loading.value = false }
}

function showPayload(row) {
  Object.assign(detailRow, row)
  detailVisible.value = true
}

function formatJson(str) {
  try { return JSON.stringify(JSON.parse(str), null, 2) } catch { return str || '' }
}

onMounted(fetchData)
</script>
