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
        <div style="display:flex;align-items:center;gap:16px">
          <el-tag type="info">租户: {{ app.tenantName }}</el-tag>
          <el-button text type="danger" @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <el-main style="padding:20px;overflow:auto">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const app = useAppStore()

function handleLogout() {
  app.logout()
  router.push('/login')
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
