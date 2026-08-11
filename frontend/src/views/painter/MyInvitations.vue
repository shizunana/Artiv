<template>
  <div class="my-invitations">
    <div class="wb-card">
      <h3 class="card-title">收到的邀请</h3>
      <el-table :data="invitations" v-loading="loading" empty-text="暂未收到定向邀请">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="message" label="邀请附言" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(INVITE_STATUS, row.status)" size="small">
              {{ statusText(INVITE_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button link type="success" size="small" @click="respond(row.id, 1)">接受</el-button>
              <el-button link type="danger" size="small" @click="respond(row.id, 2)">拒绝</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getInvitations, respondInvitation } from '@/api/invitation'
import { formatDateTime } from '@/utils/date'

const INVITE_STATUS = {
  0: { text: '待处理', type: 'warning' },
  1: { text: '已接受', type: 'success' },
  2: { text: '已拒绝', type: 'info' },
}

const invitations = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await getInvitations({ page: 1, size: 30 })
    invitations.value = data.records || []
  } finally {
    loading.value = false
  }
}

async function respond(id, status) {
  await respondInvitation(id, status)
  ElMessage.success(status === 1 ? '已接受邀请' : '已拒绝邀请')
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
