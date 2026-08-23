<template>
  <div class="admin-page">
    <header class="page-head"><div><h2>用户管理</h2><p>查看账号状态，执行限时或永久封禁</p></div></header>
    <section class="wb-card">
      <div class="filters">
        <el-input v-model="query.keyword" clearable placeholder="用户名或昵称" @keyup.enter="search" />
        <el-select v-model="query.role" clearable placeholder="注册主身份"><el-option label="默认企划方工作台" :value="1"/><el-option label="默认画师工作台" :value="2"/><el-option label="管理员" :value="3"/></el-select>
        <el-select v-model="query.status" clearable placeholder="账号状态"><el-option label="正常" :value="0"/><el-option label="封禁" :value="1"/></el-select>
        <el-select v-model="query.certStatus" clearable placeholder="认证状态"><el-option label="未认证" :value="0"/><el-option label="审核中" :value="1"/><el-option label="已认证" :value="2"/><el-option label="已驳回" :value="3"/></el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="records" empty-text="暂无用户">
        <el-table-column prop="id" label="ID" width="70"/>
        <el-table-column label="用户" min-width="170"><template #default="{row}"><div class="user-cell"><el-avatar :src="row.avatar" :size="34">{{ (row.nickname || row.username || '?')[0] }}</el-avatar><div><strong>{{ row.nickname }}</strong><small>@{{ row.username }}</small></div></div></template></el-table-column>
        <el-table-column label="注册主身份" width="150"><template #default="{row}">{{ roleText(row.role) }}</template></el-table-column>
        <el-table-column label="认证" width="100"><template #default="{row}">{{ certText(row.certStatus) }}</template></el-table-column>
        <el-table-column label="状态" width="105"><template #default="{row}"><el-tag :type="row.status===1?'danger':'success'">{{ row.status===1?'已封禁':'正常' }}</el-tag></template></el-table-column>
        <el-table-column label="注册时间" min-width="165"><template #default="{row}">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="170" fixed="right"><template #default="{row}"><el-button link type="primary" @click="showDetail(row)">详情</el-button><el-button v-if="row.role!==3" link :type="row.status===1?'success':'danger'" @click="openStatus(row)">{{ row.status===1?'解封':'封禁' }}</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </section>

    <el-dialog v-model="statusVisible" :title="statusForm.status===1?'封禁用户':'解除封禁'" width="520px">
      <el-alert v-if="statusForm.status===1" title="封禁将立即终止该用户所有未结束订单，并把托管金额退回企划方。" type="warning" :closable="false" show-icon />
      <el-form label-position="top" class="dialog-form">
        <el-form-item label="操作原因（必填）"><el-input v-model="statusForm.reason" type="textarea" :rows="3" maxlength="500" show-word-limit/></el-form-item>
        <el-form-item v-if="statusForm.status===1" label="封禁截止时间"><el-date-picker v-model="statusForm.bannedUntil" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="不选表示永久封禁"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="statusVisible=false">取消</el-button><el-button :type="statusForm.status===1?'danger':'primary'" :loading="saving" @click="submitStatus">确认</el-button></template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="用户详情" size="620px" append-to-body destroy-on-close>
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail.user" :column="1" border>
          <el-descriptions-item label="账号">{{ detail.user.username }}</el-descriptions-item><el-descriptions-item label="昵称">{{ detail.user.nickname }}</el-descriptions-item>
          <el-descriptions-item label="余额">¥{{ detail.user.balance || 0 }}</el-descriptions-item><el-descriptions-item label="简介">{{ detail.user.bio || '-' }}</el-descriptions-item>
          <el-descriptions-item label="封禁原因">{{ detail.user.banReason || '-' }}</el-descriptions-item><el-descriptions-item label="封禁截止">{{ formatDateTime(detail.user.bannedUntil) }}</el-descriptions-item>
        </el-descriptions>
        <h4>最近订单</h4><el-table :data="detail.recentOrders || []" size="small"><el-table-column prop="id" label="订单"/><el-table-column prop="amount" label="金额"/><el-table-column prop="status" label="状态"/></el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { changeAdminUserStatus, getAdminUser, getAdminUsers } from '@/api/admin'
import { formatDateTime } from '@/utils/date'
const query=reactive({page:1,size:20,keyword:'',role:null,status:null,certStatus:null});const records=ref([]);const total=ref(0);const loading=ref(false)
const statusVisible=ref(false);const saving=ref(false);const selected=ref(null);const statusForm=reactive({status:1,reason:'',bannedUntil:null})
const detailVisible=ref(false);const detailLoading=ref(false);const detail=ref({})
const roleText=v=>({1:'默认企划方工作台',2:'默认画师工作台',3:'管理员'}[v]||'-');const certText=v=>({0:'未认证',1:'审核中',2:'已认证',3:'已驳回'}[v]||'-')
async function load(){loading.value=true;try{const data=await getAdminUsers(query);records.value=data.records||[];total.value=data.total||0}finally{loading.value=false}}
function search(){query.page=1;load()}function openStatus(row){selected.value=row;statusForm.status=row.status===1?0:1;statusForm.reason='';statusForm.bannedUntil=null;statusVisible.value=true}
async function submitStatus(){if(!statusForm.reason.trim()){ElMessage.warning('请填写操作原因');return}saving.value=true;try{const result=await changeAdminUserStatus(selected.value.id,{...statusForm});ElMessage.success(statusForm.status===1?`封禁完成，已退款取消 ${result.cancelledOrders} 笔订单`:'解封完成');statusVisible.value=false;await load()}finally{saving.value=false}}
async function showDetail(row){detailVisible.value=true;detailLoading.value=true;try{detail.value=await getAdminUser(row.id)}finally{detailLoading.value=false}}
onMounted(load)
</script>

<style scoped>
.admin-page{display:grid;gap:16px}.page-head,.wb-card{background:#fff;border-radius:6px;padding:22px 24px}.page-head h2{margin:0}.page-head p{margin:7px 0 0;color:#8a929c}.filters{display:grid;grid-template-columns:1.4fr repeat(3,1fr) auto;gap:10px;margin-bottom:18px}.user-cell{display:flex;align-items:center;gap:10px}.user-cell div{display:flex;flex-direction:column}.user-cell small{color:#929aa4}.el-pagination{justify-content:flex-end;margin-top:18px}.dialog-form{margin-top:18px}h4{margin:24px 0 10px}@media(max-width:900px){.filters{grid-template-columns:1fr 1fr}}
</style>
