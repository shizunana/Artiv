<template>
  <div class="project-detail-page" v-loading="loading">
    <div v-if="project" class="project-detail-shell">
      <aside class="project-sidebar">
        <section class="sidebar-card owner-card" role="button" tabindex="0" @click="openEmployerProfile" @keyup.enter="openEmployerProfile">
          <el-avatar :size="76" :src="employer?.avatar">{{ employerName.slice(0, 1) }}</el-avatar>
          <h2>{{ employerName }}</h2>
          <p>{{ employer?.bio || '正在寻找合适的创作者。' }}</p>
        </section>

        <section class="sidebar-card project-summary-card">
          <div class="summary-item">
            <span>截稿日期</span>
            <strong>{{ project.deadline || '待商议' }}</strong>
          </div>
          <div class="summary-item">
            <span>单张稿酬预算</span>
            <strong class="budget">{{ formatProjectBudget(project, '￥') }}</strong>
          </div>
          <p class="recruit-countdown" :class="{ expired: projectExpired }">{{ deadlineStatus }}</p>
          <p v-if="pendingInvitation && currentInvitation?.message" class="invitation-message">
            <strong>邀请附言</strong>{{ currentInvitation.message }}
          </p>
          <div v-if="pendingInvitation" class="invitation-actions">
            <el-button class="invite-accept-button" type="primary" :disabled="projectExpired" @click="invitationAcceptVisible = true">
              {{ projectExpired ? '邀请已过期' : '接受邀请' }}
            </el-button>
            <el-button class="invite-reject-button" @click="invitationRejectVisible = true">拒绝邀请</el-button>
          </div>
          <el-button
            v-else-if="showApplyButton"
            class="apply-button"
            type="primary"
            :disabled="applyButtonDisabled"
            @click="handleApplyButton"
          >
            {{ applyButtonText }}
          </el-button>
          <el-button v-else-if="acceptedInvitation && currentApplication" class="apply-button" type="primary" plain @click="openQuoteDialog(currentApplication)">更改报价</el-button>
          <el-button v-else-if="acceptedInvitation" class="apply-button" disabled>已接受邀请</el-button>
          <el-button v-else-if="rejectedInvitation" class="apply-button" disabled>已婉拒邀请</el-button>
          <el-button v-if="isOwner" class="manage-button" type="primary" plain @click="openProjectPanel">进入企划面板</el-button>
          <el-button class="back-button" @click="goBack">{{ backButtonText }}</el-button>
        </section>
      </aside>

      <main class="project-main-column">
        <article class="project-main-card">
          <header class="project-heading">
            <h1 class="project-title">{{ project.title }}</h1>
            <div class="project-meta">
              <span>发布于 {{ formatDateTime(project.createdAt) }}</span>
              <span v-if="project.projectCategory">{{ project.projectCategory }}</span>
              <span v-for="subject in projectSubjects" :key="subject">{{ subject }}</span>
            </div>
          </header>

          <section v-if="projectImages.length" class="detail-section reference-section">
            <h2>参考例图</h2>
            <div class="reference-gallery" :class="`gallery-size-${Math.min(projectImages.length, 6)}`">
              <el-image
                v-for="(image, index) in projectImages"
                :key="`${image}-${index}`"
                class="reference-image"
                :src="image"
                :preview-src-list="projectImages"
                :initial-index="index"
                fit="cover"
                lazy
              />
            </div>
          </section>

          <section class="detail-section description-section">
            <h2>需求详情</h2>
            <p>{{ project.description || '暂无详细说明' }}</p>
          </section>

          <section class="detail-section requirements-section">
            <h2>稿件要求</h2>
            <dl class="requirements-grid">
              <div v-for="item in requirementItems" :key="item.label" class="requirement-item">
                <dt>{{ item.label }}</dt>
                <dd>{{ item.value }}</dd>
              </div>
            </dl>
          </section>

          <section class="detail-section phase-section">
            <h2>创作阶段</h2>
            <p class="phase-tip">订单建立后，画师按以下顺序提交稿件；最后一个阶段确认后完成交易并开放原图。</p>
            <el-steps :active="0" align-center class="phase-steps">
              <el-step v-for="(phase, index) in projectPhases" :key="`${phase.name}-${index}`" :title="phase.name" />
            </el-steps>
          </section>
        </article>

        <section class="applicant-panel" v-loading="loadingApplicants">
          <header class="applicant-heading">
            <h2>应征画师列表</h2>
            <span>共 {{ applicantTotal }} 名画师应征</span>
          </header>

          <ProjectApplicantList
            v-if="!loadingApplicants"
            :items="applicants"
            :target-application-id="Number(route.query.applicationId || 0)"
            @open-painter="openPainterProfile"
          />
        </section>
      </main>
    </div>

    <el-empty v-else-if="!loading" description="企划不存在或已下架" />

    <el-dialog v-model="applicationDialogVisible" title="应征企划" width="480px" @closed="resetApplicationForm">
      <el-form ref="applicationFormRef" :model="applicationForm" :rules="applicationRules" label-position="top">
        <el-form-item label="应征介绍" prop="intro">
          <el-input v-model="applicationForm.intro" type="textarea" :rows="5" maxlength="500" show-word-limit placeholder="介绍你的创作思路和相关经验" />
        </el-form-item>
        <el-form-item label="报价（元）" prop="quote">
          <el-input-number v-model="applicationForm.quote" :min="0.01" :precision="2" :step="100" controls-position="right" />
        </el-form-item>
        <p class="application-hint">提交前需至少公开 4 件作品，作品将展示在应征画师列表中。</p>
      </el-form>
      <template #footer>
        <el-button @click="applicationDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingApplication" @click="submitApplication">提交应征</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="invitationAcceptVisible" :title="invitationAcceptTitle" width="480px" @closed="resetInvitationAcceptForm">
      <el-form ref="invitationAcceptFormRef" :model="invitationAcceptForm" :rules="invitationAcceptRules" label-position="top">
        <el-form-item v-if="!isPricedInvitation" label="报价（元）" prop="quote">
          <el-input-number v-model="invitationAcceptForm.quote" :min="0.01" :precision="2" :step="100" controls-position="right" />
        </el-form-item>
        <div v-else class="priced-invitation-quote"><span>企划方邀请价格</span><strong>¥{{ formatAmount(currentInvitation?.offeredPrice) }}</strong></div>
        <p class="application-hint">确认后将形成有效应征并自动建立或复用双方会话，不会直接生成订单。</p>
      </el-form>
      <template #footer>
        <el-button @click="invitationAcceptVisible = false">取消</el-button>
        <el-button type="primary" :loading="respondingInvitation" @click="submitInvitationAccept">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="invitationRejectVisible" title="婉拒邀请" width="480px" @closed="resetInvitationRejectForm">
      <el-form ref="invitationRejectFormRef" :model="invitationRejectForm" :rules="invitationRejectRules" label-position="top">
        <el-form-item label="婉拒理由" prop="reason">
          <el-input v-model="invitationRejectForm.reason" type="textarea" :rows="5" maxlength="500" show-word-limit placeholder="请向企划方说明无法参与本次企划的原因" />
        </el-form-item>
        <p class="application-hint">婉拒后不会建立会话，理由将通过系统消息发送给企划方。</p>
      </el-form>
      <template #footer>
        <el-button @click="invitationRejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="respondingInvitation" @click="submitInvitationReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="quoteDialogVisible" title="更改应征" width="420px">
      <el-form label-position="top">
        <el-alert v-if="applicationQuoteLocked" title="该报价来自企划方的带价邀请，不能由画师修改。" type="info" :closable="false" show-icon />
        <el-form-item v-else label="新报价（元）">
          <el-input-number v-model="quoteForm.quote" :min="0.01" :precision="2" :step="100" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="danger" plain :loading="withdrawingApplicationId === quoteForm.id" @click="withdrawCurrentApplication">取消应征</el-button>
        <el-button @click="quoteDialogVisible = false">关闭</el-button>
        <el-button v-if="!applicationQuoteLocked" type="primary" :loading="updatingQuote" @click="submitQuoteUpdate">保存报价</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  applyProject,
  deleteApplication,
  getApplications,
  getProject,
  updateApplicationQuote,
} from '@/api/project'
import { acceptInvitation, getInvitationByProject, rejectInvitation } from '@/api/invitation'
import { useUserStore } from '@/stores/user'
import ProjectApplicantList from '@/components/projects/ProjectApplicantList.vue'
import { formatDateTime } from '@/utils/date'
import { formatProjectBudget } from '@/utils/budget'
import { isProjectExpired } from '@/utils/projectDeadline'
import { watchRouteResource } from '@/utils/routeResource'
import { projectUsageLabel } from '@/utils/projectUsage'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const project = ref(null)
const employer = ref(null)
const loading = ref(true)
const applicants = ref([])
const applicantTotal = ref(0)
const loadingApplicants = ref(false)
const withdrawingApplicationId = ref(null)
const applicationDialogVisible = ref(false)
const applicationFormRef = ref()
const submittingApplication = ref(false)
const applicationForm = reactive({ intro: '', quote: undefined })
const quoteDialogVisible = ref(false)
const updatingQuote = ref(false)
const quoteForm = reactive({ id: null, quote: undefined })
const applicationRules = {
  intro: [{ required: true, message: '请填写应征介绍', trigger: 'blur' }],
  quote: [
    { required: true, message: '请填写报价', trigger: 'change' },
    { type: 'number', min: 0.01, message: '报价必须大于 0', trigger: 'change' },
  ],
}
const currentInvitation = ref(null)
const invitationAcceptVisible = ref(false)
const invitationAcceptFormRef = ref()
const invitationAcceptForm = reactive({ quote: undefined })
const invitationRejectVisible = ref(false)
const invitationRejectFormRef = ref()
const invitationRejectForm = reactive({ reason: '' })
const respondingInvitation = ref(false)
const invitationAcceptRules = computed(() => ({
  quote: isPricedInvitation.value
    ? []
    : [
        { required: true, message: '请填写报价', trigger: 'change' },
        { type: 'number', min: 0.01, message: '报价必须大于 0', trigger: 'change' },
      ],
}))
const invitationRejectRules = {
  reason: [{ required: true, whitespace: true, message: '请填写婉拒理由', trigger: 'blur' }],
}
let projectLoadVersion = 0

const employerName = computed(() => employer.value?.nickname || `企划方 #${project.value?.employerId || ''}`)
const currentUserId = computed(() => Number(userStore.user?.id || 0))
const isOwner = computed(() => currentUserId.value > 0 && currentUserId.value === Number(project.value?.employerId))
const currentApplication = computed(() => applicants.value.find((item) => Number(item.painter?.id) === currentUserId.value))
const isDirectedProject = computed(() => Number(project.value?.mode) === 1)
const invitationType = computed(() => Number(currentInvitation.value?.invitationType || 0))
const isPublicInvitation = computed(() => [1, 2].includes(invitationType.value))
const isPricedInvitation = computed(() => invitationType.value === 2)
const pendingInvitation = computed(() => Number(currentInvitation.value?.status) === 0)
const acceptedInvitation = computed(() => Number(currentInvitation.value?.status) === 1)
const rejectedInvitation = computed(() => Number(currentInvitation.value?.status) === 2)
const invitationAcceptTitle = computed(() => ({ 0: '接受定向邀请', 1: '接受邀请报价', 2: '接受带价邀请' }[invitationType.value] || '接受邀请'))
const applicationQuoteLocked = computed(() => Number(currentApplication.value?.application?.quoteSource) === 1)
const certificationReady = ref(!userStore.isLoggedIn)
const isCertifiedPainter = computed(() => Number(userStore.user?.certStatus || 0) === 2)
const canApply = computed(() => certificationReady.value
  && userStore.isLoggedIn
  && isCertifiedPainter.value
  && !isOwner.value
  && !currentApplication.value
  && Number(project.value?.status) === 0
  && !projectExpired.value)
const applyButtonDisabled = computed(() => !currentApplication.value && !canApply.value)
const showApplyButton = computed(() => !isOwner.value
  && !isDirectedProject.value
  && (!userStore.isLoggedIn || (certificationReady.value && isCertifiedPainter.value)))
const applyButtonText = computed(() => {
  if (!userStore.isLoggedIn) return '登录后应征企划'
  if (currentApplication.value) return applicationQuoteLocked.value ? '查看应征' : '更改应征'
  if (projectExpired.value || Number(project.value?.status) !== 0) return '应征已截止'
  return '应征企划'
})
const backButtonText = computed(() => currentInvitation.value && !isOwner.value ? '返回收到的邀请' : '返回企划列表')
const projectImages = computed(() => String(project.value?.images || '').split(',').map((item) => item.trim()).filter(Boolean))
const projectSubjects = computed(() => String(project.value?.subject || '').split(',').map((item) => item.trim()).filter(Boolean))
const projectPhases = computed(() => {
  try {
    const parsed = JSON.parse(project.value?.phases || '[]')
    if (Array.isArray(parsed) && parsed.length) {
      return parsed.map((phase) => ({ name: String(phase?.name || '').trim() })).filter((phase) => phase.name)
    }
  } catch { /* 使用默认阶段 */ }
  return [{ name: '初稿' }, { name: '修改' }, { name: '终稿' }]
})
const projectExpired = computed(() => isProjectExpired(project.value?.deadline))
const deadlineStatus = computed(() => {
  if (projectExpired.value) return '已超出截稿日期'
  return `招募有效期至 ${project.value?.deadline}（当天仍可应征）`
})
const requirements = computed(() => {
  if (!project.value?.requirements) return {}
  if (typeof project.value.requirements === 'object') return project.value.requirements
  try { return JSON.parse(project.value.requirements) } catch { return {} }
})
const requirementItems = computed(() => [
  { label: '题材风格', value: projectSubjects.value.join('、') || '风格不限' },
  { label: '尺寸规格', value: requirements.value.size || '需与画师商议' },
  { label: '颜色模式', value: requirements.value.colorMode || '需与画师商议' },
  { label: '稿件格式', value: Array.isArray(requirements.value.formats) ? requirements.value.formats.join('、') : requirements.value.formats || '需与画师商议' },
  { label: '发布权限', value: requirements.value.publishPermission || '需与画师商议' },
  { label: '稿件用途', value: projectUsageLabel(project.value?.usageType) },
])

function handleApplyButton() {
  if (currentApplication.value) {
    openQuoteDialog(currentApplication.value)
  } else {
    applicationDialogVisible.value = true
  }
}

function goBack() {
  if (currentInvitation.value && !isOwner.value) {
    router.push({ name: 'PainterInvitations' })
    return
  }
  router.push({ name: 'Projects' })
}

function openProjectPanel() {
  router.push({ name: 'ProjectPanel', params: { id: project.value.id } })
}

function resetApplicationForm() {
  applicationFormRef.value?.resetFields()
}

function resetInvitationAcceptForm() {
  invitationAcceptFormRef.value?.resetFields()
  invitationAcceptForm.quote = undefined
}

function resetInvitationRejectForm() {
  invitationRejectFormRef.value?.resetFields()
  invitationRejectForm.reason = ''
}

function openPainterProfile(id) {
  if (id) router.push({ name: 'ArtistProfile', params: { id }, query: { role: 'painter' } })
}

function openEmployerProfile() {
  if (project.value?.employerId) {
    router.push({ name: 'ArtistProfile', params: { id: project.value.employerId }, query: { role: 'employer' } })
  }
}

async function submitApplication() {
  const valid = await applicationFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submittingApplication.value = true
  try {
    await applyProject(project.value.id, { intro: applicationForm.intro, quote: applicationForm.quote })
    ElMessage.success('应征提交成功')
    applicationDialogVisible.value = false
    await loadApplicants()
  } catch (error) {
    ElMessage.error(error.message || '应征提交失败')
  } finally {
    submittingApplication.value = false
  }
}

async function loadInvitation(projectId = project.value?.id) {
  currentInvitation.value = null
  if (!userStore.isLoggedIn || isOwner.value) return
  try {
    const invitation = await getInvitationByProject(projectId)
    if (Number(project.value?.id) === Number(projectId)) currentInvitation.value = invitation
  } catch {
    if (Number(project.value?.id) === Number(projectId)) currentInvitation.value = null
  }
}

async function submitInvitationAccept() {
  const valid = await invitationAcceptFormRef.value?.validate().catch(() => false)
  if (!valid || !currentInvitation.value?.invitationId) return
  respondingInvitation.value = true
  try {
    await acceptInvitation(currentInvitation.value.invitationId, {
      ...(!isPricedInvitation.value ? { quote: invitationAcceptForm.quote } : {}),
    })
    ElMessage.success('已接受邀请并建立会话')
    invitationAcceptVisible.value = false
    await Promise.all([loadInvitation(), loadApplicants()])
  } catch (error) {
    ElMessage.error(error.message || '接受邀请失败')
  } finally {
    respondingInvitation.value = false
  }
}

async function submitInvitationReject() {
  const valid = await invitationRejectFormRef.value?.validate().catch(() => false)
  if (!valid || !currentInvitation.value?.invitationId) return
  respondingInvitation.value = true
  try {
    await rejectInvitation(currentInvitation.value.invitationId, { reason: invitationRejectForm.reason.trim() })
    ElMessage.success('已婉拒邀请，理由已通知企划方')
    invitationRejectVisible.value = false
    await loadProject()
  } catch (error) {
    ElMessage.error(error.message || '拒绝邀请失败')
  } finally {
    respondingInvitation.value = false
  }
}

async function loadApplicants(projectId = project.value?.id) {
  loadingApplicants.value = true
  try {
    const data = await getApplications(projectId, { page: 1, size: 50 })
    if (Number(project.value?.id) !== Number(projectId)) return
    applicants.value = data.records || []
    applicantTotal.value = data.total || applicants.value.length
  } catch (error) {
    if (Number(project.value?.id) === Number(projectId)) ElMessage.error(error.message || '应征画师加载失败')
  } finally {
    if (Number(project.value?.id) === Number(projectId)) loadingApplicants.value = false
  }
}

async function withdrawApplication(item) {
  const confirmed = await ElMessageBox.confirm('取消后如需再次应征，需要重新提交报价。确定取消应征吗？', '取消应征', { type: 'warning' })
    .then(() => true)
    .catch(() => false)
  if (!confirmed || !item) return
  withdrawingApplicationId.value = item.application.id
  try {
    await deleteApplication(item.application.id)
    ElMessage.success('已取消应征')
    quoteDialogVisible.value = false
    await loadApplicants()
  } catch (error) {
    ElMessage.error(error.message || '取消应征失败')
  } finally {
    withdrawingApplicationId.value = null
  }
}

function withdrawCurrentApplication() {
  return withdrawApplication(currentApplication.value)
}

function openQuoteDialog(item) {
  if (!item?.application) return
  quoteForm.id = item.application.id
  quoteForm.quote = Number(item.application.quote)
  quoteDialogVisible.value = true
}

function formatAmount(value) {
  return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

async function submitQuoteUpdate() {
  if (!quoteForm.id || !quoteForm.quote || quoteForm.quote <= 0) {
    ElMessage.warning('请输入有效报价')
    return
  }
  updatingQuote.value = true
  try {
    await updateApplicationQuote(quoteForm.id, quoteForm.quote)
    ElMessage.success('报价已更新')
    quoteDialogVisible.value = false
    await loadApplicants()
  } catch (error) {
    ElMessage.error(error.message || '报价更新失败')
  } finally {
    updatingQuote.value = false
  }
}

async function loadProject() {
  const version = ++projectLoadVersion
  const projectId = route.params.id
  loading.value = true
  try {
    const nextProject = await getProject(projectId)
    if (version !== projectLoadVersion) return
    project.value = nextProject
    employer.value = project.value?.employer || null
    await Promise.all([loadInvitation(project.value.id), loadApplicants(project.value.id)])
  } catch (error) {
    if (version === projectLoadVersion) ElMessage.error(error.message || '企划加载失败')
  } finally {
    if (version === projectLoadVersion) loading.value = false
  }
}

function resetRouteInteractions() {
  applicationDialogVisible.value = false
  quoteDialogVisible.value = false
  invitationAcceptVisible.value = false
  invitationRejectVisible.value = false
  applicants.value = []
  applicantTotal.value = 0
  currentInvitation.value = null
  employer.value = null
  project.value = null
}

async function handleApplicationEntry() {
  const applicationId = Number(route.query.applicationId || 0)
  if (!applicationId) return
  await nextTick()
  document.getElementById(`application-${applicationId}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  if (route.query.editApplication === '1') {
    const target = applicants.value.find((item) => Number(item.application?.id) === applicationId)
    openQuoteDialog(target || currentApplication.value)
  }
}

onMounted(async () => {
  if (userStore.isLoggedIn) {
    await userStore.fetchMe().catch(() => null)
  }
  certificationReady.value = true
  await loadProject()
  await handleApplicationEntry()
})
watchRouteResource(route, async () => {
  resetRouteInteractions()
  await loadProject()
  await handleApplicationEntry()
})
</script>

<style scoped>
.project-detail-page { min-height: calc(100vh - 64px); padding: 40px 20px 72px; background: #e5eaef; }
.project-detail-shell { display: grid; grid-template-columns: 226px minmax(0, 1fr); align-items: start; gap: 24px; width: min(1120px, 100%); margin: 0 auto; }
.project-sidebar { position: sticky; top: 82px; display: flex; flex-direction: column; gap: 12px; }
.sidebar-card, .project-main-card, .applicant-panel { background: #fff; border: 1px solid #e6e9ed; border-radius: 5px; box-shadow: 0 1px 2px rgb(29 43 59 / 4%); }
.owner-card { padding: 27px 22px 24px; text-align: center; }
.owner-card h2 { margin: 14px 0 7px; color: #444; font-size: 17px; }
.owner-card p { margin: 0; color: #888; font-size: 13px; line-height: 1.7; }
.project-summary-card { padding: 24px 22px 18px; }
.summary-item + .summary-item { margin-top: 22px; }
.summary-item span, .summary-item strong { display: block; }
.summary-item span { margin-bottom: 7px; color: #999; font-size: 13px; }
.summary-item strong { color: #444; font-size: 18px; font-weight: 500; }
.summary-item .budget { color: #09a3df; font-size: 21px; font-weight: 600; }
.recruit-countdown { margin: 22px 0 10px; color: #8b939b; font-size: 12px; text-align: center; }
.recruit-countdown.expired { color: #f56c6c; font-weight: 600; }
.invitation-message { padding: 10px 12px; margin: 0 0 12px; color: var(--artiv-text-secondary); font-size: 12px; line-height: 1.6; overflow-wrap: anywhere; background: #f2faff; border: 1px solid #c7eafa; border-radius: 6px; }
.invitation-message strong { display: block; margin-bottom: 2px; color: var(--artiv-primary); }
.apply-button, .manage-button, .back-button { width: 100%; box-sizing: border-box; }
.apply-button { height: 42px; font-size: 15px; }
.invitation-actions { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; width: 100%; }
.invitation-actions :deep(.el-button) { width: 100%; min-width: 0; height: 42px; margin: 0; box-sizing: border-box; font-size: 15px; }
.priced-invitation-quote { display: flex; align-items: center; justify-content: space-between; padding: 15px 18px; margin-bottom: 14px; background: #f2faff; border: 1px solid #b9e5f7; border-radius: 7px; }
.priced-invitation-quote span { color: var(--artiv-text-secondary); }
.priced-invitation-quote strong { color: var(--artiv-accent); font-size: 22px; }
.invite-reject-button { color: #f56c6c; border-color: #f3b8b8; }
.manage-button, .back-button { margin-top: 12px; margin-left: 0 !important; }
.project-main-card { min-height: 650px; padding: 32px; }
.project-heading { padding-bottom: 24px; border-bottom: 1px solid #edf0f3; }
.project-title { margin: 0; color: #333; font-size: 29px; line-height: 1.45; }
.project-meta { display: flex; flex-wrap: wrap; gap: 16px; margin-top: 12px; color: #999; font-size: 13px; }
.detail-section { margin-top: 36px; }
.detail-section h2 { margin: 0 0 16px; color: #09a3df; font-size: 17px; }
.reference-gallery { display: grid; grid-template-columns: repeat(4, 110px); gap: 10px; }
.reference-image { width: 110px; height: 110px; border-radius: 4px; background: #f2f4f6; }
.description-section p { margin: 0; color: #555; font-size: 14px; line-height: 1.95; white-space: pre-wrap; overflow-wrap: anywhere; }
.requirements-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px 30px; margin: 0; padding-top: 4px; }
.requirement-item { display: grid; grid-template-columns: 88px minmax(0, 1fr); align-items: start; border-left: 3px solid #9eddf4; }
.requirement-item dt, .requirement-item dd { margin: 0; padding: 3px 10px; font-size: 13px; line-height: 1.6; }
.requirement-item dt { color: #8a929b; }
.requirement-item dd { color: #555; }
.phase-tip { margin: -4px 0 22px; color: #7f8994; font-size: 13px; line-height: 1.7; }
.phase-steps { padding: 18px 10px 6px; border-radius: 6px; background: #f7fafc; }
.applicant-panel { margin-top: 20px; }
.applicant-heading { display: flex; justify-content: space-between; align-items: baseline; padding: 20px 28px; border-bottom: 1px solid #edf0f3; }
.applicant-heading h2 { margin: 0; color: #555; font-size: 16px; }
.applicant-heading span { color: #999; font-size: 13px; }
.applicant-list { padding: 4px 28px 28px; }
.applicant-card { position: relative; display: grid; grid-template-columns: minmax(250px, 1.15fr) 110px minmax(300px, 1.5fr); gap: 20px; align-items: center; height: 164px; margin-top: 24px; padding: 42px 20px 18px; overflow: hidden; border: 1px solid #e1e5ea; border-radius: 4px; background: #fff; }
.applicant-card--target { border-color: var(--artiv-primary); box-shadow: 0 0 0 2px rgb(0 173 239 / 12%); }
.applicant-identity { display: flex; gap: 15px; align-items: center; min-width: 0; padding: 0; border: 0; background: none; text-align: left; cursor: pointer; }
.applicant-avatar { flex: 0 0 66px; width: 66px !important; min-width: 66px; max-width: 66px; height: 66px !important; min-height: 66px; max-height: 66px; overflow: hidden; }
.applicant-avatar :deep(img) { width: 100%; height: 100%; object-fit: cover; object-position: center; }
.applicant-copy { min-width: 0; }
.applicant-copy h3 { margin: 0 0 7px; color: #3e4650; font-size: 16px; }
.applicant-copy p { display: -webkit-box; min-height: 42px; overflow: hidden; margin: 0; color: #777; font-size: 13px; line-height: 21px; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.painter-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 8px; }
.quote-block { padding-left: 16px; border-left: 1px solid #edf0f3; }
.quote-block span, .quote-block strong { display: block; }
.quote-block span { margin-bottom: 6px; color: #999; font-size: 12px; }
.quote-block strong { color: #ff7043; font-size: 15px; }
.applicant-works { display: grid; grid-template-columns: repeat(4, 80px); justify-content: end; gap: 8px; }
.work-thumbnail { width: 80px; height: 80px; border-radius: 3px; background: #f1f3f5; }
.work-placeholder { display: grid; place-items: center; color: #a5acb5; font-size: 12px; }
.applicant-actions { position: absolute; top: 10px; right: 12px; z-index: 1; display: flex; gap: 6px; }
.applicant-actions :deep(.el-button) { padding: 6px 10px; }
.application-hint { margin: 0; color: #999; font-size: 12px; line-height: 1.6; }

@media (max-width: 820px) {
  .project-detail-page { padding: 16px 0 40px; }
  .project-detail-shell { display: flex; flex-direction: column; gap: 12px; }
  .project-sidebar { position: static; width: 100%; padding: 0 12px; }
  .owner-card { display: flex; align-items: center; gap: 16px; text-align: left; }
  .owner-card h2 { margin: 0 0 4px; }
  .project-main-column { width: 100%; }
  .project-main-card, .applicant-panel { border-radius: 0; }
  .project-main-card { min-height: 420px; padding: 24px 18px; }
  .project-title { font-size: 23px; }
  .reference-gallery { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .reference-image { width: 100%; height: auto; aspect-ratio: 1; }
  .requirements-grid { grid-template-columns: 1fr; }
  .applicant-heading, .applicant-list { padding-left: 18px; padding-right: 18px; }
  .applicant-card { grid-template-columns: 1fr; padding-top: 52px; }
  .quote-block { padding: 12px 0 0; border-top: 1px solid #edf0f3; border-left: 0; }
  .applicant-works { grid-template-columns: repeat(4, minmax(0, 1fr)); justify-content: stretch; }
  .work-thumbnail { width: 100%; }
}
</style>
