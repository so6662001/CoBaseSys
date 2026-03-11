<template>
  <div>
    <h2 style="margin:0 0 24px">控制台</h2>
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in cards" :key="item.label">
        <el-card shadow="hover" style="text-align:center;cursor:pointer" @click="$router.push(item.route)">
          <el-icon :size="36" :color="item.color"><component :is="item.icon" /></el-icon>
          <div style="font-size:16px;font-weight:600;margin-top:12px">{{ item.label }}</div>
          <div style="font-size:13px;color:#909399;margin-top:6px">{{ item.desc }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:24px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span style="font-weight:600">快速入口</span></template>
          <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px">
            <el-button v-for="link in quickLinks" :key="link.label" @click="$router.push(link.route)">
              {{ link.label }}
            </el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span style="font-weight:600">系统信息</span></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="系统名称">CoBaseSys 运营底座系统</el-descriptions-item>
            <el-descriptions-item label="版本">v1.0.0</el-descriptions-item>
            <el-descriptions-item label="技术栈">Java 21 + Spring Boot 3.2</el-descriptions-item>
            <el-descriptions-item label="数据库">PostgreSQL 16</el-descriptions-item>
            <el-descriptions-item label="缓存">Redis 7</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
const cards = [
  { label: '积分管理', desc: '管理积分动作和规则', icon: 'Star', color: '#f6a609', route: '/points/actions' },
  { label: '钱包管理', desc: '管理消费动作和计费', icon: 'Wallet', color: '#67c23a', route: '/wallet/actions' },
  { label: '计费订单', desc: '订单与代客下单', icon: 'ShoppingCart', color: '#409eff', route: '/billing/orders' },
  { label: '运营报表', desc: '到期预警与客户资产', icon: 'DataAnalysis', color: '#e6a23c', route: '/billing/reports' },
]

const quickLinks = [
  { label: '产品管理', route: '/billing/products' },
  { label: '套餐管理', route: '/billing/packages' },
  { label: '订阅管理', route: '/billing/subscriptions' },
  { label: '延长审批', route: '/billing/approvals' },
  { label: '代客下单', route: '/billing/orders/proxy' },
  { label: '通知模板', route: '/notifications/templates' },
]
</script>
