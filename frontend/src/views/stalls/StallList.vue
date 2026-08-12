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

    <!-- 搜索 + 排序 -->
    <div class="filter-row">
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
      <el-select v-model="sort" class="sort-select" @change="page = 1; load()">
        <el-option label="最新上架" value="newest" />
        <el-option label="价格从低到高" value="priceAsc" />
        <el-option label="价格从高到低" value="priceDesc" />
      </el-select>
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
          <div class="cover-mask">
            <span>{{ stall.category }}</span>
          </div>
        </div>
        <div class="stall-cell__infos">
          <span class="stall-cell__name">{{ stall.title }}</span>
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
const size = ref(12)
const total = ref(0)
const activeCategory = ref('全部')
const keyword = ref('')
const sort = ref('newest')

const categories = ['全部', '头像', 'Q版全身', '半身像', '立绘', '组合页', '服设', '壁纸', '表情包', 'Live2d', '平面设计']

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
  gap: 10px;
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
}

.search-input {
  max-width: 380px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 20px;
}

.sort-select {
  width: 150px;
}

/* ---------- 橱窗卡片 ---------- */
.stall-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
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

.cover-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 6px 10px;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.45));
  color: #fff;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.2s;
}

.stall-cell:hover .cover-mask {
  opacity: 1;
}

.stall-cell__infos {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 2px 0;
  gap: 8px;
}

.stall-cell__name {
  font-size: 14px;
  color: var(--huiyue-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  margin-top: 28px;
}

@media (max-width: 900px) {
  .stall-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
