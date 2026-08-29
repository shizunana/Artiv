<template>
  <el-dialog
    v-model="visible"
    title="删除企划"
    width="760px"
    append-to-body
    align-center
    destroy-on-close
    @open="loadProjects"
    @closed="reset"
  >
    <el-alert
      title="灰色企划存在关联订单或待回应定向邀请，当前无法删除。"
      type="info"
      :closable="false"
      show-icon
    />

    <div v-loading="loading" class="delete-project-list">
      <el-empty v-if="!loading && !projects.length" description="当前分类下没有可管理的企划" />
      <article
        v-for="project in projects"
        :key="project.id"
        class="delete-project-row"
        :class="{ 'is-disabled': selectionDisabled(project) }"
      >
        <el-checkbox
          :model-value="selectedIds.includes(project.id)"
          :disabled="selectionDisabled(project)"
          @change="toggleSelection(project.id, $event)"
        />
        <div class="project-name">
          <strong :title="project.title">{{ project.title }}</strong>
          <small>企划 #{{ project.id }}</small>
        </div>
        <div><small>截稿日期</small><strong>{{ project.deadline || '未设置' }}</strong></div>
        <div><small>合作方式</small><strong>{{ Number(project.mode) === 1 ? '定向邀请' : '公开招募' }}</strong></div>
        <div><small>企划状态</small><el-tag :type="projectStatus(project).type">{{ projectStatus(project).text }}</el-tag></div>
        <div class="delete-limit"><small>删除限制</small><strong>{{ limitText(project) }}</strong></div>
      </article>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="danger" :disabled="!selectedIds.length" :loading="deleting" @click="confirmDelete">
        删除企划（{{ selectedIds.length }}）
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteProject, getMyProjects } from '@/api/project'
import { isProjectExpired } from '@/utils/projectDeadline'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: Number, default: 0 },
})
const emit = defineEmits(['update:modelValue', 'deleted'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})
const projects = ref([])
const selectedIds = ref([])
const loading = ref(false)
const deleting = ref(false)

function projectStatus(project) {
  if (Number(project.moderationStatus) === 1) return { text: '违规下架', type: 'danger' }
  if (Number(project.status) === 4) {
    return Number(project.mode) === 1 && Number(project.invitationStatus) === 2
      ? { text: '已拒绝', type: 'danger' }
      : { text: '已下架', type: 'info' }
  }
  if (isProjectExpired(project.deadline)) return { text: '已过期', type: 'info' }
  if (Number(project.mode) === 1 && Number(project.invitationStatus) === 0) return { text: '待回应', type: 'warning' }
  if (Number(project.mode) === 1 && Number(project.invitationStatus) === 1) return { text: '已接受', type: 'success' }
  return { text: '招募中', type: 'success' }
}

function selectionDisabled(project) {
  if (project.deletable === false) return true
  return Number(project.mode) === 1 && Number(project.invitationStatus) === 0
}

function limitText(project) {
  if (project.deleteBlockReason) return project.deleteBlockReason
  if (selectionDisabled(project)) return '存在待回应定向邀请，无法删除'
  return '可删除'
}

function toggleSelection(id, checked) {
  selectedIds.value = checked
    ? [...selectedIds.value, id]
    : selectedIds.value.filter((item) => item !== id)
}

async function loadProjects() {
  loading.value = true
  selectedIds.value = []
  try {
    const data = await getMyProjects({ page: 1, size: 200, mode: props.mode })
    projects.value = data.records || []
  } finally { loading.value = false }
}

async function confirmDelete() {
  const confirmed = await ElMessageBox.confirm(
    `确定删除已选中的 ${selectedIds.value.length} 个企划吗？删除后无法恢复。`,
    '确认删除企划',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' },
  ).then(() => true).catch(() => false)
  if (!confirmed) return

  deleting.value = true
  const succeeded = []
  const failed = []
  try {
    for (const id of selectedIds.value) {
      try {
        await deleteProject(id)
        succeeded.push(id)
      } catch {
        failed.push(id)
      }
    }
    projects.value = projects.value.filter((item) => !succeeded.includes(item.id))
    selectedIds.value = [...failed]
    if (succeeded.length) {
      ElMessage.success(`已删除 ${succeeded.length} 个企划`)
      emit('deleted', succeeded)
    }
    if (failed.length) ElMessage.warning(`${failed.length} 个企划因存在业务关联未能删除`)
    if (!failed.length) visible.value = false
  } finally { deleting.value = false }
}

function reset() {
  projects.value = []
  selectedIds.value = []
}
</script>

<style scoped>
.delete-project-list { min-height: 280px; max-height: 480px; margin-top: 16px; overflow-y: auto; }
.delete-project-row { display: grid; grid-template-columns: 28px minmax(150px, 1.5fr) repeat(4, minmax(100px, 1fr)); align-items: center; gap: 16px; padding: 17px 10px; border-bottom: 1px solid var(--artiv-border); }
.delete-project-row.is-disabled { border-radius: 6px; background: #f5f7fa; color: var(--artiv-text-secondary); }
.delete-project-row.is-disabled :deep(.el-tag) { opacity: .72; }
.delete-project-row > div { display: grid; min-width: 0; gap: 6px; }
.delete-project-row small { color: var(--artiv-text-secondary); font-size: 12px; }
.delete-project-row strong { overflow: hidden; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.project-name > strong { font-size: 15px; }
.delete-limit strong { color: var(--artiv-text-secondary); font-weight: 500; white-space: normal; }
.delete-project-row:not(.is-disabled) .delete-limit strong { color: var(--el-color-success); }
@media (max-width: 720px) {
  .delete-project-row { grid-template-columns: 28px minmax(0, 1fr) minmax(90px, .8fr); }
  .delete-project-row > div:nth-of-type(n + 3) { display: none; }
}
</style>
