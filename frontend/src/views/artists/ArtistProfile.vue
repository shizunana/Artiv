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
            <span v-if="isPainter" class="rating-placeholder" title="评分功能预留">★ 5.0</span>
          </div>
          <div class="identity-badges">
            <el-tag v-if="isPainter" size="small" type="success" effect="light">认证画师</el-tag>
            <el-tag v-else size="small" type="info" effect="light">企划方</el-tag>
            <el-tag v-for="tag in profile.tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
          </div>
        </div>
        <div class="profile-actions">
          <el-button v-if="isOwner" @click="goEditProfile">编辑资料</el-button>
          <template v-else-if="isPainter">
            <el-button class="invite-button" type="warning" @click="showReserved('邀请画师')">邀请</el-button>
            <el-button type="primary" @click="showReserved('关注画师')">关注</el-button>
          </template>
        </div>
      </section>

      <section class="profile-introduction">
        <h2>{{ isPainter ? '画师简介' : '企划方简介' }}</h2>
        <p>{{ profile.bio || (isPainter ? '这位画师暂未填写个人简介。' : '这位企划方暂未填写个人简介。') }}</p>
      </section>

      <section class="profile-metrics" aria-label="履约数据">
        <div class="metric-item">
          <span>需求完成率</span>
          <strong>{{ formatRate(profile.completionRate) }}</strong>
        </div>
        <div v-if="isPainter" class="metric-item">
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
                  <el-tag size="small" :type="project.status === 0 ? 'success' : 'info'">{{ project.status === 0 ? '招募中' : '已完成' }}</el-tag>
                </div>
                <p>{{ project.description || '暂无需求描述' }}</p>
                <div class="project-card-meta"><span>{{ project.category || '未分类' }}</span><strong>¥{{ project.budget }}</strong></div>
              </div>
            </article>
          </div>

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
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import EmptyState from '@/components/common/EmptyState.vue'
import { getPublicProfile, getPublicProfileProjects, getPublicProfileStalls, getPublicProfileWorks } from '@/api/user'
import { useUserStore } from '@/stores/user'

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
      { key: 'reviews', label: '合作评价', count: null },
    ]
  : [
      { key: 'projects', label: '发布企划', count: profile.value?.publicProjectCount ?? 0 },
      { key: 'reviews', label: '合作评价', count: null },
    ])
const emptyDescription = computed(() => ({
  works: '暂无公开作品', stalls: '暂无上架橱窗', projects: '暂无公开企划', reviews: '合作评价功能预留，暂未开放',
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
function goEditProfile() { router.push(isPainter.value ? { name: 'PainterProfile' } : { name: 'DashboardHome' }) }
function showReserved(name) { ElMessage.info(`${name}功能已预留，暂未开放`) }
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
.rating-placeholder { color: #f59a23; font-size: 17px; }
.identity-badges { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
.profile-actions { display: flex; gap: 10px; margin-left: auto; }
.profile-actions :deep(.el-button) { min-width: 110px; }
.invite-button { color: #fff; }
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
