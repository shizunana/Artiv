<template>
  <div class="admin-page">
    <header class="page-head">
      <div><h2>用户举报</h2><p>同一被举报用户聚合为一个案件；处理结论只留档，不会自动处罚账号。</p></div>
    </header>

    <section class="wb-card">
      <div class="filters">
        <el-select v-model="query.status" clearable placeholder="处理状态">
          <el-option label="待处理" :value="0" /><el-option label="已处理" :value="1" /><el-option label="已驳回" :value="2" />
        </el-select>
        <el-input v-model.trim="query.keyword" class="keyword" clearable placeholder="昵称、账号或用户 ID" @keyup.enter="search" />
        <el-date-picker v-model="dateRange" class="date-range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" />
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="records" empty-text="暂无用户举报">
        <el-table-column label="被举报用户" min-width="180">
          <template #default="{ row }">
            <div class="user-cell"><el-avatar :size="36" :src="row.targetAvatar || undefined" /><div><strong>{{ row.targetNickname }}</strong><span>用户 #{{ row.reportCase.targetId }}</span></div></div>
          </template>
        </el-table-column>
        <el-table-column label="举报人数" width="110"><template #default="{ row }"><strong class="report-count">{{ row.reporterCount }}</strong> 人</template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="statusType(row.reportCase.status)">{{ statusText(row.reportCase.status) }}</el-tag></template></el-table-column>
        <el-table-column label="首次举报" min-width="165"><template #default="{ row }">{{ formatDateTime(row.reportCase.createdAt) }}</template></el-table-column>
        <el-table-column label="最近更新" min-width="165"><template #default="{ row }">{{ formatDateTime(row.reportCase.updatedAt) }}</template></el-table-column>
        <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="showDetail(row.reportCase.id)">{{ row.reportCase.status === 0 ? '处理' : '查看' }}</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </section>

    <el-drawer v-model="detailVisible" title="用户举报详情" size="760px" append-to-body destroy-on-close>
      <div v-if="detail.reportCase" v-loading="detailLoading" class="detail-body">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="被举报用户">{{ detail.targetNickname }}（#{{ detail.reportCase.targetId }}）</el-descriptions-item>
          <el-descriptions-item label="举报人数">{{ detail.reporterCount }} 人</el-descriptions-item>
          <el-descriptions-item label="案件状态"><el-tag :type="statusType(detail.reportCase.status)">{{ statusText(detail.reportCase.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="首次举报">{{ formatDateTime(detail.reportCase.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-note">管理员在此只记录治理结论；如需封禁账号，请继续使用“用户管理”的现有处罚能力。</div>
        <h4>举报明细</h4>
        <article v-for="item in detail.submissions || []" :key="item.submission.id" class="submission-card">
          <div class="submission-head">
            <div class="user-cell"><el-avatar :size="34" :src="item.reporterAvatar || undefined" /><div><strong>{{ item.reporterNickname }}</strong><span>举报人 #{{ item.submission.reporterId }}</span></div></div>
            <span>{{ formatDateTime(item.submission.createdAt) }}</span>
          </div>
          <el-tag size="small" effect="plain">{{ categoryText(item.submission.category) }}</el-tag>
          <p>{{ item.submission.reason }}</p>
          <div v-if="item.images?.length" class="evidence-images">
            <el-image v-for="url in item.images" :key="url" :src="url" fit="cover" :preview-src-list="item.images" />
          </div>
        </article>

        <el-alert v-if="detail.reportCase.status !== 0" :title="`${statusText(detail.reportCase.status)}：${detail.reportCase.decisionReason || '未填写说明'}`" :type="detail.reportCase.status === 1 ? 'success' : 'info'" :closable="false" />
        <div v-else class="actions"><el-button @click="resolve(false)">驳回举报</el-button><el-button type="primary" @click="resolve(true)">记录已处理</el-button></div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminReport, getAdminReports, resolveAdminReport } from '@/api/admin'
import { formatDateTime } from '@/utils/date'

const query = reactive({ page: 1, size: 20, status: 0, keyword: '' })
const dateRange = ref([])
const records = ref([])
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref({})

async function load() {
  loading.value = true
  try {
    const data = await getAdminReports({ ...query, startDate: dateRange.value?.[0], endDate: dateRange.value?.[1] })
    records.value = data?.records || []
    total.value = Number(data?.total || 0)
  } finally { loading.value = false }
}
function search() { query.page = 1; load() }
async function showDetail(id) {
  detailVisible.value = true
  detailLoading.value = true
  try { detail.value = await getAdminReport(id) } finally { detailLoading.value = false }
}
async function resolve(handled) {
  try {
    const { value } = await ElMessageBox.prompt(
      handled ? '请填写本次治理结论。该操作只记录结论，不会自动封禁用户。' : '请填写驳回原因。',
      handled ? '记录举报已处理' : '驳回举报',
      { inputType: 'textarea', inputValidator: value => !!value?.trim() || '必须填写处理说明', confirmButtonText: '确认处理' },
    )
    const id = detail.value.reportCase.id
    await resolveAdminReport(id, handled, { reason: value.trim() })
    ElMessage.success('举报案件已处理')
    await Promise.all([showDetail(id), load()])
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') throw error
  }
}
function statusText(status) { return ['待处理', '已处理', '已驳回'][status] || '未知' }
function statusType(status) { return ['warning', 'success', 'info'][status] || 'info' }
function categoryText(category) { return ({ HARASSMENT: '骚扰或人身攻击', FRAUD: '欺诈或虚假交易', IMPERSONATION: '冒充他人', ILLEGAL_CONTENT: '违法违规内容', OTHER: '其他问题' })[category] || category }
onMounted(load)
</script>

<style scoped>
.admin-page{display:grid;gap:16px;min-width:0}.page-head,.wb-card{min-width:0;padding:22px 24px;background:#fff;border-radius:6px}.page-head h2{margin:0}.page-head p{margin:7px 0 0;color:#8a929c}.filters{display:flex;flex-wrap:wrap;gap:10px;margin-bottom:18px}.filters .el-select{width:180px}.keyword{width:230px}.date-range{width:270px}.el-pagination{justify-content:flex-end;margin-top:18px}.user-cell{display:flex;align-items:center;gap:10px;min-width:0}.user-cell>div{display:grid;min-width:0}.user-cell strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.user-cell span,.submission-head>span{color:#8a929c;font-size:12px}.report-count{color:var(--artiv-primary);font-size:18px}.detail-body{padding-bottom:28px}.detail-note{margin:18px 0;padding:12px 14px;color:#66727c;background:#f3f8fb;border-radius:7px;line-height:1.6}h4{margin:22px 0 12px}.submission-card{margin-bottom:12px;padding:16px;border:1px solid #e2e8ed;border-radius:8px}.submission-head{display:flex;align-items:center;justify-content:space-between;gap:15px;margin-bottom:13px}.submission-card p{margin:12px 0 0;line-height:1.7;white-space:pre-wrap}.evidence-images{display:grid;grid-template-columns:repeat(5,1fr);gap:8px;margin-top:12px}.evidence-images .el-image{aspect-ratio:1;border-radius:6px}.actions{position:sticky;bottom:0;display:flex;justify-content:flex-end;gap:10px;margin-top:18px;padding:15px 0;background:#fff}
</style>
