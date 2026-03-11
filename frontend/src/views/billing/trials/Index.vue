<template>
  <CrudTable title="试用管理" :data="list" :loading="loading" :total="total" :page="page" :pageSize="pageSize"
    :showCreate="false" :showEdit="false" :showDelete="false" :actionWidth="120"
    @page-change="page=$event;fetchData()" @size-change="pageSize=$event;fetchData()">
    <template #filter>
      <div class="filter-bar">
        <el-select v-model="filterStatus" placeholder="状态" style="width:120px" clearable>
          <el-option value="ACTIVE" label="试用中" /><el-option value="EXPIRED" label="已到期" /><el-option value="CONVERTED" label="已转正" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
    </template>
    <el-table-column prop="id" label="ID" width="60" />
    <el-table-column prop="customerId" label="客户ID" width="100" />
    <el-table-column prop="sourceName" label="产品/套餐" width="160" />
    <el-table-column prop="trialDays" label="试用天数" width="80" />
    <el-table-column prop="startDate" label="开始日期" width="100" />
    <el-table-column prop="endDate" label="到期日期" width="100" />
    <el-table-column prop="status" label="状态" width="80">
      <template #default="{row}"><el-tag :type="{ACTIVE:'success',EXPIRED:'danger',CONVERTED:''}[row.status]" size="small">{{ {ACTIVE:'试用中',EXPIRED:'已到期',CONVERTED:'已转正'}[row.status] }}</el-tag></template>
    </el-table-column>
    <el-table-column prop="extendCount" label="延长次数" width="80" />
    <el-table-column prop="totalExtendDays" label="累计延长天" width="90" />
    <el-table-column prop="createdAt" label="创建时间" width="160" />
    <el-table-column label="操作" width="120" fixed="right">
      <template #default="{row}">
        <el-button v-if="row.status==='ACTIVE'" link type="primary" @click="openExtend(row)">申请延长</el-button>
      </template>
    </el-table-column>
  </CrudTable>

  <el-dialog v-model="extendVisible" title="申请延长试用" width="450px">
    <el-form :model="extendForm" label-width="100px">
      <el-form-item label="延长天数"><el-input-number v-model="extendForm.extendDays" :min="1" :max="90" /></el-form-item>
      <el-form-item label="申请原因"><el-input v-model="extendForm.applyReason" type="textarea" :rows="3" /></el-form-item>
      <el-form-item label="申请人ID"><el-input v-model="extendForm.applicantId" /></el-form-item>
      <el-form-item label="申请人姓名"><el-input v-model="extendForm.applicantName" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="extendVisible=false">取消</el-button><el-button type="primary" @click="submitExtend" :loading="extendSubmitting">提交申请</el-button></template>
  </el-dialog>
</template>
<script setup>
import { ref, onMounted, reactive } from 'vue'
import { billingTrialApi } from '@/api'
import { ElMessage } from 'element-plus'
import CrudTable from '@/components/CrudTable.vue'
const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20), filterStatus = ref('')
const extendVisible = ref(false), extendSubmitting = ref(false), extendTrialId = ref(null)
const extendForm = reactive({ extendDays: 7, applyReason: '', applicantId: 'admin', applicantName: '管理员' })
async function fetchData() {
  loading.value = true
  try { const params = { page:page.value, pageSize:pageSize.value }; if(filterStatus.value) params.status=filterStatus.value; const r=await billingTrialApi.listTrials(params); list.value=r.data.items; total.value=r.data.total } finally { loading.value=false }
}
function openExtend(row) { extendTrialId.value = row.id; extendForm.applyReason = ''; extendVisible.value = true }
async function submitExtend() {
  extendSubmitting.value = true
  try { await billingTrialApi.submitExtend(extendTrialId.value, extendForm); ElMessage.success('延长申请已提交，等待审批'); extendVisible.value = false } finally { extendSubmitting.value = false }
}
onMounted(fetchData)
</script>
