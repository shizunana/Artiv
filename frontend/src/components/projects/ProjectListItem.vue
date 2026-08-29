<template>
  <article
    class="project-card"
    role="link"
    tabindex="0"
    :aria-label="`查看企划 ${project.title}`"
    @click="$emit('open-detail', project.id)"
    @keydown.enter.self="$emit('open-detail', project.id)"
    @keydown.space.self.prevent="$emit('open-detail', project.id)"
  >
    <img v-if="cover" :src="cover" :alt="`${project.title}封面`" />
    <div v-else class="project-cover">企划</div>

    <div class="project-main">
      <div class="project-title-row">
        <h3 :title="project.title">{{ truncate(project.title) }}</h3>
        <el-tag :type="cardStatus.type">{{ cardStatus.text }}</el-tag>
      </div>
      <p>类型：{{ project.category || '未分类' }} · 发布时间：{{ formatDateTime(project.createdAt) }}</p>
      <p class="project-description" :title="project.description || ''">
        企划简介：{{ truncate(project.description || '暂无简介', 15) }}
      </p>
    </div>

    <div class="project-side">
      <el-button
        v-if="showPanel"
        class="project-panel-button"
        type="primary"
        round
        @click.stop="$emit('open-panel', project.id)"
      >企划面板</el-button>
      <div class="project-budget">
        <small>预算区间</small>
        <strong>{{ formatProjectBudget(project) }}</strong>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { formatProjectBudget } from '@/utils/budget'
import { formatDateTime } from '@/utils/date'
import { PROJECT_STATUS, statusText, statusType } from '@/utils/status'
import { isProjectExpired } from '@/utils/projectDeadline'

const props = defineProps({
  project: { type: Object, required: true },
  showPanel: { type: Boolean, default: true },
})

defineEmits(['open-detail', 'open-panel'])

const cover = computed(() => {
  const images = props.project.images
  if (!images) return ''
  try {
    const parsed = JSON.parse(images)
    if (Array.isArray(parsed)) return parsed[0] || ''
  } catch { /* 兼容历史逗号分隔数据 */ }
  return String(images).split(',').map((item) => item.trim()).find(Boolean) || ''
})

const cardStatus = computed(() => {
  const project = props.project
  if (Number(project.moderationStatus) === 1) return { text: '违规下架', type: 'danger' }
  if (Number(project.status) === 4) {
    return Number(project.mode) === 1 && Number(project.invitationStatus) === 2
      ? { text: '已拒绝', type: 'danger' }
      : { text: '已下架', type: 'info' }
  }
  if (isProjectExpired(project.deadline)) return { text: '已过期', type: 'info' }
  if (Number(project.mode) === 1) {
    const invitation = {
      0: { text: '待回应', type: 'warning' },
      1: { text: '已接受', type: 'success' },
      2: { text: '已拒绝', type: 'danger' },
    }[Number(project.invitationStatus)]
    if (invitation) return invitation
  }
  return {
    text: statusText(PROJECT_STATUS, project.status),
    type: statusType(PROJECT_STATUS, project.status),
  }
})

function truncate(value, limit = 10) {
  const characters = Array.from(String(value || ''))
  return characters.length > limit ? `${characters.slice(0, limit).join('')}...` : characters.join('')
}
</script>

<style scoped>
.project-card { display: grid; width: 100%; box-sizing: border-box; grid-template-columns: 92px minmax(0, 1fr) minmax(220px, 260px); align-items: center; gap: 18px; padding: 16px 8px; border-bottom: 1px solid var(--artiv-border); border-radius: 6px; outline: none; cursor: pointer; transition: background-color .18s ease, box-shadow .18s ease; }
.project-card:hover,.project-card:focus-visible { background: #f8fcfe; box-shadow: inset 3px 0 0 color-mix(in srgb, var(--artiv-primary) 55%, transparent); }
.project-card:hover .project-title-row h3 { color: var(--artiv-primary); }
.project-card img,.project-cover { width: 92px; height: 72px; border-radius: 7px; object-fit: cover; }
.project-cover { display: grid; place-items: center; background: var(--artiv-section-alt); color: var(--artiv-text-secondary); }
.project-main { min-width: 0; }
.project-title-row { display: flex; min-width: 0; align-items: center; gap: 10px; }
.project-title-row h3 { min-width: 0; margin: 0; overflow: hidden; font-size: 16px; text-overflow: ellipsis; white-space: nowrap; }
.project-main p { margin: 8px 0 0; overflow: hidden; color: var(--artiv-text-secondary); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.project-main .project-description { color: var(--artiv-text); }
.project-side { display: grid; min-width: 0; grid-template-columns: auto minmax(82px, auto); align-items: center; justify-content: end; gap: 16px; }
.project-panel-button { min-width: 96px; border: 0; font-weight: 600; box-shadow: 0 5px 14px rgb(0 169 232 / 18%); }
.project-panel-button:hover { transform: translateY(-1px); box-shadow: 0 7px 18px rgb(0 169 232 / 25%); }
.project-budget { display: grid; min-width: 0; justify-items: end; gap: 5px; }
.project-budget small { color: var(--artiv-text-secondary); }
.project-budget strong { color: var(--artiv-accent); font-size: 18px; white-space: nowrap; }
@media (max-width: 720px) {
  .project-card { grid-template-columns: 70px minmax(0, 1fr); }
  .project-card img,.project-cover { width: 70px; }
  .project-side { grid-column: 2; justify-content: start; }
  .project-budget { justify-items: start; }
}
</style>
