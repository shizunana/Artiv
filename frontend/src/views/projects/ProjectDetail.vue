<template>
  <div class="project-detail-page" v-loading="loading">
    <div v-if="project" class="project-detail-shell">
      <aside class="project-sidebar">
        <section class="sidebar-card owner-card">
          <el-avatar :size="72" :src="employer?.avatar">
            {{ employerName.slice(0, 1) }}
          </el-avatar>
          <h2>{{ employerName }}</h2>
          <p>{{ employer?.bio || '正在寻找合适的创作者' }}</p>
        </section>

        <section class="sidebar-card project-summary-card">
          <div class="summary-item">
            <span>截稿日期</span>
            <strong>{{ project.deadline || '待商议' }}</strong>
          </div>
          <div class="summary-item">
            <span>单张稿酬预算</span>
            <strong class="budget">￥{{ project.budget }}</strong>
          </div>
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
              <span v-if="project.subject">{{ project.subject }}</span>
            </div>
            <el-button v-if="canApply" class="apply-button" type="primary" @click="applicationDialogVisible = true">
              应征企划
            </el-button>
          </header>

          <section v-if="projectImages.length" class="detail-section reference-section">
            <h2>参考图</h2>
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

        <section v-if="isOwner" class="applicant-panel" v-loading="loadingApplicants">
          <header class="applicant-heading">
            <h2>应征画师列表</h2>
            <span>共 {{ applicantTotal }} 名画师应征</span>
          </header>

          <div v-if="applicants.length" class="applicant-list">
            <article v-for="item in applicants" :key="item.application.id" class="applicant-card">
              <div class="applicant-profile">
                <el-avatar :size="58" :src="item.painter?.avatar">
                  {{ painterName(item).slice(0, 1) }}
                </el-avatar>
                <div class="applicant-copy">
                  <h3>{{ painterName(item) }}</h3>
                  <p>{{ item.painter?.bio || '这位画师还没有填写个人简介。' }}</p>
                  <div v-if="painterTags(item).length" class="painter-tags">
                    <el-tag v-for="tag in painterTags(item)" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
                  </div>
                </div>
              </div>

              <div class="application-message">
                <p>{{ item.application.intro || '画师暂未填写应征介绍。' }}</p>
                <strong>报价 ￥{{ item.application.quote }}</strong>
              </div>

              <div v-if="item.works?.length" class="applicant-works">
                <el-image
                  v-for="work in item.works.slice(0, 4)"
                  :key="work.id"
                  class="work-thumbnail"
                  :src="work.image"
                  :preview-src-list="item.works.map((entry) => entry.image).filter(Boolean)"
                  fit="cover"
                  lazy
                >
                  <template #error><span class="work-placeholder">{{ work.title?.slice(0, 1) || '绘' }}</span></template>
                </el-image>
              </div>

              <div class="applicant-actions">
                <el-button :loading="startingChatId === item.painter.id" @click="openChat(item)">私信</el-button>
                <el-button type="primary" :loading="orderingPainterId === item.painter.id" @click="sendOrderIntent(item)">
                  发送企划订单
                </el-button>
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
          <el-input v-model="applicationForm.intro" type="textarea" :rows="5" placeholder="介绍你的创作思路和相关经验" />
        </el-form-item>
        <el-form-item label="报价（元）" prop="quote">
          <el-input-number v-model="applicationForm.quote" :min="0.01" :precision="2" :step="100" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applicationDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingApplication" @click="submitApplication">提交应征</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { applyProject, createProjectOrderIntent, getApplications, getProject } from '@/api/project'
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
const applicationDialogVisible = ref(false)
const applicationFormRef = ref()
const submittingApplication = ref(false)
const applicationForm = reactive({ intro: '', quote: undefined })
const applicationRules = {
  intro: [{ required: true, message: '请填写应征介绍', trigger: 'blur' }],
  quote: [
    { required: true, message: '请填写报价', trigger: 'change' },
    { type: 'number', min: 0.01, message: '报价必须大于 0', trigger: 'change' },
  ],
}

const employerName = computed(() => employer.value?.nickname || `企划方 #${project.value?.employerId || ''}`)
const isOwner = computed(() => Number(userStore.user?.id) === Number(project.value?.employerId))
const canApply = computed(() => userStore.isLoggedIn && userStore.user?.certStatus === 2 && !isOwner.value && Number(project.value?.status) === 0)
const projectImages = computed(() => String(project.value?.images || '').split(',').map((item) => item.trim()).filter(Boolean))
const requirements = computed(() => {
  if (!project.value?.requirements) return {}
  if (typeof project.value.requirements === 'object') return project.value.requirements
  try {
    return JSON.parse(project.value.requirements)
  } catch {
    return {}
  }
})
const requirementItems = computed(() => [
  { label: '题材风格', value: project.value?.subject || '风格不限' },
  { label: '尺寸规格', value: requirements.value.size || '需与画师商议' },
  { label: '颜色模式', value: requirements.value.colorMode || '需与画师商议' },
  { label: '稿件格式', value: Array.isArray(requirements.value.formats) ? requirements.value.formats.join('、') : requirements.value.formats || '需与画师商议' },
  { label: '发布权限', value: requirements.value.publishPermission || '需与画师商议' },
])

const painterName = (item) => item.painter?.nickname || `画师 #${item.application.painterId}`
const painterTags = (item) => String(item.painter?.tags || '').split(',').map((tag) => tag.trim()).filter(Boolean)
const normalizeApplicant = (item) => item.application ? item : {
  application: item,
  painter: { id: item.painterId },
  works: [],
}

function resetApplicationForm() {
  applicationFormRef.value?.resetFields()
}

async function submitApplication() {
  const valid = await applicationFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submittingApplication.value = true
  try {
    await applyProject(project.value.id, { intro: applicationForm.intro, quote: applicationForm.quote })
    ElMessage.success('应征提交成功')
    applicationDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error.message || '应征提交失败')
  } finally {
    submittingApplication.value = false
  }
}

async function loadApplicants() {
  if (!isOwner.value) return
  loadingApplicants.value = true
  try {
    const data = await getApplications(project.value.id, { page: 1, size: 50 })
    applicants.value = (data.records || []).map(normalizeApplicant)
    applicantTotal.value = data.total || applicants.value.length
  } catch (error) {
    ElMessage.error(error.message || '应征画师加载失败')
  } finally {
    loadingApplicants.value = false
  }
}

async function openChat(item) {
  startingChatId.value = item.painter.id
  try {
    await startProjectConversation(project.value.id, item.painter.id)
    const routeName = userStore.workbenchRole === 2 ? 'PainterMessages' : 'DashboardMessages'
    await router.push({
      name: routeName,
      query: { withUserId: item.painter.id, projectId: project.value.id },
    })
  } catch (error) {
    ElMessage.error(error.message || '发起私信失败')
  } finally {
    startingChatId.value = null
  }
}

async function sendOrderIntent(item) {
  orderingPainterId.value = item.painter.id
  try {
    const message = await createProjectOrderIntent(project.value.id, item.painter.id)
    ElMessage.info(message)
  } catch (error) {
    ElMessage.error(error.message || '发送企划订单失败')
  } finally {
    orderingPainterId.value = null
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
.project-detail-page {
  min-height: calc(100vh - 64px);
  padding: 40px 20px 64px;
  background: #e6eaee;
}

.project-detail-shell {
  width: min(1080px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.project-sidebar {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sidebar-card,
.project-main-card {
  background: #fff;
  border-radius: 4px;
}

.applicant-panel {
  margin-top: 20px;
  background: #fff;
  border-radius: 4px;
}

.applicant-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 30px;
  border-bottom: 1px solid #edf0f3;
}

.applicant-heading h2 {
  margin: 0;
  color: #555;
  font-size: 17px;
}

.applicant-heading span {
  color: #999;
  font-size: 13px;
}

.applicant-list {
  padding: 0 30px 30px;
}

.applicant-card {
  display: grid;
  grid-template-columns: minmax(210px, 1.15fr) minmax(190px, 1fr) 220px;
  gap: 20px;
  align-items: center;
  margin-top: 30px;
  padding: 20px;
  border: 1px solid #e5e8ec;
  border-radius: 4px;
}

.applicant-profile {
  display: flex;
  gap: 14px;
  min-width: 0;
}

.applicant-copy {
  min-width: 0;
}

.applicant-copy h3 {
  margin: 2px 0 6px;
  color: #444;
  font-size: 16px;
}

.applicant-copy p,
.application-message p {
  margin: 0;
  color: #777;
  font-size: 13px;
  line-height: 1.7;
}

.painter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 8px;
}

.application-message {
  min-width: 0;
  padding-left: 20px;
  border-left: 1px solid #edf0f3;
}

.application-message strong {
  display: block;
  margin-top: 8px;
  color: #ff7d45;
  font-size: 14px;
}

.applicant-works {
  grid-column: 1 / 3;
  display: grid;
  grid-template-columns: repeat(4, 68px);
  gap: 8px;
}

.work-thumbnail {
  width: 68px;
  height: 68px;
  border-radius: 3px;
  background: #f1f3f5;
}

.work-placeholder {
  display: grid;
  width: 100%;
  height: 100%;
  place-items: center;
  color: #a5acb5;
  font-size: 18px;
}

.applicant-actions {
  grid-column: 3;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.owner-card {
  padding: 28px 22px 24px;
  text-align: center;
}

.owner-card h2 {
  margin: 16px 0 8px;
  font-size: 17px;
  color: #555;
}

.owner-card p {
  margin: 0;
  color: #999;
  font-size: 13px;
  line-height: 1.7;
}

.project-summary-card {
  padding: 24px 22px;
}

.summary-item + .summary-item {
  margin-top: 22px;
}

.summary-item span,
.summary-item strong {
  display: block;
}

.summary-item span {
  margin-bottom: 8px;
  color: #999;
  font-size: 13px;
}

.summary-item strong {
  color: #555;
  font-size: 17px;
  font-weight: 500;
}

.summary-item .budget {
  color: #ff7d45;
}

.back-button {
  width: 100%;
  margin-top: 28px;
}

.project-main-card {
  min-height: 680px;
  padding: 30px;
}

.project-heading {
  padding-bottom: 24px;
  border-bottom: 1px solid #edf0f3;
}

.project-title {
  margin: 0;
  color: #333;
  font-size: 28px;
  line-height: 1.45;
  font-weight: 600;
}

.project-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 12px;
  color: #999;
  font-size: 13px;
}

.detail-section {
  margin-top: 36px;
}

.detail-section h2 {
  margin: 0 0 16px;
  color: #444;
  font-size: 17px;
  line-height: 1.5;
}

.reference-gallery {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.reference-gallery.gallery-size-1 {
  grid-template-columns: 1fr;
}

.reference-image {
  width: 100%;
  aspect-ratio: 16 / 10;
  border-radius: 4px;
  background: #f2f4f6;
}

.gallery-size-1 .reference-image {
  max-height: 480px;
  aspect-ratio: 16 / 9;
}

.description-section p {
  margin: 0;
  color: #666;
  font-size: 14px;
  line-height: 1.9;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.requirements-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 0;
  border-top: 1px solid #edf0f3;
  border-left: 1px solid #edf0f3;
}

.requirement-item {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  min-height: 56px;
  border-right: 1px solid #edf0f3;
  border-bottom: 1px solid #edf0f3;
}

.requirement-item dt,
.requirement-item dd {
  margin: 0;
  padding: 17px 14px;
  font-size: 13px;
  line-height: 1.6;
}

.requirement-item dt {
  color: #8a929b;
  background: #f7f8fa;
}

.requirement-item dd {
  color: #555;
}

@media (max-width: 760px) {
  .project-detail-page {
    padding: 16px 0 40px;
  }

  .project-detail-shell {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .project-sidebar,
  .project-main-column {
    width: 100%;
  }

  .project-sidebar {
    padding: 0 12px;
  }

  .owner-card {
    display: flex;
    align-items: center;
    gap: 16px;
    text-align: left;
  }

  .owner-card h2 {
    margin: 0 0 4px;
  }

  .project-main-card {
    min-height: 420px;
    padding: 24px 18px;
    border-radius: 0;
  }

  .project-title {
    font-size: 23px;
  }

  .reference-gallery,
  .requirements-grid {
    grid-template-columns: 1fr;
  }

  .applicant-panel {
    margin-top: 12px;
    border-radius: 0;
  }

  .applicant-heading,
  .applicant-list {
    padding-left: 18px;
    padding-right: 18px;
  }

  .applicant-card {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .application-message {
    padding: 14px 0 0;
    border-top: 1px solid #edf0f3;
    border-left: 0;
  }

  .applicant-works,
  .applicant-actions {
    grid-column: 1;
  }

  .applicant-works {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .work-thumbnail {
    width: 100%;
    height: auto;
    aspect-ratio: 1;
  }
}
</style>
