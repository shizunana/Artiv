<template>
  <section v-loading="loading" class="review-list">
    <el-alert v-if="error" title="评价加载失败" type="error" :closable="false">
      <template #default><el-button link type="primary" @click="load(true)">重新加载</el-button></template>
    </el-alert>
    <article v-for="item in reviews" :key="item.reviewId" class="review-card">
      <router-link class="reviewer-avatar" :to="profileLink(item)" target="_blank" title="在新标签页打开评价人主页">
        <el-avatar :size="44" :src="item.reviewerAvatar || undefined">{{ item.reviewerNickname?.slice(0, 1) || '用' }}</el-avatar>
      </router-link>
      <div class="review-content">
        <div class="review-head"><strong>{{ item.reviewerNickname }}</strong><el-rate :model-value="item.score" disabled /></div>
        <div class="review-source"><el-tag size="small" effect="dark" type="info">{{ item.sourceType === 'project' ? '企划订单' : '橱窗订单' }}</el-tag><span>{{ item.sourceTitle }}</span></div>
        <p>{{ item.content || '该用户只进行了星级评分。' }}</p>
        <div v-if="item.images?.length" class="review-images">
          <el-image v-for="url in item.images" :key="url" :src="url" fit="cover" :preview-src-list="item.images" preview-teleported />
        </div>
        <div class="review-foot"><span>{{ formatDateTime(item.createdAt) }}</span><el-button v-if="item.canAppeal" link type="danger" @click="appeal(item)">申请删除评价</el-button><span v-else-if="item.appealStatus !== null" class="appeal-status">{{ appealText(item.appealStatus) }}</span></div>
      </div>
    </article>
    <el-empty v-if="!loading && !error && !reviews.length" description="暂无合作评价" />
    <div v-if="reviews.length && hasMore" class="load-more"><el-button :loading="loadingMore" @click="load(false)">加载更多评价</el-button></div>
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReviews, submitReviewAppeal } from '@/api/review'
import { formatDateTime } from '@/utils/date'

const props = defineProps({ userId: { type: Number, required: true }, role: { type: String, required: true } })
const reviews = ref([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const error = ref(false)
const hasMore = ref(false)

onMounted(() => load(true))
watch(() => [props.userId, props.role], () => load(true))

async function load(reset) {
  if (reset) { loading.value = true; page.value = 1; reviews.value = [] } else loadingMore.value = true
  error.value = false
  try {
    const targetPage = reset ? 1 : page.value + 1
    const data = await getReviews({ userId: props.userId, role: props.role, page: targetPage, size: 10 })
    reviews.value = reset ? (data.records || []) : [...reviews.value, ...(data.records || [])]
    page.value = targetPage
    total.value = Number(data.total || 0)
    hasMore.value = reviews.value.length < total.value
  } catch { error.value = true } finally { loading.value = false; loadingMore.value = false }
}
async function appeal(item) {
  try {
    const { value } = await ElMessageBox.prompt('管理员会结合订单记录与评价内容判断，批准后评价将被隐藏且不再计分。', '申请删除评价', {
      inputType: 'textarea', inputValidator: (text) => !!text?.trim() || '请填写申诉原因', inputPlaceholder: '请说明评价存在的问题',
    })
    await submitReviewAppeal(item.reviewId, { reason: value.trim() })
    item.canAppeal = false
    item.appealStatus = 0
    ElMessage.success('申诉已提交，等待管理员处理')
  } catch {}
}
function profileLink(item) { return { name: 'ArtistProfile', params: { id: item.reviewerId }, query: { role: item.reviewerRole } } }
function appealText(status) { return ['申诉审核中', '申诉已通过', '申诉已驳回'][status] || '申诉已提交' }
</script>

<style scoped>
.review-list { min-height: 220px; }
.review-card { display: grid; grid-template-columns: 52px 1fr; gap: 14px; padding: 22px 4px; border-bottom: 1px solid #e3e8ed; }
.reviewer-avatar { width: 44px; height: 44px; }
.review-head, .review-foot, .review-source { display: flex; gap: 12px; align-items: center; }
.review-head { justify-content: space-between; }
.review-head :deep(.el-rate) { height: auto; }
.review-source { margin-top: 10px; color: #536071; font-weight: 600; }
.review-content p { margin: 13px 0; color: #4b5664; line-height: 1.75; white-space: pre-wrap; }
.review-images { display: grid; grid-template-columns: repeat(6, 92px); gap: 8px; margin: 12px 0; }
.review-images :deep(.el-image) { width: 92px; height: 92px; overflow: hidden; border-radius: 5px; }
.review-foot { justify-content: space-between; color: #9aa3ad; font-size: 12px; }
.appeal-status { color: #d18a23; }
.load-more { padding: 20px; text-align: center; }
@media(max-width:720px){.review-images{grid-template-columns:repeat(3,1fr)}.review-images :deep(.el-image){width:100%;height:auto;aspect-ratio:1}}
</style>
