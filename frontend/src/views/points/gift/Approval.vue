<template>
  <el-card shadow="never">
    <div class="page-header">
      <h2>赠送审批</h2>
      <el-radio-group v-model="filterStatus" @change="fetchData">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="PENDING">待审批</el-radio-button>
        <el-radio-button value="APPROVED">已通过</el-radio-button>
        <el-radio-button value="REJECTED">已驳回</el-radio-button>
      </el-radio-group>
    </div>
    <el-table :data="list" v-loading="loading" stripe border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="customerId" label="客户ID" width="100" />
      <el-table-column prop="customerName" label="客户名称" width="120" />
      <el-table-column prop="points" label="赠送积分" width="100">
        <template #default="{row}"><span style="font-weight:700;color:#409eff">{{ row.points }}</span></template>
      </el-table-column>
      <el-table-column prop="sourceType" label="类型" width="90">
        <template #default="{row}"><el-tag size="small">{{ {GIFT_MANUAL:'人工赠送',GIFT_ORDER:'订单赠送',GIFT_ACTIVITY:'活动赠送'}[row.sourceType]||row.sourceType }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="giftReason" label="赠送原因" show-overflow-tooltip />
      <el-table-column prop="applicantName" label="申请人" width="90" />
      <el-table-column prop="applyTime" label="申请时间" width="160" />
      <el-table-column prop="statusText" label="状态" width="80">
        <template #default="{row}"><el-tag :type="{PENDING:'warning',APPROVED:'success',REJECTED:'danger'}[row.status]" size="small">{{ row.statusText }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="approverName" label="审批人" width="80" />
      <el-table-column prop="approveRemark" label="审批意见" width="120" show-overflow-tooltip />
      <el-table-column prop="transactionNo" label="流水号" width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{row}">
          <template v-if="row.status==='PENDING'">
            <el-button link type="success" @click="openApprove(row,'approve')">通过</el-button>
            <el-button link type="danger" @click="openApprove(row,'reject')">驳回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
    <div style="display:flex;justify-content:flex-end;margin-top:16px" v-if="total>0">
      <el-pagination v-model:current-page="page" :total="total" :page-size="pageSize" layout="total, prev, pager, next" @current-change="fetchData" />
    </div>
  </el-card>

  <el-dialog v-model="approveVisible" :title="approveAction==='approve'?'审批通过':'审批驳回'" width="420px">
    <el-descriptions :column="1" border style="margin-bottom:16px">
      <el-descriptions-item label="客户">{{ currentRow?.customerName }} ({{ currentRow?.customerId }})</el-descriptions-item>
      <el-descriptions-item label="赠送积分"><span style="font-weight:700;color:#409eff">{{ currentRow?.points }}</span></el-descriptions-item>
      <el-descriptions-item label="原因">{{ currentRow?.giftReason }}</el-descriptions-item>
    </el-descriptions>
    <el-form label-width="80px">
      <el-form-item label="审批意见"><el-input v-model="approveForm.remark" type="textarea" :rows="2" :placeholder="approveAction==='reject'?'请填写驳回原因':'选填'" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="approveVisible=false">取消</el-button>
      <el-button :type="approveAction==='approve'?'success':'danger'" @click="doApprove">{{ approveAction==='approve'?'确认通过':'确认驳回' }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { pointApi } from '@/api'
import { useAppStore } from '@/stores/app'
import { ElMessage } from 'element-plus'

const app = useAppStore()
const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20), filterStatus = ref('PENDING')
const approveVisible = ref(false), approveAction = ref('approve'), currentRow = ref(null)
const approveForm = reactive({ remark: '' })

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (filterStatus.value) params.status = filterStatus.value
    const r = await pointApi.giftList(params)
    list.value = r.data.items; total.value = r.data.total
  } finally { loading.value = false }
}

function openApprove(row, action) { currentRow.value = row; approveAction.value = action; approveForm.remark = ''; approveVisible.value = true }

async function doApprove() {
  if (approveAction.value === 'reject' && !approveForm.remark) { ElMessage.warning('驳回时必须填写原因'); return }
  const params = { approverId: app.userId, approverName: app.realName || app.username, remark: approveForm.remark }
  if (approveAction.value === 'approve') await pointApi.giftApprove(currentRow.value.id, params)
  else await pointApi.giftReject(currentRow.value.id, params)
  ElMessage.success(approveAction.value === 'approve' ? '审批通过，积分已赠送' : '已驳回')
  approveVisible.value = false; fetchData()
}

onMounted(fetchData)
</script>
