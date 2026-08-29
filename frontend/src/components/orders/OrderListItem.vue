<template>
  <article
    class="order-card"
    role="link"
    tabindex="0"
    :aria-label="`查看订单 ${orderTitle}`"
    @click="$emit('open-order', order.id)"
    @keydown.enter.self="$emit('open-order', order.id)"
    @keydown.space.self.prevent="$emit('open-order', order.id)"
  >
    <button
      type="button"
      class="order-source-cover"
      :aria-label="`查看${sourceLabel} ${orderTitle}`"
      @click.stop="openSource"
    >
      <img v-if="orderCover" :src="orderCover" :alt="`${sourceLabel}封面`" />
      <span v-else class="order-cover">{{ sourceLabel }}</span>
    </button>
    <div class="order-main">
      <div class="order-title-row">
        <button type="button" class="order-source-title" @click.stop="openSource">{{ orderTitle }}</button>
        <el-tag :type="statusType(ORDER_STATUS, order.status)">{{ orderStatusText(order.status, participantRole) }}</el-tag>
      </div>
      <p>订单编号：{{ order.id }} · 下单时间：{{ formatDateTime(order.createdAt) }}</p>
      <p class="stage">当前阶段：第 {{ (order.currentStage ?? 0) + 1 }} 阶段</p>
    </div>
    <div class="order-price">
      <div class="order-actions">
        <template v-if="participantRole === 'painter' && Number(order.status) === 0">
          <el-button class="order-primary-action" type="primary" round :loading="actionLoading" @click.stop="$emit('accept', order)">接受</el-button>
          <el-button class="order-secondary-action" plain round :disabled="actionLoading" @click.stop="$emit('reject', order)">拒绝</el-button>
        </template>
        <el-button
          v-else-if="participantRole === 'painter' && Number(order.status) === 1"
          class="order-primary-action"
          type="primary"
          round
          @click.stop="$emit('deliver', order.id)"
        >交付</el-button>
        <el-button v-if="showReview" class="order-primary-action" type="primary" round @click.stop="$emit('review', order)">去评价</el-button>
      </div>
      <strong>¥{{ money(order.amount) }}</strong>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { formatDateTime } from '@/utils/date'
import { ORDER_STATUS, orderStatusText, statusType } from '@/utils/status'

const props = defineProps({
  order: { type: Object, required: true },
  participantRole: { type: String, default: 'employer' },
  showReview: { type: Boolean, default: false },
  actionLoading: { type: Boolean, default: false },
})

const emit = defineEmits(['open-order', 'open-project', 'open-stall', 'review', 'deliver', 'accept', 'reject'])

const orderTitle = computed(() => props.order.projectTitle || props.order.stallTitle || `订单 #${props.order.id}`)
const orderCover = computed(() => props.order.projectCover || props.order.stallCover || '')
const sourceLabel = computed(() => props.order.stallId ? '橱窗' : props.order.projectId ? '企划' : '订单')
function openSource() {
  if (props.order.stallId) emit('open-stall', props.order.stallId)
  else if (props.order.projectId) emit('open-project', props.order.projectId)
  else emit('open-order', props.order.id)
}
function money(value) { return Number(value || 0).toFixed(2) }
</script>

<style scoped>
.order-card { display: grid; width: 100%; box-sizing: border-box; grid-template-columns: 92px minmax(0,1fr) minmax(220px,240px); align-items: center; gap: 18px; padding: 16px 8px; border-bottom: 1px solid var(--artiv-border); border-radius: 6px; outline: none; cursor: pointer; transition: background-color .18s ease, box-shadow .18s ease; }
.order-card:hover,.order-card:focus-visible { background: #f8fcfe; box-shadow: inset 3px 0 0 color-mix(in srgb, var(--artiv-primary) 55%, transparent); }
.order-source-cover,.order-source-title { margin: 0; border: 0; background: transparent; padding: 0; color: inherit; font: inherit; text-align: left; cursor: pointer; }
.order-source-cover { display: block; width: 92px; height: 72px; overflow: hidden; border-radius: 7px; }
.order-card img,.order-cover { width: 92px; height: 72px; border-radius: 7px; object-fit: cover; }
.order-source-cover img { display: block; transition: opacity .18s ease; }
.order-source-cover:hover img,.order-source-cover:focus-visible img { opacity: .86; }
.order-cover { display: grid; place-items: center; background: var(--artiv-section-alt); color: var(--artiv-text-secondary); }
.order-title-row { display: flex; min-width: 0; align-items: center; gap: 10px; }
.order-source-title { min-width: 0; overflow: hidden; font-size: 16px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.order-source-title:hover,.order-source-title:focus-visible { color: var(--artiv-primary); text-decoration: underline; text-underline-offset: 3px; }
.order-main { min-width: 0; }
.order-main p { margin: 8px 0 0; color: var(--artiv-text-secondary); font-size: 13px; }
.order-main .stage { color: var(--artiv-text); }
.order-price { display: flex; min-width: 0; align-items: center; justify-content: flex-end; gap: 14px; }
.order-price strong { flex: 0 0 auto; color: var(--artiv-accent); font-size: 18px; white-space: nowrap; }
.order-actions { display: flex; flex: 0 0 auto; flex-wrap: wrap; justify-content: flex-end; gap: 6px 8px; }
.order-actions :deep(.el-button + .el-button) { margin-left: 0; }
.order-primary-action,.order-secondary-action { min-width: 66px; font-weight: 600; }
.order-primary-action { border: 0; box-shadow: 0 4px 10px rgb(0 169 232 / 16%); }
.order-primary-action:hover { box-shadow: 0 5px 13px rgb(0 169 232 / 23%); }
@media(max-width:720px){.order-card{grid-template-columns:70px 1fr}.order-source-cover,.order-card img,.order-cover{width:70px}.order-price{grid-column:2;justify-content:flex-start}.order-actions{justify-content:flex-start}}
</style>
