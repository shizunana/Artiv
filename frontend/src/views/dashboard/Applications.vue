<template>
  <div class="applications">
    <div class="wb-card">
      <div class="wb-card__head">
        <h3>应征管理</h3>
        <router-link :to="{ name: 'DashboardProjects' }">← 返回我的企划</router-link>
      </div>
      <el-table :data="applications" v-loading="loading" empty-text="暂无应征记录">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="intro" label="应征介绍" min-width="220" show-overflow-tooltip />
        <el-table-column prop="workLink" label="作品链接" width="120" />
        <el-table-column label="报价" width="110">
          <template #default="{ row }">¥{{ row.quote }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(APP_STATUS, row.status)" size="small">
              {{ statusText(APP_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" link type="success" size="small" @click="select(row)">选定</el-button>
            <el-button v-if="row.status === 0" link type="danger" size="small" @click="reject(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getApplications } from '@/api/project'
import { statusText, statusType } from '@/utils/status'

const APP_STATUS = {
  0: { text: '待处理', type: 'info' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已拒绝', type: 'danger' },
  3: { text: '已撤销', type: 'info' },
}

const route = useRoute()
const applications = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await getApplications(route.params.id, { page: 1, size: 30 })
    applications.value = data.records || []
  } finally {
    loading.value = false
  }
}

function select(row) {
  // TODO(阶段3)：创建订单
  ElMessage.info(`选定画师 #${row.painterId}（阶段3接入下单）`)
}

function reject(row) {
  // TODO(阶段3)：更新应征状态
  ElMessage.info(`拒绝画师 #${row.painterId}（阶段3接入）`)
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

.wb-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.wb-card__head h3 {
  margin: 0;
  font-size: 17px;
}
</style>
