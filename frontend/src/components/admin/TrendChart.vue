<template>
  <div class="trend-chart">
    <svg viewBox="0 0 680 210" role="img" :aria-label="title">
      <line v-for="y in [30,75,120,165]" :key="y" x1="35" :y1="y" x2="660" :y2="y" class="grid"/>
      <polyline v-if="primaryPoints" :points="primaryPoints" class="line primary"/>
      <polyline v-if="secondaryPoints" :points="secondaryPoints" class="line secondary"/>
      <text x="35" y="198">{{ firstDate }}</text><text x="610" y="198">{{ lastDate }}</text>
    </svg>
    <div class="legend"><span><i class="primary-dot"/>{{ primaryLabel }}</span><span v-if="hasSecondary"><i class="secondary-dot"/>{{ secondaryLabel }}</span></div>
  </div>
</template>
<script setup>
import { computed } from 'vue'
const props=defineProps({data:{type:Array,default:()=>[]},title:{type:String,default:'趋势图'},primaryLabel:{type:String,default:'数量'},secondaryLabel:{type:String,default:'对比'}})
const hasSecondary=computed(()=>props.data.some(item=>item.secondary!==undefined));const maximum=computed(()=>Math.max(1,...props.data.flatMap(item=>[Number(item.value)||0,Number(item.secondary)||0])))
function points(field){if(!props.data.length)return '';return props.data.map((item,index)=>{const x=35+(props.data.length===1?0:index*625/(props.data.length-1));const y=165-(Number(item[field])||0)*135/maximum.value;return `${x.toFixed(1)},${y.toFixed(1)}`}).join(' ')}
const primaryPoints=computed(()=>points('value'));const secondaryPoints=computed(()=>hasSecondary.value?points('secondary'):'');const firstDate=computed(()=>props.data[0]?.date||'');const lastDate=computed(()=>props.data.at(-1)?.date||'')
</script>
<style scoped>
.trend-chart{width:100%}svg{display:block;width:100%;height:auto;background:#fbfcfd;border-radius:6px}.grid{stroke:#e8edf1;stroke-width:1}.line{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.primary{stroke:#0aa7df}.secondary{stroke:#ff9d45}text{font-size:11px;fill:#8b949e}.legend{display:flex;justify-content:center;gap:22px;margin-top:8px;color:#727c87;font-size:12px}.legend span{display:flex;align-items:center;gap:6px}.legend i{width:9px;height:9px;border-radius:50%}.primary-dot{background:#0aa7df}.secondary-dot{background:#ff9d45}
</style>
