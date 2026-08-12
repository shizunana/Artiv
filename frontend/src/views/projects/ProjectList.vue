<template>
  <div class="list-page">
    <div class="list-header page-container">
      <h1 class="section-title">约稿大厅</h1>
      <p class="section-subtitle">发布需求，寻找合适的画师</p>
    </div>

    <div class="page-container">
      <!-- 筛选区（复刻参考站 projects 列表页） -->
      <div class="filter-panel">
        <div class="filter-top">
          <el-tabs v-model="activeTab" class="purpose-tabs" @tab-change="load">
            <el-tab-pane label="全部" name="all" />
            <el-tab-pane label="招募中" name="recruiting" />
          </el-tabs>
          <el-input v-model="keyword" placeholder="搜索企划标题、需求关键词" clearable class="search-input" @keyup.enter="load" @clear="load">
            <template #append><el-button @click="load">搜索</el-button></template>
          </el-input>
        </div>
        <div class="filter-row">
          <el-select v-model="sort" class="filter-select" @change="load">
            <el-option label="最新发布" value="newest" />
            <el-option label="稿酬最高" value="budget" />
            <el-option label="应征数少" value="applications" />
          </el-select>
          <el-select v-model="tag" class="filter-select" @change="load">
            <el-option label="全部类型" value="all" />
            <el-option v-for="t in tagOptions" :key="t" :label="t" :value="t" />
          </el-select>
          <el-select v-model="budgetRange" class="filter-select" @change="load">
            <el-option label="稿酬区间不限" value="all" />
            <el-option label="500 以下" value="0-500" />
            <el-option label="500-1000" value="500-1000" />
            <el-option label="1000-3000" value="1000-3000" />
            <el-option label="3000 以上" value="3000-" />
          </el-select>
          <el-select v-model="deadlineRange" class="filter-select" @change="load">
            <el-option label="截稿日期不限" value="all" />
            <el-option label="7天以内" value="within-7" />
            <el-option label="7天以上" value="after-7" />
            <el-option label="14天以上" value="after-14" />
            <el-option label="30天以上" value="after-30" />
          </el-select>
          <div class="filter-switches">
            <span class="switch-label">仅看未选定</span>
            <el-switch v-model="unselectedOnly" size="small" @change="load" />
            <span class="switch-label">仅看人设卡</span>
            <el-switch v-model="characterCardOnly" size="small" disabled />
          </div>
        </div>
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

// 筛选状态（参考站筛选区）
const activeTab = ref('all')
const sort = ref('newest')
const tag = ref('all')
const budgetRange = ref('all')
const deadlineRange = ref('all')
const unselectedOnly = ref(false)
const characterCardOnly = ref(false)

const tagOptions = ['国潮', '插画', '立绘', '场景', '原画', '同人', 'Q版', '古风', 'Live2d']

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
    if (activeTab.value === 'recruiting' || unselectedOnly.value) {
      params.status = 0
    }
    if (sort.value === 'budget') {
      params.sort = 'budget'
    }
    if (tag.value && tag.value !== 'all') {
      params.tag = tag.value
    }
    if (budgetRange.value && budgetRange.value !== 'all') {
      const [min, max] = budgetRange.value.split('-')
      if (min) params.budgetMin = Number(min)
      if (max) params.budgetMax = Number(max)
    }
    if (deadlineRange.value && deadlineRange.value !== 'all') {
      if (deadlineRange.value.startsWith('within')) {
        params.deadlineWithin = Number(deadlineRange.value.split('-')[1])
      } else if (deadlineRange.value.startsWith('after')) {
        params.deadlineAfter = Number(deadlineRange.value.split('-')[1])
      }
    }
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

.filter-panel {
  background: #fff;
  border: 1px solid var(--huiyue-border);
  border-radius: 10px;
  padding: 6px 20px 16px;
  margin-bottom: 24px;
}

.filter-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.purpose-tabs {
  flex: 1;
}

.search-input {
  width: 300px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding-top: 10px;
  border-top: 1px solid var(--huiyue-border);
}

.filter-select {
  width: 150px;
}

.filter-switches {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  flex-wrap: wrap;
}

.switch-label {
  font-size: 13px;
  color: var(--huiyue-text-secondary);
  margin-left: 8px;
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
