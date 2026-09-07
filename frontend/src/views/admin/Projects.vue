<template>
  <div class="admin-page">
    <header class="page-head"><div><h2>企划治理</h2><p>违规下架不改变已有应征和订单，企划方不能自行重新上架</p></div></header>
    <section class="wb-card">
      <div class="filters"><el-input v-model="query.keyword" clearable placeholder="企划标题" @keyup.enter="search"/><el-select v-model="query.moderationStatus" clearable placeholder="治理状态"><el-option label="正常展示" :value="0"/><el-option label="违规下架" :value="1"/></el-select><el-select v-model="query.status" clearable placeholder="业务状态"><el-option label="招募中" :value="0"/><el-option label="已选定" :value="1"/><el-option label="进行中" :value="2"/><el-option label="已完成" :value="3"/><el-option label="已关闭" :value="4"/></el-select><el-button type="primary" @click="search">查询</el-button></div>
      <el-table v-loading="loading" :data="records">
        <el-table-column label="企划" min-width="230"><template #default="{row}"><strong>{{ row.project.title }}</strong><div class="muted">#{{ row.project.id }} · {{ row.employer?.nickname || '-' }}</div></template></el-table-column>
        <el-table-column label="类型" width="110"><template #default="{row}">{{ row.project.projectCategory || '-' }}</template></el-table-column>
        <el-table-column label="预算" width="160"><template #default="{row}">{{ formatProjectBudget(row.project) }}</template></el-table-column>
        <el-table-column label="应征" width="80"><template #default="{row}">{{ row.applicationCount }}</template></el-table-column>
        <el-table-column label="治理状态" width="115"><template #default="{row}"><el-tag :type="row.project.moderationStatus===1?'danger':'success'">{{ row.project.moderationStatus===1?'违规下架':'正常' }}</el-tag></template></el-table-column>
        <el-table-column label="发布时间" min-width="165"><template #default="{row}">{{ formatDateTime(row.project.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="245" fixed="right"><template #default="{row}"><router-link :to="`/projects/${row.project.id}`" target="_blank" class="project-link">进入企划</router-link><el-button link type="primary" @click="showDetail(row.project.id)">治理详情</el-button><el-button link :type="row.project.moderationStatus===1?'success':'danger'" @click="moderate(row.project)">{{ row.project.moderationStatus===1?'管理员恢复':'违规下架' }}</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load"/>
    </section>
    <el-drawer v-model="detailVisible" title="企划治理详情" size="700px" append-to-body destroy-on-close><div v-loading="detailLoading"><div v-if="detail.project" class="drawer-head"><router-link :to="`/projects/${detail.project.id}`" target="_blank"><el-button type="primary" plain>进入企划页面</el-button></router-link></div><el-descriptions v-if="detail.project" :column="1" border><el-descriptions-item label="标题">{{ detail.project.title }}</el-descriptions-item><el-descriptions-item label="企划方">{{ detail.employer?.nickname }}（@{{ detail.employer?.username }}）</el-descriptions-item><el-descriptions-item label="需求详情">{{ detail.project.description }}</el-descriptions-item><el-descriptions-item label="下架原因">{{ detail.project.moderationReason || '-' }}</el-descriptions-item></el-descriptions><h4>现有应征（{{ detail.applications?.length || 0 }}）</h4><el-table :data="detail.applications || []" size="small"><el-table-column prop="id" label="ID"/><el-table-column prop="painterId" label="画师ID"/><el-table-column prop="quote" label="报价"/></el-table><h4>关联订单（{{ detail.orders?.length || 0 }}）</h4><el-table :data="detail.orders || []" size="small"><el-table-column prop="id" label="订单"/><el-table-column prop="amount" label="金额"/><el-table-column prop="status" label="状态"/></el-table></div></el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminProject, getAdminProjects, moderateProject } from '@/api/admin'
import { formatDateTime } from '@/utils/date'
import { formatProjectBudget } from '@/utils/budget'
const query=reactive({page:1,size:20,keyword:'',status:null,moderationStatus:null});const records=ref([]);const total=ref(0);const loading=ref(false);const detailVisible=ref(false);const detailLoading=ref(false);const detail=ref({})
async function load(){loading.value=true;try{const data=await getAdminProjects(query);records.value=data.records||[];total.value=data.total||0}finally{loading.value=false}}function search(){query.page=1;load()}
async function moderate(project){const hiding=project.moderationStatus!==1;try{const {value}=await ElMessageBox.prompt(hiding?'请填写违规下架原因':'请填写管理员恢复说明',hiding?'确认违规下架':'确认恢复',{inputType:'textarea',inputValidator:v=>!!v?.trim()||'必须填写原因'});await moderateProject(project.id,{action:hiding?'HIDE':'RESTORE',reason:value});ElMessage.success(hiding?'企划已违规下架，已通知企划方':'企划已恢复展示');await load()}catch{}}
async function showDetail(id){detailVisible.value=true;detailLoading.value=true;try{detail.value=await getAdminProject(id)}finally{detailLoading.value=false}}onMounted(load)
</script>
<style scoped>
.admin-page{display:grid;gap:16px}.page-head,.wb-card{background:#fff;border-radius:6px;padding:22px 24px}.page-head h2{margin:0}.page-head p,.muted{margin:7px 0 0;color:#8a929c}.filters{display:grid;grid-template-columns:1.5fr 1fr 1fr auto;gap:10px;margin-bottom:18px}.el-pagination{justify-content:flex-end;margin-top:18px}.project-link{margin-right:12px;color:var(--artiv-primary);font-size:14px}.drawer-head{display:flex;justify-content:flex-end;margin-bottom:16px}h4{margin:24px 0 10px}@media(max-width:900px){.filters{grid-template-columns:1fr 1fr}}
</style>
