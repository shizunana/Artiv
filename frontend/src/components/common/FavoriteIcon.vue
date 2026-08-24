<template>
  <svg class="favorite-icon" viewBox="0 0 24 24" aria-hidden="true">
    <path
      d="M12 20.4 4.4 13A5.35 5.35 0 0 1 12 5.5 5.35 5.35 0 0 1 19.6 13L12 20.4Z"
      :class="{ 'is-active': active }"
    />
  </svg>
</template>

<script setup>
defineProps({ active: { type: Boolean, default: false } })
</script>

<style scoped>
.favorite-icon { width: 18px; height: 18px; display: block; }
.favorite-icon path { fill: transparent; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
.favorite-icon path.is-active { fill: currentColor; }
</style>
