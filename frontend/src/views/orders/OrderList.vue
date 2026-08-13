<template>
  <div class="order-list wb-card">
    <header class="order-header">
      <div><h2>订单管理</h2><p>统一查看交易状态，并进入订单详情处理当前待办。</p></div>
      <el-button @click="load">刷新</el-button>
    </header>

    <el-tabs v-if="isPainterWorkbench" v-model="participantRole" class="participant-tabs" @tab-change="load">
      <el-tab-pane label="我接的订单" name="painter" />
      <el-tab-pane label="我下的订单" name="employer" />
    </el-tabs>

    <el-tabs v-model="source" class="source-tabs" @tab-change="load">
      <el-tab-pane label="企划订单" name="project" />
      <el-tab-pane label="橱窗订单" name="stall" />
    </el-tabs>

    <div class="status-filters" aria-label="订单状态筛选">
      <button v-for="item in filters" :key="item.value" :class="{ active: statusGroup === item.value }" @click="selectFilter(item.value)">
        {{ item.label }}
      </button>
    </div>

    <div v-loading="loading" class="order-cards">
      <el-empty v-if="!orders.length" :description="source === 'project' ? '企划订单将在应征订单流程中接入' : '暂无符合条件的橱窗订单'" />
      <article v-for="order in orders" :key="order.id" class="order-card" @click="goOrderDetail(order.id)">
        <img v-if="order.stallCover" :src="order.stallCover" alt="橱窗封面" />
        <div v-else class="order-cover">订单</div>
        <div class="order-main">
          <div class="order-title-row"><h3>{{ order.stallTitle || `订单 #${order.id}` }}</h3><el-tag :type="statusType(ORDER_STATUS, order.status)">{{ statusText(ORDER_STATUS, order.status) }}</el-tag></div>
          <p>订单编号：{{ order.id }} · 下单时间：{{ formatDateTime(order.createdAt) }}</p>
          <p class="stage">当前阶段：第 {{ (order.currentStage ?? 0) + 1 }} 阶段</p>
        </div>
        <div class="order-price">
          <small>托管金额</small><strong>¥{{ money(order.amount) }}</strong>
          <div class="order-actions">
            <el-button v-if="order.stallId" type="primary" link @click.stop="goStallDetail(order.stallId)">橱窗详情</el-button>
            <el-button type="primary" link @click.stop="goOrderDetail(order.id)">订单详情</el-button>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getOrders } from '@/api/order'
import { formatDateTime } from '@/utils/date'
import { ORDER_STATUS, statusText, statusType } from '@/utils/status'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const source = ref('stall')
const statusGroup = ref('all')
const participantRole = ref('painter')
const orders = ref([])
const loading = ref(false)
const filters = [
  { label: '全部订单', value: 'all' },
  { label: '待交付', value: 'delivery' },
  { label: '待评价', value: 'review' },
  { label: '退款/售后', value: 'afterSale' },
]

async function load() {
  loading.value = true
  try {
    const data = await getOrders({
      page: 1,
      size: 30,
      source: source.value,
      statusGroup: statusGroup.value,
      participantRole: isPainterWorkbench.value ? participantRole.value : 'employer',
    })
    orders.value = data.records || []
  } finally { loading.value = false }
}
function selectFilter(value) { statusGroup.value = value; load() }
const isPainterWorkbench = computed(() => userStore.workbenchRole === 2)
function goOrderDetail(id) { router.push({ name: isPainterWorkbench.value ? 'PainterOrderDetail' : 'DashboardOrderDetail', params: { id } }) }
function goStallDetail(id) { router.push({ name: 'StallDetail', params: { id }, query: { from: 'order' } }) }
function money(value) { return Number(value || 0).toFixed(2) }
onMounted(load)
</script>

<style scoped>
.wb-card { background: #fff; border-radius: 8px; padding: 24px; }
.order-header { display: flex; justify-content: space-between; align-items: flex-start; }
.order-header h2 { margin: 0 0 7px; font-size: 21px; }
.order-header p { margin: 0; color: var(--huiyue-text-secondary); }
.source-tabs { margin-top: 18px; }
.participant-tabs { margin-top: 18px; }
.participant-tabs + .source-tabs { margin-top: 0; }
.source-tabs :deep(.el-tabs__header) { margin-bottom: 14px; }
.status-filters { display: flex; gap: 10px; padding: 4px 0 18px; border-bottom: 1px solid var(--huiyue-border); }
.status-filters button { border: 1px solid var(--huiyue-border); border-radius: 18px; background: #fff; padding: 7px 18px; color: var(--huiyue-text-secondary); cursor: pointer; }
.status-filters button.active { border-color: var(--huiyue-primary); background: var(--huiyue-primary); color: #fff; }
.order-cards { min-height: 360px; padding-top: 16px; }
.order-card { display: grid; grid-template-columns: 92px minmax(0,1fr) 120px; align-items: center; gap: 18px; padding: 16px 0; border-bottom: 1px solid var(--huiyue-border); cursor: pointer; }
.order-card:hover { background: #fbfdff; }
.order-card img,.order-cover { width: 92px; height: 72px; border-radius: 7px; object-fit: cover; }
.order-cover { display: grid; place-items: center; background: var(--huiyue-section-alt); color: var(--huiyue-text-secondary); }
.order-title-row { display: flex; align-items: center; gap: 10px; }
.order-title-row h3 { margin: 0; font-size: 16px; }
.order-main p { margin: 8px 0 0; color: var(--huiyue-text-secondary); font-size: 13px; }
.order-main .stage { color: var(--huiyue-text); }
.order-price { display: grid; justify-items: end; gap: 7px; }
.order-price small { color: var(--huiyue-text-secondary); }
.order-price strong { color: var(--huiyue-accent); font-size: 18px; }
.order-actions { display: flex; justify-content: flex-end; gap: 4px; white-space: nowrap; }
@media(max-width:720px){.order-card{grid-template-columns:70px 1fr}.order-card img,.order-cover{width:70px}.order-price{grid-column:2;justify-items:start}.status-filters{overflow-x:auto}}
</style>
