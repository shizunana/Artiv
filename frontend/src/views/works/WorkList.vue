<template>
  <div class="list-page">
    <div class="list-header page-container">
      <h1 class="section-title">作品广场</h1>
      <p class="section-subtitle">来自认证画师的优选作品</p>
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

      <div class="work-grid" v-loading="loading">
        <div v-for="(work, i) in filteredWorks" :key="work.id" class="work-card">
          <div class="work-thumb" :style="gradient(i)">
            <span class="work-emoji">{{ emojis[i % emojis.length] }}</span>
          </div>
          <div class="work-info">
            <h3>{{ work.title }}</h3>
            <div class="work-meta">
              <span class="work-cat">{{ work.category }}</span>
              <span class="work-desc">{{ work.description }}</span>
            </div>
          </div>
        </div>
      </div>

      <EmptyState v-if="!loading && filteredWorks.length === 0" description="暂无作品" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getWorks } from '@/api/work'
import EmptyState from '@/components/common/EmptyState.vue'

const works = ref([])
const loading = ref(false)
const activeCategory = ref('全部')
const categories = ['全部', '国潮', '插画', '立绘', '场景', '原画', '同人']
const emojis = ['🎨', '🖌️', '✨', '🖼️', '🎭', '🌊']
const gradients = [
  'linear-gradient(135deg, #4facfe, #00f2fe)',
  'linear-gradient(135deg, #f093fb, #f5576c)',
  'linear-gradient(135deg, #43e97b, #38f9d7)',
  'linear-gradient(135deg, #fa709a, #fee140)',
  'linear-gradient(135deg, #30cfd0, #330867)',
  'linear-gradient(135deg, #a8edea, #fed6e3)',
]

const filteredWorks = computed(() => {
  if (activeCategory.value === '全部') return works.value
  return works.value.filter((w) => w.category === activeCategory.value)
})

function gradient(i) {
  return { background: gradients[i % gradients.length] }
}

onMounted(async () => {
  loading.value = true
  try {
    const data = await getWorks({ page: 1, size: 30 })
    works.value = data.records || []
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

.work-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.work-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid var(--huiyue-border);
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;
}

.work-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}

.work-thumb {
  aspect-ratio: 1 / 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.work-emoji {
  font-size: 52px;
}

.work-info {
  padding: 14px;
}

.work-info h3 {
  margin: 0 0 8px;
  font-size: 15px;
}

.work-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.work-cat {
  font-size: 12px;
  color: var(--huiyue-primary);
  background: var(--huiyue-primary-light);
  padding: 2px 8px;
  border-radius: 8px;
  flex-shrink: 0;
}

.work-desc {
  font-size: 12px;
  color: var(--huiyue-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .work-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
