<template>
  <div class="dash-home">
    <!-- 欢迎卡（参考站欢迎区） -->
    <section class="wb-card welcome-card">
      <div class="welcome-main">
        <h2>欢迎您，{{ userStore.user?.nickname || userStore.user?.username }}</h2>
        <p>在开始第一次合作前，建议先阅读企划方工作指南，让合作更加顺利。</p>
        <div class="welcome-actions">
          <el-button type="primary" round @click="router.push({ name: 'ProjectNew' })">立刻发布需求</el-button>
          <el-button round @click="router.push({ name: 'Projects' })">逛逛约稿大厅</el-button>
        </div>
      </div>
      <div class="welcome-stats">
        <div class="stat-box">
          <strong>{{ stats.total }}</strong>
          <span>我的企划</span>
        </div>
        <div class="stat-box">
          <strong>{{ stats.recruiting }}</strong>
          <span>招募中</span>
        </div>
        <div class="stat-box">
          <strong>{{ stats.orders }}</strong>
          <span>进行中订单</span>
        </div>
      </div>
    </section>

    <!-- 最近企划 -->
    <section class="wb-card">
      <div class="wb-card__head">
        <h3>我的企划</h3>
        <router-link :to="{ name: 'DashboardProjects' }">查看全部 →</router-link>
      </div>
      <el-table :data="projects" v-loading="loading" empty-text="还没有发布企划，点击右上角「发布需求」开始">
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column label="预算" width="110">
          <template #default="{ row }">¥{{ row.budget }}</template>
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
      </el-table>
    </section>

    <!-- 指南卡（参考站如何开始约稿） -->
    <section class="wb-card guide-card">
      <div class="guide-icon">📖</div>
      <div class="guide-text">
        <h3>如何开始约稿</h3>
        <p>发布需求 → 画师应征 / 定向邀请 → 选定下单 → 分阶段验收 → 互相评价。</p>
      </div>
      <el-button round @click="router.push({ name: 'ProjectNew' })">立即体验</el-button>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyProjects } from '@/api/project'
import { getOrders } from '@/api/order'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/date'
import { PROJECT_STATUS, statusText, statusType } from '@/utils/status'

const router = useRouter()
const userStore = useUserStore()
const projects = ref([])
const loading = ref(false)
const stats = reactive({ total: 0, recruiting: 0, orders: 0 })

onMounted(async () => {
  loading.value = true
  try {
    const [projectPage, orderPage] = await Promise.all([
      getMyProjects({ page: 1, size: 5 }),
      getOrders({ page: 1, size: 100 }).catch(() => ({ records: [] })),
    ])
    projects.value = projectPage.records || []
    stats.total = projectPage.total || 0
    stats.recruiting = (projectPage.records || []).filter((p) => p.status === 0).length
    stats.orders = (orderPage.records || []).filter((o) => o.status === 1 || o.status === 2).length
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dash-home {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.wb-card {
  background: #fff;
  border: 1px solid #e4ebf0;
  border-radius: 12px;
  box-shadow: 0 7px 24px rgb(42 67 84 / 5%);
  padding: 24px 26px;
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
  color: #24313b;
}

.wb-card__head a {
  font-size: 13px;
}

.welcome-card {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}

.welcome-main h2 {
  margin: 0 0 10px;
  font-size: 25px;
  color: #1b2934;
}

.welcome-main p {
  color: #7a8791;
  font-size: 14px;
  margin: 0 0 18px;
}

.welcome-actions {
  display: flex;
  gap: 12px;
}

.welcome-stats {
  display: flex;
  gap: 32px;
  align-items: center;
}

.stat-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-box strong {
  font-size: 26px;
  color: var(--artiv-primary);
}

.stat-box span {
  font-size: 13px;
  color: var(--artiv-text-secondary);
}

.guide-card {
  display: flex;
  align-items: center;
  gap: 18px;
}

.guide-icon {
  font-size: 34px;
}

.guide-text {
  flex: 1;
}

.guide-text h3 {
  margin: 0 0 6px;
  font-size: 16px;
}

.guide-text p {
  margin: 0;
  color: var(--artiv-text-secondary);
  font-size: 13px;
}
</style>
