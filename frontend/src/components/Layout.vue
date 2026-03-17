<template>
  <el-container style="height: 100vh">
    <!-- PC侧边栏 -->
    <el-aside v-if="!isMobile" :width="app.sidebarCollapsed ? '64px' : '220px'" style="transition: width 0.3s; background: #1d1e1f;">
      <div class="logo" :class="{ collapsed: app.sidebarCollapsed }">
        <span v-if="!app.sidebarCollapsed">CoBaseSys</span><span v-else>CB</span>
      </div>
      <el-scrollbar>
        <el-menu :default-active="$route.path" router :collapse="app.sidebarCollapsed"
          background-color="#1d1e1f" text-color="#bfcbd9" active-text-color="#409eff" style="border:none">
          <template v-for="(group, gi) in menuGroups" :key="gi">
            <el-menu-item-group v-if="group.title" :title="group.title" />
            <template v-for="menu in group.menus" :key="menu.index">
              <el-menu-item v-if="!menu.children" :index="menu.index">
                <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon><span>{{ menu.label }}</span>
              </el-menu-item>
              <el-sub-menu v-else :index="menu.index">
                <template #title><el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon><span>{{ menu.label }}</span></template>
                <el-menu-item v-for="child in menu.children" :key="child.index" :index="child.index">{{ child.label }}</el-menu-item>
              </el-sub-menu>
            </template>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <!-- 移动端Drawer -->
    <el-drawer v-model="mobileDrawer" direction="ltr" :size="260" :show-close="false" :with-header="false"
      style="background:#1d1e1f;padding:0">
      <div class="logo" style="height:50px;border-bottom:1px solid #333">CoBaseSys</div>
      <el-scrollbar>
        <el-menu :default-active="$route.path" router @select="mobileDrawer=false"
          background-color="#1d1e1f" text-color="#bfcbd9" active-text-color="#409eff" style="border:none">
          <template v-for="(group, gi) in menuGroups" :key="'m'+gi">
            <el-menu-item-group v-if="group.title" :title="group.title" />
            <template v-for="menu in group.menus" :key="'m'+menu.index">
              <el-menu-item v-if="!menu.children" :index="menu.index">
                <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon><span>{{ menu.label }}</span>
              </el-menu-item>
              <el-sub-menu v-else :index="menu.index">
                <template #title><el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon><span>{{ menu.label }}</span></template>
                <el-menu-item v-for="child in menu.children" :key="'m'+child.index" :index="child.index">{{ child.label }}</el-menu-item>
              </el-sub-menu>
            </template>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-drawer>

    <el-container>
      <el-header class="app-header">
        <div style="display:flex;align-items:center;gap:8px">
          <el-icon v-if="isMobile" style="cursor:pointer;font-size:22px" @click="mobileDrawer=true"><Operation /></el-icon>
          <el-icon v-else style="cursor:pointer;font-size:18px" @click="app.toggleSidebar"><Fold v-if="!app.sidebarCollapsed" /><Expand v-else /></el-icon>
          <span class="hidden-xs" style="font-size:14px;color:#606266">{{ $route.meta.title || '控制台' }}</span>
        </div>
        <div style="display:flex;align-items:center;gap:8px">
          <el-tag type="info" class="hidden-xs" size="small">租户: {{ app.tenantName }}</el-tag>
          <el-dropdown>
            <span style="font-size:13px;color:#606266;cursor:pointer;white-space:nowrap">{{ app.realName || app.username }} <el-icon><ArrowDown /></el-icon></span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="showChangePwd=true">修改密码</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout" style="color:#f56c6c">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="app-main"><router-view /></el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="showChangePwd" title="修改密码" :width="isMobile?'92%':'400px'">
    <el-form :model="pwdForm" label-width="80px">
      <el-form-item label="原密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
      <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少8位" /></el-form-item>
      <el-form-item label="确认密码"><el-input v-model="pwdForm.confirmPassword" type="password" show-password /></el-form-item>
    </el-form>
    <template #footer><el-button @click="showChangePwd=false">取消</el-button><el-button type="primary" @click="doChangePwd">确认修改</el-button></template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { authApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const app = useAppStore()
const showChangePwd = ref(false)
const pwdForm = reactive({ oldPassword:'', newPassword:'', confirmPassword:'' })
const mobileDrawer = ref(false)

const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1024)
const isMobile = computed(() => windowWidth.value < 768)
function onResize() { windowWidth.value = window.innerWidth }
onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

const menuGroups = [
  { title: null, menus: [
    { index: '/dashboard', icon: 'Odometer', label: '控制台' },
    { index: 'base', icon: 'Setting', label: '基础管理', children: [
      { index: '/tenants', label: '租户管理' }, { index: '/systems', label: '外部系统' }
    ]},
    { index: 'points', icon: 'Star', label: '积分管理', children: [
      { index: '/points/actions', label: '积分动作' }, { index: '/points/rules', label: '积分规则' },
      { index: '/points/accounts', label: '积分账户' }, { index: '/points/transactions', label: '积分流水' },
      { index: '/points/gift/apply', label: '赠送申请' }, { index: '/points/gift/approval', label: '赠送审批' },
      { index: '/points/gift/report', label: '赠送报表' },
    ]},
    { index: 'wallet', icon: 'Wallet', label: '钱包管理', children: [
      { index: '/wallet/actions', label: '消费动作' }, { index: '/wallet/rules', label: '消费规则' },
      { index: '/wallet/promotions', label: '充值促销' }, { index: '/wallet/accounts', label: '钱包账户' },
      { index: '/wallet/transactions', label: '钱包流水' }, { index: '/wallet/adjust', label: '手动调账' },
    ]},
    { index: 'member', icon: 'User', label: '会员管理', children: [
      { index: '/members/levels', label: '会员等级' }, { index: '/members/users', label: '会员用户' },
    ]},
    { index: 'notify', icon: 'Bell', label: '通知服务', children: [
      { index: '/notifications/templates', label: '通知模板' }, { index: '/notifications/rules', label: '通知规则' },
      { index: '/notifications/records', label: '通知记录' },
    ]},
    { index: 'webhook', icon: 'Connection', label: 'Webhook', children: [
      { index: '/webhooks', label: 'Webhook配置' }, { index: '/webhooks/logs', label: '推送日志' },
    ]},
    { index: 'invoice-menu', icon: 'Ticket', label: '电子发票', children: [
      { index: '/invoice/apply', label: '申请开票' }, { index: '/invoice/review', label: '开票审核' },
      { index: '/invoice/list', label: '发票记录' },
    ]},
  ]},
  { title: '计费模块', menus: [
    { index: 'bp', icon: 'Goods', label: '商品管理', children: [
      { index: '/billing/products', label: '产品管理' }, { index: '/billing/packages', label: '套餐管理' },
      { index: '/billing/pricing', label: '定价方案' },
    ]},
    { index: 'br', icon: 'Discount', label: '营销规则', children: [
      { index: '/billing/discounts', label: '折扣规则' }, { index: '/billing/gifts', label: '赠送规则' },
    ]},
    { index: 'bo', icon: 'ShoppingCart', label: '订单中心', children: [
      { index: '/billing/orders', label: '订单管理' }, { index: '/billing/orders/proxy', label: '代客下单' },
    ]},
    { index: 'bs', icon: 'Calendar', label: '订阅中心', children: [
      { index: '/billing/subscriptions', label: '订阅管理' }, { index: '/billing/trials', label: '试用管理' },
      { index: '/billing/approvals', label: '延长审批' },
    ]},
    { index: 'brp', icon: 'DataAnalysis', label: '运营报表', children: [
      { index: '/billing/reports', label: '报表中心' },
    ]},
  ]},
  { title: '安全管理', menus: [
    { index: 'sec', icon: 'Lock', label: '安全中心', children: [
      { index: '/security/users', label: '管理员' }, { index: '/security/roles', label: '角色权限' },
      { index: '/security/login-logs', label: '登录日志' }, { index: '/security/audit-logs', label: '审计日志' },
      { index: '/security/reconciliation', label: '对账报告' },
    ]},
  ]},
]

async function handleLogout() {
  try { if (app.userId) await authApi.logout(app.userId) } catch {}
  app.logout(); router.push('/login')
}
async function doChangePwd() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) { ElMessage.warning('请填写完整'); return }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) { ElMessage.warning('两次密码不一致'); return }
  if (pwdForm.newPassword.length < 8) { ElMessage.warning('密码至少8位'); return }
  try { await authApi.changePassword(app.userId, pwdForm.oldPassword, pwdForm.newPassword); ElMessage.success('密码已修改'); showChangePwd.value=false; app.logout(); router.push('/login') } catch {}
}
</script>

<style scoped>
.logo { height:56px; display:flex; align-items:center; justify-content:center; color:#fff; font-size:20px; font-weight:700; border-bottom:1px solid #333; letter-spacing:2px; }
.logo.collapsed { font-size:16px; letter-spacing:0; }
.app-header { display:flex; align-items:center; justify-content:space-between; background:#fff; border-bottom:1px solid #e4e7ed; padding:0 16px; height:50px; }
.app-main { padding:16px; overflow:auto; }
@media (max-width: 767px) {
  .hidden-xs { display: none !important; }
  .app-header { padding:0 10px; height:46px; }
  .app-main { padding:10px; }
}
</style>
