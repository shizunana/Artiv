<template>
  <div class="list-page page-container">
    <!-- 分类 Tab（参考站 stalls/list） -->
    <div class="category-bar">
      <span
        v-for="c in categories"
        :key="c"
        class="category-tab"
        :class="{ 'is-active': activeCategory === c }"
        @click="activeCategory = c; page = 1; load()"
      >
        {{ c }}
      </span>
    </div>

    <!-- 搜索 + 排序 + 题材风格 + 价格区间 + 截稿时间（参考站筛选行） -->
    <div class="filter-row">
      <el-select v-model="sort" class="sort-select" @change="page = 1; load()">
        <el-option label="最新上架" value="newest" />
        <el-option label="价格从低到高" value="priceAsc" />
        <el-option label="价格从高到低" value="priceDesc" />
      </el-select>
      <el-select v-model="subject" class="filter-select" @change="page = 1; load()">
        <el-option label="题材风格" value="all" />
        <el-option v-for="s in subjectOptions" :key="s" :label="s" :value="s" />
      </el-select>
      <el-select v-model="priceRange" class="filter-select" @change="page = 1; load()">
        <el-option label="价格区间不限" value="all" />
        <el-option label="100 以下" value="0-100" />
        <el-option label="100-300" value="100-300" />
        <el-option label="300-500" value="300-500" />
        <el-option label="500-1000" value="500-1000" />
        <el-option label="1000 以上" value="1000-" />
      </el-select>
      <el-select v-model="deliverTime" class="filter-select" @change="page = 1; load()">
        <el-option label="截稿时间不限" value="all" />
        <el-option label="24 小时内" value="24" />
        <el-option label="48 小时内" value="48" />
        <el-option label="一周内" value="一周" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="搜索橱窗标题"
        clearable
        class="search-input"
        @keyup.enter="page = 1; load()"
        @clear="page = 1; load()"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>

    <!-- 橱窗卡片 -->
    <div class="stall-grid" v-loading="loading">
      <article
        v-for="stall in stalls"
        :key="stall.id"
        class="stall-cell"
        tabindex="0"
        :aria-label="`查看橱窗：${stall.title}`"
        @click="goDetail(stall.id)"
        @keyup.enter="goDetail(stall.id)"
      >
        <div class="stall-cell__cover">
          <img
            v-if="stall.cover"
            :src="stall.cover"
            :alt="stall.title"
            loading="lazy"
            class="cover-image"
          />
          <div v-else class="cover-placeholder">
            <span>🛍️</span>
          </div>
          <div class="stall-cell__overlay">
            <div class="stall-cell__heading">
              <strong :title="stall.title">{{ displayTitle(stall.title) }}</strong>
              <small>{{ stall.category || '未分类' }}</small>
            </div>
            <span class="sales">{{ soldText(stall) }}</span>
          </div>
        </div>
        <div class="stall-cell__infos">
          <div class="painter-summary">
            <el-avatar :size="26" :src="stall.painterAvatar || undefined" class="painter-avatar">
              {{ painterInitial(stall) }}
            </el-avatar>
            <span class="painter-name" :title="stall.painterName">{{ stall.painterName || '画师 #' + stall.painterId }}</span>
          </div>
          <span class="stall-cell__price">￥{{ stall.price }}</span>
        </div>
      </article>
    </div>

    <EmptyState v-if="!loading && stalls.length === 0" description="暂无符合条件的橱窗" />
    <div class="pager">
      <Pagination v-model:page="page" v-model:size="size" :total="total" @change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getStalls } from '@/api/stall'
import { getPublicProfile } from '@/api/user'
import { SUBJECT_OPTIONS, CATEGORY_OPTIONS } from '@/constants/subjects'
import EmptyState from '@/components/common/EmptyState.vue'
import Pagination from '@/components/common/Pagination.vue'

const router = useRouter()
const stalls = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const activeCategory = ref('全部')
const keyword = ref('')
const sort = ref('newest')
const subject = ref('all')
const priceRange = ref('all')
const deliverTime = ref('all')

const categories = ['全部', ...CATEGORY_OPTIONS]
const subjectOptions = SUBJECT_OPTIONS

function goDetail(id) {
  router.push({ name: 'StallDetail', params: { id } })
}

function displayTitle(title) {
  const characters = Array.from(title || '')
  return characters.length > 10 ? `${characters.slice(0, 10).join('')}...` : title
}

function painterInitial(stall) {
  return (stall.painterName || `画师${stall.painterId || ''}`).slice(0, 1)
}

function soldText(stall) {
  return Number.isFinite(Number(stall.soldCount)) && stall.soldCount !== null
    ? `已售 ${stall.soldCount}`
    : '已售 —'
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (activeCategory.value !== '全部') params.category = activeCategory.value
    if (keyword.value) params.keyword = keyword.value
    if (sort.value !== 'newest') params.sort = sort.value
    if (subject.value !== 'all') params.subject = subject.value
    if (priceRange.value !== 'all') {
      const [min, max] = priceRange.value.split('-')
      if (min) params.priceMin = Number(min)
      if (max) params.priceMax = Number(max)
    }
    if (deliverTime.value !== 'all') params.deliverTime = deliverTime.value
    const data = await getStalls(params)
    const records = data.records || []
    const painterIds = [...new Set(records.map((stall) => stall.painterId).filter(Boolean))]
    const profileEntries = await Promise.all(painterIds.map(async (painterId) => {
      try {
        return [painterId, await getPublicProfile(painterId, 'painter')]
      } catch {
        return [painterId, null]
      }
    }))
    const profiles = new Map(profileEntries)
    stalls.value = records.map((stall) => ({
      ...stall,
      painterAvatar: profiles.get(stall.painterId)?.avatar || ''
    }))
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.list-page {
  min-height: 60vh;
  padding: 16px 16px 48px;
  max-width: 1100px;
}

/* ---------- 分类区 ---------- */
.category-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  padding: 14px 0;
  border-bottom: 1px solid var(--artiv-border);
}

.category-tab {
  padding: 7px 18px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid var(--artiv-border);
  color: var(--artiv-text-secondary);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
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

/* ---------- 搜索 + 排序 ---------- */
.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  flex-wrap: nowrap;
  justify-content: flex-start;
}

.search-input {
  width: 240px;
  flex-shrink: 0;
  margin-left: auto;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 20px;
}

.sort-select {
  width: 150px;
  flex-shrink: 0;
}

.filter-select {
  width: 140px;
  flex-shrink: 0;
}

/* ---------- 橱窗卡片 ---------- */
.stall-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 20px 16px;
  padding-top: 10px;
}

.stall-cell {
  min-width: 0;
  outline: none;
  cursor: pointer;
}

.stall-cell:focus-visible .stall-cell__cover {
  outline: 3px solid color-mix(in srgb, var(--artiv-primary) 35%, transparent);
  outline-offset: 3px;
}

.stall-cell__cover {
  position: relative;
  aspect-ratio: 1 / 1.04;
  border-radius: 11px;
  overflow: hidden;
  background: var(--artiv-section-alt);
}

.cover-image {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transition: transform 0.32s ease;
}

.stall-cell:hover .cover-image,
.stall-cell:focus-visible .cover-image {
  transform: scale(1.04);
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44px;
  color: var(--artiv-text-secondary);
}

.stall-cell__overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  padding: 58px 12px 11px;
  background: linear-gradient(transparent, rgba(13, 22, 27, 0.8));
  color: #fff;
  pointer-events: none;
}

.stall-cell__heading {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
}

.stall-cell__heading strong {
  max-width: 100%;
  overflow: hidden;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stall-cell__heading small {
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.76);
  font-size: 11px;
}

.sales {
  flex-shrink: 0;
  color: rgba(255, 255, 255, 0.84);
  font-size: 11px;
  white-space: nowrap;
}

.stall-cell__infos {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 2px 0;
  gap: 8px;
  font-size: 13px;
}

.painter-summary {
  display: flex;
  min-width: 0;
  flex: 1;
  align-items: center;
  gap: 8px;
}

.painter-avatar {
  flex: 0 0 auto;
  color: #fff;
  background: linear-gradient(145deg, #78cfed, #159bc9);
}

.painter-name {
  color: var(--artiv-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.stall-cell__price {
  color: var(--artiv-accent);
  font-weight: 700;
  font-size: 17px;
  flex-shrink: 0;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding-bottom: 8px;
}

@media (max-width: 1100px) {
  .stall-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .stall-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .filter-row {
    flex-wrap: wrap;
  }
}

@media (max-width: 680px) {
  .stall-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px 12px;
  }
}

@media (max-width: 420px) {
  .stall-grid {
    grid-template-columns: 1fr;
  }
}
</style>
