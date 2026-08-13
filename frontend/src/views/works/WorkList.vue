<template>
  <div class="list-page page-container">
    <div class="category-bar">
      <span v-for="category in categories" :key="category" class="category-tab" :class="{ 'is-active': activeCategory === category }" @click="setCategory(category)">
        {{ category }}
      </span>
    </div>

    <div class="filter-row">
      <el-select v-model="sort" class="sort-select" @change="reload">
        <el-option label="最新发布" value="newest" />
        <el-option label="最早发布" value="oldest" />
      </el-select>
      <el-select v-model="subject" class="filter-select" @change="reload">
        <el-option label="题材风格" value="all" />
        <el-option v-for="item in subjectOptions" :key="item" :label="item" :value="item" />
      </el-select>
      <el-input v-model="keyword" clearable class="search-input" placeholder="搜索作品标题、作品说明" @keyup.enter="reload" @clear="reload">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>

    <div v-loading="loading" class="work-waterfall">
      <article v-for="work in works" :key="work.id" class="work-card">
        <div class="work-cover">
          <el-image v-if="work.image" :src="work.image" fit="cover" class="work-image" :preview-src-list="[work.image]" preview-teleported />
          <div v-else class="work-placeholder"><span>🎨</span></div>
          <div class="work-overlay">
            <strong :title="work.title">{{ work.title }}</strong>
            <span>{{ work.category }}</span>
          </div>
        </div>
        <div class="work-meta">
          <span class="subject-list">{{ work.subject || '未设置题材风格' }}</span>
          <span class="public-label">公开作品</span>
        </div>
      </article>
    </div>

    <EmptyState v-if="!loading && works.length === 0" description="暂无符合筛选条件的公开作品" />
    <div v-if="total > size" class="pager">
      <Pagination v-model:page="page" v-model:size="size" :total="total" @change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getWorks } from '@/api/work'
import { CATEGORY_OPTIONS, SUBJECT_OPTIONS } from '@/constants/subjects'
import EmptyState from '@/components/common/EmptyState.vue'
import Pagination from '@/components/common/Pagination.vue'

const works = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const activeCategory = ref('全部')
const subject = ref('all')
const keyword = ref('')
const sort = ref('newest')
const categories = ['全部', ...CATEGORY_OPTIONS]
const subjectOptions = SUBJECT_OPTIONS

function setCategory(category) {
  activeCategory.value = category
  reload()
}

function reload() {
  page.value = 1
  load()
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value, sort: sort.value }
    if (activeCategory.value !== '全部') params.category = activeCategory.value
    if (subject.value !== 'all') params.subject = subject.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const data = await getWorks(params)
    works.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.list-page { min-height: 60vh; max-width: 1100px; padding: 16px 16px 48px; }
.category-bar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; padding: 14px 0; border-bottom: 1px solid var(--huiyue-border); }
.category-tab { padding: 7px 18px; border-radius: 18px; background: #fff; border: 1px solid var(--huiyue-border); color: var(--huiyue-text-secondary); cursor: pointer; font-size: 14px; transition: all .2s; }
.category-tab:hover { color: var(--huiyue-primary); border-color: var(--huiyue-primary); }
.category-tab.is-active { color: #fff; background: var(--huiyue-primary); border-color: var(--huiyue-primary); }
.filter-row { display: flex; align-items: center; gap: 12px; padding: 14px 0; }
.sort-select { width: 150px; }.filter-select { width: 140px; }.search-input { width: 260px; margin-left: auto; }.search-input :deep(.el-input__wrapper) { border-radius: 20px; }
.work-waterfall { columns: 5 180px; column-gap: 18px; min-height: 160px; }
.work-card { break-inside: avoid; margin: 0 0 18px; border: 1px solid var(--huiyue-border); border-radius: 10px; overflow: hidden; background: #fff; transition: transform .2s, box-shadow .2s; }
.work-card:hover { transform: translateY(-3px); box-shadow: 0 8px 20px rgba(0, 0, 0, .08); }
.work-cover { position: relative; background: var(--huiyue-section-alt); }
.work-image { display: block; width: 100%; height: auto; min-height: 150px; }
.work-placeholder { display: grid; place-items: center; min-height: 210px; background: linear-gradient(135deg, #e2f4fd, #efe8ff); color: #6d9db4; font-size: 44px; }
.work-overlay { position: absolute; inset: auto 0 0; display: flex; align-items: flex-end; justify-content: space-between; gap: 8px; padding: 24px 10px 9px; background: linear-gradient(transparent, rgba(0, 0, 0, .72)); color: #fff; }
.work-overlay strong { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; }.work-overlay span { flex-shrink: 0; font-size: 12px; opacity: .9; }
.work-meta { display: flex; justify-content: space-between; gap: 8px; padding: 9px 10px; color: var(--huiyue-text-secondary); font-size: 12px; }.subject-list { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.public-label { flex-shrink: 0; color: var(--huiyue-primary); }
.pager { display: flex; justify-content: center; margin-top: 32px; }
@media (max-width: 900px) { .filter-row { flex-wrap: wrap; }.search-input { margin-left: 0; }.work-waterfall { columns: 3 180px; } }
@media (max-width: 560px) { .work-waterfall { columns: 2 150px; }.category-bar { justify-content: flex-start; }.category-tab { padding: 6px 13px; } }
</style>
