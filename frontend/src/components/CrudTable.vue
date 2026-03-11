<template>
  <el-card shadow="never">
    <div class="page-header">
      <h2>{{ title }}</h2>
      <el-button v-if="showCreate" type="primary" @click="$emit('create')">
        <el-icon><Plus /></el-icon> 新增
      </el-button>
    </div>
    <slot name="filter" />
    <el-table :data="data" v-loading="loading" stripe border style="width:100%">
      <slot />
      <el-table-column v-if="showActions" label="操作" :width="actionWidth" fixed="right">
        <template #default="{ row }">
          <el-button v-if="showEdit" link type="primary" @click="$emit('edit', row)">编辑</el-button>
          <el-popconfirm v-if="showDelete" title="确定删除吗？" @confirm="$emit('delete', row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
          <slot name="extra-actions" :row="row" />
        </template>
      </el-table-column>
    </el-table>
    <div style="display:flex;justify-content:flex-end;margin-top:16px" v-if="total > 0">
      <el-pagination v-model:current-page="currentPage" v-model:page-size="currentPageSize"
        :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next"
        @current-change="$emit('page-change', $event)"
        @size-change="$emit('size-change', $event)" />
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  data: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 20 },
  showCreate: { type: Boolean, default: true },
  showEdit: { type: Boolean, default: true },
  showDelete: { type: Boolean, default: true },
  showActions: { type: Boolean, default: true },
  actionWidth: { type: Number, default: 160 },
})

const emit = defineEmits(['create', 'edit', 'delete', 'page-change', 'size-change'])

const currentPage = computed({
  get: () => props.page,
  set: (v) => emit('page-change', v)
})
const currentPageSize = computed({
  get: () => props.pageSize,
  set: (v) => emit('size-change', v)
})
</script>
