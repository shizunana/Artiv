<template>
  <div class="admin-page">
    <header class="page-head"><div><h2>评价申诉</h2><p>画师可申请隐藏不当评价；管理员只能批准隐藏或驳回，不能修改评分与内容</p></div></header>
    <section class="wb-card">
      <div class="filters">
        <el-select v-model="query.status" clearable placeholder="处理状态"><el-option label="待处理" :value="0"/><el-option label="已批准" :value="1"/><el-option label="已驳回" :value="2"/></el-select>
        <el-input v-model.trim="query.keyword" class="keyword" clearable placeholder="画师昵称或账号" @keyup.enter="search"/>
        <el-date-picker v-model="dateRange" class="date-range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期"/>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="records" empty-text="暂无评价申诉">
        <el-table-column label="申诉画师" min-width="150"><template #default="{row}"><strong>{{ row.applicantNickname }}</strong><div class="muted">用户 #{{ row.applicantId }}</div></template></el-table-column>
        <el-table-column label="原评价人" min-width="140"><template #default="{row}">{{ row.review?.reviewerNickname || '-' }}</template></el-table-column>
        <el-table-column label="来源" min-width="210"><template #default="{row}">{{ row.review?.sourceType === 'project' ? '企划订单' : '橱窗订单' }} · {{ row.review?.sourceTitle }}</template></el-table-column>
        <el-table-column label="原评分" width="120"><template #default="{row}"><span class="stars">★ {{ row.review?.score }}.0</span></template></el-table-column>
        <el-table-column label="申诉原因" min-width="250" show-overflow-tooltip><template #default="{row}">{{ row.appeal.reason }}</template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="statusType(row.appeal.status)">{{ statusText(row.appeal.status) }}</el-tag></template></el-table-column>
        <el-table-column label="提交时间" min-width="165"><template #default="{row}">{{ formatDateTime(row.appeal.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="90"><template #default="{row}"><el-button link type="primary" @click="showDetail(row.appeal.id)">{{ row.appeal.status === 0 ? '处理' : '查看' }}</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load"/>
    </section>

    <el-drawer v-model="detailVisible" title="评价申诉详情" size="720px" append-to-body destroy-on-close>
      <div v-if="detail.appeal" v-loading="detailLoading" class="detail-body">
        <el-descriptions :column="2" border><el-descriptions-item label="申诉画师">{{ detail.applicantNickname }}</el-descriptions-item><el-descriptions-item label="提交时间">{{ formatDateTime(detail.appeal.createdAt) }}</el-descriptions-item><el-descriptions-item label="申诉原因" :span="2">{{ detail.appeal.reason }}</el-descriptions-item></el-descriptions>
        <h4>被申诉评价</h4>
        <article class="review-card">
          <div class="review-head"><strong>{{ detail.review?.reviewerNickname }}</strong><span class="stars">★ {{ detail.review?.score }}.0</span></div>
          <div class="source">{{ detail.review?.sourceType === 'project' ? '企划订单' : '橱窗订单' }} · {{ detail.review?.sourceTitle }}</div>
          <p>{{ detail.review?.content || '该用户只进行了星级评分。' }}</p>
          <div v-if="detail.review?.images?.length" class="images"><el-image v-for="url in detail.review.images" :key="url" :src="url" fit="cover" :preview-src-list="detail.review.images"/></div>
        </article>
        <el-alert v-if="detail.appeal.status !== 0" :title="`${statusText(detail.appeal.status)}：${detail.appeal.decisionReason}`" :type="detail.appeal.status === 1 ? 'success' : 'info'" :closable="false"/>
        <div v-else class="actions"><el-button @click="resolve(false)">驳回申诉</el-button><el-button type="danger" @click="resolve(true)">批准并隐藏评价</el-button></div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveReviewAppeal, getAdminReviewAppeal, getAdminReviewAppeals, rejectReviewAppeal } from '@/api/admin'
import { formatDateTime } from '@/utils/date'

const query = reactive({ page: 1, size: 20, status: 0, keyword: '' })
const dateRange = ref([])
const records = ref([]), total = ref(0), loading = ref(false), detailVisible = ref(false), detailLoading = ref(false), detail = ref({})
async function load(){loading.value=true;try{const data=await getAdminReviewAppeals({...query,startDate:dateRange.value?.[0],endDate:dateRange.value?.[1]});records.value=data.records||[];total.value=data.total||0}finally{loading.value=false}}
function search(){query.page=1;load()}
async function showDetail(id){detailVisible.value=true;detailLoading.value=true;try{detail.value=await getAdminReviewAppeal(id)}finally{detailLoading.value=false}}
async function resolve(approve){try{const {value}=await ElMessageBox.prompt(approve?'批准后评价会立即从公开页面和评分统计中隐藏，且不可恢复。':'请填写驳回依据。',approve?'批准评价申诉':'驳回评价申诉',{inputType:'textarea',inputValidator:v=>!!v?.trim()||'必须填写处理说明',confirmButtonText:'确认处理'});const id=detail.value.appeal.id;if(approve)await approveReviewAppeal(id,{reason:value.trim()});else await rejectReviewAppeal(id,{reason:value.trim()});ElMessage.success('评价申诉已处理');await showDetail(id);await load()}catch{}}
function statusText(status){return ['待处理','已批准','已驳回'][status]||'未知'}
function statusType(status){return ['warning','success','info'][status]||'info'}
onMounted(load)
</script>

<style scoped>
.admin-page{display:grid;gap:16px;min-width:0}.page-head,.wb-card{min-width:0;padding:22px 24px;background:#fff;border-radius:6px}.wb-card{overflow:hidden}.page-head h2{margin:0}.page-head p,.muted{margin:7px 0 0;color:#8a929c}.filters{display:flex;flex-wrap:wrap;gap:10px;margin-bottom:18px}.filters .el-select{width:180px}.keyword{width:220px}.date-range{width:270px}.el-pagination{justify-content:flex-end;margin-top:18px}.stars{color:#f4a620;font-weight:700}.review-card{padding:18px;border:1px solid #e1e6eb;border-radius:6px}.review-head{display:flex;justify-content:space-between}.source{margin-top:10px;color:#566272;font-weight:600}.review-card p{line-height:1.7;white-space:pre-wrap}.images{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}.images .el-image{aspect-ratio:1;border-radius:5px}.actions{display:flex;justify-content:flex-end;gap:10px;position:sticky;bottom:0;padding:18px 0;background:#fff}h4{margin:24px 0 12px}
</style>
