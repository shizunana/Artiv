<template>
  <el-dialog
    v-model="visible"
    title="删除橱窗"
    width="760px"
    append-to-body
    align-center
    destroy-on-close
    @open="loadStalls"
    @closed="reset"
  >
    <el-alert
      title="灰色橱窗存在未完成订单，当前无法删除；其他橱窗删除后会同时下架且无法恢复。"
      type="info"
      :closable="false"
      show-icon
    />

    <div v-loading="loading" class="delete-stall-list">
      <el-empty v-if="!loading && !stalls.length" description="当前没有可管理的橱窗" />
      <article
        v-for="stall in stalls"
        :key="stall.id"
        class="delete-stall-row"
        :class="{ 'is-disabled': selectionDisabled(stall) }"
      >
        <el-checkbox
          :model-value="selectedIds.includes(stall.id)"
          :disabled="selectionDisabled(stall)"
          @change="toggleSelection(stall.id, $event)"
        />
        <div class="stall-name">
          <strong :title="stall.title">{{ stall.title || '未命名橱窗' }}</strong>
          <small>橱窗 #{{ stall.id }}</small>
        </div>
        <div><small>类型</small><strong>{{ stall.category || '未分类' }}</strong></div>
        <div><small>库存</small><strong>{{ stall.stock ?? 0 }} 件</strong></div>
        <div><small>橱窗状态</small><el-tag :type="stallStatus(stall).type">{{ stallStatus(stall).text }}</el-tag></div>
        <div class="delete-limit"><small>删除限制</small><strong>{{ limitText(stall) }}</strong></div>
      </article>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="danger" :disabled="!selectedIds.length" :loading="deleting" @click="confirmDelete">
        删除橱窗（{{ selectedIds.length }}）
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteStall, getMyStalls } from '@/api/stall'

const props = defineProps({ modelValue: { type: Boolean, default: false } })
const emit = defineEmits(['update:modelValue', 'deleted'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})
const stalls = ref([])
const selectedIds = ref([])
const loading = ref(false)
const deleting = ref(false)

function selectionDisabled(stall) {
  return stall.hasUnfinishedOrders === true
}

function limitText(stall) {
  return selectionDisabled(stall) ? '存在未完成订单，无法删除' : '可删除'
}

function stallStatus(stall) {
  if (Number(stall.status) !== 1) return { text: '已下架', type: 'info' }
  if (Number(stall.stock || 0) <= 0) return { text: '无库存', type: 'warning' }
  return { text: '上架中', type: 'success' }
}

function toggleSelection(id, checked) {
  selectedIds.value = checked
    ? [...selectedIds.value, id]
    : selectedIds.value.filter((item) => item !== id)
}

async function loadStalls() {
  loading.value = true
  selectedIds.value = []
  try {
    const data = await getMyStalls({ page: 1, size: 200 })
    stalls.value = data.records || []
  } finally {
    loading.value = false
  }
}

async function confirmDelete() {
  const confirmed = await ElMessageBox.confirm(
    `确定删除已选中的 ${selectedIds.value.length} 个橱窗吗？删除后会同时下架，且无法恢复。`,
    '确认删除橱窗',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' },
  ).then(() => true).catch(() => false)
  if (!confirmed) return

  deleting.value = true
  const succeeded = []
  const failed = []
  try {
    for (const id of selectedIds.value) {
      try {
        await deleteStall(id)
        succeeded.push(id)
      } catch {
        failed.push(id)
      }
    }
    stalls.value = stalls.value.filter((item) => !succeeded.includes(item.id))
    selectedIds.value = [...failed]
    if (succeeded.length) {
      ElMessage.success(`已删除 ${succeeded.length} 个橱窗`)
      emit('deleted', succeeded)
    }
    if (failed.length) ElMessage.warning(`${failed.length} 个橱窗因存在业务关联未能删除`)
    if (!failed.length) visible.value = false
  } finally {
    deleting.value = false
  }
}

function reset() {
  stalls.value = []
  selectedIds.value = []
}
</script>

<style scoped>
.delete-stall-list { min-height: 280px; max-height: 480px; margin-top: 16px; overflow-y: auto; }
.delete-stall-row { display: grid; grid-template-columns: 28px minmax(150px, 1.5fr) repeat(4, minmax(100px, 1fr)); align-items: center; gap: 16px; padding: 17px 10px; border-bottom: 1px solid var(--artiv-border); }
.delete-stall-row.is-disabled { border-radius: 6px; background: #f5f7fa; color: var(--artiv-text-secondary); }
.delete-stall-row.is-disabled :deep(.el-tag) { opacity: .72; }
.delete-stall-row > div { display: grid; min-width: 0; gap: 6px; }
.delete-stall-row small { color: var(--artiv-text-secondary); font-size: 12px; }
.delete-stall-row strong { overflow: hidden; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.stall-name > strong { font-size: 15px; }
.delete-limit strong { color: var(--artiv-text-secondary); font-weight: 500; white-space: normal; }
.delete-stall-row:not(.is-disabled) .delete-limit strong { color: var(--el-color-success); }
@media (max-width: 720px) {
  .delete-stall-row { grid-template-columns: 28px minmax(0, 1fr) minmax(90px, .8fr); }
  .delete-stall-row > div:nth-of-type(n + 3) { display: none; }
}
</style>
