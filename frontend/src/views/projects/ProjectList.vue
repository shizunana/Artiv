<template>
  <div class="list-page">
    <div class="page-container">
      <!-- 分类 Tab：类型 -->
      <div class="category-bar type-bar">
        <span
          v-for="c in categoryTabs"
          :key="c"
          class="category-tab"
          :class="{ 'is-active': activeCategory === c }"
          @click="activeCategory = c; page = 1; load()"
        >
          {{ c }}
        </span>
      </div>

      <!-- 筛选行：下拉靠左 + 搜索框靠右 -->
      <div class="filter-row">
        <el-select v-model="sort" class="filter-select" @change="load">
          <el-option label="最新发布" value="newest" />
          <el-option label="稿酬最高" value="budget" />
          <el-option label="应征数少" value="applications" />
        </el-select>
        <el-select v-model="subject" class="filter-select" @change="page = 1; load()">
          <el-option label="不限" value="all" />
          <el-option v-for="s in subjectOptions" :key="s" :label="s" :value="s" />
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
        </div>
        <el-input v-model="keyword" placeholder="搜索企划标题、需求关键词" clearable class="search-input" @keyup.enter="load" @clear="load">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>

      <div class="project-grid" v-loading="loading">
        <article
          v-for="project in projects"
          :key="project.id"
          class="project-card"
          role="link"
          tabindex="0"
          :aria-label="`查看企划 ${project.title}`"
          @click="goDetail(project.id)"
          @keydown.enter.self="goDetail(project.id)"
          @keydown.space.self.prevent="goDetail(project.id)"
        >
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
                <span v-for="item in subjectList(project.subject)" :key="item" class="tag tag-subject">{{ item }}</span>
              </div>
            </div>
            <div class="project-card__thumb">
              <img
                v-if="firstImage(project.images)"
                :src="firstImage(project.images)"
                :alt="`${project.title}封面`"
                class="project-img"
              />
              <div v-else class="thumb-placeholder"></div>
            </div>
          </div>

          <div class="project-card__divider"></div>

          <div class="project-card__info">
            <span class="info-item"><el-icon><Calendar /></el-icon>{{ project.deadline || '-' }} 截稿</span>
            <span class="info-item"><el-icon><Postcard /></el-icon>{{ project.category || '-' }}</span>
            <span class="info-item"><span class="applicant-pawn" aria-hidden="true">♟︎</span>{{ project.applicationCount }} 人应征</span>
            <span class="info-item info-budget">{{ formatProjectBudget(project) }}</span>
          </div>
        </article>
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
import { Calendar, Postcard, Search } from '@element-plus/icons-vue'
import { getApplications, getProjects } from '@/api/project'
import { SUBJECT_OPTIONS, CATEGORY_OPTIONS } from '@/constants/subjects'
import { PROJECT_STATUS, statusText, statusType } from '@/utils/status'
import { formatProjectBudget } from '@/utils/budget'
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
const activeCategory = ref('全部')
const sort = ref('newest')
const subject = ref('all')
const budgetRange = ref('all')
const deadlineRange = ref('all')
const unselectedOnly = ref(false)

// 类型选项与橱窗分类同步
const subjectOptions = SUBJECT_OPTIONS
const categoryTabs = ['全部', ...CATEGORY_OPTIONS]

function firstImage(images) {
  if (!images) return ''
  const arr = String(images).split(',').filter(Boolean)
  return arr[0] || ''
}

function subjectList(subjects) {
  if (!subjects) return []
  return String(subjects).split(',').filter(Boolean).slice(0, 3)
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (activeCategory.value !== '全部') {
      params.category = activeCategory.value
    }
    if (unselectedOnly.value) {
      params.status = 0
    }
    if (sort.value === 'budget') {
      params.sort = 'budget'
    }
    if (subject.value && subject.value !== 'all') {
      params.subject = subject.value
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
    const records = data.records || []
    projects.value = await Promise.all(records.map(async (project) => {
      try {
        const applications = await getApplications(project.id, { page: 1, size: 1 })
        return { ...project, applicationCount: applications.total || 0 }
      } catch {
        return { ...project, applicationCount: 0 }
      }
    }))
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

.list-page > .page-container {
  padding-top: 26px;
}

.category-bar {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--artiv-border);
}

.type-bar {
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.category-tab {
  padding: 6px 14px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid var(--artiv-border);
  color: var(--artiv-text-secondary);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
  white-space: nowrap;
}

.category-tab:hover {
  color: var(--artiv-primary);
  border-color: var(--artiv-primary);
}

.category-tab.is-active {
  background: var(--artiv-primary);
  border-color: var(--artiv-primary);
  color: #fff;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 0;
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
  color: var(--artiv-text-secondary);
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
  border: 1px solid var(--artiv-border);
  padding: 18px 24px;
  cursor: pointer;
  outline: none;
  transition: background-color .18s ease, border-color .18s ease, box-shadow .18s ease;
}

.project-card:hover,
.project-card:focus-visible {
  border-color: color-mix(in srgb, var(--artiv-primary) 36%, var(--artiv-border));
  background: #fbfdff;
  box-shadow: 0 5px 14px rgba(0, 0, 0, 0.06);
}

.project-card:hover .project-card__title { color: var(--artiv-primary); }

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
  color: var(--artiv-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-card__summary {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--artiv-text-secondary);
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
  color: var(--artiv-text);
  background: #f5f6f8;
  border: 1px solid var(--artiv-border);
  border-radius: 4px;
  padding: 2px 7px;
}

.tag-subject {
  color: var(--artiv-primary);
  border-color: color-mix(in srgb, var(--artiv-primary) 25%, #fff);
  background: var(--artiv-primary-light);
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
  object-fit: cover;
  cursor: pointer;
  transition: opacity .18s ease;
}

.project-card__thumb:hover .project-img { opacity: .88; }

.thumb-placeholder {
  width: 100%;
  height: 100%;
}

.project-card__divider {
  height: 1px;
  background: var(--artiv-border);
  margin: 14px 0;
}

.project-card__info {
  display: flex;
  align-items: center;
  gap: 26px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--artiv-text);
}

.info-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.info-item :deep(.el-icon) {
  color: currentColor;
  font-size: 14px;
}

.applicant-pawn {
  font-family: "Segoe UI Symbol", serif;
  font-size: 15px;
  line-height: 1;
}

.info-budget {
  margin-left: auto;
  color: var(--artiv-accent);
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
