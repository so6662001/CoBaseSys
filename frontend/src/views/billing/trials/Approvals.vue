<template>
  <el-card shadow="never">
    <div class="page-header">
      <h2>延长审批</h2>
      <el-radio-group v-model="filterStatus" @change="fetchData">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="PENDING">待审批</el-radio-button>
        <el-radio-button value="APPROVED">已通过</el-radio-button>
        <el-radio-button value="REJECTED">已驳回</el-radio-button>
      </el-radio-group>
    </div>
    <el-table :data="list" v-loading="loading" stripe border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="customerName" label="客户" width="130" />
      <el-table-column prop="sourceName" label="产品/套餐" width="160" />
      <el-table-column prop="extendDays" label="申请延长天数" width="110" />
      <el-table-column prop="applyReason" label="申请原因" show-overflow-tooltip />
      <el-table-column prop="applicantName" label="申请人" width="90" />
      <el-table-column prop="applyTime" label="申请时间" width="160" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{row}"><el-tag :type="{PENDING:'warning',APPROVED:'success',REJECTED:'danger'}[row.status]" size="small">{{ {PENDING:'待审批',APPROVED:'已通过',REJECTED:'已驳回'}[row.status] }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="approverName" label="审批人" width="90" />
      <el-table-column prop="approveRemark" label="审批意见" width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right" v-if="filterStatus===''||filterStatus==='PENDING'">
        <template #default="{row}">
          <template v-if="row.status==='PENDING'">
            <el-button link type="success" @click="handleApprove(row)">通过</el-button>
            <el-button link type="danger" @click="handleReject(row)">驳回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
    <div style="display:flex;justify-content:flex-end;margin-top:16px" v-if="total>0">
      <el-pagination v-model:current-page="page" :total="total" :page-size="pageSize" layout="total, prev, pager, next" @current-change="fetchData" />
    </div>
  </el-card>

  <el-dialog v-model="approveVisible" :title="approveAction==='approve'?'审批通过':'审批驳回'" width="400px">
    <el-form label-width="80px">
      <el-form-item label="审批人ID"><el-input v-model="approveForm.approverId" /></el-form-item>
      <el-form-item label="审批人"><el-input v-model="approveForm.approverName" /></el-form-item>
      <el-form-item label="审批意见"><el-input v-model="approveForm.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="approveVisible=false">取消</el-button>
      <el-button :type="approveAction==='approve'?'success':'danger'" @click="doApprove">确定</el-button>
    </template>
  </el-dialog>
</template>
<script setup>
import { ref, onMounted, reactive } from 'vue'
import { billingTrialApi } from '@/api'
import { ElMessage } from 'element-plus'
const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20), filterStatus = ref('PENDING')
const approveVisible = ref(false), approveAction = ref('approve'), approveId = ref(null)
const approveForm = reactive({ approverId:'admin', approverName:'管理员', remark:'' })

async function fetchData() {
  loading.value = true
  try { const params = { page:page.value, pageSize:pageSize.value }; if(filterStatus.value) params.status=filterStatus.value; const r=await billingTrialApi.listApprovals(params); list.value=r.data.items; total.value=r.data.total } finally { loading.value=false }
}
function handleApprove(row) { approveAction.value='approve'; approveId.value=row.id; approveForm.remark=''; approveVisible.value=true }
function handleReject(row) { approveAction.value='reject'; approveId.value=row.id; approveForm.remark=''; approveVisible.value=true }
async function doApprove() {
  const params = { approverId:approveForm.approverId, approverName:approveForm.approverName, remark:approveForm.remark }
  if (approveAction.value==='approve') await billingTrialApi.approve(approveId.value, params)
  else await billingTrialApi.reject(approveId.value, params)
  ElMessage.success(approveAction.value==='approve'?'审批通过':'已驳回'); approveVisible.value=false; fetchData()
}
onMounted(fetchData)
</script>
