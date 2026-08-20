<template>
  <div class="admin-page">
    <header class="page-head"><div><h2>数据统计</h2><p>默认关注用户需求，同时保留平台基础运营指标</p></div><div class="date-tools"><el-button-group><el-button @click="preset(7)">近7天</el-button><el-button @click="preset(30)">近30天</el-button></el-button-group><el-date-picker v-model="dates" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期"/><el-button type="primary" @click="load">应用</el-button></div></header>
    <el-tabs v-model="active" class="stats-tabs" @tab-change="load">
      <el-tab-pane label="用户需求分析" name="demand">
        <div v-loading="loading" class="content-grid">
          <section class="metric-grid five span-2"><article v-for="item in demandBaseMetrics" :key="item.label"><strong>{{ item.value }}</strong><span>{{ item.label }}</span></article></section>
          <section class="metric-grid five span-2 demand-summary"><article v-for="item in demandMetrics" :key="item.label"><strong>{{ item.value }}</strong><span>{{ item.label }}</span></article></section>
          <section class="wb-card span-2"><h3>企划发布与应征趋势</h3><TrendChart :data="data.trend || []" primary-label="发布企划" secondary-label="收到应征"/></section>
          <section class="wb-card"><h3>稿件类型需求</h3><BarList :items="data.categories || []"/></section>
          <section class="wb-card"><h3>题材风格 Top 10</h3><BarList :items="data.subjects || []"/></section>
          <section class="wb-card"><h3>预算区间</h3><BarList :items="data.budgetBuckets || []"/></section>
          <section class="wb-card"><h3>交付周期</h3><BarList :items="data.deadlineBuckets || []"/></section>
          <section class="wb-card span-2"><h3>需求转化漏斗</h3><div class="funnel"><div v-for="(item,index) in data.funnel || []" :key="item.name" :style="{width:`${100-index*14}%`}"><span>{{ item.name }}</span><strong>{{ item.value }}</strong></div></div></section>
          <section class="wb-card span-2"><h3>供需缺口</h3><el-table :data="data.supplyDemandGap || []"><el-table-column prop="name" label="类型"/><el-table-column prop="demand" label="需求企划"/><el-table-column prop="activeStalls" label="可售橱窗"/><el-table-column prop="certifiedPainters" label="有公开作品的认证画师"/><el-table-column label="零应征率"><template #default="{row}">{{ row.zeroApplicationRate }}%</template></el-table-column><el-table-column label="判断"><template #default="{row}"><el-tag :type="row.demand>row.activeStalls?'warning':'success'">{{ row.demand>row.activeStalls?'需求偏高':'供给充足' }}</el-tag></template></el-table-column></el-table></section>
        </div>
      </el-tab-pane>
      <el-tab-pane label="平台运营" name="operations">
        <div v-loading="loading" class="content-grid">
          <section class="metric-grid five span-2"><article v-for="item in operationMetrics" :key="item.label"><strong>{{ item.value }}</strong><span>{{ item.label }}</span></article></section>
          <section class="wb-card span-2"><h3>新增用户趋势</h3><TrendChart :data="data.userTrend || []" primary-label="新增用户"/></section>
          <section class="wb-card span-2"><h3>订单趋势</h3><TrendChart :data="data.orderTrend || []" primary-label="新增订单" secondary-label="完成订单"/></section>
          <section class="wb-card"><h3>用户角色</h3><BarList :items="mapToItems(data.userRoles)"/></section>
          <section class="wb-card"><h3>认证状态</h3><BarList :items="mapToItems(data.certificationStatus)"/></section>
          <section class="wb-card"><h3>订单状态</h3><BarList :items="mapToItems(data.orderStatus)"/></section>
          <section class="wb-card"><h3>订单来源</h3><BarList :items="mapToItems(data.orderSource)"/></section>
          <section class="wb-card span-2"><h3>成交类型 Top 5</h3><el-table :data="data.topCategories || []"><el-table-column prop="name" label="类型"/><el-table-column prop="orders" label="完成订单"/><el-table-column label="成交额"><template #default="{row}">¥{{ row.gmv }}</template></el-table-column></el-table></section>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, ref } from 'vue'
import { getAdminDemandStats, getAdminStats } from '@/api/admin'
import TrendChart from '@/components/admin/TrendChart.vue'
const active=ref('demand');const loading=ref(false);const data=ref({});const today=new Date();const iso=d=>{const copy=new Date(d.getTime()-d.getTimezoneOffset()*60000);return copy.toISOString().slice(0,10)};const dates=ref([])
function preset(days){const start=new Date();start.setDate(today.getDate()-days+1);dates.value=[iso(start),iso(today)];load()}
const params=()=>dates.value?.length?{startDate:dates.value[0],endDate:dates.value[1]}:{}
async function load(){loading.value=true;try{data.value=active.value==='demand'?await getAdminDemandStats(params()):await getAdminStats(params())}finally{loading.value=false}}
const value=(v,suffix='')=>(v??0)+suffix;const demandMetrics=computed(()=>{const s=data.value.summary||{};return[{label:'新增企划数',value:value(s.publishedProjects)},{label:'新增订单数',value:value(s.projectsWithOrders)},{label:'零应征企划',value:value(s.zeroApplicationProjects)},{label:'平均应征数',value:value(s.averageApplications)},{label:'平均首次应征',value:s.averageFirstResponseHours==null?'-':value(s.averageFirstResponseHours,' 小时')}]})
const demandBaseMetrics=computed(()=>{const m=data.value.topMetrics||{};return[{label:'总用户数',value:value(m.totalUsers)},{label:'新增用户数',value:value(m.newUsers)},{label:'新增订单数',value:value(m.newOrders)},{label:'完成订单数',value:value(m.completedOrders)},{label:'成交总额',value:`¥${m.gmv??0}`}]})
const operationMetrics=computed(()=>{const m=data.value.topMetrics||{};return[{label:'总用户数',value:value(m.totalUsers)},{label:'新增用户数',value:value(m.newUsers)},{label:'新增订单数',value:value(m.newOrders)},{label:'完成订单数',value:value(m.completedOrders)},{label:'成交总额',value:`¥${m.gmv??0}`}]})
const mapToItems=source=>Object.entries(source||{}).map(([name,value])=>({name,value}))
const BarList=defineComponent({props:{items:{type:Array,default:()=>[]}},setup(props){return()=>h('div',{class:'bar-list'},props.items.length?props.items.map(item=>{const max=Math.max(1,...props.items.map(i=>Number(i.value)||0));return h('div',{class:'bar-row'},[h('div',{class:'bar-label'},[h('span',item.name),h('strong',String(item.value))]),h('div',{class:'bar-track'},h('i',{style:{width:`${(Number(item.value)||0)*100/max}%`}}))])}):[h('p',{class:'empty'},'暂无数据')])}})
onMounted(()=>preset(30))
</script>

<style scoped>
.admin-page{display:grid;gap:16px}.page-head{display:flex;align-items:center;justify-content:space-between;gap:18px;background:#fff;border-radius:6px;padding:22px 24px}.page-head h2{margin:0}.page-head p{margin:7px 0 0;color:#8a929c}.date-tools{display:flex;align-items:center;gap:8px}.stats-tabs{background:#fff;border-radius:6px;padding:0 22px 22px}.content-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}.span-2{grid-column:span 2}.wb-card,.metric-grid article{background:#fff;border:1px solid #e8edf1;border-radius:6px;padding:20px}.wb-card h3{margin:0 0 16px}.metric-grid{display:grid;gap:12px}.metric-grid.five{grid-template-columns:repeat(5,1fr)}.metric-grid article{display:flex;flex-direction:column;gap:7px;border-top:3px solid var(--artiv-primary)}.metric-grid strong{font-size:24px}.metric-grid span{color:#7e8893;font-size:13px}.funnel{display:flex;flex-direction:column;align-items:center;gap:5px}.funnel div{display:flex;justify-content:space-between;padding:12px 18px;color:#fff;background:linear-gradient(90deg,#0aa7df,#64c9e8);clip-path:polygon(4% 0,96% 0,100% 100%,0 100%)}.bar-list{display:grid;gap:13px}.bar-label{display:flex;justify-content:space-between;margin-bottom:5px;font-size:13px}.bar-track{height:8px;background:#edf2f5;border-radius:5px;overflow:hidden}.bar-track i{display:block;height:100%;background:var(--artiv-primary);border-radius:5px}.empty{color:#9aa2aa;text-align:center}@media(max-width:1000px){.page-head{align-items:flex-start;flex-direction:column}.date-tools{flex-wrap:wrap}.content-grid{grid-template-columns:1fr}.span-2{grid-column:auto}.metric-grid.five{grid-template-columns:repeat(2,1fr)}}
.demand-summary article{border-top-color:#ff9d45}
</style>
