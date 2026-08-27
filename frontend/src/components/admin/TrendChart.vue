<template>
  <div class="trend-chart" :class="{ 'has-zoom': showZoom }">
    <div
      ref="chartRef"
      class="trend-canvas"
      role="img"
      :aria-label="ariaLabel"
    ></div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { LineChart } from 'echarts/charts'
import { DataZoomComponent, GraphicComponent, GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { init, use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'

use([LineChart, DataZoomComponent, GraphicComponent, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps({
  data: { type: Array, default: () => [] },
  title: { type: String, default: '趋势图' },
  primaryLabel: { type: String, default: '数量' },
  secondaryLabel: { type: String, default: '对比' },
  granularity: { type: String, default: 'DAY' },
})

const chartRef = ref(null)
let chart
let resizeObserver

const normalized = computed(() => props.data.map((item) => ({
  startDate: item.startDate || item.date || '',
  endDate: item.endDate || item.date || '',
  label: item.label || item.date || '',
  value: Number(item.value) || 0,
  secondary: item.secondary === undefined ? undefined : Number(item.secondary) || 0,
})))
const hasSecondary = computed(() => normalized.value.some(item => item.secondary !== undefined))
const rangeDays = computed(() => {
  const first = normalized.value[0]?.startDate
  const last = normalized.value.at(-1)?.endDate
  if (!first || !last) return 0
  return Math.round((new Date(`${last}T00:00:00`) - new Date(`${first}T00:00:00`)) / 86400000) + 1
})
const showZoom = computed(() => rangeDays.value > 31)
const maximum = computed(() => Math.max(0, ...normalized.value.flatMap(item => [item.value, item.secondary ?? 0])))
const axisStep = computed(() => {
  if (maximum.value <= 0) return 1
  const target = maximum.value / 4
  const power = 10 ** Math.floor(Math.log10(target))
  const normalizedTarget = target / power
  const factor = normalizedTarget <= 1 ? 1 : normalizedTarget <= 2 ? 2 : normalizedTarget <= 5 ? 5 : 10
  return Math.max(1, factor * power)
})
const axisMaximum = computed(() => maximum.value <= 0
  ? 1
  : (Math.floor(maximum.value / axisStep.value) + 1) * axisStep.value)
const ariaLabel = computed(() => `${props.title}；横轴为时间，纵轴为数量；鼠标移入可查看具体数值`)

const formatDate = (date) => {
  if (!date) return ''
  const [year, month, day] = date.split('-')
  return `${year}年${Number(month)}月${Number(day)}日`
}

const periodLabel = (item) => item.startDate === item.endDate
  ? formatDate(item.startDate)
  : `${formatDate(item.startDate)} 至 ${formatDate(item.endDate)}`

const tooltipFormatter = (params) => {
  const index = params?.[0]?.dataIndex ?? 0
  const item = normalized.value[index]
  if (!item) return ''
  const primary = `<div class="trend-tooltip-row"><span><i style="background:#0aa7df"></i>${props.primaryLabel}</span><b>${item.value}</b></div>`
  const secondary = hasSecondary.value
    ? `<div class="trend-tooltip-row"><span><i style="background:#ff9d45"></i>${props.secondaryLabel}</span><b>${item.secondary ?? 0}</b></div>`
    : ''
  return `<div class="trend-tooltip"><strong>${periodLabel(item)}</strong>${primary}${secondary}</div>`
}

const option = computed(() => ({
  animationDuration: 360,
  color: ['#0aa7df', '#ff9d45'],
  grid: {
    left: 18,
    right: 24,
    top: 34,
    bottom: showZoom.value ? 78 : 46,
    containLabel: true,
  },
  legend: {
    show: hasSecondary.value,
    bottom: showZoom.value ? 29 : 0,
    itemWidth: 10,
    itemHeight: 10,
    itemGap: 22,
    textStyle: { color: '#727c87', fontSize: 12 },
    data: [props.primaryLabel, props.secondaryLabel],
  },
  tooltip: {
    trigger: 'axis',
    confine: true,
    enterable: false,
    backgroundColor: 'rgba(32, 43, 54, .95)',
    borderWidth: 0,
    padding: [10, 12],
    textStyle: { color: '#fff', fontSize: 12 },
    axisPointer: {
      type: 'cross',
      snap: true,
      lineStyle: { color: '#7d8a96', width: 1, type: 'dashed' },
      crossStyle: { color: '#7d8a96', width: 1, type: 'dashed' },
      label: { backgroundColor: '#52616b', color: '#fff', precision: 0 },
    },
    formatter: tooltipFormatter,
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: normalized.value.map(item => item.label),
    axisLine: { lineStyle: { color: '#dce5eb' } },
    axisTick: { show: false },
    axisLabel: {
      color: '#7e8893',
      fontSize: 11,
      hideOverlap: true,
      showMinLabel: true,
      showMaxLabel: true,
      interval: normalized.value.length <= 7 ? 0 : 'auto',
      margin: 12,
    },
  },
  yAxis: {
    type: 'value',
    name: '数量（条）',
    nameTextStyle: { color: '#7e8893', fontSize: 11, padding: [0, 0, 6, 0] },
    min: 0,
    max: axisMaximum.value,
    interval: axisStep.value,
    minInterval: 1,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#7e8893', fontSize: 11 },
    splitLine: { lineStyle: { color: '#e8edf1' } },
  },
  dataZoom: showZoom.value ? [{
    type: 'slider',
    xAxisIndex: 0,
    start: 0,
    end: 100,
    bottom: 0,
    height: 18,
    showDetail: false,
    brushSelect: false,
    borderColor: '#dfe8ee',
    backgroundColor: '#f6f9fb',
    fillerColor: 'rgba(10, 167, 223, .14)',
    handleStyle: { color: '#0aa7df', borderColor: '#0aa7df' },
    moveHandleStyle: { color: '#8fd8ef' },
    emphasis: { handleStyle: { color: '#078fc2', borderColor: '#078fc2' } },
  }] : [],
  graphic: maximum.value <= 0 ? [{
    type: 'text',
    left: 'center',
    top: '42%',
    silent: true,
    style: { text: '当前时间范围内暂无新增数据', fill: '#9aa2aa', fontSize: 13 },
  }] : [],
  series: [
    {
      name: props.primaryLabel,
      type: 'line',
      data: normalized.value.map(item => item.value),
      showSymbol: normalized.value.length === 1,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { width: 3 },
      emphasis: { focus: 'series', scale: true },
    },
    ...(hasSecondary.value ? [{
      name: props.secondaryLabel,
      type: 'line',
      data: normalized.value.map(item => item.secondary ?? 0),
      showSymbol: normalized.value.length === 1,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { width: 3 },
      emphasis: { focus: 'series', scale: true },
    }] : []),
  ],
}))

const ensureChart = () => {
  if (chart || !chartRef.value || chartRef.value.clientWidth <= 0 || chartRef.value.clientHeight <= 0) return
  chart = init(chartRef.value)
  renderChart()
}

const renderChart = () => chart?.setOption(option.value, true)

onMounted(async () => {
  await nextTick()
  if (!chartRef.value) return
  resizeObserver = new ResizeObserver(() => {
    ensureChart()
    chart?.resize()
  })
  resizeObserver.observe(chartRef.value)
  ensureChart()
})

watch(option, () => {
  ensureChart()
  renderChart()
}, { deep: true })

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
})
</script>

<style scoped>
.trend-chart{width:100%}.trend-canvas{width:100%;height:300px}.trend-chart.has-zoom .trend-canvas{height:330px}.trend-chart :deep(.trend-tooltip){display:grid;min-width:188px;gap:9px}.trend-chart :deep(.trend-tooltip>strong){padding-bottom:7px;border-bottom:1px solid rgba(255,255,255,.18);font-size:13px}.trend-chart :deep(.trend-tooltip-row){display:flex;align-items:center;justify-content:space-between;gap:28px}.trend-chart :deep(.trend-tooltip-row span){display:flex;align-items:center;gap:7px}.trend-chart :deep(.trend-tooltip-row i){width:8px;height:8px;border-radius:50%}.trend-chart :deep(.trend-tooltip-row b){font-size:14px}@media(max-width:700px){.trend-canvas{height:280px}.trend-chart.has-zoom .trend-canvas{height:310px}}
</style>
