<template>
  <div class="my-invitations wb-card">
    <header class="invitation-header">
      <div>
        <h2>收到的邀请</h2>
        <p>查看企划方发送给我的定向邀请提醒。</p>
      </div>
      <el-button @click="load">刷新</el-button>
    </header>

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

    <div v-loading="loading" class="invitation-cards">
      <el-empty v-if="!invitations.length" :description="emptyDescription" />
      <article
        v-for="invitation in invitations"
        :key="invitation.invitationId"
        class="invitation-card"
        @click="openProject(invitation.projectId)"
      >
        <img v-if="invitation.cover" :src="invitation.cover" alt="企划封面" />
        <div v-else class="invitation-cover">企划</div>

        <div class="invitation-main">
          <div class="invitation-title-row">
            <h3 :title="invitation.projectTitle">{{ invitation.projectTitle || '未命名企划' }}</h3>
            <el-tag :type="invitationStatus(invitation.status).type" size="small">
              {{ invitationStatus(invitation.status).text }}
            </el-tag>
          </div>
          <p>类型：{{ invitation.category || '未分类' }} · 邀请时间：{{ formatDateTime(invitation.createdAt) }}</p>
          <p class="invitation-state">状态：{{ invitationStatus(invitation.status).text }}</p>
        </div>

        <div class="invitation-budget">
          <small>预算</small>
          <strong>{{ formatProjectBudget(invitation) }}</strong>
          <div class="invitation-actions">
            <el-button type="primary" link @click.stop="openProject(invitation.projectId)">企划详情</el-button>
            <el-button
              type="danger"
              link
              :loading="hidingIds.has(invitation.invitationId)"
              @click.stop="hide(invitation)"
            >删除</el-button>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { getInvitations, hideInvitation } from '@/api/invitation'
import { formatProjectBudget } from '@/utils/budget'
import { formatDateTime } from '@/utils/date'

const router = useRouter()
const filters = [
  { label: '全部邀请', value: null },
  { label: '待处理', value: 0 },
  { label: '已接受', value: 1 },
  { label: '已取消', value: 2 },
]
const statusFilter = ref(null)
const listState = ref({ queryKey: 'all', records: [] })
const loadingQueryKey = ref('')
const hidingIds = ref(new Set())
let activeRequestController = null
let requestSequence = 0

const currentQueryKey = computed(() => statusFilter.value === null ? 'all' : String(statusFilter.value))
const invitations = computed(() => listState.value.queryKey === currentQueryKey.value ? listState.value.records : [])
const loading = computed(() => loadingQueryKey.value === currentQueryKey.value)
const emptyDescription = computed(() => {
  const label = filters.find((item) => item.value === statusFilter.value)?.label || '邀请'
  return statusFilter.value === null ? '暂未收到定向邀请' : `暂无${label}记录`
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
    2: { text: '已取消', type: 'info' },
  }[Number(value)] || { text: '未知', type: 'info' }
}

function openProject(projectId) {
  router.push({ name: 'ProjectDetail', params: { id: projectId } })
}

async function hide(invitation) {
  try {
    await ElMessageBox.confirm(
      '删除后该邀请将不再显示在“收到的邀请”中，但不会影响已建立的会话、订单或其他业务记录。',
      '删除邀请提醒',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }

  const invitationId = invitation.invitationId
  hidingIds.value = new Set(hidingIds.value).add(invitationId)
  try {
    await hideInvitation(invitationId)
    if (listState.value.queryKey === currentQueryKey.value) {
      listState.value = {
        ...listState.value,
        records: listState.value.records.filter((item) => item.invitationId !== invitationId),
      }
    }
    ElMessage.success('邀请提醒已删除')
  } finally {
    const next = new Set(hidingIds.value)
    next.delete(invitationId)
    hidingIds.value = next
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
.status-filters { display: flex; gap: 10px; margin-top: 22px; padding: 4px 0 18px; border-bottom: 1px solid var(--artiv-border); }
.status-filters button { flex: 0 0 auto; border: 1px solid var(--artiv-border); border-radius: 18px; background: #fff; padding: 7px 18px; color: var(--artiv-text-secondary); cursor: pointer; }
.status-filters button.active { border-color: var(--artiv-primary); background: var(--artiv-primary); color: #fff; }
.invitation-cards { min-width: 0; min-height: 360px; padding-top: 16px; }
.invitation-card { display: grid; width: 100%; box-sizing: border-box; grid-template-columns: 92px minmax(0, 1fr) minmax(220px, 240px); align-items: center; gap: 18px; padding: 16px 0; border-bottom: 1px solid var(--artiv-border); cursor: pointer; }
.invitation-card:hover { background: #fbfdff; }
.invitation-card img,
.invitation-cover { width: 92px; height: 72px; border-radius: 7px; object-fit: cover; }
.invitation-cover { display: grid; place-items: center; background: var(--artiv-section-alt); color: var(--artiv-text-secondary); }
.invitation-main { min-width: 0; }
.invitation-title-row { display: flex; min-width: 0; align-items: center; gap: 10px; }
.invitation-title-row h3 { min-width: 0; margin: 0; overflow: hidden; font-size: 16px; text-overflow: ellipsis; white-space: nowrap; }
.invitation-main p { margin: 8px 0 0; color: var(--artiv-text-secondary); font-size: 13px; }
.invitation-main .invitation-state { color: var(--artiv-text); }
.invitation-budget { display: grid; min-width: 0; justify-items: end; gap: 7px; }
.invitation-budget small { color: var(--artiv-text-secondary); }
.invitation-budget strong { color: var(--artiv-accent); font-size: 18px; }
.invitation-actions { display: flex; width: 100%; flex-wrap: wrap; justify-content: flex-end; gap: 4px 8px; }
.invitation-actions :deep(.el-button + .el-button) { margin-left: 0; }
@media (max-width: 720px) {
  .invitation-card { grid-template-columns: 70px minmax(0, 1fr); }
  .invitation-card img,
  .invitation-cover { width: 70px; }
  .invitation-budget { grid-column: 2; justify-items: start; }
  .invitation-actions { justify-content: flex-start; }
  .status-filters { overflow-x: auto; }
}
</style>
