<template>
  <div class="my-projects">
    <div class="wb-card">
      <div class="wb-card__head">
        <h3>我的企划</h3>
        <el-button type="primary" round size="small" @click="router.push({ name: 'ProjectNew' })">
          + 发布企划
        </el-button>
      </div>

      <el-table :data="projects" v-loading="loading" empty-text="还没有发布企划">
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column label="预算" width="100">
          <template #default="{ row }">¥{{ row.budget }}</template>
        </el-table-column>
        <el-table-column label="截稿日期" width="120">
          <template #default="{ row }">{{ row.deadline || '-' }}</template>
        </el-table-column>
        <el-table-column label="合作方式" width="100">
          <template #default="{ row }">{{ row.mode === 1 ? '定向邀请' : '公开招募' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(PROJECT_STATUS, row.status)" size="small">
              {{ statusText(PROJECT_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="router.push({ name: 'ProjectDetail', params: { id: row.id } })">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <Pagination v-model:page="page" v-model:size="size" :total="total" @change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyProjects } from '@/api/project'
import { formatDateTime } from '@/utils/date'
import { PROJECT_STATUS, statusText, statusType } from '@/utils/status'
import Pagination from '@/components/common/Pagination.vue'

const router = useRouter()
const projects = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const data = await getMyProjects({ page: page.value, size: size.value })
    projects.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
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

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
