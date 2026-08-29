<template>
  <div class="my-projects wb-card">
    <header class="project-header">
      <div>
        <h2>我的企划</h2>
        <p>统一查看公开企划与定向邀请。</p>
      </div>
      <div class="header-actions">
        <el-button type="danger" plain @click="deleteVisible = true">删除企划</el-button>
        <el-button type="primary" round @click="router.push({ name: 'ProjectNew' })">+ 发布企划</el-button>
      </div>
    </header>

    <div class="mode-row">
      <el-tabs v-model="modeTab" class="mode-tabs" @tab-change="changeMode">
        <el-tab-pane label="公开企划" name="0" />
        <el-tab-pane label="定向邀请" name="1" />
      </el-tabs>
    </div>

    <div v-loading="loading" class="project-cards">
      <el-empty v-if="!projects.length" :description="emptyDescription" />
      <ProjectListItem
        v-for="project in projects"
        :key="project.id"
        :project="project"
        @open-detail="goDetail"
        @open-panel="goPanel"
      />
    </div>

    <div v-if="total" class="pager">
      <Pagination v-model:page="page" v-model:size="size" :total="total" @change="load" />
    </div>

    <ProjectDeleteDialog
      v-model="deleteVisible"
      :mode="Number(modeTab)"
      @deleted="handleDeletedProjects"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyProjects } from '@/api/project'
import Pagination from '@/components/common/Pagination.vue'
import ProjectDeleteDialog from '@/components/projects/ProjectDeleteDialog.vue'
import ProjectListItem from '@/components/projects/ProjectListItem.vue'

const router = useRouter()
const projects = ref([])
const loading = ref(false)
const deleteVisible = ref(false)
const modeTab = ref('0')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const emptyDescription = computed(() => modeTab.value === '0' ? '还没有发布公开企划' : '还没有发布定向邀请')

function goDetail(id) {
  router.push({ name: 'ProjectDetail', params: { id } })
}

function goPanel(id) {
  const target = router.resolve({ name: 'ProjectPanel', params: { id } })
  window.open(target.href, '_blank', 'noopener,noreferrer')
}

function changeMode() {
  page.value = 1
  load()
}

async function handleDeletedProjects() {
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
.mode-row { display: flex; align-items: flex-end; gap: 18px; margin-top: 18px; border-bottom: 1px solid var(--artiv-border); }
.mode-tabs { min-width: 0; flex: 1; }
.mode-tabs :deep(.el-tabs__header) { margin-bottom: 0; }
.mode-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.project-cards { min-width: 0; min-height: 360px; padding-top: 16px; }
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
@media (max-width: 720px) {
  .project-header { flex-direction: column; }
  .mode-row { align-items: stretch; flex-direction: column; gap: 8px; }
}
</style>
