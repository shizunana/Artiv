<template>
  <div class="admin-page">
    <header class="page-head"><div><h2>画师认证审核</h2><p>核验主页简介、至少 4 件公开作品和申请资料</p></div></header>
    <section class="wb-card">
      <div class="filters"><el-input v-model="query.keyword" clearable placeholder="用户名或昵称" @keyup.enter="search"/><el-select v-model="query.status" clearable placeholder="审核状态"><el-option label="待审核" :value="0"/><el-option label="已通过" :value="1"/><el-option label="已驳回" :value="2"/></el-select><el-button type="primary" @click="search">查询</el-button></div>
      <el-table v-loading="loading" :data="records">
        <el-table-column label="申请人" min-width="160"><template #default="{row}"><strong>{{ row.user?.nickname }}</strong><div class="muted">@{{ row.user?.username }}</div></template></el-table-column>
        <el-table-column label="常用ID" min-width="130"><template #default="{row}">{{ row.certification.commonId }}</template></el-table-column>
        <el-table-column label="作品快照" width="100"><template #default="{row}">{{ row.certification.publicWorkCount }} 件</template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="statusType(row.certification.status)">{{ statusText(row.certification.status) }}</el-tag></template></el-table-column>
        <el-table-column label="申请时间" min-width="165"><template #default="{row}">{{ formatDateTime(row.certification.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="100"><template #default="{row}"><el-button link type="primary" @click="showDetail(row.certification.id)">审核</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load"/>
    </section>

    <el-drawer v-model="detailVisible" title="认证申请详情" size="720px">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail.certification">
          <el-descriptions :column="2" border><el-descriptions-item label="画师">{{ detail.user?.nickname }}（@{{ detail.user?.username }}）</el-descriptions-item><el-descriptions-item label="常用ID">{{ detail.certification.commonId }}</el-descriptions-item><el-descriptions-item label="个人简介" :span="2">{{ detail.user?.bio || '未填写' }}</el-descriptions-item><el-descriptions-item label="风格标签" :span="2">{{ detail.user?.tags || '-' }}</el-descriptions-item><el-descriptions-item label="常用站" :span="2"><a v-if="detail.certification.commonSite" :href="detail.certification.commonSite" target="_blank" rel="noopener">{{ detail.certification.commonSite }}</a><span v-else>-</span></el-descriptions-item></el-descriptions>
          <div class="sub-head"><h4>主页公开作品（{{ detail.works?.length || 0 }}）</h4><router-link :to="`/profiles/${detail.user?.id}?role=painter`" target="_blank">打开画师主页</router-link></div>
          <div class="works"><article v-for="work in detail.works || []" :key="work.id"><el-image :src="work.image" fit="cover" :preview-src-list="(detail.works||[]).map(item=>item.image)"/><span>{{ work.title }}</span></article></div>
          <el-alert v-if="(detail.works?.length||0)<4" title="当前公开作品少于 4 件，不能通过认证" type="error" :closable="false" show-icon/>
          <div v-if="detail.certification.status===0" class="actions"><el-button type="danger" plain @click="decide(false)">驳回</el-button><el-button type="primary" @click="decide(true)">通过认证</el-button></div>
          <h4>历史申请</h4><el-table :data="detail.history || []" size="small"><el-table-column label="时间"><template #default="{row}">{{ formatDateTime(row.createdAt) }}</template></el-table-column><el-table-column label="结果"><template #default="{row}">{{ statusText(row.status) }}</template></el-table-column><el-table-column prop="reviewReason" label="审核意见"/></el-table>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveCertification, getAdminCertification, getAdminCertifications, rejectCertification } from '@/api/admin'
import { formatDateTime } from '@/utils/date'
const query=reactive({page:1,size:20,status:0,keyword:''});const records=ref([]);const total=ref(0);const loading=ref(false);const detailVisible=ref(false);const detailLoading=ref(false);const detail=ref({})
const statusText=v=>({0:'待审核',1:'已通过',2:'已驳回'}[v]||'-');const statusType=v=>({0:'warning',1:'success',2:'danger'}[v]||'info')
async function load(){loading.value=true;try{const data=await getAdminCertifications(query);records.value=data.records||[];total.value=data.total||0}finally{loading.value=false}}function search(){query.page=1;load()}
async function showDetail(id){detailVisible.value=true;detailLoading.value=true;try{detail.value=await getAdminCertification(id)}finally{detailLoading.value=false}}
async function decide(approved){try{const {value}=await ElMessageBox.prompt(approved?'请填写通过说明':'请填写驳回原因',approved?'确认通过认证':'确认驳回',{inputType:'textarea',inputValidator:v=>!!v?.trim()||'必须填写审核说明',confirmButtonText:'确认'});const id=detail.value.certification.id;if(approved)await approveCertification(id,value);else await rejectCertification(id,value);ElMessage.success(approved?'认证已通过':'申请已驳回');await showDetail(id);await load()}catch{}}
onMounted(load)
</script>

<style scoped>
.admin-page{display:grid;gap:16px}.page-head,.wb-card{background:#fff;border-radius:6px;padding:22px 24px}.page-head h2{margin:0}.page-head p,.muted{margin:7px 0 0;color:#8a929c}.filters{display:grid;grid-template-columns:1.4fr 1fr auto;gap:10px;margin-bottom:18px}.el-pagination{justify-content:flex-end;margin-top:18px}.detail-body{padding-right:10px}.sub-head{display:flex;align-items:center;justify-content:space-between;margin-top:22px}.sub-head a,a{color:var(--artiv-primary)}.works{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin:10px 0 18px}.works article{display:flex;flex-direction:column;gap:6px}.works .el-image{width:100%;aspect-ratio:1;border-radius:4px;background:#f3f5f7}.works span{font-size:12px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.actions{display:flex;justify-content:flex-end;margin:22px 0}h4{margin:22px 0 10px}
</style>
