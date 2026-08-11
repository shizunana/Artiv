<template>
  <div class="list-page">
    <div class="list-header page-container">
      <h1 class="section-title">服务板 · 橱窗</h1>
      <p class="section-subtitle">画师明码标价的内容服务</p>
    </div>

    <div class="page-container">
      <div class="filter-bar">
        <div class="filter-tabs">
          <span
            v-for="c in categories"
            :key="c"
            class="filter-tab"
            :class="{ 'is-active': activeCategory === c }"
            @click="activeCategory = c"
          >
            {{ c }}
          </span>
        </div>
      </div>

      <div class="stall-grid" v-loading="loading">
        <div v-for="stall in stalls" :key="stall.id" class="stall-card" @click="goDetail(stall.id)">
          <div class="stall-card__thumb" :style="gradient(stall.id)">
            <span class="stall-emoji">🛍️</span>
          </div>
          <div class="stall-card__body">
            <h3>{{ stall.title }}</h3>
            <p>{{ stall.description }}</p>
            <div class="stall-card__meta">
              <span class="price">¥{{ stall.price }}</span>
              <span class="category">{{ stall.category }}</span>
            </div>
            <el-button type="primary" size="small" round class="buy-btn" @click.stop="goDetail(stall.id)">
              立即下单
            </el-button>
          </div>
        </div>
      </div>

      <EmptyState v-if="!loading && stalls.length === 0" description="暂无上架服务" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getStalls } from '@/api/stall'
import EmptyState from '@/components/common/EmptyState.vue'

const router = useRouter()
const stalls = ref([])
const loading = ref(false)
const activeCategory = ref('全部')
const categories = ['全部', '国潮插画', '角色立绘', '场景原画', '头像', 'Q版']

const filteredStalls = computed(() => {
  if (activeCategory.value === '全部') return stalls.value
  return stalls.value.filter((s) => s.category === activeCategory.value)
})

const gradients = [
  'linear-gradient(135deg, #30cfd0, #330867)',
  'linear-gradient(135deg, #f093fb, #f5576c)',
  'linear-gradient(135deg, #43e97b, #38f9d7)',
  'linear-gradient(135deg, #fa709a, #fee140)',
]

function gradient(id) {
  return { background: gradients[id % gradients.length] }
}

function goDetail(id) {
  router.push({ name: 'StallDetail', params: { id } })
}

onMounted(async () => {
  loading.value = true
  try {
    const data = await getStalls({ page: 1, size: 30 })
    stalls.value = data.records || []
  } finally {
    loading.value = false
  }
})
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
  margin-bottom: 24px;
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
}

.filter-tab.is-active {
  background: var(--huiyue-primary);
  border-color: var(--huiyue-primary);
  color: #fff;
}

.stall-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stall-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid var(--huiyue-border);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stall-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}

.stall-card__thumb {
  aspect-ratio: 4 / 3;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stall-emoji {
  font-size: 48px;
}

.stall-card__body {
  padding: 16px;
}

.stall-card__body h3 {
  margin: 0 0 8px;
  font-size: 15px;
}

.stall-card__body p {
  margin: 0 0 12px;
  color: var(--huiyue-text-secondary);
  font-size: 13px;
  min-height: 40px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.stall-card__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.price {
  color: var(--huiyue-accent);
  font-weight: 700;
  font-size: 16px;
}

.category {
  font-size: 12px;
  color: var(--huiyue-primary);
  background: var(--huiyue-primary-light);
  padding: 2px 8px;
  border-radius: 8px;
}

.buy-btn {
  width: 100%;
}

@media (max-width: 900px) {
  .stall-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
