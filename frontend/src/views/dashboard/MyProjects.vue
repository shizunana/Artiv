<template>
  <div class="my-projects">
    <div class="wb-card">
      <div class="wb-card__head">
        <h3>我的企划</h3>
        <el-button type="primary" round size="small" @click="router.push({ name: 'ProjectNew' })">
          + 发布企划
        </el-button>
      </div>

      <el-table class="project-table" :data="projects" v-loading="loading" empty-text="还没有发布企划" table-layout="fixed">
        <el-table-column label="封面" width="64">
          <template #default="{ row }">
            <el-image v-if="firstImage(row.images)" class="project-cover" :src="firstImage(row.images)" fit="cover" />
            <div v-else class="project-cover project-cover--empty">企</div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="企划名" min-width="105" show-overflow-tooltip />
        <el-table-column prop="category" label="类型" width="72" show-overflow-tooltip />
        <el-table-column label="预算" width="80">
          <template #default="{ row }">¥{{ row.budget }}</template>
        </el-table-column>
        <el-table-column prop="description" label="企划详情" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="76">
          <template #default="{ row }">
            <el-tag :type="statusType(PROJECT_STATUS, row.status)" size="small">
              {{ statusText(PROJECT_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="106">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="router.push({ name: 'ProjectDetail', params: { id: row.id } })">
              详情
            </el-button>
            <el-button link type="primary" size="small" @click="openManage(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <Pagination v-model:page="page" v-model:size="size" :total="total" @change="load" />
      </div>
    </div>
    <ProjectManageDialog
      v-model="manageVisible"
      :project="managingProject"
      @changed="load"
      @deleted="load"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyProjects } from '@/api/project'
import { PROJECT_STATUS, statusText, statusType } from '@/utils/status'
import Pagination from '@/components/common/Pagination.vue'
import ProjectManageDialog from '@/components/projects/ProjectManageDialog.vue'

const router = useRouter()
const projects = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const manageVisible = ref(false)
const managingProject = ref(null)

function firstImage(images) {
  if (!images) return ''
  try {
    const parsed = JSON.parse(images)
    if (Array.isArray(parsed)) return parsed[0] || ''
  } catch { /* 兼容历史逗号分隔数据 */ }
  return String(images).split(',').map((item) => item.trim()).find(Boolean) || ''
}

function openManage(project) {
  managingProject.value = project
  manageVisible.value = true
}

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

.project-cover { width: 46px; height: 46px; border-radius: 4px; background: #f1f4f7; }
.project-cover--empty { display: grid; place-items: center; color: #9da6af; font-size: 12px; }
.project-table { width: 100%; }
.project-table :deep(.el-table__cell) { padding-left: 6px; padding-right: 6px; }
</style>
