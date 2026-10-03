<template>
  <!-- 只渲染后端公开投影，绝不展示 reference.detail 或使用模型生成的 URL。 -->
  <button type="button" class="resource-card" :disabled="disabled" @click="$emit('open', source)">
    <span class="resource-card__header">
      <span class="resource-card__cover">
        <img v-if="coverUrl && failedCover !== coverUrl" :src="coverUrl" :alt="`${source.title || kindLabel}封面`" loading="lazy" referrerpolicy="no-referrer" @error="failedCover = coverUrl" />
        <span v-else>{{ source.kind === 'artist' ? '作品预览' : kindLabel }}</span>
      </span>
      <span class="resource-card__heading">
        <span class="resource-card__meta">
          <span class="resource-card__kind">{{ kindLabel }}</span>
          <span v-if="source.card?.category" class="resource-card__category">{{ source.card.category }}</span>
        </span>
        <strong class="resource-card__title">{{ source.title || kindLabel }}</strong>
        <span v-if="source.kind === 'artist' || (source.kind === 'stall' && creatorName)" class="resource-card__creator">
          <span class="resource-card__avatar">
            <img v-if="avatarUrl && failedAvatar !== avatarUrl" :src="avatarUrl" :alt="`${creatorName}头像`" loading="lazy" referrerpolicy="no-referrer" @error="failedAvatar = avatarUrl" />
            <span v-else>{{ Array.from(creatorName || '画师')[0] }}</span>
          </span>
          <span>{{ source.kind === 'artist' ? '认证画师' : creatorName }}</span>
        </span>
      </span>
    </span>
    <span v-for="field in fields" :key="field.label" class="resource-card__field">
      <span class="resource-card__label">{{ field.label }}</span>
      <span :class="{ 'resource-card__price': ['预算', '价格'].includes(field.label) }">{{ field.value }}</span>
    </span>
    <span v-if="source.card?.summary" class="resource-card__summary">{{ source.card.summary }}</span>
    <span class="resource-card__action">{{ disabled ? '正在确认…' : `查看${kindLabel === '画师' ? '画师主页' : kindLabel}` }}<span aria-hidden="true">↗</span></span>
  </button>
</template>

<script setup>
import { computed, ref } from 'vue'
const props = defineProps({ source: { type: Object, required: true }, disabled: Boolean })
defineEmits(['open'])
const kindLabel = computed(() => ({ project: '企划', stall: '橱窗', artist: '画师' }[props.source.kind] || '详情'))
const creatorName = computed(() => props.source.card?.creatorName || (props.source.kind === 'artist' ? props.source.title : ''))
const fields = computed(() => (props.source.card?.fields || []).filter(field => field.value?.trim()
  && !(props.source.kind === 'stall' && creatorName.value && field.label === '画师')))
const failedCover = ref(''), failedAvatar = ref('')
function imageUrl(value) {
  if (typeof value !== 'string') return ''
  const url = value.trim()
  if (/[\\\u0000-\u001f\u007f]/.test(url)) return ''
  if (/^\/uploads\//.test(url)) return url
  try {
    const parsed = new URL(url)
    return ['http:', 'https:'].includes(parsed.protocol) && !parsed.username && !parsed.password ? url : ''
  } catch { return '' }
}
const coverUrl = computed(() => imageUrl(props.source.card?.coverUrl))
const avatarUrl = computed(() => imageUrl(props.source.card?.avatarUrl))
</script>

<style scoped>
.resource-card { display: flex; flex-direction: column; gap: 8px; box-sizing: border-box; width: 100%; padding: 14px; border: 1px solid #e0eaf0; border-radius: 12px; background: #fff; color: #344d5c; font: inherit; font-size: 12px; line-height: 1.5; text-align: left; overflow-wrap: anywhere; cursor: pointer; transition: border-color .15s, background-color .15s; }
.resource-card:hover:not(:disabled) { border-color: #84d4ee; background: #f8fcfe; }
.resource-card:focus-visible { outline: 2px solid #00aeef; outline-offset: 2px; }
.resource-card:disabled { opacity: .65; cursor: wait; }
.resource-card__header { display: flex; align-items: center; gap: 12px; width: 100%; }
.resource-card__cover { display: grid; place-items: center; flex: 0 0 80px; width: 80px; height: 88px; overflow: hidden; border-radius: 9px; background: #f0f6fa; color: #8ca2af; font-size: 11px; }
.resource-card__cover img, .resource-card__avatar img { width: 100%; height: 100%; object-fit: cover; }
.resource-card__heading { display: flex; flex-direction: column; gap: 7px; min-width: 0; flex: 1; }
.resource-card__creator { display: flex; align-items: center; gap: 6px; color: #738b99; }
.resource-card__avatar { display: grid; place-items: center; flex: 0 0 24px; width: 24px; height: 24px; border-radius: 50%; overflow: hidden; background: #e8f6fc; color: #0783ad; font-size: 11px; }
.resource-card__meta { display: flex; align-items: baseline; gap: 8px; }
.resource-card__kind { flex-shrink: 0; padding: 2px 7px; border-radius: 5px; color: #0783ad; background: #eaf8fd; }
.resource-card__category { color: #7b8c96; }
.resource-card__title { color: #243b49; font-size: 15px; font-weight: 600; }
.resource-card__field { display: grid; grid-template-columns: 56px minmax(0, 1fr); gap: 8px; }
.resource-card__label { color: #7b8c96; }
.resource-card__price { color: #087da6; font-weight: 600; }
.resource-card__summary { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; color: #7b8c96; }
.resource-card__action { display: flex; justify-content: space-between; align-items: center; margin-top: 2px; padding-top: 9px; border-top: 1px solid #edf2f5; color: #087da6; }
</style>
