<template>
  <div class="list-page">
    <div class="list-header page-container">
      <h1 class="section-title">约稿大厅</h1>
      <p class="section-subtitle">发布需求，寻找合适的画师</p>
    </div>

    <div class="page-container">
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <div class="filter-tabs">
          <span
            v-for="s in statusFilters"
            :key="s.value"
            class="filter-tab"
            :class="{ 'is-active': status === s.value }"
            @click="status = s.value; load()"
          >
            {{ s.label }}
          </span>
        </div>
        <el-input v-model="keyword" placeholder="搜索企划标题" clearable class="search-input" @keyup.enter="load" @clear="load">
          <template #append><el-button @click="load">搜索</el-button></template>
        </el-input>
      </div>

      <div class="project-grid" v-loading="loading">
        <div v-for="project in projects" :key="project.id" class="project-card" @click="goDetail(project.id)">
          <div class="project-card__thumb" :style="gradient(project.id)">
            <span class="project-emoji">📋</span>
            <el-tag class="project-status" :type="statusType(PROJECT_STATUS, project.status)" size="small">
              {{ statusText(PROJECT_STATUS, project.status) }}
            </el-tag>
          </div>
          <div class="project-card__body">
            <h3>{{ project.title }}</h3>
            <p>{{ project.description }}</p>
            <div class="project-card__meta">
              <span class="budget">¥{{ project.budget }}</span>
              <span class="deadline">截止 {{ project.deadline || '-' }}</span>
            </div>
            <div class="project-card__tags">
              <span v-for="tag in tagList(project.tags)" :key="tag" class="tag">{{ tag }}</span>
            </div>
          </div>
        </div>
      </div>

      <EmptyState v-if="!loading && projects.length === 0" description="暂无符合条件的企划" />
      <div class="pager">
        <Pagination v-model:page="page" v-model:size="size" :total="total" @change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getProjects } from '@/api/project'
import { PROJECT_STATUS, statusText, statusType } from '@/utils/status'
import Pagination from '@/components/common/Pagination.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const router = useRouter()
const projects = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(12)
const total = ref(0)
const keyword = ref('')
const status = ref(null)

const statusFilters = [
  { label: '全部', value: null },
  { label: '招募中', value: 0 },
  { label: '已选定', value: 1 },
  { label: '进行中', value: 2 },
  { label: '已完成', value: 3 },
]

const gradients = [
  'linear-gradient(135deg, #4facfe, #00f2fe)',
  'linear-gradient(135deg, #f093fb, #f5576c)',
  'linear-gradient(135deg, #43e97b, #38f9d7)',
  'linear-gradient(135deg, #fa709a, #fee140)',
]

function gradient(id) {
  return { background: gradients[id % gradients.length] }
}

function tagList(tags) {
  if (!tags) return []
  return String(tags).split(',').filter(Boolean).slice(0, 3)
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (status.value !== null) params.status = status.value
    const data = await getProjects(params)
    projects.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  router.push({ name: 'ProjectDetail', params: { id } })
}

onMounted(load)
</script>

<style scoped>
.list-page {
  min-height: 60vh;
  padding-bottom: 48px;
}

.list-header {
  padding-top: 48px;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.filter-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-tab {
  padding: 8px 20px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid var(--huiyue-border);
  color: var(--huiyue-text-secondary);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.filter-tab:hover {
  color: var(--huiyue-primary);
  border-color: var(--huiyue-primary);
}

.filter-tab.is-active {
  background: var(--huiyue-primary);
  border-color: var(--huiyue-primary);
  color: #fff;
}

.search-input {
  width: 260px;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.project-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid var(--huiyue-border);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.project-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}

.project-card__thumb {
  aspect-ratio: 16 / 7;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.project-emoji {
  font-size: 40px;
}

.project-status {
  position: absolute;
  top: 10px;
  right: 10px;
}

.project-card__body {
  padding: 16px;
}

.project-card__body h3 {
  margin: 0 0 8px;
  font-size: 16px;
  color: var(--huiyue-text);
}

.project-card__body p {
  margin: 0 0 12px;
  color: var(--huiyue-text-secondary);
  font-size: 13px;
  line-height: 1.7;
  min-height: 42px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.project-card__meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.budget {
  color: var(--huiyue-primary);
  font-weight: 600;
}

.deadline {
  color: var(--huiyue-text-secondary);
  font-size: 12px;
}

.project-card__tags {
  display: flex;
  gap: 6px;
}

.tag {
  font-size: 12px;
  color: var(--huiyue-primary);
  background: var(--huiyue-primary-light);
  padding: 2px 8px;
  border-radius: 8px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

@media (max-width: 900px) {
  .project-grid {
    grid-template-columns: 1fr;
  }
}
</style>
