<template>
  <CrudTable title="钱包账户" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    :showCreate="false" :showActions="false"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <el-table-column prop="userId" label="用户ID" width="140" />
    <el-table-column prop="balanceDisplay" label="可用余额" width="120">
      <template #default="{ row }"><span style="font-weight:600;color:#67c23a">{{ row.balanceDisplay }}</span></template>
    </el-table-column>
    <el-table-column prop="frozen" label="冻结(分)" width="100" />
    <el-table-column prop="totalRecharged" label="累计充值(分)" width="120" />
    <el-table-column prop="totalConsumed" label="累计消费(分)" width="120" />
  </CrudTable>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { walletApi } from '@/api'
import CrudTable from '@/components/CrudTable.vue'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)

async function fetchData() {
  loading.value = true
  try {
    const res = await walletApi.listAccounts({ page: page.value, pageSize: pageSize.value })
    list.value = res.data.items; total.value = res.data.total
  } finally { loading.value = false }
}
onMounted(fetchData)
</script>
