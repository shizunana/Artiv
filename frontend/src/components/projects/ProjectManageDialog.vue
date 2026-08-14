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
          <p>当前状态：{{ Number(project.status) === 0 ? '招募中' : '已下架' }}</p>
        </div>
      </div>
      <el-alert
        title="下架不会影响已建立的企划订单，订单双方仍可查看企划详情。"
        type="info"
        :closable="false"
        show-icon
      />
      <div class="manage-actions">
        <el-button
          v-if="Number(project.status) === 0"
          :loading="saving"
          @click="changeStatus(4)"
        >下架企划</el-button>
        <el-button v-else type="primary" :loading="saving" @click="changeStatus(0)">重新上架</el-button>
        <el-button type="danger" plain :loading="deleting" @click="removeProject">删除企划</el-button>
      </div>
      <p class="delete-tip">只有从未产生过任何订单的企划才能删除；已取消或已完成订单也计入关联记录。</p>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { changeProjectStatus, deleteProject } from '@/api/project'

const props = defineProps({ modelValue: Boolean, project: { type: Object, default: null } })
const emit = defineEmits(['update:modelValue', 'changed', 'deleted'])
const saving = ref(false)
const deleting = ref(false)

const cover = computed(() => {
  const value = props.project?.images
  if (!value) return ''
  try {
    const parsed = JSON.parse(value)
    if (Array.isArray(parsed)) return parsed[0] || ''
  } catch { /* 兼容逗号分隔的历史数据 */ }
  return String(value).split(',').map((item) => item.trim()).find(Boolean) || ''
})

async function changeStatus(status) {
  saving.value = true
  try {
    const updated = await changeProjectStatus(props.project.id, status)
    ElMessage.success(status === 0 ? '企划已重新上架' : '企划已下架')
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
.manage-actions { display: flex; gap: 10px; }
</style>
