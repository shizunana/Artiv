<template>
  <div v-loading="loading" class="dashboard-home">
    <section v-if="!hasDashboardHistory" class="wb-card onboarding-card">
      <div class="onboarding-copy">
        <span class="onboarding-kicker">FIRST PROJECT</span>
        <h2>欢迎您，{{ userStore.user?.nickname || userStore.user?.username }}</h2>
        <h3>从第一次发布企划开始</h3>
        <p class="onboarding-intro">
          清晰填写需求、预算和截稿日期，等待画师应征或主动邀请合适的画师，开启您的第一次合作。
        </p>

        <ol class="onboarding-steps">
          <li>
            <span>1</span>
            <div><strong>发布企划</strong><small>说明需求、预算与创作阶段</small></div>
          </li>
          <li>
            <span>2</span>
            <div><strong>选择画师</strong><small>查看应征，或主动邀请画师</small></div>
          </li>
          <li>
            <span>3</span>
            <div><strong>建立订单</strong><small>沟通细节并按阶段完成交付</small></div>
          </li>
        </ol>

        <div class="onboarding-actions">
          <button type="button" class="employer-guide-link" @click="router.push({ name: 'EmployerGuide' })">
            <el-icon><Reading /></el-icon>
            <span>企划方指南</span>
            <span class="employer-guide-link__arrow" aria-hidden="true">→</span>
          </button>
          <el-button type="primary" round @click="router.push({ name: 'ProjectNew' })">立即发布企划</el-button>
        </div>
      </div>

      <div class="onboarding-visual" aria-hidden="true">
        <div class="visual-glow"></div>
        <div class="visual-document">
          <span class="visual-document__label">NEW PROJECT</span>
          <div class="visual-document__title"></div>
          <div class="visual-document__line visual-document__line--wide"></div>
          <div class="visual-document__line"></div>
          <div class="visual-document__preview">
            <span></span>
            <i></i>
            <i></i>
          </div>
          <div class="visual-document__footer"><i></i><i></i><i></i></div>
        </div>
        <div class="visual-brand">艺</div>
      </div>
    </section>

    <template v-else>
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
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Reading } from '@element-plus/icons-vue'
import { getMyProjects } from '@/api/project'
import { getOrders } from '@/api/order'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/date'
import { formatProjectBudget } from '@/utils/budget'
import { ORDER_STATUS, PROJECT_STATUS, orderStatusText, statusText, statusType } from '@/utils/status'
import { isProjectExpired } from '@/utils/projectDeadline'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const counts = reactive({ projects: 0, recruiting: 0 })
const activeOrders = ref([])
const activeOrderTotal = ref(0)
const totalOrderCount = ref(0)
const recentProjects = ref([])

const hasDashboardHistory = computed(() => counts.projects > 0 || totalOrderCount.value > 0)

const quickActions = computed(() => [
  { label: '发布企划', detail: '创建公开企划或定向邀请', to: { name: 'ProjectNew' } },
  { label: '我的企划', detail: `${counts.projects} 个企划 · ${counts.recruiting} 个招募中`, to: { name: 'DashboardProjects' } },
  { label: '我委托的订单', detail: `${activeOrderTotal.value} 笔进行中`, to: { name: 'DashboardOrders' } },
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
    counts.recruiting = projects.filter((project) => Number(project.status) === 0 && !isProjectExpired(project.deadline)).length
    recentProjects.value = sortedProjects.slice(0, 4)

    const orders = [...projectOrders, ...stallOrders]
    totalOrderCount.value = orders.length
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
  if (Number(project.moderationStatus) === 1) return { text: '违规下架', type: 'danger' }
  if (Number(project.status) === 4) {
    return Number(project.mode) === 1 && Number(project.invitationStatus) === 2
      ? { text: '已拒绝', type: 'danger' }
      : { text: '已下架', type: 'info' }
  }
  if (isProjectExpired(project.deadline)) return { text: '已过期', type: 'info' }
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
  router.push({ name: 'DashboardOrderDetail', params: { id } })
}

function openOrders() {
  router.push({ name: 'DashboardOrders' })
}

function openProject(id) {
  router.push({ name: 'ProjectDetail', params: { id } })
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard-home { display: flex; min-width: 0; flex-direction: column; gap: 16px; }
.wb-card { min-width: 0; box-sizing: border-box; border: 1px solid #e4ebf0; border-radius: 12px; background: #fff; box-shadow: 0 7px 24px rgb(42 67 84 / 5%); padding: 24px 26px; }
.onboarding-card { display: grid; min-height: 510px; grid-template-columns: minmax(0, 1.08fr) minmax(320px, .92fr); align-items: center; gap: 44px; overflow: hidden; padding: 48px 54px; }
.onboarding-copy { position: relative; z-index: 1; }
.onboarding-kicker { display: inline-flex; align-items: center; border-radius: 999px; background: #e8f7ff; padding: 6px 11px; color: var(--artiv-primary); font-size: 11px; font-weight: 700; letter-spacing: .12em; }
.onboarding-copy h2 { margin: 17px 0 8px; color: #1b2934; font-size: 29px; line-height: 1.35; }
.onboarding-copy h3 { margin: 0 0 12px; color: var(--artiv-text); font-size: 18px; }
.onboarding-intro { max-width: 560px; margin: 0; color: var(--artiv-text-secondary); font-size: 14px; line-height: 1.85; }
.onboarding-steps { display: grid; margin: 30px 0; padding: 0; gap: 15px; list-style: none; }
.onboarding-steps li { display: flex; align-items: center; gap: 13px; }
.onboarding-steps li > span { display: grid; width: 30px; height: 30px; flex: 0 0 auto; place-items: center; border-radius: 50%; background: var(--artiv-primary); color: #fff; font-size: 13px; font-weight: 700; box-shadow: 0 5px 13px rgb(0 169 230 / 18%); }
.onboarding-steps li div { display: flex; flex-wrap: wrap; align-items: baseline; gap: 6px 10px; }
.onboarding-steps strong { color: var(--artiv-text); font-size: 14px; }
.onboarding-steps small { color: var(--artiv-text-secondary); font-size: 12px; }
.onboarding-actions { display: flex; flex-wrap: wrap; gap: 12px; }
.employer-guide-link { display: inline-flex; height: 32px; align-items: center; gap: 6px; border: 0; border-radius: 8px; background: #edf8fd; padding: 0 12px; color: var(--artiv-primary); cursor: pointer; font-size: 13px; font-weight: 600; transition: background .2s, color .2s; }
.employer-guide-link:hover { background: #dff3fc; color: #008dcc; }
.employer-guide-link:focus-visible { outline: 2px solid rgb(0 168 233 / 28%); outline-offset: 2px; }
.employer-guide-link .el-icon { font-size: 15px; }
.employer-guide-link__arrow { margin-left: 2px; font-size: 15px; transition: transform .2s; }
.employer-guide-link:hover .employer-guide-link__arrow { transform: translateX(2px); }
.onboarding-visual { position: relative; min-height: 390px; }
.visual-glow { position: absolute; inset: 26px 2px 12px 26px; border-radius: 46% 54% 41% 59% / 52% 40% 60% 48%; background: #edf8fe; }
.visual-document { position: absolute; z-index: 2; top: 52px; right: 35px; width: 250px; min-height: 292px; box-sizing: border-box; transform: rotate(2.5deg); border: 1px solid #dcebf3; border-radius: 18px; background: #fff; box-shadow: 0 22px 42px rgb(64 108 132 / 14%); padding: 28px 25px; }
.visual-document__label { color: var(--artiv-primary); font-size: 10px; font-weight: 700; letter-spacing: .12em; }
.visual-document__title { width: 62%; height: 13px; margin: 19px 0 17px; border-radius: 999px; background: #d8e9f2; }
.visual-document__line { width: 70%; height: 7px; margin-top: 10px; border-radius: 999px; background: #edf3f6; }
.visual-document__line--wide { width: 92%; }
.visual-document__preview { display: grid; height: 90px; grid-template-columns: 1fr 1fr; grid-template-rows: 1fr 1fr; margin-top: 22px; gap: 8px; }
.visual-document__preview span { grid-row: 1 / 3; border-radius: 10px; background: #dff4fd; }
.visual-document__preview i { border-radius: 8px; background: #f1f6f8; }
.visual-document__footer { display: flex; margin-top: 18px; gap: 7px; }
.visual-document__footer i { width: 34px; height: 8px; border-radius: 999px; background: #eaf1f5; }
.visual-brand { position: absolute; z-index: 3; top: 20px; right: 14px; display: grid; width: 62px; height: 62px; place-items: center; border: 7px solid #fff; border-radius: 20px; background: var(--artiv-primary); box-shadow: 0 14px 28px rgb(0 169 230 / 22%); color: #fff; font-size: 28px; font-weight: 800; transform: rotate(6deg); }
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
  .onboarding-card { grid-template-columns: 1fr; padding: 38px; }
  .onboarding-visual { display: none; }
  .dashboard-grid { grid-template-columns: 1fr; }
  .quick-list { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 720px) {
  .onboarding-card { min-height: 0; padding: 30px 24px; }
  .onboarding-copy h2 { font-size: 24px; }
  .welcome-card { align-items: flex-start; flex-direction: column; }
  .quick-list { grid-template-columns: 1fr; }
  .active-order,.recent-project { grid-template-columns: 62px minmax(0, 1fr); }
  .active-order img,.recent-project img,.item-cover { width: 62px; }
  .item-side { grid-column: 2; justify-items: start; }
}
</style>
