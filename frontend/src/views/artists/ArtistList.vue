<template>
  <div class="list-page">
    <div class="list-header page-container">
      <h1 class="section-title">画师列表</h1>
      <p class="section-subtitle">发现心仪的合作画师</p>
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

      <div class="artist-grid">
        <div v-for="artist in artists" :key="artist.id" class="artist-card" @click="goProfile(artist.id)">
          <el-avatar :size="72" class="artist-avatar">{{ artist.nickname?.slice(0, 1) || '绘' }}</el-avatar>
          <h3>{{ artist.nickname }}</h3>
          <p class="artist-tags">{{ artist.tags || '未填写风格标签' }}</p>
          <el-tag size="small" :type="artist.certStatus === 2 ? 'success' : 'info'">
            {{ artist.certStatus === 2 ? '已认证' : '未认证' }}
          </el-tag>
        </div>
      </div>

      <EmptyState v-if="artists.length === 0" description="暂无画师数据（演示数据为单画师）" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import EmptyState from '@/components/common/EmptyState.vue'

const router = useRouter()
const activeCategory = ref('全部')
const categories = ['全部', '国潮', '插画', '立绘', '场景']

// 演示数据：真实画师列表接口在阶段 3 接入，先用演示画师
const artists = ref([
  { id: 2, nickname: '画师阿绘', tags: '国潮,插画,人物', certStatus: 2 },
])

function goProfile(id) {
  router.push({ name: 'ArtistProfile', params: { id } })
}
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
  color: #fff;
}

.artist-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.artist-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid var(--huiyue-border);
  padding: 28px 20px;
  text-align: center;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.artist-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}

.artist-avatar {
  background: var(--huiyue-primary);
  color: #fff;
  font-size: 26px;
  margin-bottom: 14px;
}

.artist-card h3 {
  margin: 0 0 8px;
  font-size: 16px;
}

.artist-tags {
  color: var(--huiyue-text-secondary);
  font-size: 13px;
  margin: 0 0 12px;
}

@media (max-width: 900px) {
  .artist-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
