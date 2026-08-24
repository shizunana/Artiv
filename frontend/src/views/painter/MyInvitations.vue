<template>
  <div class="my-invitations">
    <div class="wb-card">
      <h3 class="card-title">收到的邀请</h3>
      <el-table class="invitation-table" :data="invitations" v-loading="loading" empty-text="暂未收到定向邀请">
        <el-table-column label="封面" width="96">
          <template #default="{ row }">
            <el-image v-if="row.cover" class="project-cover" :src="row.cover" fit="cover" />
            <span v-else class="project-cover project-cover--empty" aria-hidden="true"></span>
          </template>
        </el-table-column>
        <el-table-column prop="projectTitle" label="企划名" min-width="220" show-overflow-tooltip />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ row.category || '未分类' }}</template>
        </el-table-column>
        <el-table-column label="预算" width="130">
          <template #default="{ row }">
            <strong class="budget">{{ formatProjectBudget(row) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag :type="invitationStatus(row.status).type" size="small">
              {{ invitationStatus(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="企划详情" width="110" align="center" header-align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openProject(row.projectId)">查看详细</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getInvitations } from '@/api/invitation'
import { formatProjectBudget } from '@/utils/budget'

const invitations = ref([])
const router = useRouter()
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

function invitationStatus(value) {
  return {
    0: { text: '待处理', type: 'warning' },
    1: { text: '已接受', type: 'success' },
    2: { text: '已拒绝', type: 'info' },
  }[Number(value)] || { text: '未知', type: 'info' }
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

.budget {
  color: #f39a19;
  font-weight: 600;
}

.invitation-table :deep(.el-table__cell) {
  padding-top: 10px;
  padding-bottom: 10px;
}

.invitation-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}
</style>
