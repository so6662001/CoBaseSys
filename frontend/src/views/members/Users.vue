<template>
  <CrudTable title="会员用户" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    :showCreate="false" :showActions="false"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <el-table-column prop="userId" label="用户ID" width="140" />
    <el-table-column prop="levelName" label="当前等级" width="120">
      <template #default="{ row }">
        <el-tag type="warning">{{ row.levelName || '无等级' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="levelRank" label="等级排序" width="90" />
    <el-table-column prop="totalPointsEarned" label="累计积分" width="120" />
    <el-table-column prop="totalConsumption" label="累计消费(分)" width="120" />
    <el-table-column prop="pointMultiplier" label="积分倍率" width="90" />
    <el-table-column prop="discountRate" label="折扣率" width="80" />
    <el-table-column prop="levelUpdatedAt" label="升级时间" width="170" />
  </CrudTable>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { memberApi } from '@/api'
import CrudTable from '@/components/CrudTable.vue'

const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20)

async function fetchData() {
  loading.value = true
  try {
    const res = await memberApi.listUsers({ page: page.value, pageSize: pageSize.value })
    list.value = res.data.items; total.value = res.data.total
  } finally { loading.value = false }
}
onMounted(fetchData)
</script>
