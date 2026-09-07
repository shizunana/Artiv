<template>
  <main class="project-panel-page">
    <div class="project-panel page-container" v-loading="loading">
    <template v-if="project">
      <section class="panel-summary wb-card">
        <div class="summary-title">
          <div>
            <div class="title-row">
              <h2>{{ project.title }}</h2>
              <el-tag :type="projectState.type">{{ projectState.text }}</el-tag>
            </div>
            <div class="project-meta-row">
              <p>企划 #{{ project.id }} · 发布于 {{ formatDateTime(project.createdAt) }}</p>
              <el-button type="primary" link @click="openDeadlineDialog">
                截稿日期：{{ project.deadline || '未设置' }} · 修改
              </el-button>
            </div>
          </div>
          <div class="summary-actions">
            <el-tooltip :disabled="project.contentEditable !== false" content="存在应征、有效邀请或历史订单时不能完整编辑">
              <span><el-button :disabled="project.contentEditable === false" @click="editProject">编辑企划</el-button></span>
            </el-tooltip>
            <el-button type="primary" plain @click="viewDetail">企划详情</el-button>
          </div>
        </div>

        <div class="summary-grid">
          <div><span>合作方式</span><strong>{{ isDirected ? '定向邀请' : '公开招募' }}</strong></div>
          <div><span>企划类型</span><strong>{{ project.projectCategory || '未分类' }}</strong></div>
          <div><span>预算区间</span><strong class="accent">{{ formatProjectBudget(project) }}</strong></div>
          <div class="recruit-control">
            <span>上架状态</span>
            <el-tooltip :disabled="!switchDisabledReason" :content="switchDisabledReason">
              <div>
                <el-switch
                  :model-value="isRecruiting"
                  :disabled="Boolean(switchDisabledReason)"
                  :loading="changingStatus"
                  @change="toggleRecruitment"
                />
                <small>{{ isRecruiting ? '已上架' : projectState.text }}</small>
              </div>
            </el-tooltip>
          </div>
        </div>
      </section>

      <section class="panel-body wb-card">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="参与画师" name="participants" />
          <el-tab-pane :label="isDirected ? '邀请画师' : '应征画师'" name="applicants" />
          <el-tab-pane :label="isDirected ? '邀请其他画师' : '邀请画师'" :name="isDirected ? 'invite-disabled' : 'invite'" />
        </el-tabs>

        <div v-if="activeTab === 'participants'" class="tab-section">
          <div class="toolbar">
            <div class="status-filters">
              <button v-for="item in orderFilters" :key="item.value" :class="{ active: orderStatus === item.value }" @click="selectOrderStatus(item.value)">{{ item.label }}</button>
            </div>
            <el-input v-model="participantKeyword" clearable placeholder="搜索参与画师" class="painter-search" />
          </div>
          <div v-loading="loadingOrders" class="participant-groups">
            <el-empty v-if="!filteredGroups.length && !loadingOrders" description="暂无符合条件的参与画师" />
            <article v-for="group in filteredGroups" :key="group.painter.id" class="participant-group">
              <header class="participant-header">
                <button type="button" class="painter-identity" @click="openPainter(group.painter.id)">
                  <el-avatar :size="50" :src="group.painter.avatar">{{ group.painter.nickname.slice(0, 1) }}</el-avatar>
                  <span><strong>{{ group.painter.nickname }}</strong><small>{{ group.orders.length }} 笔订单 · {{ group.summary }}</small></span>
                </button>
              </header>
              <OrderListItem
                v-for="order in group.orders"
                :key="order.id"
                :order="order"
                participant-role="employer"
                :show-source-detail="false"
                @open-order="openOrder"
              />
            </article>
          </div>
        </div>

        <div v-else-if="activeTab === 'applicants'" class="tab-section" v-loading="loadingApplicants">
          <div class="section-heading">
            <div><h3>{{ isDirected ? '邀请画师' : '应征画师' }}</h3><p>{{ isDirected ? '展示本次定向企划的唯一目标画师与回应状态。' : '管理当前仍有效的应征关系。' }}</p></div>
            <span>共 {{ applicantTotal }} 名</span>
          </div>
          <ProjectApplicantList
            v-if="!loadingApplicants"
            :items="applicants"
            show-actions
            :starting-chat-id="startingChatId"
            :ordering-application-id="orderingApplicationId"
            :empty-description="isDirected ? '暂无邀请画师记录' : '暂无画师应征'"
            @open-painter="openPainter"
            @chat="chatWith"
            @order="orderAction"
          />
        </div>

        <div v-else-if="activeTab === 'invite'" class="tab-section">
          <div class="section-heading">
            <div><h3>邀请画师</h3><p>默认展示您已关注的认证画师，也可以按昵称搜索。</p></div>
            <el-button @click="loadFollowed">关注的画师</el-button>
          </div>
          <el-input v-model="inviteKeyword" class="invite-search" clearable placeholder="搜索画师昵称" @keyup.enter="searchPainters">
            <template #append><el-button @click="searchPainters">搜索</el-button></template>
          </el-input>
          <div v-loading="loadingPainters" class="invite-grid">
            <el-empty v-if="!painters.length && !loadingPainters" description="暂无可显示的认证画师" />
            <article v-for="painter in painters" :key="painter.id" class="invite-painter-card">
              <button type="button" class="painter-identity" @click="openPainter(painter.id)">
                <el-avatar :size="52" :src="painter.avatar">{{ displayPainterName(painter).slice(0, 1) }}</el-avatar>
                <span><strong>{{ displayPainterName(painter) }}</strong><small>{{ painter.bio || '暂未填写简介' }}</small></span>
              </button>
              <el-button type="primary" plain @click="openInvite(painter)">邀请</el-button>
            </article>
          </div>
        </div>

        <div v-else class="disabled-invite">
          <el-empty description="定向企划不开放邀请其他画师">
            <p>如需邀请其他画师，请重新发布公开企划；被拒绝的定向企划可通过上架开关转为公开企划。</p>
          </el-empty>
        </div>
      </section>
    </template>

    <el-dialog v-model="deadlineVisible" title="修改截稿日期" width="460px" append-to-body>
      <p class="deadline-tip">截稿日期决定企划允许公开招募或接受邀请的有效时间，不影响已经建立的应征、会话和订单。</p>
      <el-date-picker
        v-model="deadlineDraft"
        type="date"
        value-format="YYYY-MM-DD"
        format="YYYY-MM-DD"
        :disabled-date="disableBeforeProjectDeadline"
        placeholder="请选择新的截稿日期"
        style="width: 100%"
      />
      <template #footer>
        <el-button @click="deadlineVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingDeadline" @click="saveDeadline">保存截稿日期</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="inviteVisible" :title="`邀请 ${displayPainterName(invitePainter || {})}`" width="520px" @closed="resetInviteForm">
      <el-form label-position="top">
        <el-form-item label="邀请类型">
          <div class="invite-types">
            <button type="button" :class="{ active: inviteForm.invitationType === 1 }" @click="inviteForm.invitationType = 1"><strong>邀请报价</strong><small>邀请画师查看企划并自行报价</small></button>
            <button type="button" :class="{ active: inviteForm.invitationType === 2 }" @click="inviteForm.invitationType = 2"><strong>带价邀请</strong><small>由您预先提供合作价格</small></button>
          </div>
        </el-form-item>
        <el-form-item v-if="inviteForm.invitationType === 2" label="邀请价格（最低 ¥50）">
          <el-input-number v-model="inviteForm.offeredPrice" :min="50" :precision="2" :step="50" />
        </el-form-item>
        <el-form-item label="附言（选填）">
          <el-input v-model="inviteForm.message" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="可以补充希望画师关注的需求重点" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="inviteVisible = false">取消</el-button><el-button type="primary" :loading="sendingInvite" @click="sendInvite">发送邀请</el-button></template>
    </el-dialog>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  changeProjectStatus,
  createProjectOrder,
  getApplications,
  getProjectPanel,
  publishProjectPublic,
  updateProjectDeadline,
} from '@/api/project'
import { getOrders } from '@/api/order'
import { createPublicInvitation } from '@/api/invitation'
import { getFollowedPainters } from '@/api/social'
import { getArtists } from '@/api/user'
import { startProjectConversation } from '@/api/message'
import { formatDateTime } from '@/utils/date'
import { formatProjectBudget } from '@/utils/budget'
import {
  disableBeforeProjectDeadline,
  isProjectExpired,
  minimumProjectDeadlineText,
} from '@/utils/projectDeadline'
import ProjectApplicantList from '@/components/projects/ProjectApplicantList.vue'
import OrderListItem from '@/components/orders/OrderListItem.vue'

const route = useRoute()
const router = useRouter()
const project = ref(null)
const loading = ref(true)
const activeTab = ref('applicants')
const deadlineVisible = ref(false)
const deadlineDraft = ref('')
const savingDeadline = ref(false)
const changingStatus = ref(false)
const applicants = ref([])
const applicantTotal = ref(0)
const loadingApplicants = ref(false)
const orders = ref([])
const loadingOrders = ref(false)
const orderStatus = ref('all')
const participantKeyword = ref('')
const startingChatId = ref(null)
const orderingApplicationId = ref(null)
const painters = ref([])
const loadingPainters = ref(false)
const inviteKeyword = ref('')
const inviteVisible = ref(false)
const invitePainter = ref(null)
const sendingInvite = ref(false)
const inviteForm = reactive({ invitationType: 1, offeredPrice: 50, message: '' })
const orderFilters = [
  { label: '全部', value: 'all' }, { label: '待接单', value: 'pending' },
  { label: '进行中', value: 'inProgress' }, { label: '待验收', value: 'pendingReview' },
  { label: '已完成', value: 'completed' }, { label: '已取消', value: 'cancelled' },
  { label: '售后中', value: 'afterSale' },
]
const isDirected = computed(() => Number(project.value?.mode) === 1)
const moderated = computed(() => Number(project.value?.moderationStatus) === 1)
const pendingDirectedInvitation = computed(() => isDirected.value && Number(project.value?.invitationStatus) === 0)
const rejectedDirectedInvitation = computed(() => isDirected.value && Number(project.value?.invitationStatus) === 2)
const isRecruiting = computed(() => Number(project.value?.status) === 0 && !moderated.value && !isProjectExpired(project.value?.deadline))
const switchDisabledReason = computed(() => {
  if (moderated.value) return '违规下架的企划不能由用户重新上架'
  if (pendingDirectedInvitation.value) return '存在待回应定向邀请时不能下架企划'
  return ''
})
const projectState = computed(() => {
  if (moderated.value) return { text: '违规下架', type: 'danger' }
  if (Number(project.value?.status) === 4) return { text: Number(project.value?.invitationStatus) === 2 ? '已拒绝' : '已下架', type: 'info' }
  if (isProjectExpired(project.value?.deadline)) return { text: '已过期', type: 'info' }
  if (isDirected.value) return ({ 0: { text: '待回应', type: 'warning' }, 1: { text: '已接受', type: 'success' }, 2: { text: '已拒绝', type: 'danger' } })[Number(project.value?.invitationStatus)] || { text: '招募中', type: 'success' }
  return { text: '招募中', type: 'success' }
})
const groupedOrders = computed(() => {
  const groups = new Map()
  orders.value.forEach((order) => {
    const painter = order.painter || { id: order.painterId, nickname: `画师 #${order.painterId}`, avatar: '' }
    if (!groups.has(painter.id)) groups.set(painter.id, { painter, orders: [] })
    groups.get(painter.id).orders.push(order)
  })
  return [...groups.values()].map((group) => ({ ...group, summary: summarize(group.orders) }))
})
const filteredGroups = computed(() => {
  const keyword = participantKeyword.value.trim().toLowerCase()
  return keyword ? groupedOrders.value.filter((group) => group.painter.nickname.toLowerCase().includes(keyword)) : groupedOrders.value
})

function money(value) { return Number(value || 0).toFixed(2) }
function displayPainterName(painter) { return painter.nickname || painter.username || `画师 #${painter.id || ''}` }
function painterName(item) { return displayPainterName(item.painter || {}) }
function summarize(list) {
  const labels = { 0: '待接单', 1: '进行中', 2: '待验收', 3: '已完成', 4: '已取消', 5: '售后中' }
  const counts = list.reduce((map, item) => map.set(item.status, (map.get(item.status) || 0) + 1), new Map())
  return [...counts.entries()].map(([status, count]) => `${labels[status] || '其他'} ${count} 笔`).join(' · ')
}
function viewDetail() { router.push({ name: 'ProjectDetail', params: { id: project.value.id } }) }
function editProject() { router.push({ name: 'ProjectEdit', params: { id: project.value.id } }) }
function openPainter(id) { if (id) router.push({ name: 'ArtistProfile', params: { id }, query: { role: 'painter' } }) }
function openOrder(id) { router.push({ name: 'DashboardOrderDetail', params: { id } }) }
function openDeadlineDialog() {
  deadlineDraft.value = project.value?.deadline && !isProjectExpired(project.value.deadline)
    ? String(project.value.deadline).slice(0, 10)
    : minimumProjectDeadlineText()
  deadlineVisible.value = true
}
async function saveDeadline() {
  if (!deadlineDraft.value || deadlineDraft.value < minimumProjectDeadlineText()) {
    return ElMessage.warning(`截稿日期不能早于 ${minimumProjectDeadlineText()}`)
  }
  savingDeadline.value = true
  try {
    await updateProjectDeadline(project.value.id, deadlineDraft.value)
    ElMessage.success('截稿日期已更新')
    deadlineVisible.value = false
    await loadProject()
  } finally { savingDeadline.value = false }
}
async function toggleRecruitment(nextValue) {
  if (changingStatus.value) return
  if (nextValue && (!project.value.deadline || project.value.deadline < minimumProjectDeadlineText())) {
    ElMessage.warning('重新上架前请先设置新的截稿日期')
    openDeadlineDialog()
    return
  }
  changingStatus.value = true
  try {
    if (!nextValue) {
      await changeProjectStatus(project.value.id, 4)
      ElMessage.success('企划已下架')
    } else if (rejectedDirectedInvitation.value) {
      await publishProjectPublic(project.value.id, project.value.deadline)
      ElMessage.success('企划已转为公开企划并重新上架')
    } else {
      await changeProjectStatus(project.value.id, 0, project.value.deadline)
      ElMessage.success('企划已重新上架')
    }
    await loadProject()
  } catch {
    await loadProject()
  } finally { changingStatus.value = false }
}
async function handleTabChange(name) {
  if (name === 'participants') await loadOrders()
  else if (name === 'applicants') await loadApplicants()
  else if (name === 'invite' && !painters.value.length) await loadFollowed()
}
async function loadProject() {
  project.value = await getProjectPanel(route.params.id)
  if (!activeTab.value || (isDirected.value && activeTab.value === 'invite')) activeTab.value = 'applicants'
}
async function loadApplicants() {
  loadingApplicants.value = true
  try {
    const data = await getApplications(route.params.id, { page: 1, size: 50 })
    applicants.value = data.records || []
    applicantTotal.value = data.total || 0
  } finally { loadingApplicants.value = false }
}
async function loadOrders() {
  loadingOrders.value = true
  try {
    const data = await getOrders({ page: 1, size: 200, source: 'project', participantRole: 'employer', projectId: route.params.id, statusGroup: orderStatus.value })
    orders.value = data.records || []
  } finally { loadingOrders.value = false }
}
async function selectOrderStatus(value) { orderStatus.value = value; await loadOrders() }
async function chatWith(item) {
  startingChatId.value = item.painter.id
  try {
    await startProjectConversation(project.value.id, item.painter.id)
    await router.push({ name: 'Messages', query: { withUserId: item.painter.id, projectId: project.value.id } })
  } finally { startingChatId.value = null }
}
async function orderAction(item) {
  if (item.hasActiveOrder) return openOrder(item.activeOrderId)
  const confirmed = await ElMessageBox.confirm(
    `将按画师报价 ¥${money(item.application.quote)} 托管金额并发送企划订单，是否继续？`,
    '确认发送企划订单', { type: 'warning', confirmButtonText: '确认发送', cancelButtonText: '取消' },
  ).then(() => true).catch(() => false)
  if (!confirmed) return
  orderingApplicationId.value = item.application.id
  try {
    const order = await createProjectOrder(project.value.id, item.application.id)
    ElMessage.success('企划订单已发送')
    await loadApplicants()
    openOrder(order.id)
  } finally { orderingApplicationId.value = null }
}
async function loadFollowed() {
  loadingPainters.value = true
  try { const data = await getFollowedPainters({ page: 1, size: 50 }); painters.value = data.records || [] }
  finally { loadingPainters.value = false }
}
async function searchPainters() {
  const keyword = inviteKeyword.value.trim()
  if (!keyword) return loadFollowed()
  loadingPainters.value = true
  try { const data = await getArtists({ page: 1, size: 30, keyword }); painters.value = data.records || [] }
  finally { loadingPainters.value = false }
}
function openInvite(painter) { invitePainter.value = painter; inviteVisible.value = true }
function resetInviteForm() { inviteForm.invitationType = 1; inviteForm.offeredPrice = 50; inviteForm.message = ''; invitePainter.value = null }
async function sendInvite() {
  if (inviteForm.invitationType === 2 && Number(inviteForm.offeredPrice) < 50) return ElMessage.warning('邀请价格不能低于 50 元')
  sendingInvite.value = true
  try {
    await createPublicInvitation({ projectId: project.value.id, painterId: invitePainter.value.id, invitationType: inviteForm.invitationType, offeredPrice: inviteForm.invitationType === 2 ? inviteForm.offeredPrice : null, message: inviteForm.message })
    ElMessage.success('邀请已发送')
    inviteVisible.value = false
  } finally { sendingInvite.value = false }
}
async function initialize() {
  loading.value = true
  try { await loadProject(); activeTab.value = 'applicants'; await loadApplicants() }
  finally { loading.value = false }
}
onMounted(initialize)
</script>

<style scoped>
.project-panel-page { min-height: calc(100vh - var(--artiv-header-height)); padding: 28px 0 48px; background: #f4f7f9; }
.project-panel { display: grid; min-width: 0; gap: 16px; }
.wb-card { min-width: 0; padding: 24px; border-radius: 8px; background: #fff; }
.summary-title,.title-row,.summary-actions,.toolbar,.section-heading,.participant-header,.project-meta-row { display: flex; align-items: center; }
.summary-title,.toolbar,.section-heading { justify-content: space-between; gap: 18px; }
.summary-title { align-items: flex-start; }
.title-row { gap: 10px; }
.title-row h2 { margin: 0; font-size: 22px; }
.summary-title p,.section-heading p { margin: 7px 0 0; color: var(--artiv-text-secondary); font-size: 13px; }
.project-meta-row { flex-wrap: wrap; gap: 10px; }
.project-meta-row :deep(.el-button) { height: auto; margin-top: 7px; padding: 0; }
.summary-actions { flex-wrap: wrap; justify-content: flex-end; gap: 8px; }
.summary-actions :deep(.el-button + .el-button) { margin-left: 0; }
.summary-grid { display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); gap: 0; margin-top: 22px; padding-top: 20px; border-top: 1px solid var(--artiv-border); }
.summary-grid > div { display: grid; gap: 7px; padding: 0 18px; border-right: 1px solid var(--artiv-border); }
.summary-grid > div:first-child { padding-left: 0; }.summary-grid > div:last-child { border-right: 0; }
.summary-grid span,.summary-grid small { color: var(--artiv-text-secondary); font-size: 12px; }.summary-grid strong { font-size: 14px; }.summary-grid .accent { color: var(--artiv-accent); font-size: 17px; }
.recruit-control > div { display: flex; align-items: center; gap: 8px; }
.deadline-tip { margin: 0 0 16px; color: var(--artiv-text-secondary); font-size: 13px; line-height: 1.7; }
.panel-body :deep(.el-tabs__header) { margin-bottom: 18px; }
.tab-section { min-height: 380px; }
.status-filters { display: flex; min-width: 0; gap: 8px; overflow-x: auto; }
.status-filters button { flex: 0 0 auto; padding: 7px 15px; border: 1px solid var(--artiv-border); border-radius: 18px; background: #fff; color: var(--artiv-text-secondary); cursor: pointer; }
.status-filters button.active { border-color: var(--artiv-primary); background: var(--artiv-primary); color: #fff; }
.painter-search { width: 220px; }.participant-groups { min-height: 320px; margin-top: 14px; }.participant-group { margin-top: 14px; padding: 18px; border: 1px solid var(--artiv-border); border-radius: 8px; }
.participant-header { justify-content: space-between; padding-bottom: 10px; border-bottom: 1px solid var(--artiv-border); }
.painter-identity,.applicant-identity { display: flex; min-width: 0; align-items: center; gap: 12px; border: 0; background: transparent; color: inherit; cursor: pointer; text-align: left; }
.painter-identity span,.applicant-identity span { display: grid; min-width: 0; gap: 5px; }.painter-identity small,.applicant-identity small { overflow: hidden; color: var(--artiv-text-secondary); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.section-heading { margin-bottom: 16px; }.section-heading h3 { margin: 0; font-size: 18px; }.section-heading > span { color: var(--artiv-text-secondary); }
.applicant-list { display: grid; gap: 12px; }.applicant-card { display: grid; grid-template-columns: minmax(220px,1fr) 120px 260px auto; align-items: center; gap: 18px; padding: 16px; border: 1px solid var(--artiv-border); border-radius: 8px; }
.application-data { display: grid; gap: 7px; }.application-data span { color: var(--artiv-text-secondary); font-size: 12px; }.application-data strong { color: var(--artiv-accent); }.application-data .warning { color: #e6a23c; }
.work-strip { display: flex; gap: 6px; }.work-strip :deep(.el-image) { width: 56px; height: 56px; border-radius: 4px; }.applicant-actions { display: flex; flex-direction: column; gap: 8px; }.applicant-actions :deep(.el-button + .el-button) { margin-left: 0; }
.invite-search { max-width: 620px; margin-bottom: 16px; }.invite-grid { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 12px; min-height: 280px; }.invite-painter-card { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 14px; border: 1px solid var(--artiv-border); border-radius: 8px; }
.disabled-invite { min-height: 360px; }.disabled-invite p { color: var(--artiv-text-secondary); font-size: 13px; text-align: center; }
.invite-types { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }.invite-types button { display: grid; gap: 6px; padding: 14px; border: 1px solid var(--artiv-border); border-radius: 8px; background: #fff; color: var(--artiv-text); cursor: pointer; text-align: left; }.invite-types button.active { border-color: var(--artiv-primary); background: #f2fbff; }.invite-types small { color: var(--artiv-text-secondary); }
@media(max-width:900px){.summary-grid{grid-template-columns:repeat(2,1fr);row-gap:18px}.summary-grid>div{border-right:0;padding:0}.applicant-card{grid-template-columns:1fr 120px}.work-strip{grid-column:1/-1}.invite-grid{grid-template-columns:1fr}}
@media(max-width:640px){.summary-title,.toolbar{align-items:stretch;flex-direction:column}.summary-actions{justify-content:flex-start}.summary-grid{grid-template-columns:1fr}.painter-search{width:100%}.applicant-card{grid-template-columns:1fr}.application-data,.applicant-actions{grid-column:1}.work-strip{overflow-x:auto}.wb-card{padding:16px}}
</style>
