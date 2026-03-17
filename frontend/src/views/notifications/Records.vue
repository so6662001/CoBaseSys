<template>
  <CrudTable title="通知记录" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    :showCreate="false" :showActions="false"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <template #filter>
      <div class="filter-bar">
        <el-input v-model="filterUserId" placeholder="用户ID" style="width:160px" clearable @clear="fetchData" />
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
    </template>
    <el-table-column prop="id" label="ID" width="70" />
    <el-table-column prop="userId" label="用户ID" width="120" />
    <el-table-column prop="channel" label="渠道" width="80">
      <template #default="{ row }">
        <el-tag size="small" type="info">{{ { email:'邮件', sms:'短信', in_app:'应用内' }[row.channel] || row.channel }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="subject" label="标题" width="160" show-overflow-tooltip />
    <el-table-column prop="content" label="内容" show-overflow-tooltip />
    <el-table-column prop="statusText" label="状态" width="90">
      <template #default="{ row }">
        <el-tag :type="row.status===1?'success':row.status===2?'danger':'info'" size="small">{{ row.statusText }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="sentAt" label="发送时间" width="170" />
    <el-table-column prop="createdAt" label="创建时间" width="170" />
  </CrudTable>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { notificationApi } from '@/api'
import CrudTable from '@/components/CrudTable.vue'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const filterUserId = ref('')

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (filterUserId.value) params.userId = filterUserId.value
    const res = await notificationApi.listRecords(params)
    list.value = res.data.items; total.value = res.data.total
  } finally { loading.value = false }
}
onMounted(fetchData)
</script>
