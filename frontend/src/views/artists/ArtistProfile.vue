<template>
  <div v-loading="profileLoading" class="public-profile-page">
    <section class="profile-cover" aria-label="用户主页封面">
      <i class="cover-orb cover-orb--one" />
      <i class="cover-orb cover-orb--two" />
      <button v-if="canSwitchRole" class="role-switch" type="button" @click="switchRole">⇄ 切换身份</button>
    </section>

    <main v-if="profile" class="profile-content page-container">
      <section class="profile-identity">
        <el-avatar :size="124" :src="profile.avatar || undefined" class="profile-avatar">
          {{ profile.nickname?.slice(0, 1) || (isPainter ? '画' : '企') }}
        </el-avatar>
        <div class="identity-copy">
          <div class="identity-title">
            <h1>{{ profile.nickname }}</h1>
            <RatingSummary :average-score="profile.averageScore" :review-count="profile.reviewCount" show-count hide-when-empty />
          </div>
          <div class="identity-badges">
            <el-tag v-if="isPainter && profile.certStatus === 2" size="small" type="success" effect="light">认证画师</el-tag>
            <el-tag v-else-if="isPainter" size="small" type="info" effect="light">未认证</el-tag>
            <el-tag v-else size="small" type="info" effect="light">企划方</el-tag>
            <el-tag v-for="tag in profile.tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
          </div>
        </div>
        <div v-if="!isOwner" class="profile-actions">
          <el-button v-if="isPainter && profile.certStatus === 2" type="primary" :loading="followLoading" :disabled="blocked" @click="toggleFollow">
            {{ followed ? '已关注' : '关注' }}
          </el-button>
          <el-button v-if="isPainter && profile.certStatus === 2" class="invite-button" :disabled="blocked" @click="openInvite">邀请</el-button>
          <el-tag v-if="blocked" type="info" effect="light">已屏蔽</el-tag>
          <el-dropdown trigger="click" @command="handleMoreCommand">
            <el-button class="more-button" circle aria-label="更多操作"><el-icon><MoreFilled /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="block">{{ blocked ? '解除黑名单' : '加入黑名单' }}</el-dropdown-item>
                <el-dropdown-item command="report">举报用户</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </section>

      <section class="profile-introduction">
        <h2>{{ isPainter ? '画师简介' : '企划方简介' }}</h2>
        <p>{{ profile.bio || (isPainter ? '这位画师暂未填写个人简介。' : '这位企划方暂未填写个人简介。') }}</p>
      </section>

      <section v-if="isPainter" class="profile-metrics" aria-label="画师履约数据">
        <div class="metric-item">
          <span>需求完成率</span>
          <strong>{{ formatRate(profile.completionRate) }}</strong>
        </div>
        <div class="metric-item">
          <span>准时交稿率</span>
          <strong>{{ formatRate(profile.onTimeRate) }}</strong>
        </div>
        <div class="metric-note"><span class="metric-dot" />统计已结束订单；暂无样本时显示“暂无”</div>
      </section>

      <section class="profile-showcase">
        <div class="profile-tabs" role="tablist">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            :class="['profile-tab', { active: activeTab === tab.key }]"
            @click="selectTab(tab.key)"
          >
            {{ tab.label }}<span v-if="tab.count !== null"> {{ tab.count }}</span>
          </button>
        </div>

        <div v-loading="resourceLoading" class="showcase-body">
          <div v-if="activeTab === 'works' && resources.length" class="work-grid">
            <button v-for="work in resources" :key="work.id" class="work-card" type="button" @click="previewWork(work)">
              <img :src="work.image" :alt="work.title" loading="lazy" />
              <span>{{ work.title }}</span>
            </button>
          </div>

          <div v-else-if="activeTab === 'stalls' && resources.length" class="stall-grid">
            <article v-for="stall in resources" :key="stall.id" class="stall-card" @click="goStall(stall.id)">
              <img :src="stall.cover" :alt="stall.title" loading="lazy" />
              <div class="stall-card-copy">
                <h3>{{ stall.title }}</h3>
                <p>{{ stall.category }}</p>
                <strong>¥{{ stall.price }}</strong>
              </div>
            </article>
          </div>

          <div v-else-if="activeTab === 'projects' && resources.length" class="project-grid">
            <article v-for="project in resources" :key="project.id" class="project-card" @click="goProject(project.id)">
              <img v-if="projectCover(project)" :src="projectCover(project)" :alt="project.title" loading="lazy" />
              <div v-else class="project-cover-placeholder">企划</div>
              <div class="project-card-copy">
                <div class="project-card-title">
                  <h3>{{ project.title }}</h3>
                  <el-tag size="small" :type="publicProjectStatus(project).type">{{ publicProjectStatus(project).text }}</el-tag>
                </div>
                <p>{{ project.description || '暂无需求描述' }}</p>
                <div class="project-card-meta"><span>{{ project.category || '未分类' }}</span><strong>{{ formatProjectBudget(project) }}</strong></div>
              </div>
            </article>
          </div>

          <ReviewList v-else-if="activeTab === 'reviews'" :user-id="profileId" :role="requestedRole" />

          <EmptyState v-else :description="emptyDescription" />
          <div v-if="isResourceTab" ref="loadSentinel" class="load-sentinel" aria-live="polite">
            <span v-if="loadingMore">正在加载更多…</span>
            <el-button v-else-if="resourceError" link type="primary" @click="loadResources(false)">加载失败，点击重试</el-button>
            <span v-else-if="resources.length && !hasMore">已加载全部内容</span>
          </div>
        </div>
      </section>
    </main>

    <EmptyState v-else-if="!profileLoading" description="该用户主页暂不可访问" />
    <el-image-viewer v-if="previewUrl" :url-list="[previewUrl]" hide-on-click-modal @close="previewUrl = ''" />

    <el-dialog v-model="inviteVisible" title="邀请画师" width="600px" @closed="resetInviteForm">
      <el-form ref="inviteFormRef" :model="inviteForm" :rules="inviteRules" label-position="top">
        <el-form-item label="画师">
          <div class="invite-painter"><el-avatar :src="profile?.avatar || undefined" />{{ profile?.nickname }}</div>
        </el-form-item>
        <el-form-item label="邀请类型" prop="invitationType">
          <div class="invite-type-list">
            <button type="button" :class="['invite-type-card', { active: inviteForm.invitationType === 1 }]" @click="inviteForm.invitationType = 1">
              <strong>邀请报价</strong><span>邀请画师查看企划，并根据需求主动提供报价。</span>
            </button>
            <button type="button" :class="['invite-type-card', { active: inviteForm.invitationType === 2 }]" @click="inviteForm.invitationType = 2">
              <strong>带价邀请</strong><span>提前提供合作价格，画师接受后按该价格形成应征。</span>
            </button>
          </div>
        </el-form-item>
        <el-form-item label="公开企划" prop="projectId">
          <el-select v-model="inviteForm.projectId" filterable placeholder="请选择希望邀请画师参与的公开企划" style="width: 100%" :loading="inviteProjectsLoading">
            <el-option v-for="item in inviteProjects" :key="item.id" :label="item.title" :value="item.id">
              <span>{{ item.title }}</span><span class="project-budget-option">{{ formatProjectBudget(item) }}</span>
            </el-option>
          </el-select>
          <p v-if="!inviteProjectsLoading && !inviteProjects.length" class="invite-empty-tip">暂无正在招募且未过期的公开企划，请先发布或重新上架企划。</p>
        </el-form-item>
        <el-form-item v-if="inviteForm.invitationType === 2" label="邀请价格（元）" prop="offeredPrice">
          <el-input-number v-model="inviteForm.offeredPrice" :min="50" :precision="2" :step="50" controls-position="right" />
          <span class="price-rule">最低 ¥50，发送后画师不能修改该应征报价。</span>
        </el-form-item>
        <el-form-item label="附言（选填）" prop="message">
          <el-input v-model="inviteForm.message" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="可以说明希望画师关注的需求重点" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inviteVisible = false">取消</el-button>
        <el-button type="primary" :loading="inviting" :disabled="!inviteProjects.length" @click="submitInvite">确认邀请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportVisible" title="举报用户" width="600px" append-to-body destroy-on-close @closed="resetReportForm">
      <el-alert title="举报仅用于平台治理，不会自动屏蔽对方，也不会向举报人推送处理结果。" type="info" :closable="false" show-icon />
      <el-form label-position="top" class="report-form">
        <el-form-item label="举报对象">
          <div class="invite-painter"><el-avatar :src="profile?.avatar || undefined" />{{ profile?.nickname }}</div>
        </el-form-item>
        <el-form-item label="举报类型" required>
          <el-select v-model="reportForm.category" placeholder="请选择举报类型" style="width: 100%">
            <el-option v-for="item in reportCategories" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="举报说明" required>
          <el-input v-model="reportForm.reason" type="textarea" :rows="5" minlength="10" maxlength="500" show-word-limit placeholder="请具体说明问题发生的场景和原因（10–500 字）" />
        </el-form-item>
        <el-form-item label="图片证据（选填，最多 6 张）">
          <el-upload
            v-model:file-list="reportFileList"
            list-type="picture-card"
            accept="image/*"
            :limit="6"
            :http-request="uploadReportImage"
            :on-remove="removeReportImage"
          >
            <span class="upload-plus">＋</span>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="primary" :loading="reportSubmitting" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MoreFilled } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import EmptyState from '@/components/common/EmptyState.vue'
import { getPublicProfile, getPublicProfileProjects, getPublicProfileStalls, getPublicProfileWorks } from '@/api/user'
import { useUserStore } from '@/stores/user'
import RatingSummary from '@/components/reviews/RatingSummary.vue'
import ReviewList from '@/components/reviews/ReviewList.vue'
import { followPainter, getPainterFollowStatus, unfollowPainter } from '@/api/social'
import { formatProjectBudget } from '@/utils/budget'
import { isProjectExpired } from '@/utils/projectDeadline'
import { getMyProjects } from '@/api/project'
import { createPublicInvitation } from '@/api/invitation'
import { blockUser, getUserBlockStatus, submitUserReport, unblockUser } from '@/api/governance'
import { uploadFile } from '@/api/upload'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const profile = ref(null)
const profileLoading = ref(false)
const resourceLoading = ref(false)
const loadingMore = ref(false)
const resources = ref([])
const activeTab = ref('works')
const page = ref(1)
const hasMore = ref(true)
const loadSentinel = ref(null)
const previewUrl = ref('')
const resourceError = ref(false)
const followed = ref(false)
const followLoading = ref(false)
const blocked = ref(false)
const inviteVisible = ref(false)
const inviteProjectsLoading = ref(false)
const inviteProjects = ref([])
const inviteFormRef = ref()
const inviting = ref(false)
const reportVisible = ref(false)
const reportSubmitting = ref(false)
const reportFileList = ref([])
const reportCategories = [
  { value: 'HARASSMENT', label: '骚扰或人身攻击' },
  { value: 'FRAUD', label: '欺诈或虚假交易' },
  { value: 'IMPERSONATION', label: '冒充他人' },
  { value: 'ILLEGAL_CONTENT', label: '违法违规内容' },
  { value: 'OTHER', label: '其他问题' },
]
const reportForm = reactive({ category: '', reason: '', images: [] })
const inviteForm = reactive({ projectId: null, invitationType: 1, offeredPrice: undefined, message: '' })
const inviteRules = {
  projectId: [{ required: true, message: '请选择公开企划', trigger: 'change' }],
  invitationType: [{ required: true, message: '请选择邀请类型', trigger: 'change' }],
  offeredPrice: [{ validator: (_rule, value, callback) => {
    if (inviteForm.invitationType === 2 && (!value || Number(value) < 50)) callback(new Error('邀请价格不能低于 50 元'))
    else callback()
  }, trigger: 'change' }],
}
let observer = null
let requestVersion = 0
let resourceRequestVersion = 0

const profileId = computed(() => Number(route.params.id))
const requestedRole = computed(() => route.query.role === 'employer' ? 'employer' : 'painter')
const isPainter = computed(() => requestedRole.value === 'painter')
const isOwner = computed(() => Number(userStore.user?.id) === profileId.value)
const canSwitchRole = computed(() => profile.value?.painterProfileAvailable && profile.value?.employerProfileAvailable)
const isResourceTab = computed(() => ['works', 'stalls', 'projects'].includes(activeTab.value))
const pageSize = computed(() => activeTab.value === 'projects' ? 12 : 20)
const tabs = computed(() => isPainter.value
  ? [
      { key: 'works', label: '作品', count: profile.value?.publicWorkCount ?? 0 },
      { key: 'stalls', label: '橱窗', count: profile.value?.publicStallCount ?? 0 },
      { key: 'reviews', label: '合作评价', count: profile.value?.reviewCount ?? 0 },
    ]
  : [
      { key: 'projects', label: '发布企划', count: profile.value?.publicProjectCount ?? 0 },
      { key: 'reviews', label: '合作评价', count: profile.value?.reviewCount ?? 0 },
    ])
const emptyDescription = computed(() => ({
  works: '暂无公开作品', stalls: '暂无上架橱窗', projects: '暂无公开企划', reviews: '暂无合作评价',
}[activeTab.value]))

onMounted(() => {
  observer = new IntersectionObserver((entries) => {
    if (entries.some((entry) => entry.isIntersecting)) loadResources(false)
  }, { rootMargin: '500px 0px' })
  loadProfile()
})
onBeforeUnmount(() => observer?.disconnect())
watch(() => [route.params.id, route.query.role], () => loadProfile())

async function loadProfile() {
  const version = ++requestVersion
  profileLoading.value = true
  profile.value = null
  resources.value = []
  try {
    const data = await getPublicProfile(profileId.value, requestedRole.value)
    if (version !== requestVersion) return
    profile.value = data
    activeTab.value = isPainter.value ? 'works' : 'projects'
    followed.value = false
    blocked.value = false
    if (userStore.isLoggedIn && !isOwner.value) {
      const blockState = await getUserBlockStatus(profileId.value)
      if (version !== requestVersion) return
      blocked.value = Boolean(blockState?.blocked)
      if (isPainter.value && data.certStatus === 2) {
        const state = await getPainterFollowStatus(profileId.value)
        if (version !== requestVersion) return
        followed.value = Boolean(state?.followed)
      }
    }
    await resetResources()
  } finally {
    if (version === requestVersion) profileLoading.value = false
  }
}

async function selectTab(tab) {
  if (activeTab.value === tab) return
  activeTab.value = tab
  await resetResources()
}

async function resetResources() {
  resourceRequestVersion += 1
  resources.value = []
  page.value = 1
  hasMore.value = true
  resourceError.value = false
  resourceLoading.value = false
  loadingMore.value = false
  observer?.disconnect()
  if (isResourceTab.value) await loadResources(true)
  await nextTick()
  if (loadSentinel.value && isResourceTab.value) observer?.observe(loadSentinel.value)
}

async function loadResources(reset = false) {
  if (!isResourceTab.value || (!reset && (resourceLoading.value || loadingMore.value || !hasMore.value))) return
  const version = resourceRequestVersion
  const resourceTab = activeTab.value
  const userId = profileId.value
  const targetPage = reset ? 1 : page.value + 1
  resourceError.value = false
  if (reset) resourceLoading.value = true
  else loadingMore.value = true
  try {
    const params = { page: targetPage, size: pageSize.value }
    let data
    if (resourceTab === 'works') data = await getPublicProfileWorks(userId, params)
    if (resourceTab === 'stalls') data = await getPublicProfileStalls(userId, params)
    if (resourceTab === 'projects') data = await getPublicProfileProjects(userId, params)
    if (version !== resourceRequestVersion || resourceTab !== activeTab.value || userId !== profileId.value) return
    const records = data?.records || []
    const merged = reset ? records : [...resources.value, ...records]
    resources.value = [...new Map(merged.map((item) => [item.id, item])).values()]
    page.value = targetPage
    hasMore.value = resources.value.length < Number(data?.total || 0) && records.length === pageSize.value
  } catch (error) {
    if (version === resourceRequestVersion) resourceError.value = true
    throw error
  } finally {
    if (version === resourceRequestVersion) {
      resourceLoading.value = false
      loadingMore.value = false
    }
  }
}

function switchRole() {
  router.replace({ name: 'ArtistProfile', params: { id: profileId.value }, query: { role: isPainter.value ? 'employer' : 'painter' } })
}
function requireLogin() {
  if (userStore.isLoggedIn) return true
  router.push({ name: 'Login', query: { redirect: route.fullPath } })
  return false
}

async function toggleFollow() {
  if (!requireLogin() || followLoading.value || blocked.value) return
  followLoading.value = true
  try {
    const state = followed.value
      ? await unfollowPainter(profileId.value)
      : await followPainter(profileId.value)
    followed.value = Boolean(state?.followed)
    ElMessage.success(followed.value ? '已关注该画师' : '已取消关注')
  } finally {
    followLoading.value = false
  }
}

async function openInvite() {
  if (!requireLogin() || blocked.value) return
  inviteVisible.value = true
  inviteProjectsLoading.value = true
  try {
    const data = await getMyProjects({ page: 1, size: 100, mode: 0 })
    inviteProjects.value = (data?.records || []).filter((item) => Number(item.status) === 0
      && Number(item.moderationStatus || 0) === 0 && !isProjectExpired(item.deadline))
  } finally {
    inviteProjectsLoading.value = false
  }
}

function resetInviteForm() {
  inviteFormRef.value?.resetFields()
  inviteForm.projectId = null
  inviteForm.invitationType = 1
  inviteForm.offeredPrice = undefined
  inviteForm.message = ''
  inviteProjects.value = []
}

async function submitInvite() {
  const valid = await inviteFormRef.value?.validate().catch(() => false)
  if (!valid) return
  inviting.value = true
  try {
    await createPublicInvitation({
      projectId: inviteForm.projectId,
      painterId: profileId.value,
      invitationType: inviteForm.invitationType,
      ...(inviteForm.invitationType === 2 ? { offeredPrice: inviteForm.offeredPrice } : {}),
      message: inviteForm.message.trim() || undefined,
    })
    ElMessage.success(inviteForm.invitationType === 2 ? '带价邀请已发送' : '邀请报价已发送')
    inviteVisible.value = false
  } finally {
    inviting.value = false
  }
}

async function handleMoreCommand(command) {
  if (!requireLogin()) return
  if (command === 'report') {
    reportVisible.value = true
    return
  }
  try {
    if (blocked.value) {
      await ElMessageBox.confirm(
        `解除对「${profile.value?.nickname}」的屏蔽后，双方将可以重新进行平台互动。`,
        '解除屏蔽',
        { confirmButtonText: '解除屏蔽', cancelButtonText: '取消', type: 'warning' },
      )
      await unblockUser(profileId.value)
      blocked.value = false
      ElMessage.success('已解除屏蔽')
    } else {
      await ElMessageBox.confirm(
        `加入黑名单后，您与「${profile.value?.nickname}」仍可查看彼此公开内容，但无法关注、邀请、应征、下单、建立会话或发送私信。待处理邀请会失效，尚未下单的应征关系会结束。`,
        '加入黑名单',
        { confirmButtonText: '确认屏蔽', cancelButtonText: '取消', type: 'warning' },
      )
      await blockUser(profileId.value)
      blocked.value = true
      followed.value = false
      inviteVisible.value = false
      ElMessage.success('已加入黑名单')
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') throw error
  }
}

function resetReportForm() {
  Object.assign(reportForm, { category: '', reason: '', images: [] })
  reportFileList.value = []
}

async function uploadReportImage(options) {
  try {
    const url = await uploadFile(options.file)
    reportForm.images.push(url)
    options.onSuccess(url)
  } catch (error) {
    options.onError(error)
  }
}

function removeReportImage(file) {
  const url = typeof file.response === 'string' ? file.response : file.url
  reportForm.images = reportForm.images.filter((item) => item !== url)
}

async function submitReport() {
  const reason = reportForm.reason.trim()
  if (!reportForm.category) return ElMessage.warning('请选择举报类型')
  if (reason.length < 10 || reason.length > 500) return ElMessage.warning('举报说明应为 10–500 字')
  if (reportFileList.value.some((item) => item.status === 'uploading')) return ElMessage.warning('请等待图片上传完成')
  reportSubmitting.value = true
  try {
    const result = await submitUserReport({
      targetUserId: profileId.value,
      category: reportForm.category,
      reason,
      images: reportForm.images,
    })
    reportVisible.value = false
    ElMessage.success(result?.recorded === false ? '您已举报过该用户，本次未重复计数' : '举报已提交')
  } finally {
    reportSubmitting.value = false
  }
}
function formatRate(value) { return value === null || value === undefined ? '暂无' : `${value}%` }
function previewWork(work) { previewUrl.value = work.image || '' }
function projectCover(project) {
  if (Array.isArray(project.images)) return project.images[0] || ''
  if (!project.images) return ''
  try {
    const images = JSON.parse(project.images)
    return Array.isArray(images) ? images[0] || '' : ''
  } catch {
    return String(project.images).split(',').map((item) => item.trim()).filter(Boolean)[0] || ''
  }
}

function publicProjectStatus(project) {
  if (Number(project.status) === 4) return { text: '已下架', type: 'info' }
  if (isProjectExpired(project.deadline)) return { text: '已过期', type: 'info' }
  if (Number(project.status) === 0) return { text: '招募中', type: 'success' }
  return { text: '已完成', type: 'info' }
}
function goStall(id) { router.push({ name: 'StallDetail', params: { id } }) }
function goProject(id) { router.push({ name: 'ProjectDetail', params: { id } }) }
</script>

<style scoped>
.public-profile-page { min-height: calc(100vh - 56px); background: #f3f7fb; }
.profile-cover { position: relative; height: 220px; overflow: hidden; background: linear-gradient(180deg, #9ee1ff 0%, #c9efff 62%, #7c9daf 100%); }
.profile-cover::before, .profile-cover::after { position: absolute; content: ''; border: 44px solid rgb(255 255 255 / 12%); border-radius: 50%; }
.profile-cover::before { width: 300px; height: 300px; top: -170px; left: 10%; }
.profile-cover::after { width: 520px; height: 520px; top: -330px; right: 2%; }
.cover-orb { position: absolute; border-radius: 50%; background: rgb(255 255 255 / 18%); }
.cover-orb--one { width: 44px; height: 44px; top: 58px; left: 29%; }
.cover-orb--two { width: 76px; height: 76px; bottom: 12px; left: 13%; }
.role-switch { position: absolute; z-index: 1; top: 22px; right: max(24px, calc((100vw - 1180px) / 2)); padding: 7px 14px; color: #fff; cursor: pointer; background: rgb(57 91 108 / 45%); border: 1px solid rgb(255 255 255 / 55%); border-radius: 18px; }
.profile-content { max-width: 1180px; padding-bottom: 70px; }
.profile-identity { position: relative; display: flex; gap: 22px; align-items: center; min-height: 94px; }
.profile-avatar { flex: none; margin-top: -62px; color: #fff; font-size: 34px; background: #9ba8b9; border: 5px solid #fff; box-shadow: 0 7px 20px rgb(36 68 90 / 14%); }
.identity-copy { align-self: flex-start; padding-top: 18px; }
.identity-title { display: flex; gap: 12px; align-items: center; }
.identity-title h1 { margin: 0; color: #202a35; font-size: 26px; }
.identity-badges { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
.profile-actions { display: flex; gap: 10px; margin-left: auto; }
.profile-actions :deep(.el-button:not(.more-button)) { min-width: 110px; }
.invite-button { color: var(--artiv-primary); border-color: var(--artiv-primary); }
.more-button { min-width: 34px !important; }
.profile-introduction { padding: 20px 0 24px; }
.profile-introduction h2 { margin: 0 0 10px; font-size: 16px; }
.profile-introduction p { margin: 0; color: #505c6d; line-height: 1.8; white-space: pre-wrap; }
.profile-metrics { display: flex; gap: 0; align-items: center; min-height: 106px; padding: 20px 28px; background: #fff; border: 1px solid #e5ebf1; border-radius: 8px; }
.metric-item { min-width: 170px; padding-right: 34px; margin-right: 34px; border-right: 1px solid #edf0f3; }
.metric-item span { display: block; color: #45505d; font-weight: 600; }
.metric-item strong { display: block; margin-top: 9px; color: #05a8e8; font-size: 30px; }
.metric-note { margin-left: auto; color: #9aa4af; font-size: 13px; }
.metric-dot { display: inline-block; width: 8px; height: 8px; margin-right: 6px; background: #58bd69; border-radius: 50%; }
.profile-showcase { margin-top: 28px; }
.profile-tabs { display: flex; gap: 44px; border-bottom: 1px solid #d8e0e8; }
.profile-tab { position: relative; padding: 0 4px 16px; color: #394453; cursor: pointer; background: transparent; border: 0; font-size: 16px; }
.profile-tab.active { color: #03a7e7; }
.profile-tab.active::after { position: absolute; right: 0; bottom: -1px; left: 0; height: 2px; content: ''; background: #03a7e7; }
.showcase-body { min-height: 250px; padding-top: 24px; }
.work-grid, .stall-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 18px; }
.work-card { position: relative; aspect-ratio: 1; padding: 0; overflow: hidden; cursor: pointer; background: #e9eef3; border: 0; border-radius: 6px; }
.work-card img { width: 100%; height: 100%; object-fit: cover; transition: transform .25s ease; }
.work-card span { position: absolute; right: 0; bottom: 0; left: 0; padding: 32px 12px 10px; overflow: hidden; color: #fff; text-align: left; text-overflow: ellipsis; white-space: nowrap; background: linear-gradient(transparent, rgb(0 0 0 / 70%)); }
.work-card:hover img { transform: scale(1.035); }
.stall-card { overflow: hidden; cursor: pointer; background: #fff; border: 1px solid #e0e6ec; border-radius: 7px; transition: transform .2s ease, box-shadow .2s ease; }
.stall-card:hover, .project-card:hover { transform: translateY(-3px); box-shadow: 0 10px 24px rgb(44 72 94 / 12%); }
.stall-card img { width: 100%; aspect-ratio: 1; object-fit: cover; }
.stall-card-copy { padding: 12px 14px 15px; }
.stall-card-copy h3, .project-card h3 { margin: 0; color: #27323f; font-size: 16px; }
.stall-card-copy p { margin: 7px 0; color: #7f8995; }
.stall-card-copy strong, .project-card strong { color: #f39119; font-size: 19px; }
.project-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }
.project-card { display: grid; grid-template-columns: 128px 1fr; min-height: 150px; overflow: hidden; cursor: pointer; background: #fff; border: 1px solid #e0e6ec; border-radius: 7px; transition: transform .2s ease, box-shadow .2s ease; }
.project-card > img, .project-cover-placeholder { width: 128px; height: 100%; min-height: 150px; object-fit: cover; }
.project-cover-placeholder { display: grid; color: #8d99a7; background: #edf2f6; place-items: center; }
.project-card-copy { min-width: 0; padding: 16px; }
.project-card-title { display: flex; gap: 10px; align-items: center; justify-content: space-between; }
.project-card-copy p { display: -webkit-box; min-height: 42px; margin: 13px 0 18px; overflow: hidden; color: #6d7784; line-height: 1.55; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.project-card-meta { display: flex; align-items: end; justify-content: space-between; }
.project-card-meta span { color: #7f8995; }
.load-sentinel { min-height: 46px; padding-top: 20px; color: #98a2ad; text-align: center; }
.invite-painter { display: flex; align-items: center; gap: 10px; color: #27323f; font-weight: 600; }
.invite-type-list { display: grid; width: 100%; gap: 10px; }
.invite-type-card { display: grid; gap: 6px; padding: 15px 18px; color: #354152; text-align: left; cursor: pointer; background: #fff; border: 1px solid #dfe6ed; border-radius: 8px; }
.invite-type-card strong { font-size: 16px; }
.invite-type-card span { color: #7b8794; line-height: 1.5; }
.invite-type-card.active { color: #059edb; background: #f0faff; border-color: #05a8e8; box-shadow: 0 0 0 1px rgb(5 168 232 / 10%); }
.invite-type-card:focus { outline: 2px solid #05a8e8; outline-offset: 1px; }
.project-budget-option { float: right; margin-left: 18px; color: #f39119; }
.invite-empty-tip { margin: 7px 0 0; color: #9099a5; font-size: 13px; }
.price-rule { margin-left: 12px; color: #8b95a1; font-size: 13px; }
@media (max-width: 900px) {
  .profile-content { padding-right: 20px; padding-left: 20px; }
  .profile-actions { position: absolute; right: 0; bottom: 6px; }
  .profile-metrics { flex-wrap: wrap; gap: 20px; }
  .metric-note { width: 100%; margin-left: 0; }
  .work-grid, .stall-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .project-grid { grid-template-columns: 1fr; }
}
@media (max-width: 560px) {
  .profile-cover { height: 150px; }
  .profile-identity { flex-wrap: wrap; align-items: flex-start; }
  .profile-avatar { width: 92px !important; height: 92px !important; margin-top: -46px; }
  .identity-title h1 { font-size: 20px; }
  .profile-actions { position: static; margin-left: 0; }
  .profile-actions :deep(.el-button) { min-width: auto; }
  .metric-item { min-width: 120px; padding-right: 18px; margin-right: 0; }
  .profile-tabs { gap: 18px; overflow-x: auto; }
  .project-card { grid-template-columns: 92px 1fr; }
  .project-card > img, .project-cover-placeholder { width: 92px; }
}
</style>
