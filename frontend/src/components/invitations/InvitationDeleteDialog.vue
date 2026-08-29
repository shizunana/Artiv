<template>
  <el-dialog
    v-model="visible"
    title="删除邀请"
    width="720px"
    append-to-body
    align-center
    destroy-on-close
    @open="loadInvitations"
    @closed="reset"
  >
    <el-alert
      title="删除后邀请将不再显示，但不会影响已建立的会话、订单或其他业务记录。"
      type="info"
      :closable="false"
      show-icon
    />

    <div v-loading="loading" class="delete-invitation-list">
      <el-empty v-if="!loading && !invitations.length" description="当前没有可删除的邀请提醒" />
      <article v-for="invitation in invitations" :key="invitation.invitationId" class="delete-invitation-row">
        <el-checkbox
          :model-value="selectedIds.includes(invitation.invitationId)"
          @change="toggleSelection(invitation.invitationId, $event)"
        />
        <div class="invitation-name">
          <strong :title="invitation.projectTitle">{{ invitation.projectTitle || '未命名企划' }}</strong>
          <small>邀请 #{{ invitation.invitationId }}</small>
        </div>
        <div><small>企划类型</small><strong>{{ invitation.category || '未分类' }}</strong></div>
        <div><small>邀请类型</small><strong>{{ invitationType(invitation.invitationType) }}</strong></div>
        <div><small>邀请状态</small><el-tag :type="invitationStatus(invitation.status).type">{{ invitationStatus(invitation.status).text }}</el-tag></div>
        <div><small>邀请时间</small><strong>{{ formatDateTime(invitation.createdAt) }}</strong></div>
      </article>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="danger" :disabled="!selectedIds.length" :loading="deleting" @click="confirmDelete">
        删除邀请（{{ selectedIds.length }}）
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getInvitations, hideInvitation } from '@/api/invitation'
import { formatDateTime } from '@/utils/date'

const props = defineProps({ modelValue: { type: Boolean, default: false } })
const emit = defineEmits(['update:modelValue', 'deleted'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})
const invitations = ref([])
const selectedIds = ref([])
const loading = ref(false)
const deleting = ref(false)

function invitationStatus(value) {
  return {
    0: { text: '待处理', type: 'warning' },
    1: { text: '已接受', type: 'success' },
    2: { text: '已拒绝', type: 'info' },
    3: { text: '已失效', type: 'info' },
  }[Number(value)] || { text: '未知', type: 'info' }
}

function invitationType(value) {
  return { 0: '定向邀请', 1: '邀请报价', 2: '带价邀请' }[Number(value)] || '企划邀请'
}

function toggleSelection(id, checked) {
  selectedIds.value = checked
    ? [...selectedIds.value, id]
    : selectedIds.value.filter((item) => item !== id)
}

async function loadInvitations() {
  loading.value = true
  selectedIds.value = []
  try {
    const data = await getInvitations({ page: 1, size: 200 })
    invitations.value = data.records || []
  } finally {
    loading.value = false
  }
}

async function confirmDelete() {
  const confirmed = await ElMessageBox.confirm(
    `确定删除已选中的 ${selectedIds.value.length} 条邀请提醒吗？`,
    '确认删除邀请',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' },
  ).then(() => true).catch(() => false)
  if (!confirmed) return

  deleting.value = true
  const succeeded = []
  const failed = []
  try {
    for (const id of selectedIds.value) {
      try {
        await hideInvitation(id)
        succeeded.push(id)
      } catch {
        failed.push(id)
      }
    }
    invitations.value = invitations.value.filter((item) => !succeeded.includes(item.invitationId))
    selectedIds.value = [...failed]
    if (succeeded.length) {
      ElMessage.success(`已删除 ${succeeded.length} 条邀请提醒`)
      emit('deleted', succeeded)
    }
    if (failed.length) ElMessage.warning(`${failed.length} 条邀请提醒删除失败，请稍后重试`)
    if (!failed.length) visible.value = false
  } finally {
    deleting.value = false
  }
}

function reset() {
  invitations.value = []
  selectedIds.value = []
}
</script>

<style scoped>
.delete-invitation-list { min-height: 280px; max-height: 480px; margin-top: 16px; overflow-y: auto; }
.delete-invitation-row { display: grid; grid-template-columns: 28px minmax(150px, 1.5fr) repeat(4, minmax(100px, 1fr)); align-items: center; gap: 16px; padding: 17px 10px; border-bottom: 1px solid var(--artiv-border); }
.delete-invitation-row > div { display: grid; min-width: 0; gap: 6px; }
.delete-invitation-row small { color: var(--artiv-text-secondary); font-size: 12px; }
.delete-invitation-row strong { overflow: hidden; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.invitation-name > strong { font-size: 15px; }
@media (max-width: 720px) {
  .delete-invitation-row { grid-template-columns: 28px minmax(0, 1fr) minmax(90px, .8fr); }
  .delete-invitation-row > div:nth-of-type(n + 3) { display: none; }
}
</style>
