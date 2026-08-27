<template>
  <div class="pie-chart-shell">
    <div class="pie-chart" :class="{ 'is-hidden': total <= 0 }">
      <div class="pie-visual">
        <div
          ref="chartRef"
          class="pie-canvas"
          role="img"
          :aria-label="`${title}，共 ${total} 项；鼠标移入扇区可查看类型、数量与占比`"
        ></div>
        <div class="pie-total" aria-hidden="true"><strong>{{ total }}</strong><span>合计</span></div>
      </div>
      <div class="pie-legend">
        <div v-for="segment in normalized" :key="segment.name" class="legend-item">
          <i :style="{ background: segment.color }"></i>
          <span>{{ segment.name }}</span>
          <strong>{{ segment.value }}</strong>
          <small>{{ segment.percent }}%</small>
        </div>
      </div>
    </div>
    <p v-if="total <= 0" class="pie-empty">暂无可统计数据</p>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { PieChart as EChartsPieChart } from 'echarts/charts'
import { TooltipComponent } from 'echarts/components'
import { init, use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'

use([EChartsPieChart, TooltipComponent, CanvasRenderer])

const props = defineProps({
  title: { type: String, default: '类型分布' },
  items: { type: Array, default: () => [] },
})

const colors = ['#0aa7df', '#55c2e6', '#ff9d45', '#7b8ff5', '#62c49b', '#f06f86', '#a979d9', '#f2c14e', '#5b8def', '#8db596']
const chartRef = ref(null)
let chart
let resizeObserver

const source = computed(() => props.items
  .map(item => ({ name: item.name || '未分类', value: Number(item.value) || 0 }))
  .filter(item => item.value > 0)
  .sort((a, b) => b.value - a.value))
const total = computed(() => source.value.reduce((sum, item) => sum + item.value, 0))
const normalized = computed(() => source.value.map((item, index) => ({
  ...item,
  color: colors[index % colors.length],
  percent: total.value ? Math.round(item.value * 1000 / total.value) / 10 : 0,
})))

const option = computed(() => ({
  animationDuration: 360,
  color: colors,
  tooltip: {
    trigger: 'item',
    backgroundColor: 'rgba(32, 43, 54, .94)',
    borderWidth: 0,
    padding: [9, 12],
    textStyle: { color: '#fff', fontSize: 12 },
    formatter: '{b}<br/>数量：{c}<br/>占比：{d}%',
  },
  series: [{
    name: props.title,
    type: 'pie',
    center: ['50%', '50%'],
    radius: ['38%', '62%'],
    minAngle: 2,
    avoidLabelOverlap: true,
    itemStyle: {
      borderColor: '#fff',
      borderWidth: 2,
    },
    label: { show: false },
    labelLine: { show: false },
    emphasis: {
      scale: true,
      scaleSize: 5,
      label: {
        show: true,
        position: 'outside',
        alignTo: 'edge',
        edgeDistance: 3,
        color: '#34414d',
        fontSize: 11,
        fontWeight: 700,
        formatter: '{b} {d}%',
      },
      labelLine: {
        show: true,
        length: 12,
        length2: 0,
        smooth: 0.18,
        lineStyle: { width: 1.4 },
      },
      itemStyle: { shadowBlur: 12, shadowColor: 'rgba(26, 88, 116, .24)' },
    },
    data: normalized.value.map(item => ({
      name: item.name,
      value: item.value,
      itemStyle: { color: item.color },
    })),
  }],
}))

const renderChart = () => {
  if (!chart || total.value <= 0) return
  chart.setOption(option.value, true)
}

onMounted(async () => {
  await nextTick()
  if (!chartRef.value) return
  chart = init(chartRef.value)
  resizeObserver = new ResizeObserver(() => chart?.resize())
  resizeObserver.observe(chartRef.value)
  renderChart()
})

watch(option, renderChart, { deep: true })

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
})
</script>

<style scoped>
.pie-chart-shell{position:relative;min-height:220px}.pie-chart{display:grid;grid-template-columns:190px 1fr;gap:24px;align-items:center;min-height:220px}.pie-chart.is-hidden{position:absolute;inset:0;visibility:hidden}.pie-visual{position:relative;width:190px;height:220px}.pie-canvas{width:190px;height:220px}.pie-total{position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;pointer-events:none}.pie-total strong{font-size:28px}.pie-total span{color:#8b949e;font-size:12px}.pie-legend{display:grid;gap:10px}.legend-item{display:grid;grid-template-columns:10px minmax(70px,1fr) auto 48px;align-items:center;gap:8px;font-size:13px}.legend-item i{width:9px;height:9px;border-radius:50%}.legend-item span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.legend-item small{color:#8b949e;text-align:right}.pie-empty{display:flex;min-height:220px;margin:0;align-items:center;justify-content:center;color:#9aa2aa}@media(max-width:700px){.pie-chart{grid-template-columns:1fr}.pie-visual{margin:auto}}
</style>
