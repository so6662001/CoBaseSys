<template>
  <CrudTable title="钱包流水" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    :showCreate="false" :showActions="false"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <el-table-column prop="transactionNo" label="流水号" width="200" show-overflow-tooltip />
    <el-table-column prop="typeText" label="类型" width="80">
      <template #default="{ row }">
        <el-tag :type="{1:'success',2:'danger',3:'warning',4:'info',5:'info',6:''}[row.type]" size="small">{{ row.typeText }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="amountDisplay" label="金额" width="110">
      <template #default="{ row }"><span style="font-weight:600">{{ row.amountDisplay }}</span></template>
    </el-table-column>
    <el-table-column prop="balanceAfter" label="变动后余额(分)" width="130" />
    <el-table-column prop="bizOrderNo" label="业务单号" width="160" show-overflow-tooltip />
    <el-table-column prop="remark" label="备注" show-overflow-tooltip />
    <el-table-column prop="createdAt" label="时间" width="170" />
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
    const res = await walletApi.listTransactions({ page: page.value, pageSize: pageSize.value })
    list.value = res.data.items; total.value = res.data.total
  } finally { loading.value = false }
}
onMounted(fetchData)
</script>
