<template>
  <el-container style="height: 100vh">
    <el-aside :width="app.sidebarCollapsed ? '64px' : '220px'" style="transition: width 0.3s; background: #1d1e1f;">
      <div class="logo" :class="{ collapsed: app.sidebarCollapsed }">
        <span v-if="!app.sidebarCollapsed">CoBaseSys</span>
        <span v-else>CB</span>
      </div>
      <el-menu :default-active="$route.path" router :collapse="app.sidebarCollapsed"
        background-color="#1d1e1f" text-color="#bfcbd9" active-text-color="#409eff" style="border:none">
        <el-menu-item index="/dashboard"><el-icon><Odometer /></el-icon><span>控制台</span></el-menu-item>

        <el-sub-menu index="base">
          <template #title><el-icon><Setting /></el-icon><span>基础管理</span></template>
          <el-menu-item index="/tenants">租户管理</el-menu-item>
          <el-menu-item index="/systems">外部系统</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="points">
          <template #title><el-icon><Star /></el-icon><span>积分管理</span></template>
          <el-menu-item index="/points/actions">积分动作</el-menu-item>
          <el-menu-item index="/points/rules">积分规则</el-menu-item>
          <el-menu-item index="/points/accounts">积分账户</el-menu-item>
          <el-menu-item index="/points/transactions">积分流水</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="wallet">
          <template #title><el-icon><Wallet /></el-icon><span>钱包管理</span></template>
          <el-menu-item index="/wallet/actions">消费动作</el-menu-item>
          <el-menu-item index="/wallet/rules">消费规则</el-menu-item>
          <el-menu-item index="/wallet/promotions">充值促销</el-menu-item>
          <el-menu-item index="/wallet/accounts">钱包账户</el-menu-item>
          <el-menu-item index="/wallet/transactions">钱包流水</el-menu-item>
          <el-menu-item index="/wallet/adjust">手动调账</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="member">
          <template #title><el-icon><User /></el-icon><span>会员管理</span></template>
          <el-menu-item index="/members/levels">会员等级</el-menu-item>
          <el-menu-item index="/members/users">会员用户</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="notify">
          <template #title><el-icon><Bell /></el-icon><span>通知服务</span></template>
          <el-menu-item index="/notifications/templates">通知模板</el-menu-item>
          <el-menu-item index="/notifications/rules">通知规则</el-menu-item>
          <el-menu-item index="/notifications/records">通知记录</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="webhook">
          <template #title><el-icon><Connection /></el-icon><span>Webhook</span></template>
          <el-menu-item index="/webhooks">Webhook配置</el-menu-item>
          <el-menu-item index="/webhooks/logs">推送日志</el-menu-item>
        </el-sub-menu>

        <el-menu-item-group title="计费模块" />

        <el-sub-menu index="billing-product">
          <template #title><el-icon><Goods /></el-icon><span>商品管理</span></template>
          <el-menu-item index="/billing/products">产品管理</el-menu-item>
          <el-menu-item index="/billing/packages">套餐管理</el-menu-item>
          <el-menu-item index="/billing/pricing">定价方案</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="billing-rule">
          <template #title><el-icon><Discount /></el-icon><span>营销规则</span></template>
          <el-menu-item index="/billing/discounts">折扣规则</el-menu-item>
          <el-menu-item index="/billing/gifts">赠送规则</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="billing-order">
          <template #title><el-icon><ShoppingCart /></el-icon><span>订单中心</span></template>
          <el-menu-item index="/billing/orders">订单管理</el-menu-item>
          <el-menu-item index="/billing/orders/proxy">代客下单</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="billing-sub">
          <template #title><el-icon><Calendar /></el-icon><span>订阅中心</span></template>
          <el-menu-item index="/billing/subscriptions">订阅管理</el-menu-item>
          <el-menu-item index="/billing/trials">试用管理</el-menu-item>
          <el-menu-item index="/billing/approvals">延长审批</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="billing-report">
          <template #title><el-icon><DataAnalysis /></el-icon><span>运营报表</span></template>
          <el-menu-item index="/billing/reports">报表中心</el-menu-item>
        </el-sub-menu>

        <el-menu-item-group title="安全管理" />

        <el-sub-menu index="security">
          <template #title><el-icon><Lock /></el-icon><span>安全中心</span></template>
          <el-menu-item index="/security/users">管理员</el-menu-item>
          <el-menu-item index="/security/roles">角色权限</el-menu-item>
          <el-menu-item index="/security/login-logs">登录日志</el-menu-item>
          <el-menu-item index="/security/audit-logs">审计日志</el-menu-item>
          <el-menu-item index="/security/reconciliation">对账报告</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header style="display:flex;align-items:center;justify-content:space-between;background:#fff;border-bottom:1px solid #e4e7ed;padding:0 20px;height:56px;">
        <div style="display:flex;align-items:center;gap:12px">
          <el-icon style="cursor:pointer;font-size:18px" @click="app.toggleSidebar"><Fold v-if="!app.sidebarCollapsed" /><Expand v-else /></el-icon>
          <el-breadcrumb>
            <el-breadcrumb-item>{{ $route.meta.title || '控制台' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div style="display:flex;align-items:center;gap:12px">
          <el-tag type="info">租户: {{ app.tenantName }}</el-tag>
          <el-dropdown>
            <span style="font-size:13px;color:#606266;cursor:pointer">{{ app.realName || app.username }} <el-icon><ArrowDown /></el-icon></span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="showChangePwd=true">修改密码</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout" style="color:#f56c6c">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main style="padding:20px;overflow:auto">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="showChangePwd" title="修改密码" width="400px">
    <el-form :model="pwdForm" label-width="80px">
      <el-form-item label="原密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
      <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少8位，需包含字母和数字" /></el-form-item>
      <el-form-item label="确认密码"><el-input v-model="pwdForm.confirmPassword" type="password" show-password /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showChangePwd=false">取消</el-button>
      <el-button type="primary" @click="doChangePwd">确认修改</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { authApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const app = useAppStore()
const showChangePwd = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

async function handleLogout() {
  try { if (app.userId) await authApi.logout(app.userId) } catch {}
  app.logout()
  router.push('/login')
}

async function doChangePwd() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) { ElMessage.warning('请填写完整'); return }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) { ElMessage.warning('两次密码不一致'); return }
  if (pwdForm.newPassword.length < 8) { ElMessage.warning('密码至少8位'); return }
  try {
    await authApi.changePassword(app.userId, pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码已修改，请重新登录')
    showChangePwd.value = false
    app.logout()
    router.push('/login')
  } catch {}
}
</script>

<style scoped>
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  border-bottom: 1px solid #333;
  letter-spacing: 2px;
}
.logo.collapsed { font-size: 16px; letter-spacing: 0; }
</style>
