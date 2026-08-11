<template>
  <div class="page-container">
    <h2>企划大厅</h2>
    <EmptyState v-if="!loading && projects.length === 0" description="暂无企划" />
    <el-row v-else :gutter="16">
      <el-col v-for="project in projects" :key="project.id" :span="8">
        <el-card class="project-card">
          <h3>{{ project.title }}</h3>
          <p>{{ project.description }}</p>
          <div class="meta">预算：¥{{ project.budget }} · 截止：{{ formatDateTime(project.deadline) }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getProjects } from '@/api/project'
import { formatDateTime } from '@/utils/date'
import EmptyState from '@/components/common/EmptyState.vue'

const projects = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const data = await getProjects({ page: 1, size: 9 })
    projects.value = data.records || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.project-card h3 {
  margin: 0 0 8px;
}

.project-card p {
  color: var(--huiyue-text-secondary);
  min-height: 42px;
}

.meta {
  font-size: 12px;
  color: var(--huiyue-text-secondary);
}
</style>
