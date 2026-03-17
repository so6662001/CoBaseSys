<template>
  <CrudTable title="积分流水" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    :showCreate="false" :showActions="false"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <el-table-column prop="transactionNo" label="流水号" width="200" show-overflow-tooltip />
    <el-table-column prop="direction" label="方向" width="70">
      <template #default="{ row }">
        <el-tag :type="row.direction===1?'success':row.direction===-1?'danger':'info'" size="small">{{ row.directionText || (row.direction===1?'收入':'支出') }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="sourceTypeText" label="来源" width="90">
      <template #default="{ row }"><el-tag :type="row.sourceType?.startsWith('GIFT')?'warning':''" size="small">{{ row.sourceTypeText || '赚取' }}</el-tag></template>
    </el-table-column>
    <el-table-column prop="points" label="积分" width="100">
      <template #default="{ row }"><span style="font-weight:600">{{ row.points }}</span></template>
    </el-table-column>
    <el-table-column prop="balanceAfter" label="变动后余额" width="110" />
    <el-table-column prop="bizOrderNo" label="业务单号" width="160" show-overflow-tooltip />
    <el-table-column prop="remark" label="备注" show-overflow-tooltip />
    <el-table-column prop="createdAt" label="时间" width="170" />
  </CrudTable>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { pointApi } from '@/api'
import CrudTable from '@/components/CrudTable.vue'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)

async function fetchData() {
  loading.value = true
  try {
    const res = await pointApi.listTransactions({ page: page.value, pageSize: pageSize.value })
    list.value = res.data.items; total.value = res.data.total
  } finally { loading.value = false }
}
onMounted(fetchData)
</script>
