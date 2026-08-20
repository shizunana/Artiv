<template>
  <div class="admin-page">
    <header class="page-head"><div><h2>订单争议仲裁</h2><p>仅查看订单、阶段时间线与双方主动提交的图片证据</p></div></header>
    <section class="wb-card">
      <div class="filters"><el-input v-model="query.keyword" clearable placeholder="订单号或标题" @keyup.enter="search"/><el-select v-model="query.status" clearable placeholder="仲裁状态"><el-option label="待仲裁" :value="0"/><el-option label="已处理" :value="1"/></el-select><el-button type="primary" @click="search">查询</el-button></div>
      <el-table v-loading="loading" :data="records">
        <el-table-column label="订单" min-width="220"><template #default="{row}"><strong>#{{ row.order?.id }} {{ row.order?.projectTitle || row.order?.stallTitle || '' }}</strong><div class="muted">争议 #{{ row.dispute.id }}</div></template></el-table-column>
        <el-table-column label="发起人" min-width="130"><template #default="{row}">{{ row.initiator?.nickname || row.initiator?.username }}</template></el-table-column>
        <el-table-column label="争议原因" min-width="220"><template #default="{row}">{{ row.dispute.reason }}</template></el-table-column>
        <el-table-column label="金额" width="110"><template #default="{row}">¥{{ row.order?.amount }}</template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="row.dispute.status===0?'danger':'success'">{{ row.dispute.status===0?'待仲裁':'已处理' }}</el-tag></template></el-table-column>
        <el-table-column label="发起时间" min-width="165"><template #default="{row}">{{ formatDateTime(row.dispute.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="90"><template #default="{row}"><el-button link type="primary" @click="showDetail(row.dispute.id)">处理</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load"/>
    </section>

    <el-drawer v-model="detailVisible" title="争议仲裁详情" size="780px" append-to-body destroy-on-close>
      <div v-loading="detailLoading" class="detail-body" v-if="detail.dispute">
        <el-alert :title="detail.dispute.reason" type="warning" :closable="false" show-icon/>
        <el-descriptions :column="2" border class="summary"><el-descriptions-item label="订单">#{{ detail.order.id }}</el-descriptions-item><el-descriptions-item label="托管金额">¥{{ detail.order.amount }}</el-descriptions-item><el-descriptions-item label="企划方">{{ detail.employer?.nickname }}</el-descriptions-item><el-descriptions-item label="画师">{{ detail.painter?.nickname }}</el-descriptions-item></el-descriptions>
        <h4>双方提交的图片证据</h4><div v-if="detail.evidence?.length" class="evidence"><article v-for="item in detail.evidence" :key="item.id"><el-image :src="item.image" fit="cover" :preview-src-list="detail.evidence.map(e=>e.image)"/><span>{{ evidenceOwner(item.submittedBy) }}</span><small>{{ item.description || '未填写说明' }}</small></article></div><el-empty v-else description="双方均未上传图片证据" :image-size="80"/>
        <h4>订单操作时间线</h4><el-timeline><el-timeline-item v-for="event in detail.events || []" :key="event.id" :timestamp="formatDateTime(event.createdAt)"><strong>{{ event.eventType }}</strong><p>{{ event.content }}</p></el-timeline-item></el-timeline>
        <div v-if="detail.dispute.status===0" class="actions"><el-button type="primary" plain @click="arbitrate('SETTLE_PAINTER')">全额结算画师</el-button><el-button type="danger" @click="arbitrate('REFUND_EMPLOYER')">全额退款企划方</el-button></div>
        <el-alert v-else :title="`仲裁结果：${detail.dispute.result===1?'退款企划方':'结算画师'}；${detail.dispute.resolutionReason}`" type="success" :closable="false"/>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { arbitrateDispute, getAdminDispute, getAdminDisputes } from '@/api/admin'
import { formatDateTime } from '@/utils/date'
const query=reactive({page:1,size:20,status:0,keyword:''});const records=ref([]);const total=ref(0);const loading=ref(false);const detailVisible=ref(false);const detailLoading=ref(false);const detail=ref({})
async function load(){loading.value=true;try{const data=await getAdminDisputes(query);records.value=data.records||[];total.value=data.total||0}finally{loading.value=false}}function search(){query.page=1;load()}
async function showDetail(id){detailVisible.value=true;detailLoading.value=true;try{detail.value=await getAdminDispute(id)}finally{detailLoading.value=false}}
const evidenceOwner=id=>id===detail.value.order?.employerId?'企划方证据':'画师证据'
async function arbitrate(result){const refund=result==='REFUND_EMPLOYER';try{const {value}=await ElMessageBox.prompt('仲裁会立即完成资金流转，处理后不可重复执行。请填写仲裁依据。',refund?'确认全额退款企划方':'确认全额结算画师',{inputType:'textarea',inputValidator:v=>!!v?.trim()||'必须填写仲裁依据',confirmButtonText:'确认仲裁',confirmButtonClass:refund?'el-button--danger':''});const id=detail.value.dispute.id;await arbitrateDispute(id,{result,reason:value});ElMessage.success('仲裁已执行并通知双方');await showDetail(id);await load()}catch{}}
onMounted(load)
</script>
<style scoped>
.admin-page{display:grid;gap:16px}.page-head,.wb-card{background:#fff;border-radius:6px;padding:22px 24px}.page-head h2{margin:0}.page-head p,.muted{margin:7px 0 0;color:#8a929c}.filters{display:grid;grid-template-columns:1.5fr 1fr auto;gap:10px;margin-bottom:18px}.el-pagination{justify-content:flex-end;margin-top:18px}.summary{margin-top:16px}.evidence{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}.evidence article{display:flex;flex-direction:column;gap:6px}.evidence .el-image{width:100%;aspect-ratio:1;border-radius:6px;background:#f3f5f7}.evidence small{color:#8a929c}.actions{display:flex;justify-content:flex-end;gap:10px;position:sticky;bottom:0;padding:18px 0;background:#fff}h4{margin:24px 0 12px}.el-timeline p{margin:6px 0;color:#6f7882}
</style>
