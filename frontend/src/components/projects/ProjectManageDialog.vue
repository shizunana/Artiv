<template>
  <el-dialog
    :model-value="modelValue"
    title="变更企划"
    width="min(480px, calc(100vw - 32px))"
    append-to-body
    align-center
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-if="project" class="manage-content">
      <div class="project-summary">
        <el-image v-if="cover" :src="cover" fit="cover" class="project-cover" />
        <div v-else class="project-cover project-cover--empty">企划</div>
        <div>
          <h3>{{ project.title }}</h3>
          <p>当前状态：{{ displayStatus }}</p>
          <p>当前截稿日期：{{ project.deadline || '未设置' }}</p>
        </div>
      </div>
      <el-alert
        :title="manageTip"
        type="info"
        :closable="false"
        show-icon
      />
      <div class="deadline-editor">
        <label>{{ requiresNewDeadline ? '重新上架截稿日期' : '修改截稿日期' }}</label>
        <el-date-picker
          v-model="deadlineDraft"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择至少 2 天后的日期"
          :disabled-date="disableBeforeProjectDeadline"
        />
        <small>截稿日期当天仍可新增应征，次日起企划显示为已过期。</small>
      </div>
      <div class="manage-actions">
        <template v-if="pendingDirectedInvitation">
          <el-button type="primary" :loading="saving" @click="saveDeadline">保存截稿日期</el-button>
          <el-button disabled>等待画师回应</el-button>
        </template>
        <el-button
          v-else-if="rejectedDirectedInvitation"
          type="primary"
          :loading="saving"
          @click="publishPublic"
        >改为公开企划</el-button>
        <template v-else-if="Number(project.status) === 0">
          <el-button type="primary" :loading="saving" @click="saveDeadline">保存截稿日期</el-button>
          <el-button :loading="saving" @click="changeStatus(4)">下架企划</el-button>
        </template>
        <el-button v-else type="primary" :loading="saving" @click="changeStatus(0)">按新日期重新上架</el-button>
        <el-button type="danger" plain :disabled="pendingDirectedInvitation" :loading="deleting" @click="removeProject">删除企划</el-button>
      </div>
      <p class="delete-tip">只有从未产生过任何订单的企划才能删除；已取消或已完成订单也计入关联记录。</p>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { changeProjectStatus, deleteProject, publishProjectPublic, updateProjectDeadline } from '@/api/project'
import {
  disableBeforeProjectDeadline,
  isProjectExpired,
  minimumProjectDeadlineText,
} from '@/utils/projectDeadline'

const props = defineProps({ modelValue: Boolean, project: { type: Object, default: null } })
const emit = defineEmits(['update:modelValue', 'changed', 'deleted'])
const saving = ref(false)
const deleting = ref(false)
const deadlineDraft = ref('')

const cover = computed(() => {
  const value = props.project?.images
  if (!value) return ''
  try {
    const parsed = JSON.parse(value)
    if (Array.isArray(parsed)) return parsed[0] || ''
  } catch { /* 兼容逗号分隔的历史数据 */ }
  return String(value).split(',').map((item) => item.trim()).find(Boolean) || ''
})
const pendingDirectedInvitation = computed(() => Number(props.project?.mode) === 1 && Number(props.project?.invitationStatus) === 0)
const rejectedDirectedInvitation = computed(() => Number(props.project?.mode) === 1 && Number(props.project?.invitationStatus) === 2)
const requiresNewDeadline = computed(() => Number(props.project?.status) !== 0)
const displayStatus = computed(() => {
  if (Number(props.project?.moderationStatus) === 1) return '违规下架'
  if (Number(props.project?.status) === 4) return rejectedDirectedInvitation.value ? '已拒绝' : '已下架'
  if (isProjectExpired(props.project?.deadline)) return '已过期'
  if (pendingDirectedInvitation.value) return '待回应'
  if (Number(props.project?.mode) === 1 && Number(props.project?.invitationStatus) === 1) return '已接受'
  return '招募中'
})
const manageTip = computed(() => {
  if (pendingDirectedInvitation.value) return '定向邀请等待回应期间不能撤回、下架或删除企划，但可以延长截稿日期。'
  if (rejectedDirectedInvitation.value) return '画师已拒绝邀请。请选择新的截稿日期，再将企划改为公开招募。'
  if (requiresNewDeadline.value) return '重新上架必须选择新的截稿日期；不会影响已有应征、会话或订单。'
  if (isProjectExpired(props.project?.deadline)) return '企划已过期。延长截稿日期后，只要仍为上架状态就会重新进入企划大厅。'
  return '截稿日期只限制新增应征；下架或修改日期不会影响已建立的企划订单。'
})

watch(
  () => [props.modelValue, props.project?.id, props.project?.status],
  ([visible]) => {
    if (!visible) return
    deadlineDraft.value = Number(props.project?.status) === 0 ? String(props.project?.deadline || '') : ''
  },
  { immediate: true },
)

function validDeadline() {
  if (!deadlineDraft.value) {
    ElMessage.warning('请选择新的截稿日期')
    return false
  }
  if (deadlineDraft.value < minimumProjectDeadlineText()) {
    ElMessage.warning('截稿日期至少需要选择当前日期的 2 天后')
    return false
  }
  return true
}

async function saveDeadline() {
  if (!validDeadline()) return
  saving.value = true
  try {
    const updated = await updateProjectDeadline(props.project.id, deadlineDraft.value)
    ElMessage.success('截稿日期已更新')
    emit('changed', updated)
    emit('update:modelValue', false)
  } finally {
    saving.value = false
  }
}

async function changeStatus(status) {
  if (status === 0 && !validDeadline()) return
  saving.value = true
  try {
    const updated = await changeProjectStatus(props.project.id, status, status === 0 ? deadlineDraft.value : undefined)
    ElMessage.success(status === 0 ? '企划已重新上架' : '企划已下架')
    emit('changed', updated)
    emit('update:modelValue', false)
  } finally {
    saving.value = false
  }
}

async function publishPublic() {
  if (!validDeadline()) return
  saving.value = true
  try {
    const updated = await publishProjectPublic(props.project.id, deadlineDraft.value)
    ElMessage.success('企划已改为公开招募并重新上架')
    emit('changed', updated)
    emit('update:modelValue', false)
  } finally {
    saving.value = false
  }
}

async function removeProject() {
  const confirmed = await ElMessageBox.confirm(
    '删除后企划将从“我的企划”中移除。确定继续吗？',
    '删除企划',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' },
  ).then(() => true).catch(() => false)
  if (!confirmed) return
  deleting.value = true
  try {
    await deleteProject(props.project.id)
    ElMessage.success('企划已删除')
    emit('deleted', props.project.id)
    emit('update:modelValue', false)
  } finally {
    deleting.value = false
  }
}
</script>

<style scoped>
.manage-content { display: grid; gap: 18px; }
.project-summary { display: flex; gap: 14px; align-items: center; }
.project-cover { width: 72px; height: 72px; border-radius: 6px; background: #f1f4f7; }
.project-cover--empty { display: grid; place-items: center; color: #9aa3ad; }
.project-summary h3 { margin: 0 0 8px; font-size: 17px; }
.project-summary p, .delete-tip { margin: 0; color: #7c8792; font-size: 13px; line-height: 1.7; }
.deadline-editor { display: grid; gap: 8px; }
.deadline-editor label { color: var(--artiv-text); font-size: 14px; font-weight: 600; }
.deadline-editor :deep(.el-date-editor) { width: 100%; }
.deadline-editor small { color: var(--artiv-text-secondary); font-size: 12px; line-height: 1.6; }
.manage-actions { display: flex; flex-wrap: wrap; gap: 10px; }
.manage-actions :deep(.el-button + .el-button) { margin-left: 0; }
</style>
