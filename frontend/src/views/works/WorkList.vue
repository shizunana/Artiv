<template>
  <div class="page-container">
    <h2>作品广场</h2>
    <el-row :gutter="16">
      <el-col v-for="work in works" :key="work.id" :span="6">
        <el-card class="work-card">
          <h3>{{ work.title }}</h3>
          <p>{{ work.description }}</p>
          <div class="meta">{{ work.category }}</div>
        </el-card>
      </el-col>
    </el-row>
    <EmptyState v-if="!loading && works.length === 0" description="暂无作品" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getWorks } from '@/api/work'
import EmptyState from '@/components/common/EmptyState.vue'

const works = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const data = await getWorks({ page: 1, size: 8 })
    works.value = data.records || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.work-card h3 {
  margin: 0 0 8px;
}

.work-card p {
  color: var(--huiyue-text-secondary);
  min-height: 42px;
}

.meta {
  font-size: 12px;
  color: var(--huiyue-text-secondary);
}
</style>
