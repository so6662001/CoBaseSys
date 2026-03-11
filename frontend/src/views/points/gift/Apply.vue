<template>
  <el-card shadow="never">
    <div class="page-header"><h2>赠送积分申请</h2></div>
    <el-alert type="warning" :closable="false" style="margin-bottom:20px">
      赠送积分需要提交申请，由主管审批通过后才会执行赠送。请如实填写赠送原因。
    </el-alert>
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" style="max-width:550px">
      <el-form-item label="客户ID" prop="customerId"><el-input v-model="form.customerId" /></el-form-item>
      <el-form-item label="客户名称"><el-input v-model="form.customerName" /></el-form-item>
      <el-form-item label="赠送积分" prop="points"><el-input-number v-model="form.points" :min="1" style="width:100%" /></el-form-item>
      <el-form-item label="赠送类型">
        <el-select v-model="form.sourceType" style="width:100%">
          <el-option value="GIFT_MANUAL" label="人工赠送" />
          <el-option value="GIFT_ACTIVITY" label="活动赠送" />
        </el-select>
      </el-form-item>
      <el-form-item label="赠送原因" prop="giftReason"><el-input v-model="form.giftReason" type="textarea" :rows="3" placeholder="请详细说明赠送原因" /></el-form-item>
      <el-form-item label="申请人ID" prop="applicantId"><el-input v-model="form.applicantId" /></el-form-item>
      <el-form-item label="申请人姓名"><el-input v-model="form.applicantName" /></el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSubmit" :loading="submitting" size="large">提交申请</el-button>
      </el-form-item>
    </el-form>

    <el-divider v-if="result" />
    <el-result v-if="result" icon="success" title="申请已提交" sub-title="等待主管审批通过后积分将自动赠送">
      <template #extra>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="客户">{{ result.customerName }} ({{ result.customerId }})</el-descriptions-item>
          <el-descriptions-item label="赠送积分">{{ result.points }}</el-descriptions-item>
          <el-descriptions-item label="原因">{{ result.giftReason }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag type="warning">{{ result.statusText }}</el-tag></el-descriptions-item>
        </el-descriptions>
      </template>
    </el-result>
  </el-card>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { pointApi } from '@/api'
import { useAppStore } from '@/stores/app'
import { ElMessage } from 'element-plus'

const app = useAppStore()
const formRef = ref(null), submitting = ref(false), result = ref(null)
const form = reactive({
  customerId: '', customerName: '', points: 100, sourceType: 'GIFT_MANUAL',
  giftReason: '', applicantId: app.userId || 'admin', applicantName: app.realName || '管理员'
})
const rules = {
  customerId: [{ required: true, message: '请输入客户ID' }],
  points: [{ required: true, message: '请输入积分数量' }],
  giftReason: [{ required: true, message: '请输入赠送原因', min: 5 }],
  applicantId: [{ required: true, message: '请输入申请人ID' }],
}

async function handleSubmit() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    const r = await pointApi.giftApply(form)
    result.value = r.data
    ElMessage.success('赠送申请已提交，等待审批')
  } finally { submitting.value = false }
}
</script>
