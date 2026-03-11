<template>
  <el-card shadow="never">
    <div class="page-header"><h2>手动调账</h2></div>
    <el-alert type="warning" :closable="false" style="margin-bottom:20px">
      手动调账将直接修改用户钱包余额，请谨慎操作。正数为增加余额，负数为扣减余额。
    </el-alert>
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" style="max-width:500px">
      <el-form-item label="用户ID" prop="userId">
        <el-input v-model="form.userId" placeholder="请输入用户ID" />
      </el-form-item>
      <el-form-item label="调账金额(分)" prop="amount">
        <el-input-number v-model="form.amount" style="width:100%" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入调账原因" />
      </el-form-item>
      <el-form-item>
        <el-popconfirm title="确定执行调账操作吗？" @confirm="handleSubmit">
          <template #reference>
            <el-button type="primary" :loading="submitting">确认调账</el-button>
          </template>
        </el-popconfirm>
      </el-form-item>
    </el-form>

    <el-divider />

    <div v-if="result">
      <h3>调账结果</h3>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="流水号">{{ result.transactionNo }}</el-descriptions-item>
        <el-descriptions-item label="变动金额">{{ result.amountDisplay }}</el-descriptions-item>
        <el-descriptions-item label="变动后余额">{{ result.balanceDisplay }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </el-card>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { walletApi } from '@/api'
import { ElMessage } from 'element-plus'

const formRef = ref(null)
const submitting = ref(false)
const result = ref(null)
const form = reactive({ userId: '', amount: 0, remark: '' })

const rules = {
  userId: [{ required: true, message: '请输入用户ID', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  remark: [{ required: true, message: '请输入备注', trigger: 'blur' }],
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch { return }

  submitting.value = true
  try {
    const res = await walletApi.adjust({ userId: form.userId, amount: form.amount, remark: form.remark })
    result.value = res.data
    ElMessage.success('调账成功')
  } finally { submitting.value = false }
}
</script>
