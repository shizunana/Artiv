<template>
  <div class="my-applications">
    <div class="wb-card">
      <header class="application-header">
        <div>
          <h2>我的应征</h2>
          <p>这里只展示当前仍然有效的应征关系。</p>
        </div>
        <el-button @click="load">刷新</el-button>
      </header>

      <div v-loading="loading" class="application-cards">
        <el-empty v-if="!applications.length" description="暂无当前应征，去企划大厅看看吧" />
        <article v-for="item in applications" :key="item.id" class="application-card" @click="openProject(item.projectId)">
          <img v-if="firstImage(item.projectImages)" :src="firstImage(item.projectImages)" :alt="`${item.projectTitle}企划图片`" />
          <div v-else class="application-cover">企划</div>

          <div class="application-main">
            <h3 :title="item.projectTitle">{{ truncate(item.projectTitle) }}</h3>
            <p>企划类型：{{ item.projectCategory || '未分类' }} · 应征时间：{{ formatDateTime(item.createdAt) }}</p>
            <p class="project-summary" :title="item.projectDescription || ''">
              企划详情：{{ truncate(item.projectDescription || '暂无详情') }}
            </p>
          </div>

          <div class="application-quote">
            <small>我的报价</small>
            <strong>¥{{ formatAmount(item.quote) }}</strong>
            <div class="application-actions">
              <el-button type="primary" link @click.stop="openProject(item.projectId)">企划详情</el-button>
              <el-button type="primary" link @click.stop="editApplication(item)">更改应征</el-button>
            </div>
          </div>
        </article>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyApplications } from '@/api/project'
import { formatDateTime } from '@/utils/date'

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

function truncate(value, limit = 10) {
  const characters = Array.from(String(value || ''))
  return characters.length > limit ? `${characters.slice(0, limit).join('')}...` : characters.join('')
}

function formatAmount(value) {
  const amount = Number(value || 0)
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

function openProject(projectId) {
  router.push({ name: 'ProjectDetail', params: { id: projectId } })
}

function editApplication(item) {
  router.push({
    name: 'ProjectDetail',
    params: { id: item.projectId },
    query: { applicationId: item.id, editApplication: '1', from: 'my-applications' },
  })
}

onMounted(load)
</script>

<style scoped>
.wb-card {
  padding: 24px;
  border-radius: 8px;
  background: #fff;
}

.application-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.application-header h2 { margin: 0 0 7px; font-size: 21px; }
.application-header p { margin: 0; color: var(--artiv-text-secondary); }
.application-cards { min-width: 0; min-height: 240px; padding-top: 16px; }
.application-card { display: grid; width: 100%; box-sizing: border-box; grid-template-columns: 92px minmax(0, 1fr) minmax(260px, 300px); align-items: center; gap: 18px; padding: 16px 0; border-bottom: 1px solid var(--artiv-border); cursor: pointer; }
.application-card:hover { background: #fbfdff; }
.application-card img, .application-cover { width: 92px; height: 72px; border-radius: 7px; object-fit: cover; }
.application-cover { display: grid; place-items: center; background: var(--artiv-section-alt); color: var(--artiv-text-secondary); }
.application-main { min-width: 0; }
.application-main h3 { margin: 0; overflow: hidden; font-size: 16px; text-overflow: ellipsis; white-space: nowrap; }
.application-main p { margin: 8px 0 0; overflow: hidden; color: var(--artiv-text-secondary); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.application-main .project-summary { color: var(--artiv-text); }
.application-quote { display: grid; min-width: 0; justify-items: end; gap: 7px; }
.application-quote small { color: var(--artiv-text-secondary); }
.application-quote strong { color: var(--artiv-accent); font-size: 18px; }
.application-actions { display: flex; width: 100%; flex-wrap: wrap; justify-content: flex-end; gap: 4px 8px; }
.application-actions :deep(.el-button + .el-button) { margin-left: 0; }

@media (max-width: 760px) {
  .application-header { flex-direction: column; }
  .application-card { grid-template-columns: 70px minmax(0, 1fr); }
  .application-card img, .application-cover { width: 70px; }
  .application-quote { grid-column: 2; justify-items: start; }
  .application-actions { justify-content: flex-start; }
}
</style>
