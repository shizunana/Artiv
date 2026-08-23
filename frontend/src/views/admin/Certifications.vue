<template>
  <div class="admin-page">
    <header class="page-head">
      <div><h2>画师认证审核</h2><p>处理初次认证与已认证画师资料修改申请</p></div>
    </header>

    <section class="wb-card">
      <div class="filters">
        <el-input v-model="query.keyword" clearable placeholder="用户名或昵称" @keyup.enter="search" />
        <el-select v-model="query.applicationType" clearable placeholder="申请类型">
          <el-option label="初次认证" :value="0" />
          <el-option label="认证资料修改" :value="1" />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="审核状态">
          <el-option label="待审核" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已驳回" :value="2" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="records">
        <el-table-column label="申请人" min-width="150">
          <template #default="{ row }"><strong>{{ row.user?.nickname || '-' }}</strong><div class="muted">@{{ row.user?.username || '-' }}</div></template>
        </el-table-column>
        <el-table-column label="申请类型" width="130">
          <template #default="{ row }"><el-tag :type="applicationTypeTag(row.certification.applicationType)" effect="plain">{{ applicationTypeText(row.certification.applicationType) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="申请笔名" min-width="135">
          <template #default="{ row }">{{ row.certification.commonId }}</template>
        </el-table-column>
        <el-table-column label="常用站" min-width="210" show-overflow-tooltip>
          <template #default="{ row }"><a :href="row.certification.commonSite" target="_blank" rel="noopener noreferrer">{{ row.certification.commonSite }}</a></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="statusType(row.certification.status)">{{ statusText(row.certification.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="申请时间" min-width="165">
          <template #default="{ row }">{{ formatDateTime(row.certification.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="showDetail(row.certification.id)">{{ row.certification.status === 0 ? '审核' : '查看' }}</el-button></template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </section>

    <el-drawer v-model="detailVisible" title="认证申请详情" size="780px" append-to-body destroy-on-close>
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail.certification">
          <div class="detail-title-row">
            <div>
              <el-tag :type="applicationTypeTag(detail.certification.applicationType)" effect="plain">{{ applicationTypeText(detail.certification.applicationType) }}</el-tag>
              <el-tag :type="statusType(detail.certification.status)">{{ statusText(detail.certification.status) }}</el-tag>
            </div>
            <span>{{ formatDateTime(detail.certification.createdAt) }}</span>
          </div>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="申请人">{{ detail.user?.nickname }}（@{{ detail.user?.username }}）</el-descriptions-item>
            <el-descriptions-item label="作品快照">{{ detail.certification.publicWorkCount }} 件</el-descriptions-item>
            <el-descriptions-item label="画师简介" :span="2">{{ detail.user?.bio || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="题材风格" :span="2">{{ detail.user?.tags || '-' }}</el-descriptions-item>
          </el-descriptions>

          <section v-if="Number(detail.certification.applicationType || 0) === 1" class="comparison-section">
            <h4>认证资料修改对比</h4>
            <div class="comparison-grid comparison-grid--head"><span>字段</span><span>当前已生效</span><span>本次申请</span></div>
            <div class="comparison-grid"><strong>笔名</strong><span>{{ detail.certification.previousCommonId || '—' }}</span><span class="new-value">{{ detail.certification.commonId }}</span></div>
            <div class="comparison-grid"><strong>常用站</strong><span>{{ detail.certification.previousCommonSite || '—' }}</span><a class="new-value" :href="detail.certification.commonSite" target="_blank" rel="noopener noreferrer">{{ detail.certification.commonSite }}</a></div>
          </section>

          <section v-else class="submitted-section">
            <h4>提交的认证资料</h4>
            <dl>
              <div><dt>笔名</dt><dd>{{ detail.certification.commonId }}</dd></div>
              <div><dt>常用站</dt><dd><a :href="detail.certification.commonSite" target="_blank" rel="noopener noreferrer">{{ detail.certification.commonSite }}</a></dd></div>
            </dl>
          </section>

          <div class="sub-head">
            <h4>主页公开作品（{{ detail.works?.length || 0 }}）</h4>
            <router-link :to="`/profiles/${detail.user?.id}?role=painter`" target="_blank">打开画师主页</router-link>
          </div>
          <div class="works">
            <article v-for="work in detail.works || []" :key="work.id">
              <el-image :src="work.image" fit="cover" :preview-src-list="(detail.works || []).map((item) => item.image)" />
              <span>{{ work.title }}</span>
            </article>
          </div>
          <el-alert
            v-if="Number(detail.certification.applicationType || 0) === 0 && (detail.works?.length || 0) < 4"
            title="当前公开作品少于 4 件，后端将拒绝通过本次初次认证"
            type="error"
            :closable="false"
            show-icon
          />

          <div v-if="detail.certification.status === 0" class="actions">
            <el-button type="danger" plain @click="decide(false)">驳回</el-button>
            <el-button type="primary" :disabled="!canApprove" @click="decide(true)">通过申请</el-button>
          </div>

          <h4>历史申请</h4>
          <el-table :data="detail.history || []" size="small">
            <el-table-column label="时间" min-width="145"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
            <el-table-column label="类型" width="120"><template #default="{ row }">{{ applicationTypeText(row.applicationType) }}</template></el-table-column>
            <el-table-column label="结果" width="90"><template #default="{ row }">{{ statusText(row.status) }}</template></el-table-column>
            <el-table-column prop="reviewReason" label="审核意见" min-width="180" />
          </el-table>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveCertification, getAdminCertification, getAdminCertifications, rejectCertification } from '@/api/admin'
import { formatDateTime } from '@/utils/date'

const query = reactive({ page: 1, size: 20, status: 0, applicationType: null, keyword: '' })
const records = ref([])
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref({})

const canApprove = computed(() => Number(detail.value.certification?.applicationType || 0) === 1 || (detail.value.works?.length || 0) >= 4)
const statusText = (value) => ({ 0: '待审核', 1: '已通过', 2: '已驳回' }[value] || '-')
const statusType = (value) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[value] || 'info')
const applicationTypeText = (value) => Number(value || 0) === 1 ? '认证资料修改' : '初次认证'
const applicationTypeTag = (value) => Number(value || 0) === 1 ? 'primary' : 'info'

async function load() {
  loading.value = true
  try {
    const data = await getAdminCertifications(query)
    records.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}
function search() { query.page = 1; load() }
async function showDetail(id) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = {}
  try {
    detail.value = await getAdminCertification(id)
  } finally {
    detailLoading.value = false
  }
}
async function decide(approved) {
  try {
    const { value } = await ElMessageBox.prompt(
      approved ? '请填写通过说明' : '请填写驳回原因',
      approved ? '确认通过申请' : '确认驳回申请',
      { inputType: 'textarea', inputValidator: (text) => Boolean(text?.trim()) || '必须填写审核说明', confirmButtonText: '确认' },
    )
    const id = detail.value.certification.id
    if (approved) await approveCertification(id, value)
    else await rejectCertification(id, value)
    ElMessage.success(approved ? '申请已通过' : '申请已驳回')
    await Promise.all([showDetail(id), load()])
  } catch (_) {
    // 用户取消确认时不提示错误。
  }
}
onMounted(load)
</script>

<style scoped>
.admin-page { display: grid; min-width: 0; gap: 16px; }
.page-head, .wb-card { min-width: 0; padding: 22px 24px; border-radius: 6px; background: #fff; }
.wb-card { overflow: hidden; }
.page-head h2 { margin: 0; }
.page-head p, .muted { margin: 7px 0 0; color: #8a929c; }
.filters { display: grid; grid-template-columns: minmax(200px, 1.4fr) minmax(150px, .8fr) minmax(150px, .8fr) auto; gap: 10px; margin-bottom: 18px; }
.filters > * { min-width: 0; }
.el-pagination { justify-content: flex-end; margin-top: 18px; }
.detail-body { min-height: 240px; padding-right: 10px; }
.detail-title-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px; color: #8a929c; }
.detail-title-row > div { display: flex; gap: 8px; }
.sub-head { display: flex; align-items: center; justify-content: space-between; margin-top: 22px; }
a { color: var(--artiv-primary); }
.submitted-section, .comparison-section { margin-top: 22px; }
.submitted-section dl { display: grid; grid-template-columns: 1fr 2fr; gap: 10px; }
.submitted-section dl div { min-width: 0; padding: 13px 14px; border-radius: 7px; background: #f6f8fa; }
.submitted-section dt { color: #8a929c; font-size: 12px; }
.submitted-section dd { overflow-wrap: anywhere; margin: 6px 0 0; }
.comparison-grid { display: grid; grid-template-columns: 105px minmax(0, 1fr) minmax(0, 1fr); gap: 10px; padding: 12px 14px; border-bottom: 1px solid #edf1f4; overflow-wrap: anywhere; }
.comparison-grid--head { border-radius: 7px 7px 0 0; color: #7c8994; background: #f4f7f9; font-size: 12px; }
.new-value { color: #1679a5; font-weight: 600; }
.works { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin: 10px 0 18px; }
.works article { display: flex; min-width: 0; flex-direction: column; gap: 6px; }
.works .el-image { width: 100%; aspect-ratio: 1; border-radius: 4px; background: #f3f5f7; }
.works span { overflow: hidden; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.actions { display: flex; justify-content: flex-end; margin: 22px 0; }
h4 { margin: 22px 0 10px; }
@media (max-width: 900px) { .filters { grid-template-columns: 1fr 1fr; } .works { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 620px) { .filters { grid-template-columns: 1fr; } }
</style>
