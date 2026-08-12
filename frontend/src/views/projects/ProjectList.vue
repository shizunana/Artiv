<template>
  <div class="list-page">
    <div class="list-header page-container">
      <h1 class="section-title">约稿大厅</h1>
      <p class="section-subtitle">发布需求，寻找合适的画师</p>
    </div>

    <div class="page-container">
      <!-- 状态 Tab（参考站橱窗列表同款布局） -->
      <div class="category-bar">
        <el-tabs v-model="activeTab" class="purpose-tabs" @tab-change="load">
          <el-tab-pane label="全部" name="all" />
          <el-tab-pane label="招募中" name="recruiting" />
        </el-tabs>
      </div>

      <!-- 筛选行：下拉靠左 + 搜索框靠右 -->
      <div class="filter-row">
        <el-select v-model="sort" class="filter-select" @change="load">
          <el-option label="最新发布" value="newest" />
          <el-option label="稿酬最高" value="budget" />
          <el-option label="应征数少" value="applications" />
        </el-select>
        <el-select v-model="tag" class="filter-select" @change="load">
          <el-option label="类型" value="all" />
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
        <el-input v-model="keyword" placeholder="搜索企划标题、需求关键词" clearable class="search-input" @keyup.enter="load" @clear="load">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>

      <div class="project-grid" v-loading="loading">
        <div v-for="project in projects" :key="project.id" class="project-card" @click="goDetail(project.id)">
          <div class="project-card__top">
            <div class="project-card__main">
              <div class="project-card__header">
                <h3 class="project-card__title">{{ project.title }}</h3>
                <el-tag class="project-status" :type="statusType(PROJECT_STATUS, project.status)" size="small">
                  {{ statusText(PROJECT_STATUS, project.status) }}
                </el-tag>
              </div>
              <p class="project-card__summary">{{ project.description }}</p>
              <div class="project-card__tags">
                <span class="tag">{{ project.mode === 1 ? '定向邀请' : '公开招募' }}</span>
                <span v-for="t in tagList(project.tags)" :key="t" class="tag">{{ t }}</span>
              </div>
            </div>
            <div class="project-card__thumb" @click.stop>
              <el-image
                v-if="firstImage(project.images)"
                :src="firstImage(project.images)"
                :preview-src-list="imageList(project.images)"
                preview-teleported
                fit="cover"
                class="project-img"
                :initial-index="0"
              />
              <div v-else class="thumb-placeholder"></div>
            </div>
          </div>

          <div class="project-card__divider"></div>

          <div class="project-card__info">
            <span class="info-item">📅 {{ project.deadline || '-' }} 截稿</span>
            <span class="info-item">🏷️ {{ firstTag(project.tags) }}</span>
            <span class="info-item">👤 0 人应征</span>
            <span class="info-item info-budget">¥{{ project.budget }}</span>
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
import { Search } from '@element-plus/icons-vue'
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

// 类型选项与橱窗分类同步
const tagOptions = ['头像', 'Q版全身', '半身像', '立绘', '组合页', '服设', '壁纸', '表情包', 'Live2d', '平面设计']

function firstImage(images) {
  if (!images) return ''
  const arr = String(images).split(',').filter(Boolean)
  return arr[0] || ''
}

function imageList(images) {
  if (!images) return []
  return String(images).split(',').filter(Boolean)
}

function firstTag(tags) {
  if (!tags) return '-'
  const arr = String(tags).split(',').filter(Boolean)
  return arr[0] || '-'
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

.category-bar {
  display: flex;
  align-items: center;
  padding: 10px 0 0;
  border-bottom: 1px solid var(--huiyue-border);
}

.purpose-tabs {
  width: 100%;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  flex-wrap: nowrap;
}

.filter-select {
  width: 140px;
  flex-shrink: 0;
}

.filter-switches {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

.switch-label {
  font-size: 13px;
  color: var(--huiyue-text-secondary);
  margin-left: 8px;
  white-space: nowrap;
}

.search-input {
  width: 240px;
  flex-shrink: 0;
  margin-left: auto;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 20px;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.project-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid var(--huiyue-border);
  padding: 18px 24px;
  cursor: pointer;
  transition: box-shadow 0.3s, transform 0.3s;
}

.project-card:hover {
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.project-card__top {
  display: flex;
  justify-content: space-between;
  gap: 24px;
}

.project-card__main {
  flex: 1;
  min-width: 0;
}

.project-card__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.project-status {
  flex-shrink: 0;
}

.project-card__title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: var(--huiyue-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-card__summary {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--huiyue-text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.project-card__tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag {
  font-size: 12px;
  color: var(--huiyue-text);
  background: #f5f6f8;
  border: 1px solid var(--huiyue-border);
  border-radius: 4px;
  padding: 2px 7px;
}

.project-card__thumb {
  width: 100px;
  height: 100px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  background: transparent;
}

.project-img {
  width: 100%;
  height: 100%;
  display: block;
  cursor: zoom-in;
}

.thumb-placeholder {
  width: 100%;
  height: 100%;
}

.project-card__divider {
  height: 1px;
  background: var(--huiyue-border);
  margin: 14px 0;
}

.project-card__info {
  display: flex;
  align-items: center;
  gap: 26px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--huiyue-text);
}

.info-budget {
  margin-left: auto;
  color: var(--huiyue-accent);
  font-weight: 700;
  font-size: 15px;
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

  .project-card__thumb {
    display: none;
  }
}
</style>
