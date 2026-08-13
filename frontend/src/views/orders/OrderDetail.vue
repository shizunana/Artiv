<template>
  <div class="order-detail" v-loading="loading">
    <template v-if="detail">
      <section class="wb-card order-summary">
        <header><div><el-button link @click="router.back()">← 返回订单管理</el-button><h2>{{ order.stallTitle || `订单 #${order.id}` }}</h2><p>订单编号 {{ order.id }} · {{ formatDateTime(order.createdAt) }}</p></div><el-tag size="large" :type="statusType(ORDER_STATUS, order.status)">{{ statusText(ORDER_STATUS, order.status) }}</el-tag></header>
        <div class="summary-grid">
          <div><small>交易金额</small><strong>¥{{ money(order.amount) }}</strong></div>
          <div><small>企划方</small><strong>{{ detail.employerName }}</strong></div>
          <div><small>画师</small><strong>{{ detail.painterName }}</strong></div>
          <div><small>当前阶段</small><strong>第 {{ (order.currentStage ?? 0) + 1 }} 阶段</strong></div>
        </div>
        <div class="action-row">
          <template v-if="isPainter && order.status === 0"><el-button type="primary" @click="accept">确认接单</el-button><el-button @click="reasonAction('reject')">拒绝接单</el-button></template>
          <el-button v-if="isEmployer && order.status === 0" type="danger" plain @click="reasonAction('cancel')">取消订单</el-button>
          <el-button v-if="[1,2].includes(order.status)" type="danger" plain @click="reasonAction('afterSale')">申请退款/售后</el-button>
          <el-button @click="goMessage">联系{{ isPainter ? '企划方' : '画师' }}</el-button>
          <el-button v-if="order.status === 3" disabled>去评价（接口预留）</el-button>
        </div>
      </section>

      <section class="wb-card phase-card">
        <h3>阶段交付</h3>
        <el-steps :active="stepActive" finish-status="success" align-center>
          <el-step v-for="(phase,index) in phases" :key="index" :title="phase.name" :description="`${phase.percent || 0}%`" />
        </el-steps>
        <div v-if="currentSubmission" class="submission-panel">
          <div class="submission-head"><b>{{ currentSubmission.name }} · 第 {{ currentSubmission.submissionNo }} 版</b><el-tag :type="stageTag(currentSubmission.status)">{{ stageText(currentSubmission.status) }}</el-tag></div>
          <p>{{ currentSubmission.content || '画师未填写交稿说明' }}</p>
          <h4>预览图</h4>
          <div class="image-grid"><el-image v-for="url in splitFiles(currentSubmission.attachments)" :key="url" :src="url" :preview-src-list="splitFiles(currentSubmission.attachments)" fit="cover" /></div>
          <template v-if="order.status === 3 && splitFiles(currentSubmission.originals).length"><h4>高清原图</h4><div class="original-list"><a v-for="url in splitFiles(currentSubmission.originals)" :key="url" :href="url" target="_blank" download>下载原图</a></div></template>
          <div v-if="isEmployer && order.status === 2" class="review-actions"><el-button type="primary" @click="review('approve')">{{ finalStage ? '确认收货' : '通过并进入下一阶段' }}</el-button><el-button @click="review('revise')">要求修改</el-button></div>
        </div>
        <el-empty v-else description="当前阶段尚未交稿" :image-size="72" />
        <el-button v-if="isPainter && order.status === 1" type="primary" @click="submitVisible = true">交付当前阶段</el-button>
      </section>

      <section class="wb-card timeline-card"><h3>订单记录</h3><el-timeline><el-timeline-item v-for="event in detail.events" :key="event.id" :timestamp="formatDateTime(event.createdAt)">{{ event.content }}</el-timeline-item></el-timeline></section>
    </template>

    <el-dialog v-model="submitVisible" title="交付当前阶段" width="600px">
      <el-input v-model="submitForm.content" type="textarea" :rows="3" maxlength="1000" placeholder="填写交稿说明" />
      <h4>预览图（企划方验收前可见）</h4><el-upload :http-request="(o) => uploadInto(o, 'previewImages')" :show-file-list="true" multiple accept="image/*"><el-button>上传预览图</el-button></el-upload>
      <h4>高清原图（最终确认后开放）</h4><el-upload :http-request="(o) => uploadInto(o, 'originalFiles')" :show-file-list="true" multiple accept="image/*"><el-button>上传高清原图</el-button></el-upload>
      <template #footer><el-button @click="submitVisible=false">取消</el-button><el-button type="primary" :loading="acting" @click="submitStage">提交交稿</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { acceptOrder, cancelOrder, getOrder, rejectOrder, requestOrderAfterSale, reviewOrderStage, submitOrderStage } from '@/api/order'
import { uploadFile } from '@/api/upload'
import { ORDER_STATUS, statusText, statusType } from '@/utils/status'
import { formatDateTime } from '@/utils/date'
import { useUserStore } from '@/stores/user'

const route=useRoute(), router=useRouter(), userStore=useUserStore()
const detail=ref(null), loading=ref(false), acting=ref(false), submitVisible=ref(false)
const submitForm=reactive({content:'',previewImages:[],originalFiles:[]})
const order=computed(()=>detail.value?.order || {})
const isPainter=computed(()=>detail.value?.viewerRole==='painter')
const isEmployer=computed(()=>detail.value?.viewerRole==='employer')
const phases=computed(()=>{try{return JSON.parse(order.value.phaseSnapshot||'[]')}catch{return []}})
const stepActive=computed(()=>order.value.status===3?phases.value.length:order.value.currentStage||0)
const submissions=computed(()=>detail.value?.submissions||[])
const currentSubmission=computed(()=>[...submissions.value].reverse().find(s=>s.stageIndex===order.value.currentStage)||[...submissions.value].reverse()[0])
const finalStage=computed(()=>(order.value.currentStage||0)>=phases.value.length-1)

async function load(){loading.value=true;try{detail.value=await getOrder(route.params.id)}finally{loading.value=false}}
async function run(action,message){acting.value=true;try{await action();ElMessage.success(message);await userStore.fetchMe();await load()}finally{acting.value=false}}
function accept(){run(()=>acceptOrder(order.value.id),'已确认接单')}
async function reasonAction(type){try{const {value}=await ElMessageBox.prompt(type==='afterSale'?'请填写退款/售后原因':'可填写原因','确认操作',{inputType:'textarea',inputValidator:type==='afterSale'?(v)=>!!v?.trim()||'请填写原因':undefined});if(type==='reject')await run(()=>rejectOrder(order.value.id,value),'已拒绝订单');if(type==='cancel')await run(()=>cancelOrder(order.value.id,value),'订单已取消');if(type==='afterSale')await run(()=>requestOrderAfterSale(order.value.id,value),'售后申请已提交')}catch{}}
async function uploadInto(options,key){try{const url=await uploadFile(options.file);submitForm[key].push(url);options.onSuccess(url)}catch(e){options.onError(e)}}
function submitStage(){run(()=>submitOrderStage(order.value.id,submitForm),'阶段稿已提交').then(()=>{submitVisible.value=false;submitForm.content='';submitForm.previewImages=[];submitForm.originalFiles=[]})}
async function review(action){let remark='';if(action==='revise'){try{const result=await ElMessageBox.prompt('请填写明确的修改意见','要求修改',{inputType:'textarea',inputValidator:(v)=>!!v?.trim()||'请填写修改意见'});remark=result.value}catch{return}}await run(()=>reviewOrderStage(order.value.id,{action,remark}),action==='approve'?(finalStage.value?'已确认收货':'阶段已通过'):'已要求修改')}
function goMessage(){const otherId=isPainter.value?order.value.employerId:order.value.painterId;const nickname=isPainter.value?detail.value.employerName:detail.value.painterName;router.push({name:isPainter.value?'PainterMessages':'DashboardMessages',query:{withUserId:otherId,nickname,orderId:order.value.id}})}
function splitFiles(value){return value?String(value).split(',').filter(Boolean):[]}
function stageText(v){return ['待确认','已通过','需修改'][v]||'-'}
function stageTag(v){return ['warning','success','danger'][v]||'info'}
function money(v){return Number(v||0).toFixed(2)}
onMounted(load)
</script>

<style scoped>
.order-detail{display:grid;gap:16px}.wb-card{background:#fff;border-radius:8px;padding:24px}.order-summary header{display:flex;justify-content:space-between;align-items:flex-start}.order-summary h2{margin:8px 0 5px}.order-summary p{margin:0;color:var(--huiyue-text-secondary)}.summary-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin:22px 0}.summary-grid div{display:grid;gap:8px;padding:14px;background:var(--huiyue-section-alt);border-radius:7px}.summary-grid small{color:var(--huiyue-text-secondary)}.action-row{display:flex;flex-wrap:wrap;gap:10px}.phase-card h3,.timeline-card h3{margin:0 0 22px}.submission-panel{margin-top:24px;padding:18px;border:1px solid var(--huiyue-border);border-radius:8px}.submission-head{display:flex;justify-content:space-between}.image-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}.image-grid :deep(.el-image){aspect-ratio:1;border-radius:6px;overflow:hidden}.original-list{display:flex;gap:12px}.original-list a{color:var(--huiyue-primary)}.review-actions{margin-top:18px}.phase-card>.el-button{margin-top:18px}@media(max-width:760px){.summary-grid{grid-template-columns:1fr 1fr}.image-grid{grid-template-columns:repeat(2,1fr)}}
</style>
