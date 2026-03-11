<template>
  <el-card shadow="never">
    <div class="page-header">
      <h2>开票审核</h2>
      <el-radio-group v-model="filterStatus" @change="fetchData">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="PENDING">待审核</el-radio-button>
        <el-radio-button value="ISSUED">已开具</el-radio-button>
        <el-radio-button value="REJECTED">已驳回</el-radio-button>
        <el-radio-button value="VOIDED">已红冲</el-radio-button>
      </el-radio-group>
    </div>
    <el-table :data="list" v-loading="loading" stripe border>
      <el-table-column prop="applicationNo" label="申请编号" width="180" show-overflow-tooltip />
      <el-table-column prop="customerName" label="客户" width="120" />
      <el-table-column prop="invoiceTypeText" label="发票类型" width="120" />
      <el-table-column prop="titleName" label="抬头" width="160" show-overflow-tooltip />
      <el-table-column prop="totalAmountDisplay" label="金额" width="100" />
      <el-table-column prop="statusText" label="状态" width="80">
        <template #default="{row}"><el-tag :type="{PENDING:'warning',ISSUED:'success',REJECTED:'danger',VOIDED:'info',APPROVED:''}[row.status]" size="small">{{ row.statusText }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="invoiceNumber" label="发票号" width="120" />
      <el-table-column prop="createdAt" label="申请时间" width="160" />
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{row}">
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          <template v-if="row.status==='PENDING'">
            <el-button link type="success" @click="openApprove(row)">审核开票</el-button>
            <el-button link type="danger" @click="openReject(row)">驳回</el-button>
          </template>
          <el-button v-if="row.status==='ISSUED'" link type="warning" @click="openVoid(row)">红冲</el-button>
          <el-button v-if="row.status==='ISSUED'" link type="primary" @click="resend(row)">重发邮件</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="display:flex;justify-content:flex-end;margin-top:16px" v-if="total>0">
      <el-pagination v-model:current-page="page" :total="total" :page-size="pageSize" layout="total, prev, pager, next" @current-change="fetchData" />
    </div>
  </el-card>

  <!-- 详情弹窗 -->
  <el-dialog v-model="detailVisible" title="开票申请详情" width="700px">
    <el-descriptions :column="2" border v-if="detail">
      <el-descriptions-item label="申请编号">{{ detail.applicationNo }}</el-descriptions-item>
      <el-descriptions-item label="客户">{{ detail.customerName }}</el-descriptions-item>
      <el-descriptions-item label="发票类型">{{ detail.invoiceTypeText }}</el-descriptions-item>
      <el-descriptions-item label="抬头">{{ detail.titleName }}</el-descriptions-item>
      <el-descriptions-item label="税号">{{ detail.taxNo }}</el-descriptions-item>
      <el-descriptions-item label="金额">{{ detail.totalAmountDisplay }}</el-descriptions-item>
      <el-descriptions-item label="邮箱">{{ detail.receiverEmail }}</el-descriptions-item>
      <el-descriptions-item label="状态"><el-tag :type="{PENDING:'warning',ISSUED:'success',REJECTED:'danger',VOIDED:'info'}[detail.status]">{{ detail.statusText }}</el-tag></el-descriptions-item>
      <el-descriptions-item v-if="detail.invoiceNumber" label="发票号">{{ detail.invoiceNumber }}</el-descriptions-item>
      <el-descriptions-item v-if="detail.invoiceDate" label="开票日期">{{ detail.invoiceDate }}</el-descriptions-item>
      <el-descriptions-item v-if="detail.pdfUrl" label="发票PDF" :span="2"><el-link :href="detail.pdfUrl" target="_blank" type="primary">下载PDF</el-link></el-descriptions-item>
      <el-descriptions-item v-if="detail.rejectReason" label="驳回原因" :span="2"><span style="color:#f56c6c">{{ detail.rejectReason }}</span></el-descriptions-item>
      <el-descriptions-item v-if="detail.voidReason" label="红冲原因" :span="2">{{ detail.voidReason }}</el-descriptions-item>
    </el-descriptions>
    <div v-if="detail?.orders?.length" style="margin-top:16px">
      <div style="font-weight:600;margin-bottom:8px">关联订单:</div>
      <el-tag v-for="o in detail.orders" :key="o.orderId" style="margin:4px">{{ o.orderNo }} ({{ o.orderAmountDisplay }})</el-tag>
    </div>
  </el-dialog>

  <!-- 审核通过弹窗 -->
  <el-dialog v-model="approveVisible" title="审核通过并开票" width="450px">
    <el-form label-width="100px">
      <el-form-item label="开票明细方式">
        <el-select v-model="approveForm.itemMode">
          <el-option value="DEFAULT" label="默认: 技术服务费" />
          <el-option value="FROM_ORDER" label="从订单明细带入" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer><el-button @click="approveVisible=false">取消</el-button><el-button type="success" @click="doApprove" :loading="actionLoading">确认开票</el-button></template>
  </el-dialog>

  <!-- 驳回弹窗 -->
  <el-dialog v-model="rejectVisible" title="驳回开票申请" width="450px">
    <el-form label-width="80px">
      <el-form-item label="驳回原因"><el-input v-model="rejectReason" type="textarea" :rows="3" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="rejectVisible=false">取消</el-button><el-button type="danger" @click="doReject" :loading="actionLoading">确认驳回</el-button></template>
  </el-dialog>

  <!-- 红冲弹窗 -->
  <el-dialog v-model="voidVisible" title="红冲/作废发票" width="450px">
    <el-alert type="warning" :closable="false" style="margin-bottom:16px">红冲后发票将作废，关联订单可重新申请开票</el-alert>
    <el-form label-width="80px">
      <el-form-item label="红冲原因"><el-input v-model="voidReason" type="textarea" :rows="3" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="voidVisible=false">取消</el-button><el-button type="warning" @click="doVoid" :loading="actionLoading">确认红冲</el-button></template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { invoiceApi } from '@/api'
import { useAppStore } from '@/stores/app'
import { ElMessage } from 'element-plus'

const app = useAppStore()
const list = ref([]), loading = ref(false), total = ref(0), page = ref(1), pageSize = ref(20), filterStatus = ref('PENDING')
const detailVisible = ref(false), detail = ref(null)
const approveVisible = ref(false), approveForm = reactive({ itemMode: 'DEFAULT' }), currentId = ref(null)
const rejectVisible = ref(false), rejectReason = ref('')
const voidVisible = ref(false), voidReason = ref('')
const actionLoading = ref(false)

async function fetchData() {
  loading.value = true
  try { const params = { page:page.value, pageSize:pageSize.value }; if(filterStatus.value) params.status=filterStatus.value; const r=await invoiceApi.list(params); list.value=r.data.items; total.value=r.data.total } finally { loading.value=false }
}

async function viewDetail(row) { const r = await invoiceApi.getById(row.id); detail.value = r.data; detailVisible.value = true }

function openApprove(row) { currentId.value = row.id; approveForm.itemMode = 'DEFAULT'; approveVisible.value = true }
async function doApprove() {
  actionLoading.value = true
  try { await invoiceApi.approve(currentId.value, { ...approveForm, reviewerId: app.userId, reviewerName: app.realName }); ElMessage.success('开票成功'); approveVisible.value = false; fetchData() } finally { actionLoading.value = false }
}

function openReject(row) { currentId.value = row.id; rejectReason.value = ''; rejectVisible.value = true }
async function doReject() {
  if (!rejectReason.value) { ElMessage.warning('请填写驳回原因'); return }
  actionLoading.value = true
  try { await invoiceApi.reject(currentId.value, { rejectReason: rejectReason.value, reviewerId: app.userId, reviewerName: app.realName }); ElMessage.success('已驳回'); rejectVisible.value = false; fetchData() } finally { actionLoading.value = false }
}

function openVoid(row) { currentId.value = row.id; voidReason.value = ''; voidVisible.value = true }
async function doVoid() {
  if (!voidReason.value) { ElMessage.warning('请填写红冲原因'); return }
  actionLoading.value = true
  try { await invoiceApi.voidInvoice(currentId.value, { voidReason: voidReason.value }); ElMessage.success('已红冲'); voidVisible.value = false; fetchData() } finally { actionLoading.value = false }
}

async function resend(row) { await invoiceApi.resendEmail(row.id); ElMessage.success('邮件已重发') }

onMounted(fetchData)
</script>
