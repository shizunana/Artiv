<template>
  <div class="order-detail" v-loading="loading">
    <template v-if="detail">
      <section class="wb-card order-summary">
        <header><div><el-button link @click="router.back()">← 返回订单管理</el-button><h2>{{ order.projectTitle || order.stallTitle || `订单 #${order.id}` }}</h2><p>订单编号 {{ order.id }} · {{ formatDateTime(order.createdAt) }}</p></div><el-tag size="large" :type="statusType(ORDER_STATUS, order.status)">{{ orderStatusText(order.status, detail.viewerRole) }}</el-tag></header>
        <div class="summary-grid">
          <div><small>交易金额</small><strong>¥{{ money(order.amount) }}</strong></div>
          <div><small>企划方</small><strong>{{ detail.employerName }}</strong></div>
          <div><small>画师</small><strong>{{ detail.painterName }}</strong></div>
          <div><small>当前阶段</small><strong>第 {{ (order.currentStage ?? 0) + 1 }} 阶段</strong></div>
        </div>
        <div class="action-row">
          <el-button v-if="order.projectId" type="primary" plain @click="goProject">查看企划快照来源</el-button>
          <el-button v-if="order.stallId" type="primary" plain @click="goStall">查看橱窗来源</el-button>
          <template v-if="isPainter && order.status === 0"><el-button type="primary" @click="accept">确认接单</el-button><el-button @click="reasonAction('reject')">拒绝接单</el-button></template>
          <el-button v-if="isEmployer && order.status === 0" type="danger" plain @click="reasonAction('cancel')">取消订单</el-button>
          <el-button v-if="[1,2].includes(order.status)" type="danger" plain @click="openDispute">申请争议仲裁</el-button>
          <el-button @click="goMessage">联系{{ isPainter ? '企划方' : '画师' }}</el-button>
          <el-button v-if="detail.reviewStatus === 'PENDING'" type="primary" @click="reviewVisible = true">去评价</el-button>
          <el-button v-else-if="detail.reviewStatus === 'SUBMITTED'" disabled>已评价</el-button>
          <el-button v-else-if="detail.reviewStatus === 'EXPIRED'" disabled>评价已过期</el-button>
          <span v-if="detail.reviewStatus === 'PENDING'" class="review-deadline">请在 {{ formatDateTime(detail.reviewDeadline) }} 前评价</span>
        </div>
      </section>

      <section v-if="dispute.dispute" class="wb-card dispute-card">
        <div class="dispute-head"><div><h3>订单争议</h3><p>{{ dispute.dispute.reason }}</p></div><el-tag :type="dispute.dispute.status===0?'warning':'success'">{{ dispute.dispute.status===0?'等待管理员仲裁':'仲裁已完成' }}</el-tag></div>
        <div v-if="dispute.evidence?.length" class="evidence-grid"><article v-for="item in dispute.evidence" :key="item.id"><el-image :src="item.image" fit="cover" :preview-src-list="dispute.evidence.map(e=>e.image)"/><span>{{ item.submittedBy===order.employerId?'企划方':'画师' }}证据</span><small>{{ item.description || '未填写说明' }}</small></article></div>
        <el-empty v-else description="尚未提交图片证据" :image-size="70"/>
        <el-alert v-if="dispute.dispute.status===1" :title="`仲裁结果：${dispute.dispute.result===1?'全额退款企划方':'全额结算画师'}；${dispute.dispute.resolutionReason}`" type="success" :closable="false"/>
        <el-button v-else-if="!hasSubmittedEvidence" type="primary" plain @click="evidenceVisible=true">补充图片证据</el-button>
      </section>

      <section v-if="order.projectId" class="wb-card source-card">
        <h3>企划下单快照</h3>
        <div class="source-layout">
          <el-image v-if="order.projectCover" class="source-cover" :src="order.projectCover" fit="cover" />
          <div>
            <p><b>企划标题：</b>{{ projectSnapshot.title || order.projectTitle }}</p>
            <p><b>应征报价：</b>¥{{ money(projectSnapshot.applicationQuote || order.amount) }}</p>
            <p><b>截稿日期：</b>{{ projectSnapshot.deadline || '待商议' }}</p>
            <p class="source-description"><b>需求说明：</b>{{ projectSnapshot.description || '未填写' }}</p>
          </div>
        </div>
      </section>

      <section class="wb-card phase-card">
        <h3>创作阶段</h3>
        <el-steps :active="stepActive" finish-status="success" align-center>
          <el-step v-for="(phase,index) in phases" :key="index" :title="phase.name" />
        </el-steps>
        <div v-if="currentSubmission" class="submission-panel">
          <div class="submission-head"><b>{{ currentSubmission.name }} · 第 {{ currentSubmission.submissionNo }} 版</b><el-tag :type="stageTag(currentSubmission.status)">{{ stageText(currentSubmission.status) }}</el-tag></div>
          <p>{{ currentSubmission.content || '画师未填写交稿说明' }}</p>
          <div v-if="currentSubmission.status === 2 && currentSubmission.reviewRemark" class="revision-note"><b>企划方修改意见</b><p>{{ currentSubmission.reviewRemark }}</p></div>
          <h4>水印预览图</h4>
          <div class="image-grid"><el-image v-for="url in splitFiles(currentSubmission.attachments)" :key="url" :src="url" :preview-src-list="splitFiles(currentSubmission.attachments)" preview-teleported fit="cover" /></div>
          <template v-if="order.status === 3 && splitFiles(currentSubmission.originals).length"><h4>高清原图</h4><div class="original-list"><a v-for="url in splitFiles(currentSubmission.originals)" :key="url" :href="url" target="_blank" download>下载原图</a></div></template>
          <div v-if="isEmployer && order.status === 2" class="review-actions">
            <el-button v-if="!finalStage" type="primary" @click="review('approve')">通过并进入下一阶段</el-button>
            <el-button type="success" @click="review('complete')">确认当前交付并完成交易</el-button>
            <el-button @click="review('revise')">要求修改</el-button>
          </div>
        </div>
        <el-empty v-else description="当前阶段尚未交稿" :image-size="72" />
        <el-button v-if="isPainter && order.status === 1" type="primary" @click="submitVisible = true">交付当前阶段</el-button>
      </section>

      <section class="wb-card timeline-card"><h3>订单记录</h3><el-timeline><el-timeline-item v-for="event in detail.events" :key="event.id" :timestamp="formatDateTime(event.createdAt)">{{ event.content }}</el-timeline-item></el-timeline></section>
    </template>

    <el-dialog v-model="submitVisible" title="交付当前阶段" width="600px">
      <el-input v-model="submitForm.content" type="textarea" :rows="3" maxlength="1000" placeholder="填写交稿说明" />
      <h4>本阶段交付图</h4>
      <p class="upload-tip">只需上传一张原图。系统会自动生成低分辨率水印预览供企划方验收；确认完成交易后才开放原图。</p>
      <el-upload :http-request="uploadDelivery" :limit="1" :on-remove="clearDelivery" :show-file-list="true" accept="image/png,image/jpeg,image/gif,image/webp"><el-button>选择交付图片</el-button></el-upload>
      <template #footer><el-button @click="submitVisible=false">取消</el-button><el-button type="primary" :loading="acting" @click="submitStage">提交交稿</el-button></template>
    </el-dialog>

    <el-dialog v-model="disputeVisible" title="申请争议仲裁" width="600px">
      <el-alert title="仲裁期间订单暂停。管理员只会看到订单记录、阶段稿和双方主动上传的证据，不会查看无关聊天。" type="warning" :closable="false" show-icon/>
      <el-input v-model="disputeForm.reason" class="dispute-reason" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="请完整说明争议原因"/>
      <el-input v-model="disputeForm.description" placeholder="图片证据说明（选填）"/>
      <el-upload :http-request="uploadDisputeImage" :limit="9" :on-remove="removeDisputeImage" multiple accept="image/png,image/jpeg,image/gif,image/webp"><el-button>上传图片证据（最多9张）</el-button></el-upload>
      <template #footer><el-button @click="disputeVisible=false">取消</el-button><el-button type="danger" :loading="acting" @click="submitDispute">提交仲裁申请</el-button></template>
    </el-dialog>

    <el-dialog v-model="evidenceVisible" title="补充争议证据" width="600px">
      <el-input v-model="evidenceForm.description" placeholder="证据说明（选填）"/>
      <el-upload :http-request="uploadEvidenceImage" :limit="9" :on-remove="removeEvidenceImage" multiple accept="image/png,image/jpeg,image/gif,image/webp"><el-button>上传图片证据（至少1张）</el-button></el-upload>
      <template #footer><el-button @click="evidenceVisible=false">取消</el-button><el-button type="primary" :loading="acting" @click="submitEvidence">提交证据</el-button></template>
    </el-dialog>
    <ReviewDialog v-model="reviewVisible" :order-id="order.id" :order-title="order.projectTitle || order.stallTitle" @submitted="load" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { acceptOrder, addDisputeEvidence, cancelOrder, createOrderDispute, getCurrentOrderDispute, getOrder, rejectOrder, reviewOrderStage, submitOrderStage } from '@/api/order'
import { uploadFile } from '@/api/upload'
import { ORDER_STATUS, orderStatusText, statusType } from '@/utils/status'
import { formatDateTime } from '@/utils/date'
import { useUserStore } from '@/stores/user'
import ReviewDialog from '@/components/reviews/ReviewDialog.vue'

const route=useRoute(), router=useRouter(), userStore=useUserStore()
const detail=ref(null), loading=ref(false), acting=ref(false), submitVisible=ref(false)
const reviewVisible=ref(false)
const submitForm=reactive({content:'',image:''})
const dispute=ref({}), disputeVisible=ref(false), evidenceVisible=ref(false)
const disputeForm=reactive({reason:'',description:'',images:[]}), evidenceForm=reactive({description:'',images:[]})
const order=computed(()=>detail.value?.order || {})
const isPainter=computed(()=>detail.value?.viewerRole==='painter')
const isEmployer=computed(()=>detail.value?.viewerRole==='employer')
const phases=computed(()=>{try{return JSON.parse(order.value.phaseSnapshot||'[]')}catch{return []}})
const stepActive=computed(()=>order.value.status===3?phases.value.length:order.value.currentStage||0)
const submissions=computed(()=>detail.value?.submissions||[])
const currentSubmission=computed(()=>[...submissions.value].reverse().find(s=>s.stageIndex===order.value.currentStage)||[...submissions.value].reverse()[0])
const finalStage=computed(()=>(order.value.currentStage||0)>=phases.value.length-1)
const projectSnapshot=computed(()=>{try{return JSON.parse(order.value.projectSnapshot||'{}')}catch{return {}}})
const hasSubmittedEvidence=computed(()=>(dispute.value.evidence||[]).some(item=>item.submittedBy===userStore.user?.id))

async function load(){loading.value=true;try{detail.value=await getOrder(route.params.id);dispute.value=await getCurrentOrderDispute(route.params.id)}finally{loading.value=false}}
async function run(action,message){acting.value=true;try{await action();ElMessage.success(message);await userStore.fetchMe();await load()}finally{acting.value=false}}
function accept(){run(()=>acceptOrder(order.value.id),'已确认接单')}
async function reasonAction(type){try{const {value}=await ElMessageBox.prompt('可填写原因','确认操作',{inputType:'textarea'});if(type==='reject')await run(()=>rejectOrder(order.value.id,value),'已拒绝订单');if(type==='cancel')await run(()=>cancelOrder(order.value.id,value),'订单已取消')}catch{}}
function openDispute(){disputeForm.reason='';disputeForm.description='';disputeForm.images=[];disputeVisible.value=true}
async function uploadDisputeImage(options){try{const url=await uploadFile(options.file);disputeForm.images.push(url);options.onSuccess(url)}catch(e){options.onError(e)}}
function removeDisputeImage(file){const url=file.response||file.url;disputeForm.images=disputeForm.images.filter(item=>item!==url)}
async function uploadEvidenceImage(options){try{const url=await uploadFile(options.file);evidenceForm.images.push(url);options.onSuccess(url)}catch(e){options.onError(e)}}
function removeEvidenceImage(file){const url=file.response||file.url;evidenceForm.images=evidenceForm.images.filter(item=>item!==url)}
async function submitDispute(){if(!disputeForm.reason.trim()){ElMessage.warning('请填写争议原因');return}await run(()=>createOrderDispute(order.value.id,{...disputeForm}),'仲裁申请已提交');disputeVisible.value=false}
async function submitEvidence(){if(!evidenceForm.images.length){ElMessage.warning('请至少上传一张图片证据');return}await run(()=>addDisputeEvidence(dispute.value.dispute.id,{...evidenceForm}),'证据已提交');evidenceVisible.value=false;evidenceForm.description='';evidenceForm.images=[]}
async function uploadDelivery(options){try{submitForm.image=await uploadFile(options.file);options.onSuccess(submitForm.image)}catch(e){options.onError(e)}}
function clearDelivery(){submitForm.image=''}
async function submitStage(){if(!submitForm.image){ElMessage.warning('请先上传本阶段交付图片');return}await run(()=>submitOrderStage(order.value.id,submitForm),'阶段稿已提交');submitVisible.value=false;submitForm.content='';submitForm.image=''}
async function review(action){let remark='';if(action==='revise'){try{const result=await ElMessageBox.prompt('请填写明确的修改意见','要求修改',{inputType:'textarea',inputValidator:(v)=>!!v?.trim()||'请填写修改意见'});remark=result.value}catch{return}}if(action==='complete'){try{await ElMessageBox.confirm('确认后将立即完成交易、把款项结算给画师，并开放当前交付原图。未执行的后续阶段会被跳过。','确认完成交易',{type:'warning',confirmButtonText:'确认收货并完成'})}catch{return}}await run(()=>reviewOrderStage(order.value.id,{action,remark}),action==='complete'?'交易已完成并结算':action==='approve'?'阶段已通过':'已要求修改')}
function goMessage(){const otherId=isPainter.value?order.value.employerId:order.value.painterId;const nickname=isPainter.value?detail.value.employerName:detail.value.painterName;router.push({name:'Messages',query:{withUserId:otherId,nickname,orderId:order.value.id}})}
function goProject(){router.push({name:'ProjectDetail',params:{id:order.value.projectId},query:{from:'order'}})}
function goStall(){router.push({name:'StallDetail',params:{id:order.value.stallId},query:{from:'order'}})}
function splitFiles(value){return value?String(value).split(',').filter(Boolean):[]}
function stageText(v){return ['待确认','已通过','需修改'][v]||'-'}
function stageTag(v){return ['warning','success','danger'][v]||'info'}
function money(v){return Number(v||0).toFixed(2)}
onMounted(load)
</script>

<style scoped>
.order-detail{display:grid;gap:16px}.wb-card{background:#fff;border-radius:8px;padding:24px}.order-summary header{display:flex;justify-content:space-between;align-items:flex-start}.order-summary h2{margin:8px 0 5px}.order-summary p{margin:0;color:var(--artiv-text-secondary)}.summary-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin:22px 0}.summary-grid div{display:grid;gap:8px;padding:14px;background:var(--artiv-section-alt);border-radius:7px}.summary-grid small{color:var(--artiv-text-secondary)}.action-row{display:flex;flex-wrap:wrap;gap:10px}.source-card h3,.phase-card h3,.timeline-card h3{margin:0 0 22px}.source-layout{display:grid;grid-template-columns:140px minmax(0,1fr);gap:20px}.source-cover{width:140px;height:110px;border-radius:8px;background:var(--artiv-section-alt)}.source-layout p{margin:0 0 10px;color:var(--artiv-text-secondary)}.source-description{white-space:pre-wrap}.submission-panel{margin-top:24px;padding:18px;border:1px solid var(--artiv-border);border-radius:8px}.submission-head{display:flex;justify-content:space-between}.revision-note{margin:14px 0;padding:12px 14px;border-left:3px solid #f56c6c;border-radius:4px;background:#fff4f4;color:#a83d3d}.revision-note p{margin:6px 0 0;white-space:pre-wrap}.upload-tip{color:var(--artiv-text-secondary);line-height:1.65}.image-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}.image-grid :deep(.el-image){aspect-ratio:1;border-radius:6px;overflow:hidden}.original-list{display:flex;gap:12px}.original-list a{color:var(--artiv-primary)}.review-actions{display:flex;flex-wrap:wrap;gap:10px;margin-top:18px}.phase-card>.el-button{margin-top:18px}@media(max-width:760px){.summary-grid{grid-template-columns:1fr 1fr}.image-grid{grid-template-columns:repeat(2,1fr)}.source-layout{grid-template-columns:1fr}}
.dispute-head{display:flex;justify-content:space-between;align-items:flex-start}.dispute-head h3{margin:0}.dispute-head p{color:#a65c21}.evidence-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin:18px 0}.evidence-grid article{display:flex;flex-direction:column;gap:5px}.evidence-grid .el-image{width:100%;aspect-ratio:1;border-radius:6px}.evidence-grid small{color:#89929c}.dispute-reason{margin:16px 0 10px}
.review-deadline{align-self:center;color:var(--artiv-text-secondary);font-size:12px}
</style>
