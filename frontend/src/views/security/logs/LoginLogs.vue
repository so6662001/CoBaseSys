<template>
  <CrudTable title="登录日志" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    :showCreate="false" :showActions="false"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <template #filter>
      <div class="filter-bar">
        <el-input v-model="filterUserId" placeholder="用户ID" style="width:120px" clearable />
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
    </template>
    <el-table-column prop="id" label="ID" width="70" />
    <el-table-column prop="username" label="用户名" width="120" />
    <el-table-column prop="loginIp" label="登录IP" width="140" />
    <el-table-column prop="status" label="结果" width="80">
      <template #default="{row}"><el-tag :type="row.status==='SUCCESS'?'success':'danger'" size="small">{{ row.status==='SUCCESS'?'成功':'失败' }}</el-tag></template>
    </el-table-column>
    <el-table-column prop="failReason" label="失败原因" show-overflow-tooltip />
    <el-table-column prop="userAgent" label="设备" show-overflow-tooltip />
    <el-table-column prop="createdAt" label="时间" width="170" />
  </CrudTable>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { securityUserApi } from '@/api'
import CrudTable from '@/components/CrudTable.vue'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)
const filterUserId = ref('')

async function fetchData() {
  loading.value = true
  try {
    const params = { page:page.value, pageSize:pageSize.value }
    if (filterUserId.value) params.userId = filterUserId.value
    const r = await securityUserApi.loginLogs(params)
    list.value = r.data.items; total.value = r.data.total
  } finally { loading.value = false }
}
onMounted(fetchData)
</script>
