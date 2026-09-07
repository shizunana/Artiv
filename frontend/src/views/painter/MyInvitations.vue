<template>
  <div class="my-invitations wb-card">
    <header class="invitation-header">
      <div>
        <h2>收到的邀请</h2>
        <p>查看企划方发送给我的企划邀请提醒。</p>
      </div>
      <DeleteManageButton @click="deleteVisible = true">删除邀请</DeleteManageButton>
    </header>

    <div class="filter-row">
      <div class="status-filters" aria-label="邀请状态筛选">
        <button
          v-for="item in filters"
          :key="item.label"
          :class="{ active: statusFilter === item.value }"
          @click="selectFilter(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
    </div>

    <div v-loading="loading" class="invitation-cards">
      <el-empty v-if="!invitations.length" :description="emptyDescription" />
      <article
        v-for="invitation in invitations"
        :key="invitation.invitationId"
        class="invitation-card"
        :class="{ 'is-unavailable': invitation.projectAvailable === false }"
        role="link"
        tabindex="0"
        :aria-label="invitation.projectAvailable === false ? `${invitation.projectTitle || '企划'}已删除` : `查看企划 ${invitation.projectTitle || '未命名企划'}`"
        @click="openProject(invitation)"
        @keydown.enter.self="openProject(invitation)"
        @keydown.space.self.prevent="openProject(invitation)"
      >
        <img v-if="invitation.cover" :src="invitation.cover" alt="企划封面" />
        <div v-else class="invitation-cover">企划</div>

        <div class="invitation-main">
          <div class="invitation-title-row">
            <h3 :title="invitation.projectTitle">{{ invitation.projectTitle || '未命名企划' }}</h3>
            <el-tag :type="invitationStatus(invitation.status).type" size="small">
              {{ invitationStatus(invitation.status).text }}
            </el-tag>
            <el-tag class="invitation-type-tag" size="small" effect="plain">
              {{ invitationType(invitation.invitationType) }}
            </el-tag>
          </div>
          <p>类型：{{ invitation.projectCategory || '未分类' }} · 邀请时间：{{ formatDateTime(invitation.createdAt) }}</p>
          <p class="invitation-description" :title="invitation.projectDescription || ''">
            企划简介：{{ truncate(invitation.projectDescription || '暂无简介', 15) }}
          </p>
        </div>

        <div class="invitation-side">
          <div
            v-if="Number(invitation.status) === 0 && invitation.projectAvailable !== false"
            class="invitation-actions"
            aria-label="邀请操作"
          >
            <el-button
              class="invitation-primary-action"
              type="primary"
              round
              :loading="respondingInvitationId === invitation.invitationId"
              @click.stop="openAcceptDialog(invitation)"
            >
              接受
            </el-button>
            <el-button
              class="invitation-secondary-action"
              plain
              round
              :disabled="respondingInvitationId === invitation.invitationId"
              @click.stop="openRejectDialog(invitation)"
            >
              拒绝
            </el-button>
          </div>
          <div class="invitation-budget">
            <small>预算</small>
            <strong>{{ invitationBudget(invitation) }}</strong>
            <span v-if="invitation.projectAvailable === false" class="unavailable-text">企划已删除</span>
          </div>
        </div>
      </article>
    </div>

    <InvitationDeleteDialog v-model="deleteVisible" @deleted="handleDeletedInvitations" />

    <el-dialog v-model="acceptVisible" :title="acceptDialogTitle" width="480px" @closed="resetAcceptForm">
      <el-form ref="acceptFormRef" :model="acceptForm" :rules="acceptRules" label-position="top">
        <el-form-item v-if="!acceptTargetIsPriced" label="报价（元）" prop="quote">
          <el-input-number v-model="acceptForm.quote" :min="0.01" :precision="2" :step="100" controls-position="right" />
        </el-form-item>
        <div v-else class="priced-invitation-quote">
          <span>企划方邀请价格</span>
          <strong>¥{{ formatAmount(acceptTarget?.offeredPrice) }}</strong>
        </div>
        <p class="dialog-hint">确认后将形成有效应征并自动建立或复用双方会话，不会直接生成订单。</p>
      </el-form>
      <template #footer>
        <el-button @click="acceptVisible = false">取消</el-button>
        <el-button type="primary" :loading="responding" @click="submitAccept">确认接受</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="婉拒邀请" width="480px" @closed="resetRejectForm">
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-position="top">
        <el-form-item label="婉拒理由" prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="请向企划方说明无法参与本次企划的原因"
          />
        </el-form-item>
        <p class="dialog-hint">婉拒后不会建立会话，理由将通过系统消息发送给企划方。</p>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="responding" @click="submitReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { acceptInvitation, getInvitations, rejectInvitation } from '@/api/invitation'
import InvitationDeleteDialog from '@/components/invitations/InvitationDeleteDialog.vue'
import DeleteManageButton from '@/components/common/DeleteManageButton.vue'
import { formatProjectBudget } from '@/utils/budget'
import { formatDateTime } from '@/utils/date'

const router = useRouter()
const filters = [
  { label: '全部邀请', value: null },
  { label: '待处理', value: 0 },
  { label: '已接受', value: 1 },
  { label: '已拒绝', value: 2 },
  { label: '已失效', value: 3 },
]
const statusFilter = ref(null)
const listState = ref({ queryKey: 'all', records: [] })
const loadingQueryKey = ref('')
const deleteVisible = ref(false)
const acceptVisible = ref(false)
const acceptTarget = ref(null)
const acceptFormRef = ref()
const acceptForm = reactive({ quote: undefined })
const rejectVisible = ref(false)
const rejectTarget = ref(null)
const rejectFormRef = ref()
const rejectForm = reactive({ reason: '' })
const responding = ref(false)
let activeRequestController = null
let requestSequence = 0

const currentQueryKey = computed(() => statusFilter.value === null ? 'all' : String(statusFilter.value))
const invitations = computed(() => listState.value.queryKey === currentQueryKey.value ? listState.value.records : [])
const loading = computed(() => loadingQueryKey.value === currentQueryKey.value)
const acceptTargetIsPriced = computed(() => Number(acceptTarget.value?.invitationType) === 2)
const acceptDialogTitle = computed(() => ({
  0: '接受定向邀请',
  1: '接受邀请报价',
  2: '接受带价邀请',
}[Number(acceptTarget.value?.invitationType)] || '接受邀请'))
const respondingInvitationId = computed(() => {
  if (!responding.value) return null
  return acceptTarget.value?.invitationId || rejectTarget.value?.invitationId || null
})
const acceptRules = computed(() => ({
  quote: acceptTargetIsPriced.value
    ? []
    : [
        { required: true, message: '请填写报价', trigger: 'change' },
        { type: 'number', min: 0.01, message: '报价必须大于 0', trigger: 'change' },
      ],
}))
const rejectRules = {
  reason: [{ required: true, whitespace: true, message: '请填写婉拒理由', trigger: 'blur' }],
}
const emptyDescription = computed(() => {
  const label = filters.find((item) => item.value === statusFilter.value)?.label || '邀请'
  return statusFilter.value === null ? '暂未收到企划邀请' : `暂无${label}记录`
})

async function load() {
  const params = { page: 1, size: 30 }
  if (statusFilter.value !== null) params.status = statusFilter.value
  const queryKey = currentQueryKey.value
  const requestId = ++requestSequence
  activeRequestController?.abort()
  const controller = new AbortController()
  activeRequestController = controller
  loadingQueryKey.value = queryKey
  try {
    const data = await getInvitations(params, { signal: controller.signal })
    if (requestId !== requestSequence || queryKey !== currentQueryKey.value) return
    listState.value = { queryKey, records: data.records || [] }
  } catch (error) {
    if (error?.code !== 'ERR_CANCELED') throw error
  } finally {
    if (requestId === requestSequence) loadingQueryKey.value = ''
  }
}

function selectFilter(value) {
  statusFilter.value = value
  load()
}

function invitationStatus(value) {
  return {
    0: { text: '待处理', type: 'warning' },
    1: { text: '已接受', type: 'success' },
    2: { text: '已拒绝', type: 'info' },
    3: { text: '已失效', type: 'info' },
  }[Number(value)] || { text: '未知', type: 'info' }
}

function truncate(value, limit) {
  const characters = Array.from(String(value || ''))
  return characters.length > limit ? `${characters.slice(0, limit).join('')}...` : characters.join('')
}

function invitationType(value) {
  return { 0: '定向邀请', 1: '邀请报价', 2: '带价邀请' }[Number(value)] || '企划邀请'
}

function invitationBudget(invitation) {
  if (Number(invitation.invitationType) === 1) return '邀请报价'
  if (Number(invitation.invitationType) === 2) return `¥${formatAmount(invitation.offeredPrice)}`
  return formatProjectBudget(invitation)
}

function formatAmount(value) {
  return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function openProject(invitation) {
  if (invitation.projectAvailable === false) {
    ElMessage.info('原企划已删除，当前仅保留邀请提醒记录')
    return
  }
  router.push({ name: 'ProjectDetail', params: { id: invitation.projectId } })
}

function openAcceptDialog(invitation) {
  acceptTarget.value = invitation
  acceptVisible.value = true
}

function openRejectDialog(invitation) {
  rejectTarget.value = invitation
  rejectVisible.value = true
}

function resetAcceptForm() {
  acceptFormRef.value?.resetFields()
  acceptForm.quote = undefined
  acceptTarget.value = null
}

function resetRejectForm() {
  rejectFormRef.value?.resetFields()
  rejectForm.reason = ''
  rejectTarget.value = null
}

async function submitAccept() {
  const valid = await acceptFormRef.value?.validate().catch(() => false)
  const invitationId = acceptTarget.value?.invitationId
  if (!valid || !invitationId) return
  responding.value = true
  try {
    await acceptInvitation(invitationId, {
      ...(!acceptTargetIsPriced.value ? { quote: acceptForm.quote } : {}),
    })
    ElMessage.success('已接受邀请并建立会话')
    acceptVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error.message || '接受邀请失败')
  } finally {
    responding.value = false
  }
}

async function submitReject() {
  const valid = await rejectFormRef.value?.validate().catch(() => false)
  const invitationId = rejectTarget.value?.invitationId
  if (!valid || !invitationId) return
  responding.value = true
  try {
    await rejectInvitation(invitationId, { reason: rejectForm.reason.trim() })
    ElMessage.success('已婉拒邀请，理由已通知企划方')
    rejectVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error.message || '拒绝邀请失败')
  } finally {
    responding.value = false
  }
}

function handleDeletedInvitations(ids) {
  if (listState.value.queryKey === currentQueryKey.value) {
    listState.value = {
      ...listState.value,
      records: listState.value.records.filter((item) => !ids.includes(item.invitationId)),
    }
  }
}

onMounted(load)
onBeforeUnmount(() => activeRequestController?.abort())
</script>

<style scoped>
.wb-card { min-width: 0; background: #fff; border-radius: 8px; padding: 24px; }
.invitation-header { display: flex; justify-content: space-between; align-items: flex-start; }
.invitation-header h2 { margin: 0 0 7px; font-size: 21px; }
.invitation-header p { margin: 0; color: var(--artiv-text-secondary); }
.filter-row { display: flex; align-items: flex-end; gap: 18px; margin-top: 22px; border-bottom: 1px solid var(--artiv-border); }
.status-filters { display: flex; min-width: 0; flex: 1; gap: 10px; padding: 4px 0 18px; }
.status-filters button { flex: 0 0 auto; border: 1px solid var(--artiv-border); border-radius: 18px; background: #fff; padding: 7px 18px; color: var(--artiv-text-secondary); cursor: pointer; }
.status-filters button.active { border-color: var(--artiv-primary); background: var(--artiv-primary); color: #fff; }
.invitation-cards { min-width: 0; min-height: 360px; padding-top: 16px; }
.invitation-card { display: grid; width: 100%; box-sizing: border-box; grid-template-columns: 92px minmax(0, 1fr) minmax(300px, 320px); align-items: center; gap: 18px; padding: 16px 8px; border-bottom: 1px solid var(--artiv-border); border-radius: 6px; outline: none; cursor: pointer; transition: background-color .18s ease, box-shadow .18s ease; }
.invitation-card:hover,
.invitation-card:focus-visible { background: #f8fcfe; box-shadow: inset 3px 0 0 color-mix(in srgb, var(--artiv-primary) 55%, transparent); }
.invitation-card:hover .invitation-title-row h3 { color: var(--artiv-primary); }
.invitation-card.is-unavailable { cursor: default; }
.invitation-card.is-unavailable:hover,
.invitation-card.is-unavailable:focus-visible { background: #fafafa; box-shadow: inset 3px 0 0 var(--artiv-border); }
.invitation-card img,
.invitation-cover { width: 92px; height: 72px; border-radius: 7px; object-fit: cover; }
.invitation-cover { display: grid; place-items: center; background: var(--artiv-section-alt); color: var(--artiv-text-secondary); }
.invitation-main { min-width: 0; }
.invitation-title-row { display: flex; min-width: 0; align-items: center; gap: 10px; }
.invitation-title-row h3 { min-width: 0; margin: 0; overflow: hidden; font-size: 16px; text-overflow: ellipsis; white-space: nowrap; }
.invitation-type-tag { color: var(--artiv-primary); border-color: #89d7f5; background: #eefaff; }
.invitation-main p { margin: 8px 0 0; color: var(--artiv-text-secondary); font-size: 13px; }
.invitation-main .invitation-description { color: var(--artiv-text); }
.invitation-side { display: flex; min-width: 0; align-items: center; justify-content: flex-end; gap: 14px; }
.invitation-actions { display: flex; flex: 0 0 auto; align-items: center; gap: 8px; }
.invitation-actions :deep(.el-button + .el-button) { margin-left: 0; }
.invitation-primary-action,
.invitation-secondary-action { min-width: 66px; font-weight: 600; }
.invitation-primary-action { border: 0; box-shadow: 0 4px 10px rgb(0 169 232 / 16%); }
.invitation-primary-action:hover { box-shadow: 0 5px 13px rgb(0 169 232 / 23%); }
.invitation-budget { display: grid; min-width: 92px; flex: 0 0 auto; justify-items: end; gap: 7px; }
.invitation-budget small { color: var(--artiv-text-secondary); }
.invitation-budget strong { color: var(--artiv-accent); font-size: 18px; white-space: nowrap; }
.unavailable-text { color: var(--artiv-text-secondary); font-size: 13px; }
.priced-invitation-quote { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px; border-radius: 6px; background: var(--artiv-section-alt); }
.priced-invitation-quote span,
.dialog-hint { color: var(--artiv-text-secondary); }
.priced-invitation-quote strong { color: var(--artiv-accent); font-size: 20px; }
.dialog-hint { margin: 14px 0 0; font-size: 13px; line-height: 1.7; }
@media (max-width: 720px) {
  .invitation-card { grid-template-columns: 70px minmax(0, 1fr); }
  .invitation-card img,
  .invitation-cover { width: 70px; }
  .invitation-side { grid-column: 2; flex-wrap: wrap; justify-content: flex-start; }
  .invitation-budget { justify-items: start; }
  .filter-row { align-items: stretch; flex-direction: column; gap: 8px; }
  .status-filters { overflow-x: auto; }
}
</style>
