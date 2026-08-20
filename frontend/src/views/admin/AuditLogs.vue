<template>
  <div class="admin-page">
    <header class="page-head"><div><h2>操作审计</h2><p>管理员和自动任务的高风险动作留痕，不提供物理删除</p></div></header>
    <section class="wb-card">
      <div class="filters"><el-input v-model="query.actionType" clearable placeholder="动作类型，如 BAN_USER"/><el-input v-model="query.targetType" clearable placeholder="对象类型，如 USER"/><el-input v-model="query.adminId" clearable placeholder="管理员ID"/><el-button type="primary" @click="search">查询</el-button></div>
      <el-table v-loading="loading" :data="records">
        <el-table-column label="时间" min-width="170"><template #default="{row}">{{ formatDateTime(row.createdAt) }}</template></el-table-column><el-table-column prop="adminId" label="管理员" width="90"><template #default="{row}">{{ row.adminId || '系统' }}</template></el-table-column><el-table-column prop="actionType" label="动作" min-width="180"/><el-table-column prop="targetType" label="对象" width="130"/><el-table-column prop="targetId" label="对象ID" width="100"/><el-table-column prop="reason" label="原因" min-width="230" show-overflow-tooltip/><el-table-column prop="ipAddress" label="来源IP" min-width="130"/><el-table-column label="快照" width="80"><template #default="{row}"><el-button link type="primary" @click="showSnapshot(row)">查看</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load"/>
    </section>
    <el-dialog v-model="snapshotVisible" title="操作前后快照" width="760px"><div class="snapshots"><section><h4>操作前</h4><pre>{{ pretty(selected.beforeData) }}</pre></section><section><h4>操作后</h4><pre>{{ pretty(selected.afterData) }}</pre></section></div></el-dialog>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getAdminAuditLogs } from '@/api/admin'
import { formatDateTime } from '@/utils/date'
const query=reactive({page:1,size:20,actionType:'',targetType:'',adminId:''});const records=ref([]);const total=ref(0);const loading=ref(false);const snapshotVisible=ref(false);const selected=ref({})
async function load(){loading.value=true;try{const params={...query,adminId:query.adminId||undefined};const data=await getAdminAuditLogs(params);records.value=data.records||[];total.value=data.total||0}finally{loading.value=false}}function search(){query.page=1;load()}function showSnapshot(row){selected.value=row;snapshotVisible.value=true}function pretty(value){if(!value)return '-';try{return JSON.stringify(JSON.parse(value),null,2)}catch{return value}}onMounted(load)
</script>
<style scoped>
.admin-page{display:grid;gap:16px}.page-head,.wb-card{background:#fff;border-radius:6px;padding:22px 24px}.page-head h2{margin:0}.page-head p{margin:7px 0 0;color:#8a929c}.filters{display:grid;grid-template-columns:1.2fr 1fr 1fr auto;gap:10px;margin-bottom:18px}.el-pagination{justify-content:flex-end;margin-top:18px}.snapshots{display:grid;grid-template-columns:1fr 1fr;gap:12px}.snapshots h4{margin:0 0 8px}.snapshots pre{min-height:260px;margin:0;padding:12px;overflow:auto;background:#f5f7f9;border-radius:5px;white-space:pre-wrap;word-break:break-all}@media(max-width:900px){.filters,.snapshots{grid-template-columns:1fr}}
</style>
