<template>
  <div class="order-list">
    <div class="wb-card">
      <h3 class="card-title">订单管理</h3>
      <el-table :data="orders" v-loading="loading" empty-text="暂无订单">
        <el-table-column prop="id" label="订单号" width="90" />
        <el-table-column label="金额" width="110">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column label="当前阶段" width="100">
          <template #default="{ row }">{{ stageNames[row.currentStage] || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(ORDER_STATUS, row.status)" size="small">
              {{ statusText(ORDER_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getOrders } from '@/api/order'
import { formatDateTime } from '@/utils/date'
import { ORDER_STATUS, statusText, statusType } from '@/utils/status'

const router = useRouter()
const orders = ref([])
const loading = ref(false)
const stageNames = ['初稿', '修改', '终稿', '交付']

async function load() {
  loading.value = true
  try {
    const data = await getOrders({ page: 1, size: 30 })
    orders.value = data.records || []
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  // TODO(阶段3)：订单详情页
  console.log('order detail', id)
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
