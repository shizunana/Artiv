<template>
  <div class="my-projects wb-card">
    <header class="project-header">
      <div>
        <h2>我的企划</h2>
        <p>统一查看公开企划与定向邀请。</p>
      </div>
      <div class="header-actions">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" round @click="router.push({ name: 'ProjectNew' })">+ 发布企划</el-button>
      </div>
    </header>

    <el-tabs v-model="modeTab" class="mode-tabs" @tab-change="changeMode">
      <el-tab-pane label="公开企划" name="0" />
      <el-tab-pane label="定向邀请" name="1" />
    </el-tabs>

    <div v-loading="loading" class="project-cards">
      <el-empty v-if="!projects.length" :description="emptyDescription" />
      <article v-for="project in projects" :key="project.id" class="project-card" @click="goDetail(project.id)">
        <img v-if="firstImage(project.images)" :src="firstImage(project.images)" :alt="`${project.title}封面`" />
        <div v-else class="project-cover">企划</div>

        <div class="project-main">
          <div class="project-title-row">
            <h3 :title="project.title">{{ truncate(project.title) }}</h3>
            <el-tag :type="cardStatus(project).type">{{ cardStatus(project).text }}</el-tag>
          </div>
          <p>类型：{{ project.category || '未分类' }} · 发布时间：{{ formatDateTime(project.createdAt) }}</p>
          <p class="project-description" :title="project.description || ''">
            企划详情：{{ truncate(project.description || '暂无详情', 15) }}
          </p>
        </div>

        <div class="project-budget">
          <small>预算区间</small>
          <strong>{{ formatProjectBudget(project) }}</strong>
          <div class="project-actions">
            <el-button type="primary" link @click.stop="goDetail(project.id)">企划详情</el-button>
            <el-button type="primary" link @click.stop="openManage(project)">编辑</el-button>
          </div>
        </div>
      </article>
    </div>

    <div v-if="total" class="pager">
      <Pagination v-model:page="page" v-model:size="size" :total="total" @change="load" />
    </div>

    <ProjectManageDialog
      v-model="manageVisible"
      :project="managingProject"
      @changed="handleProjectChanged"
      @deleted="load"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyProjects } from '@/api/project'
import { formatProjectBudget } from '@/utils/budget'
import { formatDateTime } from '@/utils/date'
import { PROJECT_STATUS, statusText, statusType } from '@/utils/status'
import Pagination from '@/components/common/Pagination.vue'
import ProjectManageDialog from '@/components/projects/ProjectManageDialog.vue'
import { isProjectExpired } from '@/utils/projectDeadline'

const router = useRouter()
const projects = ref([])
const loading = ref(false)
const modeTab = ref('0')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const manageVisible = ref(false)
const managingProject = ref(null)
const emptyDescription = computed(() => modeTab.value === '0' ? '还没有发布公开企划' : '还没有发布定向邀请')

function firstImage(images) {
  if (!images) return ''
  try {
    const parsed = JSON.parse(images)
    if (Array.isArray(parsed)) return parsed[0] || ''
  } catch { /* 兼容历史逗号分隔数据 */ }
  return String(images).split(',').map((item) => item.trim()).find(Boolean) || ''
}

function truncate(value, limit = 10) {
  const characters = Array.from(String(value || ''))
  return characters.length > limit ? `${characters.slice(0, limit).join('')}...` : characters.join('')
}

function cardStatus(project) {
  if (Number(project.moderationStatus) === 1) return { text: '违规下架', type: 'danger' }
  if (Number(project.status) === 4) {
    return Number(project.mode) === 1 && Number(project.invitationStatus) === 2
      ? { text: '已拒绝', type: 'danger' }
      : { text: '已下架', type: 'info' }
  }
  if (isProjectExpired(project.deadline)) return { text: '已过期', type: 'info' }
  if (Number(project.mode) === 1) {
    const invitation = {
      0: { text: '待回应', type: 'warning' },
      1: { text: '已接受', type: 'success' },
      2: { text: '已拒绝', type: 'danger' },
    }[Number(project.invitationStatus)]
    if (invitation) return invitation
  }
  return {
    text: statusText(PROJECT_STATUS, project.status),
    type: statusType(PROJECT_STATUS, project.status),
  }
}

function goDetail(id) {
  router.push({ name: 'ProjectDetail', params: { id } })
}

function openManage(project) {
  managingProject.value = project
  manageVisible.value = true
}

function changeMode() {
  page.value = 1
  load()
}

async function handleProjectChanged(updated) {
  if (modeTab.value === '1' && Number(updated?.mode) === 0) {
    modeTab.value = '0'
    page.value = 1
  }
  await load()
}

async function load() {
  loading.value = true
  try {
    const data = await getMyProjects({
      page: page.value,
      size: size.value,
      mode: Number(modeTab.value),
    })
    projects.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.my-projects { min-width: 0; }
.wb-card { min-width: 0; background: #fff; border-radius: 8px; padding: 24px; }
.project-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.project-header h2 { margin: 0 0 7px; font-size: 21px; }
.project-header p { margin: 0; color: var(--artiv-text-secondary); }
.header-actions { display: flex; align-items: center; gap: 10px; }
.header-actions :deep(.el-button + .el-button) { margin-left: 0; }
.mode-tabs { margin-top: 18px; }
.mode-tabs :deep(.el-tabs__header) { margin-bottom: 0; }
.project-cards { min-width: 0; min-height: 360px; padding-top: 16px; }
.project-card { display: grid; width: 100%; box-sizing: border-box; grid-template-columns: 92px minmax(0, 1fr) minmax(220px, 260px); align-items: center; gap: 18px; padding: 16px 0; border-bottom: 1px solid var(--artiv-border); cursor: pointer; }
.project-card:hover { background: #fbfdff; }
.project-card img, .project-cover { width: 92px; height: 72px; border-radius: 7px; object-fit: cover; }
.project-cover { display: grid; place-items: center; background: var(--artiv-section-alt); color: var(--artiv-text-secondary); }
.project-main { min-width: 0; }
.project-title-row { display: flex; min-width: 0; align-items: center; gap: 10px; }
.project-title-row h3 { min-width: 0; margin: 0; overflow: hidden; font-size: 16px; text-overflow: ellipsis; white-space: nowrap; }
.project-main p { margin: 8px 0 0; overflow: hidden; color: var(--artiv-text-secondary); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.project-main .project-description { color: var(--artiv-text); }
.project-budget { display: grid; min-width: 0; justify-items: end; gap: 7px; }
.project-budget small { color: var(--artiv-text-secondary); }
.project-budget strong { color: var(--artiv-accent); font-size: 18px; white-space: nowrap; }
.project-actions { display: flex; width: 100%; flex-wrap: wrap; justify-content: flex-end; gap: 4px 8px; }
.project-actions :deep(.el-button + .el-button) { margin-left: 0; }
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
@media (max-width: 720px) {
  .project-header { flex-direction: column; }
  .project-card { grid-template-columns: 70px minmax(0, 1fr); }
  .project-card img, .project-cover { width: 70px; }
  .project-budget { grid-column: 2; justify-items: start; }
  .project-actions { justify-content: flex-start; }
}
</style>
