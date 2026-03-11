<template>
  <el-card shadow="never">
    <div class="page-header">
      <h2>{{ title }}</h2>
      <el-button v-if="showCreate" type="primary" size="default" @click="$emit('create')">
        <el-icon><Plus /></el-icon><span class="hidden-xs"> 新增</span>
      </el-button>
    </div>
    <slot name="filter" />
    <div class="table-wrapper">
      <el-table :data="data" v-loading="loading" stripe border style="width:100%" :size="isMobile?'small':'default'">
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
    </div>
    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination v-model:current-page="currentPage" v-model:page-size="currentPageSize"
        :total="total" :page-sizes="[10,20,50]"
        :layout="isMobile ? 'total, prev, next' : 'total, sizes, prev, pager, next'"
        :small="isMobile"
        @current-change="$emit('page-change', $event)"
        @size-change="$emit('size-change', $event)" />
    </div>
  </el-card>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'

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

const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1024)
const isMobile = computed(() => windowWidth.value < 768)
function onResize() { windowWidth.value = window.innerWidth }
onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

const currentPage = computed({
  get: () => props.page,
  set: (v) => emit('page-change', v)
})
const currentPageSize = computed({
  get: () => props.pageSize,
  set: (v) => emit('size-change', v)
})
</script>

<style scoped>
.table-wrapper {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
@media (max-width: 767px) {
  .hidden-xs { display: none !important; }
  .pagination-wrapper { justify-content: center; }
}
</style>
