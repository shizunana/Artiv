<template>
  <div class="my-applications">
    <div class="wb-card">
      <h3 class="card-title">我的应征</h3>
      <el-table :data="applications" v-loading="loading" empty-text="还没有应征记录，去约稿大厅看看吧">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="intro" label="应征介绍" min-width="200" show-overflow-tooltip />
        <el-table-column label="报价" width="110">
          <template #default="{ row }">¥{{ row.quote }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(APP_STATUS, row.status)" size="small">
              {{ statusText(APP_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" link type="danger" size="small" @click="withdraw(row.id)">
              撤销
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyApplications, deleteApplication } from '@/api/project'
import { formatDateTime } from '@/utils/date'

const APP_STATUS = {
  0: { text: '待处理', type: 'info' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已拒绝', type: 'danger' },
  3: { text: '已撤销', type: 'info' },
}

const applications = ref([])
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

async function withdraw(id) {
  await ElMessageBox.confirm('确定撤销该应征吗？', '提示', { type: 'warning' })
  await deleteApplication(id)
  ElMessage.success('已撤销')
  load()
}

onMounted(load)
</script>

<style scoped>
.wb-card {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 24px;
}

.card-title {
  margin: 0 0 16px;
  font-size: 17px;
}
</style>
