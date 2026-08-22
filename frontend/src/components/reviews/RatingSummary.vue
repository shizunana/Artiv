<template>
  <span v-if="reviewCount > 0" class="rating-summary" :class="{ compact }">
    <span class="rating-star">★</span>
    <strong>{{ Number(averageScore).toFixed(1) }}</strong>
    <small v-if="showCount">{{ reviewCount }} 条评价</small>
  </span>
  <span v-else-if="!hideWhenEmpty" class="rating-empty">暂无评价</span>
</template>

<script setup>
defineProps({
  averageScore: { type: Number, default: null },
  reviewCount: { type: Number, default: 0 },
  showCount: { type: Boolean, default: false },
  hideWhenEmpty: { type: Boolean, default: false },
  compact: { type: Boolean, default: false },
})
</script>

<style scoped>
.rating-summary { display: inline-flex; gap: 5px; align-items: center; color: #f4a620; }
.rating-summary strong { color: #394453; font-size: 15px; }
.rating-summary small { color: #8a94a0; font-weight: 400; }
.rating-summary.compact, .rating-summary.compact strong { font-size: 13px; }
.rating-star { font-size: 16px; line-height: 1; }
.rating-empty { color: #9aa4af; font-size: 13px; }
</style>
