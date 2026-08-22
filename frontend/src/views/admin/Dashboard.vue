<template>
  <div v-loading="loading" class="admin-page">
    <header class="page-head">
      <div><h2>管理概览</h2><p>待办、平台规模与最近高风险操作</p></div>
      <el-button @click="load">刷新</el-button>
    </header>

    <section class="metric-grid">
      <article v-for="item in metrics" :key="item.label" class="metric-card">
        <strong>{{ item.value }}</strong><span>{{ item.label }}</span>
      </article>
    </section>

    <section class="wb-card queue-card">
      <h3>待处理队列</h3>
      <div class="queue-grid">
        <router-link to="/admin/certifications" class="queue-item"><strong>{{ data.pendingCertifications || 0 }}</strong><span>认证待审核</span></router-link>
        <router-link to="/admin/disputes" class="queue-item danger"><strong>{{ data.pendingDisputes || 0 }}</strong><span>争议待仲裁</span></router-link>
        <router-link to="/admin/review-appeals" class="queue-item danger"><strong>{{ data.pendingReviewAppeals || 0 }}</strong><span>评价申诉待处理</span></router-link>
        <router-link to="/admin/projects" class="queue-item"><strong>{{ data.hiddenProjects || 0 }}</strong><span>违规下架企划</span></router-link>
      </div>
    </section>

    <section class="wb-card">
      <div class="section-head"><h3>最近管理操作</h3><router-link to="/admin/audit-logs">查看全部</router-link></div>
      <el-table :data="data.recentAuditLogs || []" empty-text="暂无审计记录">
        <el-table-column prop="createdAt" label="时间" min-width="165"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
        <el-table-column prop="actionType" label="动作" min-width="170" />
        <el-table-column prop="targetType" label="对象" width="130" />
        <el-table-column prop="targetId" label="对象ID" width="100" />
        <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getAdminOverview } from '@/api/admin'
import { formatDateTime } from '@/utils/date'

const loading = ref(false)
const data = ref({})
const metrics = computed(() => [
  { label: '总用户数', value: data.value.totalUsers || 0 },
  { label: '今日新增用户', value: data.value.newUsersToday || 0 },
  { label: '今日新增订单', value: data.value.newOrdersToday || 0 },
  { label: '进行中业务订单', value: data.value.activeOrders || 0 },
])

async function load() {
  loading.value = true
  try { data.value = await getAdminOverview() } finally { loading.value = false }
}
onMounted(load)
</script>

<style scoped>
.admin-page{display:grid;gap:16px}.page-head,.section-head{display:flex;align-items:center;justify-content:space-between}.page-head{padding:22px 24px;background:#fff;border-radius:6px}.page-head h2,.section-head h3,.wb-card h3{margin:0}.page-head p{margin:7px 0 0;color:#8a929c}.metric-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:14px}.metric-card,.wb-card{background:#fff;border-radius:6px;padding:22px}.metric-card{display:flex;flex-direction:column;gap:7px;border-top:3px solid var(--artiv-primary)}.metric-card strong{font-size:28px}.metric-card span,.queue-item span{color:#7d8792}.queue-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-top:16px}.queue-item{display:flex;flex-direction:column;gap:5px;padding:18px;background:#f4f9fc;border-radius:6px;color:inherit}.queue-item strong{font-size:24px;color:var(--artiv-primary)}.queue-item.danger strong{color:#e65b5b}.section-head{margin-bottom:14px}.section-head a{color:var(--artiv-primary)}@media(max-width:900px){.metric-grid{grid-template-columns:repeat(2,1fr)}.queue-grid{grid-template-columns:1fr}}
</style>
