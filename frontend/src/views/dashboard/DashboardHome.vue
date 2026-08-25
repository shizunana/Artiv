<template>
  <div v-loading="loading" class="dashboard-home">
    <section class="wb-card welcome-card">
      <div>
        <h2>欢迎您，{{ userStore.user?.nickname || userStore.user?.username }}</h2>
        <p>这里汇总您的企划招募和当前合作进度。</p>
      </div>
      <div class="welcome-actions">
        <el-button type="primary" round @click="router.push({ name: 'ProjectNew' })">发布企划</el-button>
        <el-button round @click="router.push({ name: 'Artists' })">浏览画师</el-button>
      </div>
    </section>

    <div class="dashboard-grid">
      <section class="wb-card active-orders-card">
        <header class="section-header compact-header">
          <div>
            <h3>进行中的订单</h3>
            <p>当前共有 {{ activeOrderTotal }} 笔待处理或履约中的订单。</p>
          </div>
          <el-button type="primary" link @click="openOrders">查看全部</el-button>
        </header>

        <el-empty v-if="!activeOrders.length" :image-size="92" description="暂无进行中的订单" />
        <div v-else class="active-order-list">
          <article v-for="order in activeOrders" :key="order.id" class="active-order" @click="openOrder(order.id)">
            <img v-if="orderCover(order)" :src="orderCover(order)" alt="订单封面" />
            <div v-else class="item-cover">订单</div>
            <div class="active-order-main">
              <div class="item-title-row">
                <h4 :title="orderTitle(order)">{{ orderTitle(order) }}</h4>
                <el-tag size="small" :type="statusType(ORDER_STATUS, order.status)">
                  {{ orderStatusText(order.status, 'employer') }}
                </el-tag>
              </div>
              <p>{{ orderSource(order) }} · 订单 #{{ order.id }} · {{ formatDateTime(order.createdAt) }}</p>
              <p class="item-summary">{{ orderProgress(order) }}</p>
            </div>
            <div class="item-side">
              <small>托管金额</small>
              <strong>{{ formatMoney(order.amount) }}</strong>
              <el-button type="primary" link @click.stop="openOrder(order.id)">继续处理</el-button>
            </div>
          </article>
        </div>
      </section>

      <aside class="wb-card quick-card">
        <header class="section-header compact-header">
          <div>
            <h3>快捷操作</h3>
            <p>快速进入常用委托功能。</p>
          </div>
        </header>
        <div class="quick-list">
          <button v-for="item in quickActions" :key="item.label" type="button" @click="router.push(item.to)">
            <span class="quick-copy">
              <strong>{{ item.label }}</strong>
              <small>{{ item.detail }}</small>
            </span>
            <span class="quick-arrow" aria-hidden="true">›</span>
          </button>
        </div>
      </aside>
    </div>

    <section class="wb-card recent-projects-card">
      <header class="section-header compact-header">
        <div>
          <h3>近期企划</h3>
          <p>共 {{ counts.projects }} 个企划，其中 {{ counts.recruiting }} 个正在招募。</p>
        </div>
        <el-button type="primary" link @click="router.push({ name: 'DashboardProjects' })">查看全部</el-button>
      </header>

      <el-empty v-if="!recentProjects.length" :image-size="92" description="还没有发布企划" />
      <div v-else class="recent-project-list">
        <article v-for="project in recentProjects" :key="project.id" class="recent-project" @click="openProject(project.id)">
          <img v-if="firstImage(project.images)" :src="firstImage(project.images)" :alt="`${project.title}封面`" />
          <div v-else class="item-cover">企划</div>
          <div class="recent-project-main">
            <div class="item-title-row">
              <h4 :title="project.title">{{ truncate(project.title, 10) }}</h4>
              <el-tag size="small" :type="projectCardStatus(project).type">{{ projectCardStatus(project).text }}</el-tag>
            </div>
            <p>类型：{{ project.category || '未分类' }} · 发布时间：{{ formatDateTime(project.createdAt) }}</p>
            <p class="item-summary" :title="project.description || ''">企划详情：{{ truncate(project.description || '暂无详情', 15) }}</p>
          </div>
          <div class="item-side">
            <small>预算区间</small>
            <strong>{{ formatProjectBudget(project) }}</strong>
            <el-button type="primary" link @click.stop="openProject(project.id)">企划详情</el-button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyProjects } from '@/api/project'
import { getOrders } from '@/api/order'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/date'
import { formatProjectBudget } from '@/utils/budget'
import { ORDER_STATUS, PROJECT_STATUS, orderStatusText, statusText, statusType } from '@/utils/status'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const counts = reactive({ projects: 0, recruiting: 0 })
const activeOrders = ref([])
const activeOrderTotal = ref(0)
const recentProjects = ref([])

const quickActions = computed(() => [
  { label: '发布企划', detail: '创建公开企划或定向邀请', to: { name: 'ProjectNew' } },
  { label: '我的企划', detail: `${counts.projects} 个企划 · ${counts.recruiting} 个招募中`, to: { name: 'DashboardProjects' } },
  { label: '订单管理', detail: `${activeOrderTotal.value} 笔进行中`, to: { name: 'DashboardOrders', query: { participantRole: 'employer' } } },
  { label: '消息中心', detail: '联系画师并查看系统通知', to: { name: 'Messages' } },
])

async function fetchAllProjects() {
  const params = { page: 1, size: 50 }
  const first = await getMyProjects(params)
  const records = [...(first.records || [])]
  const pages = Math.ceil(Number(first.total || 0) / 50)
  if (pages > 1) {
    const remaining = await Promise.all(
      Array.from({ length: pages - 1 }, (_, index) => getMyProjects({ ...params, page: index + 2 })),
    )
    remaining.forEach((page) => records.push(...(page.records || [])))
  }
  return records
}

async function fetchAllOrders(source) {
  const params = { page: 1, size: 50, source, statusGroup: 'all', participantRole: 'employer' }
  const first = await getOrders(params)
  const records = [...(first.records || [])]
  const pages = Math.ceil(Number(first.total || 0) / 50)
  if (pages > 1) {
    const remaining = await Promise.all(
      Array.from({ length: pages - 1 }, (_, index) => getOrders({ ...params, page: index + 2 })),
    )
    remaining.forEach((page) => records.push(...(page.records || [])))
  }
  return records
}

async function loadDashboard() {
  loading.value = true
  try {
    const [projects, projectOrders, stallOrders] = await Promise.all([
      fetchAllProjects().catch(() => []),
      fetchAllOrders('project').catch(() => []),
      fetchAllOrders('stall').catch(() => []),
    ])

    const sortedProjects = [...projects].sort(compareCreatedAt)
    counts.projects = projects.length
    counts.recruiting = projects.filter((project) => Number(project.status) === 0).length
    recentProjects.value = sortedProjects.slice(0, 4)

    const orders = [...projectOrders, ...stallOrders]
    const active = orders.filter((order) => [0, 1, 2, 5].includes(Number(order.status)))
      .sort(compareActiveOrders)
    activeOrderTotal.value = active.length
    activeOrders.value = active.slice(0, 4)
  } finally {
    loading.value = false
  }
}

function compareCreatedAt(left, right) {
  const timeDiff = new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()
  return timeDiff || Number(right.id || 0) - Number(left.id || 0)
}

function compareActiveOrders(left, right) {
  const rank = { 1: 0, 2: 0, 5: 0, 0: 1 }
  const rankDiff = (rank[Number(left.status)] ?? 2) - (rank[Number(right.status)] ?? 2)
  return rankDiff || compareCreatedAt(left, right)
}

function formatMoney(value) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`
}

function truncate(value, limit) {
  const characters = Array.from(String(value || ''))
  return characters.length > limit ? `${characters.slice(0, limit).join('')}...` : characters.join('')
}

function firstImage(images) {
  if (!images) return ''
  try {
    const parsed = JSON.parse(images)
    if (Array.isArray(parsed)) return parsed[0] || ''
  } catch { /* 兼容历史逗号分隔数据 */ }
  return String(images).split(',').map((item) => item.trim()).find(Boolean) || ''
}

function projectCardStatus(project) {
  if (Number(project.mode) === 1) {
    const invitation = {
      0: { text: '待回应', type: 'warning' },
      1: { text: '已接受', type: 'success' },
      2: { text: '已拒绝', type: 'danger' },
    }[Number(project.invitationStatus)]
    if (invitation) return invitation
  }
  return { text: statusText(PROJECT_STATUS, project.status), type: statusType(PROJECT_STATUS, project.status) }
}

function orderTitle(order) {
  return order.projectTitle || order.stallTitle || `订单 #${order.id}`
}

function orderCover(order) {
  return order.projectCover || order.stallCover || ''
}

function orderSource(order) {
  return order.projectId ? '企划订单' : '橱窗订单'
}

function orderProgress(order) {
  const status = Number(order.status)
  if (status === 0) return '等待画师确认接单'
  if (status === 2) return `第 ${(order.currentStage ?? 0) + 1} 阶段 · 等待您验收`
  if (status === 5) return '争议处理中'
  return `当前阶段：第 ${(order.currentStage ?? 0) + 1} 阶段`
}

function openOrder(id) {
  router.push({ name: 'DashboardOrderDetail', params: { id }, query: { participantRole: 'employer' } })
}

function openOrders() {
  router.push({ name: 'DashboardOrders', query: { participantRole: 'employer' } })
}

function openProject(id) {
  router.push({ name: 'ProjectDetail', params: { id } })
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard-home { display: flex; min-width: 0; flex-direction: column; gap: 16px; }
.wb-card { min-width: 0; box-sizing: border-box; border: 1px solid #e4ebf0; border-radius: 12px; background: #fff; box-shadow: 0 7px 24px rgb(42 67 84 / 5%); padding: 24px 26px; }
.welcome-card { display: flex; align-items: center; justify-content: space-between; gap: 20px; }
.welcome-card h2 { margin: 0 0 8px; color: #1b2934; font-size: 25px; }
.welcome-card p { margin: 0; color: #7a8791; font-size: 14px; line-height: 1.8; }
.welcome-actions { display: flex; flex-wrap: wrap; gap: 12px; }
.section-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.section-header h3 { margin: 0 0 6px; color: var(--artiv-text); font-size: 19px; }
.section-header p { margin: 0; color: var(--artiv-text-secondary); font-size: 13px; }
.dashboard-grid { display: grid; grid-template-columns: minmax(0, 2fr) minmax(230px, .82fr); align-items: stretch; gap: 16px; }
.compact-header { padding-bottom: 15px; border-bottom: 1px solid var(--artiv-border); }
.active-order-list,.recent-project-list { display: flex; flex-direction: column; }
.active-order,.recent-project { display: grid; grid-template-columns: 76px minmax(0, 1fr) minmax(105px, auto); align-items: center; gap: 14px; padding: 14px 0; border-bottom: 1px solid var(--artiv-border); cursor: pointer; }
.active-order:last-child,.recent-project:last-child { border-bottom: 0; padding-bottom: 0; }
.active-order:hover,.recent-project:hover { background: #fbfdff; }
.active-order img,.recent-project img,.item-cover { width: 76px; height: 60px; border-radius: 7px; object-fit: cover; }
.item-cover { display: grid; place-items: center; background: var(--artiv-section-alt); color: var(--artiv-text-secondary); font-size: 13px; }
.active-order-main,.recent-project-main { min-width: 0; }
.item-title-row { display: flex; min-width: 0; align-items: center; gap: 8px; }
.item-title-row h4 { min-width: 0; margin: 0; overflow: hidden; font-size: 15px; text-overflow: ellipsis; white-space: nowrap; }
.active-order-main p,.recent-project-main p { margin: 6px 0 0; overflow: hidden; color: var(--artiv-text-secondary); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.active-order-main .item-summary,.recent-project-main .item-summary { color: var(--artiv-text); }
.item-side { display: grid; justify-items: end; gap: 5px; }
.item-side small { color: var(--artiv-text-secondary); font-size: 12px; }
.item-side strong { color: var(--artiv-accent); font-size: 16px; white-space: nowrap; }
.quick-card { display: flex; flex-direction: column; }
.quick-list { display: grid; gap: 10px; padding-top: 14px; }
.quick-list button { display: flex; width: 100%; align-items: center; justify-content: space-between; gap: 12px; border: 1px solid var(--artiv-border); border-radius: 9px; background: #fff; padding: 14px; text-align: left; cursor: pointer; transition: border-color .2s, background .2s; }
.quick-list button:hover { border-color: var(--artiv-primary); background: #f7fcff; }
.quick-copy { display: flex; min-width: 0; flex-direction: column; gap: 4px; }
.quick-copy strong { color: var(--artiv-text); font-size: 14px; }
.quick-copy small { overflow: hidden; color: var(--artiv-text-secondary); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.quick-arrow { color: var(--artiv-primary); font-size: 24px; line-height: 1; }
@media (max-width: 980px) {
  .dashboard-grid { grid-template-columns: 1fr; }
  .quick-list { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 720px) {
  .welcome-card { align-items: flex-start; flex-direction: column; }
  .quick-list { grid-template-columns: 1fr; }
  .active-order,.recent-project { grid-template-columns: 62px minmax(0, 1fr); }
  .active-order img,.recent-project img,.item-cover { width: 62px; }
  .item-side { grid-column: 2; justify-items: start; }
}
</style>
