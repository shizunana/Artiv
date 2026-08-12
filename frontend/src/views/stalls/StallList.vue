<template>
  <div class="list-page page-container">
    <!-- 分类区（参考站 stalls-index） -->
    <div class="category-bar">
      <span
        v-for="c in categories"
        :key="c"
        class="category-tab"
        :class="{ 'is-active': activeCategory === c }"
        @click="activeCategory = c"
      >
        {{ c }}
      </span>
    </div>

    <!-- 橱窗卡片（参考站 stall-cell） -->
    <div class="stall-grid" v-loading="loading">
      <div v-for="stall in filteredStalls" :key="stall.id" class="stall-cell" @click="goDetail(stall.id)">
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

    <EmptyState v-if="!loading && filteredStalls.length === 0" description="暂无上架橱窗" />
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

const categories = computed(() => {
  const list = ['全部']
  stalls.value.forEach((s) => {
    if (s.category && !list.includes(s.category)) list.push(s.category)
  })
  return list
})

const filteredStalls = computed(() => {
  if (activeCategory.value === '全部') return stalls.value
  return stalls.value.filter((s) => s.category === activeCategory.value)
})

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
  padding: 16px 16px 48px;
  max-width: 1100px;
}

/* ---------- 分类区 ---------- */
.category-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  padding: 16px 0;
  margin-bottom: 8px;
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

/* ---------- 橱窗卡片 ---------- */
.stall-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  padding-top: 20px;
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

@media (max-width: 900px) {
  .stall-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
