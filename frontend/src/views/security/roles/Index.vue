<template>
  <el-card shadow="never">
    <div class="page-header"><h2>角色与权限</h2></div>

    <el-row :gutter="20">
      <el-col :span="8">
        <h3>系统角色</h3>
        <el-table :data="roles" border stripe size="small">
          <el-table-column prop="id" label="ID" width="50" />
          <el-table-column prop="roleCode" label="编码" width="130" />
          <el-table-column prop="roleName" label="名称" />
          <el-table-column prop="description" label="说明" show-overflow-tooltip />
        </el-table>
      </el-col>

      <el-col :span="16">
        <h3>权限清单</h3>
        <el-table :data="permissions" border stripe size="small">
          <el-table-column prop="id" label="ID" width="50" />
          <el-table-column prop="module" label="模块" width="80">
            <template #default="{row}"><el-tag size="small">{{ row.module }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="permissionCode" label="权限编码" width="200" />
          <el-table-column prop="permissionName" label="权限名称" />
          <el-table-column prop="action" label="操作" width="80" />
        </el-table>
      </el-col>
    </el-row>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { securityUserApi } from '@/api'

const roles = ref([]), permissions = ref([])

async function fetchData() {
  const [r1, r2] = await Promise.all([securityUserApi.listRoles(), securityUserApi.listPermissions()])
  roles.value = r1.data || []
  permissions.value = r2.data || []
}
onMounted(fetchData)
</script>
