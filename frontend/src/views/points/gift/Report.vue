<template>
  <div>
    <h2 style="margin:0 0 20px">赠送积分报表</h2>

    <el-row :gutter="16" style="margin-bottom:20px" v-if="summary">
      <el-col :span="4"><el-card shadow="hover"><div style="text-align:center"><div style="font-size:28px;font-weight:700;color:#409eff">{{ summary.totalGiftedPoints }}</div><div style="color:#909399;margin-top:4px">累计赠送积分</div></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div style="text-align:center"><div style="font-size:28px;font-weight:700;color:#67c23a">{{ summary.totalApproved }}</div><div style="color:#909399;margin-top:4px">已通过申请</div></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div style="text-align:center"><div style="font-size:28px;font-weight:700;color:#e6a23c">{{ summary.totalPending }}</div><div style="color:#909399;margin-top:4px">待审批</div></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div style="text-align:center"><div style="font-size:28px;font-weight:700">{{ summary.manualGiftedPoints }}</div><div style="color:#909399;margin-top:4px">人工赠送</div></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div style="text-align:center"><div style="font-size:28px;font-weight:700">{{ summary.orderGiftedPoints }}</div><div style="color:#909399;margin-top:4px">订单赠送</div></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div style="text-align:center"><div style="font-size:28px;font-weight:700">{{ summary.activityGiftedPoints }}</div><div style="color:#909399;margin-top:4px">活动赠送</div></div></el-card></el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:600">赠送积分流水</span>
          <div>
            <el-select v-model="filterSourceType" placeholder="赠送类型" style="width:140px;margin-right:8px" @change="fetchTransactions">
              <el-option value="GIFT_MANUAL" label="人工赠送" />
              <el-option value="GIFT_ORDER" label="订单赠送" />
              <el-option value="GIFT_ACTIVITY" label="活动赠送" />
            </el-select>
          </div>
        </div>
      </template>
      <el-table :data="transactions" v-loading="txLoading" border stripe>
        <el-table-column prop="transactionNo" label="流水号" width="200" show-overflow-tooltip />
        <el-table-column prop="sourceTypeText" label="类型" width="90">
          <template #default="{row}"><el-tag size="small" type="warning">{{ row.sourceTypeText }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="points" label="积分" width="100">
          <template #default="{row}"><span style="font-weight:600;color:#409eff">+{{ row.points }}</span></template>
        </el-table-column>
        <el-table-column prop="balanceAfter" label="变动后余额" width="110" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="170" />
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:16px" v-if="txTotal>0">
        <el-pagination v-model:current-page="txPage" :total="txTotal" :page-size="20" layout="total, prev, pager, next" @current-change="fetchTransactions" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { pointApi } from '@/api'

const summary = ref(null)
const transactions = ref([]), txLoading = ref(false), txTotal = ref(0), txPage = ref(1)
const filterSourceType = ref('GIFT_MANUAL')

async function loadSummary() {
  try { const r = await pointApi.giftSummary(); summary.value = r.data } catch {}
}

async function fetchTransactions() {
  txLoading.value = true
  try {
    const r = await pointApi.giftTransactions({ sourceType: filterSourceType.value, page: txPage.value, pageSize: 20 })
    transactions.value = r.data.items; txTotal.value = r.data.total
  } finally { txLoading.value = false }
}

onMounted(() => { loadSummary(); fetchTransactions() })
</script>
