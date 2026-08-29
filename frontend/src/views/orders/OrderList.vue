<template>
  <div class="order-list wb-card">
    <header class="order-header">
      <div><h2>{{ listTitle }}</h2><p>{{ listDescription }}</p></div>
      <el-button @click="load">刷新</el-button>
    </header>

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
      <el-empty v-if="!orders.length" :description="source === 'project' ? '暂无符合条件的企划订单' : '暂无符合条件的橱窗订单'" />
      <OrderListItem
        v-for="order in orders"
        :key="order.id"
        :order="order"
        :participant-role="participantRole"
        :show-review="statusGroup === 'review'"
        :action-loading="actingOrderId === order.id"
        @open-order="goOrderDetail"
        @open-project="goProjectDetail"
        @open-stall="goStallDetail"
        @review="openReview"
        @deliver="goDelivery"
        @accept="quickAccept"
        @reject="quickReject"
      />
    </div>
    <ReviewDialog v-model="reviewVisible" :order-id="reviewTarget?.id" :order-title="reviewTarget ? orderTitle(reviewTarget) : ''" @submitted="reviewSubmitted" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { acceptOrder, getOrders, rejectOrder } from '@/api/order'
import ReviewDialog from '@/components/reviews/ReviewDialog.vue'
import OrderListItem from '@/components/orders/OrderListItem.vue'

const route = useRoute()
const router = useRouter()
const source = ref('stall')
const statusGroup = ref('all')
const participantRole = computed(() => route.path.startsWith('/dashboard') ? 'employer' : 'painter')
const listTitle = computed(() => participantRole.value === 'employer' ? '我委托的订单' : '我承接的订单')
const listDescription = computed(() => participantRole.value === 'employer' ? '查看我向画师发起的全部交易。' : '查看我作为画师承接的全部交易。')
const listState = ref({ queryKey: '', records: [] })
const loadingQueryKey = ref('')
const reviewVisible = ref(false)
const reviewTarget = ref(null)
const actingOrderId = ref(null)
let activeRequestController = null
let requestSequence = 0
const filters = [
  { label: '全部订单', value: 'all' },
  { label: '待交付', value: 'delivery' },
  { label: '待评价', value: 'review' },
  { label: '退款/售后', value: 'afterSale' },
]

const currentQueryKey = computed(() => [participantRole.value, source.value, statusGroup.value].join(':'))
const orders = computed(() => listState.value.queryKey === currentQueryKey.value ? listState.value.records : [])
const loading = computed(() => loadingQueryKey.value === currentQueryKey.value)

async function load() {
  const params = {
    page: 1,
    size: 30,
    source: source.value,
    statusGroup: statusGroup.value,
    participantRole: participantRole.value,
  }
  const queryKey = [params.participantRole, params.source, params.statusGroup].join(':')
  const requestId = ++requestSequence
  activeRequestController?.abort()
  const controller = new AbortController()
  activeRequestController = controller
  loadingQueryKey.value = queryKey
  try {
    const data = await getOrders(params, { signal: controller.signal })
    if (requestId !== requestSequence || queryKey !== currentQueryKey.value) return
    listState.value = { queryKey, records: data.records || [] }
  } catch (error) {
    if (error?.code !== 'ERR_CANCELED') throw error
  } finally {
    if (requestId === requestSequence) loadingQueryKey.value = ''
  }
}
function selectFilter(value) { statusGroup.value = value; load() }
const isPainterRoute = computed(() => route.path.startsWith('/painter'))
function goOrderDetail(id) {
  router.push({
    name: isPainterRoute.value ? 'PainterOrderDetail' : 'DashboardOrderDetail',
    params: { id },
  })
}
function goDelivery(id) {
  router.push({
    name: 'PainterOrderDetail',
    params: { id },
    query: { action: 'deliver' },
  })
}
async function quickAccept(order) {
  actingOrderId.value = order.id
  try {
    await acceptOrder(order.id)
    ElMessage.success('已接受订单，可以开始创作')
    await load()
  } finally {
    actingOrderId.value = null
  }
}
async function quickReject(order) {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('可填写拒绝原因', `拒绝订单 #${order.id}`, {
      inputType: 'textarea',
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
    })
    reason = result.value
  } catch {
    return
  }
  actingOrderId.value = order.id
  try {
    await rejectOrder(order.id, reason)
    ElMessage.success('已拒绝订单')
    await load()
  } finally {
    actingOrderId.value = null
  }
}
function goStallDetail(id) { router.push({ name: 'StallDetail', params: { id }, query: { from: 'order' } }) }
function goProjectDetail(id) { router.push({ name: 'ProjectDetail', params: { id }, query: { from: 'order' } }) }
function openReview(order) { reviewTarget.value = order; reviewVisible.value = true }
function reviewSubmitted() {
  if (reviewTarget.value && listState.value.queryKey === currentQueryKey.value) {
    listState.value = {
      ...listState.value,
      records: listState.value.records.filter((item) => item.id !== reviewTarget.value.id),
    }
  }
  reviewTarget.value = null
}
function orderTitle(order) { return order.projectTitle || order.stallTitle || `订单 #${order.id}` }
onMounted(load)
onBeforeUnmount(() => activeRequestController?.abort())
</script>

<style scoped>
.wb-card { min-width: 0; background: #fff; border-radius: 8px; padding: 24px; }
.order-header { display: flex; justify-content: space-between; align-items: flex-start; }
.order-header h2 { margin: 0 0 7px; font-size: 21px; }
.order-header p { margin: 0; color: var(--artiv-text-secondary); }
.source-tabs { margin-top: 18px; }
.source-tabs :deep(.el-tabs__header) { margin-bottom: 14px; }
.status-filters { display: flex; gap: 10px; padding: 4px 0 18px; border-bottom: 1px solid var(--artiv-border); }
.status-filters button { border: 1px solid var(--artiv-border); border-radius: 18px; background: #fff; padding: 7px 18px; color: var(--artiv-text-secondary); cursor: pointer; }
.status-filters button.active { border-color: var(--artiv-primary); background: var(--artiv-primary); color: #fff; }
.order-cards { min-width: 0; min-height: 360px; padding-top: 16px; }
@media(max-width:720px){.status-filters{overflow-x:auto}}
</style>
