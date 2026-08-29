<template>
  <div v-if="items.length" class="applicant-list">
    <article
      v-for="item in items"
      :id="applicationCardId(item)"
      :key="item.application?.id || `invitation-${item.invitationId}`"
      class="applicant-card"
      :class="{ 'applicant-card--target': isTargetApplication(item) }"
    >
      <div v-if="showActions && item.application" class="applicant-actions">
        <el-button size="small" :loading="startingChatId === item.painter?.id" @click="$emit('chat', item)">私信</el-button>
        <el-button
          size="small"
          type="primary"
          :loading="orderingApplicationId === item.application.id"
          @click="$emit('order', item)"
        >
          {{ item.hasActiveOrder ? '订单详情' : '发送企划订单' }}
        </el-button>
      </div>

      <button class="applicant-identity" type="button" @click="$emit('open-painter', item.painter?.id)">
        <el-avatar class="applicant-avatar" :size="66" :src="item.painter?.avatar">{{ painterName(item).slice(0, 1) }}</el-avatar>
        <div class="applicant-copy">
          <h3>{{ painterName(item) }}</h3>
          <RatingSummary :average-score="item.painter?.averageScore" :review-count="item.painter?.reviewCount" compact />
          <p>{{ painterBio(item) }}</p>
          <div v-if="painterTags(item).length" class="painter-tags">
            <el-tag v-for="tag in painterTags(item)" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
          </div>
        </div>
      </button>

      <div v-if="item.application" class="quote-block">
        <span>应征报价</span>
        <strong>￥ {{ item.application.quote }}</strong>
      </div>
      <div v-else class="quote-block invitation-waiting">
        <span>邀请状态</span>
        <strong>{{ invitationStatusText(item.invitationStatus) }}</strong>
      </div>

      <div class="applicant-works">
        <template v-for="index in 4" :key="`${item.application?.id || item.invitationId}-${index}`">
          <el-image
            v-if="item.works?.[index - 1]?.image"
            class="work-thumbnail"
            :src="item.works[index - 1].image"
            :preview-src-list="item.works.map((entry) => entry.image).filter(Boolean)"
            :initial-index="index - 1"
            fit="cover"
            lazy
          />
          <div v-else class="work-thumbnail work-placeholder">作品</div>
        </template>
      </div>
    </article>
  </div>
  <el-empty v-else :description="emptyDescription" />
</template>

<script setup>
import RatingSummary from '@/components/reviews/RatingSummary.vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  showActions: { type: Boolean, default: false },
  startingChatId: { type: Number, default: null },
  orderingApplicationId: { type: Number, default: null },
  targetApplicationId: { type: Number, default: null },
  emptyDescription: { type: String, default: '暂无画师应征' },
})

defineEmits(['open-painter', 'chat', 'order'])

function painterName(item) {
  return item.painter?.nickname || item.painter?.username
    || `画师 #${item.application?.painterId || item.painter?.id || ''}`
}
function painterBio(item) {
  const characters = Array.from(String(item.painter?.bio || '这位画师还没有填写个人简介。').trim())
  return characters.length > 20 ? `${characters.slice(0, 20).join('')}...` : characters.join('')
}
function painterTags(item) {
  return String(item.painter?.tags || '').split(',').map((tag) => tag.trim()).filter(Boolean)
}
function invitationStatusText(status) {
  return ({ 0: '等待回应', 1: '已接受', 2: '已拒绝', 3: '已失效' })[Number(status)] || '未知状态'
}
function isTargetApplication(item) {
  return Number(props.targetApplicationId || 0) > 0
    && Number(item?.application?.id || 0) === Number(props.targetApplicationId)
}
function applicationCardId(item) {
  return item?.application?.id ? `application-${item.application.id}` : undefined
}
</script>

<style scoped>
.applicant-list { padding: 4px 28px 28px; }
.applicant-card { position: relative; display: grid; grid-template-columns: minmax(250px, 1.15fr) 110px minmax(300px, 1.5fr); gap: 20px; align-items: center; min-height: 104px; margin-top: 24px; padding: 42px 20px 18px; overflow: hidden; border: 1px solid #e1e5ea; border-radius: 4px; background: #fff; }
.applicant-card--target { border-color: var(--artiv-primary); box-shadow: 0 0 0 2px rgb(0 173 239 / 12%); }
.applicant-identity { display: flex; gap: 15px; align-items: center; min-width: 0; padding: 0; border: 0; background: none; color: inherit; text-align: left; cursor: pointer; }
.applicant-avatar { flex: 0 0 66px; width: 66px !important; min-width: 66px; max-width: 66px; height: 66px !important; min-height: 66px; max-height: 66px; overflow: hidden; }
.applicant-avatar :deep(img) { width: 100%; height: 100%; object-fit: cover; object-position: center; }
.applicant-copy { min-width: 0; }
.applicant-copy h3 { margin: 0 0 7px; color: #3e4650; font-size: 16px; }
.applicant-copy p { display: -webkit-box; min-height: 42px; overflow: hidden; margin: 0; color: #777; font-size: 13px; line-height: 21px; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.painter-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 8px; }
.quote-block { padding-left: 16px; border-left: 1px solid #edf0f3; }
.quote-block span,.quote-block strong { display: block; }.quote-block span { margin-bottom: 6px; color: #999; font-size: 12px; }.quote-block strong { color: #ff7043; font-size: 15px; }
.invitation-waiting strong { color: #e6a23c; }
.applicant-works { display: grid; grid-template-columns: repeat(4,80px); justify-content: end; gap: 8px; }
.work-thumbnail { width: 80px; height: 80px; border-radius: 3px; background: #f1f3f5; }
.work-placeholder { display: grid; place-items: center; color: #a5acb5; font-size: 12px; }
.applicant-actions { position: absolute; top: 10px; right: 12px; z-index: 1; display: flex; gap: 6px; }.applicant-actions :deep(.el-button) { padding: 6px 10px; }
@media(max-width:820px){.applicant-list{padding-left:18px;padding-right:18px}.applicant-card{grid-template-columns:1fr;padding-top:52px}.quote-block{padding:12px 0 0;border-top:1px solid #edf0f3;border-left:0}.applicant-works{grid-template-columns:repeat(4,minmax(0,1fr));justify-content:stretch}.work-thumbnail{width:100%}}
</style>
