<template><img v-if="url" :src="url" alt="当前会话的私有参考图" class="private-image" /><span v-else>{{ failed ? '预览不可用' : '加载预览…' }}</span></template>
<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { useGuideStore } from '@/stores/guide'
const props = defineProps({ id: { type: String, required: true } })
const guide = useGuideStore(), url = ref(''), failed = ref(false)
let controller, version = 0
function release() { version++; controller?.abort(); if (url.value) URL.revokeObjectURL(url.value); url.value = '' }
watch(() => props.id, async id => {
  release(); const current = version; failed.value = false; controller = new AbortController()
  try { const blob = await guide.referenceBlob(id, controller.signal); if (blob && current === version) url.value = URL.createObjectURL(blob) }
  catch (_) { if (current === version) failed.value = true }
}, { immediate: true })
onBeforeUnmount(release)
</script>
<style scoped>.private-image { display: block; max-width: 100%; height: 120px; object-fit: contain; margin: 8px auto; border-radius: 8px; }</style>
