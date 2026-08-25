<template>
  <div v-loading="loading" class="painter-home">
    <section v-if="certStatus !== 2" class="wb-card cert-card">
      <h2>您尚未完成画师认证</h2>
      <p>暂时无法使用画师身份的全部功能。在艺绘，您可以通过应征企划、售卖橱窗与个人或企业合作，完成约稿需求。</p>
      <div class="cert-steps">
        <div class="cert-step">
          <span class="cert-step__icon">1</span>
          <div>
            <strong>完善个人资料</strong>
            <p>上传完成度最高的作品，有助于快速通过认证。</p>
          </div>
        </div>
        <div class="cert-step">
          <span class="cert-step__icon">2</span>
          <div>
            <strong>申请画师认证</strong>
            <p>提交后由管理员审核，通过即可接稿与开设橱窗。</p>
          </div>
        </div>
      </div>
      <div class="cert-actions">
        <el-button type="primary" round @click="router.push({ name: 'PainterWorks' })">完善资料与作品</el-button>
        <el-button round @click="router.push({ name: 'AccountSettings', query: { section: 'identity' } })">进行画师认证</el-button>
      </div>
    </section>

    <section v-else class="wb-card welcome-card">
      <div>
        <h2>欢迎您，{{ userStore.user?.nickname || userStore.user?.username }}</h2>
        <p>这里汇总您当前的接稿进度和经营数据。</p>
      </div>
      <div class="welcome-actions">
        <el-button type="primary" round @click="router.push({ name: 'Projects' })">浏览企划</el-button>
        <el-button round @click="openPublicProfile">查看公开主页</el-button>
      </div>
    </section>

    <template v-if="certStatus === 2">
      <section class="wb-card overview-card">
        <header class="section-header">
          <div>
            <h3>经营概览</h3>
            <p>根据现有订单和公开评价实时汇总。</p>
          </div>
          <el-button @click="loadDashboard">刷新</el-button>
        </header>
        <div class="business-metrics">
          <article class="business-metric">
            <small>本月收入</small>
            <strong>{{ formatMoney(overview.monthRevenue) }}</strong>
            <span>{{ overview.monthCompleted }} 笔已完成订单</span>
          </article>
          <article class="business-metric">
            <small>累计成交</small>
            <strong>{{ formatMoney(overview.totalRevenue) }}</strong>
            <span>仅统计已完成订单</span>
          </article>
          <article class="business-metric">
            <small>已完成订单</small>
            <strong>{{ overview.completedOrders }}</strong>
            <span>进行中 {{ activeOrderTotal }} 笔</span>
          </article>
          <article class="business-metric">
            <small>综合评分</small>
            <strong>{{ overview.averageScore === null ? '暂无' : overview.averageScore.toFixed(1) }}</strong>
            <span>{{ overview.reviewCount ? `${overview.reviewCount} 条公开评价` : '尚无公开评价' }}</span>
          </article>
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
              <div v-else class="order-cover">订单</div>
              <div class="active-order-main">
                <div class="active-order-title">
                  <h4 :title="orderTitle(order)">{{ orderTitle(order) }}</h4>
                  <el-tag size="small" :type="statusType(ORDER_STATUS, order.status)">
                    {{ orderStatusText(order.status, 'painter') }}
                  </el-tag>
                </div>
                <p>{{ orderSource(order) }} · 订单 #{{ order.id }} · {{ formatDateTime(order.createdAt) }}</p>
                <p class="order-progress">{{ orderProgress(order) }}</p>
              </div>
              <div class="active-order-side">
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
              <p>快速进入常用接稿功能。</p>
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
    </template>

    <section v-else class="stat-row">
      <div class="stat-card"><strong>{{ counts.works }}</strong><span>我的作品</span></div>
      <div class="stat-card"><strong>{{ counts.stalls }}</strong><span>我的橱窗</span></div>
      <div class="stat-card"><strong>{{ counts.applications }}</strong><span>我的应征</span></div>
      <div class="stat-card"><strong>{{ counts.invitations }}</strong><span>待处理邀请</span></div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getOrders } from '@/api/order'
import { getMyWorks } from '@/api/work'
import { getMyStalls } from '@/api/stall'
import { getMyApplications } from '@/api/project'
import { getInvitations } from '@/api/invitation'
import { getPublicProfile } from '@/api/user'
import { formatDateTime } from '@/utils/date'
import { ORDER_STATUS, orderStatusText, statusType } from '@/utils/status'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const counts = reactive({ works: 0, stalls: 0, applications: 0, invitations: 0 })
const overview = reactive({ monthRevenue: 0, totalRevenue: 0, completedOrders: 0, monthCompleted: 0, averageScore: null, reviewCount: 0 })
const activeOrders = ref([])
const activeOrderTotal = ref(0)

const certStatus = computed(() => userStore.user?.certStatus ?? 0)
const quickActions = computed(() => [
  { label: '管理作品', detail: `${counts.works} 件作品`, to: { name: 'PainterWorks' } },
  { label: '管理橱窗', detail: `${counts.stalls} 个服务`, to: { name: 'PainterStalls' } },
  { label: '我的应征', detail: `${counts.applications} 条有效应征`, to: { name: 'PainterApplications' } },
  { label: '收到的邀请', detail: `${counts.invitations} 条待处理`, to: { name: 'PainterInvitations' } },
])

async function fetchAllOrders(source) {
  const params = { page: 1, size: 50, source, statusGroup: 'all', participantRole: 'painter' }
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
  const userId = userStore.user?.id
  try {
    const [works, stalls, applications, invitations, profile, projectOrders, stallOrders] = await Promise.all([
      getMyWorks({ page: 1, size: 1 }).catch(() => ({ total: 0 })),
      getMyStalls({ page: 1, size: 1 }).catch(() => ({ total: 0 })),
      getMyApplications({ page: 1, size: 1 }).catch(() => ({ total: 0 })),
      getInvitations({ page: 1, size: 1, status: 0 }).catch(() => ({ total: 0 })),
      userId ? getPublicProfile(userId, 'painter').catch(() => ({})) : {},
      fetchAllOrders('project').catch(() => []),
      fetchAllOrders('stall').catch(() => []),
    ])

    counts.works = Number(works.total || 0)
    counts.stalls = Number(stalls.total || 0)
    counts.applications = Number(applications.total || 0)
    counts.invitations = Number(invitations.total || 0)

    const orders = [...projectOrders, ...stallOrders]
    const completed = orders.filter((order) => Number(order.status) === 3)
    const now = new Date()
    const completedThisMonth = completed.filter((order) => {
      const completedAt = new Date(order.completedAt)
      return !Number.isNaN(completedAt.getTime())
        && completedAt.getFullYear() === now.getFullYear()
        && completedAt.getMonth() === now.getMonth()
    })
    overview.monthRevenue = sumAmounts(completedThisMonth)
    overview.totalRevenue = sumAmounts(completed)
    overview.completedOrders = completed.length
    overview.monthCompleted = completedThisMonth.length
    overview.averageScore = profile.averageScore === null || profile.averageScore === undefined
      ? null
      : Number(profile.averageScore)
    overview.reviewCount = Number(profile.reviewCount || 0)

    const active = orders.filter((order) => [0, 1, 2, 5].includes(Number(order.status)))
      .sort(compareActiveOrders)
    activeOrderTotal.value = active.length
    activeOrders.value = active.slice(0, 4)
  } finally {
    loading.value = false
  }
}

function compareActiveOrders(left, right) {
  const rank = { 1: 0, 2: 0, 5: 0, 0: 1 }
  const rankDiff = (rank[Number(left.status)] ?? 2) - (rank[Number(right.status)] ?? 2)
  if (rankDiff) return rankDiff
  const timeDiff = new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()
  return timeDiff || Number(right.id || 0) - Number(left.id || 0)
}

function sumAmounts(orders) {
  return orders.reduce((total, order) => total + Number(order.amount || 0), 0)
}

function formatMoney(value) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`
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
  if (status === 0) return '等待您确认接单'
  if (status === 2) return `第 ${(order.currentStage ?? 0) + 1} 阶段 · 等待企划方验收`
  if (status === 5) return '争议处理中'
  return `当前阶段：第 ${(order.currentStage ?? 0) + 1} 阶段`
}

function openOrder(id) {
  router.push({ name: 'PainterOrderDetail', params: { id }, query: { participantRole: 'painter' } })
}

function openOrders() {
  router.push({ name: 'PainterOrders', query: { participantRole: 'painter' } })
}

function openPublicProfile() {
  router.push({ name: 'ArtistProfile', params: { id: userStore.user?.id }, query: { role: 'painter' } })
}

onMounted(loadDashboard)
</script>

<style scoped>
.painter-home { display: flex; min-width: 0; flex-direction: column; gap: 16px; }
.wb-card { min-width: 0; box-sizing: border-box; border: 1px solid #e4ebf0; border-radius: 12px; background: #fff; box-shadow: 0 7px 24px rgb(42 67 84 / 5%); padding: 24px 26px; }
.cert-card h2,.welcome-card h2 { margin: 0 0 8px; color: #1b2934; font-size: 25px; }
.cert-card > p,.welcome-card p { margin: 0; color: #7a8791; font-size: 14px; line-height: 1.8; }
.cert-steps { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 20px; }
.cert-step { display: flex; gap: 14px; border-radius: 8px; background: var(--artiv-section-alt); padding: 18px; }
.cert-step__icon { display: inline-flex; width: 30px; height: 30px; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 50%; background: var(--artiv-primary); color: #fff; font-weight: 600; }
.cert-step strong { font-size: 14px; }
.cert-step p { margin: 6px 0 0; color: var(--artiv-text-secondary); font-size: 13px; line-height: 1.7; }
.cert-actions,.welcome-actions { display: flex; flex-wrap: wrap; gap: 12px; }
.cert-actions { margin-top: 22px; }
.welcome-card { display: flex; align-items: center; justify-content: space-between; gap: 20px; }
.section-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.section-header h3 { margin: 0 0 6px; color: var(--artiv-text); font-size: 19px; }
.section-header p { margin: 0; color: var(--artiv-text-secondary); font-size: 13px; }
.business-metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; margin-top: 20px; }
.business-metric { display: flex; min-width: 0; flex-direction: column; gap: 7px; border: 1px solid var(--artiv-border); border-radius: 9px; background: #fbfdff; padding: 18px; }
.business-metric small,.business-metric span { color: var(--artiv-text-secondary); font-size: 12px; }
.business-metric strong { overflow: hidden; color: var(--artiv-primary); font-size: 23px; text-overflow: ellipsis; white-space: nowrap; }
.dashboard-grid { display: grid; grid-template-columns: minmax(0, 2fr) minmax(230px, .82fr); align-items: stretch; gap: 16px; }
.compact-header { padding-bottom: 15px; border-bottom: 1px solid var(--artiv-border); }
.active-order-list { display: flex; flex-direction: column; }
.active-order { display: grid; grid-template-columns: 76px minmax(0, 1fr) minmax(105px, auto); align-items: center; gap: 14px; padding: 14px 0; border-bottom: 1px solid var(--artiv-border); cursor: pointer; }
.active-order:last-child { border-bottom: 0; padding-bottom: 0; }
.active-order:hover { background: #fbfdff; }
.active-order img,.order-cover { width: 76px; height: 60px; border-radius: 7px; object-fit: cover; }
.order-cover { display: grid; place-items: center; background: var(--artiv-section-alt); color: var(--artiv-text-secondary); font-size: 13px; }
.active-order-main { min-width: 0; }
.active-order-title { display: flex; min-width: 0; align-items: center; gap: 8px; }
.active-order-title h4 { min-width: 0; margin: 0; overflow: hidden; font-size: 15px; text-overflow: ellipsis; white-space: nowrap; }
.active-order-main p { margin: 6px 0 0; overflow: hidden; color: var(--artiv-text-secondary); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.active-order-main .order-progress { color: var(--artiv-text); }
.active-order-side { display: grid; justify-items: end; gap: 5px; }
.active-order-side small { color: var(--artiv-text-secondary); font-size: 12px; }
.active-order-side strong { color: var(--artiv-accent); font-size: 16px; }
.quick-card { display: flex; flex-direction: column; }
.quick-list { display: grid; gap: 10px; padding-top: 14px; }
.quick-list button { display: flex; width: 100%; align-items: center; justify-content: space-between; gap: 12px; border: 1px solid var(--artiv-border); border-radius: 9px; background: #fff; padding: 14px; text-align: left; cursor: pointer; transition: border-color .2s, background .2s; }
.quick-list button:hover { border-color: var(--artiv-primary); background: #f7fcff; }
.quick-copy { display: flex; min-width: 0; flex-direction: column; gap: 4px; }
.quick-copy strong { color: var(--artiv-text); font-size: 14px; }
.quick-copy small { overflow: hidden; color: var(--artiv-text-secondary); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.quick-arrow { color: var(--artiv-primary); font-size: 24px; line-height: 1; }
.stat-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat-card { display: flex; flex-direction: column; gap: 6px; border: 1px solid #e4ebf0; border-radius: 12px; background: #fff; box-shadow: 0 7px 24px rgb(42 67 84 / 5%); padding: 24px 26px; }
.stat-card strong { color: var(--artiv-primary); font-size: 24px; }
.stat-card span { color: var(--artiv-text-secondary); font-size: 13px; }
@media (max-width: 980px) {
  .business-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .dashboard-grid { grid-template-columns: 1fr; }
  .quick-list { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 720px) {
  .welcome-card { align-items: flex-start; flex-direction: column; }
  .cert-steps,.business-metrics,.quick-list,.stat-row { grid-template-columns: 1fr; }
  .active-order { grid-template-columns: 62px minmax(0, 1fr); }
  .active-order img,.order-cover { width: 62px; }
  .active-order-side { grid-column: 2; justify-items: start; }
}
</style>
