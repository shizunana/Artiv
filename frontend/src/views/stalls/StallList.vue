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

    <!-- 橱窗卡片（参考站 stall-cell） -->
    <div class="stall-grid" v-loading="loading">
      <div v-for="stall in stalls" :key="stall.id" class="stall-cell" @click="goDetail(stall.id)">
        <div class="stall-cell__cover">
          <el-image
            v-if="stall.cover"
            :src="stall.cover"
            :preview-src-list="[stall.cover]"
            preview-teleported
            fit="cover"
            class="cover-image"
          />
          <div v-else class="cover-placeholder">
            <span>🛍️</span>
          </div>
          <!-- 标题下沉到封面底部渐变黑（默认单行省略，hover 显示完整） -->
          <div class="stall-cell__title" :title="stall.title">{{ stall.title }}</div>
        </div>
        <div class="stall-cell__infos">
          <span class="painter-name" :title="stall.painterName">👤 {{ stall.painterName || '画师 #' + stall.painterId }}</span>
          <span class="sales">已售 0</span>
          <span class="stall-cell__price">￥{{ stall.price }}</span>
        </div>
      </div>
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

const categories = ['全部', '头像', 'Q版全身', '半身像', '立绘', '组合页', '服设', '壁纸', '表情包', 'Live2d', '平面设计']
const subjectOptions = ['正比', 'Q版', '日系', '古风', '国潮', '像素', '小动物', '风景', '半厚涂']

function goDetail(id) {
  router.push({ name: 'StallDetail', params: { id } })
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
    stalls.value = data.records || []
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
  border-bottom: 1px solid var(--huiyue-border);
}

.category-tab {
  padding: 7px 18px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid var(--huiyue-border);
  color: var(--huiyue-text-secondary);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.category-tab:hover {
  color: var(--huiyue-primary);
  border-color: var(--huiyue-primary);
}

.category-tab.is-active {
  background: var(--huiyue-primary);
  border-color: var(--huiyue-primary);
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
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
  padding-top: 10px;
}

.stall-cell {
  cursor: pointer;
  transition: transform 0.2s;
}

.stall-cell:hover {
  transform: translateY(-3px);
}

.stall-cell__cover {
  position: relative;
  aspect-ratio: 1 / 1;
  border-radius: 10px;
  overflow: hidden;
  background: var(--huiyue-section-alt);
}

.cover-image {
  width: 100%;
  height: 100%;
  display: block;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44px;
  color: var(--huiyue-text-secondary);
}

.stall-cell__title {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 22px 10px 8px;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.72));
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: background 0.2s;
}

.stall-cell:hover .stall-cell__title {
  background: rgba(0, 0, 0, 0.82);
}

.stall-cell__infos {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 2px 0;
  gap: 8px;
  font-size: 13px;
}

.painter-name {
  color: var(--huiyue-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.sales {
  color: var(--huiyue-text-secondary);
  font-size: 12px;
  flex-shrink: 0;
}

.stall-cell__price {
  color: var(--huiyue-accent);
  font-weight: 700;
  font-size: 15px;
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
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 900px) {
  .stall-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .filter-row {
    flex-wrap: wrap;
  }
}
</style>
