<template>
  <div class="my-applications">
    <div class="wb-card">
      <h3 class="card-title">我的应征</h3>
      <el-table class="application-table" :data="applications" v-loading="loading" empty-text="还没有应征记录，去约稿大厅看看吧">
        <el-table-column label="企划图片" width="96">
          <template #default="{ row }">
            <el-image v-if="firstImage(row.projectImages)" class="project-cover" :src="firstImage(row.projectImages)" fit="cover" />
            <span v-else class="project-cover project-cover--empty" aria-hidden="true"></span>
          </template>
        </el-table-column>
        <el-table-column prop="projectTitle" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="报价" width="120">
          <template #default="{ row }"><strong class="quote">¥{{ formatAmount(row.quote) }}</strong></template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(APP_STATUS, row.status)" size="small">
              {{ statusText(APP_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="详细页面" width="100" align="center" header-align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openProject(row.projectId)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyApplications } from '@/api/project'
import { formatDateTime } from '@/utils/date'
import { statusText, statusType } from '@/utils/status'

const APP_STATUS = {
  0: { text: '待处理', type: 'info' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已拒绝', type: 'danger' },
  3: { text: '已撤销', type: 'info' },
}

const applications = ref([])
const router = useRouter()
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await getMyApplications({ page: 1, size: 30 })
    applications.value = data.records || []
  } finally {
    loading.value = false
  }
}

function firstImage(images) {
  if (!images) return ''
  try {
    const parsed = JSON.parse(images)
    if (Array.isArray(parsed)) return parsed[0] || ''
  } catch { /* 兼容历史逗号分隔数据 */ }
  return String(images).split(',').map((item) => item.trim()).find(Boolean) || ''
}

function formatAmount(value) {
  const amount = Number(value || 0)
  return Number.isInteger(amount) ? amount.toLocaleString('zh-CN') : amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function openProject(projectId) {
  router.push({ name: 'ProjectDetail', params: { id: projectId } })
}

onMounted(load)
</script>

<style scoped>
.wb-card {
  background: #fff;
  border: 1px solid #e4ebf0;
  border-radius: 12px;
  box-shadow: 0 7px 24px rgb(42 67 84 / 5%);
  padding: 24px 26px;
}

.card-title {
  margin: 0 0 16px;
  font-size: 17px;
}

.project-cover {
  display: block;
  width: 58px;
  height: 58px;
  border-radius: 6px;
  background: #f3f5f7;
}

.project-cover--empty {
  background: transparent;
}

.quote {
  color: #f39a19;
  font-weight: 600;
}

.application-table :deep(.el-table__cell) {
  padding-top: 10px;
  padding-bottom: 10px;
}

.application-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}
</style>
