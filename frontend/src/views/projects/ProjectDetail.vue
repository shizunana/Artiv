<template>
  <div class="project-detail-page" v-loading="loading">
    <div v-if="project" class="project-detail-shell">
      <aside class="project-sidebar">
        <section class="sidebar-card owner-card">
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
            <strong class="budget">￥ {{ project.budget }}</strong>
          </div>
          <p class="recruit-countdown">{{ recruitCountdown }}</p>
          <el-button
            v-if="showApplyButton"
            class="apply-button"
            type="primary"
            :disabled="applyButtonDisabled"
            @click="handleApplyButton"
          >
            {{ applyButtonText }}
          </el-button>
          <el-button class="back-button" @click="router.push({ name: 'Projects' })">返回企划列表</el-button>
        </section>
      </aside>

      <main class="project-main-column">
        <article class="project-main-card">
          <header class="project-heading">
            <h1 class="project-title">{{ project.title }}</h1>
            <div class="project-meta">
              <span>发布于 {{ formatDateTime(project.createdAt) }}</span>
              <span v-if="project.category">{{ project.category }}</span>
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
        </article>

        <section class="applicant-panel" v-loading="loadingApplicants">
          <header class="applicant-heading">
            <h2>应征画师列表</h2>
            <span>共 {{ applicantTotal }} 名画师应征</span>
          </header>

          <div v-if="applicants.length" class="applicant-list">
            <article v-for="item in applicants" :key="item.application.id" class="applicant-card">
              <div v-if="showOwnerActions(item)" class="applicant-actions">
                <el-button size="small" :loading="startingChatId === item.painter.id" @click="openChat(item)">私信</el-button>
                <el-button size="small" type="primary" :loading="orderingPainterId === item.painter.id" @click="openOrderConfirm(item)">
                  发送企划订单
                </el-button>
              </div>

              <button class="applicant-identity" type="button" @click="openPainterProfile(item.painter?.id)">
                <el-avatar class="applicant-avatar" :size="66" :src="item.painter?.avatar">{{ painterName(item).slice(0, 1) }}</el-avatar>
                <div class="applicant-copy">
                  <h3>{{ painterName(item) }}</h3>
                  <p>{{ painterBio(item) }}</p>
                  <div v-if="painterTags(item).length" class="painter-tags">
                    <el-tag v-for="tag in painterTags(item)" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
                  </div>
                </div>
              </button>

              <div class="quote-block">
                <span>应征报价</span>
                <strong>￥ {{ item.application.quote }}</strong>
              </div>

              <div class="applicant-works">
                <template v-for="index in 4" :key="`${item.application.id}-${index}`">
                  <el-image
                    v-if="item.works?.[index - 1]?.image"
                    class="work-thumbnail"
                    :src="item.works[index - 1].image"
                    :preview-src-list="item.works.map((entry) => entry.image).filter(Boolean)"
                    :initial-index="index - 1"
                    fit="cover"
                    lazy
                  />
                  <div v-else class="work-thumbnail work-placeholder">作品</div>
                </template>
              </div>
            </article>
          </div>
          <el-empty v-else-if="!loadingApplicants" description="暂无画师应征" />
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

    <el-dialog v-model="quoteDialogVisible" title="更改应征" width="420px">
      <el-form label-position="top">
        <el-form-item label="新报价（元）">
          <el-input-number v-model="quoteForm.quote" :min="0.01" :precision="2" :step="100" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="danger" plain :loading="withdrawingApplicationId === quoteForm.id" @click="withdrawCurrentApplication">取消应征</el-button>
        <el-button @click="quoteDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="updatingQuote" @click="submitQuoteUpdate">保存报价</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="orderConfirmVisible" title="确认企划订单" width="540px">
      <div v-if="orderingApplication" class="confirm-order">
        <el-image v-if="projectImages[0]" class="confirm-cover" :src="projectImages[0]" fit="cover" />
        <div class="confirm-copy">
          <h3>{{ project?.title }}</h3>
          <p>接单画师：{{ painterName(orderingApplication) }}</p>
          <p>订单来源：该画师对本企划的有效应征</p>
          <p>交付流程：初稿 → 修改 → 终稿 → 交付</p>
        </div>
      </div>
      <div class="confirm-amount">
        <span>按应征报价托管</span>
        <strong>¥{{ Number(orderingApplication?.application?.quote || project?.budget || 0).toFixed(2) }}</strong>
      </div>
      <p class="confirm-tip">确认后将从账户余额托管订单金额。此订单独立流转，不影响本企划向其他应征画师继续发单。</p>
      <template #footer>
        <el-button @click="orderConfirmVisible = false">再想想</el-button>
        <el-button type="primary" :loading="creatingOrder" @click="confirmProjectOrder">确认发送订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  applyProject,
  createProjectOrder,
  deleteApplication,
  getApplications,
  getProject,
  updateApplicationQuote,
} from '@/api/project'
import { startProjectConversation } from '@/api/message'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/date'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const project = ref(null)
const employer = ref(null)
const loading = ref(false)
const applicants = ref([])
const applicantTotal = ref(0)
const loadingApplicants = ref(false)
const startingChatId = ref(null)
const orderingPainterId = ref(null)
const orderingApplication = ref(null)
const orderConfirmVisible = ref(false)
const creatingOrder = ref(false)
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

const employerName = computed(() => employer.value?.nickname || `企划方 #${project.value?.employerId || ''}`)
const currentUserId = computed(() => Number(userStore.user?.id || 0))
const isOwner = computed(() => currentUserId.value > 0 && currentUserId.value === Number(project.value?.employerId))
const currentApplication = computed(() => applicants.value.find((item) => Number(item.painter?.id) === currentUserId.value))
const canApply = computed(() => userStore.isLoggedIn
  && userStore.user?.certStatus === 2
  && !isOwner.value
  && !currentApplication.value
  && Number(project.value?.status) === 0
  && !recruitExpired.value)
const applyButtonDisabled = computed(() => currentApplication.value
  ? Number(currentApplication.value.application.status) !== 0
  : !canApply.value)
const showApplyButton = computed(() => !isOwner.value && (userStore.user?.certStatus === 2 || !userStore.isLoggedIn))
const applyButtonText = computed(() => {
  if (!userStore.isLoggedIn) return '登录后应征企划'
  if (currentApplication.value) return '更改应征'
  if (recruitExpired.value || Number(project.value?.status) !== 0) return '应征已截止'
  return '应征企划'
})
const projectImages = computed(() => String(project.value?.images || '').split(',').map((item) => item.trim()).filter(Boolean))
const projectSubjects = computed(() => String(project.value?.subject || '').split(',').map((item) => item.trim()).filter(Boolean))
const recruitExpired = computed(() => project.value?.recruitDeadline && new Date(project.value.recruitDeadline).getTime() <= Date.now())
const recruitCountdown = computed(() => {
  if (!project.value?.recruitDeadline) return '招募进行中'
  const remaining = new Date(project.value.recruitDeadline).getTime() - Date.now()
  if (remaining <= 0) return '应征已截止'
  const minutes = Math.floor(remaining / 60000)
  const days = Math.floor(minutes / 1440)
  const hours = Math.floor((minutes % 1440) / 60)
  return `${days} 天 ${hours} 小时后关闭应征`
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
])

const painterName = (item) => item.painter?.nickname || `画师 #${item.application.painterId}`
const painterBio = (item) => {
  const bio = String(item.painter?.bio || '这位画师还没有填写个人简介。').trim()
  const characters = Array.from(bio)
  return characters.length > 20 ? `${characters.slice(0, 20).join('')}...` : bio
}
const painterTags = (item) => String(item.painter?.tags || '').split(',').map((tag) => tag.trim()).filter(Boolean)
const showOwnerActions = () => isOwner.value

function handleApplyButton() {
  if (currentApplication.value) {
    openQuoteDialog(currentApplication.value)
  } else {
    applicationDialogVisible.value = true
  }
}

function resetApplicationForm() {
  applicationFormRef.value?.resetFields()
}

function openPainterProfile(id) {
  if (id) router.push({ name: 'ArtistProfile', params: { id } })
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

async function loadApplicants() {
  loadingApplicants.value = true
  try {
    const data = await getApplications(project.value.id, { page: 1, size: 50 })
    applicants.value = data.records || []
    applicantTotal.value = data.total || applicants.value.length
  } catch (error) {
    ElMessage.error(error.message || '应征画师加载失败')
  } finally {
    loadingApplicants.value = false
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
  quoteForm.id = item.application.id
  quoteForm.quote = Number(item.application.quote)
  quoteDialogVisible.value = true
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

async function openChat(item) {
  startingChatId.value = item.painter.id
  try {
    await startProjectConversation(project.value.id, item.painter.id)
    await router.push({ name: 'DashboardMessages', query: { withUserId: item.painter.id, projectId: project.value.id } })
  } catch (error) {
    ElMessage.error(error.message || '发起私信失败')
  } finally {
    startingChatId.value = null
  }
}

function openOrderConfirm(item) {
  orderingApplication.value = item
  orderConfirmVisible.value = true
}

async function confirmProjectOrder() {
  const item = orderingApplication.value
  if (!item) return
  orderingPainterId.value = item.painter.id
  creatingOrder.value = true
  try {
    const order = await createProjectOrder(project.value.id, item.application.id)
    orderConfirmVisible.value = false
    await userStore.fetchMe()
    ElMessage.success('企划订单已发送，金额已托管并通知画师接单')
    await router.push({ name: 'DashboardOrderDetail', params: { id: order.id } })
  } catch (error) {
    ElMessage.error(error.message || '发送企划订单失败')
  } finally {
    if (orderConfirmVisible.value) orderConfirmVisible.value = false
    orderingPainterId.value = null
    creatingOrder.value = false
  }
}

async function loadProject() {
  loading.value = true
  try {
    project.value = await getProject(route.params.id)
    employer.value = project.value?.employer || null
    await loadApplicants()
  } catch (error) {
    ElMessage.error(error.message || '企划加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadProject)
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
.apply-button, .back-button { width: 100%; }
.apply-button { height: 42px; font-size: 15px; }
.back-button { margin-top: 12px; margin-left: 0 !important; }
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
.applicant-panel { margin-top: 20px; }
.confirm-order { display: grid; grid-template-columns: 104px minmax(0, 1fr); gap: 16px; align-items: center; }
.confirm-cover { width: 104px; height: 104px; overflow: hidden; border-radius: 8px; background: #f2f4f6; }
.confirm-copy h3 { margin: 0 0 10px; font-size: 18px; }
.confirm-copy p { margin: 5px 0; color: #7e8790; font-size: 13px; }
.confirm-amount { display: flex; justify-content: space-between; align-items: center; margin-top: 20px; padding: 14px 16px; border-radius: 7px; background: #f5f9fc; }
.confirm-amount span { color: #6e7882; }
.confirm-amount strong { color: #f39a19; font-size: 22px; }
.confirm-tip { margin: 13px 0 0; color: #8b939b; font-size: 13px; line-height: 1.7; }
.applicant-heading { display: flex; justify-content: space-between; align-items: baseline; padding: 20px 28px; border-bottom: 1px solid #edf0f3; }
.applicant-heading h2 { margin: 0; color: #555; font-size: 16px; }
.applicant-heading span { color: #999; font-size: 13px; }
.applicant-list { padding: 4px 28px 28px; }
.applicant-card { position: relative; display: grid; grid-template-columns: minmax(250px, 1.15fr) 110px minmax(300px, 1.5fr); gap: 20px; align-items: center; height: 164px; margin-top: 24px; padding: 42px 20px 18px; overflow: hidden; border: 1px solid #e1e5ea; border-radius: 4px; background: #fff; }
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
