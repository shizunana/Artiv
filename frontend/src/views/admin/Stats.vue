<template>
  <div class="admin-page">
    <header class="page-head"><div><h2>数据统计</h2><p>默认关注用户需求，同时保留平台基础运营指标</p></div><div class="date-tools"><el-button-group><el-button @click="preset(7)">近7天</el-button><el-button @click="preset(30)">近30天</el-button></el-button-group><el-date-picker v-model="dates" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期"/><el-button type="primary" @click="load">应用</el-button></div></header>
    <el-tabs v-model="active" class="stats-tabs" @tab-change="load">
      <el-tab-pane label="用户需求分析" name="demand">
        <div v-loading="loading" class="content-grid">
          <section class="metric-grid five span-2"><article v-for="item in demandBaseMetrics" :key="item.label"><strong>{{ item.value }}</strong><span>{{ item.label }}</span></article></section>
          <section class="metric-grid five span-2 demand-summary"><article v-for="item in demandMetrics" :key="item.label"><strong>{{ item.value }}</strong><span>{{ item.label }}</span></article></section>
          <section class="wb-card span-2"><h3>企划发布与应征趋势</h3><TrendChart :data="data.trend || []" primary-label="发布企划" secondary-label="收到应征"/></section>
          <section class="wb-card"><h3>企划类型分布</h3><PieChart title="企划类型分布" :items="projectPieItems"/></section>
          <section class="wb-card"><h3>在售橱窗类型分布</h3><PieChart title="在售橱窗类型分布" :items="stallPieItems"/></section>
          <section class="wb-card span-2 price-distribution-card">
            <div class="section-title price-distribution-head">
              <div><h3>完成订单成交价格分层</h3><p>按已完成企划订单的实际成交价格，动态划分 10% / 20% / 40% / 20% / 10% 价格层级。</p></div>
              <el-select v-model="priceCategory" class="category-select" aria-label="选择企划类型" placeholder="全部类型" @change="loadPriceDistribution">
                <el-option label="全部类型" value=""/>
                <el-option v-for="item in CATEGORY_OPTIONS" :key="item" :label="item" :value="item"/>
              </el-select>
            </div>
            <div v-loading="priceLoading" class="price-distribution-body">
              <template v-if="priceData.sufficient">
                <div class="distribution-chart" aria-label="完成订单成交价格五档分层图">
                  <article v-for="item in priceData.buckets" :key="item.key" class="distribution-column">
                    <strong>{{ item.targetRate }}%</strong>
                    <el-tooltip :content="bucketTooltip(item)" placement="top">
                      <div class="distribution-bar" :style="{height: barHeight(item.targetRate)}"><span>{{ item.orderCount }} 笔</span></div>
                    </el-tooltip>
                    <b>{{ item.name }}</b>
                    <small>{{ item.rangeLabel }}</small>
                  </article>
                </div>
              </template>
              <el-empty v-else :image-size="78" :description="priceEmptyText"/>
              <div v-if="Number(priceData.sampleSize) > 0" class="distribution-summary">
                <span>样本 <b>{{ priceData.sampleSize }}</b> 笔</span>
                <span>中位成交价 <b>¥{{ formatMoney(priceData.medianAmount) }}</b></span>
                <span>平均成交价 <b>¥{{ formatMoney(priceData.averageAmount) }}</b></span>
                <span>价格范围 <b>¥{{ formatMoney(priceData.minimumAmount) }}–¥{{ formatMoney(priceData.maximumAmount) }}</b></span>
              </div>
            </div>
          </section>
          <section class="wb-card span-2 conversion-card"><div class="section-title"><div><h3>需求转化路径</h3><p>按企划去重，展示各阶段数量及相邻阶段转化率</p></div></div><div class="conversion-flow"><template v-for="(item,index) in funnelStages" :key="item.name"><article class="conversion-stage"><span class="stage-index">{{ index + 1 }}</span><strong>{{ item.value }}</strong><b>{{ item.name }}</b><small>占发布企划 {{ item.totalRate }}%</small></article><div v-if="item.nextRate !== null" class="conversion-link"><span>{{ item.nextRate }}%</span><i>→</i><small>转化</small></div></template></div></section>
          <section class="wb-card span-2"><h3>供需缺口</h3><el-table :data="data.supplyDemandGap || []"><el-table-column prop="name" label="类型"/><el-table-column prop="demand" label="需求企划"/><el-table-column prop="activeStalls" label="可售橱窗"/><el-table-column prop="certifiedPainters" label="有公开作品的认证画师"/><el-table-column label="零应征率"><template #default="{row}">{{ row.zeroApplicationRate }}%</template></el-table-column><el-table-column label="判断"><template #default="{row}"><el-tag :type="row.demand>row.activeStalls?'warning':'success'">{{ row.demand>row.activeStalls?'需求偏高':'供给充足' }}</el-tag></template></el-table-column></el-table></section>
        </div>
      </el-tab-pane>
      <el-tab-pane label="平台运营" name="operations">
        <div v-loading="loading" class="content-grid">
          <section class="metric-grid five span-2"><article v-for="item in operationMetrics" :key="item.label"><strong>{{ item.value }}</strong><span>{{ item.label }}</span></article></section>
          <section class="wb-card span-2"><h3>新增用户趋势</h3><TrendChart :data="data.userTrend || []" primary-label="新增用户"/></section>
          <section class="wb-card span-2"><h3>订单趋势</h3><TrendChart :data="data.orderTrend || []" primary-label="新增订单" secondary-label="完成订单"/></section>
          <section class="wb-card"><h3>注册主身份</h3><BarList :items="mapToItems(data.userRoles)"/></section>
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
import { getAdminDemandStats, getAdminPriceDistribution, getAdminStats } from '@/api/admin'
import PieChart from '@/components/admin/PieChart.vue'
import TrendChart from '@/components/admin/TrendChart.vue'
import { CATEGORY_OPTIONS } from '@/constants/subjects'
const active=ref('demand');const loading=ref(false);const data=ref({});const priceLoading=ref(false);const priceData=ref({});const priceCategory=ref('');const today=new Date();const iso=d=>{const copy=new Date(d.getTime()-d.getTimezoneOffset()*60000);return copy.toISOString().slice(0,10)};const dates=ref([])
function preset(days){const start=new Date();start.setDate(today.getDate()-days+1);dates.value=[iso(start),iso(today)];load()}
const params=()=>dates.value?.length?{startDate:dates.value[0],endDate:dates.value[1]}:{}
async function loadPriceDistribution(){priceLoading.value=true;try{priceData.value=await getAdminPriceDistribution({...params(),...(priceCategory.value?{category:priceCategory.value}:{})})}finally{priceLoading.value=false}}
async function load(){loading.value=true;try{if(active.value==='demand'){const [demand,distribution]=await Promise.all([getAdminDemandStats(params()),getAdminPriceDistribution({...params(),...(priceCategory.value?{category:priceCategory.value}:{})})]);data.value=demand;priceData.value=distribution}else{data.value=await getAdminStats(params())}}finally{loading.value=false}}
const value=(v,suffix='')=>(v??0)+suffix;const demandMetrics=computed(()=>{const s=data.value.summary||{};return[{label:'新增企划数',value:value(s.publishedProjects)},{label:'新增订单数',value:value(s.projectsWithOrders)},{label:'零应征企划',value:value(s.zeroApplicationProjects)},{label:'平均应征数',value:value(s.averageApplications)},{label:'平均首次应征',value:s.averageFirstResponseHours==null?'-':value(s.averageFirstResponseHours,' 小时')}]})
const demandBaseMetrics=computed(()=>{const m=data.value.topMetrics||{};return[{label:'总用户数',value:value(m.totalUsers)},{label:'新增用户数',value:value(m.newUsers)},{label:'新增订单数',value:value(m.newOrders)},{label:'完成订单数',value:value(m.completedOrders)},{label:'成交总额',value:`¥${m.gmv??0}`}]})
const operationMetrics=computed(()=>{const m=data.value.topMetrics||{};return[{label:'总用户数',value:value(m.totalUsers)},{label:'新增用户数',value:value(m.newUsers)},{label:'新增订单数',value:value(m.newOrders)},{label:'完成订单数',value:value(m.completedOrders)},{label:'成交总额',value:`¥${m.gmv??0}`}]})
const projectPieItems=computed(()=>data.value.categories||[])
const stallPieItems=computed(()=>(data.value.supplyDemandGap||[]).filter(item=>Number(item.activeStalls)>0).map(item=>({name:item.name,value:item.activeStalls})))
const funnelStages=computed(()=>{const items=data.value.funnel||[];const first=Number(items[0]?.value)||0;return items.map((item,index)=>{const current=Number(item.value)||0;const next=index<items.length-1?Number(items[index+1].value)||0:null;return{...item,value:current,totalRate:first?Math.round(current*1000/first)/10:0,nextRate:next===null?null:(current?Math.round(next*1000/current)/10:0)}})})
const mapToItems=source=>Object.entries(source||{}).map(([name,value])=>({name,value}))
const priceEmptyText=computed(()=>({NO_DATA:'当前条件下暂无已完成企划订单',INSUFFICIENT_SAMPLE:'完成订单样本不足，至少需要 10 笔',PRICE_CONCENTRATED:'成交价格过于集中，暂时无法形成五档价格区间'}[priceData.value.reason]||'暂无成交价格数据'))
const formatMoney=amount=>{const number=Number(amount);return Number.isFinite(number)?number.toLocaleString('zh-CN',{minimumFractionDigits:Number.isInteger(number)?0:2,maximumFractionDigits:2}):'-'}
const barHeight=rate=>`${Math.max(42,Number(rate||0)*4.2)}px`
const bucketTooltip=item=>`${item.name}｜${item.rangeLabel}｜${item.orderCount} 笔（实际 ${item.actualRate}%）｜成交额 ¥${formatMoney(item.gmv)}`
const BarList=defineComponent({props:{items:{type:Array,default:()=>[]}},setup(props){return()=>h('div',{class:'bar-list'},props.items.length?props.items.map(item=>{const max=Math.max(1,...props.items.map(i=>Number(i.value)||0));return h('div',{class:'bar-row'},[h('div',{class:'bar-label'},[h('span',item.name),h('strong',String(item.value))]),h('div',{class:'bar-track'},h('i',{style:{width:`${(Number(item.value)||0)*100/max}%`}}))])}):[h('p',{class:'empty'},'暂无数据')])}})
onMounted(()=>preset(7))
</script>

<style scoped>
.admin-page{display:grid;gap:16px}.page-head{display:flex;align-items:center;justify-content:space-between;gap:18px;background:#fff;border-radius:6px;padding:22px 24px}.page-head h2{margin:0}.page-head p{margin:7px 0 0;color:#8a929c}.date-tools{display:flex;align-items:center;gap:8px}.stats-tabs{background:#fff;border-radius:6px;padding:0 22px 22px}.content-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}.span-2{grid-column:span 2}.wb-card,.metric-grid article{background:#fff;border:1px solid #e8edf1;border-radius:6px;padding:20px}.wb-card h3{margin:0 0 16px}.section-title h3{margin-bottom:5px}.section-title p{margin:0;color:#8a929c;font-size:13px}.metric-grid{display:grid;gap:12px}.metric-grid.five{grid-template-columns:repeat(5,1fr)}.metric-grid article{display:flex;flex-direction:column;gap:7px;border-top:3px solid var(--artiv-primary)}.metric-grid strong{font-size:24px}.metric-grid span{color:#7e8893;font-size:13px}.conversion-flow{display:flex;align-items:stretch;gap:12px;margin-top:22px}.conversion-stage{position:relative;display:flex;min-width:0;flex:1;flex-direction:column;align-items:center;justify-content:center;gap:5px;min-height:140px;padding:18px;border:1px solid #dfe9ef;border-top:4px solid var(--artiv-primary);border-radius:8px;background:linear-gradient(180deg,#f4fbfe,#fff)}.conversion-stage strong{font-size:30px}.conversion-stage b{font-size:14px}.conversion-stage small{color:#8a929c}.stage-index{position:absolute;top:10px;left:12px;display:grid;width:22px;height:22px;place-items:center;border-radius:50%;background:var(--artiv-primary);color:#fff;font-size:12px}.conversion-link{display:flex;width:62px;flex:0 0 62px;flex-direction:column;align-items:center;justify-content:center;color:var(--artiv-primary)}.conversion-link span{font-weight:700}.conversion-link i{font-size:28px;font-style:normal;line-height:1}.conversion-link small{color:#8a929c}.bar-list{display:grid;gap:13px}.bar-label{display:flex;justify-content:space-between;margin-bottom:5px;font-size:13px}.bar-track{height:8px;background:#edf2f5;border-radius:5px;overflow:hidden}.bar-track i{display:block;height:100%;background:var(--artiv-primary);border-radius:5px}.empty{color:#9aa2aa;text-align:center}@media(max-width:1000px){.page-head{align-items:flex-start;flex-direction:column}.date-tools{flex-wrap:wrap}.content-grid{grid-template-columns:1fr}.span-2{grid-column:auto}.metric-grid.five{grid-template-columns:repeat(2,1fr)}.conversion-flow{display:grid;grid-template-columns:1fr}.conversion-link{width:auto;min-height:55px;transform:rotate(90deg);justify-self:center}.conversion-card{overflow:hidden}}
.demand-summary article{border-top-color:#ff9d45}
.price-distribution-card{min-height:330px}.price-distribution-head{display:flex;align-items:flex-start;justify-content:space-between;gap:20px}.price-distribution-head h3{margin-bottom:5px}.category-select{width:150px;flex:0 0 150px}.price-distribution-body{min-height:240px}.distribution-chart{display:flex;align-items:flex-end;justify-content:center;gap:clamp(12px,3vw,42px);min-height:225px;padding:24px 12px 0;border-bottom:1px solid #dfe8ee}.distribution-column{display:flex;width:130px;min-width:0;flex-direction:column;align-items:center;gap:7px;text-align:center}.distribution-column>strong{color:#7e8893;font-size:14px}.distribution-bar{display:flex;width:64px;align-items:flex-start;justify-content:center;padding-top:8px;border-radius:8px 8px 0 0;background:linear-gradient(180deg,#45baf0,var(--artiv-primary));box-shadow:0 8px 20px rgba(0,169,230,.13);color:#fff;cursor:default;transition:filter .2s,transform .2s}.distribution-bar:hover{filter:saturate(1.18);transform:translateY(-2px)}.distribution-bar span{font-size:12px;font-weight:700}.distribution-column b{font-size:13px}.distribution-column small{min-height:34px;color:#687583;font-size:12px;line-height:1.4}.distribution-summary{display:flex;justify-content:center;gap:30px;flex-wrap:wrap;padding:18px 12px 0;color:#7e8893;font-size:13px}.distribution-summary b{color:#273340}.price-distribution-body :deep(.el-empty){padding:32px 0 12px}.price-distribution-body :deep(.el-empty__description){margin-top:6px}.price-distribution-body :deep(.el-empty__description p){color:#7e8893}@media(max-width:760px){.price-distribution-head{align-items:stretch;flex-direction:column}.category-select{width:100%;flex-basis:auto}.price-distribution-body{overflow-x:auto}.distribution-chart{width:680px;justify-content:flex-start}.distribution-summary{width:680px;justify-content:flex-start}}
</style>
