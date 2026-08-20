<template>
  <div v-if="total > 0" class="pie-chart">
    <div class="pie-visual">
      <svg viewBox="0 0 140 140" role="img" :aria-label="`${title}，共 ${total} 项`">
        <circle class="pie-track" cx="70" cy="70" :r="radius" />
        <circle
          v-for="segment in segments"
          :key="segment.name"
          class="pie-segment"
          cx="70"
          cy="70"
          :r="radius"
          :stroke="segment.color"
          :stroke-dasharray="segment.dasharray"
          :stroke-dashoffset="segment.dashoffset"
        />
      </svg>
      <div class="pie-total"><strong>{{ total }}</strong><span>合计</span></div>
    </div>
    <div class="pie-legend">
      <div v-for="segment in segments" :key="segment.name" class="legend-item">
        <i :style="{ background: segment.color }"></i>
        <span>{{ segment.name }}</span>
        <strong>{{ segment.value }}</strong>
        <small>{{ segment.percent }}%</small>
      </div>
    </div>
  </div>
  <p v-else class="pie-empty">暂无可统计数据</p>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '类型分布' },
  items: { type: Array, default: () => [] },
})

const colors = ['#0aa7df', '#55c2e6', '#ff9d45', '#7b8ff5', '#62c49b', '#f06f86', '#a979d9', '#f2c14e', '#5b8def', '#8db596']
const radius = 52
const circumference = 2 * Math.PI * radius
const normalized = computed(() => props.items
  .map(item => ({ name: item.name || '未分类', value: Number(item.value) || 0 }))
  .filter(item => item.value > 0)
  .sort((a, b) => b.value - a.value))
const total = computed(() => normalized.value.reduce((sum, item) => sum + item.value, 0))
const segments = computed(() => {
  let offset = 0
  return normalized.value.map((item, index) => {
    const length = item.value / total.value * circumference
    const visibleLength = Math.max(0, length - (normalized.value.length > 1 ? 2 : 0))
    const segment = {
      ...item,
      color: colors[index % colors.length],
      percent: Math.round(item.value * 1000 / total.value) / 10,
      dasharray: `${visibleLength} ${circumference - visibleLength}`,
      dashoffset: -offset,
    }
    offset += length
    return segment
  })
})
</script>

<style scoped>
.pie-chart{display:grid;grid-template-columns:190px 1fr;gap:24px;align-items:center;min-height:220px}.pie-visual{position:relative;width:180px;height:180px}.pie-visual svg{width:100%;height:100%;transform:rotate(-90deg)}.pie-track,.pie-segment{fill:none;stroke-width:24}.pie-track{stroke:#edf2f5}.pie-segment{stroke-linecap:butt;transition:stroke-dasharray .25s ease}.pie-total{position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center}.pie-total strong{font-size:28px}.pie-total span{color:#8b949e;font-size:12px}.pie-legend{display:grid;gap:10px}.legend-item{display:grid;grid-template-columns:10px minmax(70px,1fr) auto 48px;align-items:center;gap:8px;font-size:13px}.legend-item i{width:9px;height:9px;border-radius:50%}.legend-item span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.legend-item small{color:#8b949e;text-align:right}.pie-empty{display:flex;min-height:220px;margin:0;align-items:center;justify-content:center;color:#9aa2aa}@media(max-width:700px){.pie-chart{grid-template-columns:1fr}.pie-visual{margin:auto}}
</style>
