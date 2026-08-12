<template>
  <div class="list-page page-container">
    <!-- 筛选区（复刻参考站 projects 列表页） -->
    <div class="filter-panel">
      <el-tabs v-model="activeTab" class="purpose-tabs" @tab-change="load">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="招募中" name="recruiting" />
      </el-tabs>

      <div class="filter-row">
        <el-select v-model="sort" class="filter-select" @change="load">
          <el-option label="最新发布" value="newest" />
          <el-option label="稿酬最高" value="budget" />
          <el-option label="应征数少" value="applications" />
        </el-select>
        <el-select v-model="tag" class="filter-select" @change="load">
          <el-option label="类型与风格" value="" />
          <el-option v-for="t in tagOptions" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="budgetRange" class="filter-select" @change="load">
          <el-option label="稿酬区间不限" value="" />
          <el-option label="500 以下" value="0-500" />
          <el-option label="500-1000" value="500-1000" />
          <el-option label="1000-3000" value="1000-3000" />
          <el-option label="3000 以上" value="3000-" />
        </el-select>
        <el-select v-model="deadlineRange" class="filter-select" @change="load">
          <el-option label="截稿日期不限" value="" />
          <el-option label="7天以内" value="within-7" />
          <el-option label="7天以上" value="after-7" />
          <el-option label="14天以上" value="after-14" />
          <el-option label="30天以上" value="after-30" />
        </el-select>
        <div class="filter-switches">
          <span class="switch-label">仅看认证企业</span>
          <el-switch v-model="certifiedOnly" size="small" disabled />
          <span class="switch-label">仅看未选定</span>
          <el-switch v-model="unselectedOnly" size="small" @change="load" />
          <span class="switch-label">仅看人设卡</span>
          <el-switch v-model="characterCardOnly" size="small" disabled />
        </div>
      </div>
    </div>

    <!-- 企划卡片列表（复刻参考站 project-cell） -->
    <div class="project-list" v-loading="loading">
      <div v-for="project in projects" :key="project.id" class="project-cell" @click="goDetail(project.id)">
        <div class="project-cell__top">
          <div class="project-cell__main">
            <h5 class="project-cell__title">{{ project.title }}</h5>
            <p class="project-cell__summary">{{ project.description }}</p>
            <div class="project-cell__tags">
              <span class="cell-tag">{{ project.mode === 1 ? '定向邀请' : '公开招募' }}</span>
            </div>
          </div>
          <div class="project-cell__thumb" :style="gradient(project.id)">
            <span class="thumb-emoji">📋</span>
          </div>
        </div>

        <div class="project-cell__divider"></div>

        <div class="project-cell__info">
          <span class="info-item">
            <el-icon class="info-icon"><Calendar /></el-icon>
            {{ project.deadline || '-' }} 截稿
          </span>
          <span class="info-item">
            <el-icon class="info-icon"><PriceTag /></el-icon>
            {{ firstTag(project.tags) }}
          </span>
          <span class="info-item">
            <el-icon class="info-icon"><User /></el-icon>
            0 人应征
          </span>
          <span class="info-item info-budget">¥{{ project.budget }}</span>
        </div>
      </div>
    </div>

    <EmptyState v-if="!loading && projects.length === 0" description="暂无符合条件的企划" />
    <div class="pager">
      <Pagination v-model:page="page" v-model:size="size" :total="total" @change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Calendar, PriceTag, User } from '@element-plus/icons-vue'
import { getProjects } from '@/api/project'
import Pagination from '@/components/common/Pagination.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const router = useRouter()
const projects = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

// 筛选状态
const activeTab = ref('all')
const sort = ref('newest')
const tag = ref('')
const budgetRange = ref('')
const deadlineRange = ref('')
const certifiedOnly = ref(false)
const unselectedOnly = ref(false)
const characterCardOnly = ref(false)

const tagOptions = ['国潮', '插画', '立绘', '场景', '原画', '同人', 'Q版', '古风', 'Live2d']

const gradients = [
  'linear-gradient(135deg, #4facfe, #00f2fe)',
  'linear-gradient(135deg, #f093fb, #f5576c)',
  'linear-gradient(135deg, #43e97b, #38f9d7)',
  'linear-gradient(135deg, #fa709a, #fee140)',
  'linear-gradient(135deg, #30cfd0, #330867)',
  'linear-gradient(135deg, #a8edea, #fed6e3)',
]

function gradient(id) {
  return { background: gradients[id % gradients.length] }
}

function firstTag(tags) {
  if (!tags) return '-'
  const arr = String(tags).split(',').filter(Boolean)
  return arr[0] || '-'
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }

    // Tab：全部 / 招募中
    if (activeTab.value === 'recruiting' || unselectedOnly.value) {
      params.status = 0
    }
    // 排序
    if (sort.value === 'budget') {
      params.sort = 'budget'
    }
    // 类型与风格
    if (tag.value) {
      params.tag = tag.value
    }
    // 稿酬区间
    if (budgetRange.value) {
      const [min, max] = budgetRange.value.split('-')
      if (min) params.budgetMin = Number(min)
      if (max) params.budgetMax = Number(max)
    }
    // 截稿日期
    if (deadlineRange.value) {
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
  padding: 16px 16px 48px;
  max-width: 1100px;
}

/* ---------- 筛选区 ---------- */
.filter-panel {
  background: #fff;
  border: 1px solid var(--huiyue-border);
  border-radius: 10px;
  padding: 6px 20px 16px;
  margin-bottom: 18px;
}

.purpose-tabs {
  margin-bottom: 6px;
}

.purpose-tabs :deep(.el-tabs__header) {
  margin-bottom: 10px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
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

/* ---------- 企划卡片（复刻参考站 project-cell） ---------- */
.project-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.project-cell {
  background: #fff;
  border: 1px solid var(--huiyue-border);
  border-radius: 12px;
  padding: 18px 24px;
  cursor: pointer;
  transition: box-shadow 0.3s, transform 0.3s;
}

.project-cell:hover {
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.project-cell__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.project-cell__main {
  flex: 1;
  min-width: 0;
}

.project-cell__title {
  margin: 0 0 8px;
  font-size: 17px;
  font-weight: 600;
  color: var(--huiyue-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-cell__summary {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--huiyue-text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.cell-tag {
  display: inline-block;
  font-size: 12px;
  color: var(--huiyue-text);
  background: #f5f6f8;
  border: 1px solid var(--huiyue-border);
  border-radius: 4px;
  padding: 2px 6px;
  margin-right: 6px;
}

.project-cell__thumb {
  width: 88px;
  height: 88px;
  border-radius: 12px;
  flex-shrink: 0;
  margin-left: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb-emoji {
  font-size: 36px;
}

.project-cell__divider {
  height: 1px;
  background: var(--huiyue-border);
  margin: 14px 0;
}

.project-cell__info {
  display: flex;
  align-items: center;
  gap: 26px;
  flex-wrap: wrap;
}

.info-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: var(--huiyue-text);
  white-space: nowrap;
}

.info-icon {
  font-size: 14px;
  color: var(--huiyue-text-secondary);
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

@media (max-width: 760px) {
  .filter-switches {
    margin-left: 0;
  }

  .project-cell__thumb {
    display: none;
  }
}
</style>
